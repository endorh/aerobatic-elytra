package endorh.aerobatic_elytra.client.trail;

import endorh.aerobatic_elytra.common.capability.IElytraSpec.RocketStar;
import endorh.aerobatic_elytra.common.flight.AerobaticFlight.VectorBase;
import endorh.util.math.Vec3d;
import endorh.util.math.Vec3f;
import net.minecraft.entity.LivingEntity;

import static endorh.util.math.Vec3f.PI;
import static net.minecraft.util.math.MathHelper.*;

public abstract class BoostShape {
	public abstract void generate(
	  LivingEntity player, Vec3d pos, Vec3f motion, VectorBase base,
	  RocketStar explosion, float radius, float trailMod
	);
	
	public static class CircleBoostShape extends BoostShape {
		public float radius;
		public int points;
		public int passes;
		public float noise;
		
		public CircleBoostShape(float radius, int points, int passes, float noise) {
			this.radius = radius;
			this.points = points;
			this.passes = passes;
			this.noise = noise;
		}
		
		@Override public void generate(
		  LivingEntity player, Vec3d pos, Vec3f motion, VectorBase base,
		  RocketStar explosion, float radius, float trailMod
		) {
			float rad = this.radius * radius;
			for (int i = 0; i < passes; i++) {
				for (int j = 0; j < points; j++) {
					float x = cos(j * PI * 2 / points) * rad;
					float y = sin(j * PI * 2 / points) * rad;
					AerobaticTrail.createBoostParticle(
					  player, explosion, pos, base, motion, x, y, 0F, noise, trailMod);
				}
				rad *= 0.9F;
			}
		}
	}
	
	public static class ShapedBoostShape extends BoostShape {
		public float[][] shape;
		public int points;
		public int passes;
		public float radius;
		public boolean mirror;
		public float noise;
		
		public ShapedBoostShape(
		  float[][] shape, int points, int passes, float radius,
		  boolean mirror, float noise
		) {
			this.shape = shape;
			this.points = points;
			this.passes = passes;
			this.mirror = mirror;
			this.radius = radius;
			this.noise = noise;
		}
		
		@Override public void generate(
		  LivingEntity player, Vec3d pos, Vec3f motion, VectorBase base,
		  RocketStar explosion, float radius, float trailMod
		) {
			float rad = this.radius * radius;
			float x_s, y_s, x_e, y_e, p, x, y;
			
			for (int i = 0; i < passes; i++) {
				x_s = shape[0][0] * rad;
				y_s = shape[0][1] * rad;
				AerobaticTrail.createBoostParticle(
				  player, explosion, pos, base, motion, x_s, y_s, 0F, noise, trailMod);
				
				for (int j = 1; j < shape.length; j++) {
					x_e = shape[j][0] * rad;
					y_e = shape[j][1] * rad;
					
					for (int k = 1; k < points; k++) {
						p = (float) k / points;
						x = lerp(p, x_e, x_s);
						y = lerp(p, y_e, y_s);
						
						AerobaticTrail.createBoostParticle(
						  player, explosion, pos, base, motion, x, y, 0F, noise, trailMod);
						if (mirror) {
							AerobaticTrail.createBoostParticle(
							  player, explosion, pos, base, motion, -x, y, 0F, noise, trailMod);
						}
					}
					x_s = x_e;
					y_s = y_e;
				}
			}
		}
	}
	
	public static class BurstBoostShape extends BoostShape {
		private static final Vec3f direction = Vec3f.ZERO.get();
		public float radius;
		public int points;
		
		public BurstBoostShape(float radius, int points) {
			this.radius = radius;
			this.points = points;
		}
		
		@Override public void generate(
		  LivingEntity player, Vec3d pos, Vec3f motion, VectorBase base,
		  RocketStar explosion, float radius, float trailMod
		) {
			float rad = this.radius * radius;
			for (int i = 0; i < points; i++) {
				direction.setRandomSpherical();
				direction.z *= 0.5F;
				direction.mul(rad);
				AerobaticTrail.createBoostParticle(
				  player, explosion, pos, base, motion,
				  direction.x, direction.y, direction.z, 0F, trailMod);
			}
		}
	}
}
