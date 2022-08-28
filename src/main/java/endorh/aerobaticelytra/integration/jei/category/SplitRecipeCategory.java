package endorh.aerobaticelytra.integration.jei.category;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.ModResources;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.SplitRecipe;
import endorh.aerobaticelytra.common.recipe.SplitRecipe.LeaveData;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.getAerobaticElytrasMatchingFocus;
import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.split;
import static endorh.util.text.TextUtil.optSplitTtc;
import static java.lang.Math.min;

public class SplitRecipeCategory extends BaseCategory<SplitRecipe> {
	public static final RecipeType<SplitRecipe> TYPE = RecipeType.create(AerobaticElytra.MOD_ID, "split", SplitRecipe.class);
	
	public SplitRecipeCategory() {
		super(TYPE, ModResources::byproduct3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA, Items.SHEARS, true);
	}
	
	@Override
	public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull SplitRecipe recipe, @NotNull IFocusGroup focuses) {
		List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focuses.getFocuses(VanillaTypes.ITEM_STACK));
		
		List<IRecipeSlotBuilder> slots = new ArrayList<>(18);
		final Pair<List<ItemStack>, List<ItemStack>> wings = split(elytras);
		for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++)
			slots.add(builder.addSlot(RecipeIngredientRole.INPUT, 18 * j, 18 * i));
		for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++)
			slots.add(builder.addSlot(RecipeIngredientRole.INPUT, 27 + 18 * j, 63 + 18 * i));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 94, 18)
		  .addItemStacks(wings.getSecond());
		
		slots.get(0).addItemStacks(elytras);
		slots.get(9).addItemStacks(wings.getFirst());
		for (int i = 0, s = min(recipe.ingredients.size(), 8); i < s; i++) {
			final List<ItemStack> ing = Arrays.asList(
			  recipe.ingredients.get(i).getLeft().getItems());
			final List<ItemStack> st = getItemMatchingFocus(
			  focuses.getFocuses(VanillaTypes.ITEM_STACK),
			  RecipeIngredientRole.INPUT, ing, ing);
			slots.get(i + 1).addItemStacks(st);
			final LeaveData data = recipe.ingredients.get(i).getRight();
			if (data.leave)
				slots.get(10 + i).addItemStacks(damage(st, data.damage));
		}
	}
	
	private static List<ItemStack> damage(List<ItemStack> stacks, int damage) {
		return stacks.stream().map(s -> {
			s = s.copy();
			s.setDamageValue(s.getDamageValue() + damage);
			return s;
		}).collect(Collectors.toList());
	}
	
	@Override public @NotNull List<Component> getTooltipStrings(
	  @NotNull SplitRecipe recipe, @NotNull IRecipeSlotsView view, double mouseX, double mouseY
	) {
		final List<Component> tt = super.getTooltipStrings(recipe, view, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.split"));
		else if (inRect(mouseX, mouseY, 7, 60, 17, 19))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.split.remainder"));
		return tt;
	}
}
