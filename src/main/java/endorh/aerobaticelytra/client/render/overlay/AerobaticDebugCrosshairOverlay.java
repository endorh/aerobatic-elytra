package endorh.aerobaticelytra.client.render.overlay;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import endorh.aerobaticelytra.client.render.CameraHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class AerobaticDebugCrosshairOverlay implements IGuiOverlay {
	@Override public void render(
		ForgeGui gui, GuiGraphics gg, float partialTick, int width, int height
	) {
		renderDebugCrosshair(gg);
	}
	
	public static void renderDebugCrosshair(GuiGraphics gg) {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		Window win = mc.getWindow();
		int winW = win.getGuiScaledWidth();
		int winH = win.getGuiScaledHeight();
		PoseStack pStack = gg.pose();
		pStack.pushPose(); {
			pStack.translate(winW / 2D, winH / 2D, -90D);
			pStack.mulPose(Axis.ZN.rotationDegrees(CameraHandler.lastRoll));
			pStack.mulPose(Axis.XN.rotationDegrees(camera.getXRot()));
			pStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot()));
			pStack.scale(-1F, -1F, -1F);
			RenderSystem.applyModelViewMatrix();
			RenderSystem.renderCrosshair(10);
		} pStack.popPose();
		RenderSystem.applyModelViewMatrix();
	}
}
