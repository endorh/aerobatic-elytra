package endorh.aerobatic_elytra.network;

import endorh.aerobatic_elytra.AerobaticElytra;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Supplier;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;

/**
 * Contains the main network channel of the mod and handles
 * packet registration
 */
@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class NetworkHandler {
	protected static final String PROTOCOL_VERSION = "1";
	protected static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
	  prefix("main"),
	  () -> PROTOCOL_VERSION,
	  PROTOCOL_VERSION::equals,
	  PROTOCOL_VERSION::equals
	);
	private static int ID_COUNT = 0;
	protected static Supplier<Integer> ID_GEN = () -> ID_COUNT++;
	
	// All packets must be registered in sequential order in both sides
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		AerobaticPackets.registerAll();
		UpgradeRecipePacket.register();
		WeatherPackets.registerAll();
		DebugPackets.registerAll();
		AerobaticElytra.logRegistered("Packets");
	}
}
