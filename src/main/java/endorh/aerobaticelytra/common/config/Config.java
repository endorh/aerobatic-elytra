package endorh.aerobaticelytra.common.config;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.api.annotation.Bind;
import endorh.simpleconfig.api.entry.FloatEntryBuilder;
import endorh.simpleconfig.api.range.FloatRange;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import static endorh.simpleconfig.api.ConfigBuilderFactoryProxy.*;
import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;
import static java.lang.Math.*;

public class Config {
	private static SimpleConfig SERVER = null;
	
	public static void register() {
		final String datapack_command = "/aerobaticelytra datapack list";
		SERVER = config(AerobaticElytra.MOD_ID, Type.SERVER, Config.class)
		  .n(group("item")
		       .add("durability", number(864).min(1))
		       .add("undamageable", yesNo(false))
		       .add("fix_nan_elytra_abilities", yesNo(false)))
		  .n(group("aerobatic")
		       .n(group("tilt")
		            .add("range_pitch", number(7.0F, 180))
		            .add("range_roll", number(9.0F, 180))
		            .add("range_yaw", number(0.7F, 180)))
		       .n(group("propulsion")
		            .add("range", range(0F, 2.4F)
		              .add_field_scale("tick", 0.05F))
		            .add("takeoff", tick(0.0F)))
		       .n(group("physics")
		            .add("gravity_multiplier", number(0.8F))
		            .add("glide_multiplier", number(1.0F))
		            .add("friction_base", number(0.95F, 0.5F, 1))
		            .add("friction_angular", number(0.94F, 0.5F, 1))
		            .add("inertia", number(0.15F, 0F, 1F))
		            .add("friction_water", number(0.85F, 0.5F, 1))
		            .add("friction_water_nerf", number(0.78F, 0.5F, 1))
		            .add("motorless_friction", number(0.99F, 0.5F, 1))
		            .add("motorless_gravity", tick(1.4F)))
		       .n(group("braking")
			         .caption("enabled", enable(true))
			         .add("max_time", number(2F).min(0)
			           .add_field_scale("ticks", 20F))
		            .add("friction", number(0.80F, 0.5F, 1))
		            .add("added_gravity", number(2.0F).min(0)
		              .add_field_scale("tick", 0.05F)))
		       .n(group("modes")
		            .add("allow_midair_change", yesNo(true))
		            .add("enable_normal_elytra_mode", enable(true))))
		  .n(group("fuel")
			    .add("usage_linear", tick(0.16F))
			    .add("usage_quad", tick(0.0F))
			    .add("usage_sqrt", tick(0.10F))
		       .add("usage_boost_multiplier", number(2F)))
		  .n(group("weather")
		       .caption("enabled", enable(true))
		       .add("ignore_cloud_level", yesNo(false))
		       .add("cloud_level", number(128))
		       .n(group("rain")
		            .add("rain_strength", tick(0.6F))
		            .add("wind_strength", tick(0.4F))
		            .add("wind_randomness", tick(4.0F))
		            .add("wind_angular_strength", tick(10.0F)))
		       .n(group("storm")
		            .add("rain_strength", tick(0.4F))
		            .add("wind_strength", tick(0.8F))
		            .add("wind_randomness", tick(4.0F))
		            .add("wind_angular_strength", tick(20.0F))))
		  .n(group("network")
		       .add("disable_aerobatic_elytra_movement_check", yesNo(false))
		       .add("aerobatic_elytra_movement_check", number(500.0F).min(0))
		       .add("disable_aerobatic_elytra_rotation_check", yesNo(false))
		       .add("aerobatic_elytra_rotation_check_overlook", number(1.1F).min(1))
		       .add("speed_cap", tick(200.0F).min(0))
		       .add("invalid_packet_kick_count", number(0).min(0)))
		  .n(group("collision")
		       .add("damage", number(1.0F).min(0))
		       .add("hay_bale_multiplier", number(0.2F).min(0))
		       .n(group("leave_breaking")
		            .caption("enable", enable(true))
		            .add("min_speed", tick(5F).min(0))
		            .add("chance", number(0.4F))
		            .add("chance_linear", number(0.08F))
		            .add("motion_multiplier", number(0.98F).min(0.1F))
		            .add("regrow_chance", fraction(0.4F)))
		       .n(group("slime_bounce")
		            .caption("enable", enable(true))
		            .add("min_speed", tick(4.0F).min(0))
		            .add("friction", fraction(0.98F))
		            .add("angular_friction", fraction(1.0F))))
		  .text(() -> ttc(
			 "aerobaticelytra.config.text.datapack_tip",
			 stc(datapack_command)
			   .withStyle(style -> style
			     .withColor(ChatFormatting.GOLD)
				  .withHoverEvent(new HoverEvent(
				    HoverEvent.Action.SHOW_TEXT, ttc("chat.copy.click")))
				  // The SUGGEST_COMMAND action requires the chat to be open
				  //.setClickEvent(new ClickEvent(
				  //  ClickEvent.Action.SUGGEST_COMMAND, datapack_command))
				  .withClickEvent(new ClickEvent(
				    ClickEvent.Action.COPY_TO_CLIPBOARD, datapack_command)))))
		  .buildAndRegister();
	}
	
