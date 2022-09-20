package endorh.aerobaticelytra.client.input;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.config.ClientConfig.lookaround;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import endorh.aerobaticelytra.common.flight.VectorBase;
import endorh.flightcore.events.PlayerTurnEvent;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static java.lang.Math.abs;
import static net.minecraft.util.math.MathHelper.*;

/**
 * Handle Player rotation events
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class RotationHandler {
	public static final float ROLL_SENS_PRESCALE = 0.012F;
	public static final float PITCH_SENS_PRESCALE = 0.02F;
	public static final float YAW_SENS_PRESCALE = 0.1F;
	
	private static boolean flying = false;
	private static int scheduledAimCancel = 0;
	
	/**
	 * Event filter for the player rotation<br>
	 */
	@SubscribeEvent
	public static void onPlayerEntityRotateEvent(PlayerTurnEvent event) {
		PlayerEntity player = event.player;
		if (AerobaticFlight.isAerobaticFlying(player)) {
			flying = true;
			event.setCanceled(true); // Prevent default rotation
			IAerobaticData data = getAerobaticDataOrDefault(player);
			float x = (float) event.x;
			float y = (float) event.y;
			if (data.isLookingAround()) {
				onLookAround(player, x, y);
			} else if (player.isInWater()) {
				onUnderwaterPlayerRotate(player, x, y);
			} else onPlayerRotate(player, x, y);
		} else if (flying) flying = false;
	}
	
	public static void onLookAround(PlayerEntity player, float x, float y) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		x *= -0.15F * lookaround.sensitivity;
		y *= 0.15F * lookaround.sensitivity;
		data.setLookAroundYaw(clamp(data.getLookAroundYaw() + x, -lookaround.max_yaw, lookaround.max_yaw));
		data.setLookAroundPitch(clamp(data.getLookAroundPitch() + y, -lookaround.max_pitch, lookaround.max_pitch));
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
		int invertPitch = settings.invertMouse ? -1 : 1;
		int invertRoll = 1;
		if (ClientConfig.controls.invert_pitch)
			invertPitch *= -1;
		if (settings.getPointOfView() == PointOfView.THIRD_PERSON_FRONT
		    && ClientConfig.controls.invert_front_third_person) {
			invertPitch *= -1;
			invertRoll *= -1;
		}
		
		// Get previous values
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		// Pre-scaled so that 1 is my preferred sensibility
		tiltRoll += scaledX * ROLL_SENS_PRESCALE * ClientConfig.controls.roll_sens * invertRoll;
		tiltPitch += scaledY * PITCH_SENS_PRESCALE * ClientConfig.controls.pitch_sens * invertPitch;
		
		// Clamp within limit
		tiltRoll = clamp(tiltRoll, -Config.aerobatic.tilt.range_roll, Config.aerobatic.tilt.range_roll);
		tiltPitch = clamp(tiltPitch, -Config.aerobatic.tilt.range_pitch, Config.aerobatic.tilt.range_pitch);
		
		// Update yaw tilt from the moveStrafing field
		float yawDelta = -0.5F * signum(tiltYaw) + 1.5F * signum(player.moveStrafing);
		if (player.moveStrafing == 0)
			yawDelta = signum(yawDelta) * clamp(2 * abs(yawDelta), 0F, abs(tiltYaw));
		tiltYaw = clamp(tiltYaw + yawDelta * YAW_SENS_PRESCALE * ClientConfig.controls.yaw_sens,
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
		int invertPitch = settings.invertMouse ? -1 : 1;
		int invertRoll = 1;
		if (ClientConfig.controls.invert_pitch)
			invertPitch *= -1;
		if (settings.getPointOfView() == PointOfView.THIRD_PERSON_FRONT
		    && ClientConfig.controls.invert_front_third_person) {
			invertPitch *= -1;
			invertRoll *= -1;
		}
		
		// Get previous values
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		VectorBase base = data.getRotationBase();
		final float underwaterSens = (float) clampedLerp(
		  Const.UNDERWATER_CONTROLS_DIRECT_SENSIBILITY_MAX, Const.UNDERWATER_CONTROLS_DIRECT_SENSIBILITY_MIN,
		  (float) player.getMotion().length() / 0.8F);
		
		// Instantaneous rotation
		final float pitchDelta =
		  (float) -scaledY * PITCH_SENS_PRESCALE * ClientConfig.controls.pitch_sens * invertPitch * underwaterSens;
		final float rollDelta =
		  (float) scaledX * ROLL_SENS_PRESCALE * ClientConfig.controls.roll_sens * invertRoll * underwaterSens;
		
		// Pre-scaled so that 1 is my preferred sensibility
		tiltRoll += scaledX * ROLL_SENS_PRESCALE * ClientConfig.controls.roll_sens * invertRoll;
		tiltPitch += scaledY * PITCH_SENS_PRESCALE * ClientConfig.controls.pitch_sens * invertPitch;
		
		// Clamp within limit
		tiltRoll = clamp(tiltRoll, -Config.aerobatic.tilt.range_roll, Config.aerobatic.tilt.range_roll);
		tiltPitch = clamp(tiltPitch, -Config.aerobatic.tilt.range_pitch, Config.aerobatic.tilt.range_pitch);
		
		// Apply instantaneous rotation
		base.look.rotateAlongOrtVecDegrees(base.roll, pitchDelta);
		base.normal.rotateAlongOrtVecDegrees(base.roll, pitchDelta);
		base.roll.rotateAlongOrtVecDegrees(base.look, rollDelta);
		base.normal.rotateAlongOrtVecDegrees(base.look, rollDelta);
		
		float yawDelta = -0.5F * signum(tiltYaw) + 1.5F * signum(player.moveStrafing);
		if (player.moveStrafing == 0)
			yawDelta = signum(yawDelta) * clamp(2 * abs(yawDelta), 0F, abs(tiltYaw));
		final float underwaterYawSens = Const.UNDERWATER_YAW_RANGE_MULTIPLIER;
		tiltYaw = clamp(tiltYaw + yawDelta * YAW_SENS_PRESCALE * ClientConfig.controls.yaw_sens * underwaterYawSens,
		  -Config.aerobatic.tilt.range_yaw * underwaterYawSens, Config.aerobatic.tilt.range_yaw * underwaterYawSens);
		
		// Update tilt
		data.setTiltPitch(tiltPitch);
		data.setTiltRoll(tiltRoll);
		data.setTiltYaw(tiltYaw);
	}
	
	@SubscribeEvent public static void onBowCharge(ArrowNockEvent event) {
		if (!lookaround.aim_with_bow) return;
		IAerobaticData data = getAerobaticDataOrDefault(event.getPlayer());
		if (!data.isLookingAround() && !data.isLookAroundPersistent()) {
			data.setAimingBow(true);
		}
	}
	
	@SubscribeEvent public static void onBowShoot(ArrowLooseEvent event) {
		IAerobaticData data = getAerobaticDataOrDefault(event.getPlayer());
		if (data.isAimingBow()) scheduledAimCancel = 2;
	}
	
	@SubscribeEvent public static void onPlayerTickEvent(PlayerTickEvent event) {
		PlayerEntity player = event.player;
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (!lookaround.aim_with_bow) {
			data.setAimingBow(false);
			return;
		}
		if (data.isAimingBow() && !(player.getHeldItemMainhand().getItem() instanceof BowItem)
		    && !(player.getHeldItemOffhand().getItem() instanceof BowItem)) {
			data.setAimingBow(false);
		} else if (scheduledAimCancel > 0 && scheduledAimCancel-- == 1) {
			data.setAimingBow(false);
		}
	}
}
