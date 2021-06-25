package dnj.aerobatic_elytra.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.capability.IAerobaticData;
import dnj.aerobatic_elytra.common.config.Const;
import dnj.endor8util.math.Interpolator;
import dnj.flight_core.events.ApplyRotationsRenderPlayerEvent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static dnj.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static net.minecraft.util.math.MathHelper.lerp;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class PlayerRendererHandler {
	/**
	 * Prevent the head from being wrongly interpolated on backflips/frontflips
	 */
	@SubscribeEvent
	public static void onRenderPlayerEvent(RenderPlayerEvent.Pre event) {
		PlayerEntity player = event.getPlayer();
		if (AerobaticElytraLogic.shouldAerobaticFly(player)) {
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
		if (AerobaticElytraLogic.shouldAerobaticFly(event.player)) {
			event.setCanceled(true);
			PlayerEntity player = event.player;
			IAerobaticData data = getAerobaticDataOrDefault(player);
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
