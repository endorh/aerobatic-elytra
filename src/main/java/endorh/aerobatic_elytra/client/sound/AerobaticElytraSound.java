package endorh.aerobatic_elytra.client.sound;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.capability.IAerobaticData;
import endorh.aerobatic_elytra.common.config.Config;
import endorh.aerobatic_elytra.common.flight.mode.FlightModeTags;
import endorh.util.common.LogUtil;
import endorh.util.common.ObfuscationReflectionUtil;
import endorh.util.common.ObfuscationReflectionUtil.SoftField;
import endorh.util.common.ObfuscationReflectionUtil.SoftMethod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ElytraSound;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.client.entity.player.RemoteClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.aerobatic_elytra.common.AerobaticElytraLogic.hasAerobaticElytra;
import static endorh.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.util.common.LogUtil.oneTimeLogger;
import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.clampedLerp;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraSound extends FadingTickableSound {
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	protected final IAerobaticData aerobaticData;
	private static final int FADE_IN = 15;
	private static final int FADE_OUT = 8;
	private static final int MIN_LEN = 20;
	private static final IAttenuation ATTENUATION = IAttenuation.linear(64F);
	
	private static final SoundCategory CATEGORY = SoundCategory.PLAYERS;
	
	private static final String REFLECTION_ERROR_MESSAGE =
	  "Aerobatic Elytra sound may not play properly";
	private static final SoftField<ElytraSound, PlayerEntity> elytraSound$player =
	  ObfuscationReflectionUtil.getSoftField(
		 ElytraSound.class, "field_189405_m", "player",
		 oneTimeLogger(LOGGER::error), REFLECTION_ERROR_MESSAGE);
	
	private static final SoftMethod<TickableSound, Void> tickableSound$finishPlaying =
	  ObfuscationReflectionUtil.getSoftMethod(
	    TickableSound.class, "func_239509_o_", "finishPlaying",
	    oneTimeLogger(LOGGER::error), REFLECTION_ERROR_MESSAGE);
	
	protected float brakeVolume = 0F;
	protected float brakePitch = 0F;
	
	protected float whistleVolume = 0F;
	protected float whistlePitch = 0F;
	
	private final PlayerTickableSubSound brakeSound;
	private final PlayerTickableSubSound whistleSound;
	
	public static void playBoostSound(PlayerEntity player) {
		playPlayerSound(player, ModSounds.AEROBATIC_ELYTRA_BOOST,
		                SoundCategory.PLAYERS, 1F, 1F);
	}
	
	public static void playSlowDownSound(PlayerEntity player) {
		playPlayerSound(player, ModSounds.AEROBATIC_ELYTRA_SLOWDOWN,
		                SoundCategory.PLAYERS, 1F, 1F);
	}
	
	/**
	 * On the server does nothing, on the client plays the sound attenuated
	 * by the distance to the client player.<br>
	 * Intended to be called from client code that runs for {@link RemoteClientPlayerEntity}s too
	 */
	public static void playPlayerSound(
	  PlayerEntity player, SoundEvent sound, SoundCategory category,
	  float volume, float pitch
	) {
		if (!player.world.isRemote)
			return;
		Vector3d position = player.getPositionVec();
		if (player instanceof RemoteClientPlayerEntity) {
			PlayerEntity client = Minecraft.getInstance().player;
			if (client == null)
				return;
			Vector3d clientPos = client.getPositionVec();
			volume = ATTENUATION.attenuate(
			  volume, (float) position.distanceTo(clientPos));
			position = clientPos;
		}
		player.world.playSound(
		  position.x, position.y, position.z, sound,
		  category, volume, pitch, false);
	}
	
	public AerobaticElytraSound(PlayerEntity player) {
		super(player, ModSounds.AEROBATIC_ELYTRA_FLIGHT, CATEGORY,
		      FADE_IN, FADE_OUT, MIN_LEN, ATTENUATION);
		aerobaticData = getAerobaticDataOrDefault(player);
		brakeSound = new PlayerTickableSubSound(
		  player, ModSounds.AEROBATIC_ELYTRA_BRAKE, CATEGORY, ATTENUATION);
		whistleSound = new PlayerTickableSubSound(
		  player, ModSounds.AEROBATIC_ELYTRA_WHISTLE, CATEGORY, ATTENUATION);
	}
	
	@Override public boolean shouldFadeOut() {
		return !player.isElytraFlying();
	}
	
	@Override protected void onStart() {
		ElytraSound elytraSound = aerobaticData.getElytraSound();
		if (elytraSound != null && !elytraSound.isDonePlaying()) {
			if (tickableSound$finishPlaying.testInvoke(elytraSound)) {
				aerobaticData.setElytraSound(null);
				brakeSound.play();
				whistleSound.play();
			} else finishPlaying();
		}
	}
	
	@Override protected void onFinish() {
		brakeSound.finish();
		whistleSound.finish();
	}
	
	@Override protected void onFadeOut() {
		aerobaticData.setPlayingSound(false);
	}
	
	@Override public void tick(float fade_factor) {
		if (hasAerobaticElytra(player)
		    && flightData.getFlightMode().is(FlightModeTags.AEROBATIC)) {
			if (player.isInWater()) {
				aerobaticUnderwaterTick(fade_factor);
			} else {
				aerobaticElytraTick(fade_factor);
			}
		} else {
			if (player.isInWater()) {
				elytraUnderwaterTick(fade_factor);
			} else {
				elytraTick(fade_factor);
			}
			brakeVolume = whistleVolume = 0F;
			brakePitch = whistlePitch = 1F;
		}
		brakeSound.setVolume(brakeVolume);
		brakeSound.setPitch(brakePitch);
		whistleSound.setVolume(whistleVolume);
		whistleSound.setPitch(whistlePitch);
	}
	
	/**
	 * Adjust volume and pitch according to aerobatic flight
	 */
	public void aerobaticElytraTick(float fade_factor) {
		float speed = (float)player.getMotion().length();
		float pitchTilt = abs(aerobaticData.getTiltPitch() / Config.aerobatic.tilt.range_pitch);
		float rollTilt = abs(aerobaticData.getTiltRoll() / Config.aerobatic.tilt.range_roll);
		float yawTilt = abs(aerobaticData.getTiltYaw() / Config.aerobatic.tilt.range_yaw);
		
		float angularStrength = 2 * pitchTilt + rollTilt + 0.2F * yawTilt;
		
		volume = clamp(speed / 2F, -0.2F, 0.6F)
		         + clamp(angularStrength / 3F, 0F, 0.1F) * fade_factor;
		volume = clamp(volume, 0F, 1F);
		pitch = 1.0F;
		
		brakeVolume = aerobaticData.getBrakeStrength() * fade_factor;
		whistlePitch = clamp(angularStrength / 2F, 1F, 1.25F);
		float wVolume = clamp(angularStrength / 2F, 0.1F, 0.9F) * fade_factor
		                * (float) clampedLerp(0F, 1F, speed / 2F);
		whistleVolume = (whistleVolume + wVolume) / 2F;
		volume *= 0.5F;
	}
	
	/**
	 * Mimic logic from {@link ElytraSound#tick}
	 */
	public void elytraTick(float fade_factor) {
		final int ageThreshold = 20;
		final float volumeThreshold = 0.8F;
		float speedSquared = (float) player.getMotion().lengthSquared();
		
		volume = speedSquared >= 1E-7D
		         ? clamp(speedSquared / 4F, 0F, 1F) : 0F;
		volume *= age > ageThreshold
		         ? min(age - ageThreshold, 20) / 20F * fade_factor : 0F;
		pitch = max(1F + volume - volumeThreshold, 1F);
	}
	
	public void aerobaticUnderwaterTick(@SuppressWarnings("unused") float fade_factor) {
		volume = brakeVolume = whistleVolume = 0F;
		pitch = brakePitch = whistlePitch = 1F;
	}
	
	public void elytraUnderwaterTick(@SuppressWarnings("unused") float fade_factor) {
		volume = 0F;
		pitch = 1F;
	}
	
	/**
	 * Intercept {@link ElytraSound}s and replace them if appropriate
	 */
	@SubscribeEvent
	public static void onSoundEvent(PlaySoundEvent event) {
		if (SoundEvents.ITEM_ELYTRA_FLYING.getName().toString().equals("minecraft:" + event.getName())) {
			ISound sound = event.getSound();
			if (!(sound instanceof ElytraSound)) {
				LogUtil.errorOnce(
				  LOGGER, "Non-ElytraSound elytra sound detected, aerobatic elytra " +
				          "sound may not play properly");
				return;
			}
			final ElytraSound elytraSound = (ElytraSound)sound;
			final PlayerEntity player = elytraSound$player.get(elytraSound);
			if (player != null) {
				getAerobaticDataOrDefault(player).setElytraSound(elytraSound);
				if (hasAerobaticElytra(player)) {
					event.setResultSound(null);
					new AerobaticElytraSound(player).play();
				}
			}
		}
	}
}
