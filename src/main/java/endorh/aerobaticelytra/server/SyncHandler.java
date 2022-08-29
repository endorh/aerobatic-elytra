package endorh.aerobaticelytra.server;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.network.AerobaticPackets.SAerobaticDataPacket;
import endorh.aerobaticelytra.network.AerobaticPackets.SFlightDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;

/**
 * Data synchronization with clients
 */
@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class SyncHandler {
	@SubscribeEvent
	public static void onStartTracking(StartTracking event) {
		if (event.getTarget() instanceof Player tracked) {
			ServerPlayer player = (ServerPlayer) event.getEntity();
			new SFlightDataPacket(tracked).sendTo(player);
			new SAerobaticDataPacket(tracked).sendTo(player);
		}
	}
	
	@SubscribeEvent
	public static void onPlayerLogin(PlayerLoggedInEvent event) {
		update((ServerPlayer) event.getEntity());
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerRespawnEvent event) {
		reset((ServerPlayer) event.getEntity());
	}
	
	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		// reset((ServerPlayerEntity) event.getEntity());
	}
	
	private static void update(ServerPlayer player) {
		new SFlightDataPacket(player).sendTo(player);
		new SAerobaticDataPacket(player).sendTo(player);
	}
	private static void reset(ServerPlayer player) {
		getFlightDataOrDefault(player).reset();
		getAerobaticDataOrDefault(player).reset();
		new SFlightDataPacket(player).sendTo(player);
		new SAerobaticDataPacket(player).sendTo(player);
	}
}
