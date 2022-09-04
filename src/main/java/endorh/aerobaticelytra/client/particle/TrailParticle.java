package endorh.aerobaticelytra.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.capability.IElytraSpec.RocketStar;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.particle.TrailParticleData;
import endorh.util.common.ColorUtil;
import endorh.util.common.LogUtil;
import endorh.util.math.Vec3f;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Optional;
import java.util.Random;

import static endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide.*;
import static java.lang.Math.max;

public class TrailParticle extends TextureSheetParticle {
	
	private static final Random random = new Random();
	private static final Logger LOGGER = LogManager.getLogger();
	
	private final SpriteSet sprites;
	private final float size;
	private final MutableBlockPos pos = new MutableBlockPos((int) x, (int) y, (int) z);
	private final boolean ownPlayer;
	
	private final Vec3f m = Vec3f.ZERO.get();
	private final Vec3f rollVec;
	private final RocketSide side;
	
	private final float[] initialColorHSB = new float[3];
	private final float[] fadeColorHSB = new float[3];
	private final float[] colorRGB = new float[3];
	
	private final Color color;
	private final Color fadeColor;
	
	/**
	 * The shape of the effect
	 * <ul>
	 *  <li>0: Small ball — Normal trail</li>
	 *  <li>1: Large ball — Long trail</li>
	 *  <li>2: Star-shaped — Textured star</li>
	 *  <li>3: Creeper-shaped — Textured face</li>
	 *  <li>4: Burst — Disperse trail</li>
	 *  <li>5: Bubble - Underwater trail</li>
	 * </ul>
	 */
	private final byte type;
	/**
	 * Trail increasingly flickers instead of slowly disappearing
	 */
	private final boolean flicker;
	/**
	 * Trail multiplies into more trails
	 */
	private final boolean trail;
	
	private final TrailData trailData;
	
	private boolean flickerState = true;
	
