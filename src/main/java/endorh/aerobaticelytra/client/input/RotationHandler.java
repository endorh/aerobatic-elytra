package endorh.aerobaticelytra.client.input;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import endorh.aerobaticelytra.common.flight.AerobaticFlight.VectorBase;
import endorh.flightcore.events.PlayerTurnEvent;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.util.math.Interpolator.clampedLerp;
import static java.lang.Math.abs;

/**
 * Handle Player rotation events
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class RotationHandler {
	
	public static final float ROLL_SENS_PRESCALE = 0.012F;
	public static final float PITCH_SENS_PRESCALE = 0.02F;
	public static final float YAW_SENS_PRESCALE = 0.1F;
	
	private static boolean flying = false;
	
	/**
	 * Event filter for the player rotation<br>
	 */
	@SubscribeEvent
	public static void onPlayerEntityRotateEvent(PlayerTurnEvent event) {
		PlayerEntity player = event.player;
		if (AerobaticFlight.isAerobaticFlying(player)) {
			flying = true;
			event.setCanceled(true); // Prevent default rotation
			if (player.isInWater())
				onUnderwaterPlayerRotate(player, (float) event.x, (float) event.y);
			else onPlayerRotate(player, (float) event.x, (float) event.y);
			// Update server (done every tick instead)
			//new DTiltPacket(data).send();
		} else if (flying) {
			flying = false;
		}
	}
	
	/**
	 * Handle the rotation of the player
	 */
	public static void onPlayerRotate(PlayerEntity player, float x, float y) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		
		// Hardcoded mouse scaling from Minecraft code (Entity#rotateTowards())
		double scaledY = y * 0.15D;
		double scaledX = x * 0.15D;
		
		// Inverted controls
		final GameSettings settings = Minecraft.getInstance().gameSettings;
		int i_p = settings.invertMouse ? -1 : 1;
		int i_r = 1;
		if (ClientConfig.controls.invert_pitch)
			i_p *= -1;
		if (settings.getPointOfView() == PointOfView.THIRD_PERSON_FRONT
		    && ClientConfig.controls.invert_front_third_person) {
			i_p *= -1;
			i_r *= -1;
		}
		
		// Get previous values
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		// Pre-scaled so that 1 is my preferred sensibility
		tiltRoll += scaledX * ROLL_SENS_PRESCALE * ClientConfig.controls.roll_sens * i_r;
		tiltPitch += scaledY * PITCH_SENS_PRESCALE * ClientConfig.controls.pitch_sens * i_p;
		
		// Clamp within limit
		tiltRoll = MathHelper.clamp(tiltRoll, -Config.aerobatic.tilt.range_roll, Config.aerobatic.tilt.range_roll);
		tiltPitch = MathHelper.clamp(tiltPitch, -Config.aerobatic.tilt.range_pitch, Config.aerobatic.tilt.range_pitch);
		
		// Update yaw tilt from the moveStrafing field
		float yawDelta = -0.5F * MathHelper.signum(tiltYaw) + 1.5F * MathHelper.signum(player.moveStrafing);
		if (player.moveStrafing == 0)
			yawDelta = MathHelper.signum(yawDelta) * MathHelper.clamp(2 * abs(yawDelta), 0F, abs(tiltYaw));
		tiltYaw = MathHelper.clamp(tiltYaw + yawDelta * YAW_SENS_PRESCALE * ClientConfig.controls.yaw_sens,
		                           -Config.aerobatic.tilt.range_yaw, Config.aerobatic.tilt.range_yaw);
		
		// Update tilt
		data.setTiltPitch(tiltPitch);
		data.setTiltRoll(tiltRoll);
		data.setTiltYaw(tiltYaw);
	}
	
	public static void onUnderwaterPlayerRotate(PlayerEntity player, float x, float y) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		
		// Hardcoded mouse scaling from Minecraft code (Entity#rotateTowards())
		double scaledY = y * 0.15D;
		double scaledX = x * 0.15D;
		
		// Inverted controls
		final GameSettings settings = Minecraft.getInstance().gameSettings;
		int i_p = settings.invertMouse ? -1 : 1;
		int i_r = 1;
		if (ClientConfig.controls.invert_pitch)
			i_p *= -1;
		if (settings.getPointOfView() == PointOfView.THIRD_PERSON_FRONT
		    && ClientConfig.controls.invert_front_third_person) {
			i_p *= -1;
			i_r *= -1;
		}
		
		// Get previous values
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		VectorBase base = data.getRotationBase();
		final float underwaterSens = clampedLerp(
		  Const.UNDERWATER_CONTROLS_DIRECT_SENSIBILITY_MAX, Const.UNDERWATER_CONTROLS_DIRECT_SENSIBILITY_MIN,
		  (float) player.getMotion().length() / 0.8F);
		
		/*LOGGER.debug(format(
		  "X delta: %5.2f, Y delta: %5.2f",
		  scaledX * ROLL_SENS_PRESCALE * ClientConfig.controls.roll_sens * i_r * underwaterSens,
		  scaledY * PITCH_SENS_PRESCALE * ClientConfig.controls.pitch_sens * i_p * underwaterSens));*/
		
		// Instantaneous rotation
		final float pitchDelta =
		  (float) -scaledY * PITCH_SENS_PRESCALE * ClientConfig.controls.pitch_sens * i_p * underwaterSens;
		final float rollDelta =
		  (float) scaledX * ROLL_SENS_PRESCALE * ClientConfig.controls.roll_sens * i_r * underwaterSens;
		
		// Pre-scaled so that 1 is my preferred sensibility
		tiltRoll += scaledX * ROLL_SENS_PRESCALE * ClientConfig.controls.roll_sens * i_r;
		tiltPitch += scaledY * PITCH_SENS_PRESCALE * ClientConfig.controls.pitch_sens * i_p;
		
		// Clamp within limit
		tiltRoll = MathHelper.clamp(tiltRoll, -Config.aerobatic.tilt.range_roll, Config.aerobatic.tilt.range_roll);
		tiltPitch = MathHelper.clamp(tiltPitch, -Config.aerobatic.tilt.range_pitch, Config.aerobatic.tilt.range_pitch);
		
		// Apply instantaneous rotation
		base.look.rotateAlongOrtVecDegrees(base.roll, pitchDelta);
		base.normal.rotateAlongOrtVecDegrees(base.roll, pitchDelta);
		base.roll.rotateAlongOrtVecDegrees(base.look, rollDelta);
		base.normal.rotateAlongOrtVecDegrees(base.look, rollDelta);
		
		float yawDelta = -0.5F * MathHelper.signum(tiltYaw) + 1.5F * MathHelper.signum(player.moveStrafing);
		if (player.moveStrafing == 0)
			yawDelta = MathHelper.signum(yawDelta) * MathHelper.clamp(2 * abs(yawDelta), 0F, abs(tiltYaw));
		final float underwaterYawSens = Const.UNDERWATER_YAW_RANGE_MULTIPLIER;
		tiltYaw = MathHelper.clamp(tiltYaw + yawDelta * YAW_SENS_PRESCALE * ClientConfig.controls.yaw_sens * underwaterYawSens,
		  -Config.aerobatic.tilt.range_yaw * underwaterYawSens, Config.aerobatic.tilt.range_yaw * underwaterYawSens);
		
		// Update tilt
		data.setTiltPitch(tiltPitch);
		data.setTiltRoll(tiltRoll);
		data.setTiltYaw(tiltYaw);
	}
}
