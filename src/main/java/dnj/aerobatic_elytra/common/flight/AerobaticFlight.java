package dnj.aerobatic_elytra.common.flight;

import dnj.aerobatic_elytra.client.sound.AerobaticElytraSound;
import dnj.aerobatic_elytra.client.sound.ModSounds;
import dnj.aerobatic_elytra.client.trail.AerobaticTrail;
import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.capability.IAerobaticData;
import dnj.aerobatic_elytra.common.capability.IElytraSpec;
import dnj.aerobatic_elytra.common.config.Config;
import dnj.aerobatic_elytra.common.config.Const;
import dnj.aerobatic_elytra.common.event.AerobaticElytraFinishFlightEvent;
import dnj.aerobatic_elytra.common.event.AerobaticElytraStartFlightEvent;
import dnj.aerobatic_elytra.common.event.AerobaticElytraTickEvent;
import dnj.aerobatic_elytra.common.event.AerobaticElytraTickEvent.Pre;
import dnj.aerobatic_elytra.common.item.AerobaticElytraWingItem;
import dnj.aerobatic_elytra.network.AerobaticPackets.DAccelerationPacket;
import dnj.aerobatic_elytra.network.AerobaticPackets.DRotationPacket;
import dnj.aerobatic_elytra.network.AerobaticPackets.DTiltPacket;
import dnj.endor8util.math.Interpolator;
import dnj.endor8util.math.Vec3d;
import dnj.endor8util.math.Vec3f;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static dnj.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticData;
import static dnj.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static dnj.aerobatic_elytra.common.item.IAbility.Ability.*;
import static dnj.endor8util.math.Interpolator.clampedLerp;
import static dnj.endor8util.math.Vec3f.PI;
import static dnj.endor8util.math.Vec3f.PI_HALF;
import static dnj.endor8util.util.TextUtil.stc;
import static dnj.endor8util.util.TextUtil.ttc;
import static java.lang.Math.abs;
import static java.lang.Math.*;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static net.minecraft.util.math.MathHelper.floor;
import static net.minecraft.util.math.MathHelper.signum;
import static net.minecraft.util.math.MathHelper.*;

/**
 * Handle aerobatic physics
 */
public class AerobaticFlight {
	private static final Logger LOGGER = LogManager.getLogger();
	
	private static final Vec3f ZERO = Vec3f.ZERO.get();
	
	// Cache vector instances (since Minecraft is not multi-threaded)
	private static final Vec3f motionVec = Vec3f.ZERO.get();
	private static final Vec3f gravAccVec = Vec3f.ZERO.get();
	private static final Vec3f rainAcc = Vec3f.ZERO.get();
	private static final Vec3f propAccVec = Vec3f.ZERO.get();
	private static final Vec3f glideAccVec = Vec3f.ZERO.get();
	private static final Vec3f angularWindVec = Vec3f.ZERO.get();
	
