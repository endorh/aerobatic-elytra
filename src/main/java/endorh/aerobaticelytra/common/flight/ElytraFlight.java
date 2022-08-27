package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.flight.mode.FlightModeTags;
import endorh.util.math.Vec3f;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static endorh.aerobaticelytra.common.item.IAbility.Ability.FUEL;
import static java.lang.Math.min;
import static net.minecraft.util.Mth.cos;

public class ElytraFlight {
	
	/**
	 * Elytra physics, from {@link Player#travel}
	 *
	 * @param player Player flying
	 * @param travelVector Travel vector from
	 *   {@link net.minecraft.world.entity.LivingEntity#travel}
	 */
	public static boolean onElytraTravel(
	  Player player, @SuppressWarnings("unused") Vec3 travelVector
	) {
		// Stop conditions
		if (!player.isFallFlying() || player.getAbilities().flying
		    || !getFlightDataOrDefault(player).getFlightMode().is(FlightModeTags.ELYTRA))
			return false;
		final ItemStack elytra = AerobaticElytraLogic.getAerobaticElytra(player);
		if (elytra.isEmpty())
			return false;
		final IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(elytra);
		if (((elytra.getDamageValue() >= elytra.getMaxDamage() - 1 || !(spec.getAbility(FUEL) > 0))
		     && !player.isCreative())
		    || player.isInLava() || player.isInWater())
			return false;
		
		// Previous pos
		double prevX = player.getX();
		double prevY = player.getY();
		double prevZ = player.getZ();
		
		// Get gravity and apply SLOW_FALLING potion effect as needed
		double grav = TravelHandler.travelGravity(player);
		
		Vec3 motionVec = player.getDeltaMovement();
		
		double hSpeedPrev = new Vec3f(motionVec).hNorm();
		
		// Cancel fall damage if falling slowly
		if (motionVec.y > -0.5D) {
			player.fallDistance = 1.0F;
		}
		
		// Compute motion
		Vec3 look = player.getLookAngle();
		float pitchRad = player.getXRot() * (float) Math.PI / 180F;
		double look_hor_norm = Math.sqrt(look.x * look.x + look.z * look.z);
		double motion_hor_norm = Math.sqrt(motionVec.x * motionVec.x + motionVec.z * motionVec.z);
		double look_norm = look.length();
		float pitchCos = cos(pitchRad);
		pitchCos = (float) ((double) pitchCos * (double) pitchCos *
		                    min(1.0D, look_norm / 0.4D));
		motionVec = player.getDeltaMovement().add(0.0D, grav * (-1.0D + (double) pitchCos * 0.75D),
		                                          0.0D);
		if (motionVec.y < 0.0D && look_hor_norm > 0.0D) {
			double y_friction = motionVec.y * -0.1D * (double) pitchCos;
			motionVec = motionVec
			  .add(
			    look.x * y_friction / look_hor_norm, y_friction, look.z * y_friction / look_hor_norm);
		}
		
		if (pitchRad < 0.0F && look_hor_norm > 0.0D) {
			double y_acc = motion_hor_norm * (double) (-Mth.sin(pitchRad)) * 0.04D;
			motionVec = motionVec.add(-look.x * y_acc / look_hor_norm, y_acc * 3.2D,
			                          -look.z * y_acc / look_hor_norm);
		}
		
		if (look_hor_norm > 0.0D) {
			motionVec = motionVec
			  .add((look.x / look_hor_norm * motion_hor_norm - motionVec.x) * 0.1D, 0.0D,
			       (look.z / look_hor_norm * motion_hor_norm - motionVec.z) * 0.1D);
		}
		
		// Apply motion
		player.setDeltaMovement(motionVec.multiply(0.99F, 0.98F, 0.99F));
		player.move(MoverType.SELF, player.getDeltaMovement());
		
		// Apply collision damage
		if (player.horizontalCollision && !player.level.isClientSide) {
			double hSpeedNew = new Vec3f(player.getDeltaMovement()).hNorm();
			double reaction = hSpeedPrev - hSpeedNew;
			float collisionStrength = (float) (reaction * 10.0D - 3.0D);
			if (collisionStrength > 0.0F) {
				player.playSound(
				  collisionStrength > 4
				  ? SoundEvents.PLAYER_BIG_FALL
				  : SoundEvents.PLAYER_SMALL_FALL,
				  1.0F, 1.0F);
				player.hurt(
				  DamageSource.FLY_INTO_WALL,
				  collisionStrength);
			}
		}
		
		// Stop flying when on ground
		if (player.isOnGround() && player.isEffectiveAi()) {
			player.stopFallFlying();
		}
		
		// Update player limbSwingAmount
		player.calculateEntityAnimation(player, player instanceof FlyingAnimal);
		
		// Add movement stat
		player.checkMovementStatistics(
		  player.getX() - prevX, player.getY() - prevY, player.getZ() - prevZ);
		
		// Cancel default travel logic
		return true;
	}
}
