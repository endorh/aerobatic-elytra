package endorh.aerobaticelytra.client.render.overlay;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.aerobaticelytra.common.capability.AerobaticDataCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config.aerobatic.tilt;
import endorh.aerobaticelytra.common.config.Const;
import endorh.util.animation.ToggleAnimator;
import endorh.util.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static endorh.aerobaticelytra.client.AerobaticElytraResources.FLIGHT_GUI_ICONS_LOCATION;
import static java.lang.Math.*;

public class AerobaticCrosshairOverlay implements IGuiOverlay {
	private static final Vec3f XP = Vec3f.XP.get();
	private static final Vec3f YP = Vec3f.YP.get();
	private static final Vec3f ZP = Vec3f.ZP.get();
	private final ToggleAnimator lookAroundAnimator = ToggleAnimator.quadOut(100L);
	private final ToggleAnimator rectifyAnimator = ToggleAnimator.quadOut(100L);
	
	public AerobaticCrosshairOverlay() {
		lookAroundAnimator.setRange(-45F, 0);
		rectifyAnimator.setRange(1.2F, 1);
	}
	
	@Override public void render(
	  ForgeGui gui, GuiGraphics gg, float partialTicks, int width, int height
	) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		Window win = mc.getWindow();

		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(
		  SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR,
		  SourceFactor.SRC_ALPHA, DestFactor.ZERO);
		
		renderCrosshair(gg, win, data, partialTicks);
		
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
	}
	
	private void renderCrosshair(
		GuiGraphics gg, Window win, IAerobaticData data, float partialTicks
	) {
		ResourceLocation t = FLIGHT_GUI_ICONS_LOCATION;

		int winW = win.getGuiScaledWidth();
		int winH = win.getGuiScaledHeight();
		
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH;
		int tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		int cS = Const.FLIGHT_GUI_CROSSHAIR_SIZE;
		
		float scaledPitch = data.getTiltPitch() / tilt.range_pitch * Const.CROSSHAIR_PITCH_RANGE_PX;
		float scaledRoll = data.getTiltRoll() / tilt.range_roll * Const.CROSSHAIR_ROLL_RANGE_DEG;
		// Underwater yaw tilt can exceed the range
		float scaledYaw = -Mth.clamp(data.getTiltYaw(), -tilt.range_yaw, tilt.range_yaw) / tilt.range_yaw * Const.CROSSHAIR_YAW_RANGE_PX;
		
		float lookYaw = Mth.lerp(partialTicks, data.getPrevLookAroundYaw(), data.getLookAroundYaw());
		float lookPitch = Mth.lerp(partialTicks, data.getPrevLookAroundPitch(), data.getLookAroundPitch());
		boolean isLookingAround = lookYaw != 0 || lookPitch != 0 || data.isLookingAround();
		if ((lookAroundAnimator.getTarget() == 1) != isLookingAround)
			lookAroundAnimator.setTarget(isLookingAround);
		if ((rectifyAnimator.getTarget() == 1) != data.isJumping())
			rectifyAnimator.setTarget(data.isJumping());
		boolean animatingLook = lookAroundAnimator.isInProgress();
		
		float cX = winW / 2F;
		float cY = winH / 2F;
		int crossX = (winW - cS) / 2;
		int crossY = (winH - cS) / 2;
		PoseStack pStack = gg.pose();
		pStack.pushPose(); {
			if (animatingLook) {
				pStack.pushPose(); {
					pStack.translate(cX, cY, 0);
					pStack.mulPose(ZP.rotationDegrees(lookAroundAnimator.getProgress()));
					pStack.translate(-cX, -cY, 0);
					gg.blit(t, crossX, crossY, 2 * cS, 0, cS, cS, tW, tH);
				} pStack.popPose();
			}
			if (isLookingAround) {
				float rotDiag = (float) sqrt(lookPitch * lookPitch + lookYaw * lookYaw);
				if (!animatingLook) gg.blit(
				  t, crossX, crossY, 2 * cS, data.isLookAroundPersistent()? cS : 0,
				  cS, cS, tW, tH);
				if (rotDiag > 1E-4F) {
					// The rotation offset must be relative to the screen size
					float diag = (float) sqrt(winW * winW + winH * winH);
					float rotationOffset = diag / 8F;
					// The rotation is scaled down for small rotations, to create the
					//   illusion that the rotated crosshair points into the flight direction
					//   relative to the player's POV, rather than from the aim crosshair
					// Ideally, the rotated crosshair would be rendered in the surface of a
					//   sphere centered around the aim crosshair, which would be placed some
					//   distance in front of the player, in the spot determined by a ray
					//   cast from the aim crosshair to the flight direction in the infinity,
					//   but this is good enough for most screen sizes and FOV values
					float rotationStrength = Mth.clamp(1 - (float) 1 / rotDiag, 0, 1);
					// Rotate the crosshair back around itself to make it more readable
					//   at angles near 90 degrees
					// The interpolation function used for the counter rotation is
					//   ∛(x / (1 + x⁴)), which produces a nice transition between angles
					//   before and beyond 90 degrees
					float relRot = (rotDiag - 90) / 5F;
					float counterRotationStrength = 0.4F * (float) cbrt(
					  relRot / (1 + (float) pow(relRot, 4)));
					pStack.translate(cX, cY, -rotationOffset);
					pStack.mulPose(XP.rotationDegrees(lookPitch * rotationStrength));
					pStack.mulPose(YP.rotationDegrees(lookYaw * rotationStrength));
					pStack.translate(0, 0, rotationOffset);
					pStack.mulPose(XP.rotationDegrees(lookPitch * counterRotationStrength));
					pStack.mulPose(YP.rotationDegrees(lookYaw * counterRotationStrength));
					pStack.translate(-cX, -cY, 0);
				}
			}
			
			// Base
			gg.blit(t, crossX, crossY, 0, 0, cS, cS, tW, tH);
			// Pitch
			pStack.pushPose(); {
				pStack.translate(0D, scaledPitch, 0D);
				gg.blit(t, crossX, crossY, cS, 0, cS, cS, tW, tH);
			} pStack.popPose();
			
			// Yaw
			pStack.pushPose(); {
				pStack.translate(scaledYaw, 0, 0);
				gg.blit(t, crossX, crossY, 0, cS, cS, cS, tW, tH);
			} pStack.popPose();
			
			// Roll
			pStack.pushPose(); {
				pStack.translate(cX, cY, 0);
				pStack.mulPose(ZP.rotationDegrees(scaledRoll));
				pStack.translate(-cX, -cY, 0);
				// Rotated crosshair
				gg.blit(t, crossX, crossY, cS, cS, cS, cS, tW, tH);
			} pStack.popPose();
			
			// Rectification trigger
			if (rectifyAnimator.isInProgress()) {
				RenderSystem.setShaderColor(1, 1, 1, rectifyAnimator.getUnitProgress());
				pStack.pushPose(); {
					float scale = rectifyAnimator.getProgress();
					pStack.translate(cX, cY, 0);
					pStack.scale(scale, scale, 1);
					pStack.translate(-cX, -cY, 0);
					gg.blit(t, crossX, crossY, 3 * cS, 0, cS, cS, tW, tH);
				} pStack.popPose();
				RenderSystem.setShaderColor(1, 1, 1, 1);
			} else if (data.isJumping())
				gg.blit(t, crossX, crossY, 3 * cS, 0, cS, cS, tW, tH);
		} pStack.popPose();
	}
}