	/**
	 * Apply the physics of a single elytra travel tick of the player<br>
	 * Is called consistently every tick (20Hz)
	 */
	public static boolean onAerobaticTravel(
	  PlayerEntity player, Vector3d travelVector
	) {
		if (!AerobaticElytraLogic.shouldAerobaticFly(player)) {
			onNonFlightTravel(player, travelVector);
			return false;
		}
		IAerobaticData data = getAerobaticDataOrDefault(player);
		IElytraSpec spec = AerobaticElytraLogic.getElytraSpecOrDefault(player);
		final boolean isRemote = AerobaticElytraLogic.isRemoteClientPlayerEntity(player);
		
		// Post Pre event
		final Pre pre = isRemote? new Pre.Remote(player, spec, data) : new Pre(player, spec, data);
		if (MinecraftForge.EVENT_BUS.post(pre))
			return pre.preventDefault;
		
		// Get gravity and apply SLOW_FALLING potion effect as needed
		double grav = TravelHandler.travelGravity(player);
		if (player.isInWater())
			grav *= 1F - spec.getAbility(AQUATIC);
		else grav *= 1F - spec.getAbility(LIFT);
		
		motionVec.set(player.getMotion());
		Vec3f prevMotionVec = motionVec.copy();
		double hSpeedPrev = motionVec.hNorm();
		
		// Cancel fall damage if falling slowly
		if (motionVec.y > -0.5D - 0.5D * spec.getAbility(LIFT)) {
			player.fallDistance = 1.0F;
		}
		
		// Apply fine acceleration first to avoid losing precision
		applyRotationAcceleration(player);
		
		// Rain and wind
		final float biomePrecipitation = WeatherData.getBiomePrecipitationStrength(player);
		final float rain = player.world.getRainStrength(1F) * biomePrecipitation;
		final float storm = player.world.getThunderStrength(1F) * biomePrecipitation;
		Vec3f windVec = WeatherData.getWindVector(player);
		rainAcc.set(
		  0F,
		  -rain * Config.rain_rain_strength_per_tick - storm * Config.storm_rain_strength_per_tick,
		  0F);
		
		// Update boost
		if (!data.isBoosted()
		    && data.getPropulsionStrength() == Config.propulsion_max
		    && data.isSprinting() && data.getBoostHeat() <= 0.2F) {
			data.setBoosted(true);
			player.addStat(FlightStats.AEROBATIC_BOOSTS, 1);
			if (AerobaticElytraLogic.isAbstractClientPlayerEntity(player)) {
				AerobaticTrail.addBoostParticles(player);
			}
			if (player.world.isRemote)
				AerobaticElytraSound.playBoostSound(player);
		} else if (data.isBoosted()
		           && (!data.isSprinting() || data.getBoostHeat() == 1F)) {
			data.setBoosted(false);
			if (player.world.isRemote)
				AerobaticElytraSound.playSlowDownSound(player);
		}
		final float heatStep = data.isBoosted() ? 0.01F : -0.0075F;
		data.setBoostHeat(clamp(data.getBoostHeat() + heatStep, 0F, 1F));
		final float boostStrength = data.isBoosted() ? 0.04F : 0F;
		
		if (data.isSprinting())
			player.setSprinting(false);
		//LOGGER.debug(format("Boost: %6b, Heat: %2.2f, Sprint: %6b", data.isBoosted(),data.getBoostHeat(), data.isSprinting()));
		
		// Update acceleration
		float propAccStrength = (Config.propulsion_max - Config.propulsion_min) / 20F; // 1 second
		float propAcc = data.getPropulsionAcceleration();
		data.setPropulsionStrength(clamp(
		  data.getPropulsionStrength() + propAcc * propAccStrength,
		  Config.propulsion_min, Config.propulsion_max));
		if (travelVector != null) {
			propAcc = (float) clamp((propAcc + 2 * Math.signum(travelVector.z)) / 3, -1F, 1F);
			data.setPropulsionAcceleration(propAcc);
		}
		
		// Update braking
		float brakeAcc = 0.1F; // Half a second to brake completely
		data.setBraking(player.isCrouching());
		float brakeStrength = clamp(
		  data.getBrakeStrength() + (data.isBraking() ? brakeAcc : - brakeAcc), 0F, 1F);
		data.setBrakeStrength(brakeStrength);
		
		// Get vector base
		VectorBase base = data.getRotationBase();
		if (data.updateFlying(true)) {
			base.init(data);
			MinecraftForge.EVENT_BUS.post(isRemote
			  ? new AerobaticElytraStartFlightEvent.Remote(player, spec, data)
			  : new AerobaticElytraStartFlightEvent(player, spec, data)
			);
		}
		
		// Underwater rotation friction
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		if (player.isInWater()) {
			final float underwaterTiltFriction = clampedLerp(
			  Const.UNDERWATER_CONTROLS_TILT_FRICTION_MAX, Const.UNDERWATER_CONTROLS_TILT_FRICTION_MIN,
			  motionVec.norm() / Const.UNDERWATER_CONTROLS_SPEED_THRESHOLD);
			tiltPitch *= underwaterTiltFriction;
			tiltRoll *= underwaterTiltFriction;
			tiltYaw *= underwaterTiltFriction;
			data.setTiltPitch(tiltPitch);
			data.setTiltRoll(tiltRoll);
			data.setTiltYaw(tiltYaw);
		}
		
		// Angular friction
		float angFriction =
		  1F - (1F - Config.friction_angular)
		       * (tiltPitch * tiltPitch + tiltRoll * tiltRoll + 0.5F * tiltYaw * tiltYaw)
		       / Config.tilt_range_pondered;
		
		float propStrength = data.getPropulsionStrength() * spec.getAbility(SPEED);
		if (data.isBoosted())
			propStrength += boostStrength;
		//float brakeStrength = data.getBrakeStrength();
		
		// Gravity acceleration
		gravAccVec.set(
		  0, -(float) grav * Config.gravity_multiplier - brakeStrength * Config.brake_gravity, 0);
		float stasis = player.isInWater()? 0F :
		               Interpolator.quadInOut(1F - propStrength / Config.propulsion_max);
		gravAccVec.y -= stasis * Config.motorless_gravity_per_tick;
		
		// Friction
		float friction;
		if (player.isInWater()) {
			friction = lerp(
			  spec.getAbility(AQUATIC), Config.friction_water_max, Config.friction_water_min);
			friction *= lerp(brakeStrength, 1F, Config.friction_brake) * angFriction;
		} else {
			friction = lerp(stasis, Config.friction_base, Config.motorless_friction);
			friction = lerp(brakeStrength, friction, Config.friction_brake) * angFriction;
		}
		
		// Glide acceleration
		final float glideAcc = -motionVec.dot(base.normal) * Config.glide_multiplier;
		//glideAcc -= glideAcc * data.getWingTilt();
		glideAccVec.set(base.normal);
		glideAccVec.mul(glideAcc);
		
		// Propulsion
		propAccVec.set(base.look);
		propAccVec.mul(propStrength);
		
		// Apply acceleration
		motionVec.add(glideAccVec);
		motionVec.add(gravAccVec);
		motionVec.add(propAccVec);
		if (Config.weather_enabled && !player.isInWater()) {
			motionVec.add(windVec);
			motionVec.add(rainAcc);
		}
		
		// Apply friction
		motionVec.mul(friction);
		if (Config.weather_enabled && !player.isInWater()) {
			// Wind drags more when braking
			Vec3f stasisVec = windVec.copy();
			stasisVec.mul(1F - friction);
			motionVec.add(stasisVec);
		}
		
		if (player instanceof ServerPlayerEntity) {
			float speed_cap = Config.speed_cap_per_tick;
			if (speed_cap > 0
			    && (motionVec.x > speed_cap || motionVec.y > speed_cap || motionVec.z > speed_cap)) {
				ITextComponent chatWarning =
				  ttc("config.aerobatic-elytra.warning.speed_cap_broken",
				      stc(format("%.1f", max(max(motionVec.x, motionVec.y), motionVec.z))));
				String warning = format(
				  "Player %s is flying too fast!: %.1f. Aerobatic Elytra config might be broken",
				  player.getScoreboardName(), max(max(motionVec.x, motionVec.y), motionVec.z));
				player.sendStatusMessage(chatWarning, false);
				LOGGER.warn(warning);
				motionVec.x = min(motionVec.x, speed_cap);
				motionVec.y = min(motionVec.y, speed_cap);
				motionVec.z = min(motionVec.z, speed_cap);
			}
		}
		
		// Apply motion
		player.setMotion(motionVec.toVector3d());
		if (!isRemote && !AerobaticElytraWingItem.hasDebugWing(player)) {
			player.move(MoverType.SELF, player.getMotion());
		}
		
		if (player.collidedHorizontally || player.collidedVertically) {
			AerobaticCollision.onAerobaticCollision(player, hSpeedPrev, motionVec);
		}
		
		if (AerobaticElytraLogic.isTheClientPlayer(player)) {
			new DTiltPacket(data).send();
			new DRotationPacket(data).send();
			new DAccelerationPacket(data).send();
		}
		
		if (player.isOnGround()) {
			data.land();
		}
		
		// Update player limb swing
		player.func_233629_a_(player, player instanceof IFlyingAnimal);
		
		// Add movement stat
		player.addStat(FlightStats.AEROBATIC_FLIGHT_ONE_CM,
		               (int)Math.round(player.getMotion().length() * 100F));
		
		if (isRemote) {
			if (data.updatePlayingSound(true))
				new AerobaticElytraSound(player).play();
		}
		
		if (AerobaticElytraLogic.isAbstractClientPlayerEntity(player)) {
			if (data.ticksFlying() > Const.TAKEOFF_ANIMATION_LENGTH_TICKS
			    && !player.collidedVertically && !player.collidedHorizontally
			    // Cowardly refuse to smooth trail on bounces
			    && System.currentTimeMillis() - data.getLastBounceTime() > 250L) {
				AerobaticTrail.addParticles(player, motionVec, prevMotionVec);
			}
		}
		
		float prev = data.getPrevTickRotationRoll();
		while (data.getRotationRoll() - prev > 360F)
			prev += 360F;
		while (data.getRotationRoll() - prev < 0F)
			prev -= 360F;
		
		// Debug
		/*if (("Server thread").equals(Thread.currentThread().getName())) {
			LOGGER.info(" L: " + data.getRotationBase().look + ", R: " + data.getRotationBase().roll
			             + ", S: " + String.format("%2.3f", player.getMotion().length()));
		} else {
			LOGGER.debug("L: " + data.getRotationBase().look + ", R: " + data.getRotationBase().roll
			             + ", S: " + String.format("%2.3f", player.getMotion().length()));
		}*/
		data.updatePrevTickAngles();
		
		// Post Post event
		MinecraftForge.EVENT_BUS.post(isRemote
		  ? new AerobaticElytraTickEvent.Post.Remote(player, spec, data)
		  : new AerobaticElytraTickEvent.Post(player, spec, data)
		);
		
		// Cancel default travel logic
		return true;
	}
	
