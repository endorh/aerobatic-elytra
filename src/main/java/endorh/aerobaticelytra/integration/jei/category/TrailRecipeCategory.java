package endorh.aerobaticelytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.ModResources;
import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.TrailRecipe;
import endorh.aerobaticelytra.integration.jei.category.TrailRecipeCategory.TrailRecipeWrapper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
	public static final RecipeType<TrailRecipeWrapper> TYPE = RecipeType.create(AerobaticElytra.MOD_ID, "trail", TrailRecipeWrapper.class);
	
	public TrailRecipeCategory() {
		super(TYPE, ModResources::regular3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA, Items.FIREWORK_ROCKET, false);
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayoutBuilder builder, @NotNull TrailRecipeWrapper recipe, IFocusGroup focuses
	) {
		final List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focuses.getFocuses(VanillaTypes.ITEM_STACK));
		final List<ItemStack> rockets = recipe.clear? ImmutableList.of(
		  new ItemStack(Items.FIREWORK_ROCKET)) : getRockets();
		builder.addSlot(RecipeIngredientRole.INPUT, 18, 18).addItemStacks(elytras);
		
		List<ItemStack> left = null;
		List<ItemStack> right = null;
		List<ItemStack> leftCenter = null;
		List<ItemStack> rightCenter = null;
		if (recipe.left) builder.addSlot(RecipeIngredientRole.INPUT, 0, 18)
		  .addItemStacks(left = randomSample(rockets, 1))
		  .addTooltipCallback(getRocketTooltip(recipe, RocketSide.LEFT));
		if (recipe.right) builder.addSlot(RecipeIngredientRole.INPUT, 36, 18)
		  .addItemStacks(right = randomSample(rockets, 1))
		  .addTooltipCallback(getRocketTooltip(recipe, RocketSide.RIGHT));
		if (recipe.leftCenter) builder.addSlot(RecipeIngredientRole.INPUT, 0, 36)
		  .addItemStacks(leftCenter = randomSample(rockets, 1))
		  .addTooltipCallback(getRocketTooltip(recipe, RocketSide.CENTER_LEFT));
		if (recipe.rightCenter) builder.addSlot(RecipeIngredientRole.INPUT, 36, 36)
		  .addItemStacks(rightCenter = randomSample(rockets, 1))
		  .addTooltipCallback(getRocketTooltip(recipe, RocketSide.CENTER_RIGHT));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 94, 18)
		  .addItemStacks(apply(elytras, left, right, leftCenter, rightCenter));
	}
	
	@NotNull private static IRecipeSlotTooltipCallback getRocketTooltip(
	  @NotNull TrailRecipeWrapper recipe, RocketSide side
	) {
		return (view, tooltip) -> {
			tooltip.addAll(splitTtc(
			  "aerobaticelytra.recipe.trail.applies_to_side",
			  side.getDisplayName().withStyle(ChatFormatting.GRAY)
			).withStyle(ChatFormatting.DARK_GRAY));
			if (recipe.clear) tooltip.addAll(splitTtc(
			  "aerobaticelytra.recipe.trail.clears_trail"
			).withStyle(ChatFormatting.DARK_GRAY));
		};
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
	  @NotNull TrailRecipeWrapper recipe, @NotNull IRecipeSlotsView view, double mouseX,
	  double mouseY
	) {
		final List<Component> tt = super.getTooltipStrings(recipe, view, mouseX, mouseY);
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
			reg.addRecipes(type, Util.make(new ArrayList<>(), l -> {
				l.add(new TrailRecipeWrapper(recipe, true, false, false, false, true));
				l.add(new TrailRecipeWrapper(recipe, true, false, false, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, true, false, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, false, true, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, false, false, true, false));
				l.add(new TrailRecipeWrapper(recipe, true, true, false, false, false));
				l.add(new TrailRecipeWrapper(recipe, false, false, true, true, false));
				l.add(new TrailRecipeWrapper(recipe, true, true, true, true, false));
			}));
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