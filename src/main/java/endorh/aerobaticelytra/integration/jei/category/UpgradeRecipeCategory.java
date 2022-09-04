package endorh.aerobaticelytra.integration.jei.category;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.AerobaticElytraResources;
import endorh.aerobaticelytra.common.capability.IElytraSpec.Upgrade;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.recipe.ItemSelector;
import endorh.aerobaticelytra.common.recipe.UpgradeRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.getAerobaticElytrasMatchingFocus;
import static endorh.util.text.TextUtil.splitTtc;

public class UpgradeRecipeCategory extends BaseCategory<UpgradeRecipe> {
	public static final RecipeType<UpgradeRecipe> TYPE = RecipeType.create(AerobaticElytra.MOD_ID, "upgrade", UpgradeRecipe.class);
	
	public UpgradeRecipeCategory() {
		super(TYPE, AerobaticElytraResources::upgradeRecipeBg, AerobaticElytraItems.AEROBATIC_ELYTRA, false);
	}
	
	@Override public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull UpgradeRecipe recipe, @NotNull IFocusGroup focuses) {
		List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());
		List<ItemStack> inputs = ingredients.stream().flatMap(i -> Arrays.stream(i.getItems())).toList();
		ingredients.add(Ingredient.of(AerobaticElytraItems.AEROBATIC_ELYTRA));
		ItemStack output = recipe.getResultItem();
		List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focuses.getFocuses(VanillaTypes.ITEM_STACK));
		List<ItemStack> items = setTag(recipe, getItemMatchingFocus(
		  focuses.getFocuses(VanillaTypes.ITEM_STACK),
		  RecipeIngredientRole.OUTPUT, Collections.singletonList(output), inputs));
		for (int i = 0; elytras.size() < items.size(); i++) elytras.add(elytras.get(i));
		for (int i = 0; items.size() < elytras.size(); i++) items.add(items.get(i));
		builder.addSlot(RecipeIngredientRole.INPUT, 20, 0)
		  .addItemStacks(elytras);
		builder.addSlot(RecipeIngredientRole.INPUT, 69, 0)
		  .addItemStacks(items);
		builder.addSlot(RecipeIngredientRole.OUTPUT, 117, 0)
		  .addItemStacks(apply(recipe, elytras, items));
	}
	
	protected List<ItemStack> apply(
	  UpgradeRecipe recipe, List<ItemStack> elytras, List<ItemStack> ingredients
	) {
		if (elytras.size() != ingredients.size()) throw new IllegalArgumentException(
		  "elytras and ingredients must be of the same size");
		List<ItemStack> result = new ArrayList<>(elytras.size());
		for (int i = 0; i < elytras.size(); i++) {
			ItemStack res = elytras.get(i).copy();
			for (UpgradeRecipe r: UpgradeRecipe.getUpgradeRecipes(elytras.get(i), ingredients.get(i)))
				res = r.getResult(res);
			result.add(res);
		}
		return result;
	}
	
	protected List<ItemStack> setTag(UpgradeRecipe recipe, List<ItemStack> stacks) {
		return stacks.stream().map(s -> {
			s = s.copy();
			final ItemStack st = s;
			recipe.getSelectors().stream().filter(sel -> sel.testIgnoringNBT(st))
			  .findFirst().flatMap(ItemSelector::matchingNBT).ifPresent(s::setTag);
			return s;
		}).collect(Collectors.toList());
	}
	
	@Override public @NotNull List<Component> getTooltipStrings(
	  @NotNull UpgradeRecipe recipe, @NotNull IRecipeSlotsView view, double x, double y
	) {
		List<Component> tt = super.getTooltipStrings(recipe, view, x, y);
		if (inRect(x, y, 3, 2, 14, 14)) {
			tt.addAll(splitTtc("aerobaticelytra.jei.help.category.upgrade"));
		} else if (inRect(x, y, 46, 1, 15, 15)) {
			for (ItemSelector sel : recipe.getSelectors())
				tt.add(sel.getDisplay());
		} else if (inRect(x, y, 91, 1, 22, 15)) {
			for (Upgrade upgrade : recipe.getUpgrades())
				tt.addAll(upgrade.getDisplay());
		}
		return tt;
	}
	
	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager, List<UpgradeRecipe> recipes
	) {
		final List<UpgradeRecipe> sorted = recipes.stream().filter(UpgradeRecipe::isValid).sorted(
		  Comparator.comparing(
			 r -> r.getSelectors().stream()
				.map(ItemSelector::toString).collect(Collectors.joining(";")))
		).toList();
		reg.addRecipes(type, sorted);
	}
}
