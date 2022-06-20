package endorh.aerobaticelytra.client.particle;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.capability.IElytraSpec.RocketStar;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.particle.TrailParticleData;
import endorh.util.common.ColorUtil;
import endorh.util.common.LogUtil;
import endorh.util.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;
import java.util.Random;

import static endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide.*;
import static java.lang.Math.max;
import static net.minecraft.util.math.MathHelper.lerp;

public class TrailParticle extends SpriteTexturedParticle {
	
	private static final Random random = new Random();
	private static final Logger LOGGER = LogManager.getLogger();
	
	private final IAnimatedSprite sprites;
	private final float size;
	private final float partialTick;
	private final boolean ownPlayer;
	
	private Vec3f m;
	private final Vec3f rollVec;
	private final RocketSide side;
	
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
	  ClientWorld world, double x, double y, double z,
	  double speedX, double speedY, double speedZ,
	  Color color, Color fadeColor, byte type, boolean flicker, boolean trail,
	  float size, int life, float partialTick, boolean ownPlayer,
	  @Nullable RocketSide rocketSide, @Nullable Vec3f rollVec,
	  @Nullable TrailData data, IAnimatedSprite sprites
	) {
		super(world, x, y, z, speedX, speedY, speedZ);
		this.sprites = sprites;
		
		this.color = color;
		this.fadeColor = fadeColor;
		this.type = type;
		this.flicker = flicker;
		this.trail = trail;
		
		this.partialTick = partialTick;
		this.ownPlayer = ownPlayer;
		this.rollVec = rollVec;
		this.side = rocketSide;
		this.trailData = data;
		
		setColor(this.color);
		
		this.size = size;
		particleScale = size;
		
		maxAge = max(life, 1);
		
		particleAlpha = 1F;
		particleGravity = 0.05F;
		if (type == 5)
			particleGravity = -0.02F;
		
		// Apply partial gravity
		double prevMotionY = -0.04D * particleGravity * (1F - partialTick);
		move(0D, prevMotionY, 0D);
		
		motionX = speedX;
		motionY = speedY + prevMotionY;
		motionZ = speedZ;
		
		canCollide = true;
	}
	
	private void setColor(Color color) {
		setColor(color.getRed()/255F, color.getGreen()/255F, color.getBlue()/255F);
	}
	
	@Override public void renderParticle(
	  @NotNull IVertexBuilder buffer, @NotNull ActiveRenderInfo renderInfo, float partialTicks
	) {
		Minecraft minecraft = Minecraft.getInstance();
		float lSquared =
		  (float)minecraft.gameRenderer.getActiveRenderInfo().getProjectedView()
		    .squareDistanceTo(posX, posY, posZ);
		boolean shouldRender = minecraft.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON
		  ? (age > 5 || lSquared > 12F) : (age > 10 || lSquared > 6F);
		if (shouldRender || !ownPlayer) {
			particleScale = scale(age + partialTicks);
			setAlphaF(alpha(age + partialTicks));
			super.renderParticle(buffer, renderInfo, partialTicks);
		}
	}
	
	
	
	public float scale(float age) {
		final float start_animation = 10F;
		final float end_animation = 30F;
		if (age < start_animation)
			return size * (age / start_animation);
		if (maxAge - age < end_animation)
			return lerp((age - maxAge + end_animation) / end_animation, size, size * 1.5F);
		return size;
	}
	
	public float alpha(float age) {
		final float end_animation = 30F;
		if (age >= maxAge)
			return 0F;
		if (flicker) {
			float r = rand.nextFloat();
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
		if (maxAge - age < end_animation)
			return lerp((age - maxAge + end_animation) / end_animation, 0.8F, 0F);
		return lerp((age / (maxAge - end_animation)), 1F, 0.8F);
	}
	
	@Override public void tick() {
		if (trail && age == 0)
			m = new Vec3f(motionX, motionY, motionZ);
		prevPosX = posX;
      prevPosY = posY;
      prevPosZ = posZ;
      if (type == 5) {
	      if (random.nextFloat() > 0.9F)
		      age = maxAge;
	      else if (!world.getBlockState(new BlockPos(posX, posY, posZ))
	        .getFluidState().getFluid().isIn(FluidTags.WATER))
		      age = maxAge;
      }
      
      selectSpriteRandomly(sprites);
		setColor(ColorUtil.hsbLerp((float)age / maxAge, color, fadeColor));
      
      if (age++ >= maxAge) {
         setExpired();
      } else {
         motionY -= 0.04D * (double)particleGravity;
         move(motionX, motionY, motionZ);
	
	      float friction = 0.98F; // 0.98F
	      motionX *= friction;
         motionY *= friction;
         motionZ *= friction;
         if (onGround) {
            this.motionX *= 0.7F;
            motionZ *= 0.7F;
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
				world.addParticle(
				  data, posX, posY, posZ,
				  r.x + f_m * m.x + f_o * o.x,
				  r.y + f_m * m.y + f_o * o.y,
				  r.z + f_m * m.z + f_o * o.z);
			}
		}
		maxAge = 2;
	}
	
	public TrailParticleData childrenParticle() {
		if (trailData == null)
			return null;
		Optional<RocketStar> explosionOpt = pickRandom(trailData.get(side));
		if (!explosionOpt.isPresent()) {
			LogUtil.warnOnce(LOGGER, "No explosions in trailed particle");
			return null;
		}
		RocketStar explosion = explosionOpt.get();
		
		Color cColor = new Color(pickRandom(explosion.colors).orElse(Color.WHITE.getRGB()));
		Color cFadeColor = new Color(pickRandom(explosion.colors).orElse(color.getRGB()));
		final int life = maxAge - 10;
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
	
	@NotNull @Override public IParticleRenderType getRenderType() {
		return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
	
	public static class Factory implements IParticleFactory<TrailParticleData> {
		private final IAnimatedSprite sprites;
		public Factory(IAnimatedSprite sprite) {
			sprites = sprite;
		}
		@SuppressWarnings("unused")
		public Factory() {
			throw new UnsupportedOperationException("Use the Factory(IAnimatedSprite) constructor");
		}
		
		@Nullable @Override
		public Particle makeParticle(
		  @NotNull TrailParticleData data, @NotNull ClientWorld world,
		  double x, double y, double z,
		  double xSpeed, double ySpeed, double zSpeed
		) {
			TrailParticle particle = new TrailParticle(
			  world, x, y, z, xSpeed, ySpeed, zSpeed,
			  data.color, data.fadeColor, data.type, data.flicker, data.trail,
			  data.size, data.life, data.partialTick,
			  data.ownPlayer, data.side, data.rollVec, data.trailData, sprites);
			particle.selectSpriteWithAge(sprites);
			return particle;
		}
	}
}
