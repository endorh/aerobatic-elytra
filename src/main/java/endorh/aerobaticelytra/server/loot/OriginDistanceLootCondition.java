package endorh.aerobaticelytra.server.loot;

import com.google.gson.*;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.util.math.vector.Vector3d;
import org.jetbrains.annotations.NotNull;

public class OriginDistanceLootCondition implements ILootCondition {
	protected final double minDistance;
	protected final double maxDistance;
	
	public OriginDistanceLootCondition(double minDistance, double maxDistance) {
		this.minDistance = minDistance;
		this.maxDistance = maxDistance;
	}
	
	@Override public @NotNull LootConditionType func_230419_b_() {
		return ModLootConditions.ORIGIN_DISTANCE;
	}
	
	@Override public boolean test(LootContext lootContext) {
		Vector3d position = lootContext.get(LootParameters.field_237457_g_);
		if (position == null)
			throw new IllegalStateException(
			  "Cannot use 'origin_distance' loot condition in this loot table type");
		final Vector3d flatPosition = position.subtract(0D, position.y, 0D);
		final double distance = flatPosition.distanceTo(Vector3d.ZERO);
		return minDistance < distance && distance < maxDistance;
	}
	
	public static class Serializer implements ILootSerializer<OriginDistanceLootCondition> {
		@Override public void serialize(
		  @NotNull JsonObject json, @NotNull OriginDistanceLootCondition condition,
		  @NotNull JsonSerializationContext serializationContext
		) {
			if (condition.minDistance > 0D)
				json.add("min", new JsonPrimitive(condition.minDistance));
			if (condition.maxDistance != Double.POSITIVE_INFINITY)
				json.add("max", new JsonPrimitive(condition.maxDistance));
		}
		
		@Override
		public @NotNull OriginDistanceLootCondition deserialize(
		  @NotNull JsonObject json, @NotNull JsonDeserializationContext deserializationContext
		) {
			double minDistance = 0;
			double maxDistance = Double.POSITIVE_INFINITY;
			if (json.has("min")) {
				final JsonElement min = json.get("min");
				if (!min.isJsonPrimitive() || !min.getAsJsonPrimitive().isNumber())
					throw new JsonSyntaxException("'min' distance must be a number");
				minDistance = min.getAsDouble();
			}
			if (json.has("max")) {
				final JsonElement max = json.get("max");
				if (!max.isJsonPrimitive() || !max.getAsJsonPrimitive().isNumber())
					throw new JsonSyntaxException("'max' distance must be a number");
				maxDistance = max.getAsDouble();
			}
			return new OriginDistanceLootCondition(minDistance, maxDistance);
		}
	}
}
