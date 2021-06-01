package dnj.aerobatic_elytra.common.item;

import com.google.common.base.CaseFormat;
import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.registry.ModRegistries;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

import static dnj.aerobatic_elytra.common.item.IAbility.DisplayType.*;
import static dnj.endor8util.util.TextUtil.stc;
import static dnj.endor8util.util.TextUtil.ttc;
import static net.minecraft.util.text.TextFormatting.*;

public interface IAbility extends IForgeRegistryEntry<IAbility> {
	/**
	 * Name used to parse in JSON, should be unique.<br>
	 */
	String jsonName();
	
	default void write(PacketBuffer buf) {
		buf.writeResourceLocation(Objects.requireNonNull(getRegistryName()));
	}
	
	static IAbility read(PacketBuffer buf) {
		return ModRegistries.ABILITY_REGISTRY.getValue(buf.readResourceLocation());
	}
	
	/**
	 * Name to be displayed in-game
	 */
	TranslationTextComponent getDisplayName();
	
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
			this.registryName = new ResourceLocation(AerobaticElytra.MOD_ID, name().toLowerCase());
			this.jsonName = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
			this.translationKey = AerobaticElytra.MOD_ID + ".abilities." + name().toLowerCase();
			this.color = color;
			this.defaultValue = defaultValue;
			this.displayType = type;
		}
		
		@Override public String jsonName() { return jsonName; }
		@Override public TranslationTextComponent getDisplayName() { return ttc(translationKey); }
		@Override public DisplayType getDisplayType() {
			return displayType;
		}
		
		@Override public TextFormatting getColor() { return color; }
		@Override public float getDefault() { return defaultValue; }
		
		@Override public ResourceLocation getRegistryName() { return registryName; }
		@Override public Class<IAbility> getRegistryType() { return IAbility.class; }
		@Override public IAbility setRegistryName(ResourceLocation name) {
			throw new IllegalStateException("Cannot set registry name of enum registry entry");
		}
	}
	
	/**
	 * Ability display types
	 */
	abstract class DisplayType {
		public abstract Optional<IFormattableTextComponent> format(IAbility ability, float value);
		public boolean isBool() { return false; }
		public static abstract class BoolDisplayType extends DisplayType {
			@Override public boolean isBool() { return true; }
		}
		
		public static final DisplayType DEFAULT = new DisplayType() {
			@Override
			public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return Optional.of(ability.getDisplayName().appendString(": ")
				  .append(stc(String.format("%3.1f", value)).mergeStyle(DARK_AQUA)));
			}
		};
		public static final DisplayType NON_ZERO = new DisplayType() {
			@Override
			public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return value != 0F ? DEFAULT.format(ability, value) : Optional.empty();
			}
		};
		public static final DisplayType SCALE = new DisplayType() {
			@Override
			public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return Optional.of(
				  ability.getDisplayName().appendString(": ")
				    .append(stc(String.format("×%3.1f", value)).mergeStyle(DARK_AQUA)));
			}
		};
		@SuppressWarnings("unused")
		public static final DisplayType SCALE_NON_ONE = new DisplayType() {
			@Override
			public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return value != 1F? SCALE.format(ability, value) : Optional.empty();
			}
		};
		@SuppressWarnings("StaticInitializerReferencesSubClass")
		public static final DisplayType SCALE_BOOL = new BoolDisplayType() {
			@Override
			public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return (value == 1F || value == 0F)
				       ? BOOL.format(ability, value) : SCALE.format(ability, value);
			}
		};
		@SuppressWarnings("StaticInitializerReferencesSubClass")
		public static final DisplayType BOOL = new BoolDisplayType() {
			@Override
			public Optional<IFormattableTextComponent> format(IAbility ability, float value) {
				return value != 0F ? Optional.of(ability.getDisplayName()) : Optional.empty();
			}
		};
	}
	
	static IAbility fromJsonName(String jsonName) {
		return ModRegistries.abilityFromJsonName(jsonName);
	}
	static boolean isDefined(String jsonName) {
		return ModRegistries.hasAbility(jsonName);
	}
}
