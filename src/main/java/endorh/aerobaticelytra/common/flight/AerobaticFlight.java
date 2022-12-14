package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.sound.AerobaticElytraSound;
import endorh.aerobaticelytra.client.sound.AerobaticSounds;
import endorh.aerobaticelytra.client.trail.AerobaticTrail;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.aerobatic.braking;
import endorh.aerobaticelytra.common.config.Config.aerobatic.physics;
import endorh.aerobaticelytra.common.config.Config.aerobatic.propulsion;
import endorh.aerobaticelytra.common.config.Config.aerobatic.tilt;
import endorh.aerobaticelytra.common.config.Config.network;
import endorh.aerobaticelytra.common.config.Config.weather;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.event.AerobaticElytraFinishFlightEvent;
import endorh.aerobaticelytra.common.event.AerobaticElytraStartFlightEvent;
import endorh.aerobaticelytra.common.event.AerobaticElytraTickEvent;
import endorh.aerobaticelytra.common.event.AerobaticElytraTickEvent.Pre;
import endorh.aerobaticelytra.common.flight.mode.FlightModeTags;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.IAbility.Ability;
import endorh.aerobaticelytra.debug.Debug;
import endorh.aerobaticelytra.network.AerobaticPackets.DAccelerationPacket;
import endorh.aerobaticelytra.network.AerobaticPackets.DLookAroundPacket;
import endorh.aerobaticelytra.network.AerobaticPackets.DRotationPacket;
import endorh.aerobaticelytra.network.AerobaticPackets.DTiltPacket;
import endorh.util.animation.Easing;
import endorh.util.math.Vec3f;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticData;
import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static endorh.aerobaticelytra.common.item.AerobaticElytraWingItem.hasOffhandDebugWing;
import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;
import static java.lang.Math.*;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

/**
 * Handle aerobatic physics
 */
