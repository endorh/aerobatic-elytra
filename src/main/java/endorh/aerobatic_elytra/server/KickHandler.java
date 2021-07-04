package endorh.aerobatic_elytra.server;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.config.Config;
import endorh.flight_core.events.DisableElytraCheckEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static endorh.aerobatic_elytra.common.flight.AerobaticFlight.isAerobaticFlying;
import static endorh.util.common.TextUtil.ttc;

/**
 * Handle player kicks
 */
@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class KickHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	public static final Map<UUID, Integer> INVALID_PACKET_COUNT = new HashMap<>();
	
	public static void incrementInvalidPacketCount(ServerPlayerEntity player) {
		UUID id = player.getUniqueID();
		int count = INVALID_PACKET_COUNT.containsKey(id)?
		            INVALID_PACKET_COUNT.get(id) + 1 : 1;
		INVALID_PACKET_COUNT.put(id, count);
		if (Config.network.invalid_packet_kick_count > 0 && count >= Config.network.invalid_packet_kick_count) {
			INVALID_PACKET_COUNT.put(id, 0);
			player.connection.disconnect(ttc("aerobatic-elytra.network.kick"));
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
		if (isAerobaticFlying(event.player)) {
			if (Config.network.disable_aerobatic_elytra_movement_check
			    || event.excess <= event.stackedPackets * Config.network.aerobatic_elytra_movement_check) {
				event.setDisable(true);
			}
		}
	}
}
