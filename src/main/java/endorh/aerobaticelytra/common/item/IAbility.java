package endorh.aerobaticelytra.common.item;

import com.google.common.base.CaseFormat;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.registry.AerobaticElytraRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static endorh.aerobaticelytra.common.item.IAbility.DisplayType.*;
import static endorh.lazulib.text.TextUtil.stc;
import static endorh.lazulib.text.TextUtil.ttc;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public interface IAbility {
	/**
	 * Name used to parse in JSON, should be unique.<br>
	 */
	String getName();
	
	default @Nullable ResourceLocation getRegistryKey() {
		return AerobaticElytraRegistries.getAbilityKey(this);
	}
	
	default String fullName() {
		ResourceLocation key = Objects.requireNonNull(getRegistryKey(), "Unregistered ability " + this);
		return key.getNamespace().replace('`', '_') + ':' + getName();
	}
	
	default void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(Objects.requireNonNull(
		  getRegistryKey(), "Unregistered ability " + this));
	}
	
	static IAbility read(FriendlyByteBuf buf) {
		return AerobaticElytraRegistries.getAbility(buf.readResourceLocation());
	}
	
	/**
	 * Name to be displayed in-game
	 */
	MutableComponent getDisplayName();
	
	/**
	 * Display type, used to show the ability in tooltips
	 */
	DisplayType getDisplayType();
	
	/**
	 * Default value when missing in item stack
	 */
	float getDefault();
	
	/**
	 * Color used in syntax highlighting
	 * If null, a random one is chosen by hashing the name
	 */
	default @Nullable ChatFormatting getColor() {
		return null;
	}
	
	enum Ability implements IAbility {
		FUEL(ChatFormatting.RED, 0F, DEFAULT),
		MAX_FUEL(ChatFormatting.LIGHT_PURPLE, 0F, DEFAULT),
		SPEED(ChatFormatting.GREEN, 1F, SCALE),
		LIFT(ChatFormatting.YELLOW, 0F, NON_ZERO),
		AQUATIC(ChatFormatting.BLUE, 1F, SCALE_BOOL),
		TRAIL(ChatFormatting.DARK_PURPLE, 1F, SCALE),
		ROCKETLESS(ChatFormatting.GRAY, 0F, BOOL);
		
		private final String jsonName;
		private final String translationKey;
		private final ChatFormatting color;
		private final float defaultValue;
		private final DisplayType displayType;
		
		Ability(ChatFormatting color, float defaultValue, DisplayType type) {
			this.jsonName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
			this.translationKey = AerobaticElytra.MOD_ID + ".abilities." + name().toLowerCase();
			this.color = color;
			this.defaultValue = defaultValue;
			this.displayType = type;
		}
		
		@Override public String getName() { return jsonName; }
		@Override public MutableComponent getDisplayName() { return ttc(translationKey); }
		@Override public DisplayType getDisplayType() {
			return displayType;
		}
		
		@Override public ChatFormatting getColor() { return color; }
		@Override public float getDefault() { return defaultValue; }
	}
	
	/**
	 * Ability display types
	 */
	abstract class DisplayType {
		public DisplayType() {
			this(false);
		}
		public DisplayType(boolean bool) {
			this.bool = bool;
		}
		
		public boolean bool;
		public abstract Optional<MutableComponent> format(IAbility ability, float value);
		public boolean isBool() { return bool; }
		
		/**
		 * Display with format {@code %s: %.1f}
		 */
		public static final DisplayType DEFAULT = formatValue("%.1f");
		
		/**
		 * Do not display
		 */
		public static DisplayType HIDE = new DisplayType() {
			@Override public Optional<MutableComponent> format(IAbility ability, float value) {
				return Optional.empty();
			}
		};
		
		/**
		 * Display with format {@code %s: %d}<br>
		 */
		public static final DisplayType INTEGER = formatValue(
		  v -> String.format("%d", Math.round(v)));
		
		/**
		 * Display as {@link #INTEGER} if not zero, otherwise {@link #HIDE}
		 */
		public static final DisplayType INTEGER_NON_ZERO = filter(
		  v -> v != 0, INTEGER, HIDE);
		
		/**
		 * Display with format {@code %s: %+d}<br>
		 */
		public static final DisplayType INTEGER_SUM = formatValue(
		  v -> String.format("%+d", Math.round(v)));
		
		/**
		 * Display as {@link #INTEGER_SUM} if not zero, otherwise {@link #HIDE}
		 */
		public static final DisplayType INTEGER_SUM_NON_ZERO = filter(
		  v -> v != 0, INTEGER_SUM, HIDE);
		
		/**
		 * Display the name of the ability, without the value, unconditionally.<br>
		 * Used by other types, not really useful on its own.
		 */
		public static final DisplayType NAME_ONLY_ALWAYS = new DisplayType() {
			@Override
			public Optional<MutableComponent> format(IAbility ability, float value) {
				return Optional.of(ability.getDisplayName());
			}
		};
		
		/**
		 * Display as {@link #DEFAULT} only if not zero
		 * @see #BOOL
		 */
		public static final DisplayType NON_ZERO = filter(v -> v != 0F, DEFAULT, HIDE);
		
		/**
		 * Display with format {@code %s: %+.1f}<br>
		 */
		public static final DisplayType SUM = formatValue("%+.1f");

		/**
		 * Display as {@link #SUM} only if not zero
		 */
		public static final DisplayType SUM_NON_ZERO = filter(v -> v != 0F, SUM, HIDE);
		
		/**
		 * Display as a multiplier with format {@code %s: ×%.1f}
		 */
		public static final DisplayType SCALE = formatValue("×%.1f");
		
		/**
		 * Display as {@link #SCALE} if the value is not {@code 1}<br>
		 * Otherwise, the ability is not displayed at all
		 */
		@SuppressWarnings("unused")
		public static final DisplayType SCALE_NON_ONE = filter(v -> v != 1F, SCALE, HIDE);
		
		/**
		 * Display the name of the ability, if the value is non-zero
		 */
		public static final DisplayType BOOL = filter(v -> v != 0F, NAME_ONLY_ALWAYS, HIDE, true);
		
		/**
		 * Display as {@link #BOOL} if the value is 0 or 1<br>
		 * Otherwise, display as {@link #SCALE}<br>
		 * Can be thought of as a {@link #SCALE_NON_ONE} that
		 * additionally is not displayed when the value is 0
		 */
		public static final DisplayType SCALE_BOOL =
		  filter(v -> v == 1F || v == 0F, BOOL, SCALE, true);
		
		
		/**
		 * Create a simple DisplayType from a format string
		 * @param format Applied to the value only
		 */
		public static DisplayType formatValue(String format) {
			return formatValue(v -> String.format(format, v));
		}
		
		/**
		 * Create a simple DisplayType from a function
		 */
		public static DisplayType formatValue(Function<Float, String> formatter) {
			return formatValue(v -> stc(formatter.apply(v)), ChatFormatting.DARK_AQUA);
		}
		
		/**
		 * Create a simple DisplayType which applies a function to the
		 * value which returns the ITextComponent to use
		 * @param format Applied to the formatted value
		 */
		public static DisplayType formatValue(
		  Function<Float, Component> formatter, ChatFormatting format) {
			return new DisplayType() {
				@Override public Optional<MutableComponent> format(IAbility ability, float value) {
					return Optional.of(
					  ability.getDisplayName().append(": ")
						 .append(formatter.apply(value).plainCopy().withStyle(format)));
				}
			};
		}
		
		/**
		 * Create a filtering {@link DisplayType}
		 * If the filter matches the value, {@code ifTrue} is used to display it.
		 * Otherwise, {@code ifFalse} is used
		 */
		public static DisplayType filter(
		  Predicate<Float> predicate, DisplayType ifTrue, DisplayType ifFalse
		) { return filter(predicate, ifTrue, ifFalse, false); }
		
		/**
		 * Create a filtering {@link DisplayType}
		 * If the filter matches the value, {@code ifTrue} is used to display it.
		 * Otherwise, {@code ifFalse} is used
		 * @param bool Whether to consider the result as a Bool type
		 */
		public static DisplayType filter(
		  Predicate<Float> predicate, DisplayType ifTrue, DisplayType ifFalse, boolean bool
		) {
			return new DisplayType(bool) {
				@Override public Optional<MutableComponent> format(IAbility ability, float value) {
					return predicate.test(value)? ifTrue.format(ability, value) : ifFalse.format(ability, value);
				}
			};
		}
	}
	
	static IAbility fromName(String name) {
		return AerobaticElytraRegistries.getAbilityByName(name);
	}
	
	static boolean isDefined(String jsonName) {
		return AerobaticElytraRegistries.hasAbilityName(jsonName);
	}
}
