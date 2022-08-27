package endorh.aerobaticelytra.client.sound;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.flight.mode.FlightModeTags;
import endorh.util.common.LogUtil;
import endorh.util.common.ObfuscationReflectionUtil;
import endorh.util.common.ObfuscationReflectionUtil.SoftField;
import endorh.util.common.ObfuscationReflectionUtil.SoftMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.util.common.LogUtil.oneTimeLogger;
import static java.lang.Math.*;
import static net.minecraft.util.Mth.clamp;
import static net.minecraft.util.Mth.clampedLerp;

@EventBusSubscriber(value=Dist.CLIENT, modid=AerobaticElytra.MOD_ID)
public class AerobaticElytraSound extends FadingTickableSound {
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	protected final IAerobaticData aerobaticData;
	protected static final int FADE_IN = 15;
	protected static final int FADE_OUT = 8;
	protected static final int MIN_LEN = 20;
	protected static final IAttenuation ATTENUATION = IAttenuation.linear(64F);
	
	private static final SoundSource CATEGORY = SoundSource.PLAYERS;
	
	private static final String REFLECTION_ERROR_MESSAGE =
	  "Aerobatic Elytra sound may not play properly";
	private static final SoftField<ElytraOnPlayerSoundInstance, Player> elytraSound$player =
	  ObfuscationReflectionUtil.getSoftField(
		 ElytraOnPlayerSoundInstance.class, "player", "player",
		 oneTimeLogger(LOGGER::error), REFLECTION_ERROR_MESSAGE);
	
	private static final SoftMethod<AbstractTickableSoundInstance, Void>
	  tickableSound$finishPlaying =
	  ObfuscationReflectionUtil.getSoftMethod(
		 AbstractTickableSoundInstance.class, "stop", "finishPlaying",
		 oneTimeLogger(LOGGER::error), REFLECTION_ERROR_MESSAGE);
	
	protected float brakeVolume = 0F;
	protected float brakePitch = 1F;
	
	protected float rotateVolume = 0F;
	protected float rotatePitch = 1F;
	
	protected float whistleVolume = 0F;
	protected float whistlePitch = 1F;
	
	private final PlayerTickableSubSound brakeSound;
	private final PlayerTickableSubSound rotateSound;
	private final PlayerTickableSubSound whistleSound;
	
	public static void playBoostSound(Player player) {
		playPlayerSound(player, ModSounds.AEROBATIC_ELYTRA_BOOST,
		                SoundSource.PLAYERS, ClientConfig.sound.boost, 1F);
	}
	
	public static void playSlowDownSound(Player player) {
		playPlayerSound(player, ModSounds.AEROBATIC_ELYTRA_SLOWDOWN,
		                SoundSource.PLAYERS, ClientConfig.sound.boost, 1F);
	}
	
	/**
	 * On the server does nothing, on the client plays the sound attenuated
	 * by the distance to the client player.<br>
	 * Intended to be called from client code that runs for {@link RemotePlayer}s too
	 */
	public static void playPlayerSound(
	  Player player, SoundEvent sound, SoundSource category,
	  float volume, float pitch
	) {
		if (!player.level.isClientSide)
			return;
		Vec3 position = player.position();
		if (player instanceof RemotePlayer) {
			Player client = Minecraft.getInstance().player;
			if (client == null)
				return;
			Vec3 clientPos = client.position();
			volume = ATTENUATION.attenuate(
			  volume, (float) position.distanceTo(clientPos));
			position = clientPos;
		}
		player.level.playLocalSound(
		  position.x, position.y, position.z, sound,
		  category, volume, pitch, false);
	}
	
	public AerobaticElytraSound(Player player) {
		super(player, ModSounds.AEROBATIC_ELYTRA_FLIGHT, CATEGORY,
		      FADE_IN, FADE_OUT, MIN_LEN, ATTENUATION);
		aerobaticData = getAerobaticDataOrDefault(player);
		brakeSound = new PlayerTickableSubSound(
		  player, ModSounds.AEROBATIC_ELYTRA_BRAKE, CATEGORY, ATTENUATION);
		rotateSound = new PlayerTickableSubSound(
		  player, ModSounds.AEROBATIC_ELYTRA_ROTATE, CATEGORY, ATTENUATION);
		whistleSound = new PlayerTickableSubSound(
		  player, ModSounds.AEROBATIC_ELYTRA_WHISTLE, CATEGORY, ATTENUATION);
	}
	
	@Override public boolean shouldFadeOut() {
		return !player.isFallFlying();
	}
	
	@Override protected void onStart() {
		ElytraOnPlayerSoundInstance elytraSound = aerobaticData.getElytraSound();
		if (elytraSound != null && !elytraSound.isStopped()) {
			if (tickableSound$finishPlaying.testInvoke(elytraSound)) {
				aerobaticData.setElytraSound(null);
				brakeSound.play();
				rotateSound.play();
				whistleSound.play();
			} else stop();
		}
	}
	
