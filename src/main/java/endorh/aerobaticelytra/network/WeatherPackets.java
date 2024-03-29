package endorh.aerobaticelytra.network;

import endorh.aerobaticelytra.common.flight.WeatherData;
import endorh.aerobaticelytra.common.flight.WeatherData.WindRegion;
import endorh.lazulib.math.Vec3f;
import endorh.lazulib.network.ServerWorldPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WeatherPackets {
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void registerAll() {
		ServerWorldPacket.with(NetworkHandler.CHANNEL, NetworkHandler.ID_GEN)
		  .register(SWindNodePacket::new);
	}
	
	public static class SWindNodePacket extends ServerWorldPacket {
		protected WindRegion node = null;
		protected long x;
		protected long z;
		public Vec3f wind;
		public Vec3f angularWind;
		
		private SWindNodePacket() {}
		
		public SWindNodePacket(WindRegion node) {
			super(node.region.world);
			this.node = node;
			x = node.region.x;
			z = node.region.z;
			wind = node.wind;
			angularWind = node.angularWind;
		}
		
		@Override public void onClient(Level world, Context ctx) {
			WindRegion node = WindRegion.of(world, x, z);
			node.update(this);
		}
		
		@Override public void serialize(FriendlyByteBuf buf) {
			buf.writeLong(x);
			buf.writeLong(z);
			wind.write(buf);
			angularWind.write(buf);
		}
		
		@Override public void deserialize(FriendlyByteBuf buf) {
			x = buf.readLong();
			z = buf.readLong();
			wind = Vec3f.read(buf);
			angularWind = Vec3f.read(buf);
		}
		
		public void sendTracking() {
			sendTarget(WeatherData.TRACKING_WEATHER_REGION.with(() -> node.region));
		}
	}
}
