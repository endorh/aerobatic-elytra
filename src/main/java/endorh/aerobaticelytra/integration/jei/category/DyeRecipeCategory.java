package endorh.aerobaticelytra.integration.jei.category;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.recipe.DyeRecipe;
import endorh.aerobaticelytra.integration.jei.AbstractContextualRecipeWrapper;
import endorh.aerobaticelytra.integration.jei.ContextualRecipeCategory;
import endorh.aerobaticelytra.integration.jei.DyeMixGenerator;
import endorh.aerobaticelytra.integration.jei.DyeMixGenerator.DyeMix;
import endorh.aerobaticelytra.integration.jei.gui.JeiResources;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static endorh.aerobaticelytra.common.item.AerobaticElytraItems.AEROBATIC_ELYTRA;
import static endorh.aerobaticelytra.common.item.AerobaticElytraItems.AEROBATIC_ELYTRA_WING;
import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.optSplitTtc;
import static endorh.util.text.TextUtil.ttc;
import static java.lang.Math.min;
import static mezz.jei.api.recipe.RecipeIngredientRole.INPUT;
import static mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT;

public class DyeRecipeCategory extends BaseCategory<DyeRecipeCategory.DyeRecipeWrapper>
	implements ContextualRecipeCategory<DyeRecipeCategory.DyeRecipeWrapper, DyeRecipeCategory> {
	public static final RecipeType<DyeRecipeWrapper> TYPE = RecipeType.create(
	  AerobaticElytra.MOD_ID, "dye", DyeRecipeWrapper.class);
	protected static long lastIconChange = 0;
	
	public DyeRecipeCategory() {
		super(TYPE, JeiResources::regular3x3RecipeBg, AEROBATIC_ELYTRA, Items.RED_DYE, true);
	}
	
	@Override public void setRecipe(
	  @NotNull IRecipeLayoutBuilder builder, @NotNull DyeRecipeWrapper recipe, @NotNull IFocusGroup focuses
	) {
		List<IRecipeSlotBuilder> slots = new ArrayList<>(10);
		for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++)
			slots.add(builder.addSlot(INPUT, 18 * j, 18 * i));

		List<ItemStack> elytras = null;
		List<List<ItemStack>> dyes;
		ItemStack out = getSingleFocusElytraOrWings(focuses, OUTPUT);
		if (!out.isEmpty()) {
			List<DyeMix> mixes = recipe.makeMixes(out, 5);
			elytras = IntStream.range(0, 5).mapToObj(
				i -> recipe.dyementFromRemainder(mixes.get(i), i)
			).map(d -> {
				ItemStack stack = new ItemStack(AEROBATIC_ELYTRA);
				d.write(stack);
				return stack;
         }).toList();
			if (recipe.wings) elytras = split(elytras).getFirst();
			dyes = mixes.stream().map(DyeMix::getDyeStacks).toList();
		} else {
			ItemStack inputElytra = getSingleFocusElytraOrWings(focuses, INPUT);
			if (!inputElytra.isEmpty()) elytras = List.of(inputElytra.copy());
			ItemStack inputDye = getSingleFocusStack(focuses, i -> i instanceof DyeItem, INPUT);
			dyes = IntStream.range(0, 5).<List<ItemStack>>mapToObj(i -> Util.make(new ArrayList<>(), l -> {
				if (!inputDye.isEmpty()) l.add(inputDye.copy());
				for (int j = inputDye.isEmpty()? 0 : 1; j < recipe.dyeAmount; j++) l.add(
					new ItemStack(DyeItem.byColor(nextDyeColor())));
			})).toList();
		}
		if (elytras == null) {
			elytras = getAerobaticElytras();
			if (recipe.wings) elytras = split(elytras).getFirst();
		}

		slots.get(0).addItemStacks(elytras);
		for (int i = 0, s = min(recipe.dyeAmount, 8); i < s; i++)
         for (List<ItemStack> mix : dyes)
            slots.get(i + 1).addItemStack(mix.get(i));
		IntStream.range(1, min(recipe.dyeAmount + 1, 9)).mapToObj(slots::get).forEach(
		  s -> s.addTooltipCallback((view, tooltip) -> tooltip.add(
			 ttc("jei.tooltip.recipe.tag", "minecraft:dyes").withStyle(ChatFormatting.GRAY))));
		
		builder.addSlot(OUTPUT, 95, 19).addItemStacks(dye(elytras, dyes));
	}
	
	public static List<ItemStack> dye(List<ItemStack> stacks, List<List<ItemStack>> dyes) {
		if (dyes.isEmpty())
			return stacks;
		return IntStream.range(0, mcm(stacks.size(), dyes.get(0).size()))
			.mapToObj(i -> DyeRecipe.dye(stacks.get(i % stacks.size()), dyes.stream()
				.map(l -> (DyeItem) l.get(i % l.size()).getItem()).collect(Collectors.toList())))
			.toList();
	}
	
	@Override public @NotNull List<Component> getTooltipStrings(
	  @NotNull DyeRecipeWrapper recipe, @NotNull IRecipeSlotsView view, double mouseX, double mouseY
	) {
		final List<Component> tt = super.getTooltipStrings(recipe, view, mouseX, mouseY);
		if (inRect(mouseX, mouseY, 61, 19, 22, 15))
			tt.addAll(optSplitTtc("aerobaticelytra.jei.help.category.dye"));
		return tt;
	}

	@Override public RecipeType<?> getContextualRecipeType() {
		return TYPE;
	}

	@Override public List<DyeRecipeWrapper> getContextualRecipes(RecipeManager manager) {
		final Optional<Recipe<?>> opt = manager.getRecipes().stream()
			.filter(r -> r instanceof DyeRecipe).findAny();
		if (opt.isPresent()) return IntStream.range(0, 16)
			.mapToObj(i -> new DyeRecipeWrapper(i / 2 + 1, i % 2 == 1))
			.toList();
		return Collections.emptyList();
	}

	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		// No need to register recipes, as we use a recipe manager plugin
	}
	
	public static class DyeRecipeWrapper extends AbstractContextualRecipeWrapper {
		private static final DyeMixGenerator MIX_GEN = new DyeMixGenerator();

		protected static final List<DyeItem> dyeItems =
		  Arrays.stream(DyeColor.values()).map(DyeItem::byColor).collect(Collectors.toList());
		protected static final Ingredient dyeIngredient =
		  Ingredient.of(dyeItems.toArray(new DyeItem[0]));
		protected final int dyeAmount;
		protected final boolean wings;
		
		public DyeRecipeWrapper(int dyeAmount, boolean wings) {
			super(List.of(
				Ingredient.of(wings? AEROBATIC_ELYTRA_WING : AEROBATIC_ELYTRA), dyeIngredient
			), List.of(Ingredient.of(wings? AEROBATIC_ELYTRA_WING : AEROBATIC_ELYTRA)));
			this.dyeAmount = dyeAmount;
			this.wings = wings;
		}
		
		public List<Ingredient> getIngredients() {
			return Util.make(new ArrayList<>(), l -> {
				l.add(Ingredient.of(AEROBATIC_ELYTRA));
				for (int i = 0; i < dyeAmount; i++)
					l.add(dyeIngredient);
			});
		}

		@Override public boolean isOutputReachable(ItemStack output) {
			ElytraDyement dyement = new ElytraDyement();
			dyement.read(output);
			return !dyement.hasWingDyement && dyement.getWing(WingSide.LEFT).hasColor;
		}

		public List<DyeMix> makeMixes(ItemStack outputElytra, int amount) {
			ElytraDyement dyement = new ElytraDyement();
			dyement.read(outputElytra);
			if (dyement.hasWingDyement || !dyement.getWing(WingSide.LEFT).hasColor)
				throw new IllegalStateException("Contextual recipe is not applicable");
			int color = dyement.getWing(WingSide.LEFT).color;
			return IntStream.range(0, amount).mapToObj(i -> MIX_GEN.generateMix(color, dyeAmount))
				.sorted(Comparator.comparing(m -> m.error() - (m.remainder() == null ? 1F : 0F)))
				.toList();
		}

		public ElytraDyement dyementFromRemainder(DyeMix mix, int parts) {
			Integer remainder = mix.remainder();
			ElytraDyement dyement = new ElytraDyement();
			if (remainder == null) return dyement;
			if (parts <= 1 || parts == 2 && wings) {
            dyement.setColor(remainder);
         } else if (parts == 2) {
				int[] pair = MIX_GEN.splitRemainder(remainder);
				dyement.getWing(WingSide.LEFT).setColor(pair[0]);
				dyement.getWing(WingSide.RIGHT).setColor(pair[1]);
			} else {
				List<DyeColor> colors = MIX_GEN.separateRemainderIntoDyes(remainder, parts);
				if (parts % 2 == 0 && !wings) {
					dyement.getWing(WingSide.LEFT).setPattern(
						colors.get(0), IntStream.range(0, colors.size() / 2)
							.mapToObj(i -> Pair.of(nextPattern(), colors.get(i))).toList());
					dyement.getWing(WingSide.RIGHT).setPattern(
						colors.get(colors.size() / 2), IntStream.range(colors.size() / 2 + 1, colors.size())
							.mapToObj(i -> Pair.of(nextPattern(), colors.get(i))).toList());
				} else {
					dyement.setPattern(colors.get(0), IntStream.range(1, colors.size())
						.mapToObj(i -> Pair.of(nextPattern(), colors.get(i))).toList());
				}
			}
			return dyement;
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
