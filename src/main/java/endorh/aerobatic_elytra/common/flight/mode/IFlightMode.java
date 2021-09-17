package endorh.aerobatic_elytra.common.flight.mode;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.client.render.model.IElytraPose;
import endorh.aerobatic_elytra.common.registry.ModRegistries;
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
	
	@Override
	default Class<IFlightMode> getRegistryType() {
		return IFlightMode.class;
	}
	
	@Override
	IFlightMode setRegistryName(ResourceLocation name);
	
	@NotNull
	@Override
	ResourceLocation getRegistryName();
	
	default int getRegistryOrder() {
		return 0;
	}
	
	BiPredicate<PlayerEntity, Vector3d> getFlightHandler();
	
	@Nullable
	default BiConsumer<PlayerEntity, Vector3d> getNonFlightHandler() {
		return null;
	}
	
	@Nullable
	default Consumer<PlayerEntity> getRemoteFlightHandler() {
		return null;
	}
	
	@Nullable
	default Consumer<PlayerEntity> getRemoteNonFlightHandler() {
		return null;
	}
	
	ResourceLocation getPopupIconLocation();
	int getPopupIconU();
	int getPopupIconV();
	
	default boolean canChangeTo(IFlightMode other) {
		return true;
	}
	
	default IFlightMode next() {
		return next(IFlightMode::shouldCycle, 1);
	}
	
	default IFlightMode prev() {
		return next(IFlightMode::shouldCycle, -1);
	}
	
	default IFlightMode next(Predicate<IFlightMode> predicate) {
		return next(predicate, 1);
	}
	
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
	
	default void write(PacketBuffer buf) {
		buf.writeResourceLocation(getRegistryName());
	}
	
	static IFlightMode read(PacketBuffer buf) {
		final ResourceLocation regName = buf.readResourceLocation();
		if (!ModRegistries.FLIGHT_MODE_REGISTRY.containsKey(regName))
			throw new IllegalArgumentException(
			  "Invalid FlightMode registry name in packet: '" + regName + "'");
		return ModRegistries.FLIGHT_MODE_REGISTRY.getValue(regName);
	}
	
	default IElytraPose getElytraPose(PlayerEntity player) {
		return null;
	}
	
	/**
	 * Boilerplate for enum based IFlightMode s
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
