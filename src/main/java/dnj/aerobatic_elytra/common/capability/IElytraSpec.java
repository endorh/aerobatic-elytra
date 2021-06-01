package dnj.aerobatic_elytra.common.capability;

import com.google.gson.*;
import dnj.aerobatic_elytra.client.trail.AerobaticTrail.RocketSide;
import dnj.aerobatic_elytra.common.item.ElytraDyementReader.WingSide;
import dnj.aerobatic_elytra.common.item.IAbility;
import dnj.aerobatic_elytra.common.item.IAbility.Ability;
import dnj.aerobatic_elytra.common.registry.ModRegistries;
import dnj.endor8util.math.MathParser.FixedKeySetFallbackMap;
import dnj.endor8util.math.MathParser.ParsedExpression;
import dnj.endor8util.network.PacketBufferUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.DyeColor;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

import static dnj.endor8util.util.TextUtil.stc;
import static dnj.endor8util.util.TextUtil.ttc;
import static java.lang.String.format;
import static net.minecraft.util.math.MathHelper.clamp;

public interface IElytraSpec {
	/**
	 * Shorthand for {@code getAbilities().get(prop)}
	 * @param prop Ability to get
	 * @return Ability value
	 */
	default float getAbility(IAbility prop) {
		return getAbilities().get(prop);
	}
	
	/**
	 * Shorthand for {@code getAbilities().put(prop, value)}
	 * @param prop Ability to set
	 * @param value Ability value
	 */
	default void setAbility(IAbility prop, float value) {
		getAbilities().put(prop, value);
	}
	
	/**
	 * Shorthand for {@code getAbilities().put(prop, prop.getDefault())}
	 * @param prop Ability to reset
	 */
	default void resetAbility(IAbility prop) {
		setAbility(prop, prop.getDefault());
	}
	
	/**
	 * Obtain the internal map used to store the abilities.<br>
	 * Modifying an ability on the returned map is allowed.
	 * @see IElytraSpec#getAbility
	 * @see IElytraSpec#setAbility
	 */
	Map<IAbility, Float> getAbilities();
	
	/**
	 * Obtain the internal map used to store unknown abilities.<br>
	 * This abilities are ignored during gameplay, but kept in the
	 * NBT, allowing abilities from unloaded mods to be preserved.
	 */
	Map<String, Float> getUnknownAbilities();
	
	/**
	 * Copy from other spec
	 * @param spec Source
	 */
	default void copy(IElytraSpec spec) {
		final Map<IAbility, Float> properties = getAbilities();
		properties.clear();
		properties.putAll(spec.getAbilities());
		final Map<String, Float> unknown = getUnknownAbilities();
		unknown.clear();
		unknown.putAll(spec.getUnknownAbilities());
		getTrailData().read(spec.getTrailData().write());
	}
	
	@Nonnull TrailData getTrailData();
	
	@SuppressWarnings("unused")
	default void addTooltipInfo(final List<ITextComponent> tooltip) {
		addTooltipInfo(tooltip, "");
	}
	
	default void addTooltipInfo(final List<ITextComponent> tooltip, final String indent) {
		final List<IAbility> abs = new ArrayList<>(getAbilities().keySet());
		abs.remove(Ability.FUEL);
		abs.remove(Ability.MAX_FUEL);
		if (abs.isEmpty()) {
			tooltip.add(
			  stc(indent).append(
				 ttc("aerobatic-elytra.abilities")
					.appendString(": ")
					.append(ttc("gui.none").mergeStyle(TextFormatting.DARK_GRAY))
			  ).mergeStyle(TextFormatting.GRAY));
		} else if (Screen.hasAltDown()) {
			abs.sort(Comparator.comparing(ab -> ab.getDisplayName().getString()));
			tooltip.add(
			  stc(indent).append(
			    ttc("aerobatic-elytra.abilities")
			      .appendString(": ")
			  ).mergeStyle(TextFormatting.GRAY)
			);
			final String innerIndent = indent + "  ";
			for (IAbility ability : abs)
				ability.getDisplayType().format(ability, getAbility(ability)).ifPresent(
				  tc -> tooltip.add(stc(innerIndent).append(tc).mergeStyle(TextFormatting.GRAY)));
		} else {
			tooltip.add(
			  stc(indent).append(
			    ttc("aerobatic-elytra.abilities")
				   .appendString(": ")
				   .append(
					  ttc("aerobatic-elytra.gui.alt_to_expand")
					    .mergeStyle(TextFormatting.DARK_GRAY))
			  ).mergeStyle(TextFormatting.GRAY));
		}
	}
	
