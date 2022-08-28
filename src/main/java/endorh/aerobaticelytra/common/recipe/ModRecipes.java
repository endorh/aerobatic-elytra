package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class ModRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
	  DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, AerobaticElytra.MOD_ID);
	
	// Special recipes with default Serializer (no parameters)
	public static final RegistryObject<SimpleRecipeSerializer<DyeRecipe>> DYE_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "dye_recipe", () -> new SimpleRecipeSerializer<>(DyeRecipe::new));
	public static final RegistryObject<SimpleRecipeSerializer<BannerRecipe>> BANNER_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "banner_recipe", () -> new SimpleRecipeSerializer<>(BannerRecipe::new));
	public static final RegistryObject<SimpleRecipeSerializer<TrailRecipe>> TRAIL_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "trail_recipe", () -> new SimpleRecipeSerializer<>(TrailRecipe::new));
	public static final RegistryObject<SimpleRecipeSerializer<JoinRecipe>> JOIN_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "join_recipe", () -> new SimpleRecipeSerializer<>(JoinRecipe::new));
	public static final RegistryObject<SimpleRecipeSerializer<CraftedUpgradeRecipe>>
	  CRAFTED_UPGRADE_RECIPE =
	  RECIPE_SERIALIZERS.register(
		 "crafted_upgrade_recipe", () -> new SimpleRecipeSerializer<>(CraftedUpgradeRecipe::new));
	
	// Recipes with custom Serializer
	@SubscribeEvent
	public static void onRegister(RegistryEvent.Register<RecipeSerializer<?>> event) {
		event.getRegistry().registerAll(
		  AbilityNBTInheritingShapedRecipe.SERIALIZER,
		  UpgradeRecipe.SERIALIZER,
		  RepairRecipe.SERIALIZER,
		  SplitRecipe.SERIALIZER,
		  CreativeTabAbilitySetRecipe.SERIALIZER
		);
		AerobaticElytra.logRegistered("Recipes");
	}
}
