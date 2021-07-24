package endorh.aerobatic_elytra.client.config;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.simple_config.core.SimpleConfig;
import endorh.simple_config.core.SimpleConfigGroup;
import endorh.simple_config.core.annotation.Bind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import static endorh.simple_config.core.SimpleConfig.group;
import static endorh.simple_config.core.entry.Builders.*;
import static endorh.util.common.TextUtil.makeLink;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class ClientConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	private static SimpleConfig CONFIG;
	
	public static void register() {
		CONFIG = SimpleConfig.builder(AerobaticElytra.MOD_ID, Type.CLIENT, ClientConfig.class)
		  .n(group("controls", true)
		       .add("pitch_sens", number(1.0F).min(0))
		       .add("roll_sens", number(1.0F).min(0))
		       .add("yaw_sens", number(1.0F).min(0))
		       .add("invert_pitch", bool(false))
		       .add("invert_front_third_person", bool(false))
		  ).n(group("style")
		       .add("flight_crosshair", bool(true))
		       .add("flight_bar", enum_(FlightBarDisplay.OVER_XP))
		       .add("mode_popup_length_seconds", number(2.0).min(0))
		       .add("mode_popup_x_percentage", number(50F, 100).slider())
		       .add("mode_popup_y_percentage", number(70F, 100).slider())
		       .add("enchantment_glint_visibility", enum_(Visibility.ALT_UP))
		       .add("fuel_visibility", enum_(Visibility.SHIFT_DOWN))
		       .add("fuel_display", enum_(FuelDisplay.ROCKETS))
		       .add("disable_wing_glint", bool(false))
		       .add("fov_effect_strength", number(1.0F).min(0))
		       .text("dark_theme_desc",
		             makeLink("Default Dark Theme", "https://www.curseforge.com/minecraft/texture-packs/default-dark-mode"),
		             makeLink("Dark Mod GUIs", "https://www.curseforge.com/minecraft/texture-packs/dark-mod-guis"))
		       .add("dark_theme_gui", bool(false))
		       .add("auto_detect_dark_theme_gui", bool(true))
		       .add("auto_detect_dark_theme_gui_pattern", pattern("dark.*(gui|theme)")
		         .flags(Pattern.CASE_INSENSITIVE))
		      //.add("default_color", new Color(0xBAC1DB), true)
		  ).setGUIDecorator((config, builder) -> builder.setDefaultBackgroundTexture(
		      new ResourceLocation("textures/block/birch_planks.png")))
		  .buildAndRegister();
	}
	
	@Bind public static class controls {
		@Bind public static float pitch_sens;
		@Bind public static float roll_sens;
		@Bind public static float yaw_sens;
		
		@Bind public static boolean invert_pitch;
		@Bind public static boolean invert_front_third_person;
	}
	
	@Bind public static class style {
		@Bind public static boolean flight_crosshair;
		@Bind public static FlightBarDisplay flight_bar;
		public static long mode_popup_length_millis;
		public static float mode_popup_x_fraction;
		public static float mode_popup_y_fraction;
		@Bind public static Visibility enchantment_glint_visibility;
		@Bind public static Visibility fuel_visibility;
		@Bind public static FuelDisplay fuel_display;
		@Bind public static boolean disable_wing_glint;
		@Bind public static float fov_effect_strength;
		
		@Bind public static boolean dark_theme_gui;
		@Bind public static boolean auto_detect_dark_theme_gui;
		@Bind public static Pattern auto_detect_dark_theme_gui_pattern;
		
		static void bake(SimpleConfigGroup group) {
			mode_popup_length_millis = Math.round(group.getDouble("mode_popup_length_seconds") * 1000D);
			mode_popup_x_fraction = group.getFloat("mode_popup_x_percentage") / 100F;
			mode_popup_y_fraction = group.getFloat("mode_popup_y_percentage") / 100F;
			
			if (auto_detect_dark_theme_gui)
				autoEnableDarkTheme();
		}
	}
	
	public static void autoEnableDarkTheme() {
		final boolean dark =
		  Minecraft.getInstance().getResourcePackList().getEnabledPacks().stream().anyMatch(
			 p -> style.auto_detect_dark_theme_gui_pattern.asPredicate().test(p.getName()));
		if (dark && !style.dark_theme_gui) {
			CONFIG.set("style.dark_theme_gui", true);
			style.dark_theme_gui = true;
			LOGGER.info("Dark theme resource pack detected, enabling Aerobatic Elytra dark theme automatically");
		} else if (!dark && style.dark_theme_gui) {
			CONFIG.set("style.dark_theme_gui", false);
			style.dark_theme_gui = false;
			LOGGER.info("No dark theme resource pack detected, disabling Aerobatic Elytra dark theme automatically");
		}
	}
	
	public enum FlightBarDisplay {
		HIDE, OVER_XP, REPLACE_XP
	}
	
	public enum Visibility {
		ALWAYS(() -> true), NEVER(() -> false),
		SHIFT_DOWN(Screen::hasShiftDown),
		SHIFT_UP(Screen::hasShiftDown, true),
		CONTROL_DOWN(Screen::hasControlDown),
		CONTROL_UP(Screen::hasControlDown, true),
		ALT_DOWN(Screen::hasAltDown),
		ALT_UP(Screen::hasAltDown, true);
		
		private final Supplier<Boolean> check;
		private final boolean invert;
		
		Visibility(Supplier<Boolean> checkIn) {
			this(checkIn, false);
		}
		
		Visibility(Supplier<Boolean> checkIn, boolean invertIn) {
			check = checkIn;
			invert = invertIn;
		}
		
		public boolean test() {
			return invert != check.get();
		}
	}
	
	public enum FuelDisplay {
		ROCKETS, DURABILITY_BAR, DURABILITY_BAR_IF_LOWER
	}
}
