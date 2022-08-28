package endorh.aerobaticelytra.common.capability;

import endorh.aerobaticelytra.client.sound.AerobaticElytraSound;
import endorh.aerobaticelytra.common.flight.AerobaticFlight.VectorBase;
import endorh.util.capability.ISerializableCapability;
import endorh.util.math.Vec3d;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * {@link Player} {@link Capability} containing flight data
 * such as rotation, inclination and acceleration
 */
public interface IAerobaticData
  extends ILocalPlayerCapability<IAerobaticData>, ISerializableCapability {
	/**
	 * Player of the capability<br>
	 * Used to access the pitch and yaw
	 */
	Player getPlayer();
	
	/**
	 * Player pitch, in degrees
	 *
	 * @return {@link Player#getXRot()}
	 */
	default float getRotationPitch() {
		return getPlayer().getXRot();
	}
	
	/**
	 * Player roll, in degrees
	 */
	float getRotationRoll();
	
	/**
	 * Player yaw, in degrees
	 *
	 * @return {@link Player#getYRot()}
	 */
	default float getRotationYaw() {
		return getPlayer().getYRot();
	}
	
	/**
	 * Set player pitch
	 *
	 * @param pitch Pitch in degrees
	 */
	default void setRotationPitch(float pitch) {
		Player player = getPlayer();
		player.xRotO = player.getXRot();
		player.setXRot(pitch);
	}
	
	/**
	 * Set player roll
	 *
	 * @param roll Roll in degrees, 0 means no roll
	 */
	void setRotationRoll(float roll);
	
	/**
	 * Set player yaw
	 *
	 * @param yaw Yaw in degrees, not bound to -180~180, as Minecraft
	 *   doesn't bound
	 */
	default void setRotationYaw(float yaw) {
		Player player = getPlayer();
		player.yRotO = player.getYRot();
		player.setYRot(yaw);
	}
	
	/**
	 * Rotation base containing the look, roll and normal vectors
	 * of the player while flying.<br>
	 * Is used to store the flying rotation, because spherical coordinates
	 * aren't lossless near the poles and cause many headaches
	 */
	VectorBase getRotationBase();
	
	/**
	 * Rotation base containing the smoothed camera orientation which
	 * should be used to determine the player rotation ingame<br>
	 * Differs from getRotationBase during slime bounce smoothing.
	 */
	VectorBase getCameraBase();
	
	/**
	 * Rotation base containing the camera orientation previous to the last
	 * bounce, used to smooth camera rotations during bounces.<br>
	 * Must be stored, since 3D rotations are non-commutative
	 */
	VectorBase getPreBounceBase();
	
	/**
	 * Rotation base containing the camera orientation after the last bounce,
	 * used to smooth camera rotations during bounces.<br>
	 * Must be stored, since 3D rotations are non-commutative
	 */
	VectorBase getPosBounceBase();
	
	/**
	 * Last bounce time in ms<br>
	 * Used to interpolate the camera rotation
	 */
	long getLastBounceTime();
	
	/**
	 * Last bounce time in ms<br>
	 * Used to interpolate the camera rotation
	 */
	void setLastBounceTime(long time);
	
	/**
	 * Previous player pitch, in degrees, used for rendering interpolation
	 *
	 * @return {@link Player#xRotO}
	 */
	default float getPrevTickRotationPitch() {
		return getPlayer().xRotO;
	}
	
	/**
	 * Previous player roll, used for rendering interpolation
	 */
	float getPrevTickRotationRoll();
	
	/**
	 * Previous player yaw, in degrees, used for rendering interpolation
	 *
	 * @return {@link Player#yRotO}
	 */
	default float getPrevTickRotationYaw() {
		return getPlayer().yRotO;
	}
	
	/**
	 * Set previous player pitch
	 *
	 * @param pitch Pitch in degrees
	 */
	void setPrevTickRotationPitch(float pitch);
	
	/**
	 * Set previous player roll
	 *
	 * @param roll Roll in degrees
	 */
	void setPrevTickRotationRoll(float roll);
	
	/**
	 * Set previous player yaw
	 *
	 * @param yaw Yaw in degrees
	 */
	void setPrevTickRotationYaw(float yaw);
	
	/**
	 * Updates previous rotation angles (pitch, roll, yaw)
	 * from the current.<br>
	 * Typically called every tick before updating the new angles.
	 */
	default void updatePrevTickAngles() {
		setPrevTickRotationPitch(getRotationPitch());
		setPrevTickRotationRoll(getRotationRoll());
		setPrevTickRotationYaw(getRotationYaw());
	}
	
	/**
	 * Player tilt in the pitch axis.<br>
	 * Makes the player rotate in pitch every tick.
	 *
	 * @return Pitch tilt, in degrees per tick
	 */
	float getTiltPitch();
	
	/**
	 * Player tilt in the roll axis.<br>
	 * Makes the player rotate in roll every tick.
	 *
	 * @return Roll tilt, in degrees per tick
	 */
	float getTiltRoll();
	
	/**
	 * Player tilt in the yaw axis.<br>
	 * Makes the player rotate in yaw every tick
	 *
	 * @return Yaw tilt, in degrees per tick
	 */
	float getTiltYaw();
	
	/**
	 * Set player tilt in the pitch axis.<br>
	 * Makes the player rotate in pitch every tick
	 *
	 * @param tiltPitch Pitch tilt, in degrees per tick
	 */
	void setTiltPitch(float tiltPitch);
	
	/**
	 * Set player tilt in the roll axis.<br>
	 * Makes the player rotate in roll every tick
	 *
	 * @param tiltRoll Roll tilt, in degrees per tick
	 */
	void setTiltRoll(float tiltRoll);
	
	/**
	 * Set player tilt in yaw axis.<br>
	 * Makes the player rotate in yaw every tick
	 *
	 * @param tiltYaw Yaw tilt, in degrees per tick
	 */
	void setTiltYaw(float tiltYaw);
	
	/**
	 * Internal flying state
	 */
	boolean isFlying();
	
	/**
	 * Set internal flying state
	 */
	void setFlying(boolean flying);
	
	/**
	 * Update internal flying state
	 *
	 * @return True if the state changed as a result of the call
	 */
	default boolean updateFlying(boolean flying) {
		if (isFlying() != flying) {
			setFlying(flying);
			return true;
		}
		return false;
	}
	
	/**
	 * Number of ticks using Aerobatic Flight.<br>
	 * Reset to 0 when landing or switching flight mode
	 *
	 * @see Player#getFallFlyingTicks()
	 */
	int getTicksFlying();
	
	/**
	 * {@code player.world.canBlockSeeSky(player.getPosition())}<br>
	 * Updated every tick the player is flying to prevent useless re-computation
	 */
	boolean isAffectedByWeather();
	
	void setAffectedByWeather(boolean affected);
	
	/**
	 * Propulsion delta per tick
	 */
	float getPropulsionAcceleration();
	
	/**
	 * Set propulsion delta per tick
	 */
	void setPropulsionAcceleration(float acc);
	
	/**
	 * @return Player propulsion, in motion/tick
	 */
	float getPropulsionStrength();
	
	/**
	 * Set player propulsion
	 *
	 * @param strength Propulsion, in motion/tick
	 */
	void setPropulsionStrength(float strength);
	
	/**
	 * Boost state
	 */
	boolean isBoosted();
	
	/**
	 * Set boost state
	 */
	void setBoosted(boolean boosted);
	
	/**
	 * Update boost state
	 *
	 * @return True if the boost state changed as a result of the call
	 */
	default boolean updateBoosted(boolean boosted) {
		if (isBoosted() != boosted) {
			setBoosted(boosted);
			return true;
		}
		return false;
	}
	
	/**
	 * @return Boost heat, between 0~1
	 */
	float getBoostHeat();
	
	/**
	 * Set boost heat
	 *
	 * @param heat Boost heat, between 0~1
	 */
	void setBoostHeat(float heat);
	
	/**
	 * @return Braking state
	 */
	boolean isBraking();
	
	/**
	 * Set braking state
	 *
	 * @param braking True if braking
	 */
	void setBraking(boolean braking);
	
	/**
	 * @return Brake strength, between 0~1
	 */
	float getBrakeStrength();
	
	/**
	 * Set brake strength
	 *
	 * @param strength Brake strength, between 0~1
	 */
	void setBrakeStrength(float strength);
	
	/**
	 * Get the brake heat, between 0~1
	 */
	float getBrakeHeat();
	
	/**
	 * Set the brake heat, between 0~1
	 */
	void setBrakeHeat(float heat);
	
	/**
	 * Get the brake cooling state
	 */
	boolean isBrakeCooling();
	
	/**
	 * Set the brake cooling state
	 */
	void setBrakeCooling(boolean cooling);
	
	/**
	 * Get the lift cut applied when colliding
	 */
	float getLiftCut();
	
	/**
	 * Set the lift cut applied when colliding
	 */
	void setLiftCut(float cut);
	
	/**
	 * Internal sneaking state, different from {@link Player#isCrouching()}
	 */
	boolean isSneaking();
	
	/**
	 * Internal jumping state, different from the {@link Player}'s
	 * {@code isJumping} field
	 */
	boolean isJumping();
	
	/**
	 * Internal sprinting state, different from {@link Player#isSprinting()}
	 */
	boolean isSprinting();
	
	/**
	 * Set internal sneaking state
	 */
	void setSneaking(boolean sneaking);
	
	/**
	 * Set internal jumping state
	 */
	void setJumping(boolean jumping);
	
	/**
	 * Set internal sprinting state
	 */
	void setSprinting(boolean sprinting);
	
	/**
	 * Update internal sneaking state
	 *
	 * @return True if the state changed
	 */
	default boolean updateSneaking(boolean sneaking) {
		if (sneaking != isSneaking()) {
			setSneaking(sneaking);
			return true;
		}
		return false;
	}
	
	/**
	 * Update internal jumping state
	 *
	 * @return True if the state changed
	 */
	default boolean updateJumping(boolean jumping) {
		if (jumping != isJumping()) {
			setJumping(jumping);
			return true;
		}
		return false;
	}
	
	/**
	 * Update internal sprinting state
	 *
	 * @return True if the state changed
	 */
	default boolean updateSprinting(boolean sprinting) {
		if (sprinting != isSprinting()) {
			setSprinting(sprinting);
			return true;
		}
		return false;
	}
	
	/**
	 * Used on RemoteClientPlayerEntity s to keep track of the
	 * sound event state
	 */
	boolean isPlayingSound();
	
	void setPlayingSound(boolean playing);
	
	default boolean updatePlayingSound(boolean playing) {
		if (isPlayingSound() != playing) {
			setPlayingSound(playing);
			return true;
		}
		return false;
	}
	
	/**
	 * Last rotation time, in seconds.<br>
	 * Used to interpolate camera angles smoothly on clients.
	 */
	double getLastRotationTime();
	
	/**
	 * Set last rotation time
	 *
	 * @param time Last rotation time, in seconds
	 */
	void setLastRotationTime(double time);
	
	Vec3d getLastTrailPos();
	
	/**
	 * Get the {@link ElytraOnPlayerSoundInstance} playing for the current player.<br>
	 * Stored to swap it by an {@link AerobaticElytraSound}
	 * if the player switches to use an aerobatic elytra mid-flight
	 */
	@Nullable ElytraOnPlayerSoundInstance getElytraSound();
	
	/**
	 * Set the {@link ElytraOnPlayerSoundInstance} playing for the current player.
	 * Stored to swap it by an {@link AerobaticElytraSound}
	 * if the player switches to use an aerobatic elytra mid-flight
	 *
	 * @param sound Elytra sound event, null to remove it
	 */
	void setElytraSound(@Nullable ElytraOnPlayerSoundInstance sound);
	
	/**
	 * Copy save/login-persistent flight data
	 */
	@Override default void copy(IAerobaticData data) {
		setRotationRoll(data.getRotationRoll());
		setTiltPitch(data.getTiltPitch());
		setTiltRoll(data.getTiltRoll());
		setTiltYaw(data.getTiltYaw());
		setFlying(data.isFlying());
		setLastRotationTime(data.getLastRotationTime());
		setPropulsionStrength(data.getPropulsionStrength());
		getRotationBase().set(data.getRotationBase());
	}
	
	/**
	 * Reset flight data, when copying the player across dimensions
	 */
	@Override default void reset() {
		setFlying(false);
		setRotationRoll(0F);
		setTiltPitch(0F);
		setTiltRoll(0F);
		setTiltYaw(0F);
		setLastRotationTime(0D);
		setPropulsionStrength(0F);
		getRotationBase().valid = false;
		getLastTrailPos().set(Vec3.ZERO);
		setLiftCut(0F);
	}
	
	/**
	 * Land the player<br>
	 * Sets flying to false, invalidates the rotation base,
	 * sets all tilts to 0 and sets the roll to 0
	 */
	default void land() {
		setFlying(false);
		setRotationRoll(0F);
		setTiltPitch(0F);
		setTiltRoll(0F);
		setTiltYaw(0F);
		setLastRotationTime(0D);
		setBrakeCooling(false);
		setBrakeHeat(0F);
		getRotationBase().valid = false;
		getLastTrailPos().set(Vec3.ZERO);
		setLiftCut(0F);
	}
}
