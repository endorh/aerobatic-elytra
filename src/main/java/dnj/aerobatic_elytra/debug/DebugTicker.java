package dnj.aerobatic_elytra.debug;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

//@EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = AerobaticElytra.MOD_ID)
public class DebugTicker {
	private static final Logger LOGGER = LogManager.getLogger();
	
	@SubscribeEvent
	public static void onTick(WorldTickEvent event) {
		if (!Debug.isEnabled())
			return;
		final List<? extends PlayerEntity> players = event.world.getPlayers();
		if (!players.isEmpty()) {
			ServerPlayerEntity player = (ServerPlayerEntity) players.get(0);
			if (event.phase == Phase.START) {
				onPreTick(player);
			}
			else if (event.phase == Phase.END) {
				onPosTick(player);
			}
		}
	}
	
	public static void onPreTick(ServerPlayerEntity player) {
		/*int floatingTickCount = -1;
		if (TravelHandler.ServerPlayNetHandler$floatingTickCount != null) {
			try {
				floatingTickCount =
				  TravelHandler.ServerPlayNetHandler$floatingTickCount.getInt(player.connection);
			} catch (IllegalAccessException e) {
				LOGGER.debug("Couldn't read floating tick count");
			}
		} else {
			LogUtil.warnOnce(LOGGER, "Reflection for floating tick count has failed");
		}
		if (floatingTickCount != -1) {
			LOGGER.debug("Floating tick count: " + floatingTickCount);
		}*/
	}
	
	public static void onPosTick(ServerPlayerEntity player) {
	
	}
}
