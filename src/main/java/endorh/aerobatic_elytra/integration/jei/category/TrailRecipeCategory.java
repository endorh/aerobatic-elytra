package endorh.aerobatic_elytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import endorh.aerobatic_elytra.client.ModResources;
import endorh.aerobatic_elytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobatic_elytra.common.item.AerobaticElytraItem;
import endorh.aerobatic_elytra.common.item.AerobaticElytraWingItem;
import endorh.aerobatic_elytra.common.item.ModItems;
import endorh.aerobatic_elytra.common.recipe.TrailRecipe;
import endorh.aerobatic_elytra.integration.jei.category.TrailRecipeCategory.TrailRecipeWrapper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;
import static endorh.aerobatic_elytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.*;
import static net.minecraft.item.ItemStack.EMPTY;

public class TrailRecipeCategory extends BaseCategory<TrailRecipeWrapper> {
	public static final ResourceLocation UID = prefix("trail");
	
	public TrailRecipeCategory() {
		super(UID, TrailRecipeWrapper.class, ModResources::regular3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA, Items.FIREWORK_ROCKET, false);
	}
	
	@Override public void setIngredients(
	  @NotNull TrailRecipeWrapper recipe, @NotNull IIngredients ingredients
	) {
		ingredients.setInputIngredients(ImmutableList.of(
		  Ingredient.fromItems(ModItems.AEROBATIC_ELYTRA, ModItems.AEROBATIC_ELYTRA_WING),
		  Ingredient.fromTag(ItemTags.BANNERS)));
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(ImmutableList.of(
		  new ItemStack(ModItems.AEROBATIC_ELYTRA), new ItemStack(ModItems.AEROBATIC_ELYTRA_WING))));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull TrailRecipeWrapper recipe,
	  @NotNull IIngredients ingredients
	) {
		IFocus<?> focus = layout.getFocus();
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 18, 18);
		stacks.init(1, true, 0, 18);
		stacks.init(2, true, 36, 18);
		stacks.init(3, true, 0, 36);
		stacks.init(4, true, 36, 36);
		stacks.init(5, false, 94, 18);
		
		if (focus != null && (focus.getValue() instanceof AerobaticElytraItem
		                      || focus.getValue() instanceof AerobaticElytraWingItem)) {
			stacks.setOverrideDisplayFocus(null);
		}
		
		final List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focus);
		final List<ItemStack> rockets = recipe.clear? ImmutableList.of(
		  new ItemStack(Items.FIREWORK_ROCKET)) : getRockets();
		stacks.set(0, elytras);
		List<ItemStack> left = null;
		List<ItemStack> right = null;
		List<ItemStack> leftCenter = null;
		List<ItemStack> rightCenter = null;
		if (recipe.left)
			stacks.set(1, left = randomSample(rockets, 1));
		if (recipe.right)
			stacks.set(2, right = randomSample(rockets, 1));
		if (recipe.leftCenter)
			stacks.set(3, leftCenter = randomSample(rockets, 1));
		if (recipe.rightCenter)
			stacks.set(4, rightCenter = randomSample(rockets, 1));
		stacks.set(5, apply(elytras, left, right, leftCenter, rightCenter));
		stacks.addTooltipCallback((i, input, ingredient, tooltip) -> {
			if (1 <= i && i <= 4) {
				tooltip.addAll(splitTtc(
				  "aerobatic-elytra.recipe.trail.applies_to_side",
				  RocketSide.values()[i - 1].getDisplayName().mergeStyle(TextFormatting.GRAY)
				).mergeStyle(TextFormatting.DARK_GRAY));
				if (recipe.clear)
					tooltip.addAll(
					  splitTtc("aerobatic-elytra.recipe.trail.clears_trail")
					    .mergeStyle(TextFormatting.DARK_GRAY));
			}
		});
	}
	
	public static List<ItemStack> apply(
	  List<ItemStack> elytras, @Nullable List<ItemStack> left, @Nullable List<ItemStack> right,
	  @Nullable List<ItemStack> leftCenter, @Nullable List<ItemStack> rightCenter
	) {
		int rocketSize = left != null? left.size() :
		                 right != null ? right.size() :
		                 leftCenter != null ? leftCenter.size() :
		                 rightCenter != null? rightCenter.size() : 0;
		if (rocketSize == 0) return elytras;
		return IntStream.range(0, mcm(elytras.size(), rocketSize)).mapToObj(
		  i -> TrailRecipe.apply(elytras.get(i % elytras.size()), new ItemStack[] {
		    left != null? left.get(i % rocketSize) : EMPTY,
		    right != null? right.get(i % rocketSize) : EMPTY,
		    leftCenter != null? leftCenter.get(i % rocketSize) : EMPTY,
		    rightCenter != null? rightCenter.get(i % rocketSize) : EMPTY
		  })
		).collect(Collectors.toList());
	}
	
	@Override public @NotNull List<ITextComponent> getTooltipStrings(
	  @NotNull TrailRecipeWrapper recipe, double mouseX, double mouseY
	) {
		final List<ITextComponent> tt = super.getTooltipStrings(recipe, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobatic-elytra.jei.help.category.trail"));
		return tt;
	}
	
	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		final Optional<IRecipe<?>> opt = recipeManager.getRecipes().stream()
		  .filter(r -> r instanceof TrailRecipe).findAny();
		if (opt.isPresent()) {
			final TrailRecipe recipe = (TrailRecipe) opt.get();
			reg.addRecipes(Util.make(new ArrayList<>(), l -> {
				l.add(new TrailRecipeWrapper(recipe, true, false, false, false, true));
				l.add(new TrailRecipeWrapper(recipe, true, false, false, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, true, false, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, false, true, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, false, false, true, false));
				l.add(new TrailRecipeWrapper(recipe, true, true, false, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, false, true, true, false));
				l.add(new TrailRecipeWrapper(recipe, true, true, true, true, false));
			}), UID);
		}
	}
	
	public static class TrailRecipeWrapper {
		protected final TrailRecipe recipe;
		protected boolean left;
		protected boolean right;
		protected boolean leftCenter;
		protected boolean rightCenter;
		protected boolean clear;
		
		public TrailRecipeWrapper(
		  TrailRecipe recipe, boolean left, boolean right, boolean leftCenter, boolean rightCenter,
		  boolean clear
		) {
			this.recipe = recipe;
			this.left = left;
			this.right = right;
			this.leftCenter = leftCenter;
			this.rightCenter = rightCenter;
			this.clear = clear;
		}
	}
}