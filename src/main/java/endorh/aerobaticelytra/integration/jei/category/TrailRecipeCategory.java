package endorh.aerobaticelytra.integration.jei.category;

import com.google.common.collect.Sets;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.recipe.TrailRecipe;
import endorh.aerobaticelytra.integration.jei.AbstractContextualRecipeWrapper;
import endorh.aerobaticelytra.integration.jei.ContextualRecipeCategory;
import endorh.aerobaticelytra.integration.jei.category.TrailRecipeCategory.TrailRecipeWrapper;
import endorh.aerobaticelytra.integration.jei.gui.JeiResources;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.IntStream;

import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.aerobaticelytra.common.item.AerobaticElytraItems.AEROBATIC_ELYTRA;
import static endorh.aerobaticelytra.integration.jei.AerobaticElytraJeiHelper.*;
import static endorh.util.text.TextUtil.optSplitTtc;
import static endorh.util.text.TextUtil.splitTtc;
import static mezz.jei.api.recipe.RecipeIngredientRole.INPUT;
import static mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT;

public class TrailRecipeCategory extends BaseCategory<TrailRecipeWrapper>
	implements ContextualRecipeCategory<TrailRecipeWrapper, TrailRecipeCategory> {
	public static final RecipeType<TrailRecipeWrapper> TYPE = RecipeType.create(AerobaticElytra.MOD_ID, "trail", TrailRecipeWrapper.class);
	
	public TrailRecipeCategory() {
		super(TYPE, JeiResources::regular3x3RecipeBg,
		      AEROBATIC_ELYTRA, Items.FIREWORK_ROCKET, false);
	}

	@Override public void setRecipe(
		@NotNull IRecipeLayoutBuilder builder, @NotNull TrailRecipeWrapper recipe, @NotNull IFocusGroup focuses
	) {
		List<ItemStack> elytras;
		EnumMap<RocketSide, List<ItemStack>> rockets = new EnumMap<>(RocketSide.class);

		ItemStack outElytra = getSingleFocusStack(focuses, AEROBATIC_ELYTRA, OUTPUT);
		ItemStack inputRocket = getSingleFocusStack(focuses, Items.FIREWORK_ROCKET, INPUT);
		if (!outElytra.isEmpty()) {
			elytras = recipe.getInputElytras(outElytra);
			for (RocketSide side : recipe.mask)
				rockets.put(side, recipe.getRockets(outElytra, side));
		} else {
			ItemStack inElytra = getSingleFocusStack(focuses, AEROBATIC_ELYTRA, INPUT);
			elytras = inElytra.isEmpty()? getAerobaticElytras() : List.of(inElytra.copy());
			if (!inputRocket.isEmpty()) for (RocketSide side : recipe.mask)
				rockets.put(side, List.of(inputRocket.copy()));
			else for (RocketSide side : recipe.mask)
				rockets.put(side, getRockets(5));
		}

		builder.addSlot(INPUT, 19, 19).addItemStacks(elytras);
		for (RocketSide side : recipe.mask)
			builder.addSlot(INPUT, getSlotX(side), getSlotY(side))
				.addItemStacks(rockets.get(side))
				.addTooltipCallback(getRocketTooltip(side));
		builder.addSlot(OUTPUT, 95, 19).addItemStacks(apply(elytras, rockets));
	}

	private static int getSlotX(RocketSide side) {
		return side.wingSide == WingSide.LEFT? 1 : 37;
	}

	private static int getSlotY(RocketSide side) {
		return side == RocketSide.CENTER_LEFT || side == RocketSide.CENTER_RIGHT? 37 : 19;
	}

	private static @NotNull IRecipeSlotTooltipCallback getRocketTooltip(RocketSide side) {
		return (view, tooltip) -> {
			tooltip.add(Component.empty());
         tooltip.addAll(splitTtc(
            view.getItemStacks().anyMatch(TrailRecipeCategory::hasNoStars)
               ? "aerobaticelytra.recipe.trail.clears_side"
               : "aerobaticelytra.recipe.trail.changes_side",
            side.getDisplayName().withStyle(ChatFormatting.DARK_AQUA)
         ).withStyle(ChatFormatting.GRAY));
      };
	}

	private static boolean hasNoStars(ItemStack rocket) {
		CompoundTag fireworks = rocket.getTagElement("Fireworks");
		return fireworks == null || !fireworks.contains("Explosions");
	}
	
	public static List<ItemStack> apply(
	  List<ItemStack> elytras, Map<RocketSide, List<ItemStack>> rockets
	) {
		int rocketSize = rockets.values().stream().mapToInt(List::size).max().orElse(0);
		if (rocketSize == 0) return elytras;
		return IntStream.range(0, mcm(elytras.size(), rocketSize)).mapToObj(
		  i -> TrailRecipe.apply(elytras.get(i % elytras.size()), new ItemStack[] {
			  getOrDefault(rockets, RocketSide.LEFT, i, ItemStack.EMPTY),
			  getOrDefault(rockets, RocketSide.RIGHT, i, ItemStack.EMPTY),
			  getOrDefault(rockets, RocketSide.CENTER_LEFT, i, ItemStack.EMPTY),
			  getOrDefault(rockets, RocketSide.CENTER_RIGHT, i, ItemStack.EMPTY)
		  })
		).toList();
	}

	private static <K, T> T getOrDefault(Map<K, List<T>> map, K key, int idx, T def) {
		List<T> l = map.get(key);
		return l != null? l.get(idx % l.size()) : def;
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

	@Override public RecipeType<?> getContextualRecipeType() {
		return TYPE;
	}

	@Override
	public List<TrailRecipeWrapper> getContextualRecipes(RecipeManager manager) {
		final Optional<Recipe<?>> opt = manager.getRecipes().stream()
			.filter(r -> r instanceof TrailRecipe).findAny();
		if (opt.isPresent()) return Util.make(new ArrayList<>(), l -> {
			l.add(TrailRecipeWrapper.clear(EnumSet.allOf(RocketSide.class)));
			for (RocketSide side : RocketSide.values())
				l.add(TrailRecipeWrapper.set(EnumSet.of(side)));
			l.add(TrailRecipeWrapper.set(EnumSet.of(RocketSide.LEFT, RocketSide.RIGHT)));
			l.add(TrailRecipeWrapper.set(EnumSet.of(RocketSide.CENTER_LEFT, RocketSide.CENTER_RIGHT)));
			l.add(TrailRecipeWrapper.set(EnumSet.allOf(RocketSide.class)));
		});
		return Collections.emptyList();
	}

	@Override public List<TrailRecipeWrapper> getContextualRecipes(RecipeManager manager, IFocusGroup focuses) {
		ItemStack outElytra = getSingleFocusStack(focuses, AEROBATIC_ELYTRA, OUTPUT);
		if (!outElytra.isEmpty()) {
			TrailData trail = getElytraSpecOrDefault(outElytra).getTrailData();
			if (trail.getUsedSides().isEmpty())
				return Collections.emptyList();
			else return List.of(TrailRecipeWrapper.set(trail.getUsedSides()));
		}
		return ContextualRecipeCategory.super.getContextualRecipes(manager, focuses);
	}

	@Override public void registerRecipes(
	  IRecipeRegistration reg, RecipeManager recipeManager
	) {
		// No need to register recipes, as we use a recipe manager plugin
	}

	public static class TrailRecipeWrapper extends AbstractContextualRecipeWrapper {
		public final Set<RocketSide> mask;
		public final boolean clear;

		protected TrailRecipeWrapper(Set<RocketSide> mask, boolean clear) {
			super(List.of(
				Ingredient.of(AEROBATIC_ELYTRA),
				Ingredient.of(Items.FIREWORK_ROCKET)
			), List.of(Ingredient.of(AEROBATIC_ELYTRA)));
			this.mask = Sets.immutableEnumSet(mask);
			this.clear = clear;
		}

		public static TrailRecipeWrapper set(Set<RocketSide> mask) {
			return new TrailRecipeWrapper(mask, false);
		}
		public static TrailRecipeWrapper clear(Set<RocketSide> mask) {
			return new TrailRecipeWrapper(mask, true);
		}

		@Override public boolean isOutputReachable(ItemStack output) {
			TrailData trail = getElytraSpecOrDefault(output).getTrailData();
			return !clear || trail.getUsedSides().stream().noneMatch(mask::contains);
		}

		@Override public boolean isInputUsable(ItemStack input) {
			if (input.getItem() == Items.FIREWORK_ROCKET) {
				TrailData trail = TrailData.empty();
				trail.readRocket(RocketSide.LEFT, input);
            return clear == (trail.get(RocketSide.LEFT).length == 0);
			}
			return true;
		}

		public List<ItemStack> getRockets(ItemStack outputElytra, RocketSide side) {
			TrailData trail = getElytraSpecOrDefault(outputElytra).getTrailData();
			if (clear && trail.getUsedSides().stream().anyMatch(mask::contains))
				throw new IllegalStateException("Contextual recipe is not reachable!");
			if (!mask.contains(side)) return Collections.emptyList();
			ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
			trail.writeRocket(side, rocket);
			return List.of(rocket);
		}

		public List<ItemStack> getInputElytras(ItemStack outputElytra) {
			TrailData trail = getElytraSpecOrDefault(outputElytra).getTrailData();
			if (clear && trail.getUsedSides().stream().anyMatch(mask::contains))
				throw new IllegalStateException("Contextual recipe is not reachable!");
			List<ItemStack> list = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				ItemStack copy = outputElytra.copy();
				TrailData trailCopy = trail.copy();
				for (RocketSide side: RocketSide.values()) if (mask.contains(side))
               trailCopy.put(side, trailCopy.get(side).length > 0 ? null : makeRocketStars());
				getElytraSpecOrDefault(copy).getTrailData().read(trailCopy.write());
				list.add(copy);
			}
			return list;
		}
	}
}