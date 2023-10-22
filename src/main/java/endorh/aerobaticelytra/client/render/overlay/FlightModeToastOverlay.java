package endorh.aerobaticelytra.client.render.overlay;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import static java.lang.Math.round;
import static java.lang.System.currentTimeMillis;

public class FlightModeToastOverlay implements IGuiOverlay {
	private long toastEnd = 0L;
	private long remainingToastTime = 0L;
	private IFlightMode mode;
	
	@Override public void render(
		ForgeGui gui, GuiGraphics gg, float partialTick, int width, int height
	) {
		if (remainingToastTime > 0) {
			float alpha = remainingToastTime / (float) visual.mode_toast_length_ms;
			ResourceLocation t = mode.getToastIconLocation();
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
			Window win = Minecraft.getInstance().getWindow();
			int winW = win.getGuiScaledWidth();
			int winH = win.getGuiScaledHeight();
			int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH, tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
			int iW = Const.FLIGHT_MODE_TOAST_WIDTH, iH = Const.FLIGHT_MODE_TOAST_HEIGHT;
			int u = mode.getToastIconU(), v = mode.getToastIconV();
			if (u != -1 && v != -1) {
				int x = round((winW - iW) * visual.mode_toast_x_fraction);
				int y = round((winH - iH) * visual.mode_toast_y_fraction);
				gg.blit(t, x, y, u, v, iW, iH, tW, tH);
			}
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			RenderSystem.disableBlend();
			final long tt = currentTimeMillis();
			remainingToastTime = toastEnd - tt;
		}
	}
	
	public void showModeToast(IFlightMode modeIn) {
		mode = modeIn;
		toastEnd = currentTimeMillis() + visual.mode_toast_length_ms;
		remainingToastTime = visual.mode_toast_length_ms;
	}
}
