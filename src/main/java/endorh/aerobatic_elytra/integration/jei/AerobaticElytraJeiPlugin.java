package endorh.aerobatic_elytra.integration.jei;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.client.input.KeyHandler;
import endorh.aerobatic_elytra.common.item.ModItems;
import endorh.aerobatic_elytra.common.recipe.ItemSelector;
import endorh.aerobatic_elytra.common.recipe.JoinRecipe;
import endorh.aerobatic_elytra.common.recipe.UpgradeRecipe;
import endorh.aerobatic_elytra.integration.jei.category.JoinRecipeCategory;
import endorh.aerobatic_elytra.integration.jei.category.UpgradeRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;

/**
 * Expose the mod recipe categories to the JEI mod
 */
@SuppressWarnings("unused")
@JeiPlugin
public class AerobaticElytraJeiPlugin implements IModPlugin {
	public static final ResourceLocation pluginUid = new ResourceLocation(
	  AerobaticElytra.MOD_ID, "jei_plugin");
	public IRecipeCategory<UpgradeRecipe> upgradeRecipeCategory;
	public IRecipeCategory<JoinRecipe> joinRecipeIRecipeCategory;
	
	@NotNull @Override public ResourceLocation getPluginUid() {
		return pluginUid;
	}
	
	@Override public void registerCategories(IRecipeCategoryRegistration reg) {
		IJeiHelpers jeiHelpers = reg.getJeiHelpers();
		IGuiHelper guiHelper = jeiHelpers.getGuiHelper();
		reg.addRecipeCategories(this.upgradeRecipeCategory = new UpgradeRecipeCategory(guiHelper));
		reg.addRecipeCategories(this.joinRecipeIRecipeCategory = new JoinRecipeCategory(guiHelper));
	}
	
	@Override
	public void registerItemSubtypes(@NotNull ISubtypeRegistration reg) {
		// Differentiate aerobatic elytras by their abilities
		reg.registerSubtypeInterpreter(
		  ModItems.AEROBATIC_ELYTRA,
		  stack -> getElytraSpecOrDefault(stack).getAbilities().entrySet().stream().map(
			 entry -> entry.getKey().toString() + ":" + entry.getValue()
		  ).collect(Collectors.joining()));
	}
	
	@Override public void registerRecipes(@NotNull IRecipeRegistration reg) {
		// Add info to item
		String key = KeyHandler.FLIGHT_MODE_KEYBINDING.getKey().getTranslationKey();
		String keyName = I18n.format(key);
		if (key.equals(keyName))
			keyName = keyName.replaceFirst("key\\.keyboard\\.", "");
		reg.addIngredientInfo(
		  new ItemStack(ModItems.AEROBATIC_ELYTRA), VanillaTypes.ITEM,
		  I18n.format("aerobatic-elytra.jei.info.aerobatic_elytra", keyName));
		
		// Get recipe list
		final ClientWorld world = Minecraft.getInstance().world;
		assert world != null;
		RecipeManager recipeManager = world.getRecipeManager();
		final Collection<IRecipe<?>> recipeList = recipeManager.getRecipes();
		
		// Register upgrade recipes
		//noinspection unchecked
		List<UpgradeRecipe> upgradeRecipes =
		  ((Stream<UpgradeRecipe>) (Stream<?>) recipeList.stream().filter(
			 r -> (r instanceof UpgradeRecipe)
		  )).filter(UpgradeRecipe::isValid).sorted(
			 Comparator.comparing(
				r -> r.getSelectors().stream()
				  .map(ItemSelector::toString).collect(Collectors.joining(";")))
		  ).collect(Collectors.toList());
		reg.addRecipes(upgradeRecipes, UpgradeRecipeCategory.UID);
		
		// Register join recipes
		//noinspection unchecked
		List<JoinRecipe> joinRecipes =
		  (List<JoinRecipe>) (List<?>) recipeList.stream().filter(
		    recipe -> recipe instanceof JoinRecipe
		  ).collect(Collectors.toList());
		reg.addRecipes(joinRecipes, JoinRecipeCategory.UID);
	}
	
	@Override public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
		// Register the Aerobatic Elytra item as catalyst for upgrade recipes
		reg.addRecipeCatalyst(new ItemStack(ModItems.AEROBATIC_ELYTRA), UpgradeRecipeCategory.UID);
	}
}