package endorh.aerobaticelytra.common.item;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;

import static endorh.util.common.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraItems {
	@ObjectHolder(value=AerobaticElytra.MOD_ID + ":" + AerobaticElytraItem.NAME, registryName="item")
	public static final AerobaticElytraItem AEROBATIC_ELYTRA = futureNotNull();
	@ObjectHolder(value=AerobaticElytra.MOD_ID + ":" + AerobaticElytraWingItem.NAME, registryName="item")
	public static final AerobaticElytraWingItem AEROBATIC_ELYTRA_WING = futureNotNull();
	
	@SubscribeEvent
	public static void onItemsRegistration(RegisterEvent event) {
		event.register(ForgeRegistries.ITEMS.getRegistryKey(), r -> {
			r.register(AerobaticElytraItem.NAME, new AerobaticElytraItem());
			r.register(AerobaticElytraWingItem.NAME, new AerobaticElytraWingItem());
			AerobaticElytra.logRegistered("Items");
		});
	}
}
