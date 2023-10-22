package endorh.aerobaticelytra.integration.jei;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.capability.IElytraSpec.RocketStar;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.item.IAbility.Ability;
import endorh.aerobaticelytra.common.recipe.JoinRecipe;
import endorh.aerobaticelytra.integration.jei.gui.MultiIngredientDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static mezz.jei.api.recipe.RecipeIngredientRole.INPUT;
import static mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT;

@SuppressWarnings("SameParameterValue")
public class AerobaticElytraJeiHelper {
	public static final RandomSource RANDOM = new LegacyRandomSource(0);
	protected static final ElytraDyement dyement = new ElytraDyement();

	public static List<ItemStack> getAerobaticElytras() {
		return getAerobaticElytras(null, null, null);
	}
	public static List<ItemStack> getAerobaticElytras(InputDyementQuery dyementDataQuery) {
		return getAerobaticElytras(dyementDataQuery, null, null);
	}
	public static List<ItemStack> getAerobaticElytras(TrailDataQuery trailDataQuery) {
		return getAerobaticElytras(null, trailDataQuery, null);
	}

	public static List<ItemStack> getAerobaticElytras(@Nullable AerobaticElytraJeiHelper.InputDyementQuery dyementQuery, @Nullable TrailDataQuery trailQuery, IFocusGroup focus) {
		List<ItemStack> list = new ArrayList<>();
		// Plain elytra
		for (int i = 0; i < 5; i++) {
			list.add(new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA));
			list.add(makeElytra(nextDyeColor()));
			list.add(makeElytra(nextDyeColor(), nextDyeColor()));
			list.add(makeElytra(
				nextDyeColor(),
				Pair.of(nextPattern(), nextDyeColor()),
				Pair.of(nextPattern(), nextDyeColor()),
				Pair.of(nextPattern(), nextDyeColor()),
				Pair.of(nextPattern(), nextDyeColor())));
			list.add(makeElytra(
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
		for (ItemStack itemStack : list) {
			IElytraSpec spec = getElytraSpecOrDefault(itemStack);
			TrailData data = getTrailData(trailQuery);
			spec.getTrailData().read(data.write());
			spec.setAbility(Ability.MAX_FUEL, 50F);
			spec.setAbility(Ability.FUEL, 30F);
		}
		return list;
	}

	public static ItemStack applyDyement(ItemStack elytra, ElytraDyement dyement) {
		ItemStack copy = elytra.copy();
		dyement.write(copy);
		return copy;
	}

	public static ItemStack applyTrail(ItemStack elytra, TrailData trail) {
		ItemStack copy = elytra.copy();
		TrailData trailData = getElytraSpecOrDefault(copy).getTrailData();
		trailData.read(trail.write());
		return copy;
	}


	public static List<ElytraDyement> makeDyements() {
		List<ElytraDyement> list = new ArrayList<>();
		list.add(new ElytraDyement());
		list.add(Util.make(new ElytraDyement(), d -> d.setColor(nextDyeColor().getTextColor())));
		list.add(Util.make(new ElytraDyement(), d -> {
			d.getWing(WingSide.LEFT).setColor(nextDyeColor().getTextColor());
			d.getWing(WingSide.RIGHT).setColor(nextDyeColor().getTextColor());
		}));
		list.add(Util.make(new ElytraDyement(), d -> d.setPattern(nextDyeColor(), nextPatternLayers())));
		list.add(Util.make(new ElytraDyement(), d -> {
			d.getWing(WingSide.LEFT).setPattern(nextDyeColor(), nextPatternLayers());
			d.getWing(WingSide.RIGHT).setPattern(nextDyeColor(), nextPatternLayers());
		}));
		return list;
	}
	
	public static DyeColor nextDyeColor() {
		return DyeColor.values()[RANDOM.nextInt(DyeColor.values().length)];
	}
	
	public static BannerPattern nextPattern() {
		return BuiltInRegistries.BANNER_PATTERN.getRandom(RANDOM).orElseThrow(
		  () -> new IllegalStateException("No banner patterns registered")).get();
	}

	public static List<Pair<BannerPattern, DyeColor>> nextPatternLayers() {
		List<Pair<BannerPattern, DyeColor>> list = new ArrayList<>();
		for (int i = 0, l = RANDOM.nextInt(2, 6); i < l; i++)
			list.add(Pair.of(nextPattern(), nextDyeColor()));
		return list;
	}
	
	public static ItemStack makeElytra(DyeColor color) {
		final ItemStack e = new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.setColor(color.getTextColor());
		dyement.write(e);
		return e;
	}
	
	public static ItemStack makeElytra(DyeColor leftColor, DyeColor rightColor) {
		final ItemStack e = new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.getWing(WingSide.LEFT).setColor(leftColor.getTextColor());
		dyement.getWing(WingSide.RIGHT).setColor(rightColor.getTextColor());
		dyement.write(e);
		return e;
	}
	
	@SafeVarargs
	public static ItemStack makeElytra(DyeColor base, Pair<BannerPattern, DyeColor>... patterns) {
		final ItemStack e = new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.setPattern(base, Arrays.asList(patterns));
		dyement.write(e);
		return e;
	}
	
	public static ItemStack makeElytra(
	  DyeColor baseLeft, List<Pair<BannerPattern, DyeColor>> leftPatterns,
	  DyeColor baseRight, List<Pair<BannerPattern, DyeColor>> rightPatterns
	) {
		final ItemStack e = new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA);
		dyement.read(e);
		dyement.getWing(WingSide.LEFT).setPattern(baseLeft, leftPatterns);
		dyement.getWing(WingSide.RIGHT).setPattern(baseRight, rightPatterns);
		dyement.write(e);
		return e;
	}
	
	public static Pair<ItemStack, ItemStack> split(ItemStack elytra) {
		if (!(elytra.getItem() instanceof final AerobaticElytraItem item))
			throw new IllegalArgumentException("Cannot split non-elytra item");
		return Pair.of(
		  item.getWing(elytra, WingSide.LEFT), item.getWing(elytra, WingSide.RIGHT));
	}
	
	public static Pair<List<ItemStack>, List<ItemStack>> split(List<ItemStack> elytras) {
		final ArrayList<ItemStack> left = new ArrayList<>(), right = new ArrayList<>();
		elytras.stream().map(AerobaticElytraJeiHelper::split).forEachOrdered(p -> {
			left.add(p.getFirst());
			right.add(p.getSecond());
		});
		return Pair.of(left, right);
	}
	
	public static ItemStack makeBanner(DyeColor base, List<Pair<BannerPattern, DyeColor>> patterns) {
		final ItemStack banner = new ItemStack(BannerBlock.byColor(base).asItem());
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
	
	public static ItemStack makeRocket(RocketStar[] stars) {
		final ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
		final CompoundTag fireworks = rocket.getOrCreateTagElement("Fireworks");
		fireworks.put("Explosions", RocketStar.listAsNBT(stars));
		return rocket;
	}

	public static RocketStar[] getRocketStars(ItemStack rocket) {
		CompoundTag tag = rocket.getTagElement("Fireworks");
		if (tag != null && tag.contains("Explosions"))
         return RocketStar.listFromNBT(tag.getList("Explosions", Tag.TAG_COMPOUND));
		return new RocketStar[0];
	}
	
	public static ItemStack makeRocket() {
		return makeRocket(makeRocketStars());
	}

	public static List<TrailData> makeTrails() {
		List<TrailData> list = new ArrayList<>();
		list.add(TrailData.empty());
		RocketStar[] stars = makeRocketStars();
		TrailData data = TrailData.empty();
		data.put(RocketSide.LEFT, stars);
		data.put(RocketSide.RIGHT, stars);
		list.add(data);
		list.add(makeTrailData(EnumSet.of(RocketSide.LEFT, RocketSide.RIGHT)));
		data = data.copy();
		stars = makeRocketStars();
		data.put(RocketSide.LEFT, stars);
		data.put(RocketSide.RIGHT, stars);
		stars = makeRocketStars();
		data.put(RocketSide.CENTER_LEFT, stars);
		data.put(RocketSide.CENTER_RIGHT, stars);
		list.add(data);
		list.add(makeTrailData(EnumSet.allOf(RocketSide.class)));
		return list;
	}


	public static TrailData makeTrailData() {
		TrailData trail = TrailData.empty();
		for (RocketSide side : RocketSide.values())
         if (RANDOM.nextFloat() > 0.5F)
            trail.put(side, makeRocketStars());
		return trail;
	}

	public static TrailData makeTrailData(Set<RocketSide> mask) {
		TrailData trail = TrailData.empty();
		for (RocketSide side : mask)
			trail.put(side, makeRocketStars());
		return trail;
	}
	
	public static RocketStar[] makeRocketStars() {
		List<RocketStar> list = new ArrayList<>();
		for (int j = 0, e = RANDOM.nextInt(2) + 1; j < e; j++) list.add(new RocketStar(
			getDyeColors(RANDOM.nextInt(3)),
			getDyeColors(RANDOM.nextInt(3)),
			RANDOM.nextFloat() > 0.7F, RANDOM.nextFloat() > 0.7F,
			(byte) RANDOM.nextInt(5)));
		return list.toArray(RocketStar[]::new);
	}
	
	public static List<ItemStack> getRockets(RocketSide side, TrailDataQuery query, IFocusGroup focus) {
		if (!query.mask().contains(side)) return Collections.emptyList();
		if (query.clear()) return ImmutableList.of(new ItemStack(Items.FIREWORK_ROCKET));
		List<ItemStack> list = new ArrayList<>();
		ItemStack rocket = getSingleFocusStack(focus, Items.FIREWORK_ROCKET, INPUT);
		if (!rocket.isEmpty() && getRocketStars(rocket).length > 0) return List.of(rocket);
		ItemStack outElytra = getSingleFocusStack(focus, i -> i instanceof AerobaticElytraItem, OUTPUT);
		// ItemStack inElytra = getSingleFocusStack(focus, i -> i instanceof AerobaticElytraItem, INPUT);
		if (!outElytra.isEmpty()) {
			TrailData outTrail = getElytraSpecOrDefault(outElytra).getTrailData();
			if (outTrail.get(side).length > 0)
				list.add(makeRocket(outTrail.get(side)));
			else list.add(new ItemStack(Items.FIREWORK_ROCKET));
		} else for (int i = 0; i < 20; i++) list.add(makeRocket());
		return list;
	}

	public static @NotNull ItemStack getSingleFocusStack(IFocusGroup focuses, Predicate<Item> p, RecipeIngredientRole role) {
		return focuses.getItemStackFocuses(role)
			.flatMap(f -> f.getTypedValue().getItemStack().stream())
			.filter(ss -> p.test(ss.getItem()))
			.findFirst().orElse(ItemStack.EMPTY);
	}

	public static @NotNull ItemStack getSingleFocusStack(IFocusGroup focuses, Item item, RecipeIngredientRole role) {
		return getSingleFocusStack(focuses, i -> i == item, role);
	}

	public static @NotNull ItemStack getSingleFocusElytraOrWings(IFocusGroup focuses, RecipeIngredientRole role) {
		return getSingleFocusStack(focuses, p -> p instanceof AerobaticElytraItem || p instanceof AerobaticElytraWingItem, role);
	}

	public record TrailDataQuery(Set<RocketSide> mask, boolean clear) {}
	public sealed interface InputDyementQuery permits InputDyeQuery, InputBannerQuery {
	}
	public record InputDyeQuery(int amount) implements InputDyementQuery {}
	public record InputBannerQuery() implements InputDyementQuery {}

	public static TrailData getTrailData(TrailDataQuery query) {
		return query != null && query.clear()? makeTrailData(query.mask()) : makeTrailData();
	}

	public static List<Pair<TrailData, TrailData>> getRocketsMatchingFocus(
		Stream<IFocus<ItemStack>> focuses, Set<RocketSide> mask, int variations
	) {
		List<Pair<TrailData, TrailData>> list = new ArrayList<>();
		focuses.forEach(f -> {
			RecipeIngredientRole role = f.getRole();
			ItemStack stack = f.getTypedValue().getIngredient();
			if (stack.getItem() instanceof AerobaticElytraItem) {
				if (role == INPUT) {
					TrailData trail = getElytraSpecOrDefault(stack).getTrailData();
					for (int i = 0; i < variations; i++) list.add(Pair.of(trail, makeTrailData(mask)));
				} else if (role == OUTPUT) {
					TrailData trail = getElytraSpecOrDefault(stack).getTrailData();
					for (int i = 0; i < variations; i++) {
						TrailData src = TrailData.empty();
						for (RocketSide side : mask)
							src.put(side, makeRocketStars());
						list.add(Pair.of(src, trail));
					}
				}
			}
		});
		return list.isEmpty()? getRockets(mask, variations) : list;
	}

	public static List<ItemStack> getRockets(int amount) {
		return IntStream.range(0, amount).mapToObj(i -> makeRocket(makeRocketStars())).toList();
	}

	public static List<Pair<TrailData, TrailData>> getRockets(Set<RocketSide> sides, int variations) {
		return IntStream.range(0, variations).mapToObj(i -> {
			TrailData trail = TrailData.empty();
			for (RocketSide side : sides)
				trail.put(side, makeRocketStars());
			return Pair.of(TrailData.empty(), trail);
		}).toList();
	}

	public static List<ItemStack> getAerobaticElytrasMatchingFocus(
	  Stream<IFocus<ItemStack>> focuses
	) {
		return getAerobaticElytrasMatchingFocus(focuses, null);
	}

	public static List<ItemStack> getAerobaticElytrasMatchingFocus(
	  Stream<IFocus<ItemStack>> focuses, @Nullable TrailDataQuery trailQuery
	) {
		List<ItemStack> list = new ArrayList<>();
		focuses.forEach(f -> {
			ItemStack focusStack = f.getTypedValue().getIngredient();
			if (focusStack.getItem() instanceof AerobaticElytraItem) {
				final ItemStack elytra = focusStack.copy();
				elytra.setDamageValue(0);
				list.add(elytra);
			} else if (focusStack.getItem() instanceof AerobaticElytraWingItem) {
				final ItemStack right = focusStack.copy();
				right.setDamageValue(0);
				final List<ItemStack> leftWings = getAerobaticElytras(trailQuery).stream()
				  .map(s -> ((AerobaticElytraItem) s.getItem()).getWing(s, WingSide.LEFT))
				  .collect(Collectors.toList());
				leftWings.add(0, right.copy());
				leftWings.stream().map(w -> {
					ItemStack l = right.copy();
					dyement.read(w);
					dyement.write(l);
					return JoinRecipe.join(l, right);
				}).forEach(list::add);
			}
		});
		return list.isEmpty()? getAerobaticElytras(trailQuery) : list;
	}
	
	public static <T> List<T> randomSample(List<T> pool, int copies) {
		List<T> result = new ArrayList<>();
		for (int i = 0, s = copies * pool.size(); i < s; i++)
			result.add(pool.get(RANDOM.nextInt(pool.size())));
		return result;
	}
	
	public static int gcd(int a, int b) {return b == 0? a : gcd(b, a % b);}
	
	public static int mcm(int a, int b) {return a * b / gcd(a, b);}
	
	public static <V> IDrawable createMultiIngredientDrawable(IIngredientType<V> type, V first, V second) {
		return new MultiIngredientDrawable<>(
		  first, second, AerobaticElytraJeiPlugin.ingredientManager.getIngredientRenderer(type));
	}
}
