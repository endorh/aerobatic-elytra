package endorh.aerobaticelytra.server.loot;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.core.Registry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class AerobaticLootConditions {
	public static LootItemConditionType ORIGIN_DISTANCE;
	public static LootItemConditionType AEROBATIC_FLYING;
	public static LootItemConditionType ELYTRA_FLYING;
	public static LootItemConditionType SUBMERGED;
	
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		register();
		AerobaticElytra.logRegistered("Loot conditions");
	}
	
	public static void register() {
		ORIGIN_DISTANCE = register("origin_distance", new OriginDistanceLootCondition.ConditionSerializer());
		AEROBATIC_FLYING = register("aerobatic_flying", new AerobaticFlyingLootCondition.ConditionSerializer());
		ELYTRA_FLYING = register("elytra_flying", new ElytraFlyingLootCondition.ConditionSerializer());
		SUBMERGED = register("submerged", new SubmergedLootCondition.ConditionSerializer());
	}
	
	public static LootItemConditionType register(
	  String name, Serializer<? extends LootItemCondition> serializer
	) {
		return Registry.register(
		  Registry.LOOT_CONDITION_TYPE, AerobaticElytra.prefix(name),
		  new LootItemConditionType(serializer));
	}
}