	class TrailData {
		private static final Random random = new Random();
		
		public RocketExplosion[] left = null;
		public RocketExplosion[] right = null;
		public RocketExplosion[] centerLeft = null;
		public RocketExplosion[] centerRight = null;
		
		public TrailData() {}
		
		public RocketExplosion[] get(RocketSide side) {
			switch (side) {
				case LEFT: return left;
				case RIGHT: return right;
				case CENTER_LEFT: return centerLeft;
				case CENTER_RIGHT: return centerRight;
				default: throw new IllegalStateException();
			}
		}
		
		/**
		 * Picks a random {@code RocketExplosion[]} from a random side
		 */
		public Optional<RocketExplosion[]> pickRandom() {
			RocketExplosion[][] pool =
			  Arrays.stream(new RocketExplosion[][]{
			    left, right, centerLeft, centerRight
			  }).filter(Objects::nonNull).toArray(RocketExplosion[][]::new);
			if (pool.length == 0)
				return Optional.empty();
			return Optional.of(pool[random.nextInt(pool.length)]);
		}
		
		/**
		 * Copy the kept side to the other
		 * @param side Side to keep
		 */
		public void keep(WingSide side) {
			if (side == WingSide.LEFT) {
				right = left != null? Arrays.copyOf(left, left.length) : null;
				centerRight = centerLeft != null? Arrays.copyOf(centerLeft, centerLeft.length) : null;
			} else if (side == WingSide.RIGHT) {
				left = right != null? Arrays.copyOf(right, right.length) : null;
				centerLeft = centerRight != null? Arrays.copyOf(centerRight, centerRight.length) : null;
			}
		}
		
		/**
		 * Copies a side from another TrailData
		 * @param side Side to copy, null copies both
		 * @param source Source trail data
		 */
		public void set(WingSide side, TrailData source) {
			if (side == WingSide.LEFT || side == null) {
				left = source.left != null? Arrays.copyOf(source.left, source.left.length) : null;
				centerLeft = source.centerLeft != null? Arrays.copyOf(source.centerLeft, source.centerLeft.length) : null;
			}
			if (side == WingSide.RIGHT || side == null) {
				right = source.right != null? Arrays.copyOf(source.right, source.right.length) : null;
				centerRight = source.centerRight != null? Arrays.copyOf(source.centerRight, source.centerRight.length) : null;
			}
		}
		
		/**
		 * Put array of explosions in specific slot
		 * @param side Rocket slot
		 * @param value Explosion array
		 */
		public void put(RocketSide side, RocketExplosion[] value) {
			switch (side) {
				case LEFT:
					left = value;
					break;
				case RIGHT:
					right = value;
					break;
				case CENTER_LEFT:
					centerLeft = value;
					break;
				case CENTER_RIGHT:
					centerRight = value;
			}
		}
		
		public static TrailData read(PacketBuffer buf) {
			TrailData data = new TrailData();
			data.read(buf.readCompoundTag());
			return data;
		}
		
		public void write(PacketBuffer buf) {
			buf.writeCompoundTag(write());
		}
		
		public void read(CompoundNBT trailNBT) {
			if (trailNBT == null)
				trailNBT = new CompoundNBT();
			for (RocketSide rocketSide : RocketSide.values()) {
				if (trailNBT.contains(rocketSide.tagName)) {
					put(rocketSide, RocketExplosion.listFromNBT(trailNBT.getList(rocketSide.tagName, 10)));
				}
			}
		}
		
		@Nullable public CompoundNBT write() {
			CompoundNBT trailNBT = new CompoundNBT();
			int count = 0;
			for (RocketSide rocketSide : RocketSide.values()) {
				RocketExplosion[] explosions = get(rocketSide);
				if (explosions != null) {
					trailNBT.put(rocketSide.tagName, RocketExplosion.listAsNBT(explosions));
					count++;
				}
			}
			if (count > 0)
				return trailNBT;
			return null;
		}
		
