package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.flight.VectorBase;
import endorh.util.capability.CapabilityProviderSerializable;
import endorh.util.math.Vec3d;
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

import javax.annotation.Nullable;
import java.util.Optional;

import static net.minecraft.util.math.MathHelper.lerp;

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
	  AerobaticElytra.prefix("aerobatic_data");
	
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
	public static IAerobaticData requireAerobaticData(PlayerEntity player) {
		assert CAPABILITY != null;
		return player.getCapability(CAPABILITY).orElseThrow(
		  () -> new IllegalStateException("Missing IAerobaticData capability on player: " + player));
	}
	
	/**
	 * @return The {@link IAerobaticData} from the player or a default
	 * if for some reason the player doesn't have the capability or it's
	 * invalid now
	 * @see AerobaticDataCapability#getAerobaticData
	 * @see AerobaticDataCapability#requireAerobaticData
	 */
	public static IAerobaticData getAerobaticDataOrDefault(PlayerEntity player) {
		assert CAPABILITY != null;
		return player.getCapability(CAPABILITY)
		  .orElse(new AerobaticData(player));
	}
	
	/**
	 * @return The optional {@link IAerobaticData} capability from the player
	 * @see AerobaticDataCapability#getAerobaticDataOrDefault
	 * @see AerobaticDataCapability#requireAerobaticData
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
		IAerobaticData playerData = requireAerobaticData(event.getPlayer());
		playerData.copy(getAerobaticDataOrDefault(event.getOriginal()));
		playerData.reset();
	}
	
	/**
	 * Default implementation for {@link IAerobaticData}
	 */
	public static class AerobaticData implements IAerobaticData {
		protected final PlayerEntity player;
		protected float rotationYaw = 0F;
		protected float rotationPitch = 0F;
		protected float rotationRoll = 0F;
		
		protected float prevTickRotationPitch = 0F;
		protected float prevTickRotationRoll = 0F;
		protected float prevTickRotationYaw = 0F;
		
		protected float tiltPitch = 0F;
		protected float tiltRoll = 0F;
		protected float tiltYaw = 0F;
		
		protected float lookAroundYaw = 0F;
		protected float lookAroundPitch = 0F;
		protected float lookAroundRoll;
		protected float prevLookAroundYaw;
		protected float prevLookAroundPitch;
		protected float prevLookAroundRoll;
		
		protected final VectorBase rotationBase = new VectorBase();
		protected final VectorBase cameraBase = new VectorBase();
		protected final VectorBase preBounceBase = new VectorBase();
		protected final VectorBase posBounceBase = new VectorBase();
		protected final VectorBase lookAroundBase = new VectorBase();
		
		protected boolean braking = false;
		protected float brakeStrength = 0F;
		protected float brakeHeat = 0F;
		protected boolean brakeCooling = false;
		
		protected float propStrength = 0F;
		protected float propAcc = 0F;
		protected boolean boosted = false;
		protected float boostHeat = 0F;
		protected float liftCut = 0F;
		
		protected boolean isFlying = false;
		protected double lastRotationTime = 0D;
		protected long lastBounceTime = 0L;
		
		protected boolean sneaking = false;
		protected boolean jumping = false;
		protected boolean suppressJumping = false;
		protected boolean sprinting = false;
		protected boolean lookingAround = false;
		protected boolean persistentLookAround = false;
		protected boolean aimingBow = false;
		protected boolean playingSound = false;
		
		protected ElytraSound elytraSound = null;
		protected int lastLiftOff = 0;
		protected final Vec3d lastTrailPos = Vec3d.ZERO.get();
		protected boolean affectedByWeather;
		
		public AerobaticData(PlayerEntity player) {
			this.player = player;
		}
		
		@Override public PlayerEntity getPlayer() { return player; }
		
		@Override public float getRotationPitch() {
			return rotationPitch;
		}
		@Override public float getRotationYaw() {
			return rotationYaw;
		}
		@Override public void setRotationPitch(float pitch) {
			rotationPitch = pitch;
			// if (player == null) return;
			// player.xRotO = player.getXRot();
			// player.setXRot(pitch);
		}
		
		@Override public void updateRotation(VectorBase base, float partialTick) {
			float[] spherical = base.toSpherical(player.prevRotationYaw);
			setRotationYaw(spherical[0]);
			setRotationPitch(spherical[1]);
			setRotationRoll(spherical[2]);
			
			lookAroundBase.set(base);
			float lookYaw = lerp(partialTick, getPrevLookAroundYaw(), getLookAroundYaw());
			float lookPitch = lerp(partialTick, getPrevLookAroundPitch(), getLookAroundPitch());
			lookAroundBase.applyLookAround(lookYaw, lookPitch);
			float[] lookSpherical = lookAroundBase.toSpherical(player.prevRotationYaw);
			
			player.prevRotationYaw = player.rotationYaw;
			player.rotationYaw = lookSpherical[0];
			player.prevRotationPitch = player.rotationPitch;
			player.rotationPitch = lookSpherical[1];
			prevLookAroundRoll = lookAroundRoll;
			lookAroundRoll = lookSpherical[2];
		}
		@Override public void setRotationYaw(float yaw) {
			rotationYaw = yaw;
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
			prevTickRotationPitch = pitch;
		}
		@Override public void setPrevTickRotationRoll(float roll) {
			prevTickRotationRoll = roll;
		}
		@Override public void setPrevTickRotationYaw(float yaw) {
			prevTickRotationYaw = yaw;
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
		
		@Override public float getLookAroundYaw() {
			return lookAroundYaw;
		}
		@Override public float getLookAroundPitch() {
			return lookAroundPitch;
		}
		@Override public float getLookAroundRoll() {
			return lookAroundRoll;
		}
		
		@Override public void setLookAroundYaw(float yaw) {
			lookAroundYaw = yaw;
		}
		@Override public void setLookAroundPitch(float pitch) {
			lookAroundPitch = pitch;
		}
		@Override public void setLookAroundRoll(float roll) {
			lookAroundRoll = roll;
		}
		
		@Override public float getPrevLookAroundYaw() {
			return prevLookAroundYaw;
		}
		@Override public float getPrevLookAroundPitch() {
			return prevLookAroundPitch;
		}
		@Override public float getPrevLookAroundRoll() {
			return prevLookAroundRoll;
		}
		
		@Override public void setPrevLookAroundYaw(float yaw) {
			prevLookAroundYaw = yaw;
		}
		@Override public void setPrevLookAroundPitch(float pitch) {
			prevLookAroundPitch = pitch;
		}
		@Override public void setPrevLookAroundRoll(float roll) {
			prevLookAroundRoll = roll;
		}
		
		@Override public boolean isFlying() {
			return isFlying;
		}
		@Override public void setFlying(boolean flying) {
			if (player == null) return;
			isFlying = flying;
			if (flying) lastLiftOff = player.ticksExisted;
		}
		
		@Override public int ticksFlying() {
			return isFlying? player.ticksExisted - lastLiftOff : 0;
		}
		
		@Override public boolean isAffectedByWeather() {
			return affectedByWeather;
		}
		@Override public void setAffectedByWeather(boolean affected) {
			affectedByWeather = affected;
		}
		
		@Override public float getPropulsionStrength() {
			return propStrength;
		}
		@Override public void setPropulsionStrength(float strength) {
			propStrength = strength;
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
		@Override public float getBrakeHeat() {
			return brakeHeat;
		}
		@Override public void setBrakeHeat(float heat) {
			brakeHeat = heat;
		}
		@Override public boolean isBrakeCooling() {
			return brakeCooling;
		}
		@Override public void setBrakeCooling(boolean cooling) {
			brakeCooling = cooling;
		}
		
		@Override public float getLiftCut() {
			return liftCut;
		}
		@Override public void setLiftCut(float cut) {
			liftCut = cut;
		}
		
		@Override public boolean isSneaking() {
			return sneaking;
		}
		@Override public void setSneaking(boolean sneaking) {
			this.sneaking = sneaking;
		}
		@Override public boolean isJumping() {
			return jumping && !isSuppressJumping();
		}
		@Override public void setJumping(boolean jumping) {
			this.jumping = jumping;
			if (!jumping) setSuppressJumping(false);
		}
		
		@Override public boolean isSuppressJumping() {
			return suppressJumping;
		}
		@Override public void setSuppressJumping(boolean suppress) {
			suppressJumping = suppress;
		}
		
		@Override public boolean isSprinting() {
			return sprinting;
		}
		@Override public void setSprinting(boolean sprinting) {
			this.sprinting = sprinting;
		}
		
		@Override public boolean isLookingAround() {
			return lookingAround || isAimingBow();
		}
		@Override public void setLookingAround(boolean lookingAround) {
			this.lookingAround = lookingAround;
		}
		
		@Override public boolean updateLookingAround(boolean lookingAround) {
			if (lookingAround != this.lookingAround) {
				boolean prev = isLookingAround();
				setLookingAround(lookingAround);
				return prev != isLookingAround() && isLookingAround() == lookingAround;
			}
			return false;
		}
		
		@Override public boolean isLookAroundPersistent() {
			return persistentLookAround;
		}
		@Override public void setLookAroundPersistent(boolean persistent) {
			persistentLookAround = persistent;
		}
		
		@Override public boolean isAimingBow() {
			return aimingBow;
		}
		@Override public void setAimingBow(boolean aiming) {
			aimingBow = aiming;
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
			lastRotationTime = time;
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
		public static final String TAG_LOOK_PITCH = "LookPitch";
		public static final String TAG_LOOK_YAW = "LookYaw";
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
			
			nbt.putFloat(TAG_LOOK_PITCH, inst.getLookAroundPitch());
			nbt.putFloat(TAG_LOOK_YAW, inst.getLookAroundYaw());
			
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
			
			inst.setLookAroundPitch(data.getFloat(TAG_LOOK_PITCH));
			inst.setLookAroundYaw(data.getFloat(TAG_LOOK_YAW));
			
			inst.setFlying(data.getBoolean(TAG_FLYING));
			
			inst.setPropulsionStrength(data.getFloat(TAG_PROPULSION));
			
			inst.getRotationBase().readNBT(data.getCompound(TAG_ROTATION_BASE));
		}
	}
}
