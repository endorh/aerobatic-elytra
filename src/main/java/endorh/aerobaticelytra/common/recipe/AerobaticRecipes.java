package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticRecipes {
	public static final DeferredRegister<IRecipeSerializer<?>> RECIPE_SERIALIZERS =
	  DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, AerobaticElytra.MOD_ID);
	
	// Special recipes with default Serializer (no parameters)
	public static final RegistryObject<SpecialRecipeSerializer<DyeRecipe>> DYE_RECIPE =
	  RECIPE_SERIALIZERS.register(
	    "dye_recipe", () -> new SpecialRecipeSerializer<>(DyeRecipe::new));
	public static final RegistryObject<SpecialRecipeSerializer<BannerRecipe>> BANNER_RECIPE =
	  RECIPE_SERIALIZERS.register(
	    "banner_recipe", () -> new SpecialRecipeSerializer<>(BannerRecipe::new));
	public static final RegistryObject<SpecialRecipeSerializer<TrailRecipe>> TRAIL_RECIPE =
	  RECIPE_SERIALIZERS.register(
	    "trail_recipe", () -> new SpecialRecipeSerializer<>(TrailRecipe::new));
	public static final RegistryObject<SpecialRecipeSerializer<JoinRecipe>> JOIN_RECIPE =
	  RECIPE_SERIALIZERS.register(
	    "join_recipe", () -> new SpecialRecipeSerializer<>(JoinRecipe::new));
	public static final RegistryObject<SpecialRecipeSerializer<CraftedUpgradeRecipe>> CRAFTED_UPGRADE_RECIPE =
	  RECIPE_SERIALIZERS.register(
	    "crafted_upgrade_recipe", () -> new SpecialRecipeSerializer<>(CraftedUpgradeRecipe::new));
	
	// Recipes with custom Serializer
	@SubscribeEvent
	public static void onRegister(RegistryEvent.Register<IRecipeSerializer<?>> event) {
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
