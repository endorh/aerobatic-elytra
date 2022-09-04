package endorh.aerobaticelytra.client.render.overlay;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.render.CameraHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;

public class AerobaticDebugCrosshairOverlay implements IIngameOverlay {
	public static final String NAME = AerobaticElytra.MOD_ID + ":debug_crosshair";
	
	@Override public void render(
	  ForgeIngameGui gui, PoseStack poseStack, float partialTick, int width, int height
	) {
		renderDebugCrosshair();
	}
	
	public static void renderDebugCrosshair() {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		Window win = mc.getWindow();
		int winW = win.getGuiScaledWidth();
		int winH = win.getGuiScaledHeight();
		PoseStack pStack = RenderSystem.getModelViewStack();
		pStack.pushPose(); {
			pStack.translate(winW / 2D, winH / 2D, -90D);
			pStack.mulPose(Vector3f.ZN.rotationDegrees(CameraHandler.lastRoll));
			pStack.mulPose(Vector3f.XN.rotationDegrees(camera.getXRot()));
			pStack.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot()));
			pStack.scale(-1F, -1F, -1F);
			RenderSystem.applyModelViewMatrix();
			RenderSystem.renderCrosshair(10);
		} pStack.popPose();
		RenderSystem.applyModelViewMatrix();
	}
}
