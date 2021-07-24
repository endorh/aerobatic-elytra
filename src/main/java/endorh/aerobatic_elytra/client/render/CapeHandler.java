package endorh.aerobatic_elytra.client.render;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.AerobaticElytraLogic;
import endorh.flight_core.events.CancelCapeRenderEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class CapeHandler {
	@SubscribeEvent
	public static void onCancelCapeRenderEvent(CancelCapeRenderEvent event) {
		// Hide the cape for players with Aerobatic Elytra
		if (AerobaticElytraLogic.hasAerobaticElytra(event.player))
			event.setCanceled(true);
	}
}
