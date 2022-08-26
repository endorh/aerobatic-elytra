package endorh.aerobaticelytra.client.trail;

import endorh.aerobaticelytra.client.trail.BoostShape.BurstBoostShape;
import endorh.aerobaticelytra.client.trail.BoostShape.CircleBoostShape;
import endorh.aerobaticelytra.client.trail.BoostShape.ShapedBoostShape;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.AerobaticDataCapability;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.capability.IElytraSpec.RocketStar;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.flight.AerobaticFlight.VectorBase;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.item.IAbility.Ability;
import endorh.aerobaticelytra.common.particle.TrailParticleData;
import endorh.util.math.Vec3d;
import endorh.util.math.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TranslationTextComponent;

import java.awt.*;
import java.util.List;
import java.util.*;

import static endorh.aerobaticelytra.common.capability.IElytraSpec.RocketStar.*;
import static java.lang.Math.max;
import static java.lang.Math.round;

public class AerobaticTrail {
	public static final Map<Byte, BoostShape> SHAPES = new HashMap<>();
	
	static {
		SHAPES.put(
		  SHAPE_SMALL_BALL, new CircleBoostShape(0.2F, 50, 2, 0.04F));
		SHAPES.put(
		  SHAPE_LARGE_BALL, new CircleBoostShape(0.3F, 60, 3, 0.05F));
		SHAPES.put(
		  SHAPE_STAR,
		  new ShapedBoostShape(
			 new float[][] {
				{0.0F, 1.0F}, {0.3455F, 0.309F}, {0.9511F, 0.309F},
				{0.3795918F, -0.1265306F}, {0.6122449F, -0.8040816F}, {0.0F, -0.3591837F}
			 }, 5, 3, 0.35F, true, 0.03F));
		SHAPES.put(
		  SHAPE_CREEPER,
		  new ShapedBoostShape(
			 new float[][] {
				{0.0F, 0.2F}, {0.2F, 0.2F}, {0.2F, 0.6F}, {0.6F, 0.6F},
				{0.6F, 0.2F}, {0.2F, 0.2F}, {0.2F, 0.0F}, {0.4F, 0.0F},
				{0.4F, -0.6F}, {0.2F, -0.6F}, {0.2F, -0.4F}, {0.0F, -0.4F}
			 }, 4, 3, 0.45F, true, 0.025F));
		SHAPES.put(
		  SHAPE_BURST, new BurstBoostShape(0.4F, 70));
	}
	
	private static final Random random = new Random();
	
	// Vector cache
	private static final Vec3d rocketLeft = Vec3d.ZERO.get();
	private static final Vec3d rocketRight = Vec3d.ZERO.get();
	private static final Vec3d rocketCenterLeft = Vec3d.ZERO.get();
	private static final Vec3d rocketCenterRight = Vec3d.ZERO.get();
	private static final Vec3d rocketLeftTarget = Vec3d.ZERO.get();
	private static final Vec3d rocketRightTarget = Vec3d.ZERO.get();
	private static final Vec3d rocketCenterLeftTarget = Vec3d.ZERO.get();
	private static final Vec3d rocketCenterRightTarget = Vec3d.ZERO.get();
	
	private static final Vec3d pos = Vec3d.ZERO.get();
	private static final Vec3f motion = Vec3f.ZERO.get();
	private static final Vec3f particleMotion = Vec3f.ZERO.get();
	private static final VectorBase base = new VectorBase();
	private static final VectorBase prevBase = new VectorBase();
	
