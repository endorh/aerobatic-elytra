package dnj.aerobatic_elytra.debug;

import dnj.aerobatic_elytra.common.capability.AerobaticDataCapability;
import dnj.aerobatic_elytra.common.capability.IAerobaticData;
import dnj.aerobatic_elytra.common.flight.AerobaticFlight.VectorBase;
import dnj.aerobatic_elytra.common.config.Config;
import dnj.aerobatic_elytra.common.flight.WeatherData;
import dnj.aerobatic_elytra.common.flight.WeatherData.WeatherRegion;
import dnj.aerobatic_elytra.common.flight.WeatherData.WindRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Show various debug info in the Debug Screen
 */
//@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class DebugOverlay {
	public static float sound = 0F;
	public static float animation = 0F;
	
	@SubscribeEvent
	public static void onDebugScreenRenderEvent(RenderGameOverlayEvent.Text event) {
		if (!Debug.isEnabled())
			return;
		if (event.getType() == ElementType.TEXT
		    && !Minecraft.getInstance().gameSettings.showDebugInfo) {
			event.getLeft().addAll(getLeftInfo());
			event.getRight().addAll(getRightInfo());
		}
	}
	
	// Server config
	public static List<String> getLeftInfo() {
		ArrayList<String> ret = new ArrayList<>();
		
		ret.add(format("crafting_enabled: %b", Config.crafting_enabled));
		ret.add(format("acrobatic_flight_enabled: %b", Config.aerobatic_flight_enabled));
		
		ret.add("");
		
		/*ret.add(format("max_fuel: %.2f", Config.max_fuel));
		ret.add(format("max_speed_upgrades: %d", Config.max_speed_upgrades));
		ret.add(format("max_fuel_upgrades: %d", Config.max_fuel_upgrades));*/
		ret.add(format("durability: %d", Config.durability));
		
		ret.add("");
		
		ret.add("Loaded regions: ");
		synchronized (WeatherData.weatherRegions) {
			for (World world : WeatherData.weatherRegions.keySet()) {
				final Map<Pair<Long, Long>, WeatherRegion> worldRegions =
				  WeatherData.weatherRegions.get(world);
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (worldRegions) {
					ret.add(format("  World: %s: %d", world, worldRegions.size()));
					/*if (Screen.hasAltDown()) {
						for (WeatherRegion region : worldRegions.values()) {
							ret.add(format("    %s", region));
						}
					}*/
				}
			}
		}
		
		ret.add("");
		
		/*ret.add(format("tilt_range_pitch: %.2f", Config.tilt_range_pitch));
		ret.add(format("tilt_range_roll: %.2f", Config.tilt_range_roll));
		ret.add(format("tilt_range_yaw: %.2f", Config.tilt_range_yaw));
		
		ret.add(format("propulsion_min: %.2f", Config.propulsion_min));
		ret.add(format("propulsion_max: %.2f", Config.propulsion_max));
		ret.add(format("propulsion_takeoff: %.2f", Config.propulsion_takeoff));
		
		ret.add("");
		
		ret.add(format("jetpack_prop_max: %.2f", Config.jetpack_propulsion_max_per_tick));
		ret.add(format("jetpack_tilt_range: %.2f", Config.jetpack_horizontal_tilt_range));*/
		
		ret.add("");
		
		PlayerEntity player = Minecraft.getInstance().player;
		if (player != null) {
			ret.add(format("yaw:  %.2f", player.rotationYaw));
			ret.add(format("yawh: %.2f", player.getRotationYawHead()));
		}
		
		ret.add("");
		
		ret.add(format("Sound: %.2f", sound));
		ret.add(format("Animation: %.2f", animation));
		
		ret.add("");
		
		
		/*ret.add(format("friction_brake: %.2f", Config.friction_brake));
		ret.add(format("friction_base: %.2f", Config.friction_base));
		ret.add(format("friction_angular: %.2f", Config.friction_angular));
		ret.add(format("collision_damage: %.2f", Config.collision_damage));
		
		ret.add("");
		
		ret.add(format("gravity_multiplier: %.2f", Config.gravity_multiplier));
		ret.add(format("glide_multiplier: %.2f", Config.glide_multiplier));
		
		ret.add(format("brake_gravity: %.2f", Config.brake_gravity));
		ret.add(format("speed_cap: %.2f", Config.speed_cap));
		
		ret.add(format("fuel_usage_linear: %.2f", Config.fuel_usage_linear));
		ret.add(format("fuel_usage_quad: %.2f", Config.fuel_usage_quad));
		
		ret.add("");*/
		
		return ret;
	}
	
	// Client config
	public static List<String> getRightInfo() {
		PlayerEntity player = Minecraft.getInstance().player;
		assert player != null;
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		ArrayList<String> ret = new ArrayList<>();
		
		VectorBase rotation = data.getRotationBase();
		
		ret.add("Look: " + rotation.look.toString());
		ret.add("Normal: " + rotation.normal.toString());
		ret.add("Roll: " + rotation.roll.toString());
		
		ret.add("");
		
		ret.add(format("Tilt Pitch: %+2.3f", data.getTiltPitch()));
		ret.add(format("Tilt Roll: %+2.3f", data.getTiltRoll()));
		ret.add(format("Tilt Yaw: %+2.3f", data.getTiltYaw()));
		
		ret.add("");
		
		float pitch = player.rotationPitch;
		
		ret.add(format("Yaw: %+2.3f", ((player.rotationYaw % 360F) + 360F * 2) % 360F));
		if (Math.abs(pitch) >= 89.9F)
			ret.add(format(">> Pitch: %+2.3f", player.rotationPitch));
		else
			ret.add(format("Pitch: %+2.3f", player.rotationPitch));
		ret.add(format("Roll: %+2.3f", data.getRotationRoll()));
		
		ret.add("");
		
		ret.add(format("rain rain strength: %+2.3f", Config.rain_rain_strength_per_tick));
		ret.add(format("storm rain strength: %+2.3f", Config.storm_rain_strength_per_tick));
		
		ret.add(format("rain wind strength: %+2.3f", Config.rain_wind_strength_per_tick));
		ret.add(format("rain wind randomness: %+2.3f", Config.rain_wind_randomness_per_tick));
		ret.add(format("rain wind angular strength: %+2.3f", Config.rain_wind_angular_strength_per_tick));
		
		ret.add(format("storm wind strength: %+2.3f", Config.storm_wind_strength_per_tick));
		ret.add(format("storm wind randomness: %+2.3f", Config.storm_wind_randomness_per_tick));
		ret.add(format("storm wind angular strength: %+2.3f", Config.storm_wind_angular_strength_per_tick));
		
		WindRegion node = WindRegion.of(
		  player.world, WeatherRegion.scale(player.getPosX()), WeatherRegion.scale(player.getPosZ()));
		ret.add("Wind: " + node.wind);
		ret.add("Angular Wind: " + node.angularWind);
		
		ret.add("");
		
		/*ret.add(format("inverted_pitch: %b", Config.inverted_pitch));
		ret.add(format("inverted_front_third_person: %b", Config.inverted_front_third_person));
		
		ret.add("");
		
		ret.add(format("pitch_sens: %.2f", Config.pitch_sens));
		ret.add(format("roll_sens: %.2f", Config.roll_sens));
		ret.add(format("yaw_sens: %.2f", Config.yaw_sens));
		
		ret.add("");
		
		ret.add(format("flight_crosshair: %b", Config.flight_crosshair));
		ret.add(format("flight_bar_old: %s", Config.flight_bar_old));
		
		ret.add("");
		
		ret.add(format("Rain strength: %.2f", player.world.rainingStrength));
		ret.add(format("Thunder strength: %.2f", player.world.thunderingStrength));
		
		ret.add("");*/
		
		ret.add(format("Speed: %.2f", player.getMotion().length()));
		
		/*
		ret.add("");
		
		Set<WeatherRegion> playerRegions = WeatherRegion.of(player);
		ret.add(format("Player regions: %2d", playerRegions.size()));
		if (Screen.hasControlDown()) {
			for (WeatherRegion region : playerRegions) {
				ret.add((region.contains(player)? "✩" : "") + region.toString()
				        + region.affectedPlayers().size());
			}
		}*/
		
		//ret.add(format("SLOW_FALLING: %s", TravelHandler.SLOW_FALLING != null));
		
		ret.add("");
		
		/*ret.add(format("jetpack_fuel_usage_linear: %.2f", Config.jetpack_fuel_usage_linear));
		ret.add(format("jetpack_fuel_usage_quad: %.2f", Config.jetpack_fuel_usage_quad));
		ret.add(format("jetpack_fuel_usage_sqrt: %.2f", Config.jetpack_fuel_usage_sqrt));
		ret.add(format("jetpack_fuel_usage_hover: %.2f", Config.jetpack_fuel_usage_hover));
		
		ret.add("");
		
		ret.add(format("jetpack_height_penalty_max_height: %.2f",
		               Config.jetpack_height_penalty_max_height));
		ret.add(format("jetpack_height_penalty_min_height: %.2f",
		               Config.jetpack_height_penalty_min_height));
		ret.add(format("jetpack_height_penalty: %.2f", Config.jetpack_height_penalty));
		
		ret.add("");
		
		ret.add(format("jetpack_horizontal_tilt_range: %.2f", Config.jetpack_horizontal_tilt_range));
		ret.add(format("jetpack_hover_horizontal_speed_range: %.2f",
		               Config.jetpack_hover_horizontal_speed_range));
		ret.add(format("jetpack_hover_vertical_speed_range: %.2f",
		               Config.jetpack_hover_vertical_speed_range));
		
		ret.add("");
		
		ret.add(format("jetpack_propulsion_base: %.2f", Config.jetpack_propulsion_base_per_tick));
		ret.add(format("jetpack_propulsion_max: %.2f", Config.jetpack_propulsion_max_per_tick));
		ret.add(format("jetpack_charge_time: %.2f", Config.jetpack_charge_per_tick));
		ret.add(format("jetpack_cooldown_time: %.2f", Config.jetpack_cooldown_per_tick));*/
		
		return ret;
	}
}
