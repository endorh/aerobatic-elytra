package endorh.aerobaticelytra.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Const;
import endorh.flightcore.events.SetupRotationsRenderPlayerEvent;
import endorh.util.animation.Easing;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.flight.AerobaticFlight.isAerobaticFlying;
import static net.minecraft.util.Mth.lerp;

@EventBusSubscriber(value=Dist.CLIENT, modid=AerobaticElytra.MOD_ID)
public class PlayerRendererHandler {
	/**
	 * Prevent the head from being wrongly interpolated on backflips/frontflips
	 */
	@SubscribeEvent
	public static void onRenderPlayerEvent(RenderPlayerEvent.Pre event) {
		Player player = event.getPlayer();
		if (isAerobaticFlying(player)) {
			final float yRot = player.getYRot();
			player.yBodyRot = yRot;
			player.yBodyRotO = yRot;
			player.yHeadRot = yRot;
			player.yHeadRotO = yRot;
		}
	}
	
	/**
	 * Rotate the player model when flying
	 */
	@SubscribeEvent
	public static void onSetupRotationsRenderPlayerEvent(
	  SetupRotationsRenderPlayerEvent event
	) {
		Player player = event.player;
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.isFlying()) {
			event.setCanceled(true);
			PoseStack mStack = event.matrixStack;
			float t = (player.getFallFlyingTicks() + event.partialTicks) /
			          Const.TAKEOFF_ANIMATION_LENGTH_TICKS;
			float yaw = 180F - data.getRotationYaw();
			float pitch = -90F - data.getRotationPitch();
			if (t < 1F) {
				// Smooth lift off
				float i = Easing.quadInOut(t);
				// Standing pitch is 0
				pitch = lerp(i, 0F, pitch);
				// Only yaw affects standing players' rotation
				yaw = lerp(i, 180F - data.getPrevTickRotationYaw(), yaw);
				// No need to smooth the roll since it starts being 0
			}
			
			mStack.mulPose(Vector3f.YP.rotationDegrees(yaw));
			mStack.mulPose(Vector3f.XP.rotationDegrees(pitch));
			mStack.mulPose(Vector3f.YP.rotationDegrees(
			  data.getRotationRoll() + data.getTiltRoll() * Const.TILT_ROLL_RENDER_OFFSET));
			mStack.mulPose(Vector3f.XP.rotationDegrees(
			  -data.getTiltPitch() * Const.TILT_PITCH_RENDER_OFFSET));
			mStack.mulPose(Vector3f.ZP.rotationDegrees(
			  data.getTiltYaw() * Const.TILT_YAW_RENDER_OFFSET));
			
			// Keep the easter egg
			String s = ChatFormatting.stripFormatting(player.getName().getString());
			if (("Dinnerbone".equals(s) || "Grumm".equals(s)) &&
			    player.isModelPartShown(PlayerModelPart.CAPE)) {
				mStack.translate(0D, (double) player.getBbHeight() + 0.1F, 0D);
				mStack.mulPose(Vector3f.ZP.rotationDegrees(180F));
			}
		}
	}
}
