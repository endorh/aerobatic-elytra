package endorh.aerobatic_elytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import endorh.aerobatic_elytra.common.item.ModItems;
import endorh.aerobatic_elytra.common.recipe.JoinRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocus.Mode;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;

public class JoinRecipeCategory implements IRecipeCategory<JoinRecipe> {
	public static final ResourceLocation UID = prefix("join");
	
	private final IDrawable background;
	private final IDrawable icon;
	private final String localizedName = I18n.format("aerobatic-elytra.recipe.category.join");
	
	public JoinRecipeCategory(IGuiHelper guiHelper) {
		ResourceLocation location = prefix("textures/gui/recipes.png");
		background = guiHelper.createDrawable(location, 0, 18, 116, 54);
		icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.AEROBATIC_ELYTRA_WING));
	}
	
	@Override public @NotNull ResourceLocation getUid() {
		return UID;
	}
	@Override public @NotNull Class<? extends JoinRecipe> getRecipeClass() {
		return JoinRecipe.class;
	}
	
	@Override public @NotNull String getTitle() {
		return localizedName;
	}
	@Override public @NotNull IDrawable getBackground() {
		return background;
	}
	@Override public @NotNull IDrawable getIcon() {
		return icon;
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
		List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
		List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);
		IFocus<?> focus = layout.getFocus();
		
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 0, 0);
		stacks.set(0, getItemMatchingFocus(focus, Mode.INPUT, outputs.get(0), inputs.get(0)));
		
		stacks.init(1, true, 18,  0);
		stacks.set(1, getItemMatchingFocus(focus, Mode.INPUT, outputs.get(0), inputs.get(0)));
		
		stacks.init(2, true, 94, 18);
		stacks.set(2, getItemMatchingFocus(focus, Mode.OUTPUT, inputs.get(0), outputs.get(0)));
		
	}
	
	private List<ItemStack> getItemMatchingFocus(IFocus<?> focus, IFocus.Mode mode, List<ItemStack> focused, List<ItemStack> other) {
		if (focus != null && focus.getMode() == mode) {
			ItemStack focusStack = (ItemStack) focus.getValue();
			for (int i = 0; i < focused.size(); i++) {
				if (focusStack.isItemEqual(focused.get(i))) {
					return Collections.singletonList(other.get(i));
				}
			}
		}
		return other;
	}
}
