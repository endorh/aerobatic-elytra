package endorh.aerobaticelytra.integration.jei.category;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.recipe.BannerRecipe;
import endorh.aerobaticelytra.integration.jei.AbstractContextualRecipeWrapper;
import endorh.aerobaticelytra.integration.jei.ContextualRecipeCategory;
import endorh.aerobaticelytra.integration.jei.category.BannerRecipeCategory.BannerRecipeWrapper;
import endorh.aerobaticelytra.integration.jei.gui.JeiResources;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.BannerBlock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static endorh.aerobaticelytra.common.item.AerobaticElytraItems.AEROBATIC_ELYTRA;
import static endorh.aerobaticelytra.common.item.AerobaticElytraItems.AEROBATIC_ELYTRA_WING;
import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.optSplitTtc;
import static endorh.util.text.TextUtil.ttc;
import static mezz.jei.api.recipe.RecipeIngredientRole.INPUT;
import static mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT;

public class BannerRecipeCategory extends BaseCategory<BannerRecipeWrapper>
	implements ContextualRecipeCategory<BannerRecipeWrapper, BannerRecipeCategory> {
	public static final RecipeType<BannerRecipeWrapper> TYPE = RecipeType.create(
	  AerobaticElytra.MOD_ID, "banner", BannerRecipeWrapper.class);
	protected static long lastIconChange = 0;
	
	public BannerRecipeCategory() {
		super(TYPE, JeiResources::regular3x3RecipeBg,
		      AEROBATIC_ELYTRA, Items.RED_BANNER, true);
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayoutBuilder builder, @NotNull BannerRecipeWrapper recipe,
	  @NotNull IFocusGroup focuses
	) {
		List<ItemStack> elytras = null;

		ItemStack outElytra = getSingleFocusElytraOrWings(focuses, OUTPUT);
		ItemStack inElytra = getSingleFocusElytraOrWings(focuses, INPUT);
		List<ItemStack> banners = null;
		if (!outElytra.isEmpty()) {
			ElytraDyement dyement = new ElytraDyement();
			dyement.read(outElytra);
			WingDyement wing = dyement.getWing(WingSide.LEFT);
			if (wing.hasPattern) {
				ItemStack stack = new ItemStack(BannerBlock.byColor(wing.basePatternColor));
				wing.write(stack, null);
				banners = List.of(stack);
			}
		} else if (!inElytra.isEmpty()) elytras = List.of(inElytra.copy());

		ItemStack inputBanner = getSingleFocusStack(focuses, i -> i instanceof BannerItem, INPUT);
		if (!inputBanner.isEmpty() && !outElytra.isEmpty())
			banners = List.of(inputBanner);
		if (banners == null)
			banners = getBanners();

		if (elytras == null) {
			elytras = getAerobaticElytras();
			if (recipe.wings) elytras = split(elytras).getFirst();
		}

		builder.addSlot(INPUT, 1, 1).addItemStacks(elytras);
		builder.addSlot(INPUT, 19, 1).addItemStacks(banners).addTooltipCallback(
			(view, tooltip) -> tooltip.add(
				ttc("jei.tooltip.recipe.tag", ItemTags.BANNERS.location())
					.withStyle(ChatFormatting.GRAY)));
		builder.addSlot(OUTPUT, 95, 19).addItemStacks(elytras(elytras, banners));
	}
	
	public static List<ItemStack> elytras(List<ItemStack> elytras, List<ItemStack> patterns) {
		if (patterns.isEmpty()) return elytras;
		return IntStream.range(0, mcm(elytras.size(), patterns.size()))
			.mapToObj(i -> BannerRecipe.apply(elytras.get(i % elytras.size()), patterns.get(i % patterns.size())))
			.toList();
	}

	@Override public RecipeType<?> getContextualRecipeType() {
		return TYPE;
	}

	@Override public List<BannerRecipeWrapper> getContextualRecipes(RecipeManager manager) {
		final Optional<Recipe<?>> opt = manager.getRecipes().stream()
			.filter(r -> r instanceof BannerRecipe).findAny();
		if (opt.isPresent()) return IntStream.range(0, 2)
			.mapToObj(i -> new BannerRecipeWrapper(i != 0))
			.toList();
		return Collections.emptyList();
	}

	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		// No need to register recipes, as we use a recipe manager plugin
	}
	
	@Override public @NotNull List<Component> getTooltipStrings(
	  @NotNull BannerRecipeWrapper recipe, @NotNull IRecipeSlotsView view, double mouseX,
	  double mouseY
	) {
		final List<Component> tt = super.getTooltipStrings(recipe, view, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.banner"));
		return tt;
	}
	
	public static class BannerRecipeWrapper extends AbstractContextualRecipeWrapper {
		protected final boolean wings;
		
		public BannerRecipeWrapper(boolean wings) {
			super(List.of(
				Ingredient.of(wings? AEROBATIC_ELYTRA_WING : AEROBATIC_ELYTRA),
				Ingredient.of(ItemTags.BANNERS)
			), List.of(Ingredient.of(wings? AEROBATIC_ELYTRA_WING : AEROBATIC_ELYTRA)));
			this.wings = wings;
		}

		@Override public boolean isOutputReachable(ItemStack output) {
			ElytraDyement dyement = new ElytraDyement();
			dyement.read(output);
			return (wings || !dyement.hasWingDyement) && dyement.getWing(WingSide.LEFT).hasPattern;
		}

		public ItemStack getInputPattern(@NotNull ItemStack outputElytra) {
			ElytraDyement dyement = new ElytraDyement();
			dyement.read(outputElytra);
			WingDyement wing = dyement.getWing(WingSide.LEFT);
			if (!wings && dyement.hasWingDyement || !wing.hasPattern)
				throw new IllegalStateException("Contextual recipe is not reachable!");
			ItemStack banner = new ItemStack(BannerBlock.byColor(wing.basePatternColor));
			wing.write(banner, null);
			return banner;
		}
	}
	
	@Override public @NotNull IDrawable getIcon() {
		final long t = System.currentTimeMillis();
		if (t - lastIconChange > 1000L) {
			icon = createMultiIngredientDrawable(
			  VanillaTypes.ITEM_STACK,
			  new ItemStack(iconItems.getFirst()), makeBanner(
				 nextDyeColor(), Util.make(new ArrayList<>(), l -> {
					 for (int i = 0, n = RANDOM.nextInt(4); i < n; i++)
						 l.add(Pair.of(nextPattern(), nextDyeColor()));
				 })));
			lastIconChange = t;
		}
		return icon;
	}
}

