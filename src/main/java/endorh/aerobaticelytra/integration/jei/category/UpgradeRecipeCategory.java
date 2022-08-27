package endorh.aerobaticelytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.ModResources;
import endorh.aerobaticelytra.common.capability.IElytraSpec.Upgrade;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.ItemSelector;
import endorh.aerobaticelytra.common.recipe.UpgradeRecipe;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocus.Mode;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static endorh.util.text.TextUtil.splitTtc;

public class UpgradeRecipeCategory extends BaseCategory<UpgradeRecipe> {
	public static final ResourceLocation UID = AerobaticElytra.prefix("upgrade");
	
	public UpgradeRecipeCategory() {
		super(UID, UpgradeRecipe.class, ModResources::upgradeRecipeBg, ModItems.AEROBATIC_ELYTRA, false);
	}
	
	@Override public void setIngredients(
	  @NotNull UpgradeRecipe recipe, @NotNull IIngredients ingredients
	) {
		final List<Ingredient> ing = new ArrayList<>(recipe.getIngredients());
		ing.add(Ingredient.of(ModItems.AEROBATIC_ELYTRA));
		ingredients.setInputIngredients(ing);
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(ImmutableList.of(
		  recipe.getResultItem())));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull UpgradeRecipe recipe, @NotNull IIngredients ingredients
	) {
		List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
		List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);
		IFocus<?> focus = layout.getFocus(VanillaTypes.ITEM);
		
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 20, 0);
		stacks.init(1, true, 69, 0);
		stacks.init(2, false, 117, 0);
		
		List<ItemStack> elytras = AerobaticElytraJeiHelper.getAerobaticElytrasMatchingFocus(focus);
		stacks.set(0, elytras);
		stacks.set(1, setTag(recipe, getItemMatchingFocus(focus, Mode.OUTPUT, outputs.get(0), inputs.get(0))));
		stacks.set(2, apply(recipe, elytras));
	}
	
	protected List<ItemStack> apply(UpgradeRecipe recipe, List<ItemStack> stacks) {
		return stacks.stream().map(
		  s -> recipe.getResult(s.copy())
		).collect(Collectors.toList());
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
	  @NotNull UpgradeRecipe recipe, double x, double y
	) {
		List<Component> tt = super.getTooltipStrings(recipe, x, y);
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
		).collect(Collectors.toList());
		// reg.addRecipes(sorted, UID);
	}
}