	@Override protected void onFinish() {
		brakeSound.finish();
		rotateSound.finish();
		whistleSound.finish();
	}
	
	@Override protected void onFadeOut() {
		aerobaticData.setPlayingSound(false);
	}
	
	@Override public void tick(float fade_factor) {
		if (AerobaticElytraLogic.hasAerobaticElytra(player) && flightData.getFlightMode().is(
		  FlightModeTags.AEROBATIC)) {
			if (player.isInWater())
				aerobaticUnderwaterTick(fade_factor);
			else aerobaticElytraTick(fade_factor);
		} else {
			brakeVolume = rotateVolume = whistleVolume = 0F;
			brakePitch = rotatePitch = whistlePitch = 1F;
			if (player.isInWater())
				elytraUnderwaterTick(fade_factor);
			else elytraTick(fade_factor);
		}
		brakeSound.setVolume(brakeVolume * ClientConfig.sound.brake);
		brakeSound.setPitch(brakePitch);
		rotateSound.setVolume(rotateVolume * ClientConfig.sound.rotating_wind);
		rotateSound.setPitch(rotatePitch);
		whistleSound.setVolume(whistleVolume * ClientConfig.sound.whistle);
		whistleSound.setPitch(whistlePitch);
	}
	
	/**
	 * Adjust volume and pitch according to aerobatic flight
	 */
	public void aerobaticElytraTick(float fade_factor) {
		float speed = (float) player.getDeltaMovement().length();
		float pitchTilt = abs(aerobaticData.getTiltPitch() / Config.aerobatic.tilt.range_pitch);
		float rollTilt = abs(aerobaticData.getTiltRoll() / Config.aerobatic.tilt.range_roll);
		float yawTilt = abs(aerobaticData.getTiltYaw() / Config.aerobatic.tilt.range_yaw);
		
		float angularStrength = 2 * pitchTilt + rollTilt + 0.2F * yawTilt;
		
		volume = clamp(speed / 4F, -0.2F, 0.6F)
		         + clamp(angularStrength / 3F, 0F, 0.1F) * fade_factor;
		volume = clamp(volume, 0F, 1F);
		pitch = 1.0F;
		
		brakeVolume = aerobaticData.getBrakeStrength() * fade_factor;
		rotatePitch = clamp(angularStrength / 2F, 1F, 1.25F);
		float wVolume = clamp(angularStrength / 2F, 0.1F, 0.9F) * fade_factor
		                * clampedLerp(0F, 1F, speed / 2F);
		rotateVolume = (rotateVolume + wVolume) / 2F;
		
		float wave = (float) sin(player.tickCount / 40F);
		whistleVolume = (float) clampedLerp(0F, 1F, (speed - 2.8) / 1.2 + wave * 0.2F);
		whistlePitch = (float) clampedLerp(1F, 1.4F, (speed - 2.8) / 1.8 + wave * 0.3F);
		volume *= ClientConfig.sound.wind;
	}
	
	/**
	 * Mimic logic from {@link ElytraOnPlayerSoundInstance#tick}
	 */
	public void elytraTick(float fade_factor) {
		final int ageThreshold = 20;
		final float volumeThreshold = 0.8F;
		float speedSquared = (float) player.getDeltaMovement().lengthSqr();
		
		volume = speedSquared >= 1E-7D
		         ? clamp(speedSquared / 4F, 0F, 1F) : 0F;
		volume *= age > ageThreshold
		          ? min(age - ageThreshold, 20) / 20F * fade_factor : 0F;
		pitch = max(1F + volume - volumeThreshold, 1F);
	}
	
	public void aerobaticUnderwaterTick(@SuppressWarnings("unused") float fade_factor) {
		volume = brakeVolume = rotateVolume = whistleVolume = 0F;
		pitch = brakePitch = rotatePitch = whistlePitch = 1F;
	}
	
	public void elytraUnderwaterTick(@SuppressWarnings("unused") float fade_factor) {
		volume = 0F;
		pitch = 1F;
	}
	
	/**
	 * Intercept {@link ElytraOnPlayerSoundInstance}s and replace them if appropriate
	 */
	@SubscribeEvent public static void onSoundEvent(PlaySoundEvent event) {
		if (SoundEvents.ELYTRA_FLYING.getLocation().toString()
		  .equals("minecraft:" + event.getName())) {
			SoundInstance sound = event.getSound();
			if (!(sound instanceof final ElytraOnPlayerSoundInstance elytraSound)) {
				LogUtil.errorOnce(
				  LOGGER, "Non-ElytraSound elytra sound detected, aerobatic elytra " +
				          "sound may not play properly");
				return;
			}
			final Player player = elytraSound$player.get(elytraSound);
			if (player != null) {
				getAerobaticDataOrDefault(player).setElytraSound(elytraSound);
				if (AerobaticElytraLogic.hasAerobaticElytra(player)) {
					event.setResultSound(null);
					new AerobaticElytraSound(player).play();
				}
			}
		}
	}
}
