package endorh.aerobaticelytra.client.render;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.config.ClientConfig.FlightBarDisplay;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.client.render.overlay.AerobaticCrosshairOverlay;
import endorh.aerobaticelytra.client.render.overlay.AerobaticDebugCrosshairOverlay;
import endorh.aerobaticelytra.client.render.overlay.FlightBarOverlay;
import endorh.aerobaticelytra.client.render.overlay.FlightModeToastOverlay;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class AerobaticOverlays {
	public static AerobaticCrosshairOverlay AEROBATIC_CROSSHAIR;
	public static AerobaticDebugCrosshairOverlay AEROBATIC_DEBUG_CROSSHAIR;
	public static FlightBarOverlay FLIGHT_BAR;
	public static FlightModeToastOverlay MODE_TOAST_OVERLAY;
	
	private static boolean rotatingDebugCrosshair = false;
	private static boolean showingCrosshair = true;
	private static boolean showingFlightBar = true;
	private static boolean showingExperienceBar = true;
	
	public static void showModeToastIfRelevant(Player player, IFlightMode mode) {
		if (AerobaticElytraLogic.hasAerobaticElytra(player))
			showModeToast(mode);
	}
	
	public static void showModeToast(IFlightMode modeIn) {
		MODE_TOAST_OVERLAY.showModeToast(modeIn);
	}
	
	/**
	 * Enable and disable overlays
	 */
	@SubscribeEvent public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
		if (event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
			rotatingDebugCrosshair = false;
			boolean showCrosshair = false;
			if (visual.flight_crosshair) {
				Minecraft mc = Minecraft.getInstance();
				Player pl = mc.player;
				assert pl != null;
				IAerobaticData data = getAerobaticDataOrDefault(pl);
				float roll = data.getRotationRoll();
				if (AerobaticFlight.isAerobaticFlying(pl) || Float.compare(roll, 0F) != 0) {
					Options opt = mc.options;
					assert mc.gameMode != null;
					if (opt.getCameraType().isFirstPerson()
					    && mc.gameMode.getPlayerMode() != GameType.SPECTATOR
					    && !opt.hideGui
					) {
						if (opt.renderDebug && !pl.isReducedDebugInfo() &&
						    !opt.reducedDebugInfo().get()) {
							rotatingDebugCrosshair = true;
						} else showCrosshair = true;
					}
				}
			}
			if (showCrosshair != showingCrosshair || showCrosshair)
				showingCrosshair = showCrosshair;
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
				showingExperienceBar = false;
			} else if (showFlightBar != showingFlightBar) {
				showingExperienceBar = true;
			}
			if (showFlightBar != showingFlightBar)
				showingFlightBar = showFlightBar;
		} else if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
			event.setCanceled(showingCrosshair || rotatingDebugCrosshair);
		} else if (event.getOverlay().overlay() == AEROBATIC_DEBUG_CROSSHAIR) {
			event.setCanceled(!rotatingDebugCrosshair);
		} else if (event.getOverlay().overlay() == AEROBATIC_CROSSHAIR) {
			event.setCanceled(!showingCrosshair);
		} else if (event.getOverlay().overlay() == FLIGHT_BAR) {
			event.setCanceled(!showingFlightBar);
		} else if (event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
			event.setCanceled(!showingExperienceBar);
		}
	}
	
	@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
	public static class Registrar {
		@SubscribeEvent public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
			event.registerAboveAll(
			  "crosshair", AEROBATIC_CROSSHAIR = new AerobaticCrosshairOverlay());
			event.registerAboveAll(
			  "debug_crosshair", AEROBATIC_DEBUG_CROSSHAIR = new AerobaticDebugCrosshairOverlay());
			event.registerBelow(
			  new ResourceLocation("experience_bar"), "flight_bar", FLIGHT_BAR = new FlightBarOverlay());
			event.registerAboveAll(
			  "flight_mode_toast", MODE_TOAST_OVERLAY = new FlightModeToastOverlay());
		}
	}
}
