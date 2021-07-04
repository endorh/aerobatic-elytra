package endorh.aerobatic_elytra.common.flight;

import endorh.aerobatic_elytra.AerobaticElytra;
import net.minecraft.stats.IStatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;

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
		AEROBATIC_FLIGHT_ONE_CM = reg("aerobatic_flight_one_cm", IStatFormatter.DISTANCE);
		AEROBATIC_SLIME_BOUNCES = reg("aerobatic_slime_bounces", IStatFormatter.DEFAULT);
		AEROBATIC_BOOSTS = reg("aerobatic_boosts", IStatFormatter.DEFAULT);
		AerobaticElytra.logRegistered("Stats");
	}
	
	@SuppressWarnings("SameParameterValue")
	private static ResourceLocation reg(
	  String key, IStatFormatter formatter
	) {
		ResourceLocation location = prefix(key);
		Registry.register(Registry.CUSTOM_STAT, key, location);
		Stats.CUSTOM.get(location, formatter);
		return location;
	}
}