	/**
	 * Adds particles tailing a flying player<br>
	 * @param player Player flying
	 * @param motionVec Player motion
	 * @param prevMotionVec Previous player motion
	 */
	public static void addParticles(
	  PlayerEntity player, Vec3f motionVec, Vec3f prevMotionVec
	) {
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		base.set(data.getCameraBase());
		prevBase.update(data.getPrevTickRotationYaw(),
		                data.getPrevTickRotationPitch(),
		                data.getPrevTickRotationRoll());
		
		float tiltYaw = data.getTiltYaw();
		float tiltPitch = data.getTiltPitch();
		float tiltRoll = data.getTiltRoll();
		
		pos.set(player.getPositionVec());
		Vec3d lastPos = data.getLastTrailPos();
		if (lastPos.normSquared() < 0.2D) {
			lastPos.set(pos);
			return;
		}
		rocketLeft.set(lastPos);
		rocketRight.set(lastPos);
		rocketCenterLeft.set(lastPos);
		rocketCenterRight.set(lastPos);
		
		if (player.isInWater()) {
			pos.sub(base.look, 1.5F);
			if (data.getPropulsionStrength() == 0F)
				return;
		}
		
		rocketLeftTarget.set(pos);
		rocketRightTarget.set(pos);
		rocketCenterLeftTarget.set(pos);
		rocketCenterRightTarget.set(pos);
		lastPos.set(pos);
		
		prevBase.tilt(tiltYaw, tiltPitch, tiltRoll);
		base.tilt(tiltYaw, tiltPitch, tiltRoll);
		prevBase.offset(rocketLeft, rocketRight, rocketCenterLeft, rocketCenterRight);
		base.offset(rocketLeftTarget, rocketRightTarget,
		            rocketCenterLeftTarget, rocketCenterRightTarget);
		base.normal.unitary();
		base.roll.unitary();
		
		motionVec.mul(-1F);
		prevMotionVec.mul(-1F);
		
		particleMotion.set(prevMotionVec);
		final int c = 5;
		final float s = 1F / c;
		
		final boolean ownPlayer = Minecraft.getInstance().player == player;
		
		final Vec3f roll = base.roll;
		
		ItemStack elytra = AerobaticElytraLogic.getAerobaticElytra(player);
		for (int i = 0; i < c; i++) {
			float t = i * s;
			float l = s / (1F - t);
			rocketLeft.lerp(rocketLeftTarget, l);
			rocketRight.lerp(rocketRightTarget, l);
			rocketCenterLeft.lerp(rocketCenterLeftTarget, l);
			rocketCenterRight.lerp(rocketCenterRightTarget, l);
			particleMotion.set(prevMotionVec);
			particleMotion.lerp(motionVec, t);
			particleMotion.mul(0.1F);
			
			getTrailParticle(player, RocketSide.RIGHT, elytra, i, t, ownPlayer, roll).ifPresent(
			  particle -> {
			  	float[] off = getTransversalOffset(particle.type);
			  	player.world.addParticle(
			  	  particle,
			     rocketRight.x, rocketRight.y, rocketRight.z,
			     particleMotion.x + base.normal.x * off[0] + base.roll.x * off[1],
			     particleMotion.y + base.normal.y * off[0] + base.roll.y * off[1],
			     particleMotion.z + base.normal.z * off[0] + base.roll.z * off[1]);
			  });
			getTrailParticle(player, RocketSide.LEFT, elytra, i, t, ownPlayer, roll).ifPresent(
			  particle -> {
			  	float[] off = getTransversalOffset(particle.type);
				player.world.addParticle(
				  particle,
				  rocketLeft.x, rocketLeft.y, rocketLeft.z,
				  particleMotion.x + base.normal.x * off[0] + base.roll.x * off[1],
				  particleMotion.y + base.normal.y * off[0] + base.roll.y * off[1],
				  particleMotion.z + base.normal.z * off[0] + base.roll.z * off[1]);
			  });
			getTrailParticle(player, RocketSide.CENTER_RIGHT, elytra, i, t, ownPlayer, roll).ifPresent(
			  particle -> {
			  	float[] off = getTransversalOffset(particle.type);
			  	player.world.addParticle(
				  particle,
				  rocketCenterRight.x, rocketCenterRight.y, rocketCenterRight.z,
				  particleMotion.x + base.normal.x * off[0] + base.roll.x * off[1],
				  particleMotion.y + base.normal.y * off[0] + base.roll.y * off[1],
				  particleMotion.z + base.normal.z * off[0] + base.roll.z * off[1]);
			  });
			getTrailParticle(player, RocketSide.CENTER_LEFT, elytra, i, t, ownPlayer, roll).ifPresent(
			  particle -> {
			  	float[] off = getTransversalOffset(particle.type);
				player.world.addParticle(
				  particle,
				  rocketCenterLeft.x, rocketCenterLeft.y, rocketCenterLeft.z,
				  particleMotion.x + base.normal.x * off[0] + base.roll.x * off[1],
				  particleMotion.y + base.normal.y * off[0] + base.roll.y * off[1],
				  particleMotion.z + base.normal.z * off[0] + base.roll.z * off[1]);
			  });
		}
	}
	
