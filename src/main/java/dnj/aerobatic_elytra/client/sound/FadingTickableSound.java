package dnj.aerobatic_elytra.client.sound;

import dnj.aerobatic_elytra.common.capability.IFlightData;
import dnj.aerobatic_elytra.debug.DebugOverlay;
import dnj.endor8util.sound.AudioUtil;
import dnj.endor8util.sound.PlayerTickableSound;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

import static dnj.aerobatic_elytra.common.capability.FlightDataCapability.getFlightDataOrDefault;

/**
 * Faded TickableSound with recovery
 */
public abstract class FadingTickableSound extends PlayerTickableSound {
	protected static final Logger LOGGER = LogManager.getLogger();
	
	protected final IFlightData flightData;
	protected int fadeIn;
	protected int fadeOut;
	protected final ResourceLocation type;
	protected int age = 0;
	
	private int remainingMinimum;
	private boolean fading_out;
	private int animation = 0;
	
	@SuppressWarnings("unused")
	public FadingTickableSound(
	  PlayerEntity player, SoundEvent sound, SoundCategory category
	) { this(player, sound, category, 8, 8, 0); }
	
	public FadingTickableSound(
	  PlayerEntity player, SoundEvent sound, SoundCategory category,
	  int fadeIn, int fadeOut, int minimumLength
	) { this(player, sound, category, fadeIn, fadeOut, minimumLength, null); }
	
	public FadingTickableSound(
	  PlayerEntity player, SoundEvent sound, SoundCategory category,
	  int fadeIn, int fadeOut, int minimumLength, @Nullable IAttenuation attenuation
	) { this(player, sound, category, fadeIn, fadeOut, minimumLength, attenuation, sound.getRegistryName()); }
	
	public FadingTickableSound(
	  PlayerEntity player, SoundEvent sound, SoundCategory category,
	  int fadeIn, int fadeOut, int minimumLength,
	  @Nullable IAttenuation attenuation, ResourceLocation type
	) {
		super(player, sound, category, attenuation);
		this.flightData = getFlightDataOrDefault(player);
		this.fadeIn = fadeIn;
		this.fadeOut = fadeOut;
		this.type = type;
		this.remainingMinimum = minimumLength;
		volume = 0F;
		pitch = 1F;
	}
	
	public void recover() {
		fading_out = false;
		animation = Math.round(animation / (float)fadeOut * fadeIn);
	}
	
	public void fadeOut() {
		if (fading_out)
			return;
		fading_out = true;
		animation = Math.round(animation / (float)fadeIn * fadeOut);
		onFadeOut();
	}
	
	@SuppressWarnings("unused")
	protected boolean isFadingOut() {
		return fading_out;
	}
	
	protected void onStart() {}
	protected void onFadeOut() {}
	protected void onFinish() {}
	
	public void play() {
		FadingTickableSound sound = flightData.getFlightSound(type);
		if (sound != null) {
			sound.recover();
			finishPlaying();
		} else {
			Minecraft.getInstance().getSoundHandler().play(this);
			flightData.putFlightSound(type, this);
			onStart();
		}
	}
	
	@Override public final void tick() {
  		super.tick();
		if (animation < 0)
			return;
		age++;
		remainingMinimum--;
		DebugOverlay.animation = animation;
		float fade_factor = 1F;
		if (!player.isAlive() || shouldFadeOut())
			fadeOut();
		if (fading_out) {
			if (remainingMinimum < fadeOut)
				fade_factor = AudioUtil.fadeOutExp((fadeOut - --animation) / (float)fadeOut);
			tick(fade_factor);
			if (animation <= 0) {
				onFinish();
				finishPlaying();
				animation = -1;
				volume = 0F;
				if (flightData.getFlightSound(type) == this)
					flightData.putFlightSound(type, null);
			}
		} else {
			if (animation < fadeIn)
				fade_factor = AudioUtil.fadeInExp(++animation / (float) fadeIn);
			tick(fade_factor);
		}
		DebugOverlay.sound = volume;
	}
	
	public abstract void tick(float fade_factor);
	public boolean shouldFadeOut() {
		return false;
	}
}
