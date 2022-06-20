package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.sound.FadingTickableSound;
import endorh.aerobaticelytra.common.flight.mode.FlightModes;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.common.registry.ModRegistries;
import endorh.util.capability.CapabilityProviderSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class FlightDataCapability {
	/** The {@link Capability} instance */
	@CapabilityInject(IFlightData.class)
	public static Capability<IFlightData> CAPABILITY = null;
	
	private static final Storage storage = new Storage();
	public static final ResourceLocation ID = AerobaticElytra.prefix("flight_data");
	
	/** Registers the capability */
	public static void register() {
		CapabilityManager.INSTANCE.register(
		  IFlightData.class, storage, () -> new FlightData(null));
	}
	
	/**
	 * Deserialize an {@link IFlightData} from NBT
	 */
	public static IFlightData fromNBT(CompoundNBT nbt) {
		IFlightData data = new FlightData(null);
		storage.readNBT(CAPABILITY, data, null, nbt);
		return data;
	}
	
	/**
	 * Serialize an {@link IFlightData} to NBT
	 */
	public static CompoundNBT asNBT(IFlightData data) {
		return (CompoundNBT) storage.writeNBT(CAPABILITY, data, null);
	}
	
	/**
	 * @return The {@link IFlightData} from the player
	 * @throws IllegalStateException if the capability was not present
	 * @see FlightDataCapability#getFlightDataOrDefault
	 * @see FlightDataCapability#getFlightData
	 */
	public static IFlightData demandFlightData(PlayerEntity player) {
		return player.getCapability(CAPABILITY).orElseThrow(
		  () -> new IllegalStateException("Missing IFlightData capability on player: " + player));
	}
	
	/**
	 * Return the {@link IFlightData} from the player or a
	 * default one if for some reason the player's one is
	 * invalid right now
	 * @see FlightDataCapability#demandFlightData
	 * @see FlightDataCapability#getFlightData
	 */
	public static IFlightData getFlightDataOrDefault(PlayerEntity player) {
		return player.getCapability(CAPABILITY).orElse(new FlightData(null));
	}
	
	/**
	 * @return The optional {@link IFlightData} from the player
	 * @see FlightDataCapability#demandFlightData
	 * @see FlightDataCapability#getFlightDataOrDefault
	 */
	public static Optional<IFlightData> getFlightData(PlayerEntity player) {
		assert CAPABILITY != null;
		return player.getCapability(CAPABILITY).resolve();
	}
	
	/** Create a serializable provider for a player */
	public static ICapabilitySerializable<INBT> createProvider(PlayerEntity player) {
		if (CAPABILITY == null)
			return null;
		return new CapabilityProviderSerializable<>(CAPABILITY, null, new FlightData(player));
	}
	
	/**
	 * Attach the capability to players
	 */
	@SubscribeEvent
	public static void onAttachCapability(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof PlayerEntity) {
			event.addCapability(ID, createProvider((PlayerEntity)event.getObject()));
		}
	}
	
	/**
	 * Copy capability to cloned player
	 */
	@SubscribeEvent
	public static void onClonePlayer(PlayerEvent.Clone event) {
		IFlightData playerData = demandFlightData(event.getPlayer());
		playerData.copy(demandFlightData(event.getOriginal()));
		playerData.reset();
	}
	
	/**
	 * Default implementation for {@link IFlightData}
	 */
	public static class FlightData implements IFlightData {
		@SuppressWarnings({"unused", "FieldCanBeLocal"})
		protected final WeakReference<PlayerEntity> player;
		protected @Nonnull IFlightMode mode = FlightModes.ELYTRA_FLIGHT;
		
		protected final Map<ResourceLocation, FadingTickableSound> flightSounds = new HashMap<>();
		
		@Override public PlayerEntity getPlayer() {
			return player.get();
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
		
		public FlightData(PlayerEntity player) {
			this.player = new WeakReference<>(player);
		}
	}
	
	/**
	 * Default Storage implementation
	 */
	public static class Storage implements IStorage<IFlightData> {
		public static final String TAG_FLIGHT_MODE = "FlightMode";
		
		@Nullable @Override
		public INBT writeNBT(Capability<IFlightData> cap, IFlightData inst, Direction side) {
			CompoundNBT nbt = new CompoundNBT();
			nbt.putString(TAG_FLIGHT_MODE, inst.getFlightMode().getRegistryName().toString());
			return nbt;
		}
		
		@Override
		public void readNBT(Capability<IFlightData> cap, IFlightData inst, Direction side, INBT nbt) {
			CompoundNBT data = (CompoundNBT) nbt;
			
			ResourceLocation regName = new ResourceLocation(data.getString(TAG_FLIGHT_MODE));
			// Registry entries may vary between world loads
			
			if (!ModRegistries.FLIGHT_MODE_REGISTRY.containsKey(regName))
				inst.setFlightMode(FlightModes.ELYTRA_FLIGHT);
			else {
				IFlightMode mode = ModRegistries.FLIGHT_MODE_REGISTRY.getValue(regName);
				if (mode == null)
					mode = FlightModes.ELYTRA_FLIGHT;
				inst.setFlightMode(mode);
			}
		}
	}
}
