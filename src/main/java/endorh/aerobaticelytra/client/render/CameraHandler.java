package endorh.aerobaticelytra.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.EntityViewRenderEvent.FOVModifier;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class CameraHandler {
	public static double lastFOV = 0F;
	public static float lastRoll = 0F;
	public static final Logger LOGGER = LogManager.getLogger();
	
	/** Apply aerobatic camera roll */
	@SubscribeEvent
	public static void onCameraSetup(final CameraSetup event) {
		ActiveRenderInfo info = event.getInfo();
		Entity entity = info.getRenderViewEntity();
		if (entity instanceof ClientPlayerEntity) {
			ClientPlayerEntity player = (ClientPlayerEntity)entity;
			IAerobaticData data = getAerobaticDataOrDefault(player);
			
			GameSettings gameSettings = Minecraft.getInstance().gameSettings;
			int i = gameSettings.getPointOfView() == PointOfView.THIRD_PERSON_FRONT? -1 : 1;
			if (data.isFlying()) {
				lastRoll = data.getRotationRoll();
				event.setRoll(lastRoll * i);
				
				// Prevent wrong interpolation of arm render offsets when flying
				player.renderArmYaw = player.prevRenderArmYaw = player.rotationYaw;
				player.renderArmPitch = player.prevRenderArmPitch = player.rotationPitch;
			} else {
				if (lastRoll != 0F) {
					lastRoll = lastRoll > 180F ? 360F - (360F - lastRoll) * 0.75F : lastRoll * 0.75F;
					if (lastRoll < 0.0001F || lastRoll > 359.9999F)
						lastRoll = 0F;
					event.setRoll(lastRoll * i);
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
		PlayerEntity player = mc.player;
		if (AerobaticFlight.isAerobaticFlying(player)) {
			cameraOffset = true;
			if (event.getHand() == Hand.MAIN_HAND) {
				IAerobaticData data = getAerobaticDataOrDefault(player);
				lastPitchOffset = MathHelper.lerp(
				  0.1F, lastPitchOffset,
				  data.getTiltPitch() / Config.aerobatic.tilt.range_pitch * 3F);
				lastRollOffset = MathHelper.lerp(
				  0.1F, lastRollOffset,
				  data.getTiltRoll() / Config.aerobatic.tilt.range_roll * 5F);
				lastYawOffset = MathHelper.lerp(
				  0.1F, lastYawOffset,
				  data.getTiltYaw() / Config.aerobatic.tilt.range_yaw * -1.5F);
			}
			final MatrixStack mStack = event.getMatrixStack();
			mStack.rotate(Vector3f.XP.rotationDegrees(lastPitchOffset));
			mStack.rotate(Vector3f.YP.rotationDegrees(lastYawOffset));
			mStack.rotate(Vector3f.ZP.rotationDegrees(lastRollOffset));
		} else if (cameraOffset) {
			cameraOffset = false;
			lastPitchOffset = lastRollOffset = lastYawOffset = 0F;
		}
	}
	
	/**
	 * Apply flight FOV
	 */
	@SubscribeEvent
	public static void onFovModifier(final FOVModifier event) {
		ActiveRenderInfo info = event.getInfo();
		Entity entity = info.getRenderViewEntity();
		if (entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity)entity;
			IAerobaticData data = getAerobaticDataOrDefault(player);
			final double fov = event.getFOV();
			double newFOV = 0D;
			if (data.isFlying()) {
				final double f = Math.min(1D, data.ticksFlying() / 4D);
				final double p = MathHelper.abs(data.getPropulsionStrength()) / Config.aerobatic.propulsion.span * 10;
				final double b = data.isBoosted()? 15 : 0;
				newFOV = f * (p + b) * visual.fov_effect_strength;
			}
			lastFOV = (lastFOV * 3 + newFOV) / 4;
			event.setFOV(fov + lastFOV);
		}
	}
}
