package endorh.aerobaticelytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.client.ModResources;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.BannerRecipe;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper;
import endorh.aerobaticelytra.integration.jei.category.BannerRecipeCategory.BannerRecipeWrapper;
import endorh.aerobaticelytra.AerobaticElytra;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.*;

public class BannerRecipeCategory extends BaseCategory<BannerRecipeWrapper> {
	public static final ResourceLocation UID = AerobaticElytra.prefix("banner");
	protected static long lastIconChange = 0;
	
	public BannerRecipeCategory() {
		super(UID, BannerRecipeWrapper.class, ModResources::regular3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA, Items.RED_BANNER, true);
	}
	
	@Override public void setIngredients(
	  @NotNull BannerRecipeWrapper recipe, @NotNull IIngredients ingredients
	) {
		ingredients.setInputIngredients(ImmutableList.of(
		  Ingredient.fromItems(ModItems.AEROBATIC_ELYTRA, ModItems.AEROBATIC_ELYTRA_WING),
		  Ingredient.fromTag(ItemTags.BANNERS)));
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(ImmutableList.of(
		  new ItemStack(ModItems.AEROBATIC_ELYTRA), new ItemStack(ModItems.AEROBATIC_ELYTRA_WING))));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull BannerRecipeWrapper recipe,
	  @NotNull IIngredients ingredients
	) {
		IFocus<?> focus = layout.getFocus();
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		stacks.init(0, true, 0, 0);
		stacks.init(1, true, 18, 0);
		stacks.init(2, false, 94, 18);
		
		if (focus != null && (focus.getValue() instanceof AerobaticElytraItem
		                      || focus.getValue() instanceof AerobaticElytraWingItem)) {
			stacks.setOverrideDisplayFocus(null);
		}
		
		List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focus);
		if (recipe.wings)
			elytras = split(elytras).getFirst();
		final List<ItemStack> banners = getBanners();
		stacks.set(0, elytras);
		stacks.set(1, banners);
		stacks.set(2, elytras(new ItemStack(
		  recipe.wings? ModItems.AEROBATIC_ELYTRA_WING : ModItems.AEROBATIC_ELYTRA
		), banners));
		stacks.addTooltipCallback((i, input, ingredient, tooltip) -> {
			if (i == 1)
				tooltip.add(ttc("jei.tooltip.recipe.tag", ItemTags.BANNERS.getName()).mergeStyle(TextFormatting.GRAY));
		});
	}
	
	public static List<ItemStack> elytras(ItemStack elytra, List<ItemStack> patterns) {
		return patterns.stream().map(p -> BannerRecipe.apply(elytra, p)).collect(Collectors.toList());
	}
	
	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		final Optional<IRecipe<?>> opt = recipeManager.getRecipes().stream()
		  .filter(r -> r instanceof BannerRecipe).findAny();
		if (opt.isPresent()) {
			final BannerRecipe recipe = (BannerRecipe) opt.get();
			reg.addRecipes(
			  IntStream.range(0, 2).mapToObj(i -> new BannerRecipeWrapper(recipe, i != 0))
				 .collect(Collectors.toList()), UID);
		}
	}
	
	@Override public @NotNull List<ITextComponent> getTooltipStrings(
	  @NotNull BannerRecipeWrapper recipe, double mouseX, double mouseY
	) {
		final List<ITextComponent> tt = super.getTooltipStrings(recipe, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.banner"));
		return tt;
	}
	
	public static class BannerRecipeWrapper {
		protected final BannerRecipe recipe;
		protected final boolean wings;
		
		public BannerRecipeWrapper(BannerRecipe recipe, boolean wings) {
			this.recipe = recipe;
			this.wings = wings;
		}
	}
	
	@Override public @NotNull IDrawable getIcon() {
		final long t = System.currentTimeMillis();
		if (t - lastIconChange > 1000L) {
			icon = createMultiIngredientDrawable(
			  new ItemStack(iconItems.getFirst()), makeBanner(
				 nextDyeColor(), Util.make(new ArrayList<>(), l -> {
					 for (int i = 0, n = AerobaticElytraJeiHelper.RANDOM.nextInt(4); i < n; i++)
						 l.add(Pair.of(nextPattern(), nextDyeColor()));
				 })));
			lastIconChange = t;
		}
		return icon;
	}
}

