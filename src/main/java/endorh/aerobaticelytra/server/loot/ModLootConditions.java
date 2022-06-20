package endorh.aerobaticelytra.server.loot;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModLootConditions {
	public static LootConditionType ORIGIN_DISTANCE;
	public static LootConditionType AEROBATIC_FLYING;
	public static LootConditionType ELYTRA_FLYING;
	public static LootConditionType SUBMERGED;
	
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		register();
		AerobaticElytra.logRegistered("Loot conditions");
	}
	
	public static void register() {
		ORIGIN_DISTANCE = register("origin_distance", new OriginDistanceLootCondition.Serializer());
		AEROBATIC_FLYING = register("aerobatic_flying", new AerobaticFlyingLootCondition.Serializer());
		ELYTRA_FLYING = register("elytra_flying", new ElytraFlyingLootCondition.Serializer());
		SUBMERGED = register("submerged", new SubmergedLootCondition.Serializer());
	}
	
	public static LootConditionType register(
	  String name, ILootSerializer<? extends ILootCondition> serializer
	) {
		return Registry.register(
		  Registry.LOOT_CONDITION_TYPE, AerobaticElytra.prefix(name), new LootConditionType(serializer));
	}
}
