package endorh.aerobaticelytra.debug;

import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.flight.VectorBase;
import endorh.aerobaticelytra.common.flight.WeatherData;
import endorh.aerobaticelytra.common.flight.WeatherData.WeatherRegion;
import endorh.aerobaticelytra.common.flight.WeatherData.WindRegion;
import endorh.util.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static java.lang.String.format;

/**
 * Show various debug info in the Debug Screen
 */
public class DebugOverlay {
	public static float sound = 0F;
	public static float animation = 0F;
	
	@SubscribeEvent
	public static void onDebugScreenRenderEvent(CustomizeGuiOverlayEvent.DebugText event) {
		if (Debug.DEBUG.enabled && !Minecraft.getInstance().options.renderDebug) {
			event.getLeft().addAll(getLeftInfo());
			event.getRight().addAll(getRightInfo());
		}
	}
	
	// Server config
	public static List<String> getLeftInfo() {
		List<String> ret = new ArrayList<>();
		
		ret.add("Loaded regions: ");
		synchronized (WeatherData.weatherRegions) {
			for (Level world: WeatherData.weatherRegions.keySet()) {
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
		
		Player player = Minecraft.getInstance().player;
		if (player != null) {
			IAerobaticData data = getAerobaticDataOrDefault(player);
			ret.add(format("yaw:  %.2f", player.getYRot()));
			ret.add(format("yawh: %.2f", player.getYHeadRot()));
			
			ret.add("");
			
			ret.add(format("Lift cut: %.2f", data.getLiftCut()));
			
			ret.add("");
			
			ret.add(format("Look Yaw: %.2f", data.getLookAroundYaw()));
			ret.add(format("Look Pitch: %.2f", data.getLookAroundPitch()));
		}
		
		ret.add("");
		
		ret.add(format("Sound: %.2f", sound));
		ret.add(format("Animation: %.2f", animation));
		
		ret.add("");
		
		return ret;
	}
	
	// Client config
	public static List<String> getRightInfo() {
		Player player = Debug.DEBUG.getTargetPlayer();
		assert player != null;
		List<String> ret = new ArrayList<>();
		
		ret.add("Debuggee: " + player.getScoreboardName());
		ret.add("");
		
		IAerobaticData data = getAerobaticDataOrDefault(player);
		VectorBase rotation = data.getRotationBase();
		
		ret.add("Look: " + rotation.look.toString());
		ret.add("Normal: " + rotation.normal.toString());
		ret.add("Roll: " + rotation.roll.toString());
		
		ret.add("");
		
		ret.add(format("Tilt Pitch: %+2.3f", data.getTiltPitch()));
		ret.add(format("Tilt Roll: %+2.3f", data.getTiltRoll()));
		ret.add(format("Tilt Yaw: %+2.3f", data.getTiltYaw()));
		
		ret.add("");
		
		float pitch = player.getXRot();
		
		ret.add(format("Yaw: %+2.3f", ((player.getYRot() % 360F) + 360F * 2) % 360F));
		if (Math.abs(pitch) >= 89.9F)
			ret.add(format(">> Pitch: %+2.3f", player.getXRot()));
		else
			ret.add(format("Pitch: %+2.3f", player.getXRot()));
		ret.add(format("Roll: %+2.3f", data.getRotationRoll()));
		
		ret.add("");
		
		WindRegion node = WindRegion.of(
		  player.level, WeatherRegion.scale(player.getX()), WeatherRegion.scale(player.getZ()));
		ret.add("Wind: " + node.wind);
		ret.add("Angular Wind: " + node.angularWind);
		
		ret.add(format("Affected by weather: %b", data.isAffectedByWeather()));
		
		ret.add("");
		
		ret.add(format("Speed: %.2f", player.getDeltaMovement().length()));
		ret.add(format("Motion: %s", new Vec3f(player.getDeltaMovement())));
		
		ret.add("");
		
		ret.add(format("Last trail pos: %s", data.getLastTrailPos().toString()));
		
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
		
		return ret;
	}
}
