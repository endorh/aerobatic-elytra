package endorh.aerobaticelytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.ModResources;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.SplitRecipe;
import endorh.aerobaticelytra.common.recipe.SplitRecipe.LeaveData;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocus.Mode;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.split;
import static endorh.util.text.TextUtil.optSplitTtc;
import static java.lang.Math.min;

public class SplitRecipeCategory extends BaseCategory<SplitRecipe> {
	public static final ResourceLocation UID = AerobaticElytra.prefix("split");
	
	public SplitRecipeCategory() {
		super(UID, SplitRecipe.class, ModResources::byproduct3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA, Items.SHEARS, true);
	}
	
	@Override public void setIngredients(
	  @NotNull SplitRecipe recipe, @NotNull IIngredients ingredients
	) {
		ingredients.setInputIngredients(recipe.getIngredients());
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(
		  ImmutableList.of(new ItemStack(ModItems.AEROBATIC_ELYTRA_WING))));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull SplitRecipe recipe, @NotNull IIngredients ingredients
	) {
		IFocus<?> focus = layout.getFocus();
		
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				stacks.init(i * 3 + j, true, 18 * j, 18 * i);
				stacks.init(9 + i * 3 + j, false, 27 + 18 * j, 63 + 18 * i);
			}
		}
		stacks.init(18, false, 94, 18);
		if (focus != null)
			stacks.setOverrideDisplayFocus(null);
		
		final List<ItemStack> elytras = AerobaticElytraJeiHelper.getAerobaticElytrasMatchingFocus(focus);
		final Pair<List<ItemStack>, List<ItemStack>> wings = split(elytras);
		stacks.set(0, elytras);
		stacks.set(9, wings.getFirst());
		stacks.set(18, wings.getSecond());
		for (int i = 0, s = min(recipe.ingredients.size(), 8); i < s; i++) {
			final List<ItemStack> ing = Arrays.asList(
			  recipe.ingredients.get(i).getLeft().getItems());
			final List<ItemStack> st = getItemMatchingFocus(
			  focus, Mode.INPUT, ing, ing);
			stacks.set(i + 1, st);
			final LeaveData data = recipe.ingredients.get(i).getRight();
			if (data.leave)
				stacks.set(10 + i, damage(st, data.damage));
		}
	}
	
	private static List<ItemStack> damage(List<ItemStack> stacks, int damage) {
		return stacks.stream().map(s -> {
			s = s.copy();
			s.setDamageValue(s.getDamageValue() + damage);
			return s;
		}).collect(Collectors.toList());
	}
	
	@Override public @NotNull List<ITextComponent> getTooltipStrings(
	  @NotNull SplitRecipe recipe, double mouseX, double mouseY
	) {
		final List<ITextComponent> tt = super.getTooltipStrings(recipe, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.split"));
		else if (inRect(mouseX, mouseY, 7, 60, 17, 19))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.split.remainder"));
		return tt;
	}
}
