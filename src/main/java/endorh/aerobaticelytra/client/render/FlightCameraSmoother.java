package endorh.aerobaticelytra.client.render;


import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.RenderTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static java.lang.System.currentTimeMillis;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class FlightCameraSmoother {
	
	/**
	 * Camera rotation needs to be updated every frame for the client player.
	 */
	@SubscribeEvent
	public static void onRenderTick(final RenderTickEvent event) {
		if (event.phase == Phase.START) {
			Minecraft mc = Minecraft.getInstance();
			Player player = mc.player;
			if (player == null) return;
			IAerobaticData data = getAerobaticDataOrDefault(player);
			if (data.isFlying()) {
				if (mc.isPaused()) {
					data.setLastRotationTime(currentTimeMillis() / 1000D);
				} else AerobaticFlight.applyRotationAcceleration(player, event.renderTickTime);
			}
		}
	}
}
