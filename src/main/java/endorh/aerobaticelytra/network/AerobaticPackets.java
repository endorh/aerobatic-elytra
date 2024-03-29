package endorh.aerobaticelytra.network;

import endorh.aerobaticelytra.common.capability.AerobaticDataCapability;
import endorh.aerobaticelytra.common.capability.FlightDataCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IFlightData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.aerobatic.propulsion;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.flight.VectorBase;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.server.KickHandler;
import endorh.lazulib.math.Vec3f;
import endorh.lazulib.network.DistributedPlayerPacket;
import endorh.lazulib.network.ServerPlayerPacket;
import endorh.lazulib.network.ValidatedDistributedPlayerPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static endorh.aerobaticelytra.common.registry.AerobaticElytraRegistries.FLIGHT_MODE_REGISTRY;
import static java.lang.Math.abs;
import static java.lang.Math.max;

public class AerobaticPackets {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String invalidPacketWarnSuffix =
	  "\nServer config might be out of sync, the server could be lagging, " +
	  "or the player could be trying to cheat.";
	
	// Avoid logging the same warnings too frequently
	private static final int TIME_BETWEEN_WARNINGS = 40; // Ticks
	private static final Map<String, Map<String, Integer>> LAST_WARNS = new HashMap<>();
	
	private static boolean logWarning(Player player, String message, Object... args) {
		final String name = player.getScoreboardName();
		final int time = player.tickCount;
		if (!LAST_WARNS.computeIfAbsent(name, s -> new HashMap<>()).containsKey(message)
		    || time - LAST_WARNS.get(name).get(message) >= TIME_BETWEEN_WARNINGS) {
			Object[] formatArgs = new Object[args.length + 1];
			formatArgs[0] = player;
			System.arraycopy(args, 0, formatArgs, 1, args.length);
			LOGGER.warn(String.format(message, formatArgs));
			LAST_WARNS.get(name).put(message, time);
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	private static boolean handlePlayerWarning(Player player, String message, Object... args) {
		KickHandler.incrementInvalidPacketCount((ServerPlayer) player);
		return logWarning(player, message + invalidPacketWarnSuffix, args);
	}
	
	public static void registerAll() {
		DistributedPlayerPacket.with(NetworkHandler.CHANNEL, NetworkHandler.ID_GEN)
		  .registerLocal(DFlightModePacket::new)
		  .registerLocal(DTiltPacket::new)
		  .registerLocal(DAccelerationPacket::new)
		  .registerLocal(DSneakingPacket::new)
		  .registerLocal(DJumpingPacket::new)
		  .registerLocal(DSprintingPacket::new)
		  .registerLocal(DRotationPacket::new)
		  .registerLocal(DLookAroundPacket::new);
		ServerPlayerPacket.with(NetworkHandler.CHANNEL, NetworkHandler.ID_GEN)
		  .register(SFlightDataPacket::new)
		  .register(SAerobaticDataPacket::new);
	}
	
	/**
	 * {@link IAerobaticData} update packet<br>
	 * Sent by the player when its flight mode changes
	 */
	public static class DFlightModePacket extends ValidatedDistributedPlayerPacket {
		IFlightMode mode;
		
		private DFlightModePacket() {}
		
		public DFlightModePacket(IFlightMode mode) {
			this.mode = mode;
		}
		
		@Override protected void onServer(Player sender, Context ctx) {
			if (mode.canBeUsedBy(sender)) {
				getFlightDataOrDefault(sender).setFlightMode(mode);
			} else {
				mode = getFlightDataOrDefault(sender).getFlightMode();
				invalidate();
				handlePlayerWarning(
				  sender, "Player '%s' tried to use restricted flight mode: %s",
				  FLIGHT_MODE_REGISTRY.getKey(mode));
			}
		}
		
		@Override protected void onClient(Player sender, Context ctx) {
			getFlightDataOrDefault(sender).setFlightMode(mode);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			mode.write(buf);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			mode = IFlightMode.read(buf);
		}
	}
	
	public static class DRotationPacket extends ValidatedDistributedPlayerPacket {
		VectorBase rotation;
		
		public DRotationPacket() {}
		
		public DRotationPacket(IAerobaticData data) {
			rotation = data.getRotationBase();
		}
		
		@Override protected void onServer(Player sender, Context ctx) {
			IAerobaticData data = getAerobaticDataOrDefault(sender);
			if (!Config.network.disable_aerobatic_elytra_rotation_check && !sender.isInWater()) {
				MinecraftServer server = sender.getServer();
				assert server != null;
				long[] times = server.getTickTime(sender.level().dimension());
				long mspt = times != null? max(Arrays.stream(times).sum() / times.length, 50L) : 50L;
				float lag = max(50F, ((ServerPlayer) sender).latency);
				
				// Take lag into account when validating packets, plus extra tolerance
				float overlook =
				  (mspt / 50F) * (lag / 50F) * Config.network.aerobatic_elytra_rotation_check_overlook;
				// Multiply by motion strength, plus extra tolerance
				float mul =
				  overlook * max(0.2F, abs(new Vec3f(sender.getDeltaMovement()).dot(rotation.look)));
				
				float[] distance = data.getRotationBase().distance(rotation);
				float tiltYaw = validateClose(distance[0], 0F, Config.aerobatic.tilt.range_yaw * mul);
				float tiltPitch =
				  validateClose(distance[1], 0F, Config.aerobatic.tilt.range_pitch * mul);
				float tiltRoll = validateClose(distance[2], 0F, Config.aerobatic.tilt.range_roll * mul);
				
				if (isInvalid()) {
					data.getRotationBase().rotate(tiltPitch, tiltYaw, tiltRoll);
					handlePlayerWarning(sender, "Player '%s' rotated too fast!");
				} else {
					data.getRotationBase().set(rotation);
				}
			} else {
				data.getRotationBase().set(rotation);
			}
		}
		
		@Override public void onCommon(Player sender, Context ctx) {
			IAerobaticData data = getAerobaticDataOrDefault(sender);
			data.getRotationBase().set(rotation);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			rotation.write(buf);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			rotation = VectorBase.read(buf);
		}
	}
	
	/**
	 * {@link IAerobaticData} update packet<br>
	 * Sent by the player when its tilt changes
	 */
	public static class DTiltPacket extends ValidatedDistributedPlayerPacket {
		// Add delay for out-of-water checks
		private static final Map<String, Integer> STRIKES = new HashMap<>();
		
		float tiltPitch;
		float tiltRoll;
		float tiltYaw;
		
		public DTiltPacket() {}
		
		public DTiltPacket(IAerobaticData data) {
			tiltPitch = data.getTiltPitch();
			tiltRoll = data.getTiltRoll();
			tiltYaw = data.getTiltYaw();
		}
		
		@Override protected void onServer(Player sender, Context ctx) {
			IAerobaticData data = getAerobaticDataOrDefault(sender);
			
			boolean inWater = sender.isInWater(); //(sender.isInWater() ? STRIKES.put(name, 3) :
			// STRIKES.compute(name, (n, i) -> i != null ? max(0, i - 1) : 0)) > 0;
			data.setTiltPitch(validateClamp(
			  tiltPitch, -Config.aerobatic.tilt.range_pitch, Config.aerobatic.tilt.range_pitch));
			data.setTiltRoll(validateClamp(
			  tiltRoll, -Config.aerobatic.tilt.range_roll, Config.aerobatic.tilt.range_roll));
			final float tiltRangeYaw = inWater
			                           ? Const.UNDERWATER_YAW_RANGE_MULTIPLIER *
			                             Config.aerobatic.tilt.range_yaw
			                           : Config.aerobatic.tilt.range_yaw;
			data.setTiltYaw(validateClamp(tiltYaw, -tiltRangeYaw, tiltRangeYaw));
			final String name = sender.getScoreboardName();
			if (isInvalid()) {
				if (STRIKES.compute(name, (n, i) -> i != null? i + 1 : 1) < 4) {
					unInvalidate();
				} else handlePlayerWarning(sender, "Player '%s' tilted too much!");
			} else STRIKES.computeIfPresent(name, (n, i) -> max(0, i - 1));
		}
		
		@Override protected void onClient(Player sender, Context ctx) {
			IAerobaticData data = getAerobaticDataOrDefault(sender);
			data.setTiltPitch(tiltPitch);
			data.setTiltRoll(tiltRoll);
			data.setTiltYaw(tiltYaw);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeFloat(tiltPitch);
			buf.writeFloat(tiltRoll);
			buf.writeFloat(tiltYaw);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			tiltPitch = buf.readFloat();
			tiltRoll = buf.readFloat();
			tiltYaw = buf.readFloat();
		}
	}
	
	/**
	 * {@link IAerobaticData} update packet<br>
	 * Sent from the player when its acceleration changes
	 */
	public static class DAccelerationPacket extends ValidatedDistributedPlayerPacket {
		float propStrength;
		float brakeStrength;
		
		public DAccelerationPacket() {}
		
		public DAccelerationPacket(IAerobaticData data) {
			this.propStrength = data.getPropulsionStrength();
			this.brakeStrength = data.getBrakeStrength();
		}
		
		@Override public void onCommon(Player sender, Context ctx) {
			IAerobaticData data = getAerobaticDataOrDefault(sender);
			data.setPropulsionStrength(
			  validateClamp(
			    propStrength, propulsion.range_tick.getFloatMin(),
			    propulsion.range_tick.getFloatMax()));
			data.setBrakeStrength(validateClamp(brakeStrength, 0F, 1F));
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeFloat(propStrength);
			buf.writeFloat(brakeStrength);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			propStrength = buf.readFloat();
			brakeStrength = buf.readFloat();
		}
	}
	
	/**
	 * Input update packet<br>
	 * Sent when the player sneaking input changes
	 */
	public static class DSneakingPacket extends DistributedPlayerPacket {
		boolean sneaking;
		
		public DSneakingPacket() {}
		
		public DSneakingPacket(IAerobaticData data) {
			sneaking = data.isSneaking();
		}
		
		@Override public void onCommon(Player sender, Context ctx) {
			IAerobaticData target = getAerobaticDataOrDefault(sender);
			target.setSneaking(sneaking);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeBoolean(sneaking);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			sneaking = buf.readBoolean();
		}
	}
	
	/**
	 * Input update packet<br>
	 * Sent when the player jumping input changes
	 */
	public static class DJumpingPacket extends DistributedPlayerPacket {
		boolean jumping;
		
		public DJumpingPacket() {}
		
		public DJumpingPacket(IAerobaticData data) {
			jumping = data.isJumping();
		}
		
		@Override protected void onCommon(Player sender, Context ctx) {
			IAerobaticData target = getAerobaticDataOrDefault(sender);
			target.setJumping(jumping);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeBoolean(jumping);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			jumping = buf.readBoolean();
		}
	}
	
	public static class DSprintingPacket extends DistributedPlayerPacket {
		boolean sprinting;
		
		public DSprintingPacket() {}
		
		public DSprintingPacket(IAerobaticData data) {
			sprinting = data.isSprinting();
		}
		
		@Override protected void onCommon(Player sender, Context ctx) {
			IAerobaticData target = getAerobaticDataOrDefault(sender);
			if (sprinting)
				sender.setSprinting(false);
			target.setSprinting(sprinting);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeBoolean(sprinting);
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			sprinting = buf.readBoolean();
		}
	}
	
	public static class DLookAroundPacket extends DistributedPlayerPacket {
		float yaw;
		float pitch;
		
		public DLookAroundPacket() {}
		public DLookAroundPacket(IAerobaticData data) {
			yaw = data.getLookAroundYaw();
			pitch = data.getLookAroundPitch();
		}
		
		@Override protected void onCommon(Player sender, Context ctx) {
			IAerobaticData data = getAerobaticDataOrDefault(sender);
			data.setLookAroundYaw(yaw);
			data.setLookAroundPitch(pitch);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeFloat(yaw);
			buf.writeFloat(pitch);
		}
		@Override protected void deserialize(FriendlyByteBuf buf) {
			yaw = buf.readFloat();
			pitch = buf.readFloat();
		}
	}
	
	/**
	 * {@link IAerobaticData} initialization packet<br>
	 * Sent by the server when a client starts tracking another or self
	 */
	public static class SAerobaticDataPacket extends ServerPlayerPacket {
		IAerobaticData data;
		
		protected SAerobaticDataPacket() {}
		
		public SAerobaticDataPacket(Player player) {
			super(player);
			data = getAerobaticDataOrDefault(player);
		}
		
		@Override protected void onClient(Player player, Context ctx) {
			IAerobaticData targetData = getAerobaticDataOrDefault(player);
			targetData.copy(data);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeNbt(AerobaticDataCapability.asNBT(data));
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			data = AerobaticDataCapability.fromNBT(buf.readNbt());
		}
	}
	
	
	/**
	 * {@link IFlightData} initialization packet<br>
	 * Sent by the server when a client starts tracking another or self
	 */
	public static class SFlightDataPacket extends ServerPlayerPacket {
		IFlightData data;
		
		protected SFlightDataPacket() {}
		
		public SFlightDataPacket(Player player) {
			super(player);
			data = getFlightDataOrDefault(player);
		}
		
		@Override protected void onClient(Player player, Context ctx) {
			IFlightData targetData = getFlightDataOrDefault(player);
			targetData.copy(data);
		}
		
		@Override protected void serialize(FriendlyByteBuf buf) {
			buf.writeNbt(FlightDataCapability.asNBT(data));
		}
		
		@Override protected void deserialize(FriendlyByteBuf buf) {
			data = FlightDataCapability.fromNBT(buf.readNbt());
		}
	}
}
