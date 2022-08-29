package endorh.aerobaticelytra.client.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.config.ClientConfig.FlightBarDisplay;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.client.render.overlay.AerobaticCrosshairOverlay;
import endorh.aerobaticelytra.client.render.overlay.FlightBarOverlay;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static java.lang.Math.round;
import static java.lang.System.currentTimeMillis;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class AerobaticOverlays {
	public static final IIngameOverlay AEROBATIC_CROSSHAIR = new AerobaticCrosshairOverlay();
	public static final IIngameOverlay FLIGHT_BAR = new FlightBarOverlay();
	
	private static long toastEnd = 0L;
	private static long remainingToastTime = 0L;
	private static IFlightMode mode = null;
	private static boolean rotatingDebugCrosshair = false;
	private static boolean awaitingDebugCrosshair = false;
	private static boolean showingCrosshair = true;
	private static boolean showingFlightBar = true;
	
	public static void showModeToastIfRelevant(Player player, IFlightMode mode) {
		if (AerobaticElytraLogic.hasAerobaticElytra(player))
			showModeToast(mode);
	}
	
	public static void showModeToast(IFlightMode modeIn) {
		mode = modeIn;
		toastEnd = currentTimeMillis() + visual.mode_toast_length_ms;
		remainingToastTime = visual.mode_toast_length_ms;
	}
	
	@SubscribeEvent
	public static void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event) {
		if (event.getType() == ElementType.ALL && remainingToastTime > 0) {
			Minecraft mc = Minecraft.getInstance();
			Player pl = mc.player;
			assert pl != null;
			float alpha = remainingToastTime / (float) visual.mode_toast_length_ms;
			renderToast(mode, alpha, event.getMatrixStack(), event.getWindow());
			final long t = currentTimeMillis();
			remainingToastTime = toastEnd - t;
		}
	}
	
	public static void renderToast(
	  IFlightMode mode, float alpha, PoseStack mStack, Window win
	) {
		RenderSystem.setShaderTexture(0, mode.getToastIconLocation());
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
		int winW = win.getGuiScaledWidth();
		int winH = win.getGuiScaledHeight();
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH, tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		int iW = Const.FLIGHT_MODE_TOAST_WIDTH, iH = Const.FLIGHT_MODE_TOAST_HEIGHT;
		int u = mode.getToastIconU(), v = mode.getToastIconV();
		if (u != -1 && v != -1) {
			int x = round((winW - iW) * visual.mode_toast_x_fraction);
			int y = round((winH - iH) * visual.mode_toast_y_fraction);
			GuiComponent.blit(mStack, x, y, u, v, iW, iH, tW, tH);
		}
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.disableBlend();
	}
	
	/**
	 * Enable and disable overlays.
	 */
	@SubscribeEvent
	public static void onRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.ALL) {
			rotatingDebugCrosshair = false;
			boolean showCrosshair = false;
			if (visual.flight_crosshair) {
				Minecraft mc = Minecraft.getInstance();
				Player pl = mc.player;
				assert pl != null;
				if (AerobaticFlight.isAerobaticFlying(pl)) {
					Options opt = mc.options;
					assert mc.gameMode != null;
					if (opt.getCameraType().isFirstPerson()
					    && mc.gameMode.getPlayerMode() != GameType.SPECTATOR
					    && !opt.hideGui) {
						if (opt.renderDebug && !pl.isReducedDebugInfo() && !opt.reducedDebugInfo) {
							rotatingDebugCrosshair = true;
						} else {
							showCrosshair = true;
						}
					}
				}
			}
			if (showCrosshair != showingCrosshair || showCrosshair) {
				OverlayRegistry.enableOverlay(AEROBATIC_CROSSHAIR, showCrosshair);
				OverlayRegistry.enableOverlay(ForgeIngameGui.CROSSHAIR_ELEMENT, !showCrosshair);
				showingCrosshair = showCrosshair;
			}
			boolean showFlightBar = false;
			boolean suppressExperienceBar = false;
			if (visual.flight_bar != FlightBarDisplay.HIDE) {
				Minecraft mc = Minecraft.getInstance();
				Player player = mc.player;
				assert player != null;
				if (AerobaticFlight.isAerobaticFlying(player)) {
					showFlightBar = true;
					if (visual.flight_bar == ClientConfig.FlightBarDisplay.REPLACE_XP) {
						suppressExperienceBar = true;
					}
				}
			}
			if (suppressExperienceBar) {
				OverlayRegistry.enableOverlay(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, false);
			} else if (showFlightBar != showingFlightBar) {
				OverlayRegistry.enableOverlay(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, false);
			}
			if (showFlightBar != showingFlightBar) {
				showingFlightBar = showFlightBar;
				OverlayRegistry.enableOverlay(FLIGHT_BAR, showFlightBar);
			}
		}
	}
	
	/**
	 * Pushes the matrix stack to rotate the crosshair<br>
	 * {@link AerobaticOverlays#onPostDebugCrosshair} must be called after this on the same frame.
	 */
	@SubscribeEvent
	public static void onPreDebugCrosshair(RenderGameOverlayEvent.PreLayer event) {
		if (rotatingDebugCrosshair && event.getOverlay() == ForgeIngameGui.CROSSHAIR_ELEMENT) {
			Window win = Minecraft.getInstance().getWindow();
			int winW = win.getGuiScaledWidth();
			int winH = win.getGuiScaledHeight();
			
			PoseStack pStack = RenderSystem.getModelViewStack();
			pStack.pushPose(); {
				pStack.translate(winW / 2F, winH / 2F, 0F);
				pStack.mulPose(Quaternion.fromXYZDegrees(new Vector3f(0F, 0F, -CameraHandler.lastRoll)));
				pStack.translate(-(winW / 2F), -(winH / 2F), 0F);
			}
			// Warning! Ensure onPostDebugCrosshair gets called,
			//          or the matrix stack will break
			awaitingDebugCrosshair = true;
		}
	}
	
	@SubscribeEvent
	public static void onPostDebugCrosshair(RenderGameOverlayEvent.PostLayer event) {
		if (awaitingDebugCrosshair && event.getOverlay() == ForgeIngameGui.CROSSHAIR_ELEMENT) {
			RenderSystem.getModelViewStack().popPose();
			awaitingDebugCrosshair = false;
		}
	}
	
	@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
	public static class Registrar {
		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event) {
			OverlayRegistry.registerOverlayAbove(ForgeIngameGui.CROSSHAIR_ELEMENT, AerobaticCrosshairOverlay.NAME, AEROBATIC_CROSSHAIR);
			OverlayRegistry.registerOverlayAbove(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, FlightBarOverlay.NAME, FLIGHT_BAR);
		}
	}
}