		private static final String I18N_TRAIL = "aerobatic-elytra.item.trail";
		
		public static void addTooltipInfo(
		  List<ITextComponent> tooltip, TrailData data, String indent
		) { addTooltipInfo(tooltip, data, null, indent); }
		
		/**
		 * Adds description to a tooltip.
		 * @param wingSide Specific side to add info from, null to add from both.
		 * @param indent Additional indent to use.
		 */
		public static void addTooltipInfo(
		  List<ITextComponent> tooltip, TrailData data, @Nullable WingSide wingSide, String indent
		) {
			List<ITextComponent> trailInfo = new ArrayList<>();
			String innerIndent = indent + "  ";
			for (RocketSide side : RocketSide.forWingSide(wingSide)) {
				RocketExplosion[] explosions = data.get(side);
				if (explosions != null) {
					trailInfo.add(
					  stc(innerIndent)
					    .append(side.getDisplayName())
					    .appendString(": ")
					    .mergeStyle(TextFormatting.GRAY));
					trailInfo.addAll(RocketExplosion.getTooltipInfo(
					  explosions, innerIndent + "  ", TextFormatting.GRAY)
					);
				}
			}
			if (trailInfo.size() == 0) {
				tooltip.add(
				  stc(indent).append(
				    ttc(I18N_TRAIL)
				      .appendString(": ")
				      .append(ttc("gui.none").mergeStyle(TextFormatting.DARK_GRAY))
				  ).mergeStyle(TextFormatting.GRAY));
			} else if (Screen.hasControlDown()) {
				tooltip.add(
				  stc(indent).append(
				    ttc(I18N_TRAIL)
				      .appendString(": ")
				  ).mergeStyle(TextFormatting.GRAY));
				tooltip.addAll(trailInfo);
			} else {
				tooltip.add(
				  stc(indent).append(
				    ttc(I18N_TRAIL)
				      .appendString(": ")
				      .append(
				        ttc("aerobatic-elytra.gui.control_to_expand")
				          .mergeStyle(TextFormatting.DARK_GRAY))
				  ).mergeStyle(TextFormatting.GRAY));
			}
		}
	}
	
	/**
	 * Describes a single firework rocket explosion.<br>
	 * Every rocket has one per firework star.
	 */
	class RocketExplosion {
		@SuppressWarnings("unused")
		public static final byte
		  SHAPE_SMALL_BALL = 0,
		  SHAPE_LARGE_BALL = 1,
		  SHAPE_STAR = 2,
		  SHAPE_CREEPER = 3,
		  SHAPE_BURST = 4,
		// Underwater
		  SHAPE_BUBBLE = 5,
		// TODO: Easter egg
		  SHAPE_CHICKEN = 6;
		
		public final boolean flicker;
		public final boolean trail;
		public final byte type;
		public final int[] colors;
		public final int[] fadeColors;
		
		public RocketExplosion(
		  boolean flickerIn, boolean trailIn, byte typeIn, int[] colorsIn, int[] fadeColorsIn
		) {
			flicker = flickerIn;
			trail = trailIn;
			type = typeIn;
			colors = colorsIn;
			fadeColors = fadeColorsIn;
		}
		
		public static RocketExplosion[] listFromNBT(ListNBT nbt) {
			final int size = nbt.size();
			RocketExplosion[] explosions = new RocketExplosion[size];
			for (int i = 0; i < size; i++) {
				CompoundNBT item = nbt.getCompound(i);
				explosions[i] = new RocketExplosion(
				  item.getBoolean("Flicker"),
				  item.getBoolean("Trail"),
				  item.getByte("Type"),
				  item.getIntArray("Colors"),
				  item.getIntArray("FadeColors"));
			}
			return explosions;
		}
		
		public static ListNBT listAsNBT(RocketExplosion[] list) {
			ListNBT nbt = new ListNBT();
			for (RocketExplosion explosion : list) {
				CompoundNBT item = new CompoundNBT();
				item.putBoolean("Flicker", explosion.flicker);
				item.putBoolean("Trail", explosion.trail);
				item.putByte("Type", explosion.type);
				item.putIntArray("Colors", explosion.colors);
				item.putIntArray("FadeColors", explosion.fadeColors);
				nbt.add(item);
			}
			return nbt;
		}
		