	public static Optional<TrailParticleData> getTrailParticle(
	  PlayerEntity player, RocketSide side, ItemStack elytra,
	  int partial, float partialTick, boolean ownPlayer, Vec3f rollVec
	) {
		IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(elytra);
		TrailData data = spec.getTrailData();
		if (player.isInWater()) {
			return shouldGenerate((byte)5, partial) ? Optional.of(new TrailParticleData(
			  Color.WHITE, Color.LIGHT_GRAY, (byte)5, false, false,
			  35, 0.16F, partialTick, ownPlayer, rollVec, side, data
			)) : Optional.empty();
		}
		Optional<RocketStar> explosionOpt = pickRandom(data.get(side));
		if (!explosionOpt.isPresent()) // Default
			return shouldGenerate((byte)0, partial) ? Optional.of(new TrailParticleData(
			  Color.WHITE, Color.WHITE, (byte)0, false, false,
			  25, 0.2F, partialTick, ownPlayer, rollVec, side, data)) : Optional.empty();
		RocketStar explosion = explosionOpt.get();
		
		final Color color = new Color(pickRandom(explosion.colors).orElse(Color.WHITE.getRGB()));
		final Color fadeColor = new Color(pickRandom(explosion.fadeColors).orElse(color.getRGB()));
		final int life = round(getLife(explosion.type, explosion.trail) * spec.getAbility(
		  Ability.TRAIL));
		final float size = (float) MathHelper.clampedLerp(0.4F, 0.5F, spec.getAbility(Ability.TRAIL));
		
		return shouldGenerate(explosion.type, partial) ? Optional
		  .of(new TrailParticleData(
			 color, fadeColor, explosion.type, explosion.flicker, explosion.trail, life, size,
			 partialTick, ownPlayer, rollVec, side, data)) : Optional.empty();
	}
	
	public static void addBoostParticles(PlayerEntity player) {
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		IElytraSpec spec = AerobaticElytraLogic.getElytraSpecOrDefault(player);
		pos.set(player.getPositionVec());
		motion.set(player.getMotion());
		particleMotion.set(motion);
		particleMotion.mul(0.1F);
		base.update(data.getRotationYaw(), data.getRotationPitch(), data.getRotationRoll());
		TrailData trail = spec.getTrailData();
		final float trailModifier = spec.getAbility(Ability.TRAIL);
		
		Optional<RocketStar[]> listOpt = trail.pickRandom();
		RocketStar explosion = null;
		if (listOpt.isPresent()) {
			final Optional<RocketStar> explosionOpt = pickRandom(listOpt.get());
			if (explosionOpt.isPresent())
				explosion = explosionOpt.get();
		}
		
		byte shape = explosion == null? 0 : explosion.type;
		BoostShape boostShape = SHAPES.get(shape);
		
		if (explosion != null && explosion.trail) {
			float radius = 1.2F;
			int halos = 3;
			pos.sub(motion, halos);
			particleMotion.sub(motion, 0.05F);
			for (int i = 0; i < halos; i++) {
				boostShape.generate(
				  player, pos, particleMotion, base, explosion, radius, trailModifier);
				if (i < halos - 1) {
					pos.add(motion);
					particleMotion.add(motion, 0.05F);
					radius *= 0.8F;
				}
			}
		} else {
			boostShape.generate(player, pos, particleMotion, base, explosion, 1F, trailModifier);
		}
	}
	
	public static final TrailParticleData DEFAULT_BOOST_PARTICLE =
	  new TrailParticleData(
		 Color.WHITE, Color.WHITE, (byte)0, false, false,
		 35, 0.3F, 0F, false, null,
		 null, null);
	public static final TrailParticleData UNDERWATER_BOOST_PARTICLE =
	  new TrailParticleData(
		 Color.WHITE, Color.LIGHT_GRAY, (byte)5, false, false,
		 35, 0.16F, 0F, false, null,
		 null, null);
	
	public static TrailParticleData getBoostParticle(
	  LivingEntity player, RocketStar explosion, float trailMod
	) {
		if (player.isInWater())
			return UNDERWATER_BOOST_PARTICLE;
		if (explosion == null)
			return DEFAULT_BOOST_PARTICLE;
		final Color color = new Color(pickRandom(explosion.colors).orElse(Color.WHITE.getRGB()));
		final Color fadeColor = new Color(pickRandom(explosion.fadeColors).orElse(color.getRGB()));
		final int life = round(getLife(explosion.type, explosion.trail) * trailMod * 0.6F);
		final float size = (float) MathHelper.clampedLerp(0.4F, 0.5F, trailMod);
		
		return new TrailParticleData(
		  color, fadeColor, explosion.type, explosion.flicker, false, life, size,
		  0F, false, null, null, null);
	}
	
