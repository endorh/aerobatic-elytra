package dnj.aerobatic_elytra.common.capability;

import com.google.gson.*;
import dnj.aerobatic_elytra.client.trail.AerobaticTrail.RocketSide;
import dnj.aerobatic_elytra.common.item.ElytraDyementReader.WingSide;
import dnj.aerobatic_elytra.common.item.IAbility;
import dnj.aerobatic_elytra.common.item.IAbility.Ability;
import dnj.aerobatic_elytra.common.item.IDatapackAbilityReloadListener;
import dnj.aerobatic_elytra.common.item.IDatapackAbility;
import dnj.aerobatic_elytra.common.item.IEffectAbility;
import dnj.aerobatic_elytra.common.registry.ModRegistries;
import dnj.endor8util.math.MathParser.ExpressionParser.ParseException;
import dnj.endor8util.math.MathParser.ExpressionParser.ParseException.NameParseException;
import dnj.endor8util.math.MathParser.FixedNamespaceSet;
import dnj.endor8util.math.MathParser.ParsedExpression;
import dnj.endor8util.network.PacketBufferUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ServerPlayerEntity;
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

/**
 * Holds Aerobatic Elytra specifications<br>
 *
 * Implementations must call {@link IDatapackAbilityReloadListener#registerAerobaticElytraDatapackAbilityReloadListener()}
 * on every new instance to be able to receive ability reload callbacks
 */
public interface IElytraSpec extends IDatapackAbilityReloadListener {
	/**
	 * Shorthand for {@code getAbilities().get(prop)}
	 * @param prop Ability to get
	 * @return Ability value
	 */
	default float getAbility(IAbility prop) {
		return getAbilities().get(prop);
	}
	
	/**
	 * Set an ability value
	 * @param prop Ability to set
	 * @param value Ability value
	 */
	void setAbility(IAbility prop, float value);
	
	/**
	 * Remove an ability<br>
	 * This is different from setting it to the default value<br>
	 * Implementations must check if the ability is an effect ability
	 * and undo its effect if it's being applied
	 * @param ability Ability to remove
	 * @return The removed ability value, or null if the ability wasn't present
	 */
	Float removeAbility(IAbility ability);
	
	/**
	 * Shorthand for {@code getAbilities().put(prop, prop.getDefault())}
	 * @param prop Ability to reset
	 */
	default void resetAbility(IAbility prop) {
		setAbility(prop, prop.getDefault());
	}
	
	/**
	 * Set the provided abilities<br>
	 * This does not clear abilities not present in the provided
	 * map. For that behaviour use {@link IElytraSpec#setAbilities(Map)}
	 * @param abilities Ability values
	 */
	void putAbilities(Map<IAbility, Float> abilities);
	
	/**
	 * Set all the abilities<br>
	 * This clears the previous abilities, use {@link IElytraSpec#putAbilities(Map)}
	 * to just update values.
	 * @param abilities Ability values
	 */
	void setAbilities(Map<IAbility, Float> abilities);
	
	/**
	 * Check if an ability is present in the elytra
	 * @param ability Ability to check
	 */
	default boolean hasAbility(IAbility ability) {
		return getAbilities().containsKey(ability);
	}
	
	/**
	 * Obtain a read-only map with all the abilities of the elytra<br>
	 * Modifying this map is not allowed
	 * @see IElytraSpec#getAbility
	 * @see IElytraSpec#setAbility
	 * @see IElytraSpec#setAbilities
	 * @see IElytraSpec#putAbilities
	 */
	Map<IAbility, Float> getAbilities();
	
	/**
	 * Obtain the map with all the unknown abilities<br>
	 * This abilities are ignored at runtime, but kept in the
	 * NBT, allowing abilities from unloaded mods/datapacks to be
	 * preserved.<br>
	 * Modifying this map is allowed
	 */
	Map<String, Float> getUnknownAbilities();
	
	/**
	 * Return a map of the effect abilities of this elytra<br>
	 * Modifying the key set of this map is forbidden and will result
	 * in undefined behaviour. Its contents will be updated automatically
	 * if the spec's abilities change<br>
	 * The true values of this map represent effects that are currently
	 * being applied
	 */
	Map<IEffectAbility, Boolean> getEffectAbilities();
	
	void updatePlayerEntity(ServerPlayerEntity player);
	@Nullable ServerPlayerEntity getPlayerEntity();
	
	/**
	 * Copy from other spec
	 * @param spec Source
	 */
	default void copy(IElytraSpec spec) {
		setAbilities(spec.getAbilities());
		final Map<String, Float> unknown = getUnknownAbilities();
		unknown.clear();
		unknown.putAll(spec.getUnknownAbilities());
		getTrailData().read(spec.getTrailData().write());
	}
	
	/**
	 * Refresh the map of abilities after a datapack reload
	 */
	@Override default void onAerobaticElytraDatapackAbilityReload() {
		final Map<String, Float> unknown = getUnknownAbilities();
		for (IDatapackAbility ability : ModRegistries.getOutdatedAbilities()) {
			if (hasAbility(ability))
				unknown.put(ability.fullName(), removeAbility(ability));
		}
		for (IDatapackAbility ability : ModRegistries.getDatapackAbilities().values()) {
			final String name = ability.fullName();
			if (unknown.containsKey(name))
				setAbility(ability, unknown.remove(name));
		}
	}
	