@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class AerobaticFlight {
	private static final Logger LOGGER = LogManager.getLogger();
	private static boolean remoteLookingAround;
	
	private static final Vec3f ZERO = Vec3f.ZERO.get();
	
	// Cache vector instances (since Minecraft is not multi-threaded)
	private static final Vec3f prevMotionVec = Vec3f.ZERO.get();
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
	  Player player, Vec3 travelVector
	) {
		if (!shouldAerobaticFly(player)) {
			onNonFlightTravel(player, travelVector);
			return false;
		}
		IAerobaticData data = getAerobaticDataOrDefault(player);
		IElytraSpec spec = AerobaticElytraLogic.getElytraSpecOrDefault(player);
		final boolean isRemote = AerobaticElytraLogic.isRemoteLocalPlayer(player);
		
		// Post Pre event
		final AerobaticElytraTickEvent pre =
		  isRemote? new AerobaticElytraTickEvent.Remote.Pre(player, spec, data)
		          : new Pre(player, spec, data);
		if (MinecraftForge.EVENT_BUS.post(pre))
			return pre instanceof Pre p && p.isPreventDefault();
		
		// Get gravity and apply SLOW_FALLING potion effect as needed
		double grav = TravelHandler.travelGravity(player);
		if (player.isInWater()) {
			grav *= 1F - spec.getAbility(Ability.AQUATIC);
		} else grav *= 1F - spec.getAbility(Ability.LIFT);
		float liftCut = data.getLiftCut();
		
		motionVec.set(player.getDeltaMovement());
		prevMotionVec.set(motionVec);
		Vec3f prevMotionVec = motionVec.copy();
		double hSpeedPrev = motionVec.hNorm();
		
		// Cancel fall damage if falling slowly
		if (motionVec.y > -0.5D - 0.5D * spec.getAbility(Ability.LIFT)) {
			player.fallDistance = 1.0F;
		}
		
		// Apply fine acceleration first to avoid losing precision
		applyRotationAcceleration(player, 1F);
		
		// Rain and wind
		final boolean affectedByWeather =
		  weather.ignore_cloud_level || player.blockPosition().getY() > weather.cloud_level
		  || player.level.canSeeSkyFromBelowWater(player.blockPosition());
		data.setAffectedByWeather(affectedByWeather);
		final float biomePrecipitation = WeatherData.getBiomePrecipitationStrength(player);
		final float rain = player.level.getRainLevel(1F) * biomePrecipitation;
		final float storm = player.level.getThunderLevel(1F) * biomePrecipitation;
		final boolean useWeather = Config.weather.enabled && rain > 0F && !player.isInWater() && affectedByWeather;
		Vec3f windVec = WeatherData.getWindVector(player);
		rainAcc.set(
		  0F,
		  -rain * Config.weather.rain.rain_strength_tick - storm * Config.weather.storm.rain_strength_tick,
		  0F);
		
		// Update boost
		if (!data.isBoosted()
		    && data.getPropulsionStrength() == propulsion.range_tick.getFloatMax()
		    && data.isSprinting() && data.getBoostHeat() <= 0.2F
		) {
			data.setBoosted(true);
			player.awardStat(FlightStats.AEROBATIC_BOOSTS, 1);
			if (player.level.isClientSide) {
				AerobaticTrail.addBoostParticles(player);
				AerobaticElytraSound.playBoostSound(player);
			}
		} else if (
		  data.isBoosted() && (!data.isSprinting() || data.getBoostHeat() == 1F)
		) {
			data.setBoosted(false);
			if (player.level.isClientSide)
				AerobaticElytraSound.playSlowDownSound(player);
		}
		final float heatStep = data.isBoosted() ? 0.01F : -0.0075F;
		data.setBoostHeat(Mth.clamp(data.getBoostHeat() + heatStep, 0F, 1F));
		final float boostStrength = data.isBoosted() ? 0.04F : 0F;
		
		if (data.isSprinting())
			player.setSprinting(false);
		
		// Update acceleration
		float propAccStrength = propulsion.range_length / 20F; // 1 second
		float propAcc = data.getPropulsionAcceleration();
		data.setPropulsionStrength(Mth.clamp(
		  data.getPropulsionStrength() + propAcc * propAccStrength,
		  propulsion.range_tick.getFloatMin(), propulsion.range_tick.getFloatMax()));
		if (travelVector != null) {
			propAcc = (float) Mth.clamp((propAcc + 2 * Math.signum(travelVector.z)) / 3, -1F, 1F);
			data.setPropulsionAcceleration(propAcc);
		}
		
		// Update braking
		float brakeAcc = 0.1F; // Half a second to brake completely
		data.setBraking(player.isCrouching() && !data.isBrakeCooling());
		if (braking.max_time_ticks > 0) {
			data.setBrakeHeat(
			  Mth.clamp(data.getBrakeHeat() + (data.isBraking()? 1F : -1F) / braking.max_time_ticks, 0F, 1F));
			if (data.getBrakeHeat() >= 1F) {
				data.setBrakeCooling(true);
			} else if (data.getBrakeHeat() <= 0F)
				data.setBrakeCooling(false);
		} else {
			data.setBrakeHeat(0F);
			data.setBrakeCooling(false);
		}
		float brakeStrength = braking.enabled ? Mth.clamp(
		  data.getBrakeStrength() + (data.isBraking() ? brakeAcc : - brakeAcc), 0F, 1F) : 0F;
		data.setBrakeStrength(brakeStrength);
		
		// Get vector base
		VectorBase base = data.getRotationBase();
		if (data.updateFlying(true)) {
			// Copy player rotation
			data.setRotationYaw(player.getYRot());
			data.setRotationPitch(player.getXRot());
			data.setRotationRoll(0F);
			// Suppress tilt decay when holding the flight key
			data.setSuppressJumping(true);
			// Init base
			base.init(data);
			// Fire flight started event
			MinecraftForge.EVENT_BUS.post(isRemote
			  ? new AerobaticElytraStartFlightEvent.Remote(player, spec, data)
			  : new AerobaticElytraStartFlightEvent(player, spec, data));
		}
		
		// Underwater rotation friction
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		if (player.isInWater()) {
			final float underwaterTiltFriction = Mth.clampedLerp(
			  Const.UNDERWATER_CONTROLS_TILT_FRICTION_MAX, Const.UNDERWATER_CONTROLS_TILT_FRICTION_MIN,
			  motionVec.norm() / Const.UNDERWATER_CONTROLS_SPEED_THRESHOLD);
			data.setTiltPitch(tiltPitch *= underwaterTiltFriction);
			data.setTiltRoll(tiltRoll *= underwaterTiltFriction);
			data.setTiltYaw(tiltYaw *= underwaterTiltFriction);
		}
		
		if (data.isJumping()) {
			data.setTiltPitch(tiltPitch *= Const.JUMP_TILT_DECAY);
			data.setTiltRoll(tiltRoll *= Const.JUMP_TILT_DECAY);
			data.setTiltYaw(tiltYaw *= Const.JUMP_TILT_DECAY);
		}
		
		// Angular friction
		float angFriction =
		  1F - (1F - physics.friction_angular)
		       * (tiltPitch * tiltPitch + tiltRoll * tiltRoll + 0.5F * tiltYaw * tiltYaw)
		       / tilt.range_pondered;
		
		float propStrength = data.getPropulsionStrength() * spec.getAbility(Ability.SPEED);
		if (data.isBoosted())
			propStrength += boostStrength;
		
		// Gravity acceleration
		gravAccVec.set(
		  0, -(float) grav * physics.gravity_multiplier - brakeStrength * braking.added_gravity_tick, 0);
		float stasis = player.isInWater()? 0F : Easing.quadInOut(1F - propStrength / propulsion.range_tick.getFloatMax());
		gravAccVec.y -= stasis * physics.motorless_gravity_tick;
		
		// Friction
		float friction;
		if (player.isInWater()) {
			friction = Mth.lerp(
			  spec.getAbility(Ability.AQUATIC), physics.friction_water_nerf, physics.friction_water);
			friction *= Mth.lerp(brakeStrength, 1F, braking.friction) * angFriction;
		} else {
			friction = Mth.lerp(stasis, physics.friction_base, physics.motorless_friction);
			friction = Mth.lerp(brakeStrength, friction, braking.friction) * angFriction;
		}
		
		// Glide acceleration
		final float glideAcc = -motionVec.dot(base.normal) * physics.glide_multiplier;
		glideAccVec.set(base.normal);
		glideAccVec.mul(glideAcc);
		
		// Propulsion
		propAccVec.set(base.look);
		propAccVec.mul(propStrength);
		
		// Apply lift cut
		gravAccVec.mul(1F + liftCut * 0.8F);
		glideAccVec.mul(1F - liftCut);
		motionVec.mul(1F - liftCut * 0.8F);
		
		// Apply acceleration
		motionVec.add(glideAccVec);
		motionVec.add(gravAccVec);
		motionVec.add(propAccVec);
		if (useWeather) {
			motionVec.add(windVec);
			motionVec.add(rainAcc);
		}
		
		// Apply friction
		motionVec.mul(friction);
		if (useWeather) {
			// Wind drags more when braking
			Vec3f stasisVec = windVec.copy();
			stasisVec.mul(1F - friction);
			motionVec.add(stasisVec);
		}
		
		// Speed cap
		if (player instanceof ServerPlayer) {
			float speed_cap = network.speed_cap_tick;
			if (speed_cap > 0
			    && (motionVec.x > speed_cap || motionVec.y > speed_cap || motionVec.z > speed_cap)) {
				Component chatWarning =
				  ttc("aerobaticelytra.config.warning.speed_cap_broken",
				      stc(format("%.1f", max(max(motionVec.x, motionVec.y), motionVec.z))));
				String warning = format(
				  "Player %s is flying too fast!: %.1f. Aerobatic Elytra config might be broken",
				  player.getScoreboardName(), max(max(motionVec.x, motionVec.y), motionVec.z));
				player.displayClientMessage(chatWarning, false);
				LOGGER.warn(warning);
				motionVec.x = min(motionVec.x, speed_cap);
				motionVec.y = min(motionVec.y, speed_cap);
				motionVec.z = min(motionVec.z, speed_cap);
			}
		}
		
		// Apply simulated inertia
		boolean debugWing = AerobaticElytraWingItem.hasDebugWing(player);
		if (!debugWing) // Omitting this 'if' can be funny
			motionVec.lerp(prevMotionVec, physics.inertia);
		
		// Apply motion
		player.setDeltaMovement(motionVec.toVector3d());
		if (!isRemote) {
			if (debugWing == Debug.DEBUG.invertFreeze) player.move(MoverType.SELF, player.getDeltaMovement());
		}
		
		// Collisions
		if (player.horizontalCollision || player.verticalCollision) {
			AerobaticCollision.onAerobaticCollision(player, hSpeedPrev, motionVec);
		} else data.setLiftCut(Mth.clamp(liftCut - 0.15F, 0F, 1F));
		
		// Interpolate looking around
		float lookAroundYaw = data.getLookAroundYaw();
		float lookAroundPitch = data.getLookAroundPitch();
		data.setPrevLookAroundYaw(lookAroundYaw);
		data.setPrevLookAroundPitch(lookAroundPitch);
		
		if (player.isLocalPlayer()) {
			// Update look around
			if (!data.isLookingAround()) {
				if (!data.isLookAroundPersistent()) {
					data.setLookAroundYaw(lookAroundYaw *= 0.5F);
					data.setLookAroundPitch(lookAroundPitch *= 0.5F);
				}
				if (lookAroundYaw != 0F && abs(lookAroundYaw) < 1F)
					data.setLookAroundYaw(lookAroundYaw = 0F);
				if (lookAroundPitch != 0F && abs(lookAroundPitch) < 1F)
					data.setLookAroundPitch(lookAroundPitch = 0F);
			}
			
			// Send update packets to the server
			new DTiltPacket(data).send();
			new DRotationPacket(data).send();
			new DAccelerationPacket(data).send();
			boolean lookingAround = lookAroundYaw != 0F || lookAroundPitch != 0F;
			if (lookingAround || remoteLookingAround)
				new DLookAroundPacket(data).send();
			remoteLookingAround = lookingAround;
		}
		
		// Landing
		if (player.isOnGround())
			data.land();
		
		// Update player limb swing
		player.calculateEntityAnimation(player, player instanceof FlyingAnimal);
		
		// Add movement stat
		player.awardStat(
		  FlightStats.AEROBATIC_FLIGHT_ONE_CM,
		  (int) Math.round(player.getDeltaMovement().length() * 100F));
		
		// Update sound for remote players
		if (isRemote && data.updatePlayingSound(true))
			new AerobaticElytraSound(player).play();
		
		// Add trail
		if (player.level.isClientSide) {
			if (!Debug.DEBUG.suppressParticles) {
				if (data.getTicksFlying() > Const.TAKEOFF_ANIMATION_LENGTH_TICKS
				    && !player.verticalCollision && !player.horizontalCollision
				    // Cowardly refuse to smooth trail on bounces
				    && System.currentTimeMillis() - data.getLastBounceTime() > 250L
				    && !hasOffhandDebugWing(player)) {
					AerobaticTrail.addParticles(player, motionVec, prevMotionVec);
				}
			}
		}
		
		// Update prev tick angles
		float prev = data.getPrevTickRotationRoll();
		while (data.getRotationRoll() - prev > 360F)
			prev += 360F;
		while (data.getRotationRoll() - prev < 0F)
			prev -= 360F;
		data.updatePrevTickAngles();
		
		// Post post event
		MinecraftForge.EVENT_BUS.post(isRemote
		  ? new AerobaticElytraTickEvent.Remote.Post(player, spec, data)
		  : new AerobaticElytraTickEvent.Post(player, spec, data));
		
		// Cancel default travel logic
		return true;
	}
	
	public static void onNonFlightTravel(
	  Player player, @SuppressWarnings("unused") Vec3 travelVector
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.updateBoosted(false)) player.level.playSound(
		  player, player.blockPosition(), AerobaticSounds.AEROBATIC_ELYTRA_SLOWDOWN,
		  SoundSource.PLAYERS, 1F, 1F);
		if (data.updateFlying(false))
			doLand(player, data);
		cooldown(player, data);
	}
	
	/**
	 * Stop braking and decrease propulsion strength until reaching
	 * takeoff propulsion
	 */
	public static void onOtherModeTravel(
	  Player player, Vec3 travelVector
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.updateBoosted(false)) player.level.playSound(
		  player, player.blockPosition(), AerobaticSounds.AEROBATIC_ELYTRA_SLOWDOWN,
		  SoundSource.PLAYERS, 1F, 1F);
		if (data.getRotationBase().valid)
			doLand(player, data);
		cooldown(player, data);
	}
	
	public static void onRemoteOtherModeTravel(Player player) {
		onOtherModeTravel(player, null);
	}
	
	public static void doLand(Player player, IAerobaticData data) {
		data.land();
		MinecraftForge.EVENT_BUS.post(
		  AerobaticElytraLogic.isRemoteLocalPlayer(player)
		  ? new AerobaticElytraFinishFlightEvent.Remote(player, data)
		  : new AerobaticElytraFinishFlightEvent(player, data));
	}
	
	public static void onRemoteFlightTravel(
	  Player player
	) {
		onAerobaticTravel(player, null);
	}
	
	public static void cooldown(Player player, IAerobaticData data) {
		float propStrength = data.getPropulsionStrength();
		if (propStrength != propulsion.takeoff_tick) {
			float step = player.isOnGround() ? 0.05F : 0.02F;
			data.setPropulsionStrength(
			  propulsion.takeoff_tick +
			  Mth.sign(propStrength - propulsion.takeoff_tick) *
			  max(0F, abs(propStrength - propulsion.takeoff_tick) -
			          step * max(propulsion.range_tick.getFloatMax(), propulsion.range_tick.getFloatMin())));
		}
		float boostHeat = data.getBoostHeat();
		if (boostHeat > 0F) data.setBoostHeat(max(0F, boostHeat - 0.2F));
	}
	
	/**
	 * Shorthand for {@code getAerobaticDataOrDefault(player).isFlying()}
	 */
	public static boolean isAerobaticFlying(Player player) {
		return getAerobaticDataOrDefault(player).isFlying();
	}
	
	private static boolean shouldAerobaticFly(Player player) {
		if (!player.isFallFlying() || player.getAbilities().flying
		    || !getFlightDataOrDefault(player).getFlightMode().is(FlightModeTags.AEROBATIC))
			return false;
		final ItemStack elytra = AerobaticElytraLogic.getAerobaticElytra(player);
		if (elytra.isEmpty())
			return false;
		final IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(elytra);
		return (elytra.getDamageValue() < elytra.getMaxDamage() - 1 && spec.getAbility(Ability.FUEL) > 0
		        || player.isCreative())
		       && !player.isInLava() && (!player.isInWater() || spec.getAbility(Ability.AQUATIC) != 0);
	}
	
	/**
	 * Applies rotation acceleration.<br>
	 * Gets called more frequently than onAerobaticTravel, because
	 * camera angles must be interpolated per frame to avoid jittery
	 * visuals.
	 */
	public static void applyRotationAcceleration(Player player, float partialTick) {
		Optional<IAerobaticData> opt = getAerobaticData(player);
		if (opt.isEmpty()) return;
		IAerobaticData data = opt.get();
		
		VectorBase rotationBase = data.getRotationBase();
		VectorBase cameraBase = data.getCameraBase();
		
		// Get time delta
		double time = currentTimeMillis() / 1000D; // Time in seconds
		double lastTime = data.getLastRotationTime();
		data.setLastRotationTime(time);
		float delta = lastTime == 0D? 0F : (float) (time - lastTime) * 20F;
		if (delta == 0F) // Happens
			return;
		
		// Wind
		if (Config.weather.enabled && data.isAffectedByWeather()) {
			angularWindVec.set(WeatherData.getAngularWindVector(player));
		} else angularWindVec.set(ZERO);
		
		// Angular acceleration
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		float tiltYaw = data.getTiltYaw();
		
		motionVec.set(player.getDeltaMovement());
		
		if (!rotationBase.valid) rotationBase.init(data);
		
		float strength = motionVec.dot(rotationBase.look);
		if (player.isInWater())
			strength = strength * strength / (abs(strength) + 2) + 0.5F;
		final float pitch = (-tiltPitch * strength - angularWindVec.x) * delta;
		float yaw = (tiltYaw * strength - angularWindVec.y) * delta;
		final float roll = (tiltRoll * strength + angularWindVec.z) * delta;
		if (player.isInWater()) yaw *= 4F;
		
		rotationBase.rotate(pitch, yaw, roll);
		
		long bounceTime = System.currentTimeMillis();
		if (bounceTime - data.getLastBounceTime() <
		    Const.SLIME_BOUNCE_CAMERA_ANIMATION_LENGTH_MS
		) {
			float t = Easing.quadOut(
			  (bounceTime - data.getLastBounceTime()) /
			  (float) Const.SLIME_BOUNCE_CAMERA_ANIMATION_LENGTH_MS);
			cameraBase.interpolate(
			  t, data.getPreBounceBase(), data.getPosBounceBase(), rotationBase);
		} else cameraBase.set(rotationBase);
		
		data.updateRotation(cameraBase, partialTick);
	}
}