	private static final Vec3f off = Vec3f.ZERO.get();
	public static void createBoostParticle(
	  LivingEntity player, RocketStar explosion,
	  Vec3d pos, VectorBase base, Vec3f motion,
	  float x, float y, float z, float noise, float trailMod
	) {
		TrailParticleData data = getBoostParticle(player, explosion, trailMod);
		if (data == null)
			return;
		off.set(base.roll);
		off.mul(x);
		off.add(base.normal, y);
		if (z != 0F) off.add(base.look, z);
		if (noise > 0F) {
			off.add(base.roll, (float) random.nextGaussian() * noise * max(0.2F, x));
			off.add(base.normal, (float) random.nextGaussian() * noise * max(0.2F, y));
			off.add(base.look, (float) random.nextGaussian() * noise * max(0.2F, z));
		}
		player.world.addParticle(
		  data, pos.x, pos.y, pos.z,
		  motion.x + off.x, motion.y + off.y, motion.z + off.z);
	}
	
	public static boolean shouldGenerate(byte type, int partial) {
		return type != 5 || partial == 0;
	}
	
	public static int getLife(byte type, boolean trail) {
		int life = type == SHAPE_LARGE_BALL ? 180 : 120;
		if (trail)
			life = round(life * 0.6F);
		return life;
	}
	
	public static float[] getTransversalOffset(byte type) {
		switch (type) {
			case SHAPE_BUBBLE:
				return new float[] {
				  (float)random.nextGaussian() * 0.025F,
				  (float)random.nextGaussian() * 0.025F};
			case SHAPE_BURST:
				return new float[] {
				  (float)random.nextGaussian() * 0.05F,
				  (float)random.nextGaussian() * 0.05F};
			case SHAPE_STAR:
			case SHAPE_CREEPER:
				return new float[] {
				  (float)random.nextGaussian() * 0.002F,
				  (float)random.nextGaussian() * 0.002F};
			default:
				return new float[] {0F, 0F};
		}
	}
	
	// Utils
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
	
	/**
	 * The four rockets attached to an Aerobatic Elytra
	 */
	public enum RocketSide {
		LEFT("Left", "aerobaticelytra.side.left", WingSide.LEFT),
		RIGHT("Right", "aerobaticelytra.side.right", WingSide.RIGHT),
		CENTER_LEFT("CenterLeft", "aerobaticelytra.side.center_left", WingSide.LEFT),
		CENTER_RIGHT("CenterRight", "aerobaticelytra.side.center_right", WingSide.RIGHT);
		
		public final String tagName;
		public final String translationKey;
		public final WingSide wingSide;
		
		private static final Map<WingSide, RocketSide[]> wingSideMap = new HashMap<>();
		
		static {
			Map<WingSide, List<RocketSide>> listMap = new HashMap<>();
			for (RocketSide side : values()) {
				if (!listMap.containsKey(side.wingSide))
					listMap.put(side.wingSide, new ArrayList<>());
				listMap.get(side.wingSide).add(side);
			}
			for (Map.Entry<WingSide, List<RocketSide>> entry : listMap.entrySet()) {
				//noinspection SimplifyStreamApiCallChains
				wingSideMap.put(entry.getKey(), entry.getValue().stream().toArray(RocketSide[]::new));
			}
		}
		
		RocketSide(String tagName, String translationKey, WingSide wingSide) {
			this.tagName = tagName;
			this.translationKey = translationKey;
			this.wingSide = wingSide;
		}
		public TranslationTextComponent getDisplayName() {
			return new TranslationTextComponent(translationKey);
		}
		
		public RocketSide opposite() {
			switch (this) {
				case LEFT: return RIGHT;
				case RIGHT: return LEFT;
				case CENTER_LEFT: return CENTER_RIGHT;
				case CENTER_RIGHT: return CENTER_LEFT;
				default: return null;
			}
		}
		
		public static RocketSide[] forWingSide(WingSide side) {
			if (side == null)
				return values();
			return wingSideMap.getOrDefault(side, new RocketSide[0]);
		}
	}
}
