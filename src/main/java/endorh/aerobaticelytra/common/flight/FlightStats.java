package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class FlightStats {
	public static ResourceLocation AEROBATIC_FLIGHT_ONE_CM;
	public static ResourceLocation AEROBATIC_BOOSTS;
	public static ResourceLocation AEROBATIC_SLIME_BOUNCES;
	
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		register();
	}
	
	public static void register() {
		AEROBATIC_FLIGHT_ONE_CM = reg("aerobatic_flight_one_cm", StatFormatter.DISTANCE);
		AEROBATIC_SLIME_BOUNCES = reg("aerobatic_slime_bounces", StatFormatter.DEFAULT);
		AEROBATIC_BOOSTS = reg("aerobatic_boosts", StatFormatter.DEFAULT);
		AerobaticElytra.logRegistered("Stats");
	}
	
	@SuppressWarnings("SameParameterValue")
	private static ResourceLocation reg(
	  String key, StatFormatter formatter
	) {
		ResourceLocation location = prefix(key);
		Registry.register(Registry.CUSTOM_STAT, key, location);
		Stats.CUSTOM.get(location, formatter);
		return location;
	}
}
