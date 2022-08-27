package endorh.aerobaticelytra.client.render.layer;

import endorh.aerobaticelytra.client.render.model.AerobaticElytraModel;
import endorh.aerobaticelytra.client.render.model.AerobaticElytraModelPose;
import endorh.aerobaticelytra.client.render.model.IElytraPose;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Holds render data for different uses of {@link AerobaticElytraModel}s,
 * in order to provide smooth transitions between their poses.
 */
public class AerobaticRenderData {
	private static final Map<UUID, AerobaticRenderData> INSTANCES =
	  new LinkedHashMap<>(256, 0.75F, true) {
		  @Override protected boolean removeEldestEntry(Entry eldest) {
			  return size() > 1024;
		  }
	  };
	
	public float animationStart = 0F;
	public float cancelLimbSwingAmountProgress = 0F;
	public float animationLength = AerobaticElytraModel.DEFAULT_ANIMATION_LENGTH;
	
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
		if (INSTANCES.containsKey(entity.getUUID())) {
			return INSTANCES.get(entity.getUUID());
		} else {
			AerobaticRenderData data = new AerobaticRenderData();
			INSTANCES.put(entity.getUUID(), data);
			return data;
		}
	}
}
