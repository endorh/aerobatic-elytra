package dnj.aerobatic_elytra.client.input;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.client.config.ClientConfig;
import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.capability.IAerobaticData;
import dnj.aerobatic_elytra.common.config.Config;
import dnj.aerobatic_elytra.common.flight.AerobaticFlight.VectorBase;
import dnj.flight_core.events.PlayerEntityRotateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static dnj.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static java.lang.Math.abs;
import static java.lang.String.format;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.signum;

/**
 * Handle Player rotation events
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class RotationHandler {
	
	public static final float ROLL_SENS_PRESCALE = 0.012F;
	public static final float PITCH_SENS_PRESCALE = 0.02F;
	public static final float YAW_SENS_PRESCALE = 0.1F;
	private static final Logger LOGGER = LogManager.getLogger();
	
	private static boolean flying = false;
	
	/**
	 * Event filter for the player rotation<br>
	 */
	@SubscribeEvent
	public static void onPlayerEntityRotateEvent(PlayerEntityRotateEvent event) {
		PlayerEntity player = event.player;
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (AerobaticElytraLogic.shouldAerobaticFly(player)) {
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
		int i_p = Minecraft.getInstance().gameSettings.invertMouse? -1 : 1;
		int i_r = 1;
		if (ClientConfig.invert_pitch)
			i_p *= -1;
		if (Minecraft.getInstance().gameSettings.getPointOfView()
		    == PointOfView.THIRD_PERSON_FRONT
		    && ClientConfig.invert_front_third_person) {
			i_p *= -1;
			i_r *= -1;
		}
		
		// Get previous values
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		// Pre-scaled so that 1 is my preferred sensibility
		tiltRoll += scaledX * ROLL_SENS_PRESCALE * ClientConfig.roll_sens * i_r;
		tiltPitch += scaledY * PITCH_SENS_PRESCALE * ClientConfig.pitch_sens * i_p;
		
		tiltRoll = clamp(tiltRoll, -Config.tilt_range_roll, Config.tilt_range_roll);
		tiltPitch = clamp(tiltPitch, -Config.tilt_range_pitch, Config.tilt_range_pitch);
		
		// Update yaw tilt from the moveStrafing field
		float yawDelta = -0.5F * signum(tiltYaw) + 1.5F * signum(player.moveStrafing);
		if (player.moveStrafing == 0)
			yawDelta = signum(yawDelta) * clamp(2 * abs(yawDelta), 0F, abs(tiltYaw));
		tiltYaw = clamp(tiltYaw + yawDelta * YAW_SENS_PRESCALE * ClientConfig.yaw_sens,
		                  -Config.tilt_range_yaw, Config.tilt_range_yaw);
		
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
		int i_p = Minecraft.getInstance().gameSettings.invertMouse? -1 : 1;
		int i_r = 1;
		if (ClientConfig.invert_pitch)
			i_p *= -1;
		if (Minecraft.getInstance().gameSettings.getPointOfView()
		    == PointOfView.THIRD_PERSON_FRONT
		    && ClientConfig.invert_front_third_person) {
			i_p *= -1;
			i_r *= -1;
		}
		
		// Get previous values
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		VectorBase base = data.getRotationBase();
		float underwaterSens = 8F;
		
		LOGGER.debug(format(
		  "X delta: %5.2f, Y delta: %5.2f",
		  scaledX * ROLL_SENS_PRESCALE * ClientConfig.roll_sens * i_r * underwaterSens,
		  scaledY * PITCH_SENS_PRESCALE * ClientConfig.pitch_sens * i_p * underwaterSens));
		
		float pitchDelta =
		  (float) -scaledY * PITCH_SENS_PRESCALE * ClientConfig.pitch_sens * i_p * underwaterSens;
		float rollDelta =
		  (float) scaledX * ROLL_SENS_PRESCALE * ClientConfig.roll_sens * i_r * underwaterSens;
		
		tiltRoll += scaledX * ROLL_SENS_PRESCALE * ClientConfig.roll_sens * i_r;
		tiltPitch += scaledY * PITCH_SENS_PRESCALE * ClientConfig.pitch_sens * i_p;
		
		tiltRoll = clamp(tiltRoll, -Config.tilt_range_roll * 0.5F, Config.tilt_range_roll * 0.5F);
		tiltPitch = clamp(tiltPitch, -Config.tilt_range_pitch * 0.5F, Config.tilt_range_pitch * 0.5F);
		
		base.look.rotateAlongOrtVecDegrees(base.roll, pitchDelta);
		base.normal.rotateAlongOrtVecDegrees(base.roll, pitchDelta);
		base.roll.rotateAlongOrtVecDegrees(base.look, rollDelta);
		base.normal.rotateAlongOrtVecDegrees(base.look, rollDelta);
		
		float yawDelta = -0.5F * signum(tiltYaw) + 1.5F * signum(player.moveStrafing);
		if (player.moveStrafing == 0)
			yawDelta = signum(yawDelta) * clamp(2 * abs(yawDelta), 0F,
			                                    abs(tiltYaw));
		tiltYaw = clamp(
		  tiltYaw + yawDelta * YAW_SENS_PRESCALE * ClientConfig.yaw_sens,
		  -Config.tilt_range_yaw, Config.tilt_range_yaw);
		
		// Update tilt
		data.setTiltPitch(tiltPitch);
		data.setTiltRoll(tiltRoll);
		data.setTiltYaw(tiltYaw);
	}
}