	private static FloatEntryBuilder tick(float v) {
		return number(v).add_field_scale("tick", 0.05F);
	}
	
	@Bind
	public static class item {
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
			
			static void bake() {
				range_pondered =
				  range_pitch * range_pitch
				  + range_roll * range_roll
				  + 0.5F * range_yaw * range_yaw;
			}
		}
		
		@Bind public static class propulsion {
			@Bind public static FloatRange range_tick;
			@Bind public static float takeoff_tick;
			public static float span;
			public static float range_length;
			public static float positive_span;
			public static float negative_span;
			
			static void bake() {
				float min_tick = range_tick.getFloatMin();
				float max_tick = range_tick.getFloatMax();
				range_length = (float) range_tick.getSize();
				span = max(abs(min_tick), abs(max_tick));
				positive_span = max_tick - max(0F, min_tick);
				negative_span = -min_tick + min(0F, max_tick);
			}
		}
		
		@Bind public static class physics {
			@Bind public static float gravity_multiplier;
			@Bind public static float glide_multiplier;
			@Bind public static float friction_base;
			@Bind public static float friction_angular;
			@Bind public static float friction_water_nerf;
			@Bind public static float friction_water;
			@Bind public static float motorless_friction;
			@Bind public static float motorless_gravity_tick;
			@Bind public static float inertia;
		}
		
		@Bind public static class braking {
			@Bind public static boolean enabled;
			@Bind public static float friction;
			@Bind public static float added_gravity_tick;
			@Bind public static float max_time_ticks;
		}
		
		@Bind public static class modes {
			@Bind public static boolean allow_midair_change;
			@Bind public static boolean enable_normal_elytra_mode;
		}
	}
	
	@Bind public static class fuel {
		@Bind public static float usage_linear_tick;
		@Bind public static float usage_quad_tick;
		@Bind public static float usage_sqrt_tick;
		@Bind public static float usage_boost_multiplier;
	}
	
	@Bind public static class weather {
		@Bind public static boolean enabled;
		@Bind public static boolean ignore_cloud_level;
		@Bind public static int cloud_level;
		@Bind public static class rain {
			@Bind public static float rain_strength_tick;
			@Bind public static float wind_strength_tick;
			@Bind public static float wind_randomness_tick;
			@Bind public static float wind_angular_strength_tick;
		}
		@Bind public static class storm {
			@Bind public static float rain_strength_tick;
			@Bind public static float wind_strength_tick;
			@Bind public static float wind_randomness_tick;
			@Bind public static float wind_angular_strength_tick;
		}
	}
	
	@Bind public static class network {
		@Bind public static boolean disable_aerobatic_elytra_movement_check;
		@Bind public static float aerobatic_elytra_movement_check;
		@Bind public static boolean disable_aerobatic_elytra_rotation_check;
		@Bind public static float aerobatic_elytra_rotation_check_overlook;
		@Bind public static float speed_cap_tick;
		@Bind public static int invalid_packet_kick_count;
	}
	
	@Bind public static class collision {
		@Bind public static float damage;
		@Bind public static float hay_bale_multiplier;
		
		@Bind public static class leave_breaking {
			@Bind public static boolean enable;
			@Bind public static float min_speed_tick;
			@Bind public static float chance;
			@Bind public static float chance_linear;
			@Bind public static float motion_multiplier;
			@Bind public static float regrow_chance;
		}
		
		@Bind public static class slime_bounce {
			@Bind public static boolean enable;
			@Bind public static float min_speed_tick;
			@Bind public static float friction;
			@Bind public static float angular_friction;
		}
	}
}