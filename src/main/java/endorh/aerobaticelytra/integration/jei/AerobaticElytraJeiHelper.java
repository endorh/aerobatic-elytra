package endorh.aerobaticelytra.integration.jei;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.capability.IElytraSpec.RocketStar;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.item.IAbility.Ability;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.JoinRecipe;
import endorh.aerobaticelytra.common.recipe.TrailRecipe;
import endorh.aerobaticelytra.integration.jei.gui.MultiIngredientDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.block.BannerBlock;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public class AerobaticElytraJeiHelper {
	public static final Random RANDOM = new Random();
	protected static final ElytraDyement dyement = new ElytraDyement();
	
	public static List<ItemStack> getAerobaticElytras() {
		return Util.make(new ArrayList<>(), l -> {
			for (int i = 0; i < 5; i++) {
				l.add(new ItemStack(ModItems.AEROBATIC_ELYTRA));
				l.add(makeElytra(nextDyeColor()));
				l.add(makeElytra(nextDyeColor(), nextDyeColor()));
				l.add(makeElytra(
				  nextDyeColor(),
				  Pair.of(nextPattern(), nextDyeColor()),
				  Pair.of(nextPattern(), nextDyeColor()),
				  Pair.of(nextPattern(), nextDyeColor()),
				  Pair.of(nextPattern(), nextDyeColor())));
				l.add(makeElytra(
				  nextDyeColor(),
				  Util.make(new ArrayList<>(), w -> {
					  w.add(Pair.of(nextPattern(), nextDyeColor()));
					  w.add(Pair.of(nextPattern(), nextDyeColor()));
				  }),
				  nextDyeColor(),
				  Util.make(new ArrayList<>(), w -> {
					  w.add(Pair.of(nextPattern(), nextDyeColor()));
					  w.add(Pair.of(nextPattern(), nextDyeColor()));
				  })
				));
			}
			for (int i = 0; i < l.size(); i++) {
				if (RANDOM.nextFloat() > 0.5F)
					l.set(i, TrailRecipe.apply(l.get(i), new ItemStack[]{makeRocket(), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY}));
				if (RANDOM.nextFloat() > 0.5F)
					l.set(i, TrailRecipe.apply(l.get(i), new ItemStack[]{ItemStack.EMPTY, makeRocket(), ItemStack.EMPTY, ItemStack.EMPTY}));
				if (RANDOM.nextFloat() > 0.8F)
					l.set(i, TrailRecipe.apply(l.get(i), new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, makeRocket(), ItemStack.EMPTY}));
				if (RANDOM.nextFloat() > 0.8F)
					l.set(i, TrailRecipe.apply(l.get(i), new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, makeRocket()}));
				final IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(l.get(i));
				spec.setAbility(Ability.MAX_FUEL, 40F);
				spec.setAbility(Ability.FUEL, 30F);
			}
		});
	}
	
	public static DyeColor nextDyeColor() {
		return DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)];
	}
	
	public static BannerPattern nextPattern() {
		return BannerPattern.values()[RANDOM.nextInt(BannerPattern.values().length - 1) + 1];
	}
	
	public static ItemStack makeElytra(DyeColor color) {
		final ItemStack e = new ItemStack(ModItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.setColor(color.getColorValue());
		dyement.write(e);
		return e;
	}
	
	public static ItemStack makeElytra(DyeColor leftColor, DyeColor rightColor) {
		final ItemStack e = new ItemStack(ModItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.getWing(WingSide.LEFT).setColor(leftColor.getColorValue());
		dyement.getWing(WingSide.RIGHT).setColor(rightColor.getColorValue());
		dyement.write(e);
		return e;
	}
	
	@SafeVarargs
	public static ItemStack makeElytra(DyeColor base, Pair<BannerPattern, DyeColor>... patterns) {
		final ItemStack e = new ItemStack(ModItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.setPattern(base, Arrays.asList(patterns));
		dyement.write(e);
		return e;
	}
	
	public static ItemStack makeElytra(
	  DyeColor baseLeft, List<Pair<BannerPattern, DyeColor>> leftPatterns,
	  DyeColor baseRight, List<Pair<BannerPattern, DyeColor>> rightPatterns
	) {
		final ItemStack e = new ItemStack(ModItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.getWing(WingSide.LEFT).setPattern(baseLeft, leftPatterns);
		dyement.getWing(WingSide.RIGHT).setPattern(baseRight, rightPatterns);
		dyement.write(e);
		return e;
	}
	
	public static Pair<ItemStack, ItemStack> split(ItemStack elytra) {
		if (!(elytra.getItem() instanceof AerobaticElytraItem))
			throw new IllegalArgumentException("Cannot split non-elytra item");
		final AerobaticElytraItem item = (AerobaticElytraItem) elytra.getItem();
		return Pair.of(
		  item.getWing(elytra, WingSide.LEFT), item.getWing(elytra, WingSide.RIGHT));
	}
	
	public static Pair<List<ItemStack>, List<ItemStack>> split(List<ItemStack> elytras) {
		final ArrayList<ItemStack> left = new ArrayList<>(), right = new ArrayList<>();
		elytras.stream().map(AerobaticElytraJeiHelper::split).forEachOrdered(p -> {
		  left.add(p.getFirst()); right.add(p.getSecond());});
		return Pair.of(left, right);
	}
	
	public static ItemStack makeBanner(DyeColor base, List<Pair<BannerPattern, DyeColor>> patterns) {
		final ItemStack banner = new ItemStack(BannerBlock.forColor(base).asItem());
		dyement.read(banner);
		dyement.setPattern(base, patterns);
		dyement.write(banner);
		return banner;
	}
	
	public static List<ItemStack> getBanners() {
		return Util.make(new ArrayList<>(), l -> {
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 4; j++) {
					final int fj = j;
					l.add(makeBanner(nextDyeColor(), Util.make(new ArrayList<>(), p -> {
						for (int k = 1; k <= fj; k++)
							p.add(Pair.of(nextPattern(), nextDyeColor()));
					})));
				}
			}
		});
	}
	
	public static List<DyeColor> getDyeColors(int size) {
		return Util.make(new ArrayList<>(), l -> {
			for (int i = 0; i < size; i++)
				l.add(nextDyeColor());
		});
	}
	
	public static ItemStack makeRocket(List<RocketStar> stars) {
		final ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
		final CompoundNBT fireworks = rocket.getOrCreateChildTag("Fireworks");
		fireworks.put("Explosions", RocketStar.listAsNBT(stars.toArray(new RocketStar[0])));
		return rocket;
	}
	
	public static ItemStack makeRocket() {
		return makeRocket(makeRocketStars());
	}
	
	public static List<RocketStar> makeRocketStars() {
		return Util.make(new ArrayList<>(), s -> {
			for (int j = 0, e = RANDOM.nextInt(2) + 1; j < e; j++)
				s.add(new RocketStar(
				  getDyeColors(RANDOM.nextInt(3)),
				  getDyeColors(RANDOM.nextInt(3)),
				  RANDOM.nextFloat() > 0.7F, RANDOM.nextFloat() > 0.7F,
				  (byte) RANDOM.nextInt(5)));
		});
	}
	
	public static List<ItemStack> getRockets() {
		return Util.make(new ArrayList<>(), l -> {
			for (int i = 0; i < 20; i++)
				l.add(makeRocket());
		});
	}
	
	public static List<ItemStack> getAerobaticElytrasMatchingFocus(
	  IFocus<?> focus
	) {
		if (focus != null) {
			final ItemStack focusStack = (ItemStack) focus.getValue();
			if (focusStack.getItem() instanceof AerobaticElytraItem) {
				final ItemStack elytra = focusStack.copy();
				elytra.setDamage(0);
				return ImmutableList.of(elytra);
			} else if (focusStack.getItem() instanceof AerobaticElytraWingItem) {
				final ItemStack right = focusStack.copy();
				right.setDamage(0);
				final List<ItemStack> leftWings = getAerobaticElytras().stream()
				  .map(s -> ((AerobaticElytraItem) s.getItem()).getWing(s, WingSide.LEFT))
				  .collect(Collectors.toList());
				leftWings.add(0, right.copy());
				return leftWings.stream().map(w -> {
					ItemStack l = right.copy();
					dyement.read(w);
					dyement.write(l);
					return JoinRecipe.join(l, right);
				}).collect(Collectors.toList());
			}
		}
		return getAerobaticElytras();
	}
	
	public static <T> List<T> randomSample(List<T> pool, int copies) {
		List<T> result = new ArrayList<>();
		for (int i = 0, s = copies * pool.size(); i < s; i++)
			result.add(pool.get(RANDOM.nextInt(pool.size())));
		return result;
	}
	
	public static int gcd(int a, int b) { return b == 0 ? a : gcd(b, a % b); }
	public static int mcm(int a, int b) { return a * b / gcd(a, b); }
	
	public static <V> IDrawable createMultiIngredientDrawable(V first, V second) {
		return new MultiIngredientDrawable<>(
		  first, second, AerobaticElytraJeiPlugin.ingredientManager.getIngredientRenderer(first));
	}
}
