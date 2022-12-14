package endorh.aerobaticelytra.common.flight.mode;

import endorh.aerobaticelytra.client.render.model.IElytraPose;
import endorh.aerobaticelytra.common.registry.AerobaticElytraRegistries;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static endorh.aerobaticelytra.common.registry.AerobaticElytraRegistries.FLIGHT_MODE_REGISTRY;

/**
 * Abstract flight mode.<br>
 * A player can be in only one flight mode at a time.<br>
 * The player may attempt to change flight mode at any time.<br>
 * Mods may add other key combinations to change flight modes.<br>
 * A flight mode can prevent being left by overriding {@link IFlightMode#canChangeTo(IFlightMode)}
 * or being used by overriding {@link IFlightMode#canBeUsedBy(Player)}<br><br>
 * A flight mode is responsible for handling the player movement by overriding
 * {@link IFlightMode#getFlightHandler()} and its variants.<br>
 * Flight modes may also provide their own {@link IElytraPose}s for players.<br>
 */
public interface IFlightMode {
	/**
	 * Whether should the mode be used as one of the candidates
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
	default boolean canBeUsedBy(Player player) {
		return true;
	}
	
	default int getRegistryOrder() {
		return 0;
	}
	
	/**
	 * @return The method used to handle a flight tick for a player
	 */
	BiPredicate<Player, Vec3> getFlightHandler();
	
	/**
	 * @return The method used to handle a non-flight tick for a player
	 */
	@Nullable default BiConsumer<Player, Vec3> getNonFlightHandler() {
		return null;
	}
	
	/**
	 * @return The method used to handle a flight tick for a
	 * {@link RemotePlayer}
	 */
	@Nullable default Consumer<Player> getRemoteFlightHandler() {
		return null;
	}
	
	/**
	 * @return The method used to handle a non-flight tick for a
	 * {@link RemotePlayer}
	 */
	@Nullable default Consumer<Player> getRemoteNonFlightHandler() {
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
		int l = AerobaticElytraRegistries.FLIGHT_MODE_LIST.size();
		int o = AerobaticElytraRegistries.FLIGHT_MODE_LIST.indexOf(this);
		for (int i = 0; i < l; i++) {
			IFlightMode mode =
			  AerobaticElytraRegistries.FLIGHT_MODE_LIST.get((o + (i + 1) * step + l) % l);
			if (predicate.test(mode))
				return mode;
		}
		return this;
	}
	
	/**
	 * Serialize to packet
	 */
	default void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(Objects.requireNonNull(
		  FLIGHT_MODE_REGISTRY.getKey(this), "Unregistered flight mode " + this));
	}
	
	/**
	 * Read froom packet
	 */
	static IFlightMode read(FriendlyByteBuf buf) {
		final ResourceLocation regName = buf.readResourceLocation();
		if (!FLIGHT_MODE_REGISTRY.containsKey(regName))
			throw new IllegalArgumentException(
			  "Invalid FlightMode registry name in packet: '" + regName + "'");
		return FLIGHT_MODE_REGISTRY.getValue(regName);
	}
	
	/**
	 * Get the {@link IElytraPose} for a player in the current state.
	 */
	default @Nullable IElytraPose getElytraPose(Player player) {
		return null;
	}
}
