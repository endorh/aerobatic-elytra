package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.stream.Stream;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class ModCapabilities {
	@SubscribeEvent
	public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
		Stream.of(IFlightData.class, IAerobaticData.class, IElytraSpec.class)
		  .forEach(event::register);
		AerobaticElytra.logRegistered("Capabilities");
	}
}
