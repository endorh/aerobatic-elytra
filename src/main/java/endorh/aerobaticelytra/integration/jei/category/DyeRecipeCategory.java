package endorh.aerobaticelytra.integration.jei.category;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.recipe.DyeRecipe;
import endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper;
import endorh.aerobaticelytra.integration.jei.gui.JeiResources;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
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
	public static final RecipeType<DyeRecipeWrapper> TYPE = RecipeType.create(
	  AerobaticElytra.MOD_ID, "dye", DyeRecipeWrapper.class);
	protected static long lastIconChange = 0;
	
	public DyeRecipeCategory() {
		super(TYPE, JeiResources::regular3x3RecipeBg, AerobaticElytraItems.AEROBATIC_ELYTRA, Items.RED_DYE, true);
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayoutBuilder builder, @NotNull DyeRecipeWrapper recipe, @NotNull IFocusGroup focuses
	) {
		List<IRecipeSlotBuilder> slots = new ArrayList<>(10);
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++)
				slots.add(builder.addSlot(RecipeIngredientRole.INPUT, 18 * j, 18 * i));
		}
		
		List<ItemStack> elytras = getAerobaticElytrasMatchingFocus(focuses.getFocuses(VanillaTypes.ITEM_STACK));
		if (recipe.wings)
			elytras = split(elytras).getFirst();
		slots.get(0).addItemStacks(elytras);
		List<List<DyeItem>> dyes = new ArrayList<>();
		for (int i = 0, s = min(recipe.dyeAmount, 8); i < s; i++) {
			final List<ItemStack> d = randomSample(DyeRecipeWrapper.dyeStacks, 4);
			dyes.add(d.stream().map(e -> (DyeItem) e.getItem()).collect(Collectors.toList()));
			slots.get(i + 1).addItemStacks(d);
		}
		IntStream.range(1, min(recipe.dyeAmount + 1, 9)).mapToObj(slots::get).forEach(
		  s -> s.addTooltipCallback((view, tooltip) -> tooltip.add(
			 ttc("jei.tooltip.recipe.tag", "minecraft:dyes").withStyle(ChatFormatting.GRAY))));
		
		builder.addSlot(RecipeIngredientRole.OUTPUT, 94, 18).addItemStacks(dye(elytras, dyes));
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
	  @NotNull DyeRecipeWrapper recipe, @NotNull IRecipeSlotsView view, double mouseX, double mouseY
	) {
		final List<Component> tt = super.getTooltipStrings(recipe, view, mouseX, mouseY);
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
			  type, IntStream.range(1, 17).mapToObj(
				 i -> new DyeRecipeWrapper(recipe, (int) ceil(i / 2.0), i % 2 == 0)
			  ).collect(Collectors.toList()));
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
				l.add(Ingredient.of(AerobaticElytraItems.AEROBATIC_ELYTRA));
				for (int i = 0; i < dyeAmount; i++)
					l.add(dyeIngredient);
			});
		}
	}
	
	@Override public @NotNull IDrawable getIcon() {
		final long t = System.currentTimeMillis();
		if (t - lastIconChange > 1000L) {
			icon = createMultiIngredientDrawable(
			  VanillaTypes.ITEM_STACK,
			  new ItemStack(iconItems.getFirst()), new ItemStack(DyeItem.byColor(nextDyeColor())));
			lastIconChange = t;
		}
		return icon;
	}
}
