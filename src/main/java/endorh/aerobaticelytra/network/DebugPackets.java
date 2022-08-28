package endorh.aerobaticelytra.network;

import endorh.aerobaticelytra.debug.Debug;
import endorh.util.network.ServerPlayerPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;

public class DebugPackets {
	public static void registerAll() {
		ServerPlayerPacket.with(NetworkHandler.CHANNEL, NetworkHandler.ID_GEN)
		  .register(SToggleDebugPacket::new);
	}
	
	public static class SToggleDebugPacket extends ServerPlayerPacket {
		private boolean enable;
		
		private SToggleDebugPacket() {}
		public SToggleDebugPacket(Player player, boolean enable) {
			super(player);
			this.enable = enable;
		}
		
		@Override protected void onClient(Player player, Context ctx) {
			if (player instanceof LocalPlayer) {
				Debug.toggleDebug(player, enable);
			}
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeBoolean(enable);
		}
		@Override protected void deserialize(FriendlyByteBuf buf) {
			enable = buf.readBoolean();
		}
		
		public void send() {
			sendTo(player);
		}
	}
}
