package endorh.aerobatic_elytra.server;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.network.AerobaticPackets.SAerobaticDataPacket;
import endorh.aerobatic_elytra.network.AerobaticPackets.SFlightDataPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import static endorh.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobatic_elytra.common.capability.FlightDataCapability.getFlightDataOrDefault;

/**
 * Data synchronization with clients
 */
@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class SyncHandler {
	@SubscribeEvent
	public static void onStartTracking(StartTracking event) {
		if (event.getTarget() instanceof PlayerEntity) {
			PlayerEntity tracked = (PlayerEntity)event.getTarget();
			ServerPlayerEntity player = (ServerPlayerEntity)event.getPlayer();
			new SFlightDataPacket(tracked).sendTo(player);
			new SAerobaticDataPacket(tracked).sendTo(player);
		}
	}
	
	@SubscribeEvent
	public static void onPlayerLogin(PlayerLoggedInEvent event) {
		update((ServerPlayerEntity)event.getPlayer());
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerRespawnEvent event) {
		reset((ServerPlayerEntity)event.getPlayer());
	}
	
	// TODO: Maybe test with Immersive Portals mod?
	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		reset((ServerPlayerEntity)event.getPlayer());
	}
	
	private static void update(ServerPlayerEntity player) {
		new SFlightDataPacket(player).sendTo(player);
		new SAerobaticDataPacket(player).sendTo(player);
	}
	private static void reset(ServerPlayerEntity player) {
		getFlightDataOrDefault(player).reset();
		getAerobaticDataOrDefault(player).reset();
		new SFlightDataPacket(player).sendTo(player);
		new SAerobaticDataPacket(player).sendTo(player);
	}
}
