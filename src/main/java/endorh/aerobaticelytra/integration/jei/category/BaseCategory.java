package endorh.aerobaticelytra.integration.jei.category;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.client.config.ClientConfig.style.dark_theme;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiPlugin;
import endorh.aerobaticelytra.integration.jei.gui.JeiResources;
import endorh.aerobaticelytra.integration.jei.gui.ShapelessDecoratedDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static endorh.util.text.TextUtil.ttc;

/**
 * Common logic for categories
 */
public abstract class BaseCategory<T> implements IRecipeCategory<T> {
	protected final ResourceLocation UID;
	protected final IDrawable background;
	protected final IDrawable background_dark;
	protected IDrawable icon;
	protected Pair<Item, Item> iconItems;
	protected final String localizedNameKey;
	protected final Class<? extends T> recipeClass;
	protected final boolean shapeless;
	
	public BaseCategory(
	  ResourceLocation UID, Class<? extends T> recipeClass,
	  Function<IGuiHelper, IDrawable[]> backgroundProvider,
	  Item icon, boolean shapeless
	) { this(UID, recipeClass, backgroundProvider, icon, null, shapeless); }
	
	public BaseCategory(
	  ResourceLocation UID, Class<? extends T> recipeClass,
	  Function<IGuiHelper, IDrawable[]> backgroundProvider,
	  Item icon, Item iconSecond, boolean shapeless
	) {
		IGuiHelper guiHelper = AerobaticElytraJeiPlugin.guiHelper;
		final String shortName = UID.getPath().replace("/", ".");
		this.UID = UID;
		if (shapeless)
			backgroundProvider = JeiResources.shapeless(backgroundProvider);
		final IDrawable[] bg = backgroundProvider.apply(guiHelper);
		background = bg[0];
		background_dark = bg[1];
		localizedNameKey = "aerobaticelytra.recipe.category." + shortName;
		this.recipeClass = recipeClass;
		iconItems = Pair.of(icon, iconSecond);
		this.shapeless = shapeless;
	}
	
	@Override public @NotNull ResourceLocation getUid() {
		return UID;
	}
	
	@Override public @NotNull Class<? extends T> getRecipeClass() {
		return recipeClass;
	}
	
	@Override public @NotNull String getTitle() {
		return I18n.format(localizedNameKey);
	}
	
	@Override public @NotNull IDrawable getBackground() {
		return dark_theme.enabled ? background_dark : background;
	}
	
	@Override public @NotNull IDrawable getIcon() {
		if (icon == null) {
			IGuiHelper guiHelper = AerobaticElytraJeiPlugin.guiHelper;
			final Item first = iconItems.getFirst();
			final Item second = iconItems.getSecond();
			this.icon = second != null
			            ? AerobaticElytraJeiHelper.createMultiIngredientDrawable(
			              new ItemStack(first), new ItemStack(second))
			            : guiHelper.createDrawableIngredient(new ItemStack(first));
		}
		return icon;
	}
	
	public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		//noinspection unchecked
		registerRecipes(reg, recipeManager, recipeManager.getRecipes().stream()
		  .filter(getRecipeClass()::isInstance)
		  .sorted(Comparator.comparing(
		    r -> r.getIngredients().stream().map(Ingredient::toString)
		      .collect(Collectors.joining(";"))
		  )).map(r -> (T) r).collect(Collectors.toList()));
	}
	
	public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager, List<T> recipes
	) {
		reg.addRecipes(recipes, UID);
	}
	
	protected static List<ItemStack> getItemMatchingFocus(
	  IFocus<?> focus, IFocus.Mode mode, List<ItemStack> focused, List<ItemStack> other
	) {
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
	  @NotNull T recipe, double mouseX, double mouseY
	) {
		List<ITextComponent> list = new ArrayList<>();
		if (shapeless && inRect(
		  mouseX, mouseY, background.getWidth() - ShapelessDecoratedDrawable.shapelessIconWidth(),
		  0, ShapelessDecoratedDrawable.shapelessIconWidth(), ShapelessDecoratedDrawable.shapelessIconHeight()))
			list.add(ttc("jei.tooltip.shapeless.recipe"));
		return list;
	}
	
	protected static boolean inRect(double x, double y, double l, double t, double w, double h) {
		return x >= l && x < l + w && y >= t && y < t + h;
	}
}
