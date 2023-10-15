package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.flight.mode.FlightModeTags;
import endorh.util.math.Vec3f;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static endorh.aerobaticelytra.common.item.IAbility.Ability.FUEL;
import static java.lang.Math.min;

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
		if ((elytra.getDamageValue() >= elytra.getMaxDamage() - 1 || !(spec.getAbility(FUEL) > 0))
		     && !player.isCreative()
		    || player.isInLava() || player.isInWater())
			return false;
		
		// Previous pos
		double prevX = player.getX();
		double prevY = player.getY();
		double prevZ = player.getZ();
		
		// Get gravity and apply SLOW_FALLING potion effect as needed
		double grav = TravelHandler.travelGravity(player);
		
		Vec3 delta = player.getDeltaMovement();
		
		double hSpeedPrev = new Vec3f(delta).hNorm();
		
		// Cancel fall damage if falling slowly
		if (delta.y > -0.5D) player.fallDistance = 1.0F;
		
		// Compute motion
		Vec3 look = player.getLookAngle();
		float pitchRad = player.getXRot() * (float) Math.PI / 180F;
		double look_h_norm = look.horizontalDistance();
		double delta_h_norm = delta.horizontalDistance();
		double look_norm = look.length();
		float pitchCos = Mth.cos(pitchRad);
		pitchCos = pitchCos * pitchCos * min(1.0F, (float) look_norm / 0.4F);
		delta = player.getDeltaMovement().add(0.0D, grav * (-1.0D + (double) pitchCos * 0.75D), 0.0D);
		if (delta.y < 0.0D && look_h_norm > 0.0D) {
			double y_friction = delta.y * -0.1D * (double) pitchCos;
			delta = delta.add(look.x * y_friction / look_h_norm, y_friction, look.z * y_friction / look_h_norm);
		}
		
		if (pitchRad < 0.0F && look_h_norm > 0.0D) {
			double y_acc = delta_h_norm * (double) (-Mth.sin(pitchRad)) * 0.04D;
			delta = delta.add(-look.x * y_acc / look_h_norm, y_acc * 3.2D,
			                          -look.z * y_acc / look_h_norm);
		}
		
		if (look_h_norm > 0.0D) {
			delta = delta.add(
				(look.x / look_h_norm * delta_h_norm - delta.x) * 0.1D, 0.0D,
				(look.z / look_h_norm * delta_h_norm - delta.z) * 0.1D);
		}
		
		// Apply motion
		player.setDeltaMovement(delta.multiply(0.99F, 0.98F, 0.99F));
		player.move(MoverType.SELF, player.getDeltaMovement());
		
		// Apply collision damage
		if (player.horizontalCollision && !player.level.isClientSide) {
			double hSpeedNew = new Vec3f(player.getDeltaMovement()).hNorm();
			double reaction = hSpeedPrev - hSpeedNew;
			float collisionStrength = (float) (reaction * 10.0D - 3.0D);
			if (collisionStrength > 0.0F) {
				// replicate `LivingEntity#getFallDamageSound`
				player.playSound(
				  collisionStrength > 4
				  ? SoundEvents.PLAYER_BIG_FALL
				  : SoundEvents.PLAYER_SMALL_FALL,
				  1.0F, 1.0F);
				player.hurt(player.damageSources().flyIntoWall(), collisionStrength);
			}
		}
		
		// Stop flying when on ground
		if (player.isOnGround() && player.isEffectiveAi()) {
			player.stopFallFlying();
		}
		
		// Update player limbSwingAmount
		player.calculateEntityAnimation(player instanceof FlyingAnimal);
		
		// Add movement stat
		player.checkMovementStatistics(
		  player.getX() - prevX, player.getY() - prevY, player.getZ() - prevZ);
		
		// Cancel default travel logic
		return true;
	}
}
