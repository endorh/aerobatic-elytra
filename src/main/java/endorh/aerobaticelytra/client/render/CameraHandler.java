package endorh.aerobaticelytra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.aerobatic.propulsion;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.EntityViewRenderEvent.FieldOfView;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static net.minecraft.util.Mth.abs;
import static net.minecraft.util.Mth.lerp;

@EventBusSubscriber(value=Dist.CLIENT, modid=AerobaticElytra.MOD_ID)
public class CameraHandler {
	public static double lastFOV = 0D;
	public static float lastRoll = 0F;
	public static final Logger LOGGER = LogManager.getLogger();
	
	/** Apply aerobatic camera roll */
	@SubscribeEvent
	public static void onCameraSetup(final CameraSetup event) {
		Camera cam = event.getCamera();
		Entity entity = cam.getEntity();
		if (entity instanceof LocalPlayer player) {
			IAerobaticData data = getAerobaticDataOrDefault(player);
			
			Options gameSettings = Minecraft.getInstance().options;
			int invertRoll = gameSettings.getCameraType() == CameraType.THIRD_PERSON_FRONT? -1 : 1;
			if (data.isFlying()) {
				lastRoll = data.getLookAroundRoll();
				event.setRoll(lastRoll * invertRoll);
				
				// Prevent wrong interpolation of arm render offsets when flying
				player.yBob = player.yBobO = player.getYRot();
				player.xBob = player.xBobO = player.getXRot();
			} else {
				if (lastRoll != 0F) {
					lastRoll = lastRoll > 180F? 360F - (360F - lastRoll) * 0.75F : lastRoll * 0.75F;
					if (lastRoll < 0.0001F || lastRoll > 359.9999F)
						lastRoll = 0F;
					event.setRoll(lastRoll * invertRoll);
				}
			}
		}
	}
	
	private static float
	  lastPitchOffset = 0F,
	  lastRollOffset = 0F,
	  lastYawOffset = 0F;
	private static boolean cameraOffset;
	
	@SubscribeEvent
	public static void onRenderHandEvent(RenderHandEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null)
			return;
		Player player = mc.player;
		if (AerobaticFlight.isAerobaticFlying(player)) {
			cameraOffset = true;
			if (event.getHand() == InteractionHand.MAIN_HAND) {
				IAerobaticData data = getAerobaticDataOrDefault(player);
				lastPitchOffset = lerp(
				  0.1F, lastPitchOffset,
				  data.getTiltPitch() / Config.aerobatic.tilt.range_pitch * 3F);
				lastRollOffset = lerp(
				  0.1F, lastRollOffset,
				  data.getTiltRoll() / Config.aerobatic.tilt.range_roll * 5F);
				lastYawOffset = lerp(
				  0.1F, lastYawOffset,
				  data.getTiltYaw() / Config.aerobatic.tilt.range_yaw * -1.5F);
			}
			final PoseStack mStack = event.getPoseStack();
			mStack.mulPose(Vector3f.XP.rotationDegrees(lastPitchOffset));
			mStack.mulPose(Vector3f.YP.rotationDegrees(lastYawOffset));
			mStack.mulPose(Vector3f.ZP.rotationDegrees(lastRollOffset));
		} else if (cameraOffset) {
			cameraOffset = false;
			lastPitchOffset = lastRollOffset = lastYawOffset = 0F;
		}
	}
	
	/**
	 * Apply flight FOV
	 */
	@SubscribeEvent
	public static void onFovModifier(final FieldOfView event) {
		Camera cam = event.getCamera();
		Entity entity = cam.getEntity();
		if (entity instanceof Player player) {
			IAerobaticData data = getAerobaticDataOrDefault(player);
			final double fov = event.getFOV();
			double newFOV = 0F;
			if (data.isFlying()) {
				final double f = Math.min(1D, (data.getTicksFlying() + event.getPartialTicks()) / 4D);
				final double p = abs(data.getPropulsionStrength()) / propulsion.span * 10D;
				final double b = data.isBoosted()? 15D : 0D;
				newFOV = f * (p + b) * visual.fov_effect_strength;
			}
			lastFOV = (lastFOV * 3 + newFOV) / 4;
			event.setFOV(fov + lastFOV);
		}
	}
}
