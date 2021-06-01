package dnj.aerobatic_elytra.common.capability;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.flight.AerobaticFlight.VectorBase;
import dnj.endor8util.capability.CapabilityProviderSerializable;
import dnj.endor8util.math.Vec3d;
import net.minecraft.client.audio.ElytraSound;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static dnj.aerobatic_elytra.AerobaticElytra.prefix;

/**
 * Capability for {@link IAerobaticData}
 */
@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class AerobaticDataCapability {
	/**
	 * The {@link Capability} instance
	 */
	@SuppressWarnings("CanBeFinal")
	@CapabilityInject(IAerobaticData.class)
	public static Capability<IAerobaticData> CAPABILITY = null;
	private static final Storage storage = new Storage();
	public static final ResourceLocation ID =
	  prefix("aerobatic_data");
	
	/**
	 * Registers the capability
	 */
	public static void register() {
		CapabilityManager.INSTANCE.register(
		  IAerobaticData.class, storage, () -> new AerobaticData(null));
	}
	
	/**
	 * Deserialize an {@link IAerobaticData} from NBT
	 */
	public static IAerobaticData fromNBT(CompoundNBT nbt) {
		IAerobaticData data = new AerobaticData(null);
		storage.readNBT(CAPABILITY, data, null, nbt);
		return data;
	}
	
	/**
	 * Serialize an {@link IAerobaticData} to NBT
	 */
	public static CompoundNBT asNBT(IAerobaticData data) {
		return (CompoundNBT) storage.writeNBT(CAPABILITY, data, null);
	}
	
	/**
	 * @return The {@link IAerobaticData} from the player
	 * @throws IllegalStateException if the player doesn't have the capability
	 * @see AerobaticDataCapability#getAerobaticDataOrDefault
	 * @see AerobaticDataCapability#getAerobaticData
	 */
	public static IAerobaticData demandAerobaticData(PlayerEntity player) {
		assert CAPABILITY != null;
		return player.getCapability(CAPABILITY).orElseThrow(
		  () -> new IllegalStateException("Missing IAerobaticData capability on player: " + player));
	}
	
	/**
	 * @return The {@link IAerobaticData} from the player or a default
	 * if for some reason the player doesn't have the capability or it's
	 * invalid now
	 * @see AerobaticDataCapability#getAerobaticData
	 * @see AerobaticDataCapability#demandAerobaticData
	 */
	public static IAerobaticData getAerobaticDataOrDefault(PlayerEntity player) {
		assert CAPABILITY != null;
		return player.getCapability(CAPABILITY)
		  .orElse(new AerobaticData(player));
	}
	
	/**
	 * @return The optional {@link IAerobaticData} capability from the player
	 * @see AerobaticDataCapability#getAerobaticDataOrDefault
	 * @see AerobaticDataCapability#demandAerobaticData
	 */
	public static Optional<IAerobaticData> getAerobaticData(PlayerEntity player) {
		assert CAPABILITY != null;
		return player.getCapability(CAPABILITY).resolve();
	}
	
	/**
	 * Create a serializable provider for a player
	 */
	public static ICapabilitySerializable<INBT> createProvider(PlayerEntity player) {
		if (CAPABILITY == null)
			return null;
		return new CapabilityProviderSerializable<>(CAPABILITY, null, new AerobaticData(player));
	}
	
	/**
	 * Attach the capability to a player
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
		IAerobaticData playerData = demandAerobaticData(event.getPlayer());
		playerData.copy(demandAerobaticData(event.getOriginal()));
		playerData.reset();
	}
	
	/**
	 * Default implementation for {@link IAerobaticData}
	 */
	public static class AerobaticData implements IAerobaticData {
		private final PlayerEntity player;
		private float rotationRoll = 0F;
		
		private float prevTickRotationPitch = 0F;
		private float prevTickRotationRoll = 0F;
		private float prevTickRotationYaw = 0F;
		
		private float tiltPitch = 0F;
		private float tiltRoll = 0F;
		private float tiltYaw = 0F;
		
		private final VectorBase rotationBase = new VectorBase();
		private final VectorBase cameraBase = new VectorBase();
		private final VectorBase preBounceBase = new VectorBase();
		private final VectorBase posBounceBase = new VectorBase();
		
		private boolean braking = false;
		private float brakeStrength = 0F;
		
		private float propStrength = 0F;
		private float propAcc = 0F;
		private boolean boosted = false;
		private float boostHeat = 0F;
		
		private boolean isFlying = false;
		private double lastRotationTime = 0D;
		private long lastBounceTime = 0L;
		
		private boolean sneaking = false;
		private boolean jumping = false;
		private boolean sprinting = false;
		private boolean playingSound = false;
		
		private ElytraSound elytraSound = null;
		private int lastLiftOff = 0;
		private final Vec3d lastTrailPos = Vec3d.ZERO.get();
		
		public AerobaticData(PlayerEntity player) {
			this.player = player;
		}
		
		@Override public PlayerEntity getPlayer() { return player; }
		
		@Override public float getRotationPitch() {
			if (player == null)
				return 0F;
			return player.rotationPitch;
		}
		@Override public float getRotationYaw() {
			if (player == null)
				return 0F;
			return player.rotationYaw;
		}
		@Override public void setRotationPitch(float pitch) {
			if (player == null)
				return;
			player.prevRotationPitch = player.rotationPitch;
			player.rotationPitch = pitch;
		}
		@Override public void setRotationYaw(float yaw) {
			if (player == null)
				return;
			player.prevRotationYaw = player.rotationYaw;
			player.rotationYaw = yaw;
		}
		
		@Override public VectorBase getRotationBase() {
			return rotationBase;
		}
		@Override public VectorBase getCameraBase() {
			return cameraBase;
		}
		@Override public VectorBase getPreBounceBase() {
			return preBounceBase;
		}
		@Override public VectorBase getPosBounceBase() {
			return posBounceBase;
		}
		
		@Override public float getPrevTickRotationPitch() {
			return prevTickRotationPitch;
		}
		@Override public float getPrevTickRotationRoll() {
			return prevTickRotationRoll;
		}
		@Override public float getPrevTickRotationYaw() {
			return prevTickRotationYaw;
		}
		
		@Override public void setPrevTickRotationPitch(float pitch) {
			this.prevTickRotationPitch = pitch;
		}
		@Override public void setPrevTickRotationRoll(float roll) {
			this.prevTickRotationRoll = roll;
		}
		@Override public void setPrevTickRotationYaw(float yaw) {
			this.prevTickRotationYaw = yaw;
		}
		
		@Override public float getRotationRoll() {
			return rotationRoll;
		}
		@Override public void setRotationRoll(float rotationRoll) {
			this.rotationRoll = rotationRoll;
		}
		
		@Override public float getTiltPitch() {
			return tiltPitch;
		}
		@Override public float getTiltRoll() {
			return tiltRoll;
		}
		@Override public float getTiltYaw() {
			return tiltYaw;
		}
		@Override public void setTiltPitch(float tiltPitch) {
			this.tiltPitch = tiltPitch;
		}
		@Override public void setTiltRoll(float tiltRoll) {
			this.tiltRoll = tiltRoll;
		}
		@Override public void setTiltYaw(float tiltYaw) {
			this.tiltYaw = tiltYaw;
		}
		
		@Override public boolean isFlying() {
			return isFlying;
		}
		@Override public void setFlying(boolean flying) {
			if (player == null)
				return;
			isFlying = flying;
			if (flying)
				lastLiftOff = player.ticksExisted;
		}
		
		@Override public int ticksFlying() {
			return isFlying? player.ticksExisted - lastLiftOff : 0;
		}
		
		@Override public float getPropulsionStrength() {
			return propStrength;
		}
		@Override public void setPropulsionStrength(float strength) {
			this.propStrength = strength;
		}
		@Override public float getPropulsionAcceleration() {
			return propAcc;
		}
		@Override public void setPropulsionAcceleration(float acc) {
			propAcc = acc;
		}
		
		@Override public boolean isBoosted() {
			return boosted;
		}
		@Override public void setBoosted(boolean boosted) {
			this.boosted = boosted;
		}
		
		@Override public float getBoostHeat() {
			return boostHeat;
		}
		@Override public void setBoostHeat(float heat) {
			boostHeat = heat;
		}
		
		@Override public float getBrakeStrength() {
			return brakeStrength;
		}
		@Override public void setBrakeStrength(float strength) {
			brakeStrength = strength;
		}
		@Override public boolean isBraking() {
			return braking;
		}
		@Override public void setBraking(boolean braking) {
			this.braking = braking;
		}
		
		@Override public boolean isSneaking() {
			return sneaking;
		}
		@Override public void setSneaking(boolean sneaking) {
			this.sneaking = sneaking;
		}
		@Override public boolean isJumping() {
			return jumping;
		}
		@Override public void setJumping(boolean jumping) {
			this.jumping = jumping;
		}
		
		@Override public boolean isSprinting() {
			return sprinting;
		}
		@Override public void setSprinting(boolean sprinting) {
			this.sprinting = sprinting;
		}
		
		@Override public boolean isPlayingSound() {
			return playingSound;
		}
		@Override public void setPlayingSound(boolean playing) {
			playingSound = playing;
		}
		
		@Override public double getLastRotationTime() {
			return lastRotationTime;
		}
		@Override public void setLastRotationTime(double time) {
			this.lastRotationTime = time;
		}
		
		@Override public long getLastBounceTime() {
			return lastBounceTime;
		}
		@Override public void setLastBounceTime(long time) {
			lastBounceTime = time;
		}
		
		@Override public Vec3d getLastTrailPos() {
			return lastTrailPos;
		}
		
		@Nullable @Override public ElytraSound getElytraSound() {
			return elytraSound;
		}
		
		@Override public void setElytraSound(ElytraSound sound) {
			elytraSound = sound;
		}
		
	}
	
	/** Default Storage implementation */
	public static class Storage implements IStorage<IAerobaticData> {
		public static final String TAG_ROLL = "Roll";
		public static final String TAG_PITCH_TILT = "PitchTilt";
		public static final String TAG_ROLL_TILT = "RollTilt";
		public static final String TAG_YAW_TILT = "YawTilt";
		public static final String TAG_FLYING = "Flying";
		public static final String TAG_PROPULSION = "Propulsion";
		public static final String TAG_ROTATION_BASE = "Rotation";
		
		@Nullable @Override
		public INBT writeNBT(Capability<IAerobaticData> cap, IAerobaticData inst, Direction side) {
			CompoundNBT nbt = new CompoundNBT();
			
			nbt.putFloat(TAG_ROLL, inst.getRotationRoll());
			
			nbt.putFloat(TAG_PITCH_TILT, inst.getTiltPitch());
			nbt.putFloat(TAG_ROLL_TILT, inst.getTiltRoll());
			nbt.putFloat(TAG_YAW_TILT, inst.getTiltYaw());
			
			nbt.putBoolean(TAG_FLYING, inst.isFlying());
			
			nbt.putFloat(TAG_PROPULSION, inst.getPropulsionStrength());
			
			nbt.put(TAG_ROTATION_BASE, inst.getRotationBase().toNBT());
			
			return nbt;
		}
		
		@Override
		public void readNBT(Capability<IAerobaticData> cap, IAerobaticData inst, Direction side, INBT nbt) {
			CompoundNBT data = (CompoundNBT) nbt;
			
			inst.setRotationRoll(data.getFloat(TAG_ROLL));
			
			inst.setTiltPitch(data.getFloat(TAG_PITCH_TILT));
			inst.setTiltRoll(data.getFloat(TAG_ROLL_TILT));
			inst.setTiltYaw(data.getFloat(TAG_YAW_TILT));
			
			inst.setFlying(data.getBoolean(TAG_FLYING));
			
			inst.setPropulsionStrength(data.getFloat(TAG_PROPULSION));
			
			inst.getRotationBase().readNBT(data.getCompound(TAG_ROTATION_BASE));
		}
	}
}
