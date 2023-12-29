package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.client.sound.FadingTickableSound;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.lazulib.capability.ISerializableCapability;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IFlightData extends ILocalPlayerCapability<IFlightData>, ISerializableCapability {
	/**
	 * Get the associated player.
	 */
	Player getPlayer();
	
	/**
	 * Get the current flight mode.
	 */
	@Nonnull IFlightMode getFlightMode();
	
	/**
	 * Set the current flight mode.
	 */
	void setFlightMode(@Nonnull IFlightMode mode);
	
	/**
	 * Check if the current flight mode matches a given one.
	 */
	default boolean isFlightMode(IFlightMode mode) {
		return this.getFlightMode() == mode;
	}
	
	/**
	 * Cycle to the next flight mode.
	 */
	default void nextFlightMode() {
		setFlightMode(getFlightMode().next(m -> m.shouldCycle() && m.canBeUsedBy(getPlayer())));
	}
	
	/**
	 * Cycle to the previous flight mode.
	 */
	default void prevFlightMode() {
		setFlightMode(getFlightMode().next(m -> m.shouldCycle() && m.canBeUsedBy(getPlayer()), -1));
	}
	
	/**
	 * Get flight sound of thte given type.
	 */
	@Nullable FadingTickableSound getFlightSound(ResourceLocation type);
	
	/**
	 * Set the flight sound for a given type.
	 */
	void putFlightSound(ResourceLocation type, @Nullable FadingTickableSound sound);
	
	@Override default void copy(IFlightData data) {
		setFlightMode(data.getFlightMode());
	}
	
	@Override default void reset() {
	
	}
}
