package endorh.aerobaticelytra.network;

import endorh.aerobaticelytra.debug.Debug;
import endorh.lazulib.network.ServerPlayerPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;

public class DebugPackets {
	public static void registerAll() {
		ServerPlayerPacket.with(NetworkHandler.CHANNEL, NetworkHandler.ID_GEN)
		  .register(SDebugSettingsPacket::new);
	}
	
	public static class SDebugSettingsPacket extends ServerPlayerPacket {
		private Debug debug = Debug.DEBUG;
		
		public SDebugSettingsPacket() {}
		
		public SDebugSettingsPacket(Player player, Debug debug) {
			super(player);
			this.debug = debug;
		}
		
		@Override protected void onClient(Player player, Context ctx) {
			if (player instanceof LocalPlayer)
				Debug.update(debug);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			debug.serialize(buf);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			debug = Debug.deserialize(buf);
		}
	}
}
