package endorh.aerobatic_elytra.client.render;


import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.capability.IAerobaticData;
import endorh.aerobatic_elytra.common.flight.AerobaticFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static java.lang.System.currentTimeMillis;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class FlightCameraSmoother {
	private static final Logger LOGGER = LogManager.getLogger();
	
	/**
	 * Camera rotation needs to be updated every frame for the client player.
	 */
	@SubscribeEvent
	public static void onRenderTick(final RenderTickEvent event) {
		if (event.phase == Phase.START) {
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity player = mc.player;
			if (player == null) return;
			IAerobaticData data = getAerobaticDataOrDefault(player);
			if (data.isFlying()) {
				if (mc.isGamePaused()) {
					data.setLastRotationTime(currentTimeMillis() / 1000D);
					LOGGER.debug("Paused");
				} else AerobaticFlight.applyRotationAcceleration(player);
			}
		}
	}
}
