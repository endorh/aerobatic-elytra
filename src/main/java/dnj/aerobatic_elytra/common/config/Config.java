package dnj.aerobatic_elytra.common.config;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.simple_config.core.SimpleConfig;
import dnj.simple_config.core.SimpleConfig.Builder;
import dnj.simple_config.core.SimpleConfig.Group;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static dnj.simple_config.core.SimpleConfig.group;
import static java.lang.Math.*;

public class Config {
	private static final Logger LOGGER = LogManager.getLogger();
	private static SimpleConfig SERVER = null;
	
	public static void register() {
		final double INF = Double.POSITIVE_INFINITY;
		final long MAX = Long.MAX_VALUE;
		final String datapack_command = "/aerobatic-elytra datapack install";
		SERVER = new Builder(AerobaticElytra.MOD_ID, Type.SERVER)
		  .n(group("item")
		       .add("durability", 432 * 2L, 0L, (long) 1E10)
		       .add("undamageable", false)
		       .add("fix_nan_elytra_abilities", false)
		  ).n(group("aerobatic")
		       .n(group("tilt", true)
		            .add("range_pitch", 7.0, 0, 180)
		            .add("range_roll", 9.0, 0, 180)
		            .add("range_yaw", 0.8, 0, 180)
		       ).n(group("propulsion", true)
		            .add("min", 0.0, -INF, INF)
		            .add("max", 2.4, -INF, INF, d ->
			           d < SERVER.getGroup("aerobatic").getGroup("propulsion").getDouble("min")
			           ? Optional.of(new TranslationTextComponent(
				          "config.aerobatic-elytra.error.max_min"))
			           : Optional.empty())
		            .add("takeoff", 0.0, -INF, INF)
			    ).n(group("physics")
		            .add("gravity_multiplier", 0.7, -INF, INF)
		            .add("glide_multiplier", 0.6, -INF, INF)
		            .add("friction_base", 0.96, 0.5, 1)
		            .add("friction_brake", 0.80, 0.5, 1)
		            .add("brake_gravity", 2.0, 0, INF)
		            .add("friction_angular", 0.92, 0.5, 1)
		            .add("friction_water_min", 0.92, 0.5, 1)
		            .add("friction_water_max", 0.82, 0.5, 1)
		            .add("motorless_friction", 0.99, 0.5, 1)
		            .add("motorless_gravity", 1.6, -INF, INF))
		  ).n(group("fuel")
			    .add("usage_linear", 0.06, -INF, INF)
			    .add("usage_quad", 0.0, -INF, INF)
			    .add("usage_sqrt", 0.04, -INF, INF)
		  ).n(group("weather")
			     .add("weather_enabled", true)
		        .n(group("rain", true)
		             .add("rain_strength", 0.6, -INF, INF)
		             .add("wind_strength", 0.4, -INF, INF)
		             .add("wind_randomness", 4.0, -INF, INF)
		             .add("wind_angular_strength", 10.0, -INF, INF)
		        ).n(group("storm", true)
		             .add("rain_strength", 0.4, -INF, INF)
		             .add("wind_strength", 0.8, -INF, INF)
		             .add("wind_randomness", 4.0, -INF, INF)
		             .add("wind_angular_strength", 20, -INF, INF))
		  ).n(group("network")
			     .add("disable_aerobatic_elytra_movement_check", false)
			     .add("aerobatic_elytra_movement_check", 500D, 0D, 1E10D)
			     .add("disable_aerobatic_elytra_rotation_check", false)
			     .add("aerobatic_elytra_rotation_check_overlook", 1.1, 1.0, INF)
			     .add("speed_cap", 200.0, 0, INF)
			     .add("invalid_packet_kick_count", 0, 0, MAX)
		  ).n(group("collision")
			     .add("collision_damage", 1.0, 0, INF)
			     .add("hay_bale_multiplier", 0.2, 0, INF)
			     .n(group("leave_breaking", true)
			          .add("enable", true)
			          .add("min_speed", 0.6, 0, INF)
			          .add("chance", 0.3, -INF, INF)
			          .add("chance_linear", 0.65, -INF, INF)
			          .add("motion_multiplier", 0.95, 0.1, INF)
			     ).n(group("slime_bounce", true)
			          .add("enable", true)
			          .add("min_speed", 0.4, 0, INF)
			          .add("friction", 0.98, 0, 1)
			          .add("angular_friction", 1.0, 0, 1))
		  ).text(() -> new TranslationTextComponent(
			 "config.aerobatic-elytra.text.datapack_tip",
			 new StringTextComponent(datapack_command)
				.modifyStyle(style -> style
				  .setFormatting(TextFormatting.GOLD)
				  // Unfortunately, the SUGGEST_COMMAND action requires the chat to be open
				  //.setClickEvent(new ClickEvent(
				  //  ClickEvent.Action.SUGGEST_COMMAND, datapack_command))
				  .setHoverEvent(new HoverEvent(
				    HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent("chat.copy.click")))
				  .setClickEvent(new ClickEvent(
				    ClickEvent.Action.COPY_TO_CLIPBOARD, datapack_command))
				)))
		  .setBaker(Config::bakeSimpleServerConfig)
		  .build();
	}
	
