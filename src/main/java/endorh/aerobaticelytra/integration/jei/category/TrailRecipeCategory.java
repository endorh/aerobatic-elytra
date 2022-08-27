package endorh.aerobaticelytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.ModResources;
import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.TrailRecipe;
import endorh.aerobaticelytra.integration.jei.category.TrailRecipeCategory.TrailRecipeWrapper;
import endorh.util.text.TextUtil;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.optSplitTtc;
import static endorh.util.text.TextUtil.splitTtc;

public class TrailRecipeCategory extends BaseCategory<TrailRecipeWrapper> {
	public static final ResourceLocation UID = AerobaticElytra.prefix("trail");
	
	public TrailRecipeCategory() {
		super(UID, TrailRecipeWrapper.class, ModResources::regular3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA, Items.FIREWORK_ROCKET, false);
	}
	
	@Override public void setIngredients(
	  @NotNull TrailRecipeWrapper recipe, @NotNull IIngredients ingredients
	) {
		ingredients.setInputIngredients(ImmutableList.of(
		  Ingredient.of(ModItems.AEROBATIC_ELYTRA, ModItems.AEROBATIC_ELYTRA_WING),
		  Ingredient.of(ItemTags.BANNERS)));
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(ImmutableList.of(
		  new ItemStack(ModItems.AEROBATIC_ELYTRA), new ItemStack(ModItems.AEROBATIC_ELYTRA_WING))));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull TrailRecipeWrapper recipe,
	  @NotNull IIngredients ingredients
	) {
		IFocus<?> focus = layout.getFocus(VanillaTypes.ITEM);
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
				tooltip.addAll(TextUtil.splitTtc(
				  "aerobaticelytra.recipe.trail.applies_to_side",
				  RocketSide.values()[i - 1].getDisplayName().withStyle(ChatFormatting.GRAY)
				).withStyle(ChatFormatting.DARK_GRAY));
				if (recipe.clear)
					tooltip.addAll(
					  splitTtc("aerobaticelytra.recipe.trail.clears_trail")
					    .withStyle(ChatFormatting.DARK_GRAY));
			}
		});
	}
	
	public static List<ItemStack> apply(
	  List<ItemStack> elytras, @Nullable List<ItemStack> left, @Nullable List<ItemStack> right,
	  @Nullable List<ItemStack> leftCenter, @Nullable List<ItemStack> rightCenter
	) {
		int rocketSize = left != null? left.size() :
		                 right != null? right.size() :
		                 leftCenter != null? leftCenter.size() :
		                 rightCenter != null? rightCenter.size() : 0;
		if (rocketSize == 0) return elytras;
		return IntStream.range(0, mcm(elytras.size(), rocketSize)).mapToObj(
		  i -> TrailRecipe.apply(elytras.get(i % elytras.size()), new ItemStack[] {
		    left != null? left.get(i % rocketSize) : ItemStack.EMPTY,
		    right != null? right.get(i % rocketSize) : ItemStack.EMPTY,
		    leftCenter != null? leftCenter.get(i % rocketSize) : ItemStack.EMPTY,
		    rightCenter != null? rightCenter.get(i % rocketSize) : ItemStack.EMPTY
		  })
		).collect(Collectors.toList());
	}
	
	@Override public @NotNull List<Component> getTooltipStrings(
	  @NotNull TrailRecipeWrapper recipe, double mouseX, double mouseY
	) {
		final List<Component> tt = super.getTooltipStrings(recipe, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.trail"));
		return tt;
	}
	
	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		final Optional<Recipe<?>> opt = recipeManager.getRecipes().stream()
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