package endorh.aerobaticelytra.server.loot;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.functions.ILootFunction;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticLootFunctions {
	
	public static LootFunctionType SET_ABILITIES;
	
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent event) {
		register();
		AerobaticElytra.logRegistered("Loot functions");
	}
	
	public static void register() {
		SET_ABILITIES = register("set_abilities", new SetAbilitiesLootFunction.Serializer());
	}
	
	public static LootFunctionType register(String name, ILootSerializer<? extends ILootFunction> serializer) {
		return Registry.register(
		  Registry.LOOT_FUNCTION_TYPE, AerobaticElytra.prefix(name), new LootFunctionType(serializer));
	}
}
