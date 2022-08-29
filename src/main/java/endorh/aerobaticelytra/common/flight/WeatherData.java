package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.config.Config.weather;
import endorh.aerobaticelytra.network.WeatherPackets.SWindNodePacket;
import endorh.util.math.Vec3f;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class WeatherData {
	protected static final Queue<ChunkUpdateTask> CHUNK_UPDATE_TASKS = new ConcurrentLinkedQueue<>();
	
	// Because synchronizedMap keeps crashing without reason or locks the world
	protected static class ChunkUpdateTask {
		protected final Level world;
		protected final int x;
		protected final int z;
		protected final boolean unload;
		
		private ChunkUpdateTask(ChunkAccess chunk, boolean unload) {
			world = (Level) chunk.getWorldForge();
			x = chunk.getPos().getMinBlockX();
			z = chunk.getPos().getMinBlockZ();
			this.unload = unload;
		}
		
		protected static ChunkUpdateTask load(ChunkAccess chunk) {
			return new ChunkUpdateTask(chunk, false);
		}
		protected static ChunkUpdateTask unload(ChunkAccess chunk) {
			return new ChunkUpdateTask(chunk, true);
		}
	}
	
	public final static Map<Level, Map<Pair<Long, Long>, WeatherRegion>> weatherRegions = synchronizedMap(new HashMap<>());
	public final static Map<WeatherRegion, WindRegion> windRegions = synchronizedMap(new HashMap<>());
	
	/**
	 * {@link PacketDistributor} targeting all players which are tracking
	 * the argument weather region.
	 */
	@SuppressWarnings({"RedundantCast", "unchecked"})
	public static final PacketDistributor<WeatherRegion> TRACKING_WEATHER_REGION = new PacketDistributor<>(
	  (dist, regionSupplier) -> p ->
		  ((Set<? extends ServerPlayer>)regionSupplier.get().affectedPlayers()).forEach(
		    pl -> pl.connection.connection.send(p)),
	  NetworkDirection.PLAY_TO_CLIENT
	);
	
	/**
	 * Describes a weather region, consisting of a square with
	 * side of 16 chunks, or 256 blocks.<br>
	 * Players within 2 chunks of the border are considered as affected
	 * as well, to smooth transition between regions.<br>
	 * A region is kept in memory as long as it contains loaded chunks,
	 * or has been loaded by a client in the last 80 ticks.
	 * @see WindRegion
	 */
	@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
	public static class WeatherRegion {
		public static final int DIAMETER_SHIFT = 8;
		public static final long RADIUS = 1 << (DIAMETER_SHIFT - 1);
		public static final long DIAMETER = 1 << DIAMETER_SHIFT;
		public static final long BORDER = 16 * 2;
		public static final int TEMP_TICKET_TICKS = 80;
		public final long x, z;
		public final double centerX, centerZ;
		public final Level world;
		
		private int tempTicket = 0;
		private final long[] chunks = new long[] {0L, 0L, 0L, 0L};
		
		private WeatherRegion(Level world, long x, long z) {
			this.world = world;
			this.x = x;
			this.z = z;
			centerX = (x << DIAMETER_SHIFT) + RADIUS;
			centerZ = (z << DIAMETER_SHIFT) + RADIUS;
		}
		
		public static long scale(double c) {
			return (long) c >> DIAMETER_SHIFT;
		}
		
		public static int mod(long c) {
			return (int) (c - (scale(c) << DIAMETER_SHIFT));
		}
		
		public static Set<WeatherRegion> of(Player player) {
			return of(player.level, player.getX(), player.getZ());
		}
		
		public static Set<WeatherRegion> of(Level world, double x, double z) {
			long dX = scale(x), dZ = scale(z);
			double rX = x - (dX << DIAMETER_SHIFT), rZ = z - (dZ << DIAMETER_SHIFT);
			Set<WeatherRegion> adj = new HashSet<>();
			adj.add(WeatherRegion.of(world, dX, dZ));
			if (rX < BORDER) {
				adj.add(WeatherRegion.of(world, dX - 1, dZ));
				if (rZ < BORDER) {
					adj.add(WeatherRegion.of(world, dX, dZ - 1));
					adj.add(WeatherRegion.of(world, dX - 1, dZ - 1));
				} else if (DIAMETER - rZ < BORDER) {
					adj.add(WeatherRegion.of(world, dX, dZ + 1));
					adj.add(WeatherRegion.of(world, dX - 1, dZ + 1));
				}
			} else if (DIAMETER - rX < BORDER) {
				adj.add(WeatherRegion.of(world, dX + 1, dZ));
				if (rZ < BORDER) {
					adj.add(WeatherRegion.of(world, dX, dZ - 1));
					adj.add(WeatherRegion.of(world, dX + 1, dZ - 1));
				} else if (DIAMETER - rZ < BORDER) {
					adj.add(WeatherRegion.of(world, dX, dZ + 1));
					adj.add(WeatherRegion.of(world, dX + 1, dZ + 1));
				}
			} else if (rZ < BORDER) {
				adj.add(WeatherRegion.of(world, dX, dZ - 1));
			} else if (DIAMETER - rZ < BORDER) {
				adj.add(WeatherRegion.of(world, dX, dZ + 1));
			}
			return adj;
		}
		
		public static WeatherRegion of(Level world, long x, long z) {
			Pair<Long, Long> xz = Pair.of(x, z);
			if (weatherRegions.containsKey(world)) {
				if (weatherRegions.get(world).containsKey(xz))
					return weatherRegions.get(world).get(xz);
				else {
					WeatherRegion reg = new WeatherRegion(world, x, z);
					reg.tempTicket = TEMP_TICKET_TICKS;
					weatherRegions.get(world).put(xz, reg);
					return reg;
				}
			} else {
				weatherRegions.put(world, synchronizedMap(new HashMap<>()));
				WeatherRegion reg = new WeatherRegion(world, x, z);
				reg.tempTicket = TEMP_TICKET_TICKS;
				weatherRegions.get(world).put(xz, reg);
				return reg;
			}
		}
		
		public static WeatherRegion of(ChunkUpdateTask task) {
			final long x = scale(task.x), z = scale(task.z);
			final Pair<Long, Long> xz = Pair.of(x, z);
			if (weatherRegions.containsKey(task.world)) {
				final Map<Pair<Long, Long>, WeatherRegion> worldRegions = weatherRegions.get(task.world);
				if (worldRegions.containsKey(xz))
					return worldRegions.get(xz);
				else {
					WeatherRegion reg = new WeatherRegion(task.world, x, z);
					worldRegions.put(xz, reg);
					return reg;
				}
			} else {
				weatherRegions.put(task.world, synchronizedMap(new HashMap<>()));
				WeatherRegion reg = new WeatherRegion(task.world, x, z);
				weatherRegions.get(task.world).put(xz, reg);
				return reg;
			}
		}
		
		public static void handleChunkTasks() {
			ChunkUpdateTask task = CHUNK_UPDATE_TASKS.poll();
			while (task != null) {
				onChunkTask(task);
				task = CHUNK_UPDATE_TASKS.poll();
			}
		}
		
		public static void onChunkTask(ChunkUpdateTask task) {
			WeatherRegion.of(task).chunkUpdate(task);
		}
		
		@SubscribeEvent
		public static void onChunkLoad(ChunkEvent.Load event) {
			if (!(event.getLevel() instanceof Level))
				return;
			CHUNK_UPDATE_TASKS.add(ChunkUpdateTask.load(event.getChunk()));
		}
		
		@SubscribeEvent
		public static void onChunkUnload(ChunkEvent.Unload event) {
			if (!(event.getLevel() instanceof Level))
				return;
			CHUNK_UPDATE_TASKS.add(ChunkUpdateTask.unload(event.getChunk()));
		}
		
		@SubscribeEvent
		public static void onWorldUnload(LevelEvent.Unload event) {
			if (!(event.getLevel() instanceof Level world))
				return;
			if (weatherRegions.containsKey(world)) {
				final Map<Pair<Long, Long>, WeatherRegion> worldRegions = weatherRegions.get(world);
				weatherRegions.remove(world);
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (worldRegions) {
					for (WeatherRegion region : worldRegions.values()) {
						windRegions.remove(region);
					}
				}
			}
		}
		
		private void chunkUpdate(ChunkUpdateTask task) {
			tempTicket = 0;
			final int rX = (int)(task.x - (scale(task.x) << DIAMETER_SHIFT)),
			  rZ = (int)(task.z - (scale(task.z) << DIAMETER_SHIFT));
			final int i = (rX >> 4 << 4) + (rZ >> 4);
			final int p = i >> 6, r = i & 63;
			if (task.unload) {
				chunks[p] &= ~(1L << r);
				if ((chunks[0] | chunks[1] | chunks[2] | chunks[3]) == 0L)
					unload();
			} else {
				chunks[p] |= 1L << r;
			}
		}
		
		private void unload() {
			weatherRegions.get(world).remove(Pair.of(x, z));
			windRegions.remove(this);
			if (weatherRegions.get(world).isEmpty())
				weatherRegions.remove(world);
		}
		
		public boolean contains(Player player) {
			if (player.level != world)
				return false;
			return containsNoWorldCheck(player);
		}
		
		public boolean containsNoWorldCheck(Player player) {
			return contains(player.getX(), player.getZ());
		}
		
		@SuppressWarnings("unused")
		public boolean affects(Player player) {
			if (world != player.level)
				return false;
			return affectsNoWorldCheck(player);
		}
		
		public boolean affectsNoWorldCheck(Player player) {
			return EntitySelector.NO_SPECTATORS.test(player)
			       && abs(player.getX() - centerX) < RADIUS + BORDER
			       && abs(player.getZ() - centerZ) < RADIUS + BORDER;
		}
		
		public boolean contains(double x, double z) {
			return contains(scale(x), scale(z));
		}
		
		public boolean contains(long x, long z) {
			return x == this.x && z == this.z;
		}
		
		public Set<? extends Player> affectedPlayers() {
			return world.players().stream().filter(
			  this::affectsNoWorldCheck).collect(Collectors.toSet());
		}
		
		public void tick() {
			WindRegion.of(this).tick();
			if (tempTicket != 0) {
				tempTicket--;
				if (tempTicket == 0 && ((chunks[0] | chunks[1] | chunks[2] | chunks[3]) == 0L))
					unload();
			}
		}
		
		@Override
		public String toString() {
			int chunkCount = Long.bitCount(chunks[0]) + Long.bitCount(chunks[1])
			  + Long.bitCount(chunks[2]) + Long.bitCount(chunks[3]);
			return format(
			  "<Region: ⟨%+4d, %+4d⟩→⟨%+6d, %+6d⟩, %s>",
			  x, z, (long)centerX, (long)centerZ,
			  (chunkCount > 0? format("chunks: %3d, ", chunkCount) : "")
			  + (tempTicket > 0? format("temp: %3d, ", tempTicket) : "")
			  + format("affects: %2d", affectedPlayers().size())
			  );
		}
	}
	
	/**
	 * Wind data attached to a {@link WeatherRegion}.<br>
	 * Contains two wind vectors, which are updated every tick on the
	 * server<br>
	 * When the wind is non-zero, wind updates are sent to clients
	 * affected by the region.
	 */
	public static class WindRegion {
		protected static final Random RANDOM = new Random();
		
		public final WeatherRegion region;
		public final Vec3f wind = Vec3f.ZERO.get();
		public final Vec3f angularWind = Vec3f.ZERO.get();
		
		private WindRegion(WeatherRegion region) {
			this.region = region;
		}
		
		public static Set<WindRegion> of(Player player) {
			return of(player.level, player.getX(), player.getZ());
		}
		
		public static Set<WindRegion> of(Level world, double x, double z) {
			return WeatherRegion.of(world, x, z).stream().map(WindRegion::of).collect(Collectors.toSet());
		}
		
		public static WindRegion of(Level world, long x, long z) {
			return of(WeatherRegion.of(world, x, z));
		}
		
		public static WindRegion of(WeatherRegion region) {
			if (windRegions.containsKey(region)) {
				return windRegions.get(region);
			} else {
				WindRegion node = new WindRegion(region);
				windRegions.put(region, node);
				return node;
			}
		}
		
		public Vec3f getWind() {
			return wind;
		}
		
		public Vec3f getAngularWind() {
			return angularWind;
		}
		
		private static final Vec3f angularWindDelta = Vec3f.ZERO.get();
		private static final Vec3f orthogonal = Vec3f.ZERO.get();
		private static final Vec3f last = Vec3f.ZERO.get();
		
		protected void tick() {
			if (!weather.enabled)
				return;
			float rain = region.world.getRainLevel(1F);
			float storm = region.world.getThunderLevel(1F);
			if (rain > 0F || !wind.isZero(1E-5)) {
				float wind_randomness =
				  weather.rain.wind_randomness_tick * rain * weather.rain.wind_strength_tick
				  + weather.storm.wind_randomness_tick * storm * weather.storm.wind_strength_tick;
				if (wind_randomness > 0F) {
					float strength = rain * weather.rain.wind_strength_tick + storm * weather.storm.wind_strength_tick;
					float delta = rain * weather.rain.wind_randomness_tick + storm * weather.storm.wind_randomness_tick;
					
					last.set(wind);
					if (wind.isZero(1E-5))
						wind.set(1F, 0F, 0F);
					wind.unitary();
					wind.mul(strength * (RANDOM.nextFloat() * 0.4F + 0.8F));
					orthogonal.setOrthogonalUnitary(wind);
					wind.rotateAlongVecDegrees(orthogonal, (RANDOM.nextFloat() - 0.5F) * 2F * delta);
					wind.lerp(last, 0.6F);
					
					angularWindDelta.setRandom(
					  weather.rain.wind_randomness_tick * rain *
					  weather.rain.wind_angular_strength_tick
					  + weather.storm.wind_randomness_tick * storm *
					    weather.storm.wind_angular_strength_tick
					);
					angularWind.add(angularWindDelta);
					angularWind.clamp(
					  rain * weather.rain.wind_angular_strength_tick
					  + storm * weather.storm.wind_angular_strength_tick);
				}
				new SWindNodePacket(this).sendTracking();
			}
		}
		
		public void update(SWindNodePacket packet) {
			this.wind.set(packet.wind);
			this.angularWind.set(packet.angularWind);
		}
		
		@Override
		public String toString() {
			return format("<WeatherNode at %s: %s, %s>",
			              region, wind, angularWind);
		}
	}
	
	@SubscribeEvent
	public static void tick(LevelTickEvent event) {
		WeatherRegion.handleChunkTasks();
		if (event.side.isServer()) {
			Level world = event.level;
			if (weatherRegions.containsKey(world)) {
				final Map<Pair<Long, Long>, WeatherRegion> worldRegions = weatherRegions.get(world);
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (worldRegions) {
					for (WeatherRegion region : worldRegions.values()) {
						region.tick();
					}
				}
			}
		}
	}
	
	public static float getBiomePrecipitationStrength(Player player) {
		return switch (player.level.getBiome(player.blockPosition()).value().getPrecipitation()) {
			case NONE -> 0F;
			case SNOW -> 1.2F;
			case RAIN -> 1F;
		};
	}
	
	public static Vec3f getWindVector(Player player) {
		return Vec3f.average(
		  WindRegion.of(player).stream().map(WindRegion::getWind).collect(Collectors.toSet()));
	}
	
	public static Vec3f getAngularWindVector(Player player) {
		return Vec3f.average(
		  WindRegion.of(player).stream().map(WindRegion::getAngularWind).collect(Collectors.toSet()));
	}
}
