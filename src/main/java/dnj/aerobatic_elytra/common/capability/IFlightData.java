package dnj.aerobatic_elytra.common.capability;

import dnj.aerobatic_elytra.client.sound.FadingTickableSound;
import dnj.aerobatic_elytra.common.flight.mode.IFlightMode;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IFlightData extends ILocalPlayerCapability<IFlightData> {
	
	@Nonnull IFlightMode getFlightMode();
	void setFlightMode(@Nonnull IFlightMode mode);
	default boolean isFlightMode(IFlightMode mode) {
		return this.getFlightMode() == mode;
	}
	default void nextFlightMode() { setFlightMode(getFlightMode().next()); }
	default void prevFlightMode() { setFlightMode(getFlightMode().prev()); }
	
	@Nullable FadingTickableSound getFlightSound(ResourceLocation type);
	void putFlightSound(ResourceLocation type, @Nullable FadingTickableSound sound);
	
	@Override default void copy(IFlightData data) {
		setFlightMode(data.getFlightMode());
	}
	
	@Override default void reset() {
	
	}
}
