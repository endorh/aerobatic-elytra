package endorh.aerobatic_elytra.client.config;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.simple_config.core.SimpleConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static endorh.simple_config.core.SimpleConfig.group;
import static endorh.simple_config.core.entry.Builders.*;

public class ClientConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void register() {
		SimpleConfig.builder(AerobaticElytra.MOD_ID, Type.CLIENT, ClientConfig.class)
		  .n(group("controls", true)
		       .add("pitch_sens", number(1.0).min(0))
		       .add("roll_sens", number(1.0).min(0))
		       .add("yaw_sens", number(1.0).min(0))
		       .add("invert_pitch", bool(false))
		       .add("invert_front_third_person", bool(false))
		  ).n(group("style")
		       .add("flight_crosshair", bool(true))
		       .add("flight_bar", enum_(FlightBarDisplay.OVER_XP))
		       .add("mode_popup_length_seconds", number(2.0).min(0))
		       .add("mode_popup_x_percentage", number(50, 100).slider())
		       .add("mode_popup_y_percentage", number(70, 100).slider())
		       .add("enchantment_glint_visibility", enum_(Visibility.ALT_UP))
		       .add("fuel_visibility", enum_(Visibility.SHIFT_DOWN))
		       .add("fuel_display", enum_(FuelDisplay.ROCKETS))
		       .add("wing_glint_alpha", fractional(0.6))
		       .add("fov_effect_strength", number(1.0).min(0))
		      //.add("default_color", new Color(0xBAC1DB), true)
		  ).setBaker(ClientConfig::bakeSimpleClientConfig)
		  .setGUIDecorator((config, builder) -> builder.setDefaultBackgroundTexture(
		      new ResourceLocation("textures/block/birch_planks.png")))
		  .buildAndRegister();
	}
	
	// Client config
	public static float pitch_sens;
	public static float roll_sens;
	public static float yaw_sens;
	public static boolean invert_pitch;
	public static boolean invert_front_third_person;
	public static boolean flight_crosshair;
	public static double mode_popup_length_millis;
	public static float mode_popup_x_fraction;
	public static float mode_popup_y_fraction;
	public static FlightBarDisplay flight_bar;
	public static Visibility glint_visibility;
	public static Visibility fuel_visibility;
	public static FuelDisplay fuel_display;
	
	public static double aerobatic_flight_fov_strength;
	public static float aerobatic_elytra_wing_glint_alpha;
	
	//public static Color aerobatic_elytra_default_color;
	
	public static void bakeSimpleClientConfig(SimpleConfig config) {
		LOGGER.debug("Baking client config");
		
		pitch_sens = config.getFloat("controls.pitch_sens");
		roll_sens = config.getFloat("controls.roll_sens");
		yaw_sens = config.getFloat("controls.yaw_sens");
		
		invert_pitch = config.get("controls.invert_pitch");
		invert_front_third_person = config.get("controls.invert_front_third_person");
		
		flight_crosshair = config.get("style.flight_crosshair");
		
		mode_popup_length_millis = config.getDouble("style.mode_popup_length_seconds") * 1000D;
		mode_popup_x_fraction = config.getInt("style.mode_popup_x_percentage") / 100F;
		mode_popup_y_fraction = config.getInt("style.mode_popup_y_percentage") / 100F;
		
		glint_visibility = config.get("style.enchantment_glint_visibility");
		fuel_visibility = config.get("style.fuel_visibility");
		fuel_display = config.get("style.fuel_display");
		
		flight_bar = config.get("style.flight_bar");
		
		aerobatic_flight_fov_strength = config.getFloat("style.fov_effect_strength");
		aerobatic_elytra_wing_glint_alpha = config.getFloat("style.wing_glint_alpha");
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
