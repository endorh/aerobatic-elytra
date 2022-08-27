package endorh.aerobaticelytra.server;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import endorh.flightcore.events.DisableElytraCheckEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static endorh.util.text.TextUtil.ttc;

/**
 * Handle player kicks
 */
@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class KickHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final Map<UUID, Integer> INVALID_PACKET_COUNT = new HashMap<>();
	
	public static void incrementInvalidPacketCount(ServerPlayer player) {
		UUID id = player.getUUID();
		int count = INVALID_PACKET_COUNT.containsKey(id)?
		            INVALID_PACKET_COUNT.get(id) + 1 : 1;
		INVALID_PACKET_COUNT.put(id, count);
		if (Config.network.invalid_packet_kick_count > 0 && count >= Config.network.invalid_packet_kick_count) {
			INVALID_PACKET_COUNT.put(id, 0);
			player.connection.disconnect(ttc("aerobaticelytra.network.kick"));
			LOGGER.warn("Kicked player '" + player.getScoreboardName() + "' for " +
			            "reaching the limit of invalid aerobatic flight packets.\n" +
			            "Server config might be out of sync, or the player could be cheating.\n" +
			            "You may change the limit or disable it completely in the aerobatic elytra " +
			            "server config.");
		}
	}
	
	@SubscribeEvent
	public static void onDisableElytraCheck(DisableElytraCheckEvent event) {
		// Disabled by other mod
		if (event.getDisable())
			return;
		if (AerobaticFlight.isAerobaticFlying(event.player)) {
			if (Config.network.disable_aerobatic_elytra_movement_check
			    || event.excess <= event.stackedPackets * Config.network.aerobatic_elytra_movement_check) {
				event.setDisable(true);
			}
		}
	}
}
