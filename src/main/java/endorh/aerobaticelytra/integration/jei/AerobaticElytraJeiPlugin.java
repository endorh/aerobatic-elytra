package endorh.aerobaticelytra.integration.jei;

import com.google.common.collect.ImmutableList;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.input.KeyHandler;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.recipe.UpgradeRecipe;
import endorh.aerobaticelytra.integration.jei.category.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocus.Mode;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.util.text.TextUtil.ttc;

/**
 * Expose the mod recipe categories to the JEI mod
 */
@SuppressWarnings("unused")
@JeiPlugin
public class AerobaticElytraJeiPlugin implements IModPlugin {
	public static final ResourceLocation pluginUid = new ResourceLocation(
	  AerobaticElytra.MOD_ID, "jei_plugin");
	public static IIngredientManager ingredientManager = null;
	public static IRecipeManager recipeManager = null;
	public static IGuiHelper guiHelper = null;
	
	protected static final List<Supplier<BaseCategory<?>>> categoryConstructors =
	  Util.make(new ArrayList<>(), l -> {
		  l.add(UpgradeRecipeCategory::new);
		  l.add(TrailRecipeCategory::new);
		  l.add(DyeRecipeCategory::new);
		  l.add(BannerRecipeCategory::new);
		  l.add(SplitRecipeCategory::new);
		  l.add(JoinRecipeCategory::new);
	  });
	protected static final List<BaseCategory<?>> categories = new ArrayList<>();
	
	@NotNull @Override public ResourceLocation getPluginUid() {
		return pluginUid;
	}
	
	@Override public void registerCategories(IRecipeCategoryRegistration reg) {
		IJeiHelpers jeiHelpers = reg.getJeiHelpers();
		guiHelper = jeiHelpers.getGuiHelper();
		categories.clear();
		for (Supplier<BaseCategory<?>> c: categoryConstructors) {
			final BaseCategory<?> cat = c.get();
			categories.add(cat);
			reg.addRecipeCategories(cat);
		}
	}
	
	@Override public void registerItemSubtypes(@NotNull ISubtypeRegistration reg) {
		// Differentiate aerobatic elytras by their abilities
		reg.registerSubtypeInterpreter(
		  AerobaticElytraItems.AEROBATIC_ELYTRA,
		  (stack, context) -> getElytraSpecOrDefault(stack).getAbilities().entrySet().stream().map(
			 entry -> entry.getKey().toString() + ":" + entry.getValue()
		  ).collect(Collectors.joining()));
	}
	
	@Override public void registerRecipes(@NotNull IRecipeRegistration reg) {
		// Add info to item
		String key = KeyHandler.FLIGHT_MODE.getKey().getName();
		String keyName = I18n.get(key);
		if (key.equals(keyName))
			keyName = keyName.replaceFirst("key\\.keyboard\\.", "");
		reg.addIngredientInfo(
		  new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA), VanillaTypes.ITEM,
		  ttc("aerobaticelytra.jei.info.aerobatic_elytra", keyName));
		
		// Get recipe list
		final ClientLevel world = Minecraft.getInstance().level;
		assert world != null;
		RecipeManager recipeManager = world.getRecipeManager();
		final Collection<Recipe<?>> recipeList = recipeManager.getRecipes();
		
		for (BaseCategory<?> cat: categories)
			registerRecipes(reg, recipeManager, cat);
	}
	
	protected static <V> void registerRecipes(
	  IRecipeRegistration reg, RecipeManager manager, BaseCategory<V> cat
	) {
		ingredientManager = reg.getIngredientManager();
		final Class<? extends V> cls = cat.getRecipeClass();
		cat.registerRecipes(reg, manager);
	}
	
	@Override public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
		// Register the Aerobatic Elytra item as catalyst for upgrade recipes
		reg.addRecipeCatalyst(new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA), UpgradeRecipeCategory.UID);
		final ItemStack craftingTable = new ItemStack(Items.CRAFTING_TABLE);
		reg.addRecipeCatalyst(
		  craftingTable, JoinRecipeCategory.UID, SplitRecipeCategory.UID,
		  DyeRecipeCategory.UID, BannerRecipeCategory.UID, TrailRecipeCategory.UID);
	}
	
	@Override public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		ingredientManager = jeiRuntime.getIngredientManager();
		recipeManager = jeiRuntime.getRecipeManager();
	}
	
	@Override
	public void registerAdvanced(IAdvancedRegistration registration) {
		registration.addRecipeManagerPlugin(new RecipeManagerPlugin());
	}
	
	public static class RecipeManagerPlugin implements IRecipeManagerPlugin {
		@Override public <V> @NotNull List<ResourceLocation> getRecipeCategoryUids(
		  @NotNull IFocus<V> focus
		) {
			if (focus.getValue() instanceof AerobaticElytraItem)
				return ImmutableList.of(UpgradeRecipeCategory.UID);
			else return Collections.emptyList();
		}
		
		@Override public <T, V> @NotNull List<T> getRecipes(
		  @NotNull IRecipeCategory<T> recipeCategory, @NotNull IFocus<V> focus
		) {
			if (recipeCategory instanceof UpgradeRecipeCategory) {
				final ItemStack focused = (ItemStack) focus.getValue();
				if (focused.getItem() instanceof AerobaticElytraItem) {
					if (focus.getMode() == Mode.INPUT) {
						//noinspection unchecked
						return (List<T>) UpgradeRecipe.getUpgradeRecipes().stream().filter(
						  r -> r.getResult(focused.copy(), 1).getRight() > 0
						).collect(Collectors.toList());
					} else return (List<T>) UpgradeRecipe.getUpgradeRecipes();
				} else if (focus.getMode() == Mode.INPUT) {
					//noinspection unchecked
					return (List<T>) UpgradeRecipe.getUpgradeRecipes().stream().filter(
					  r -> r.getSelectors().stream().anyMatch(s -> s.test(focused))
					).collect(Collectors.toList());
				}
			}
			return Collections.emptyList();
		}
		
		@Override public <T> @NotNull List<T> getRecipes(@NotNull IRecipeCategory<T> recipeCategory) {
			if (recipeCategory instanceof UpgradeRecipeCategory)
				return (List<T>) UpgradeRecipe.getUpgradeRecipes();
			return Collections.emptyList();
		}
	}
}