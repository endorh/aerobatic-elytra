package endorh.aerobatic_elytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import endorh.aerobatic_elytra.client.config.ClientConfig;
import endorh.aerobatic_elytra.common.capability.IElytraSpec.Upgrade;
import endorh.aerobatic_elytra.common.item.ModItems;
import endorh.aerobatic_elytra.common.recipe.ItemSelector;
import endorh.aerobatic_elytra.common.recipe.UpgradeRecipe;
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
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;
import static endorh.util.common.TextUtil.ttc;

public class UpgradeRecipeCategory implements IRecipeCategory<UpgradeRecipe> {
	public static final ResourceLocation UID = prefix("upgrade");
	
	private final IDrawable background;
	private final IDrawable background_dark;
	private final IDrawable icon;
	private final String localizedName = I18n.format("aerobatic-elytra.recipe.category.upgrade");
	
	public UpgradeRecipeCategory(IGuiHelper guiHelper) {
		ResourceLocation location = prefix("textures/gui/recipes.png");
		background = guiHelper.createDrawable(location, 0, 0, 138, 18);
		background_dark = guiHelper.createDrawable(location, 0, 18, 138, 18);
		icon = guiHelper.createDrawableIngredient(new ItemStack(ModItems.AEROBATIC_ELYTRA));
	}
	
	@Override public @NotNull ResourceLocation getUid() {
		return UID;
	}
	@Override public @NotNull Class<? extends UpgradeRecipe> getRecipeClass() {
		return UpgradeRecipe.class;
	}
	@Override public @NotNull String getTitle() {
		return localizedName;
	}
	@Override public @NotNull IDrawable getBackground() {
		return ClientConfig.style.dark_theme_gui? background_dark : background;
	}
	@Override public @NotNull IDrawable getIcon() {
		return icon;
	}
	@Override public void setIngredients(
	  @NotNull UpgradeRecipe recipe, @NotNull IIngredients ingredients
	) {
		final List<Ingredient> ing = new ArrayList<>(recipe.getIngredients());
		ing.add(Ingredient.fromItems(ModItems.AEROBATIC_ELYTRA));
		ingredients.setInputIngredients(ing);
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(ImmutableList.of(
		  recipe.getRecipeOutput())));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull UpgradeRecipe recipe, @NotNull IIngredients ingredients
	) {
		List<List<ItemStack>> inputs = ingredients.getInputs(VanillaTypes.ITEM);
		List<List<ItemStack>> outputs = ingredients.getOutputs(VanillaTypes.ITEM);
		IFocus<?> focus = layout.getFocus();
		
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 20, 0);
		stacks.set(0, getItemMatchingFocus(focus, Mode.INPUT, inputs.get(0), outputs.get(0)));
		
		stacks.init(1, true, 69, 0);
		stacks.set(1, getItemMatchingFocus(focus, Mode.OUTPUT, outputs.get(0), inputs.get(0)));
		
		stacks.init(2, true, 117, 0);
		stacks.set(2, getItemMatchingFocus(focus, Mode.INPUT, inputs.get(0), outputs.get(0)));
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
	
	@Override public @NotNull List<ITextComponent> getTooltipStrings(
	  @NotNull UpgradeRecipe recipe, double x, double y
	) {
		List<ITextComponent> tt = new ArrayList<>();
		if (inRect(x, y, 3, 2, 14, 14)) {
			tt.add(ttc("aerobatic-elytra.jei.help.category.upgrade"));
		} else if (inRect(x, y, 46, 1, 15, 15)) {
			for (ItemSelector sel : recipe.getSelectors())
				tt.add(sel.getDisplay());
		} else if (inRect(x, y, 91, 1, 22, 15)) {
			for (Upgrade upgrade : recipe.getUpgrades())
				tt.addAll(upgrade.getDisplay());
		}
		return tt;
	}
	
	private boolean inRect(double x, double y, double l, double t, double w, double h) {
		return x >= l && x < l + w && y >= t && y < t + h;
	}
}
