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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class AerobaticOverlays {
	public static final AerobaticCrosshairOverlay AEROBATIC_CROSSHAIR = new AerobaticCrosshairOverlay();
	public static final AerobaticDebugCrosshairOverlay AEROBATIC_DEBUG_CROSSHAIR = new AerobaticDebugCrosshairOverlay();
	public static final FlightBarOverlay FLIGHT_BAR = new FlightBarOverlay();
	public static final FlightModeToastOverlay MODE_TOAST_OVERLAY = new FlightModeToastOverlay();
	
	private static boolean showingCrosshair = true;
	private static boolean showingFlightBar = true;
	private static boolean rotatingDebugCrosshair = false;
	
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
	@SubscribeEvent
	public static void onRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.ALL) {
			boolean rotateDebugCrosshair = false;
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
						if (opt.renderDebug && !pl.isReducedDebugInfo() && !opt.reducedDebugInfo) {
							rotateDebugCrosshair = true;
						} else showCrosshair = true;
					}
				}
			}
			boolean showDefault = !rotateDebugCrosshair && !showCrosshair;
			if (rotateDebugCrosshair != rotatingDebugCrosshair) {
				OverlayRegistry.enableOverlay(AEROBATIC_DEBUG_CROSSHAIR, rotateDebugCrosshair);
				OverlayRegistry.enableOverlay(ForgeIngameGui.CROSSHAIR_ELEMENT, showDefault);
				rotatingDebugCrosshair = rotateDebugCrosshair;
			}
			if (showCrosshair != showingCrosshair) {
				OverlayRegistry.enableOverlay(AEROBATIC_CROSSHAIR, showCrosshair);
				OverlayRegistry.enableOverlay(ForgeIngameGui.CROSSHAIR_ELEMENT, showDefault);
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
					suppressExperienceBar = visual.flight_bar == ClientConfig.FlightBarDisplay.REPLACE_XP;
				}
			}
			
			if (showFlightBar != showingFlightBar) {
				OverlayRegistry.enableOverlay(FLIGHT_BAR, showFlightBar);
				OverlayRegistry.enableOverlay(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, !showFlightBar || !suppressExperienceBar);
				showingFlightBar = showFlightBar;
			}
		}
	}
	
	@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
	public static class Registrar {
		@SubscribeEvent public static void onClientSetup(FMLClientSetupEvent event) {
			OverlayRegistry.registerOverlayAbove(ForgeIngameGui.CROSSHAIR_ELEMENT, AerobaticCrosshairOverlay.NAME, AEROBATIC_CROSSHAIR);
			OverlayRegistry.registerOverlayAbove(ForgeIngameGui.CROSSHAIR_ELEMENT, AerobaticDebugCrosshairOverlay.NAME, AEROBATIC_DEBUG_CROSSHAIR);
			OverlayRegistry.registerOverlayBelow(ForgeIngameGui.EXPERIENCE_BAR_ELEMENT, FlightBarOverlay.NAME, FLIGHT_BAR);
			OverlayRegistry.registerOverlayTop(FlightModeToastOverlay.NAME, MODE_TOAST_OVERLAY);
			OverlayRegistry.enableOverlay(AEROBATIC_CROSSHAIR, false);
			OverlayRegistry.enableOverlay(AEROBATIC_DEBUG_CROSSHAIR, false);
			OverlayRegistry.enableOverlay(FLIGHT_BAR, false);
		}
	}
}
