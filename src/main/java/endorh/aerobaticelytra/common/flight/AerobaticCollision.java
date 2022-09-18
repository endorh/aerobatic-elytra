package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.common.block.BrokenLeavesBlock;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.collision.slime_bounce;
import endorh.aerobaticelytra.common.config.Const;
import endorh.util.math.Vec3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.util.math.Vec3f.forAxis;
import static java.lang.Math.max;
import static net.minecraft.core.Direction.fromAxisAndDirection;
import static net.minecraft.util.Mth.*;

/**
 * Aerobatic collisions logic
 */
public class AerobaticCollision {
	
	/**
	 * Handle aerobatic collisions, both vertically and horizontally
	 *
	 * @param player The player colliding
	 * @param hSpeedPrev Previous speed
	 */
	public static void onAerobaticCollision(
	  Player player, double hSpeedPrev, Vec3f motionVec
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		float propStrength = data.getPropulsionStrength();
		
		// Apply collision damage
		double hSpeedNew = new Vec3f(player.getDeltaMovement()).hNorm();
		double reaction = hSpeedPrev - hSpeedNew;
		float collisionStrength = (float) (reaction * 10D - 3D);
		float damageModifier = 0F;
		final AABB aaBB = player.getBoundingBox();
		List<BlockPos> collided = null;
		double d;
		for (d = 0.005D; d <= 0.5D; d *= 2D) {
			collided = getCollidedBlocksInAABB(player.level, aaBB.inflate(d));
			if (!collided.isEmpty()) break;
		}
		if (collided.isEmpty()) return; // Do not set onGround to false here
		int destroyed = 0;
		float speed = motionVec.norm() * 20F;
		boolean slimeBounce = false;
		boolean shouldBreakLeaves =
		  Config.collision.leave_breaking.enable
		  && speed > Config.collision.leave_breaking.min_speed_tick
		  && player.getRandom().nextFloat()
		     < Config.collision.leave_breaking.chance
		       + Config.collision.leave_breaking.chance_linear * speed;
		boolean preventLanding = false;
		for (BlockPos pos: collided) {
			final BlockState bs = player.level.getBlockState(pos);
			if (bs.is(Blocks.HAY_BLOCK)) {
				damageModifier = max(damageModifier, Config.collision.hay_bale_multiplier);
			} else if (bs.is(BlockTags.LEAVES)) {
				if (shouldBreakLeaves) {
					BrokenLeavesBlock.breakLeaves(player.level, pos);
					propStrength *= 0.8F;
					motionVec.mul(0.6F);
					destroyed++;
					preventLanding = true;
				}
			} else if (bs.getBlock() instanceof SlimeBlock) {
				slimeBounce = true;
			} else damageModifier = 1F;
		}
		if (destroyed > 0) {
			data.setPropulsionStrength(propStrength);
			player.setDeltaMovement(motionVec.toVector3d());
		}
		if (slimeBounce && slime_bounce.enable
		    && motionVec.norm() > slime_bounce.min_speed_tick) {
			preventLanding = true;
			VectorBase base = data.getRotationBase();
			if (player.level.isClientSide)
				data.getCameraBase().set(base);
			
			boolean bounced = tryBounce(player, base, motionVec);
			
			if (bounced && player.level.isClientSide) {
				data.setLastBounceTime(System.currentTimeMillis());
				data.getPreBounceBase().set(data.getCameraBase());
				data.getPosBounceBase().set(base);
			}
		}
		
		if (collisionStrength > 0F && damageModifier > 0F) {
			player.playSound(
			  collisionStrength > 4F
			  ? SoundEvents.PLAYER_BIG_FALL
			  : SoundEvents.PLAYER_SMALL_FALL,
			  1F, 1F);
			player.hurt(
			  DamageSource.FLY_INTO_WALL,
			  damageModifier * collisionStrength * Config.collision.damage);
		}
		
		data.setLiftCut(clamp(data.getLiftCut() + 0.2F, 0F, 1F));
		
		// Stop flying when on ground
		if (player.isOnGround() && player.isEffectiveAi() && !preventLanding)
			player.stopFallFlying();
		if (preventLanding)
			player.setOnGround(false);
	}
	
