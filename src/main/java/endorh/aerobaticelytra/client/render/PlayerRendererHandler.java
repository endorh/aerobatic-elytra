package endorh.aerobaticelytra.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Const;
import endorh.util.math.Interpolator;
import endorh.flight_core.events.ApplyRotationsRenderPlayerEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.flight.AerobaticFlight.isAerobaticFlying;
import static net.minecraft.util.math.MathHelper.lerp;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class PlayerRendererHandler {
	/**
	 * Prevent the head from being wrongly interpolated on backflips/frontflips
	 */
	@SubscribeEvent
	public static void onRenderPlayerEvent(RenderPlayerEvent.Pre event) {
		PlayerEntity player = event.getPlayer();
		if (isAerobaticFlying(player)) {
			player.renderYawOffset = player.rotationYaw;
			player.prevRenderYawOffset = player.rotationYaw;
			player.rotationYawHead = player.rotationYaw;
			player.prevRotationYawHead = player.rotationYaw;
		}
	}
	
	/**
	 * Rotate the player model when flying
	 */
	@SubscribeEvent
	public static void onApplyRotationsRenderPlayerEvent(
	  ApplyRotationsRenderPlayerEvent event
	) {
		PlayerEntity player = event.player;
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.isFlying()) {
			event.setCanceled(true);
			MatrixStack mStack = event.matrixStack;
			float t = (player.getTicksElytraFlying() + event.partialTicks) / Const.TAKEOFF_ANIMATION_LENGTH_TICKS;
			float yaw = (180F - player.rotationYaw);
			float pitch = (-90F - player.rotationPitch);
			if (t < 1F) {
				// Smooth lift off
				float i = Interpolator.quadInOut(t);
				yaw = lerp(i, (180F - player.prevRotationYaw), yaw);
				pitch = lerp(i, 0F, pitch);
				// No need to smooth the roll since it starts being 0
			}
			
			mStack.rotate(Vector3f.YP.rotationDegrees(yaw));
			mStack.rotate(Vector3f.XP.rotationDegrees(pitch));
			mStack.rotate(Vector3f.YP.rotationDegrees(
			  data.getRotationRoll() + data.getTiltRoll() * Const.TILT_ROLL_RENDER_OFFSET));
			mStack.rotate(Vector3f.XP.rotationDegrees(- data.getTiltPitch() * Const.TILT_PITCH_RENDER_OFFSET));
			mStack.rotate(Vector3f.ZP.rotationDegrees(data.getTiltYaw() * Const.TILT_YAW_RENDER_OFFSET));
			
			// Keep the easter egg
			String s = TextFormatting.getTextWithoutFormattingCodes(player.getName().getString());
			//noinspection SpellCheckingInspection
			if (("Dinnerbone".equals(s) || "Grumm".equals(s)) && player.isWearing(PlayerModelPart.CAPE)) {
				mStack.translate(0D, (double)player.getHeight() + 0.1F, 0D);
				mStack.rotate(Vector3f.ZP.rotationDegrees(180F));
			}
		}
	}
}
