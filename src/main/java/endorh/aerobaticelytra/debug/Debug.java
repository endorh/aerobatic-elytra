package endorh.aerobaticelytra.debug;

import endorh.aerobaticelytra.network.DebugPackets.SToggleDebugPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;

public class Debug {
	private static boolean registered = false;
	private static boolean enabled = false;
	
	public static void toggleDebug(Player player, boolean enable) {
		if (enable && !registered)
			register();
		enabled = enable;
		if (!player.level.isClientSide && player instanceof ServerPlayer) {
			new SToggleDebugPacket(player, enable).sendTo((ServerPlayer) player);
		}
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isEnabled() {
		return enabled;
	}
	
	public static void register() {
		if (registered)
			return;
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
		  MinecraftForge.EVENT_BUS.register(DebugOverlay.class));
		DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () ->
		  MinecraftForge.EVENT_BUS.register(DebugTicker.class));
		registered = true;
	}
}