		public static List<ITextComponent> getTooltipInfo(
		  RocketExplosion[] list, String indent, TextFormatting format
		) {
			List<ITextComponent> tooltip = new ArrayList<>();
			for (RocketExplosion explosion : list) {
				FireworkRocketItem.Shape shape = FireworkRocketItem.Shape.get(explosion.type);
				tooltip.add(
				  stc(indent)
				    .append(ttc("item.minecraft.firework_star.shape." + shape.getShapeName()))
				    .mergeStyle(format)
				);
				if (explosion.colors.length > 0)
					tooltip.add(joinComma(stc(indent), explosion.colors).mergeStyle(format));
				if (explosion.fadeColors.length > 0)
					tooltip.add(
					  stc(indent)
					    .append(joinComma(
					      ttc("item.minecraft.firework_star.fade_to")
					        .appendString(" "),
					      explosion.fadeColors))
					    .mergeStyle(format)
					);
				if (explosion.trail || explosion.flicker) {
					final StringTextComponent tc = stc(indent);
					if (explosion.trail)
						tc.append(ttc("item.minecraft.firework_star.trail"));
					if (explosion.flicker) {
						if (explosion.trail)
							tc.append(stc(", "));
						tc.append(ttc("item.minecraft.firework_star.flicker"));
					}
					tc.mergeStyle(format);
					tooltip.add(tc);
				}
			}
			return tooltip;
		}
		
		private static IFormattableTextComponent joinComma(IFormattableTextComponent tc, int[] list) {
			for(int i = 0; i < list.length; ++i) {
				if (i > 0)
					tc.appendString(", ");
				tc.append(dyeName(list[i]));
			}
			return tc;
		}
		
		private static ITextComponent dyeName(int color) {
			DyeColor dyecolor = DyeColor.byFireworkColor(color);
			return dyecolor == null
			       ? new TranslationTextComponent(
			         "item.minecraft.firework_star.custom_color")
			       : new TranslationTextComponent(
			         "item.minecraft.firework_star." + dyecolor.getTranslationKey());
		}
	}
	
	/**
	 * Class representing an Acrobatic Elytra upgrade<br>
	 * Recharging fuel is considered an upgrade
	 */
	class Upgrade {
		private final IAbility type;
		private final ParsedExpression<Double> expression;
		private final IFormattableTextComponent prettyExpression;
		private final boolean booleanValue;
		private final float min;
		private final float max;
		
		/**
		 * Create an upgrade
		 * @param type Upgrade type
		 * @param expression Upgrade expression
		 */
		public Upgrade(IAbility type, String expression, float min, float max) {
			this.type = type;
			this.expression = ModRegistries.ABILITY_EXPRESSION_PARSER.parse(expression);
			this.min = min;
			this.max = max;
			IFormattableTextComponent prettyExpression = ModRegistries.ABILITY_EXPRESSION_HIGHLIGHTER
			  .parse(expression).eval();
			if (type.getDisplayType().isBool()) {
				final String expr = prettyExpression.getString().trim();
				boolean val = false;
				try {
					double parsed = Double.parseDouble(expr);
					if (parsed == 1.0D && min <= 1F && max >= 1F) {
						prettyExpression = null;
						val = true;
					} else if (parsed == 0.0D && min <= 0F && max >= 0F) {
						prettyExpression = null;
						val = false;
					} else val = false;
				} catch (NumberFormatException ignored) {}
				booleanValue = val;
			} else booleanValue = false;
			this.prettyExpression = prettyExpression;
		}
		
		public static List<Upgrade> deserialize(JsonArray arr) {
			List<Upgrade> upgrades = new ArrayList<>();
			for (int i = 0; i < arr.size(); i++) {
				try {
					upgrades.add(deserialize(arr.get(i).getAsJsonObject()));
				} catch (Exception e) {
					throw new JsonSyntaxException("Malformed upgrade at index " + i, e);
				}
			}
			return upgrades;
		}
		
