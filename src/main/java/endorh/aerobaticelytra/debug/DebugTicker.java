package endorh.aerobaticelytra.debug;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DebugTicker {
	private static final Logger LOGGER = LogManager.getLogger();
	
	@SubscribeEvent
	public static void onTick(LevelTickEvent event) {
		if (!Debug.DEBUG.enabled)
			return;
		final List<? extends Player> players = event.level.players();
		if (!players.isEmpty()) {
			ServerPlayer player = (ServerPlayer) players.get(0);
			if (event.phase == Phase.START) {
				onPreTick(player);
			}
			else if (event.phase == Phase.END) {
				onPosTick(player);
			}
		}
	}
	
	public static void onPreTick(ServerPlayer player) {
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
	
	public static void onPosTick(ServerPlayer player) {
	
	}
}
