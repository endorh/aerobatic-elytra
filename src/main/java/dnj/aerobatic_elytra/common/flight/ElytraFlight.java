package dnj.aerobatic_elytra.common.flight;

import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.endor8util.math.Vec3f;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import static java.lang.Math.min;
import static net.minecraft.util.math.MathHelper.cos;

public class ElytraFlight {
	
	/**
	 * Elytra physics, from {@link net.minecraft.entity.player.PlayerEntity#travel}
	 * @param player Player flying
	 * @param travelVector Travel vector from {@link net.minecraft.entity.LivingEntity#travel}
	 */
	public static boolean onElytraTravel(
	  PlayerEntity player, @SuppressWarnings("unused") Vector3d travelVector
	) {
		if (!AerobaticElytraLogic.shouldElytraFly(player))
			return false;
		
		// Previous pos
		double prevX = player.getPosX();
		double prevY = player.getPosY();
		double prevZ = player.getPosZ();
		
		// Get gravity and apply SLOW_FALLING potion effect as needed
		double grav = TravelHandler.travelGravity(player);
		
		Vector3d motionVec = player.getMotion();
		
		double hSpeedPrev = new Vec3f(motionVec).hNorm();
		
		// Cancel fall damage if falling slowly
		if (motionVec.y > -0.5D) {
			player.fallDistance = 1.0F;
		}
		
		// Compute motion
		Vector3d look = player.getLookVec();
		float pitchRad = player.rotationPitch * (float)Math.PI / 180F;
		double look_hor_norm = Math.sqrt(look.x * look.x + look.z * look.z);
		double motion_hor_norm = Math.sqrt(motionVec.x * motionVec.x + motionVec.z * motionVec.z);
		double look_norm = look.length();
		float pitchCos = cos(pitchRad);
		pitchCos = (float) ((double) pitchCos * (double) pitchCos *
		                    min(1.0D, look_norm / 0.4D));
		motionVec = player.getMotion().add(0.0D, grav * (-1.0D + (double) pitchCos * 0.75D),
		                                   0.0D);
		if (motionVec.y < 0.0D && look_hor_norm > 0.0D) {
			double y_friction = motionVec.y * -0.1D * (double) pitchCos;
			motionVec = motionVec
			  .add(look.x * y_friction / look_hor_norm, y_friction, look.z * y_friction / look_hor_norm);
		}
		
		if (pitchRad < 0.0F && look_hor_norm > 0.0D) {
			double y_acc = motion_hor_norm * (double) (-MathHelper.sin(pitchRad)) * 0.04D;
			motionVec = motionVec.add(-look.x * y_acc / look_hor_norm, y_acc * 3.2D,
			                          -look.z * y_acc / look_hor_norm);
		}
		
		if (look_hor_norm > 0.0D) {
			motionVec = motionVec
			  .add((look.x / look_hor_norm * motion_hor_norm - motionVec.x) * 0.1D, 0.0D,
			       (look.z / look_hor_norm * motion_hor_norm - motionVec.z) * 0.1D);
		}
		
		// Apply motion
		player.setMotion(motionVec.mul(0.99F, 0.98F, 0.99F));
		player.move(MoverType.SELF, player.getMotion());
		
		// Apply collision damage
		if (player.collidedHorizontally && !player.world.isRemote) {
			double hSpeedNew = new Vec3f(player.getMotion()).hNorm();
			double reaction = hSpeedPrev - hSpeedNew;
			float collisionStrength = (float)(reaction * 10.0D - 3.0D);
			if (collisionStrength > 0.0F) {
				player.playSound(
				  collisionStrength > 4
				  ? SoundEvents.ENTITY_PLAYER_BIG_FALL
				  : SoundEvents.ENTITY_PLAYER_SMALL_FALL,
				  1.0F, 1.0F);
				player.attackEntityFrom(
				  DamageSource.FLY_INTO_WALL,
				  collisionStrength);
			}
		}
		
		// Stop flying when on ground
		if (player.isOnGround() && player.isServerWorld()) {
			player.stopFallFlying();
		}
		
		// Update player limbSwingAmount
		player.func_233629_a_(player, player instanceof IFlyingAnimal);
		
		// Add movement stat
		player.addMovementStat(
		  player.getPosX() - prevX, player.getPosY() - prevY, player.getPosZ() - prevZ);
		
		// Cancel default travel logic
		return true;
	}
}
