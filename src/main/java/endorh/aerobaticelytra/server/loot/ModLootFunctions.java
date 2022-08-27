package endorh.aerobaticelytra.server.loot;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.core.Registry;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModLootFunctions {
	
	public static LootItemFunctionType SET_ABILITIES;
	
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		register();
		AerobaticElytra.logRegistered("Loot functions");
	}
	
	public static void register() {
		SET_ABILITIES = register("set_abilities", new SetAbilitiesLootFunction.Serializer());
	}
	
	public static LootItemFunctionType register(String name, Serializer<? extends LootItemFunction> serializer) {
		return Registry.register(
		  Registry.LOOT_FUNCTION_TYPE, AerobaticElytra.prefix(name), new LootItemFunctionType(serializer));
	}
}