	public static boolean tryBounce(Player player, VectorBase base, Vec3f motionVec) {
		boolean bounced = false;
		for (int includeCorners = 0; includeCorners < 2; includeCorners++) {
			for (Axis axis: Axis.values()) {
				final Direction dir = fromAxisAndDirection(axis, AxisDirection.POSITIVE);
				if (shouldBounceDir(player, dir, false) ^
				    shouldBounceDir(player, dir.getOpposite(), includeCorners != 0)) {
					bounce(player, base, motionVec, axis);
					bounced = true;
				}
			}
			if (bounced) break;
			// If none of the axis resulted in a bounce, try again with wider hitboxes, including corners
		}
		player.setDeltaMovement(motionVec.toVector3d());
		return bounced;
	}
	
	public static void bounce(
	  Player player, VectorBase base, Vec3f motionVec, Axis axis
	) {
		// The bounce axis is slightly tilted based on the player's roll tilt,
		// depending on how parallel is the bounce against the plane
		final Vec3f ax = forAxis(axis);
		final Vec3f look = base.look.copy();
		look.sub(ax, ax.dot(look));
		if (!look.isZero()) {
			final float bounceTilt = clamp(
			  (1F - Math.abs(base.look.dot(ax)))
			  * getAerobaticDataOrDefault(player).getTiltRoll()
			  * Const.SLIME_BOUNCE_ROLLING_TILT_SENS,
			  -Const.SLIME_BOUNCE_MAX_ROLLING_TILT_DEG, Const.SLIME_BOUNCE_MAX_ROLLING_TILT_DEG
			);
			look.unitary();
			ax.rotateAlongOrtVecDegrees(look, bounceTilt);
		}
		base.mirror(ax);
		motionVec.reflect(ax);
		motionVec.mul(slime_bounce.friction);
		if (slime_bounce.angular_friction < 1F) {
			final IAerobaticData data = getAerobaticDataOrDefault(player);
			data.setTiltPitch(data.getTiltPitch() * slime_bounce.angular_friction);
			data.setTiltYaw(data.getTiltYaw() * slime_bounce.angular_friction);
			data.setTiltRoll(data.getTiltRoll() * slime_bounce.angular_friction);
		}
		player.level.playSound(
		  player, player.blockPosition(), SoundEvents.SLIME_BLOCK_HIT,
		  SoundSource.PLAYERS, clampedLerp(0F, 1F, motionVec.norm() / 1.6F), 1F);
		player.awardStat(FlightStats.AEROBATIC_SLIME_BOUNCES, 1);
	}
	
	public static boolean shouldBounceDir(
	  Player player, Direction dir, boolean includeCorners
	) {
		return !getCollidedBlocksInAABB(
		  player.level, sideAABB(player.getBoundingBox(), dir, includeCorners),
		  bs -> bs.getBlock() instanceof SlimeBlock
		).isEmpty();
	}
	
	public static AABB sideAABB(AABB b, Direction dir, boolean includeCorners) {
		double e = 0.5D; // Grow epsilon
		double c = includeCorners? e : 0D;
		return switch (dir) {
			case EAST -> new AABB(b.maxX, b.minY - c, b.minZ - c, b.maxX + e, b.maxY + c, b.maxZ + c);
			case WEST -> new AABB(b.minX - e, b.minY - c, b.minZ - c, b.minX, b.maxY + c, b.maxZ + c);
			case SOUTH -> new AABB(b.minX - c, b.minY - c, b.maxZ, b.maxX + c, b.maxY + c, b.maxZ + e);
			case NORTH -> new AABB(b.minX - c, b.minY - c, b.minZ - e, b.maxX + c, b.maxY + c, b.minZ);
			case UP -> new AABB(b.minX - c, b.maxY, b.minZ - c, b.maxX + c, b.maxY + e, b.maxZ + c);
			case DOWN -> new AABB(b.minX - c, b.minY - e, b.minZ - c, b.maxX + c, b.minY, b.maxZ + c);
		};
	}
	
	public static List<BlockPos> getCollidedBlocksInAABB(
	  Level world, AABB aaBB
	) {
		return getCollidedBlocksInAABB(world, aaBB, bs -> bs.getMaterial().blocksMotion());
	}
	
	public static List<BlockPos> getCollidedBlocksInAABB(
	  Level world, AABB aaBB, Predicate<BlockState> selector
	) {
		List<BlockPos> list = new ArrayList<>();
		for (int i = floor(aaBB.minX); i < ceil(aaBB.maxX); i++) {
			for (int j = floor(aaBB.minY); j < ceil(aaBB.maxY); j++) {
				for (int k = floor(aaBB.minZ); k < ceil(aaBB.maxZ); k++) {
					BlockPos pos = new BlockPos(i, j, k);
					if (selector.test(world.getBlockState(pos)))
						list.add(pos);
				}
			}
		}
		return list;
	}
}