	public static void bakeSimpleServerConfig(SimpleConfig config) {
		LOGGER.debug("Baking server config");
		
		final Group item = config.getGroup("item");
		final Group aerobatic = config.getGroup("aerobatic");
		final Group tilt = aerobatic.getGroup("tilt");
		final Group propulsion = aerobatic.getGroup("propulsion");
		final Group physics = aerobatic.getGroup("physics");
		final Group fuel = config.getGroup("fuel");
		final Group weather = config.getGroup("weather");
		final Group rain = weather.getGroup("rain");
		final Group storm = weather.getGroup("storm");
		final Group network = config.getGroup("network");
		final Group collision = config.getGroup("collision");
		final Group leaves = collision.getGroup("leave_breaking");
		final Group slime = collision.getGroup("slime_bounce");
		
		durability = item.getInt("durability");
		damageable = !item.getBoolean("undamageable");
		fix_nan_elytra_abilities = item.get("fix_nan_elytra_abilities");
		
		tilt_range_pitch = tilt.getFloat("range_pitch");
		tilt_range_roll = tilt.getFloat("range_roll");
		tilt_range_yaw = tilt.getFloat("range_yaw");
		tilt_range_pondered = tilt_range_pitch * tilt_range_pitch
		                      + tilt_range_roll * tilt_range_roll
		                      + 0.5F * tilt_range_yaw * tilt_range_yaw;
		
		propulsion_min = propulsion.getFloat("min") / 20F;
		propulsion_max = propulsion.getFloat("max") / 20F;
		propulsion_takeoff = propulsion.getFloat("takeoff") / 20F;
		
		propulsion_range = max(abs(propulsion_min), abs(propulsion_max));
		propulsion_positive_range = propulsion_max - max(0F, propulsion_min);
		propulsion_negative_range = -propulsion_min + min(0F, propulsion_max);
		
		glide_multiplier = physics.getFloat("glide_multiplier");
		gravity_multiplier = physics.getFloat("gravity_multiplier");
		friction_base = physics.getFloat("friction_base");
		friction_brake = physics.getFloat("friction_brake");
		brake_gravity = physics.getFloat("brake_gravity") / 20F;
		friction_angular = physics.getFloat("friction_angular");
		friction_water_min = physics.getFloat("friction_water_min");
		friction_water_max = physics.getFloat("friction_water_max");
		motorless_friction = physics.getFloat("motorless_friction");
		motorless_gravity_per_tick = physics.getFloat("motorless_gravity") / 20F;
		
		fuel_usage_linear = fuel.getFloat("usage_linear") / 20F;
		fuel_usage_quad = fuel.getFloat("usage_quad") / 20F;
		fuel_usage_sqrt = fuel.getFloat("usage_sqrt") / 20F;
		
		weather_enabled = weather.get("weather_enabled");
		
		rain_rain_strength_per_tick = rain.getFloat("rain_strength") / 20F;
		rain_wind_strength_per_tick = rain.getFloat("wind_strength") / 20F;
		rain_wind_randomness_per_tick = rain.getFloat("wind_randomness") / 20F;
		rain_wind_angular_strength_per_tick = rain.getFloat("wind_angular_strength") / 20F;
		
		storm_rain_strength_per_tick = storm.getFloat("rain_strength") / 20F;
		storm_wind_strength_per_tick = storm.getFloat("wind_strength") / 20F;
		storm_wind_randomness_per_tick = storm.getFloat("wind_randomness") / 20F;
		storm_wind_angular_strength_per_tick = storm.getFloat("wind_angular_strength") / 20F;
		
		disable_aerobatic_elytra_movement_check = network.get("disable_aerobatic_elytra_movement_check");
		aerobatic_elytra_movement_check = network.getFloat("aerobatic_elytra_movement_check");
		disable_aerobatic_elytra_rotation_check = network.get("disable_aerobatic_elytra_rotation_check");
		aerobatic_elytra_rotation_check_overlook = network.getFloat("aerobatic_elytra_rotation_check_overlook");
		speed_cap_per_tick = network.getFloat("speed_cap") / 20F;
		invalid_packet_kick_count = network.getInt("invalid_packet_kick_count");
		
		collision_damage = collision.getFloat("collision_damage");
		hay_bale_collision_multiplier = collision.getFloat("hay_bale_multiplier");
		
		should_break_leaves = leaves.get("enable");
		break_leaves_min_speed = leaves.getFloat("min_speed");
		break_leaves_chance = leaves.getFloat("chance");
		break_leaves_chance_linear = leaves.getFloat("chance_linear");
		broken_leaves_motion_multiplier = leaves.getDouble("motion_multiplier");
		
		should_bounce_on_slime = slime.get("enable");
		slime_bounce_min_speed = slime.getFloat("min_speed");
		slime_bounce_friction = slime.getFloat("friction");
		slime_bounce_angular_friction = slime.getFloat("angular_friction");
	}
	
