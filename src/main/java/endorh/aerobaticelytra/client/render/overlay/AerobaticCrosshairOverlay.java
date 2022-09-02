package endorh.aerobaticelytra.client.render.overlay;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.aerobaticelytra.common.capability.AerobaticDataCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config.aerobatic.tilt;
import endorh.aerobaticelytra.common.config.Const;
import endorh.util.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static endorh.aerobaticelytra.client.AerobaticElytraResources.FLIGHT_GUI_ICONS_LOCATION;

public class AerobaticCrosshairOverlay implements IGuiOverlay {
	private static final Vec3f ZP = Vec3f.ZP.get();
	
	@Override public void render(
	  ForgeGui gui, PoseStack mStack, float partialTicks, int width, int height
	) {
		RenderSystem.setShaderTexture(0, FLIGHT_GUI_ICONS_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
		  GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
		  GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
		  GlStateManager.SourceFactor.ONE,
		  GlStateManager.DestFactor.ZERO);
		
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;
		
		Window win = Minecraft.getInstance().getWindow();
		int winW = win.getGuiScaledWidth();
		int winH = win.getGuiScaledHeight();
		
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH;
		int tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		int cS = Const.FLIGHT_GUI_CROSSHAIR_SIZE;
		
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		
		float scaledPitch = data.getTiltPitch() / tilt.range_pitch * Const.CROSSHAIR_PITCH_RANGE_PX;
		float scaledRoll = data.getTiltRoll() / tilt.range_roll * Const.CROSSHAIR_ROLL_RANGE_DEG;
		// Underwater yaw tilt can exceed the range
		float scaledYaw = -Mth.clamp(data.getTiltYaw(), -tilt.range_yaw, tilt.range_yaw) / tilt.range_yaw * Const.CROSSHAIR_YAW_RANGE_PX;
		
		mStack.pushPose(); {
			// Base
			GuiComponent.blit(mStack, (winW - cS) / 2, (winH - cS) / 2, 0, 0, cS, cS, tW, tH);
			// Pitch
			mStack.pushPose(); {
				mStack.translate(0D, scaledPitch, 0D);
				GuiComponent.blit(mStack, (winW - cS) / 2, (winH - cS) / 2, cS, 0, cS, cS, tW, tH);
			} mStack.popPose();
			
			// Yaw
			mStack.pushPose(); {
				mStack.translate(scaledYaw, 0F, 0F);
				GuiComponent.blit(mStack, (winW - cS) / 2, (winH - cS) / 2, 0, cS, cS, cS, tW, tH);
			} mStack.popPose();
			
			// Roll
			mStack.pushPose(); {
				mStack.translate(winW / 2F, winH / 2F, 0F);
				mStack.mulPose(ZP.rotationDegrees(scaledRoll));
				mStack.translate(-(winW / 2F), -(winH / 2F), 0F);
				// Rotated crosshair
				GuiComponent.blit(mStack, (winW - cS) / 2, (winH - cS) / 2, cS, cS, cS, cS, tW, tH);
			} mStack.popPose();
		} mStack.popPose();
		
		RenderSystem.defaultBlendFunc();
	}
}
