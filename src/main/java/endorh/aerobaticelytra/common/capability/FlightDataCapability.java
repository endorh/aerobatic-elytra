package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.sound.FadingTickableSound;
import endorh.aerobaticelytra.common.flight.mode.FlightModes;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.common.registry.ModRegistries;
import endorh.util.capability.SerializableCapabilityWrapperProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid=AerobaticElytra.MOD_ID)
public class FlightDataCapability {
	/** The {@link Capability} instance */
	public static Capability<IFlightData> CAPABILITY =
	  CapabilityManager.get(new CapabilityToken<>() {});
	public static final ResourceLocation ID = AerobaticElytra.prefix("flight_data");
	
	/**
	 * Deserialize an {@link IFlightData} from NBT
	 */
	public static IFlightData fromNBT(CompoundTag nbt) {
		IFlightData data = new FlightData(null);
		data.deserializeCapability(nbt);
		return data;
	}
	
	/**
	 * Serialize an {@link IFlightData} to NBT
	 */
	public static CompoundTag asNBT(IFlightData data) {
		return data.serializeCapability();
	}
	
	/**
	 * @return The {@link IFlightData} from the player
	 * @throws IllegalStateException if the capability was not present
	 * @see FlightDataCapability#getFlightDataOrDefault
	 * @see FlightDataCapability#getFlightData
	 */
	public static IFlightData requireFlightData(Player player) {
		return player.getCapability(CAPABILITY).orElseThrow(
		  () -> new IllegalStateException("Missing IFlightData capability on player: " + player));
	}
	
	/**
	 * Return the {@link IFlightData} from the player or a
	 * default one if for some reason the player's one is
	 * invalid right now
	 *
	 * @see FlightDataCapability#requireFlightData
	 * @see FlightDataCapability#getFlightData
	 */
	public static IFlightData getFlightDataOrDefault(Player player) {
		return player.getCapability(CAPABILITY).orElse(new FlightData(player));
	}
	
	/**
	 * @return The optional {@link IFlightData} from the player
	 * @see FlightDataCapability#requireFlightData
	 * @see FlightDataCapability#getFlightDataOrDefault
	 */
	public static Optional<IFlightData> getFlightData(Player player) {
		assert CAPABILITY != null;
		return player.getCapability(CAPABILITY).resolve();
	}
	
	/** Create a serializable provider for a player */
	public static ICapabilitySerializable<Tag> createProvider(Player player) {
		if (CAPABILITY == null) return null;
		return new SerializableCapabilityWrapperProvider<>(CAPABILITY, null, new FlightData(player));
	}
	
	/**
	 * Attach the capability to players
	 */
	@SubscribeEvent
	public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof Player player)
			event.addCapability(ID, createProvider(player));
	}
	
	/**
	 * Copy capability to cloned player
	 */
	@SubscribeEvent
	public static void onClonePlayer(PlayerEvent.Clone event) {
		IFlightData playerData = requireFlightData(event.getPlayer());
		playerData.copy(getFlightDataOrDefault(event.getOriginal()));
		playerData.reset();
	}
	
	/**
	 * Default implementation for {@link IFlightData}
	 */
	public static class FlightData implements IFlightData {
		public static final String TAG_FLIGHT_MODE = "FlightMode";
		
		protected final Player player;
		protected @Nonnull IFlightMode mode = FlightModes.ELYTRA_FLIGHT;
		
		protected final Map<ResourceLocation, FadingTickableSound> flightSounds = new HashMap<>();
		
		public FlightData(Player player) {
			this.player = player;
		}
		
		@Override public Player getPlayer() {
			return player;
		}
		
		@Override public @Nonnull IFlightMode getFlightMode() {
			return mode;
		}
		
		@Override public void setFlightMode(@Nonnull IFlightMode mode) {
			this.mode = mode;
		}
		
		@Override public @Nullable FadingTickableSound getFlightSound(ResourceLocation type) {
			return flightSounds.get(type);
		}
		
		@Override public void putFlightSound(ResourceLocation type, FadingTickableSound sound) {
			flightSounds.put(type, sound);
		}
		
		@Override public CompoundTag serializeCapability() {
			CompoundTag nbt = new CompoundTag();
			nbt.putString(TAG_FLIGHT_MODE, getFlightMode().getRegistryName().toString());
			return nbt;
		}
		
		@Override public void deserializeCapability(CompoundTag nbt) {
			
			ResourceLocation regName = new ResourceLocation(nbt.getString(TAG_FLIGHT_MODE));
			// Registry entries may vary between world loads
			
			if (!ModRegistries.FLIGHT_MODE_REGISTRY.containsKey(regName)) {
				setFlightMode(FlightModes.ELYTRA_FLIGHT);
			} else {
				IFlightMode mode = ModRegistries.FLIGHT_MODE_REGISTRY.getValue(regName);
				if (mode == null)
					mode = FlightModes.ELYTRA_FLIGHT;
				setFlightMode(mode);
			}
		}
	}
}
