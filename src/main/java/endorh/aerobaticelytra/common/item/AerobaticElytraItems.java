package endorh.aerobaticelytra.common.item;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.recipe.CreativeTabAbilitySetRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;

import java.util.List;
import java.util.stream.Stream;

import static endorh.lazulib.common.ForgeUtil.futureNotNull;

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

	@SubscribeEvent
	public static void onCreativeModeTabPopulation(BuildCreativeModeTabContentsEvent event) {
		CreativeModeTab tab = event.getTab();
		if (tab.getType() != CreativeModeTab.Type.CATEGORY) return;
		// The default values are added by the default `creative_tab_ability_set` recipe
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) return;
		//noinspection unchecked
		List<CreativeTabAbilitySetRecipe> abilitySets =
			((Stream<CreativeTabAbilitySetRecipe>) (Stream<?>) level.getRecipeManager()
				.getRecipes().stream()
				.filter(r -> r instanceof CreativeTabAbilitySetRecipe))
				.sorted().toList();
		for (CreativeTabAbilitySetRecipe abilitySet : abilitySets) {
			if (abilitySet.matchesTab(tab))
				event.accept(abilitySet.getElytraStack(), abilitySet.getVisibility());
		}
	}
}
