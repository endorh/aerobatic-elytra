package endorh.aerobaticelytra.debug;

import endorh.aerobaticelytra.network.DebugPackets.SToggleDebugPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;

public class Debug {
	private static boolean registered = false;
	private static boolean enabled = false;
	
	public static void toggleDebug(PlayerEntity player, boolean enable) {
		if (enable && !registered)
			register();
		enabled = enable;
		if (!player.world.isRemote && player instanceof ServerPlayerEntity) {
			new SToggleDebugPacket(player, enable).sendTo((ServerPlayerEntity) player);
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
