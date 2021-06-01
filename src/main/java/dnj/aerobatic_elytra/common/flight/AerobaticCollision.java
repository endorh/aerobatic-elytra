package dnj.aerobatic_elytra.common.flight;

import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.block.BrokenLeavesBlock;
import dnj.aerobatic_elytra.common.capability.IAerobaticData;
import dnj.aerobatic_elytra.common.config.Config;
import dnj.aerobatic_elytra.common.flight.AerobaticFlight.VectorBase;
import dnj.endor8util.math.Vec3f;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlimeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static dnj.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static dnj.endor8util.math.Vec3f.forAxis;
import static java.lang.Math.max;
import static net.minecraft.util.Direction.*;
import static net.minecraft.util.Direction.Axis.*;
import static net.minecraft.util.math.MathHelper.*;

/**
 * Aerobatic collisions logic
 */
public class AerobaticCollision {
	/**
	 * Handle aerobatic collisions, both vertically and horizontally
	 * @param player The player colliding
	 * @param hSpeedPrev Previous speed
	 */
	public static void onAerobaticCollision(
	  PlayerEntity player, double hSpeedPrev, Vec3f motionVec
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		float propStrength = data.getPropulsionStrength();
		
		// Apply collision damage
		double hSpeedNew = new Vec3f(player.getMotion()).hNorm();
		double reaction = hSpeedPrev - hSpeedNew;
		float collisionStrength = (float)(reaction * 10D - 3D);
		float damageModifier = 0F;
		final AxisAlignedBB aaBB = player.getBoundingBox();
		List<BlockPos> collided = getCollidedBlocksInAABB(player.world, aaBB.grow(0.001D));
		if (collided.isEmpty())
			damageModifier = 1F;
		int destroyed = 0;
		float speed = motionVec.norm();
		boolean slimeBounce = false;
		boolean shouldBreakLeaves =
		  Config.should_break_leaves
		  && speed > Config.break_leaves_min_speed
		  && player.getRNG().nextFloat() <
		     Config.break_leaves_chance + Config.break_leaves_chance_linear * speed;
		boolean preventLanding = false;
		for (BlockPos pos : collided) {
			final BlockState bs = player.world.getBlockState(pos);
			Block block = bs.getBlock();
			if (block == Blocks.HAY_BLOCK) {
				damageModifier = max(damageModifier, Config.hay_bale_collision_multiplier);
			} else if (block.isIn(BlockTags.LEAVES)) {
				if (shouldBreakLeaves) {
					BrokenLeavesBlock.breakLeaves(player.world, pos);
					propStrength *= 0.8F;
					motionVec.mul(0.6F);
					destroyed++;
					preventLanding = true;
				}
			} else if (block instanceof SlimeBlock) {
				slimeBounce = true;
			} else damageModifier = 1F;
		}
		if (destroyed > 0) {
			data.setPropulsionStrength(propStrength);
			player.setMotion(motionVec.toVector3d());
		}
		if (slimeBounce && Config.should_bounce_on_slime
		    && motionVec.norm() > Config.slime_bounce_min_speed) {
			VectorBase base = data.getRotationBase();
			boolean bounced = false;
			if (AerobaticElytraLogic.isAbstractClientPlayerEntity(player))
				data.getCameraBase().set(base);
			if (bounceDir(player, EAST) ^ bounceDir(player, WEST)) {
				bounce(player, base, motionVec, X);
				bounced = true;
			}
			if (bounceDir(player, SOUTH) ^ bounceDir(player, NORTH)) {
				bounce(player, base, motionVec, Z);
				bounced = true;
			}
			if (bounceDir(player, UP) ^ bounceDir(player, DOWN)) {
				bounce(player, base, motionVec, Y);
				preventLanding = true;
				bounced = true;
			}
			player.setMotion(motionVec.toVector3d());
			if (bounced && AerobaticElytraLogic.isAbstractClientPlayerEntity(player)) {
				data.setLastBounceTime(System.currentTimeMillis());
				data.getPreBounceBase().set(data.getCameraBase());
				data.getPosBounceBase().set(base);
			}
		}
		if (collisionStrength > 0F && damageModifier > 0F) {
			player.playSound(
			  collisionStrength > 4F
			  ? SoundEvents.ENTITY_PLAYER_BIG_FALL
			  : SoundEvents.ENTITY_PLAYER_SMALL_FALL,
			  1F, 1F);
			player.attackEntityFrom(
			  DamageSource.FLY_INTO_WALL,
			  damageModifier * collisionStrength * Config.collision_damage);
		}
		
		// Stop flying when on ground
		if (player.isOnGround() && player.isServerWorld() && !preventLanding)
			player.stopFallFlying();
		if (preventLanding) {
			player.setOnGround(false);
		}
	}
	
	public static void bounce(
	  PlayerEntity player, VectorBase base, Vec3f motionVec, Axis axis
	) {
		final Vec3f ax = forAxis(axis);
		// Totally arbitrary
		final Vec3f look = base.look.copy();
		look.sub(ax, ax.dot(look));
		if (!look.isZero()) {
			final float par = 1F - Math.abs(base.look.dot(ax));
			final float rollTilt = getAerobaticDataOrDefault(player).getTiltRoll() / 6F;
			look.unitary();
			ax.rotateAlongOrtVec(look, par * rollTilt);
		}
		base.mirror(ax);
		motionVec.reflect(ax);
		motionVec.mul(Config.slime_bounce_friction);
		player.world.playSound(
		  player, player.getPosition(), SoundEvents.BLOCK_SLIME_BLOCK_HIT,
		  SoundCategory.PLAYERS, (float)clampedLerp(0F, 1F, motionVec.norm() / 1.6F), 1F);
		player.addStat(FlightStats.AEROBATIC_SLIME_BOUNCES, 1);
	}
	
	public static boolean bounceDir(
	  PlayerEntity player, Direction dir) {
		return !getCollidedBlocksInAABB(
		  player.world, sideAABB(player.getBoundingBox(), dir),
		  bs -> bs.getBlock() instanceof SlimeBlock
		).isEmpty();
	}
	
	public static AxisAlignedBB sideAABB(AxisAlignedBB b, Direction dir) {
		double d = 0.001D;
		switch (dir) {
			case EAST: return new AxisAlignedBB(b.maxX, b.minY, b.minZ, b.maxX + d, b.maxY, b.maxZ);
			case WEST: return new AxisAlignedBB(b.minX - d, b.minY, b.minZ, b.minX, b.maxY, b.maxZ);
			case SOUTH: return new AxisAlignedBB(b.minX, b.minY, b.maxZ, b.maxX, b.maxY, b.maxZ + d);
			case NORTH: return new AxisAlignedBB(b.minX, b.minY, b.minZ - d, b.maxX, b.maxY, b.minZ);
			case UP: return new AxisAlignedBB(b.minX, b.maxY, b.minZ, b.maxX, b.maxY + d, b.maxZ);
			case DOWN: return new AxisAlignedBB(b.minX, b.minY - d, b.minZ, b.maxX, b.minY, b.maxZ);
			default: throw new IllegalArgumentException("Unknown direction: " + dir);
		}
	}
	
	public static List<BlockPos> getCollidedBlocksInAABB(
	  World world, AxisAlignedBB aaBB
	) {
		return getCollidedBlocksInAABB(world, aaBB, bs -> bs.getMaterial().blocksMovement());
	}
	
	public static List<BlockPos> getCollidedBlocksInAABB(
	  World world, AxisAlignedBB aaBB, Predicate<BlockState> selector
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
