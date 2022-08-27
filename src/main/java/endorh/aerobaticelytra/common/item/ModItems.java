package endorh.aerobaticelytra.common.item;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

import java.util.function.Supplier;

import static endorh.util.common.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModItems {
	@ObjectHolder(AerobaticElytra.MOD_ID + ":" + AerobaticElytraItem.NAME)
	public static final AerobaticElytraItem AEROBATIC_ELYTRA = futureNotNull();
	@ObjectHolder(AerobaticElytra.MOD_ID + ":" + AerobaticElytraWingItem.NAME)
	public static final AerobaticElytraWingItem AEROBATIC_ELYTRA_WING = futureNotNull();
	
	@SubscribeEvent
	public static void onItemsRegistration(RegistryEvent.Register<Item> event) {
		registerItems(event.getRegistry());
		AerobaticElytra.logRegistered("Items");
	}
	
	/**
	 * Register mod items
	 */
	public static void registerItems(IForgeRegistry<Item> registry) {
		registerItem(registry, AerobaticElytraItem::new);
		registerItem(registry, AerobaticElytraWingItem::new);
	}
	
	private static <T extends Item> void registerItem(
	  IForgeRegistry<Item> registry, Supplier<T> itemSupplier) {
		T item = itemSupplier.get();
		registry.register(item);
	}
}
