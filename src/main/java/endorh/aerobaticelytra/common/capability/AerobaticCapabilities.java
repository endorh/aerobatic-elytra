package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticCapabilities {
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		FlightDataCapability.register();
		AerobaticDataCapability.register();
		ElytraSpecCapability.register();
		AerobaticElytra.logRegistered("Capabilities");
	}
}