	public static void onNonFlightTravel(
	  PlayerEntity player, @SuppressWarnings("unused") Vector3d travelVector
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.updateBoosted(false)) {
			player.world.playSound(
			  player, player.getPosition(), ModSounds.AEROBATIC_ELYTRA_SLOWDOWN,
			  SoundCategory.PLAYERS, 1F, 1F);
		}
		if (data.updateFlying(false))
			doLand(player, data);
		cooldown(player, data);
	}
	
	/**
	 * Stop braking and decrease propulsion strength until reaching
	 * takeoff propulsion
	 */
	public static void onOtherModeTravel(
	  PlayerEntity player, @SuppressWarnings("unused") Vector3d travelVector
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.updateBoosted(false)) {
			player.world.playSound(
			  player, player.getPosition(), ModSounds.AEROBATIC_ELYTRA_SLOWDOWN,
			  SoundCategory.PLAYERS, 1F, 1F);
		}
		if (data.getRotationBase().valid)
			doLand(player, data);
		cooldown(player, data);
	}
	
	public static void doLand(PlayerEntity player, IAerobaticData data) {
		data.land();
		MinecraftForge.EVENT_BUS.post(
		  AerobaticElytraLogic.isRemoteClientPlayerEntity(player)
		  ? new AerobaticElytraFinishFlightEvent.Remote(player, data)
		  : new AerobaticElytraFinishFlightEvent(player, data)
		);
	}
	
	public static void onRemoteFlightTravel(
	  PlayerEntity player
	) {
		onAerobaticTravel(player, null);
	}
	
	public static void cooldown(PlayerEntity player, IAerobaticData data) {
		float propStrength = data.getPropulsionStrength();
		if (propStrength != Config.propulsion_takeoff) {
			float step = player.isOnGround() ? 0.05F : 0.02F;
			data.setPropulsionStrength(
			  Config.propulsion_takeoff +
			  signum(propStrength - Config.propulsion_takeoff) *
			  max(0F, abs(propStrength - Config.propulsion_takeoff) -
			          step * max(Config.propulsion_max, Config.propulsion_min)));
		}
		float boostHeat = data.getBoostHeat();
		if (boostHeat > 0F) {
			data.setBoostHeat(max(0F, boostHeat - 0.2F));
		}
	}
	
	/**
	 * Applies rotation acceleration.<br>
	 * Gets called more frequently than onAerobaticTravel, because
	 * camera angles must be interpolated per frame to avoid jittery
	 * visuals.
	 */
	public static void applyRotationAcceleration(PlayerEntity player) {
		Optional<IAerobaticData> opt = getAerobaticData(player);
		if (!opt.isPresent())
			return;
		IAerobaticData data = opt.get();
		
		VectorBase rotationBase = data.getRotationBase();
		VectorBase cameraBase = data.getCameraBase();
		
		// Get time delta
		double time = currentTimeMillis() / 1000D; // Time in seconds
		double lastTime = data.getLastRotationTime();
		data.setLastRotationTime(time);
		float delta = (lastTime == 0D) ? 0F : (float) (time - lastTime) * 20F;
		if (delta == 0F) // Happens
			return;
		
		// Wind
		if (Config.weather_enabled)
			angularWindVec.set(WeatherData.getAngularWindVector(player));
		else angularWindVec.set(ZERO);
		
		// Angular acceleration
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		motionVec.set(player.getMotion());
		
		if (!rotationBase.valid) {
			rotationBase.init(data);
		}
		float strength = motionVec.dot(rotationBase.look);
		if (player.isInWater())
			strength = strength * strength / (abs(strength) + 2) + 0.5F;
		final float pitch = (-tiltPitch * strength - angularWindVec.x) * delta;
		float yaw = (tiltYaw * strength - angularWindVec.y) * delta;
		final float roll = (tiltRoll * strength + angularWindVec.z) * delta;
		if (player.isInWater())
			yaw *= 4F;
		
		rotationBase.rotate(pitch, yaw, roll);
		
		long bounceTime = System.currentTimeMillis();
		if (bounceTime - data.getLastBounceTime() <
		    Const.SLIME_BOUNCE_CAMERA_ANIMATION_LENGTH_MS
		) {
			//data.getBounceRotation().add(pitch, yaw, roll);
			float t = Interpolator.quadOut(
			  (bounceTime - data.getLastBounceTime()) /
			  (float) Const.SLIME_BOUNCE_CAMERA_ANIMATION_LENGTH_MS);
			cameraBase.interpolate(
			  t, data.getPreBounceBase(), data.getPosBounceBase(), rotationBase);
		} else {
			cameraBase.set(rotationBase);
		}
		float[] spherical = cameraBase.toSpherical(player.prevRotationYaw);
		
		data.setRotationYaw(spherical[0]);
		data.setRotationPitch(spherical[1]);
		data.setRotationRoll(spherical[2]);
	}
	
	/**
	 * Rotation vector base<br>
	 * Not thread safe
	 */
	public static class VectorBase {
		private static final Vec3f tempVec = Vec3f.ZERO.get();
		private static final VectorBase temp = new VectorBase();
		
		public final Vec3f look = Vec3f.ZERO.get();
		public final Vec3f roll = Vec3f.ZERO.get();
		public final Vec3f normal = Vec3f.ZERO.get();
		
		public boolean valid = true;
		
		public VectorBase() {}
		
		public void init(IAerobaticData data) {
			update(data.getRotationYaw(), data.getRotationPitch(), data.getRotationRoll());
			valid = true;
		}
		
		/**
		 * Set from the spherical coordinates of the look vector, in degrees
		 */
		public void update(float yawDeg, float pitchDeg, float rollDeg) {
			look.set(yawDeg, pitchDeg, true);
			roll.set(yawDeg + 90F, 0F, true);
			roll.rotateAlongOrtVecDegrees(look, rollDeg);
			normal.set(roll);
			normal.cross(look);
		}
		
		/**
		 * Translate to spherical coordinates
		 * @param prevYaw Previous yaw value, since Minecraft does not
		 *                restrict its domain
		 * @return [yaw, pitch, roll] of the look vector, in degrees
		 */
		public float[] toSpherical(float prevYaw) {
			float newPitch = look.getPitch();
			float newYaw;
			float newRoll;
			
			if (abs(newPitch) <= 89.9F) {
				newYaw = look.getYaw();
				tempVec.set(newYaw + 90F, 0F, true);
				newRoll = tempVec.angleUnitaryDegrees(roll, look);
			} else {
				newYaw = newPitch > 0? normal.getYaw() : (normal.getYaw() + 180F) % 360F;
				newRoll = 0F;
			}
			
			// Catch up;
			newYaw += floor(prevYaw / 360F) * 360F;
			if (newYaw - prevYaw > 180F)
				newYaw -= 360F;
			if (newYaw - prevYaw <= -180F)
				newYaw += 360F;
			
			if (Float.isNaN(newYaw) || Float.isNaN(newPitch) || Float.isNaN(newRoll)) {
 				LOGGER.error("Error translating spherical coordinates");
				return new float[] {0F, 0F, 0F};
			}
			
			return new float[] {newYaw, newPitch, newRoll};
		}
		
		/**
		 * Interpolate between bases {@code pre} and {@code pos}, and then rotate as
		 * would be necessary to carry {@code pos} to {@code target}.<br>
		 *
		 * The {@code pos} base can't be dropped, applying the same rotations applied to
		 * {@code target} also to {@code pre}, because 3D rotations are not
		 * commutative. All 3 bases are needed for the interpolation.
		 *
		 * @param t Interpolation progress ∈ [0, 1]
		 * @param pre Start base
		 * @param pos End base
		 * @param target Rotated end base
		 */
		public void interpolate(
		  float t, VectorBase pre, VectorBase pos, VectorBase target
		) {
			set(pre);
			// Lerp rotation
			Vec3f axis = look.copy();
			axis.cross(pos.look);
			if (axis.isZero()) {
				axis.set(normal);
			} else axis.unitary();
			float lookAngle = look.angleUnitary(pos.look, axis);
			tempVec.set(roll);
			tempVec.rotateAlongVec(axis, lookAngle);
			tempVec.unitary();
			float rollAngle = tempVec.angleUnitary(pos.roll, pos.look);
			if (rollAngle > PI) {
				rollAngle = rollAngle - 2 * PI;
			}
			look.rotateAlongOrtVec(axis, lookAngle * t);
			normal.rotateAlongVec(axis, lookAngle * t);
			roll.rotateAlongVec(axis, lookAngle * t);
			roll.rotateAlongOrtVec(look, rollAngle * t);
			normal.rotateAlongOrtVec(look, rollAngle * t);
			
			rotate(pos.angles(target));
			
			look.unitary();
			roll.unitary();
			normal.unitary();
		}
		
		/**
		 * Determine the rotation angles necessary to carry {@code this}
		 * to {@code other} in pitch, yaw, roll order.
		 * @param other Target base
		 * @return [pitch, yaw, roll];
		 */
		public float[] angles(VectorBase other) {
			temp.set(this);
			final float pitch = temp.look.angleProjectedDegrees(other.look, temp.roll);
			temp.look.rotateAlongOrtVecDegrees(temp.roll, pitch);
			temp.normal.rotateAlongOrtVecDegrees(temp.roll, pitch);
			final float yaw = temp.look.angleProjectedDegrees(other.look, temp.normal);
			temp.look.rotateAlongOrtVecDegrees(temp.normal, yaw);
			temp.roll.rotateAlongOrtVecDegrees(temp.normal, yaw);
			final float roll = temp.roll.angleProjectedDegrees(other.roll, temp.look);
			return new float[] {pitch, yaw, roll};
		}
		
		/**
		 * Rotate in degrees in pitch, yaw, roll order and normalize
		 * @param angles [pitch, yaw, roll]
		 */
		public void rotate(float[] angles) {
			rotate(angles[0], angles[1], angles[2]);
		}
		
		/**
		 * Rotate in degrees in pitch, yaw, roll order and normalize.
		 */
		public void rotate(float pitch, float yaw, float roll) {
			look.rotateAlongOrtVecDegrees(this.roll, pitch);
			normal.rotateAlongOrtVecDegrees(this.roll, pitch);
			look.rotateAlongOrtVecDegrees(normal, yaw);
			this.roll.rotateAlongOrtVecDegrees(normal, yaw);
			this.roll.rotateAlongOrtVecDegrees(look, roll);
			normal.rotateAlongOrtVecDegrees(look, roll);
			look.unitary();
			normal.unitary();
			this.roll.unitary();
		}
		
		/**
		 * Mirror across the plane defined by the given axis
		 * @param axis Normal vector to the plane of reflection
		 */
		public void mirror(Vec3f axis) {
			Vec3f ax = axis.copy();
			float angle = ax.angleUnitary(look);
			float mul = -2F;
			if (angle > PI_HALF) {
				angle = PI - angle;
				mul = 2F;
			}
			if (angle < 0.001F)
				ax = normal;
			else {
				ax.cross(look);
				ax.unitary();
			}
			angle = PI + mul * angle;
			look.rotateAlongVec(ax, angle);
			roll.rotateAlongVec(ax, angle);
			normal.rotateAlongVec(ax, angle);
		}
		
		/**
		 * Tilt a base in the same way as the player model is
		 * tilted before rendering.<br>
		 * That is, in degrees in yaw, -pitch, roll order<br>
		 * No normalization is applied
		 */
		public void tilt(float yaw, float pitch, float rollDeg) {
			look.rotateAlongOrtVecDegrees(normal, yaw);
			roll.rotateAlongOrtVecDegrees(normal, yaw);
			look.rotateAlongOrtVecDegrees(roll, -pitch);
			normal.rotateAlongOrtVecDegrees(roll, -pitch);
			roll.rotateAlongOrtVecDegrees(look, rollDeg);
			normal.rotateAlongOrtVecDegrees(look, rollDeg);
		}
		
		/**
		 * Offset the rocket vectors to position them approximately where the
		 * rockets should be
		 */
		public void offset(
		  Vec3d leftRocket, Vec3d rightRocket, Vec3d leftCenterRocket, Vec3d rightCenterRocket
		) {
			look.mul(1.6F);
			normal.mul(0.4F);
			roll.mul(0.7F);
			
			leftRocket.add(look);
			leftRocket.add(normal);
			rightRocket.set(leftRocket);
			leftCenterRocket.set(leftRocket);
			rightCenterRocket.set(rightRocket);
			leftRocket.sub(roll);
			rightRocket.add(roll);
			
			roll.mul(0.1F / 0.7F);
			leftCenterRocket.sub(roll);
			rightCenterRocket.add(roll);
		}
		
		/**
		 * Measure approximate distances to another base in each
		 * axis of rotation
		 * @param base Target base
		 * @return [yaw, pitch, roll] in degrees
		 */
		public float[] distance(VectorBase base) {
			Vec3f compare = base.look.copy();
			Vec3f axis = roll.copy();
			axis.mul(axis.dot(compare));
			compare.sub(axis);
			float pitch;
			if (compare.isZero())
				pitch = 0F;
			else {
				compare.unitary();
				pitch = look.angleUnitaryDegrees(compare);
			}
			compare.set(base.look);
			axis.set(normal);
			axis.mul(axis.dot(compare));
			compare.sub(axis);
			float yaw;
			if (compare.isZero())
				yaw = 0F;
			else {
				compare.unitary();
				yaw = look.angleUnitaryDegrees(compare);
			}
			compare.set(base.roll);
			axis.set(look);
			axis.mul(axis.dot(compare));
			compare.sub(axis);
			float roll;
			if (compare.isZero())
				roll = 0F;
			else {
				compare.unitary();
				roll = this.roll.angleUnitaryDegrees(compare);
			}
			return new float[] {yaw, pitch, roll};
		}
		
		public void set(VectorBase base) {
			look.set(base.look);
			roll.set(base.roll);
			normal.set(base.normal);
		}
		
		public void write(PacketBuffer buf) {
			look.write(buf);
			roll.write(buf);
			normal.write(buf);
		}
		
		public static VectorBase read(PacketBuffer buf) {
			VectorBase base = new VectorBase();
			base.look.set(Vec3f.read(buf));
			base.roll.set(Vec3f.read(buf));
			base.normal.set(Vec3f.read(buf));
			return base;
		}
		
		public CompoundNBT toNBT() {
			CompoundNBT nbt = new CompoundNBT();
			nbt.put("Look", look.toNBT());
			nbt.put("Roll", roll.toNBT());
			nbt.put("Normal", normal.toNBT());
			return nbt;
		}
		
		@SuppressWarnings("unused")
		public static VectorBase fromNBT(CompoundNBT nbt) {
			VectorBase base = new VectorBase();
			base.look.readNBT(nbt.getCompound("Look"));
			base.roll.readNBT(nbt.getCompound("Roll"));
			base.normal.readNBT(nbt.getCompound("Normal"));
			return base;
		}
		
		public void readNBT(CompoundNBT nbt) {
			look.readNBT(nbt.getCompound("Look"));
			roll.readNBT(nbt.getCompound("Roll"));
			normal.readNBT(nbt.getCompound("Normal"));
		}
		
		@Override public String toString() {
			return format("[ %s\n  %s\n  %s ]", look, roll, normal);
		}
	}
}
