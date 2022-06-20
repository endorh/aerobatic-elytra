package endorh.aerobaticelytra.network;

import endorh.aerobaticelytra.debug.Debug;
import endorh.util.network.ServerPlayerPacket;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class DebugPackets {
	public static void registerAll() {
		ServerPlayerPacket.with(NetworkHandler.CHANNEL, NetworkHandler.ID_GEN)
		  .register(SToggleDebugPacket::new);
	}
	
	public static class SToggleDebugPacket extends ServerPlayerPacket {
		private boolean enable;
		
		private SToggleDebugPacket() {}
		public SToggleDebugPacket(PlayerEntity player, boolean enable) {
			super(player);
			this.enable = enable;
		}
		
		@Override protected void onClient(PlayerEntity player, Context ctx) {
			if (player instanceof ClientPlayerEntity) {
				Debug.toggleDebug(player, enable);
			}
		}
		
		@Override protected void serialize(PacketBuffer buf) {
			buf.writeBoolean(enable);
		}
		@Override protected void deserialize(PacketBuffer buf) {
			enable = buf.readBoolean();
		}
		
		public void send() {
			sendTo(player);
		}
	}
}
