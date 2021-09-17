package endorh.aerobatic_elytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import endorh.aerobatic_elytra.client.ModResources;
import endorh.aerobatic_elytra.common.item.ModItems;
import endorh.aerobatic_elytra.common.recipe.JoinRecipe;
import endorh.aerobatic_elytra.integration.jei.AerobaticElytraJeiHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;
import static endorh.aerobatic_elytra.integration.jei.AerobaticElytraJeiHelper.split;
import static endorh.util.text.TextUtil.optSplitTtc;

public class JoinRecipeCategory extends BaseCategory<JoinRecipe> {
	public static final ResourceLocation UID = prefix("join");
	
	public JoinRecipeCategory() {
		super(UID, JoinRecipe.class, ModResources::regular3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA_WING, ModItems.AEROBATIC_ELYTRA_WING, false);
	}
	
	@Override public void setIngredients(
	  @NotNull JoinRecipe recipe, @NotNull IIngredients ingredients
	) {
		ingredients.setInputIngredients(ImmutableList.of(
		  Ingredient.fromItems(ModItems.AEROBATIC_ELYTRA_WING),
		  Ingredient.fromItems(ModItems.AEROBATIC_ELYTRA_WING)));
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(
		  ImmutableList.of(new ItemStack(ModItems.AEROBATIC_ELYTRA))));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull JoinRecipe recipe, @NotNull IIngredients ingredients
	) {
		IFocus<?> focus = layout.getFocus();
		
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 0, 0);
		stacks.init(1, true, 18,  0);
		stacks.init(2, false, 94, 18);
		
		if (focus != null)
			stacks.setOverrideDisplayFocus(null);
		
		final List<ItemStack> elytras = AerobaticElytraJeiHelper
		  .getAerobaticElytrasMatchingFocus(focus);
		final Pair<List<ItemStack>, List<ItemStack>> wings = split(elytras);
		stacks.set(0, wings.getFirst());
		stacks.set(1, wings.getSecond());
		stacks.set(2, elytras);
	}
	
	@Override public @NotNull List<ITextComponent> getTooltipStrings(
	  @NotNull JoinRecipe recipe, double mouseX, double mouseY
	) {
		final List<ITextComponent> tt = super.getTooltipStrings(recipe, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobatic-elytra.jei.help.category.join"));
		return tt;
	}
}