	protected TrailParticle(
	  ClientLevel world, double x, double y, double z,
	  double speedX, double speedY, double speedZ,
	  Color color, Color fadeColor, byte type, boolean flicker, boolean trail,
	  float size, int life, float partialTick, boolean ownPlayer,
	  @Nullable RocketSide rocketSide, @Nullable Vec3f rollVec,
	  @Nullable TrailData data, SpriteSet sprites
	) {
		super(world, x, y, z, speedX, speedY, speedZ);
		this.sprites = sprites;
		
		this.color = color;
		Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), initialColorHSB);
		this.fadeColor = fadeColor;
		Color.RGBtoHSB(fadeColor.getRed(), fadeColor.getGreen(), fadeColor.getBlue(), fadeColorHSB);
		this.type = type;
		this.flicker = flicker;
		this.trail = trail;
		
		this.ownPlayer = ownPlayer;
		this.rollVec = rollVec;
		this.side = rocketSide;
		this.trailData = data;
		
		ColorUtil.hsbLerpToRgb(0, initialColorHSB, fadeColorHSB, colorRGB);
		setColor(colorRGB[0], colorRGB[1], colorRGB[2]);
		
		this.size = size;
		quadSize = size;
		
		lifetime = max(life, 1);
		
		alpha = 1F;
		gravity = 0.05F;
		if (type == 5)
			gravity = -0.02F;
		
		// Apply partial gravity
		double prevMotionY = -0.04D * gravity * (1F - partialTick);
		move(0D, prevMotionY, 0D);
		
		xd = speedX;
		yd = speedY + prevMotionY;
		zd = speedZ;
		
		hasPhysics = true;
	}
	
	@Override public void render(
	  @NotNull VertexConsumer buffer, @NotNull Camera renderInfo, float partialTicks
	) {
		Minecraft mc = Minecraft.getInstance();
		float lSquared = (float) mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z);
		boolean shouldRender = !ownPlayer || (
		  mc.options.getCameraType() == CameraType.FIRST_PERSON
		  ? (age > 5 || lSquared > 12F) : (age > 10 || lSquared > 6F));
		if (shouldRender) {
			quadSize = getScaleForAge(age + partialTicks);
			setAlpha(getAlphaForAge(age + partialTicks));
			super.render(buffer, renderInfo, partialTicks);
		}
	}
	
	
	
	public float getScaleForAge(float age) {
		final float start_animation = 10F;
		final float end_animation = 30F;
		if (age < start_animation)
			return size * (age / start_animation);
		if (lifetime - age < end_animation)
			return Mth.lerp((age - lifetime + end_animation) / end_animation, size, size * 1.5F);
		return size;
	}
	
	public float getAlphaForAge(float age) {
		final float end_animation = 30F;
		if (age >= lifetime)
			return 0F;
		if (flicker) {
			float r = random.nextFloat();
			if (flickerState) {
				if (r >= 0.6F) {
					flickerState = false;
					return 0F;
				}
			} else if (r < 0.5F) {
				return 0F;
			} else {
				flickerState = true;
			}
		}
		if (lifetime - age < end_animation)
			return Mth.lerp((age - lifetime + end_animation) / end_animation, 0.8F, 0F);
		return Mth.lerp((age / (lifetime - end_animation)), 1F, 0.8F);
	}
	
	@Override public void tick() {
		if (trail && age == 0)
			m.set(xd, yd, zd);
		xo = x;
      yo = y;
      zo = z;
      if (type == 5) {
	      if (random.nextFloat() > 0.9F) {
		      age = lifetime;
	      } else if (!level.getBlockState(pos.set(x, y, z)).getFluidState().is(FluidTags.WATER)) {
		      age = lifetime;
	      }
      }
      
      pickSprite(sprites);
		ColorUtil.hsbLerpToRgb((float) age / lifetime, initialColorHSB, fadeColorHSB, colorRGB);
		setColor(colorRGB[0], colorRGB[1], colorRGB[2]);
      
      if (age++ >= lifetime) {
         remove();
      } else {
         yd -= 0.04D * (double)gravity;
         move(xd, yd, zd);
	
	      float friction = 0.98F; // 0.98F
	      xd *= friction;
         yd *= friction;
         zd *= friction;
         if (onGround) {
            this.xd *= 0.7F;
            zd *= 0.7F;
         }
      }
      if (trail && age == 1) {
      	trail();
      }
	}
	
	/**
	 * Generates particles from this
	 */
	public void trail() {
		m.mul(0.2F);
		m.unitary();
		rollVec.unitary();
		Vec3f r = rollVec.copy();
		float f_r = 0.015F, f_m = 0.1F, f_o = 0.015F, p_r;
		if (side == LEFT || side == CENTER_LEFT)
			rollVec.mul(-1);
		Vec3f o = rollVec.copy();
		if (side == CENTER_LEFT || side == CENTER_RIGHT) {
			rollVec.rotateAlongOrtVecDegrees(m, 30F);
			f_r = 0.025F;
			o.mul(0F);
		}
		for (int i = -2; i <= 2; i += 1) {
			if (i == 0 && (side == CENTER_LEFT || side == CENTER_RIGHT))
				rollVec.rotateAlongOrtVecDegrees(m, -60F);
			p_r = i * f_r;
			r.set(rollVec);
			r.mul(p_r);
			TrailParticleData data = childrenParticle();
			if (data != null) {
				level.addParticle(
				  data, x, y, z,
				  r.x + f_m * m.x + f_o * o.x,
				  r.y + f_m * m.y + f_o * o.y,
				  r.z + f_m * m.z + f_o * o.z);
			}
		}
		lifetime = 2;
	}
	
	public TrailParticleData childrenParticle() {
		if (trailData == null)
			return null;
		Optional<RocketStar> explosionOpt = pickRandom(trailData.get(side));
		if (explosionOpt.isEmpty()) {
			LogUtil.warnOnce(LOGGER, "No explosions in trailed particle");
			return null;
		}
		RocketStar explosion = explosionOpt.get();
		
		Color cColor = new Color(pickRandom(explosion.colors).orElse(color.getRGB()));
		Color cFadeColor = new Color(pickRandom(explosion.colors).orElse(fadeColor.getRGB()));
		final int life = lifetime - 10;
		final float cSize = size * 0.5F;
		return new TrailParticleData(
		  cColor, cFadeColor, explosion.type, explosion.flicker, false,
		  life, cSize, 0F, ownPlayer, rollVec, side, trailData);
	}
	
	private static Optional<Integer> pickRandom(int[] array) {
		if (array == null || array.length == 0)
			return Optional.empty();
		return Optional.of(array[random.nextInt(array.length)]);
	}
	private static<T> Optional<T> pickRandom(T[] array) {
		if (array == null || array.length == 0)
			return Optional.empty();
		return Optional.of(array[random.nextInt(array.length)]);
	}
	
	@NotNull @Override public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
	
	public static class Factory implements ParticleProvider<TrailParticleData> {
		private final SpriteSet sprites;
		public Factory(SpriteSet sprite) {
			sprites = sprite;
		}
		
		@Nullable @Override
		public Particle createParticle(
		  @NotNull TrailParticleData data, @NotNull ClientLevel world,
		  double x, double y, double z,
		  double xSpeed, double ySpeed, double zSpeed
		) {
			TrailParticle particle = new TrailParticle(
			  world, x, y, z, xSpeed, ySpeed, zSpeed,
			  data.color, data.fadeColor, data.type, data.flicker, data.trail,
			  data.size, data.life, data.partialTick,
			  data.ownPlayer, data.side, data.rollVec, data.trailData, sprites);
			particle.setSpriteFromAge(sprites);
			return particle;
		}
	}
	
	private static class MutableBlockPos extends BlockPos {
		public MutableBlockPos(int x, int y, int z) {
			super(x, y, z);
		}
		
		public TrailParticle.MutableBlockPos set(int x, int y, int z) {
			setX(x);
			setY(y);
			setZ(z);
			return this;
		}
		
		public TrailParticle.MutableBlockPos set(double x, double y, double z) {
			setX((int) x);
			setY((int) y);
			setZ((int) z);
			return this;
		}
	}
}
