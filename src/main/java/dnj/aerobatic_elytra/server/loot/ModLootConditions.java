package dnj.aerobatic_elytra.server.loot;

import dnj.aerobatic_elytra.AerobaticElytra;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import static dnj.aerobatic_elytra.AerobaticElytra.prefix;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModLootConditions {
	public static LootConditionType ORIGIN_DISTANCE;
	
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		register();
		AerobaticElytra.logRegistered("Loot conditions");
	}
	
	public static void register() {
		ORIGIN_DISTANCE = register("origin_distance", new OriginDistanceLootCondition.Serializer());
	}
	
	public static LootConditionType register(
	  String name, ILootSerializer<? extends ILootCondition> serializer
	) {
		return Registry.register(
		  Registry.LOOT_CONDITION_TYPE, prefix(name), new LootConditionType(serializer));
	}
}
