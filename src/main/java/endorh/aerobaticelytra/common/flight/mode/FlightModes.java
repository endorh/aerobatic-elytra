package endorh.aerobaticelytra.common.flight.mode;

import endorh.aerobaticelytra.client.AerobaticElytraResources;
import endorh.aerobaticelytra.common.config.Config.aerobatic.modes;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import endorh.aerobaticelytra.common.flight.ElytraFlight;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode.IEnumFlightMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public enum FlightModes implements IEnumFlightMode {
	ELYTRA_FLIGHT(
	  () -> modes.enable_normal_elytra_mode, -4000,
	  Const.FLIGHT_MODE_TOAST_U_OFFSET, Const.FLIGHT_MODE_TOAST_V_OFFSET,
	  ElytraFlight::onElytraTravel, null, null, null,
	  FlightModeTags.ELYTRA),
	AEROBATIC_FLIGHT(
	  () -> true, -3000,
	  Const.FLIGHT_MODE_TOAST_U_OFFSET + Const.FLIGHT_MODE_TOAST_WIDTH,
	  Const.FLIGHT_MODE_TOAST_V_OFFSET,
	  AerobaticFlight::onAerobaticTravel, AerobaticFlight::onOtherModeTravel,
	  AerobaticFlight::onRemoteFlightTravel, null,
	  FlightModeTags.ELYTRA, FlightModeTags.AEROBATIC);
	
	private final Supplier<Boolean> shouldCycle;
	private final int order;
	private final int u;
	private final int v;
	
	private final Set<ResourceLocation> tags = new HashSet<>();
	
	private final BiPredicate<PlayerEntity, Vector3d> flightHandler;
	private final BiConsumer<PlayerEntity, Vector3d> nonFlightHandler;
	private final Consumer<PlayerEntity> remoteFlightHandler;
	private final Consumer<PlayerEntity> remoteNonFlightHandler;
	
	FlightModes(
	  Supplier<Boolean> shouldCycle, int order, int u, int v,
	  BiPredicate<PlayerEntity, Vector3d> flightHandler,
	  @Nullable BiConsumer<PlayerEntity, Vector3d> nonFlightHandler,
	  @Nullable Consumer<PlayerEntity> remoteFlightHandler,
	  @Nullable Consumer<PlayerEntity> remoteNonFlightHandler,
	  ResourceLocation... tags
	) {
		this.shouldCycle = shouldCycle;
		this.order = order;
		this.flightHandler = flightHandler;
		this.nonFlightHandler = nonFlightHandler;
		this.remoteFlightHandler = remoteFlightHandler;
		this.remoteNonFlightHandler = remoteNonFlightHandler;
		this.u = u;
		this.v = v;
		Collections.addAll(this.tags, tags);
	}
	
	@Override public boolean shouldCycle() {
		return shouldCycle.get();
	}
	
	@Override public boolean is(ResourceLocation tag) {
		return tags.contains(tag);
	}
	
	@Override public int getRegistryOrder() {
		return order;
	}
	
	@Override public BiPredicate<PlayerEntity, Vector3d> getFlightHandler() {
		return flightHandler;
	}
	@Nullable
	@Override public BiConsumer<PlayerEntity, Vector3d> getNonFlightHandler() {
		return nonFlightHandler;
	}
	@Override public @Nullable Consumer<PlayerEntity> getRemoteFlightHandler() {
		return remoteFlightHandler;
	}
	@Override public @Nullable Consumer<PlayerEntity> getRemoteNonFlightHandler() {
		return remoteNonFlightHandler;
	}
	
	
	@Override public ResourceLocation getToastIconLocation() {
		return AerobaticElytraResources.FLIGHT_GUI_ICONS_LOCATION;
	}
	
	@Override public int getToastIconU() { return u; }
	@Override public int getToastIconV() { return v; }
	
	@Override public boolean canChangeTo(IFlightMode other) {
		return modes.allow_midair_change;
	}
}