	@Nonnull TrailData getTrailData();
	
	@SuppressWarnings("unused")
	default void addAbilityTooltipInfo(final List<ITextComponent> tooltip) {
		addAbilityTooltipInfo(tooltip, "");
	}
	
	default void addAbilityTooltipInfo(final List<ITextComponent> tooltip, final String indent) {
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
			abs.sort(Comparator.<IAbility, Integer>comparing(
			  ab -> ab.getDisplayType().isBool() ? 1 : 0
			).thenComparing(ab -> ab.getDisplayName().getString()));
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
	 * Recharging fuel is considered an upgrade<br>
	 * <br>
	 * Since some {@link IAbility}s are loaded after recipes
	 * (the ones provided by datapacks), Upgrades that rely
	 * on them may be temporarily invalid.
	 */
	class Upgrade {
		protected final String abilityName;
		protected IAbility type;
		protected final String rawExpression;
		protected ParsedExpression<Double> expression;
		protected IFormattableTextComponent prettyExpression;
		protected boolean booleanValue;
		protected final float min;
		protected final float max;
		protected boolean valid;
		
		/**
		 * Create an upgrade
		 * @param abilityName Upgrade type
		 * @param expression Upgrade expression
		 */
		public Upgrade(String abilityName, String expression, float min, float max) {
			this.abilityName = abilityName;
			rawExpression = expression;
			this.min = min;
			this.max = max;
			reloadAbilities();
		}
		
		/**
		 * Perform the ability parsing again<br>
		 * @return The new valid state
		 */
		public boolean reloadAbilities() {
			type = ModRegistries.getAbilityByName(abilityName);
			try {
				expression = ModRegistries.ABILITY_EXPRESSION_PARSER.parse(rawExpression);
			} catch (NameParseException ignored) { expression = null; }
			valid = expression != null && type != null;
			if (valid) {
				IFormattableTextComponent pretty = ModRegistries.ABILITY_EXPRESSION_HIGHLIGHTER.parse(rawExpression).eval();
				if (type.getDisplayType().isBool()) {
					final String expr = pretty.getString().trim();
					boolean val = false;
					try {
						double parsed = Double.parseDouble(expr);
						if (parsed == 1.0D && min <= 1F && max >= 1F) {
							pretty = null;
							val = true;
						} else if (parsed == 0.0D && min <= 0F && max >= 0F) {
							pretty = null;
							val = false;
						} else val = false;
					} catch (NumberFormatException ignored) {}
					booleanValue = val;
				} else booleanValue = false;
				prettyExpression = pretty;
			}
			return valid;
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
			final String abilityName = JSONUtils.getString(obj, "type");
			final JsonElement expr = obj.get("expression");
			if (!expr.isJsonPrimitive())
				throw new JsonSyntaxException("Expected 'expression' to be a primitive JSON value");
			final JsonPrimitive primitive = expr.getAsJsonPrimitive();
			String expression;
			if (primitive.isNumber())
				expression = String.valueOf(primitive.getAsDouble());
			else if (primitive.isBoolean())
				expression = String.valueOf(primitive.getAsBoolean()? 1F : 0F);
			else if (primitive.isString())
				expression = JSONUtils.getString(obj, "expression");
			else throw new JsonSyntaxException("'expression' must be either a number, boolean or a string");
			final float min = JSONUtils.getFloat(obj, "min", Float.NEGATIVE_INFINITY);
			final float max = JSONUtils.getFloat(obj, "max", Float.POSITIVE_INFINITY);
			if (min > max)
				throw new JsonSyntaxException("'min' cannot be greater than 'max'");
			return new Upgrade(abilityName, expression, min, max);
		}
		
		public static Upgrade read(PacketBuffer buf) {
			final String abilityName = PacketBufferUtil.readString(buf);
			final String expression = PacketBufferUtil.readString(buf);
			final float min = buf.readFloat();
			final float max = buf.readFloat();
			return new Upgrade(abilityName, expression, min, max);
		}
		
		public void write(PacketBuffer buf) {
			buf.writeString(abilityName);
			buf.writeString(expression.getExpression());
			buf.writeFloat(min);
			buf.writeFloat(max);
		}
		
		public boolean apply(IElytraSpec spec) {
			final FixedNamespaceSet<Double> namespaceSet = expression.getNamespaceSet();
			for (IAbility t : ModRegistries.getAbilities().values())
				namespaceSet.set(t.jsonName(), (double) spec.getAbility(t));
			
			float result = (float) clamp(expression.eval(), min, max);
			if (spec.getAbility(type) != result) {
				spec.setAbility(type, result);
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
		@SuppressWarnings("unused")
		public boolean isValid() {
			return valid;
		}
		
		public List<ITextComponent> getDisplay() {
			List<ITextComponent> tt = new ArrayList<>();
			if (type == null) {
				tt.add(stc("<Error>").mergeStyle(TextFormatting.RED));
				return tt;
			}
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
