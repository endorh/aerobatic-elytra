package endorh.aerobaticelytra.client.render;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.flightcore.events.CancelCapeRenderEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(value=Dist.CLIENT, modid=AerobaticElytra.MOD_ID)
public class CapeHandler {
	@SubscribeEvent
	public static void onCancelCapeRenderEvent(CancelCapeRenderEvent event) {
		// Hide the cape for players with Aerobatic Elytra
		if (AerobaticElytraLogic.hasAerobaticElytra(event.player))
			event.setCanceled(true);
	}
}
