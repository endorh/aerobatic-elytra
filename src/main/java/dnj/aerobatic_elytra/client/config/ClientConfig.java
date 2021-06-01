package dnj.aerobatic_elytra.client.config;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.simple_config.core.SimpleConfig;
import dnj.simple_config.core.SimpleConfig.Builder;
import dnj.simple_config.core.SimpleConfig.Group;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static dnj.simple_config.core.SimpleConfig.group;

public class ClientConfig {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void register() {
		final double INF = Double.POSITIVE_INFINITY;
		new Builder(AerobaticElytra.MOD_ID, Type.CLIENT)
		  .n(group("controls", true)
		       .add("pitch_sens", 1F, 0F, INF)
		       .add("roll_sens", 1F, 0F, INF)
		       .add("yaw_sens", 1F, 0F, INF)
		       .add("invert_pitch", false)
		       .add("invert_front_third_person", false)
		  ).n(group("style")
		       .add("flight_crosshair", true)
		       .add("flight_bar", FlightBarDisplay.OVER_XP)
		       .add("mode_popup_length_seconds", 2.0, 0, INF)
		       .add("mode_popup_x_percentage", 50, 0, 100, true)
		       .add("mode_popup_y_percentage", 70, 0, 100, true)
		       .add("enchantment_glint_visibility", Visibility.ALT_UP)
		       .add("fuel_visibility", Visibility.SHIFT_DOWN)
		       .add("fuel_display", FuelDisplay.ROCKETS)
		       .add("wing_glint_alpha", 0.6, 0, 1)
		       .add("fov_effect_strength", 1.0, 0, INF)
		      //.add("default_color", new Color(0xBAC1DB), true)
		  ).setBaker(ClientConfig::bakeSimpleClientConfig)
		  .setDecorator(
		    (config, builder) -> {
			    builder.setDefaultBackgroundTexture(
			      new ResourceLocation("textures/block/birch_planks.png"));
		    })
		  .build();
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
		Group controls = config.getGroup("controls");
		Group style = config.getGroup("style");
		
		pitch_sens = controls.getFloat("pitch_sens");
		roll_sens = controls.getFloat("roll_sens");
		yaw_sens = controls.getFloat("yaw_sens");
		
		invert_pitch = controls.get("invert_pitch");
		invert_front_third_person = controls.get("invert_front_third_person");
		
		
		flight_crosshair = style.get("flight_crosshair");
		
		mode_popup_length_millis = style.getDouble("mode_popup_length_seconds") * 1000D;
		mode_popup_x_fraction = style.getInt("mode_popup_x_percentage") / 100F;
		mode_popup_y_fraction = style.getInt("mode_popup_y_percentage") / 100F;
		
		glint_visibility = style.get("enchantment_glint_visibility");
		fuel_visibility = style.get("fuel_visibility");
		fuel_display = style.get("fuel_display");
		
		flight_bar = style.get("flight_bar");
		
		aerobatic_flight_fov_strength = style.getFloat("fov_effect_strength");
		aerobatic_elytra_wing_glint_alpha = style.getFloat("wing_glint_alpha");
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
	
	private static ITextComponent enumName(String enumName, String name) {
		return new TranslationTextComponent(
		  "config." + AerobaticElytra.MOD_ID + ".enum." + enumName.toLowerCase() + "." + name.toLowerCase());
	}
	
	/*
	*//**
	 * Client configuration, not synced
	 *//*
	public static class Spec {
		
		public final DoubleValue pitch_sens;
		public final DoubleValue roll_sens;
		public final DoubleValue yaw_sens;
		
		public final BooleanValue invert_pitch;
		public final BooleanValue invert_front_third_person;
		
		public final BooleanValue flight_crosshair;
		public final BooleanValue flight_bar_old;
		
		public final DoubleValue mode_popup_length_seconds;
		public final DoubleValue mode_popup_x_percentage;
		public final DoubleValue mode_popup_y_percentage;
		
		public final ForgeConfigSpec.EnumValue<FlightBarDisplay> flight_bar;
		
		public final EnumValue<Visibility> enchantment_glint_visibility;
		public final EnumValue<Visibility> fuel_visibility;
		
		public final EnumValue<FuelDisplay> fuel_display;
		
		public Spec(ForgeConfigSpec.Builder builder) {
			
			builder
			  .comment(" Controls tweaking")
			  .push("control");
			{
				pitch_sens = builder
				  .comment(" Pitch (vertical) sensibility")
				  .defineInRange("pitch_sens", ClientDefault.pitch_sens, 0F, 1000F);
				roll_sens = builder
				  .comment(" Roll (horizontal rotation) sensibility")
				  .defineInRange("roll_sens", ClientDefault.roll_sens, 0F, 1000F);
				yaw_sens = builder
				  .comment(" Yaw (horizontal) (strafing keys) sensibility")
				  .defineInRange("yaw_sens", ClientDefault.yaw_sens, 0F, 1000F);
				
				invert_pitch = builder
				  .comment(" Inverted flight pitch (will stack with the game setting)")
				  .define("inverted_pitch", ClientDefault.invert_pitch);
				invert_front_third_person = builder
				  .comment(" Invert controls naturally when in front third person view")
				  .define("inverted_front_third_person", ClientDefault.invert_front_third_person);
			}
			builder.pop();
			builder
			  .comment(" Style choices (remember you can use F1 to hide all GUIs)")
			  .push("style");
			{
				flight_crosshair = builder
				  .comment(" Render dynamic flight crosshair in first person flight")
				  .define("flight_crosshair", ClientDefault.flight_crosshair);
				flight_bar_old = builder
				  .comment(" Render flight bar over XP bar when flying")
				  .define("flight_bar_old", ClientDefault.flight_bar_old);
				flight_bar = builder
				  .comment(" How should the flight bar render")
				  .defineEnum("flight_bar", ClientDefault.flight_bar);
				mode_popup_length_seconds = builder
				  .comment(" How long should last the flight mode popup in seconds")
				  .defineInRange("mode_popup_length", ClientDefault.mode_popup_length_seconds, 0D, 1E10D);
				mode_popup_x_percentage = builder
				  .comment(" Horizontal position of the flight mode popup as percentage\n" +
				           " 0% is at the left border, 100% is at the right border")
				  .defineInRange("mode_popup_x_percentage", ClientDefault.mode_popup_x_percentage, 0D, 100D);
				mode_popup_y_percentage = builder
				  .comment(" Vertical position of the flight mode popup as percentage\n" +
				           " 0% is at the top border, 100% is at the bottom border")
				  .defineInRange("mode_popup_y_percentage", ClientDefault.mode_popup_y_percentage, 0D, 100D);
				
				enchantment_glint_visibility = builder
				  .comment(" When should the enchantment glint be displayed over the" +
				           " Aerobatic Elytra item")
				  .defineEnum("enchantment_glint_visibility", ClientDefault.enchantment_glint_visibility);
				fuel_visibility = builder
				  .comment(" When should the fuel meter be displayed over the" +
				           " Aerobatic Elytra item")
				  .defineEnum("fuel_visibility", ClientDefault.fuel_visibility);
				fuel_display = builder
				  .comment(" Where should the fuel meter be displayed over the" +
				           " Aerobatic Elytra item")
				  .defineEnum("fuel_display", ClientDefault.fuel_display);
			}
			builder.pop();
		}
	}*/
}
