package dnj.aerobatic_elytra.common.capability;

import dnj.aerobatic_elytra.common.flight.AerobaticFlight.VectorBase;
import dnj.endor8util.math.Vec3d;
import net.minecraft.client.audio.ElytraSound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * {@link PlayerEntity} {@link Capability} containing flight data
 * such as rotation, inclination and acceleration
 */
public interface IAerobaticData extends ILocalPlayerCapability<IAerobaticData> {
	/**
	 * Player of the capability<br>
	 * Used to access the pitch and yaw
	 */
	PlayerEntity getPlayer();
	
	/**
	 * Player pitch, in degrees
	 * @return {@link PlayerEntity#rotationPitch}
	 */
	default float getRotationPitch() {
		return getPlayer().rotationPitch;
	}
	/**
	 * Player roll, in degrees
	 */
	float getRotationRoll();
	/**
	 * Player yaw, in degrees
	 * @return {@link PlayerEntity#rotationYaw}
	 */
	default float getRotationYaw() {
		return getPlayer().rotationYaw;
	}
	
	/**
	 * Set player pitch
	 * @param pitch Pitch in degrees
	 */
	default void setRotationPitch(float pitch) {
		PlayerEntity player = getPlayer();
		player.prevRotationPitch = player.rotationPitch;
		player.rotationPitch = pitch;
	}
	/**
	 * Set player roll
	 * @param roll Roll in degrees, 0 means no roll
	 */
	void setRotationRoll(float roll);
	/**
	 * Set player yaw
	 * @param yaw Yaw in degrees, not bound to -180~180, as Minecraft
	 *            doesn't bound
	 */
	default void setRotationYaw(float yaw) {
		PlayerEntity player = getPlayer();
		player.prevRotationYaw = player.rotationYaw;
		player.rotationYaw = yaw;
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
	 * bounce, used to smooth camera rotations during bounces
	 */
	VectorBase getPreBounceBase();
	
	/**
	 * Rotation base containing the camera orientation after the last bounce,
	 * used to smooth camera rotations during bounces
	 */
	VectorBase getPosBounceBase();
	
	long getLastBounceTime();
	void setLastBounceTime(long time);
	
	/**
	 * Previous player pitch, in degrees, used for rendering interpolation
	 * @return {@link PlayerEntity#prevRotationPitch}
	 */
	default float getPrevTickRotationPitch() {
		return getPlayer().prevRotationPitch;
	}
	/**
	 * Previous player roll, used for rendering interpolation
	 */
	float getPrevTickRotationRoll();
	/**
	 * Previous player yaw, in degrees, used for rendering interpolation
	 * @return {@link PlayerEntity#prevRotationYaw}
	 */
	default float getPrevTickRotationYaw() {
		return getPlayer().prevRotationYaw;
	}
	/**
	 * Set previous player pitch
	 * @param pitch Pitch in degrees
	 */
	void setPrevTickRotationPitch(float pitch);
	/**
	 * Set previous player roll
	 * @param roll Roll in degrees
	 */
	void setPrevTickRotationRoll(float roll);
	/**
	 * Set previous player yaw
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
	 * @return Pitch tilt, in degrees per tick
	 */
	float getTiltPitch();
	/**
	 * Player tilt in the roll axis.<br>
	 * Makes the player rotate in roll every tick.
	 * @return Roll tilt, in degrees per tick
	 */
	float getTiltRoll();
	/**
	 * Player tilt in the yaw axis.<br>
	 * Makes the player rotate in yaw every tick
	 * @return Yaw tilt, in degrees per tick
	 */
	float getTiltYaw();
	
	/**
	 * Set player tilt in the pitch axis.<br>
	 * Makes the player rotate in pitch every tick
	 * @param tiltPitch Pitch tilt, in degrees per tick
	 */
	void setTiltPitch(float tiltPitch);
	/**
	 * Set player tilt in the roll axis.<br>
	 * Makes the player rotate in roll every tick
	 * @param tiltRoll Roll tilt, in degrees per tick
	 */
	void setTiltRoll(float tiltRoll);
	/**
	 * Set player tilt in yaw axis.<br>
	 * Makes the player rotate in yaw every tick
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
	 * @see PlayerEntity#getTicksElytraFlying()
	 */
	int ticksFlying();
	
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
	 * @param heat Boost heat, between 0~1
	 */
	void setBoostHeat(float heat);
	
	/**
	 * @return Braking state
	 */
	boolean isBraking();
	/**
	 * Set braking state
	 * @param braking True if braking
	 */
	void setBraking(boolean braking);
	
	/**
	 * @return Brake strength, between 0~1
	 */
	float getBrakeStrength();
	/**
	 * Set brake strength
	 * @param strength Brake strength, between 0~1
	 */
	void setBrakeStrength(float strength);
	
	/**
	 * Internal sneaking state, different from {@link PlayerEntity#isSneaking()}
	 */
	boolean isSneaking();
	/**
	 * Internal jumping state, different from the {@link PlayerEntity}'s
	 * {@code isJumping} field
	 */
	boolean isJumping();
	/**
	 * Internal sprinting state, different from {@link PlayerEntity#isSprinting()}
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
	 * @param time Last rotation time, in seconds
	 */
	void setLastRotationTime(double time);
	
	Vec3d getLastTrailPos();
	
	/**
	 * Get the {@link ElytraSound} playing for the current player.<br>
	 * Stored to swap it by an {@link dnj.aerobatic_elytra.client.sound.AerobaticElytraSound}
	 * if the player switches to use an aerobatic elytra mid-flight
	 */
	@Nullable ElytraSound getElytraSound();
	/**
	 * Set the {@link ElytraSound} playing for the current player.
	 * Stored to swap it by an {@link dnj.aerobatic_elytra.client.sound.AerobaticElytraSound}
	 * if the player switches to use an aerobatic elytra mid-flight
	 * @param sound Elytra sound event, null to remove it
	 */
	void setElytraSound(@Nullable ElytraSound sound);
	
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
		getLastTrailPos().set(Vector3d.ZERO);
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
		getRotationBase().valid = false;
		getLastTrailPos().set(Vector3d.ZERO);
	}
}