		public static Upgrade deserialize(JsonObject obj) {
			IAbility type = IAbility.fromJsonName(
			  JSONUtils.getString(obj, "type"));
			JsonElement expr = obj.get("expression");
			if (!expr.isJsonPrimitive())
				throw new JsonSyntaxException("Expected 'expression' to be a primitive JSON value");
			JsonPrimitive primitive = expr.getAsJsonPrimitive();
			String expression;
			if (primitive.isNumber())
				expression = String.valueOf(primitive.getAsDouble());
			else if (primitive.isBoolean())
				expression = String.valueOf(primitive.getAsBoolean()? 1F : 0F);
			else if (primitive.isString())
				expression = JSONUtils.getString(obj, "expression");
			else throw new JsonSyntaxException("'expression' must be either a number, boolean or a string");
			float min = JSONUtils.getFloat(obj, "min", Float.NEGATIVE_INFINITY);
			float max = JSONUtils.getFloat(obj, "max", Float.POSITIVE_INFINITY);
			if (min > max)
				throw new JsonSyntaxException("'min' cannot be greater than 'max'");
			return new Upgrade(type, expression, min, max);
		}
		
		public static Upgrade read(PacketBuffer buf) {
			IAbility type = ModRegistries.ABILITY_REGISTRY.getValue(
			  buf.readResourceLocation());
			if (type == null)
				throw new IllegalArgumentException("Received packet with unknown ability type");
			String expression = PacketBufferUtil.readString(buf);
			float min = buf.readFloat();
			float max = buf.readFloat();
			return new Upgrade(type, expression, min, max);
		}
		
		public void write(PacketBuffer buf) {
			//noinspection ConstantConditions
			buf.writeResourceLocation(type.getRegistryName());
			buf.writeString(expression.getExpression());
			buf.writeFloat(min);
			buf.writeFloat(max);
		}
		
		public boolean apply(IElytraSpec spec) {
			final FixedKeySetFallbackMap<String, Double> namespace = expression.getNamespace();
			for (IAbility t : ModRegistries.ABILITY_REGISTRY)
				namespace.set(t.jsonName(), (double) spec.getAbility(t));
			
			float result = (float) clamp(expression.eval(), min, max);
			if (spec.getAbility(type) != result) {
				spec.setAbility(type, (float) clamp(expression.eval(), min, max));
				return true;
			}
			return false;
		}
		
		public IAbility getType() { return type; }
		public ParsedExpression<Double> getExpression() { return expression; }
		@SuppressWarnings("unused")
		public IFormattableTextComponent getPrettyExpression() { return prettyExpression; }
		public float getMin() { return min; }
		public float getMax() { return max; }
		
		public List<ITextComponent> getDisplay() {
			List<ITextComponent> tt = new ArrayList<>();
			String name = type.jsonName();
			TextFormatting color = ModRegistries.ABILITY_EXPRESSION_HIGHLIGHTER.getNameColor(name);
			if (prettyExpression != null) {
				tt.add(stc(name).mergeStyle(color)
				         .append(stc(" = ").mergeStyle(TextFormatting.GOLD))
				         .append(prettyExpression));
			} else {
				tt.add(ttc("aerobatic-elytra.recipe.upgrade." + (booleanValue? "enable" : "disable"),
				           stc(name).mergeStyle(color)).mergeStyle(TextFormatting.GRAY));
			}
			if (min != Float.NEGATIVE_INFINITY || max != Float.POSITIVE_INFINITY) {
				IFormattableTextComponent l = stc("  ");
				if (min != Float.NEGATIVE_INFINITY) {
					l.append(stc("min: ")
					           .append(stc(format("%.2f", min)).mergeStyle(TextFormatting.AQUA))
					           .mergeStyle(TextFormatting.GRAY));
				}
				if (max != Float.POSITIVE_INFINITY) {
					if (min != Float.NEGATIVE_INFINITY)
						l.append(stc(", ").mergeStyle(TextFormatting.DARK_GRAY));
					l.append(stc("max: ")
					           .append(stc(format("%.2f", max)).mergeStyle(TextFormatting.AQUA))
					           .mergeStyle(TextFormatting.GRAY));
				}
				tt.add(l);
			}
			return tt;
		}
		
		@Override public String toString() {
			return "<" + type.jsonName() + " = " + expression
			       + (min != Float.NEGATIVE_INFINITY? "; min: " + min : "")
			       + (max != Float.POSITIVE_INFINITY? "; max: " + max : "") + ">";
		}
	}
}
