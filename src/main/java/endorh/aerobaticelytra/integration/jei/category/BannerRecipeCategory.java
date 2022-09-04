package endorh.aerobaticelytra.integration.jei.category;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.AerobaticElytraResources;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.recipe.BannerRecipe;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper;
import endorh.aerobaticelytra.integration.jei.category.BannerRecipeCategory.BannerRecipeWrapper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.optSplitTtc;
import static endorh.util.text.TextUtil.ttc;

public class BannerRecipeCategory extends BaseCategory<BannerRecipeWrapper> {
	public static final RecipeType<BannerRecipeWrapper> TYPE = RecipeType.create(
	  AerobaticElytra.MOD_ID, "banner", BannerRecipeWrapper.class);
	protected static long lastIconChange = 0;
	
	public BannerRecipeCategory() {
		super(TYPE, AerobaticElytraResources::regular3x3RecipeBg,
		      AerobaticElytraItems.AEROBATIC_ELYTRA, Items.RED_BANNER, true);
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayoutBuilder builder, @NotNull BannerRecipeWrapper recipe,
	  @NotNull IFocusGroup focuses
	) {
		List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focuses.getFocuses(VanillaTypes.ITEM_STACK));
		if (recipe.wings) elytras = split(elytras).getFirst();
		final List<ItemStack> banners = getBanners();
		
		builder.addSlot(RecipeIngredientRole.INPUT, 0, 0)
		  .addItemStacks(elytras);
		builder.addSlot(RecipeIngredientRole.INPUT, 18, 0)
		  .addItemStacks(banners).addTooltipCallback(
		    (view, tooltip) -> tooltip.add(ttc("jei.tooltip.recipe.tag", ItemTags.BANNERS.location()).withStyle(ChatFormatting.GRAY)));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 94, 18)
		  .addItemStacks(elytras(
			 new ItemStack(recipe.wings? AerobaticElytraItems.AEROBATIC_ELYTRA_WING : AerobaticElytraItems.AEROBATIC_ELYTRA),
			 banners));
	}
	
	public static List<ItemStack> elytras(ItemStack elytra, List<ItemStack> patterns) {
		return patterns.stream().map(p -> BannerRecipe.apply(elytra, p)).collect(Collectors.toList());
	}
	
	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		final Optional<Recipe<?>> opt = recipeManager.getRecipes().stream()
		  .filter(r -> r instanceof BannerRecipe).findAny();
		if (opt.isPresent()) {
			final BannerRecipe recipe = (BannerRecipe) opt.get();
			reg.addRecipes(
			  type, IntStream.range(0, 2)
				 .mapToObj(i -> new BannerRecipeWrapper(recipe, i != 0))
				 .collect(Collectors.toList()));
		}
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
			  VanillaTypes.ITEM_STACK,
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

