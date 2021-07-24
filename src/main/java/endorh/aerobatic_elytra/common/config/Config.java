package endorh.aerobatic_elytra.common.config;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.simple_config.core.SimpleConfig;
import endorh.simple_config.core.SimpleConfigGroup;
import endorh.simple_config.core.annotation.Bind;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.config.ModConfig.Type;

import java.util.Optional;

import static endorh.simple_config.core.SimpleConfig.group;
import static endorh.simple_config.core.entry.Builders.*;
import static endorh.util.common.TextUtil.stc;
import static endorh.util.common.TextUtil.ttc;
import static java.lang.Math.*;

public class Config {
	private static SimpleConfig SERVER = null;
	
	public static void register() {
		final String datapack_command = "/aerobatic-elytra datapack install";
		SERVER = SimpleConfig.builder(AerobaticElytra.MOD_ID, Type.SERVER, Config.class)
		  .n(group("item")
		       .add("durability", number(432 * 2).min(0))
		       .add("undamageable", bool(false))
		       .add("fix_nan_elytra_abilities", bool(false))
		  ).n(group("aerobatic")
		       .n(group("tilt", true)
		            .add("range_pitch", number(7.0F, 180))
		            .add("range_roll", number(9.0F, 180))
		            .add("range_yaw", number(0.8F, 180))
		       ).n(group("propulsion", true)
		            .add("min", number(0.0F))
		            .add("max", number(2.4F).error(
		              d -> d < SERVER.getFloat("aerobatic.propulsion.min")
		                   ? Optional.of(ttc("aerobatic-elytra.config.error.max_min"))
		                   : Optional.empty()))
		            .add("takeoff", number(0.0F))
			    ).n(group("physics")
		            .add("gravity_multiplier", number(0.7F))
		            .add("glide_multiplier", number(0.6F))
		            .add("friction_base", number(0.96F, 0.5F, 1))
		            .add("friction_brake", number(0.80F, 0.5F, 1))
		            .add("brake_gravity", number(2.0F).min(0))
		            .add("friction_angular", number(0.92F, 0.5F, 1))
		            .add("friction_water_min", number(0.92F, 0.5F, 1))
		            .add("friction_water_max", number(0.82F, 0.5F, 1))
		            .add("motorless_friction", number(0.99F, 0.5F, 1))
		            .add("motorless_gravity", number(1.6F)))
		  ).n(group("fuel")
			    .add("usage_linear", number(0.06F))
			    .add("usage_quad", number(0.0F))
			    .add("usage_sqrt", number(0.04F))
		       .add("usage_boost_multiplier", number(2F))
		  ).n(group("weather")
			     .add("enabled", bool(true))
		        .n(group("rain", true)
		             .add("rain_strength", number(0.6F))
		             .add("wind_strength", number(0.4F))
		             .add("wind_randomness", number(4.0F))
		             .add("wind_angular_strength", number(10.0F))
		        ).n(group("storm", true)
		             .add("rain_strength", number(0.4F))
		             .add("wind_strength", number(0.8F))
		             .add("wind_randomness", number(4.0F))
		             .add("wind_angular_strength", number(20.0F)))
		  ).n(group("network")
			     .add("disable_aerobatic_elytra_movement_check", bool(false))
			     .add("aerobatic_elytra_movement_check", number(500.0F).min(0))
			     .add("disable_aerobatic_elytra_rotation_check", bool(false))
			     .add("aerobatic_elytra_rotation_check_overlook", number(1.1F).min(1))
			     .add("speed_cap", number(200.0F).min(0))
			     .add("invalid_packet_kick_count", number(0).min(0))
		  ).n(group("collision")
			     .add("damage", number(1.0F).min(0))
			     .add("hay_bale_multiplier", number(0.2F).min(0))
			     .n(group("leave_breaking", true)
			          .add("enable", bool(true))
			          .add("min_speed", number(5F).min(0))
			          .add("chance", number(0.4F))
			          .add("chance_linear", number(0.08F))
			          .add("motion_multiplier", number(0.98F).min(0.1F))
			          .add("regrow_chance", fractional(0.4F))
			     ).n(group("slime_bounce", true)
			          .add("enable", bool(true))
			          .add("min_speed", number(4.2F).min(0))
			          .add("friction", fractional(0.98F))
			          .add("angular_friction", fractional(1.0F))))
		  .text(() -> ttc(
			 "aerobatic-elytra.config.text.datapack_tip",
			 stc(datapack_command)
				.modifyStyle(style -> style
				  .setFormatting(TextFormatting.GOLD)
				  .setHoverEvent(new HoverEvent(
				    HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
				  // The SUGGEST_COMMAND action requires the chat to be open
				  //.setClickEvent(new ClickEvent(
				  //  ClickEvent.Action.SUGGEST_COMMAND, datapack_command))
				  .setClickEvent(new ClickEvent(
				    ClickEvent.Action.COPY_TO_CLIPBOARD, datapack_command)))))
		  .buildAndRegister();
	}
	
	@Bind public static class item {
		@Bind public static int durability;
		@Bind public static boolean undamageable;
		@Bind public static boolean fix_nan_elytra_abilities;
	}
	
	@Bind public static class aerobatic {
		@Bind public static class tilt {
			@Bind public static float range_pitch;
			@Bind public static float range_roll;
			@Bind public static float range_yaw;
			public static float range_pondered;
			
			static void bake(SimpleConfigGroup g) {
				range_pondered = range_pitch * range_pitch
				                 + range_roll * range_roll
				                 + 0.5F * range_yaw * range_yaw;
			}
		}
		
		@Bind public static class propulsion {
			public static float min_tick;
			public static float max_tick;
			public static float takeoff_tick;
			public static float range;
			public static float positive_range;
			public static float negative_range;
			
			static void bake(SimpleConfigGroup g) {
				min_tick = g.getFloat("min") / 20F;
				max_tick = g.getFloat("max") / 20F;
				takeoff_tick = g.getFloat("takeoff") / 20F;
				
				range = max(abs(propulsion.min_tick), abs(propulsion.max_tick));
				positive_range = propulsion.max_tick - max(0F, propulsion.min_tick);
				negative_range = -propulsion.min_tick + min(0F, propulsion.max_tick);
			}
		}
		
		@Bind public static class physics {
			@Bind public static float gravity_multiplier;
			@Bind public static float glide_multiplier;
			@Bind public static float friction_base;
			@Bind public static float friction_brake;
			public static float brake_gravity_per_tick;
			@Bind public static float friction_angular;
			@Bind public static float friction_water_min;
			@Bind public static float friction_water_max;
			@Bind public static float motorless_friction;
			public static float motorless_gravity_per_tick;
			
			static void bake(SimpleConfigGroup g) {
				brake_gravity_per_tick = g.getFloat("brake_gravity") / 20F;
				motorless_gravity_per_tick = g.getFloat("motorless_gravity") / 20F;
			}
		}
	}
	
	@Bind public static class fuel {
		@Bind static float usage_linear;
		@Bind static float usage_quad;
		@Bind static float usage_sqrt;
		@Bind public static float usage_boost_multiplier;
		
		public static float usage_linear_tick;
		public static float usage_quad_tick;
		public static float usage_sqrt_tick;
		
		protected static void bake(SimpleConfigGroup g) {
			usage_linear_tick = usage_linear / 20F;
			usage_quad_tick = usage_quad / 20F;
			usage_sqrt_tick = usage_sqrt / 20F;
		}
	}
	
	@Bind public static class weather {
		@Bind public static boolean enabled;
		@Bind public static class rain {
			public static float rain_strength_per_tick;
			public static float wind_strength_per_tick;
			public static float wind_randomness_per_tick;
			public static float wind_angular_strength_per_tick;
			
			static void bake(SimpleConfigGroup g) {
				rain_strength_per_tick = g.getFloat("rain_strength") / 20F;
				wind_strength_per_tick = g.getFloat("wind_strength") / 20F;
				wind_randomness_per_tick = g.getFloat("wind_randomness") / 20F;
				wind_angular_strength_per_tick = g.getFloat("wind_angular_strength") / 20F;
			}
		}
		@Bind public static class storm {
			public static float rain_strength_per_tick;
			public static float wind_strength_per_tick;
			public static float wind_randomness_per_tick;
			public static float wind_angular_strength_per_tick;
			
			static void bake(SimpleConfigGroup g) {
				rain_strength_per_tick = g.getFloat("rain_strength") / 20F;
				wind_strength_per_tick = g.getFloat("wind_strength") / 20F;
				wind_randomness_per_tick = g.getFloat("wind_randomness") / 20F;
				wind_angular_strength_per_tick = g.getFloat("wind_angular_strength") / 20F;
			}
		}
	}
	
	@Bind public static class network {
		@Bind public static boolean disable_aerobatic_elytra_movement_check;
		@Bind public static float aerobatic_elytra_movement_check;
		@Bind public static boolean disable_aerobatic_elytra_rotation_check;
		@Bind public static float aerobatic_elytra_rotation_check_overlook;
		public static float speed_cap_tick;
		@Bind public static int invalid_packet_kick_count;
		
		static void bake(SimpleConfigGroup g) {
			speed_cap_tick = g.getFloat("speed_cap") / 20F;
		}
	}
	
	@Bind public static class collision {
		@Bind public static float damage;
		@Bind public static float hay_bale_multiplier;
		
		@Bind public static class leave_breaking {
			@Bind public static boolean enable;
			public static float min_speed_tick;
			@Bind public static float chance;
			@Bind public static float chance_linear;
			@Bind public static float motion_multiplier;
			@Bind public static float regrow_chance;
			
			static void bake(SimpleConfigGroup g) {
				min_speed_tick = g.getFloat("min_speed") / 20F;
			}
		}
		
		@Bind public static class slime_bounce {
			@Bind public static boolean enable;
			public static float min_speed_tick;
			@Bind public static float friction;
			@Bind public static float angular_friction;
			
			static void bake(SimpleConfigGroup g) {
				min_speed_tick = g.getFloat("min_speed") / 20F;
			}
		}
	}
}