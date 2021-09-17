package endorh.aerobatic_elytra.common.item;

import com.google.common.base.CaseFormat;
import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.registry.ModRegistries;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;
import static endorh.aerobatic_elytra.common.item.IAbility.DisplayType.*;
import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;
import static net.minecraft.util.text.TextFormatting.*;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public interface IAbility extends IForgeRegistryEntry<IAbility> {
	/**
	 * Name used to parse in JSON, should be unique.<br>
	 */
	String getName();
	
	default String fullName() {
		assert getRegistryName() != null;
		return getRegistryName().getNamespace().replace('`', '_') + ':' + getName();
	}
	
	default void write(PacketBuffer buf) {
		buf.writeResourceLocation(Objects.requireNonNull(getRegistryName()));
	}
	
	static IAbility read(PacketBuffer buf) {
		return ModRegistries.getAbility(buf.readResourceLocation());
	}
	
	/**
	 * Name to be displayed in-game
	 */
	IFormattableTextComponent getDisplayName();
	
	/**
	 * Display type, used to show the ability in tooltips
	 */
	DisplayType getDisplayType();
	
	/**
	 * Default value when missing in item stack
	 */
	float getDefault();
	
	@Override default Class<IAbility> getRegistryType() {
		return IAbility.class;
	}
	
	/**
	 * Color used in syntax highlighting
	 * If null, a random one is chosen by hashing the name
	 */
	default @Nullable TextFormatting getColor() {
		return null;
	}
	
	enum Ability implements IAbility {
		FUEL(RED, 0F, DEFAULT),
		MAX_FUEL(LIGHT_PURPLE, 0F, DEFAULT),
		SPEED(GREEN, 1F, SCALE),
		LIFT(YELLOW, 0F, NON_ZERO),
		AQUATIC(BLUE, 1F, SCALE_BOOL),
		TRAIL(DARK_PURPLE, 1F, SCALE);
		
		private final ResourceLocation registryName;
		private final String jsonName;
		private final String translationKey;
		private final TextFormatting color;
		private final float defaultValue;
		private final DisplayType displayType;
		
		Ability(TextFormatting color, float defaultValue, DisplayType type) {
			this.registryName = prefix(name().toLowerCase());
			this.jsonName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
			this.translationKey = AerobaticElytra.MOD_ID + ".abilities." + name().toLowerCase();
			this.color = color;
			this.defaultValue = defaultValue;
			this.displayType = type;
		}
		
		@Override public String getName() { return jsonName; }
		@Override public IFormattableTextComponent getDisplayName() { return ttc(translationKey); }
		@Override public DisplayType getDisplayType() {
			return displayType;
		}
		
		@Override public TextFormatting getColor() { return color; }
		@Override public float getDefault() { return defaultValue; }
		
		@Override public ResourceLocation getRegistryName() { return registryName; }
		@Override public IAbility setRegistryName(ResourceLocation name) {
			throw new IllegalStateException("Cannot set registry name of enum registry entry");
		}
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
		public abstract Optional<IFormattableTextComponent> format(IAbility ability, float value);
		public boolean isBool() { return bool; }
		
		/**
		 * Display with format {@code %s: %3.1f}
		 */
		public static final DisplayType DEFAULT = formatValue("%3.1f");
		/**
		 * Display with format {@code %s: %3d}<br>
		 */
		@SuppressWarnings("unused")
		public static final DisplayType INTEGER = formatValue(
		  v -> String.format("%3d", Math.round(v)));
		
		/**
		 * Display the name of the ability, without the value, unconditionally.<br>
		 * Used by other types, not really useful on its own.
		 */
		public static final DisplayType NAME_ONLY_ALWAYS = new DisplayType() {
			@Override
			public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return Optional.of(ability.getDisplayName());
			}
		};
		
		/**
		 * Do not display
		 */
		public static DisplayType HIDE = new DisplayType() {
			@Override public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return Optional.empty();
			}
		};
		
		/**
		 * Display as {@link DisplayType#DEFAULT} only if not zero
		 * @see DisplayType#BOOL
		 */
		public static final DisplayType NON_ZERO = filter(v -> v != 0F, DEFAULT, HIDE);
		/**
		 * Display as a multiplier with format {@code %s: ×3.1f}
		 */
		public static final DisplayType SCALE = formatValue("×%3.1f");
		
		/**
		 * Display as {@link DisplayType#SCALE} if the value is not {@code 1}<br>
		 * Otherwise, the ability is not displayed at all
		 */
		@SuppressWarnings("unused")
		public static final DisplayType SCALE_NON_ONE = filter(v -> v != 1F, SCALE, HIDE);
		
		/**
		 * Display the name of the ability, if the value is non-zero
		 */
		public static final DisplayType BOOL = filter(v -> v != 0F, NAME_ONLY_ALWAYS, HIDE, true);
		
		/**
		 * Display as {@link DisplayType#BOOL} if the value is 0 or 1<br>
		 * Otherwise, display as {@link DisplayType#SCALE}<br>
		 * Can be thought of as a {@link DisplayType#SCALE_NON_ONE} that
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
			return formatValue(v -> stc(formatter.apply(v)), DARK_AQUA);
		}
		
		/**
		 * Create a simple DisplayType which applies a function to the
		 * value which returns the ITextComponent to use
		 * @param format Applied to the formatted value
		 */
		public static DisplayType formatValue(
		  Function<Float, ITextComponent> formatter, TextFormatting format) {
			return new DisplayType() {
				@Override public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
					return Optional.of(
					  ability.getDisplayName().appendString(": ")
						 .append(formatter.apply(value).copyRaw().mergeStyle(format)));
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
		 * @param bool Whether or not to consider the result as a Bool type
		 */
		public static DisplayType filter(
		  Predicate<Float> predicate, DisplayType ifTrue, DisplayType ifFalse, boolean bool
		) {
			return new DisplayType(bool) {
				@Override public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
					return predicate.test(value)? ifTrue.format(ability, value) : ifFalse.format(ability, value);
				}
			};
		}
	}
	
	static IAbility fromName(String name) {
		return ModRegistries.getAbilityByName(name);
	}
	
	static boolean isDefined(String jsonName) {
		return ModRegistries.hasAbilityName(jsonName);
	}
}
