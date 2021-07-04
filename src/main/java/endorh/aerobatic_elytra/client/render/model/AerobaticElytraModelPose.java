package endorh.aerobatic_elytra.client.render.model;

import net.minecraft.util.math.MathHelper;

public class AerobaticElytraModelPose {
	
	public ModelMovableRotation leftWing = new ModelMovableRotation();
	public float leftTip = 0F;
	public float leftRoll = 0F;
	public float leftPitch = 0F;
	public ModelRotation leftRocket = new ModelRotation();
	public ModelMovableRotation rightWing = new ModelMovableRotation();
	public float rightTip = 0F;
	public float rightRoll = 0F;
	public float rightPitch = 0F;
	public ModelMovableRotation rightRocket = new ModelMovableRotation();
	
	public AerobaticElytraModelPose() {}
	
	public void copyRightFromLeft() {
		rightWing.copy(leftWing, true);
		rightTip = leftTip;
		rightRoll = -leftRoll;
		rightPitch = -leftPitch;
		rightRocket.copy(leftRocket, true);
	}
	
	public void copyLeftFromRight() {
		leftWing.copy(rightWing, true);
		leftTip = rightTip;
		leftRoll = -rightRoll;
		leftPitch = -rightPitch;
		leftRocket.copy(rightRocket, true);
	}
	
	public static class ModelRotation {
		public static final float TO_RAD = (float)(Math.PI / 180D);
		public static final float DEG_360 = 360F * TO_RAD;
		public static final float DEG_5 = 5F * TO_RAD;
		public static final float DEG_10 = 10F * TO_RAD;
		public static final float DEG_15 = 15F * TO_RAD;
		public static final float DEG_20 = 20F * TO_RAD;
		public static final float DEG_25 = 25F * TO_RAD;
		public static final float DEG_30 = 30F * TO_RAD;
		public static final float DEG_35 = 30F * TO_RAD;
		public static final float DEG_40 = 30F * TO_RAD;
		public static final float DEG_45 = 45F * TO_RAD;
		public static final float DEG_60 = 60F * TO_RAD;
		public static final float DEG_85 = 85F * TO_RAD;
		public static final float DEG_90 = 90F * TO_RAD;
		public static final float DEG_95 = 95F * TO_RAD;
		public static final float DEG_175 = 175F * TO_RAD;
		public static final float DEG_180 = 180F * TO_RAD;
		
		public static float lerpAngle(float t, float a, float b) {
			if (a < b && b - a > a - b + DEG_360) {
				final float r = MathHelper.lerp(t, a, b - DEG_360);
				return r <= -DEG_180? r + DEG_360 : r;
			} else if (a > b && a - b > b - a + DEG_360) {
				final float r = MathHelper.lerp(t, a, b + DEG_360);
				return r > DEG_180? r - DEG_360 : r;
			} else {
				return MathHelper.lerp(t, a, b);
			}
		}
		
		public float x = 0F;
		public float y = 0F;
		public float z = 0F;
		
		public ModelRotation() {}
		
		public ModelRotation(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void copy(ModelRotation source) {
			copy(source, false);
		}
		
		public void copy(ModelRotation source, boolean mirror) {
			this.x = source.x;
			this.y = mirror? -source.y : source.y;
			this.z = mirror? -source.z : source.z;
		}
	}
	
	public static class ModelMovableRotation extends ModelRotation {
		public RotationPoint origin;
		
		public ModelMovableRotation() {
			origin = new RotationPoint();
		}
		
		public ModelMovableRotation(float x, float y, float z) {
			super(x, y, z);
		}
		
		public ModelMovableRotation(float x, float y, float z, float oX, float oY, float oZ) {
			super(x, y, z);
			origin = new RotationPoint(oX, oY, oZ);
		}
		
		@Override public void copy(ModelRotation source, boolean mirror) {
			super.copy(source, mirror);
			if (source instanceof ModelMovableRotation)
				origin.copy(((ModelMovableRotation) source).origin, mirror);
		}
		
		public void copyAngles(ModelRotation source) {
			copyAngles(source, false);
		}
		
		public void copyAngles(ModelRotation source, boolean mirror) {
			super.copy(source, mirror);
		}
		
		public void copyOrigin(ModelMovableRotation source) {
			copyOrigin(source, false);
		}
		
		public void copyOrigin(ModelMovableRotation source, boolean mirror) {
			origin.copy(source.origin, mirror);
		}
	}
	
	public static class RotationPoint {
		public float x = 0F;
		public float y = 0F;
		public float z = 0F;
		
		public RotationPoint() {}
		
		public RotationPoint(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public void copy(RotationPoint source) {
			copy(source, false);
		}
		
		public void copy(RotationPoint source, boolean mirror) {
			x = mirror? -source.x : source.x;
			y = source.y;
			z = source.z;
		}
	}
}
