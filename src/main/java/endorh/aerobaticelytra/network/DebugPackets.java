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
		  .register(SDebugSettingsPacket::new);
	}
	
	public static class SDebugSettingsPacket extends ServerPlayerPacket {
		private Debug debug = Debug.DEBUG;
		
		public SDebugSettingsPacket() {}
		
		public SDebugSettingsPacket(PlayerEntity player, Debug debug) {
			super(player);
			this.debug = debug;
		}
		
		@Override protected void onClient(PlayerEntity player, Context ctx) {
			if (player instanceof ClientPlayerEntity)
				Debug.update(debug);
		}
		
		@Override protected void serialize(PacketBuffer buf) {
			debug.serialize(buf);
		}
		
		@Override protected void deserialize(PacketBuffer buf) {
			debug = Debug.deserialize(buf);
		}
	}
}
