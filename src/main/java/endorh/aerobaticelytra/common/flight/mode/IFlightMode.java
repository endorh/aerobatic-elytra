package endorh.aerobaticelytra.common.flight.mode;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.render.model.IElytraPose;
import endorh.aerobaticelytra.common.registry.ModRegistries;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Abstract flight mode.<br>
 * A player can be in only one flight mode at a time.<br>
 * The player may attempt to change flight mode at any time.<br>
 * Mods may add other key combinations to change flight modes.<br>
 * A flight mode can prevent being left by overriding {@link IFlightMode#canChangeTo(IFlightMode)}
 * or being used by overriding {@link IFlightMode#canBeUsedBy(PlayerEntity)}<br><br>
 * A flight mode is responsible for handling the player movement by overriding
 * {@link IFlightMode#getFlightHandler()} and its variants.<br>
 * Flight modes may also provide their own {@link IElytraPose}s for players.<br>
 */
public interface IFlightMode extends IForgeRegistryEntry<IFlightMode> {
	/**
	 * Whether or not should the mode be used as one of the candidates
	 * for the "toggle flight mode" key<br>
	 * Mods may choose to make their flight modes accessible through
	 * other means, such as a separate key
	 */
	boolean shouldCycle();
	
	/**
	 * Check if the flight mode has a flight mode tag.<br>
	 * Tags are merely ResourceLocations
	 * @return true if the mode has the tag
	 * @see FlightModeTags#ELYTRA
	 * @see FlightModeTags#AEROBATIC
	 */
	boolean is(ResourceLocation tag);
	
	/**
	 * Restrict which players can use a certain flight mode
	 */
	default boolean canBeUsedBy(PlayerEntity player) {
		return true;
	}
	
	@Override default Class<IFlightMode> getRegistryType() {
		return IFlightMode.class;
	}
	
	@Override IFlightMode setRegistryName(ResourceLocation name);
	
	@NotNull @Override ResourceLocation getRegistryName();
	
	default int getRegistryOrder() {
		return 0;
	}
	
	/**
	 * @return The method used to handle a flight tick for a player
	 */
	BiPredicate<PlayerEntity, Vector3d> getFlightHandler();
	
	/**
	 * @return The method used to handle a non-flight tick for a player
	 */
	@Nullable default BiConsumer<PlayerEntity, Vector3d> getNonFlightHandler() {
		return null;
	}
	
	/**
	 * @return The method used to handle a flight tick for a
	 * {@link net.minecraft.client.entity.player.RemoteClientPlayerEntity}
	 */
	@Nullable default Consumer<PlayerEntity> getRemoteFlightHandler() {
		return null;
	}
	
	/**
	 * @return The method used to handle a non-flight tick for a
	 * {@link net.minecraft.client.entity.player.RemoteClientPlayerEntity}
	 */
	@Nullable default Consumer<PlayerEntity> getRemoteNonFlightHandler() {
		return null;
	}
	
	/**
	 * @return The texture location of the toast displaying the flight mode icon
	 */
	ResourceLocation getToastIconLocation();
	
	/**
	 * @return The texture U coordinate of the toast displaying the flight mode icon
	 */
	int getToastIconU();
	/**
	 * @return The texture V coordinate of the toast displaying the flight mode icon
	 */
	int getToastIconV();
	
	/**
	 * Determine if it's possible to change to another flight mode
	 * in the current state
	 */
	default boolean canChangeTo(IFlightMode other) {
		return true;
	}
	
	/**
	 * @return The next flight mode when cycling
	 */
	default IFlightMode next() {
		return next(IFlightMode::shouldCycle, 1);
	}
	
	/**
	 * @return The previous flight mode when cycling
	 */
	default IFlightMode prev() {
		return next(IFlightMode::shouldCycle, -1);
	}
	
	/**
	 * Get the next flight mode in cycle order that satisfies a predicate
	 */
	default IFlightMode next(Predicate<IFlightMode> predicate) {
		return next(predicate, 1);
	}
	
	/**
	 * Get a flight mode in cycle order by a certain step that satisfies a predicate.<br>
	 * Usually, the step is either +1 or -1
	 */
	default IFlightMode next(Predicate<IFlightMode> predicate, int step) {
		int l = ModRegistries.FLIGHT_MODE_LIST.size();
		int o = ModRegistries.FLIGHT_MODE_LIST.indexOf(this);
		for (int i = 0; i < l; i++) {
			IFlightMode mode =
			  ModRegistries.FLIGHT_MODE_LIST.get((o + (i + 1) * step + l) % l);
			if (predicate.test(mode))
				return mode;
		}
		return this;
	}
	
	/**
	 * Serialize to packet
	 */
	default void write(PacketBuffer buf) {
		buf.writeResourceLocation(getRegistryName());
	}
	
	/**
	 * Read froom packet
	 */
	static IFlightMode read(PacketBuffer buf) {
		final ResourceLocation regName = buf.readResourceLocation();
		if (!ModRegistries.FLIGHT_MODE_REGISTRY.containsKey(regName))
			throw new IllegalArgumentException(
			  "Invalid FlightMode registry name in packet: '" + regName + "'");
		return ModRegistries.FLIGHT_MODE_REGISTRY.getValue(regName);
	}
	
	/**
	 * Get the {@link IElytraPose} for a player in the current state.
	 */
	default @Nullable IElytraPose getElytraPose(PlayerEntity player) {
		return null;
	}
	
	/**
	 * Boilerplate for enum based IFlightMode s, taking use of the Enum#name() method
	 */
	interface IEnumFlightMode extends IFlightMode {
		String name();
		
		@NotNull @Override default ResourceLocation getRegistryName() {
			return new ResourceLocation(AerobaticElytra.MOD_ID, name().toLowerCase());
		}
		
		@Override default IFlightMode setRegistryName(ResourceLocation name) {
			throw new IllegalArgumentException("Cannot set registry name of enum flight mode!");
		}
	}
}
