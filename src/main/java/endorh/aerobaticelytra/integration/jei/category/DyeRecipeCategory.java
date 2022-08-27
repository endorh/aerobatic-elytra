package endorh.aerobaticelytra.integration.jei.category;

import com.google.common.collect.ImmutableList;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.ModResources;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.DyeRecipe;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.optSplitTtc;
import static endorh.util.text.TextUtil.ttc;
import static java.lang.Math.ceil;
import static java.lang.Math.min;

public class DyeRecipeCategory extends BaseCategory<DyeRecipeCategory.DyeRecipeWrapper> {
	public static final ResourceLocation UID = AerobaticElytra.prefix("dye");
	protected static long lastIconChange = 0;
	
	public DyeRecipeCategory() {
		super(UID, DyeRecipeWrapper.class, ModResources::regular3x3RecipeBg,
		      ModItems.AEROBATIC_ELYTRA, Items.RED_DYE, true);
	}
	
	@Override public void setIngredients(
	  @NotNull DyeRecipeWrapper recipe, @NotNull IIngredients ingredients
	) {
		ingredients.setInputIngredients(ImmutableList.of(
		  Ingredient.of(ModItems.AEROBATIC_ELYTRA, ModItems.AEROBATIC_ELYTRA_WING),
		  DyeRecipeWrapper.dyeIngredient));
		ingredients.setOutputLists(VanillaTypes.ITEM, ImmutableList.of(ImmutableList.of(
		  new ItemStack(ModItems.AEROBATIC_ELYTRA), new ItemStack(ModItems.AEROBATIC_ELYTRA_WING))));
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayout layout, @NotNull DyeRecipeWrapper recipe,
	  @NotNull IIngredients ingredients
	) {
		IFocus<?> focus = layout.getFocus(VanillaTypes.ITEM);
		final IGuiItemStackGroup stacks = layout.getItemStacks();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++)
				stacks.init(i * 3 + j, true, 18 * j, 18 * i);
		}
		stacks.init(9, false, 94, 18);
		
		if (focus != null && (focus.getValue() instanceof AerobaticElytraItem
		                      || focus.getValue() instanceof AerobaticElytraWingItem)) {
			stacks.setOverrideDisplayFocus(null);
		}
		
		List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focus);
		if (recipe.wings)
			elytras = split(elytras).getFirst();
		stacks.set(0, elytras);
		List<List<DyeItem>> dyes = new ArrayList<>();
		for (int i = 0, s = min(recipe.dyeAmount, 8); i < s; i++) {
			final List<ItemStack> d = randomSample(DyeRecipeWrapper.dyeStacks, 4);
			dyes.add(d.stream().map(e -> (DyeItem) e.getItem()).collect(Collectors.toList()));
			stacks.set(i + 1, d);
		}
		stacks.addTooltipCallback((i, input, ingredient, tooltip) -> {
			if (1 <= i && i <= min(recipe.dyeAmount, 8))
				tooltip.add(
				  ttc("jei.tooltip.recipe.tag", "minecraft:dyes").withStyle(ChatFormatting.GRAY));
		});
		stacks.set(9, dye(elytras, dyes));
	}
	
	public static List<ItemStack> dye(List<ItemStack> stacks, List<List<DyeItem>> dyes) {
		if (dyes.isEmpty())
			return stacks;
		return IntStream.range(0, AerobaticElytraJeiHelper.mcm(stacks.size(), dyes.get(0).size()))
		  .mapToObj(i ->
			           DyeRecipe.dye(stacks.get(i % stacks.size()), dyes.stream()
				          .map(l -> l.get(i % l.size())).collect(Collectors.toList()))
		  ).collect(Collectors.toList());
	}
	
	@Override public @NotNull List<Component> getTooltipStrings(
	  @NotNull DyeRecipeWrapper recipe, double mouseX, double mouseY
	) {
		final List<Component> tt = super.getTooltipStrings(recipe, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.dye"));
		return tt;
	}
	
	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		final Optional<Recipe<?>> opt = recipeManager.getRecipes().stream()
		  .filter(r -> r instanceof DyeRecipe).findAny();
		if (opt.isPresent()) {
			final DyeRecipe recipe = (DyeRecipe) opt.get();
			reg.addRecipes(
			  IntStream.range(1, 17).mapToObj(
				 i -> new DyeRecipeWrapper(recipe, (int) ceil(i / 2.0), i % 2 == 0)
			  ).collect(Collectors.toList()), UID);
		}
	}
	
	public static class DyeRecipeWrapper {
		protected static final List<DyeItem> dyeItems =
		  Arrays.stream(DyeColor.values()).map(DyeItem::byColor).collect(Collectors.toList());
		protected static final Ingredient dyeIngredient =
		  Ingredient.of(dyeItems.toArray(new DyeItem[0]));
		protected static final List<ItemStack> dyeStacks =
		  Arrays.asList(dyeIngredient.getItems());
		protected final DyeRecipe recipe;
		protected final int dyeAmount;
		protected final boolean wings;
		
		public DyeRecipeWrapper(DyeRecipe recipe, int dyeAmount, boolean wings) {
			this.recipe = recipe;
			this.dyeAmount = dyeAmount;
			this.wings = wings;
		}
		
		public List<Ingredient> getIngredients() {
			return Util.make(new ArrayList<>(), l -> {
				l.add(Ingredient.of(ModItems.AEROBATIC_ELYTRA));
				for (int i = 0; i < dyeAmount; i++)
					l.add(dyeIngredient);
			});
		}
	}
	
	@Override public @NotNull IDrawable getIcon() {
		final long t = System.currentTimeMillis();
		if (t - lastIconChange > 1000L) {
			icon = createMultiIngredientDrawable(
			  new ItemStack(iconItems.getFirst()), new ItemStack(DyeItem.byColor(nextDyeColor())));
			lastIconChange = t;
		}
		return icon;
	}
}
