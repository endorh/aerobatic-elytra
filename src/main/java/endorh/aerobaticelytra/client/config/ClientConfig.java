package endorh.aerobaticelytra.client.config;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig.style.dark_theme;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.annotation.Bind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.util.text.TextUtil.makeLink;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class ClientConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	private static SimpleConfig CONFIG;
	
	public static void register() {
		CONFIG = config(AerobaticElytra.MOD_ID, Type.CLIENT, ClientConfig.class)
		  .n(group("controls", true)
		       .add("pitch_sens", number(1.0F).min(0))
		       .add("roll_sens", number(1.0F).min(0))
		       .add("yaw_sens", number(1.0F).min(0))
		       .add("invert_pitch", yesNo(false))
		       .add("invert_front_third_person", yesNo(false)))
		  .n(group("style")
		       .n(group("visual")
		            .add("fov_effect_strength", number(1F).min(0))
		            .add("flight_crosshair", yesNo(true))
		            .add("flight_bar", option(FlightBarDisplay.OVER_XP))
		            .add("mode_toast_length_seconds", number(2.0).min(0)
		              .field("mode_toast_length_ms", s -> /*(Long)*/ (long) (s * 1000), Long.class))
		            .add("mode_toast_x_percentage", number(50F, 100).slider()
		              .fieldScale("mode_toast_x_fraction", 0.01F))
		            .add("mode_toast_y_percentage", number(70F, 100).slider()
		               .fieldScale("mode_toast_y_fraction", 0.01F))
		            .add("max_rendered_banner_layers", number(16).max(16)))
		       .n(group("visibility")
		            .add("fuel_display", option(FuelDisplay.ROCKETS))
		            .add("fuel_visibility", option(Visibility.ALWAYS))
		            .add("disable_wing_glint", yesNo(false))
		            .add("enchantment_glint_visibility", option(Visibility.ALT_UP)))
		       .n(group("dark_theme")
		            .text("desc",
		                  makeLink("Default Dark Theme", "https://www.curseforge.com/minecraft/texture-packs/default-dark-mode"),
		                  makeLink("Dark Mod GUIs", "https://www.curseforge.com/minecraft/texture-packs/dark-mod-guis"))
		            .caption("enabled", enable(false))
		            .add("auto_detect", yesNo(true))
		            .add("auto_detect_pattern", pattern("dark.*(gui|theme)")
			           .flags(Pattern.CASE_INSENSITIVE))))
		  .n(group("sound")
		       .caption("master", volume(1F))
		       .add("wind", volume(1F))
		       .add("rotating_wind", volume(1F))
		       .add("whistle", volume(1F))
		       .add("boost", volume(1F))
		       .add("brake", volume(1F)))
		      //.add("default_color", new Color(0xBAC1DB), true)
		  .withBackground("textures/block/birch_planks.png")
		  .buildAndRegister();
	}
	
	@Bind public static class controls {
		@Bind public static float pitch_sens;
		@Bind public static float roll_sens;
		@Bind public static float yaw_sens;
		
		@Bind public static boolean invert_pitch;
		@Bind public static boolean invert_front_third_person;
	}
	
	// @Bind does not compile here
	public static class style {
		@Bind public static class visual {
			@Bind public static float fov_effect_strength;
			@Bind public static boolean flight_crosshair;
			@Bind public static FlightBarDisplay flight_bar;
			@Bind public static long mode_toast_length_ms;
			@Bind public static float mode_toast_x_fraction;
			@Bind public static float mode_toast_y_fraction;
			@Bind public static int max_rendered_banner_layers;
		}
		
		@Bind public static class visibility {
			@Bind public static Visibility fuel_visibility;
			@Bind public static FuelDisplay fuel_display;
			@Bind public static boolean disable_wing_glint;
			@Bind public static Visibility enchantment_glint_visibility;
		}
		
		@Bind public static class dark_theme {
			@Bind public static boolean enabled;
			@Bind public static boolean auto_detect;
			@Bind public static Pattern auto_detect_pattern;
			
			static void bake() {
				if (auto_detect) autoEnableDarkTheme();
			}
		}
	}
	
	@Bind public static class sound {
		@Bind public static float master;
		@Bind public static float wind;
		@Bind public static float rotating_wind;
		@Bind public static float whistle;
		@Bind public static float boost;
		@Bind public static float brake;
		
		static void bake() {
			wind *= master;
			rotating_wind *= master;
			whistle *= master;
			boost *= master;
			brake *= master;
		}
	}
	
	public static void autoEnableDarkTheme() {
		final boolean dark =
		  Minecraft.getInstance().getResourcePackRepository().getSelectedPacks().stream().anyMatch(
			 p -> dark_theme.auto_detect_pattern.asPredicate().test(p.getId()));
		if (dark && !dark_theme.enabled) {
			CONFIG.set("style.dark_theme.enabled", true);
			dark_theme.enabled = true;
			LOGGER.info("Dark theme resource pack detected, enabling Aerobatic Elytra dark theme automatically");
		} else if (!dark && dark_theme.enabled) {
			CONFIG.set("style.dark_theme.enabled", false);
			dark_theme.enabled = false;
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
