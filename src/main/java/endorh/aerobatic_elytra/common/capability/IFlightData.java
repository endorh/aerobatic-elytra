package endorh.aerobatic_elytra.common.capability;

import endorh.aerobatic_elytra.client.sound.FadingTickableSound;
import endorh.aerobatic_elytra.common.flight.mode.IFlightMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IFlightData extends ILocalPlayerCapability<IFlightData> {
	
	PlayerEntity getPlayer();
	@Nonnull IFlightMode getFlightMode();
	void setFlightMode(@Nonnull IFlightMode mode);
	default boolean isFlightMode(IFlightMode mode) {
		return this.getFlightMode() == mode;
	}
	default void nextFlightMode() {
		setFlightMode(getFlightMode().next(m -> m.shouldCycle() && m.canBeUsedBy(getPlayer())));
	}
	default void prevFlightMode() {
		setFlightMode(getFlightMode().next(m -> m.shouldCycle() && m.canBeUsedBy(getPlayer()), -1));
	}
	
	@Nullable FadingTickableSound getFlightSound(ResourceLocation type);
	void putFlightSound(ResourceLocation type, @Nullable FadingTickableSound sound);
	
	@Override default void copy(IFlightData data) {
		setFlightMode(data.getFlightMode());
	}
	
	@Override default void reset() {
	
	}
}
