package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.util.math.Vec3d;
import endorh.util.math.Vec3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.util.math.Vec3f.PI;
import static endorh.util.math.Vec3f.PI_HALF;
import static java.lang.Math.abs;
import static java.lang.String.format;

/**
 * Rotation vector base<br>
 * Contains specific methods related to aerobatic flight, it's not
 * a general purpose vector base<br>
 * Not thread safe
 */
public class VectorBase {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Vec3f tempVec = Vec3f.ZERO.get();
	private static final VectorBase temp = new VectorBase();
	
	public final Vec3f look = Vec3f.ZERO.get();
	public final Vec3f roll = Vec3f.ZERO.get();
	public final Vec3f normal = Vec3f.ZERO.get();
	
	public boolean valid = true;
	
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
	 *
	 * @param prevYaw Previous yaw value, since Minecraft does not
	 *   restrict its domain
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
		newYaw += Mth.floor(prevYaw / 360F) * 360F;
		if (newYaw - prevYaw > 180F)
			newYaw -= 360F;
		if (newYaw - prevYaw <= -180F)
			newYaw += 360F;
		
		if (Float.isNaN(newYaw) || Float.isNaN(newPitch) || Float.isNaN(newRoll)) {
			LOGGER.error("Error translating spherical coordinates");
			return new float[]{0F, 0F, 0F};
		}
		
		return new float[]{newYaw, newPitch, newRoll};
	}
	
	/**
	 * Interpolate between bases {@code pre} and {@code pos}, and then rotate as
	 * would be necessary to carry {@code pos} to {@code target}.<br>
	 * <p>
	 * The {@code pos} parameter can't be removed, applying the same rotations applied to
	 * {@code target} also to {@code pre}, because 3D rotations are not commutative.
	 * All 3 bases are needed for the interpolation.
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
		if (rollAngle > PI)
			rollAngle = rollAngle - 2 * PI;
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
	 *
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
		return new float[]{pitch, yaw, roll};
	}
	
	/**
	 * Rotate in degrees in pitch, yaw, roll order and normalize
	 *
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
	 *
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
		if (angle < 0.001F) {
			ax = normal;
		} else {
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
	 * rockets should be in the wings model
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
		
		look.unitary();
		normal.unitary();
		roll.unitary();
	}
	
	public void applyLookAround(float lookYaw, float lookPitch) {
		look.rotateAlongOrtVecDegrees(normal, lookYaw);
		roll.rotateAlongOrtVecDegrees(normal, lookYaw);
		look.rotateAlongOrtVecDegrees(roll, -lookPitch);
		normal.rotateAlongOrtVecDegrees(roll, -lookPitch);
		look.unitary();
		normal.unitary();
		roll.unitary();
	}
	
	/**
	 * Measure approximate distances to another base in each
	 * axis of rotation
	 *
	 * @param base Target base
	 * @return [yaw, pitch, roll] in degrees
	 */
	public float[] distance(VectorBase base) {
		Vec3f compare = base.look.copy();
		Vec3f axis = roll.copy();
		axis.mul(axis.dot(compare));
		compare.sub(axis);
		float pitch;
		if (compare.isZero()) {
			pitch = 0F;
		} else {
			compare.unitary();
			pitch = look.angleUnitaryDegrees(compare);
		}
		compare.set(base.look);
		axis.set(normal);
		axis.mul(axis.dot(compare));
		compare.sub(axis);
		float yaw;
		if (compare.isZero()) {
			yaw = 0F;
		} else {
			compare.unitary();
			yaw = look.angleUnitaryDegrees(compare);
		}
		compare.set(base.roll);
		axis.set(look);
		axis.mul(axis.dot(compare));
		compare.sub(axis);
		float roll;
		if (compare.isZero()) {
			roll = 0F;
		} else {
			compare.unitary();
			roll = this.roll.angleUnitaryDegrees(compare);
		}
		return new float[]{yaw, pitch, roll};
	}
	
	public void set(VectorBase base) {
		look.set(base.look);
		roll.set(base.roll);
		normal.set(base.normal);
	}
	
	public void write(FriendlyByteBuf buf) {
		look.write(buf);
		roll.write(buf);
		normal.write(buf);
	}
	
	public static VectorBase read(FriendlyByteBuf buf) {
		VectorBase base = new VectorBase();
		base.look.set(Vec3f.read(buf));
		base.roll.set(Vec3f.read(buf));
		base.normal.set(Vec3f.read(buf));
		return base;
	}
	
	public CompoundTag toNBT() {
		CompoundTag nbt = new CompoundTag();
		nbt.put("Look", look.toNBT());
		nbt.put("Roll", roll.toNBT());
		nbt.put("Normal", normal.toNBT());
		return nbt;
	}
	
	@SuppressWarnings("unused")
	public static VectorBase fromNBT(CompoundTag nbt) {
		VectorBase base = new VectorBase();
		base.readNBT(nbt);
		return base;
	}
	
	public void readNBT(CompoundTag nbt) {
		look.readNBT(nbt.getCompound("Look"));
		roll.readNBT(nbt.getCompound("Roll"));
		normal.readNBT(nbt.getCompound("Normal"));
	}
	
	@Override public String toString() {
		return format("[ %s\n  %s\n  %s ]", look, roll, normal);
	}
}
