package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class AerobaticRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
	  DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, AerobaticElytra.MOD_ID);
	
	// Special recipes with default Serializer (no parameters)
	public static final RegistryObject<SimpleCraftingRecipeSerializer<DyeRecipe>> DYE_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "dye_recipe", () -> new SimpleCraftingRecipeSerializer<>(DyeRecipe::new));
	public static final RegistryObject<SimpleCraftingRecipeSerializer<BannerRecipe>> BANNER_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "banner_recipe", () -> new SimpleCraftingRecipeSerializer<>(BannerRecipe::new));
	public static final RegistryObject<SimpleCraftingRecipeSerializer<TrailRecipe>> TRAIL_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "trail_recipe", () -> new SimpleCraftingRecipeSerializer<>(TrailRecipe::new));
	public static final RegistryObject<SimpleCraftingRecipeSerializer<JoinRecipe>> JOIN_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "join_recipe", () -> new SimpleCraftingRecipeSerializer<>(JoinRecipe::new));
	public static final RegistryObject<SimpleCraftingRecipeSerializer<CraftedUpgradeRecipe>>
	  CRAFTED_UPGRADE_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "crafted_upgrade_recipe", () -> new SimpleCraftingRecipeSerializer<>(CraftedUpgradeRecipe::new));
	
	// Recipes with custom Serializer
	@SubscribeEvent
	public static void onRegister(RegisterEvent event) {
		event.register(ForgeRegistries.RECIPE_SERIALIZERS.getRegistryKey(), r -> {
			r.register("ability_nbt_inheriting_shaped_recipe", AbilityNBTInheritingShapedRecipe.SERIALIZER);
			r.register("upgrade_recipe", UpgradeRecipe.SERIALIZER);
			r.register("repair_recipe", RepairRecipe.SERIALIZER);
			r.register("split_recipe", SplitRecipe.SERIALIZER);
			r.register("creative_tab_ability_set", CreativeTabAbilitySetRecipe.SERIALIZER);
			AerobaticElytra.logRegistered("Recipes");
		});
	}
}
