package dnj.aerobatic_elytra.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.client.config.ClientConfig;
import dnj.aerobatic_elytra.common.capability.IAerobaticData;
import dnj.aerobatic_elytra.common.config.Config;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.EntityViewRenderEvent.FOVModifier;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static dnj.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static dnj.aerobatic_elytra.common.flight.AerobaticFlight.isAerobaticFlying;
import static net.minecraft.util.math.MathHelper.abs;
import static net.minecraft.util.math.MathHelper.lerp;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class CameraHandler {
	private static double lastFOV = 0F;
	public static final Logger LOGGER = LogManager.getLogger();
	
	// TODO: Smooth roll and FOV changes (?)
	/** Apply aerobatic camera roll */
	@SubscribeEvent
	public static void onCameraSetup(final CameraSetup event) {
		ActiveRenderInfo info = event.getInfo();
		Entity entity = info.getRenderViewEntity();
		if (entity instanceof ClientPlayerEntity) {
			ClientPlayerEntity player = (ClientPlayerEntity)entity;
			IAerobaticData data = getAerobaticDataOrDefault(player);
			
			if (data.isFlying()) {
				GameSettings gameSettings = Minecraft.getInstance().gameSettings;
				int i = gameSettings.getPointOfView() == PointOfView.THIRD_PERSON_FRONT? -1 : 1;
				
				event.setRoll(data.getRotationRoll() * i);
				
				// Prevent wrong interpolation of arm render offsets when flying
				player.renderArmYaw = player.prevRenderArmYaw = player.rotationYaw;
				player.renderArmPitch = player.prevRenderArmPitch = player.rotationPitch;
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
		if (isAerobaticFlying(player)) {
			cameraOffset = true;
			if (event.getHand() == Hand.MAIN_HAND) {
				IAerobaticData data = getAerobaticDataOrDefault(player);
				lastPitchOffset = lerp(
				  0.1F, lastPitchOffset,
				  data.getTiltPitch() / Config.tilt_range_pitch * 3F);
				lastRollOffset = lerp(
				  0.1F, lastRollOffset,
				  data.getTiltRoll() / Config.tilt_range_roll * 5F);
				lastYawOffset = lerp(
				  0.1F, lastYawOffset,
				  data.getTiltYaw() / Config.tilt_range_yaw * -1.5F);
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
			if (data.isFlying()) {
				final double fov = event.getFOV();
				final double f = Math.min(1D, data.ticksFlying() / 4D);
				final double p = abs(data.getPropulsionStrength()) / Config.propulsion_range * 10;
				final double b = data.isBoosted()? 15 : 0;
				final double newFOV = f * (p + b) * ClientConfig.aerobatic_flight_fov_strength;
				lastFOV = (lastFOV * 3 + newFOV) / 4;
				event.setFOV(fov + lastFOV);
			}
		}
	}
}
