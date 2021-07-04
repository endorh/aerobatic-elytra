package endorh.aerobatic_elytra.client.render.layer;

import endorh.aerobatic_elytra.client.render.model.AerobaticElytraModel;
import endorh.aerobatic_elytra.client.render.model.AerobaticElytraModelPose;
import endorh.aerobatic_elytra.client.render.model.IElytraPose;
import net.minecraft.entity.LivingEntity;

import java.util.HashMap;
import java.util.UUID;

import static endorh.aerobatic_elytra.client.render.model.AerobaticElytraModel.DEFAULT_ANIMATION_LENGTH;

/**
 * Holds render data for different {@link AerobaticElytraModel}
 * instances, in order to provide smooth transitions between
 * poses.
 */
public class AerobaticRenderData {
	private static final
	HashMap<UUID, AerobaticRenderData> INSTANCES = new HashMap<>();
	
	public float animationStart = 0F;
	public float cancelLimbSwingAmountProgress = 0F;
	public float animationLength = DEFAULT_ANIMATION_LENGTH;
	
	public IElytraPose pose = IElytraPose.STANDING_POSE;
	
	public final AerobaticElytraModelPose capturedPose =
	  new AerobaticElytraModelPose();
	
	public boolean updatePose(IElytraPose newState) {
		if (newState != pose) {
			pose = newState;
			return true;
		}
		return false;
	}
	
	private AerobaticRenderData() {}
	
	public static AerobaticRenderData getAerobaticRenderData(LivingEntity entity) {
		if (INSTANCES.containsKey(entity.getUniqueID())) {
			return INSTANCES.get(entity.getUniqueID());
		} else {
			AerobaticRenderData data = new AerobaticRenderData();
			INSTANCES.put(entity.getUniqueID(), data);
			return data;
		}
	}
	
	public static void recycle(LivingEntity entity) {
		INSTANCES.remove(entity.getUniqueID());
	}
}
