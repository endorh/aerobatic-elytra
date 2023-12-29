package endorh.aerobaticelytra.integration.jei;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.input.KeyHandler;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.recipe.CraftedUpgradeRecipe;
import endorh.aerobaticelytra.integration.jei.category.*;
import endorh.lazulib.recipe.RecipeManagerHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.lazulib.text.TextUtil.ttc;
import static mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;

/**
 * Expose the mod recipe categories to the JEI mod
 */
@JeiPlugin
public class AerobaticElytraJeiPlugin implements IModPlugin {
	public static final ResourceLocation pluginUid = new ResourceLocation(
	  AerobaticElytra.MOD_ID, "jei_plugin");
	public static IIngredientManager ingredientManager = null;
	public static IRecipeManager recipeManager = null;
	public static IJeiHelpers jeiHelpers = null;
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
	protected static List<ContextualRecipeCategory<?, ?>> contextualCategories = new ArrayList<>();
	
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
			if (cat instanceof ContextualRecipeCategory<?, ?> ct)
				contextualCategories.add(ct);
			reg.addRecipeCategories(cat);
		}
	}
	
	@Override public void registerItemSubtypes(@NotNull ISubtypeRegistration reg) {
		// Differentiate aerobatic elytras by their abilities
		reg.registerSubtypeInterpreter(
		  AerobaticElytraItems.AEROBATIC_ELYTRA, (stack, context) -> switch (context) {
			  case Ingredient -> getElytraSpecOrDefault(stack).getAbilities().entrySet().stream()
				  .sorted(Comparator.comparing(e -> e.getKey().getName()))
				  .map(e -> e.getKey().getName() + ":" + e.getValue())
				  .collect(Collectors.joining(";"));
			  case Recipe -> NONE;
		  });
	}

	@Override public void registerRecipes(@NotNull IRecipeRegistration reg) {
		// Add info to item
		String key = KeyHandler.FLIGHT_MODE.getKey().getName();
		String keyName = I18n.get(key);
		if (key.equals(keyName))
			keyName = keyName.replaceFirst("key\\.keyboard\\.", "");
		reg.addIngredientInfo(
		  new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA), VanillaTypes.ITEM_STACK,
		  ttc("aerobaticelytra.jei.info.aerobatic_elytra", keyName));
		
		// Get recipe list
		final ClientLevel world = Minecraft.getInstance().level;
		assert world != null;
		RecipeManager recipeManager = world.getRecipeManager();
		
		for (BaseCategory<?> cat: categories)
			registerRecipes(reg, recipeManager, cat);
	}
	
	protected static <V> void registerRecipes(
	  IRecipeRegistration reg, RecipeManager manager, BaseCategory<V> cat
	) {
		ingredientManager = reg.getIngredientManager();
		cat.registerRecipes(reg, manager);
	}

	@Override public void registerRecipeTransferHandlers(@NotNull IRecipeTransferRegistration reg) {
		reg.addRecipeTransferHandler(CraftingMenu.class, MenuType.CRAFTING, JoinRecipeCategory.TYPE, 0, 9, 9, 36);
		reg.addRecipeTransferHandler(CraftingMenu.class, MenuType.CRAFTING, SplitRecipeCategory.TYPE, 0, 9, 9, 36);
		// Getting these to work would require a custom IRecipeTransferHandler to properly disambiguate/tolerate
		// fireworks/dyes/banners from the player's inventory, and nobody is ever going to use it.
		// There's no benefit from autofilling a customization recipe.
		// reg.addRecipeTransferHandler(CraftingMenu.class, MenuType.CRAFTING, BannerRecipeCategory.TYPE, 0, 9, 9, 36);
		// reg.addRecipeTransferHandler(CraftingMenu.class, MenuType.CRAFTING, DyeRecipeCategory.TYPE, 0, 9, 9, 36);
		// reg.addRecipeTransferHandler(CraftingMenu.class, MenuType.CRAFTING, TrailRecipeCategory.TYPE, 3, 6, 9, 36);
	}

	@Override public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
		// Register the Aerobatic Elytra item as catalyst for upgrade recipes
		reg.addRecipeCatalyst(new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA),
			UpgradeRecipeCategory.TYPE);
		final ItemStack craftingTable = new ItemStack(Items.CRAFTING_TABLE);
		reg.addRecipeCatalyst(
		  craftingTable,
		  JoinRecipeCategory.TYPE, SplitRecipeCategory.TYPE, DyeRecipeCategory.TYPE, BannerRecipeCategory.TYPE, TrailRecipeCategory.TYPE);
		if (RecipeManagerHelper.getRecipeManager().getRecipes().stream()
			.anyMatch(r -> r instanceof CraftedUpgradeRecipe)
		) reg.addRecipeCatalyst(craftingTable, UpgradeRecipeCategory.TYPE);
	}
	
	public static <T> RecipeType<T> create(ResourceLocation location, Class<T> cls) {
		return RecipeType.create(location.getNamespace(), location.getPath(), cls);
	}
	
	@Override public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		ingredientManager = jeiRuntime.getIngredientManager();
		recipeManager = jeiRuntime.getRecipeManager();
		jeiHelpers = jeiRuntime.getJeiHelpers();
	}

	@Override public void registerAdvanced(@NotNull IAdvancedRegistration reg) {
		reg.addRecipeManagerPlugin(new AerobaticElytraRecipeManagerPlugin(
			contextualCategories,
			reg.getJeiHelpers().getFocusFactory()
		));
	}
}