	// Server config
	public static boolean crafting_enabled;
	public static boolean aerobatic_flight_enabled;
	public static boolean disable_aerobatic_elytra_movement_check;
	public static float aerobatic_elytra_movement_check;
	
	public static int durability;
	public static boolean damageable;
	
	public static float tilt_range_pitch;
	public static float tilt_range_roll;
	public static float tilt_range_yaw;
	public static float tilt_range_pondered;
	
	public static float propulsion_min;
	public static float propulsion_max;
	public static float propulsion_takeoff;
	
	public static float propulsion_range;
	public static float propulsion_positive_range;
	public static float propulsion_negative_range;
	
	public static float friction_brake;
	public static float friction_base;
	public static float friction_angular;
	
	public static float friction_water_min;
	public static float friction_water_max;
	
	public static float gravity_multiplier;
	public static float glide_multiplier;
	
	public static float brake_gravity;
	public static float speed_cap_per_tick;
	
	public static float fuel_usage_linear;
	public static float fuel_usage_quad;
	public static float fuel_usage_sqrt;
	
	public static float motorless_friction;
	public static float motorless_gravity_per_tick;
	
	public static boolean weather_enabled;
	
	public static float rain_rain_strength_per_tick;
	public static float rain_wind_strength_per_tick;
	public static float rain_wind_randomness_per_tick;
	public static float rain_wind_angular_strength_per_tick;
	
	public static float storm_rain_strength_per_tick;
	public static float storm_wind_strength_per_tick;
	public static float storm_wind_randomness_per_tick;
	public static float storm_wind_angular_strength_per_tick;
	
	public static int invalid_packet_kick_count;
	
	public static boolean fix_nan_elytra_abilities;
	
	public static boolean disable_aerobatic_elytra_rotation_check;
	public static float aerobatic_elytra_rotation_check_overlook;
	
	public static float collision_damage;
	public static float hay_bale_collision_multiplier;
	
	public static boolean should_break_leaves;
	public static float break_leaves_min_speed;
	public static float break_leaves_chance;
	public static float break_leaves_chance_linear;
	public static double broken_leaves_motion_multiplier;
	
	public static boolean should_bounce_on_slime;
	public static float slime_bounce_min_speed;
	public static float slime_bounce_friction;
	public static float slime_bounce_angular_friction;
}