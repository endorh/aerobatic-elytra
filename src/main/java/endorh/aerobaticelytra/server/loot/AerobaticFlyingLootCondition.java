package endorh.aerobaticelytra.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import org.jetbrains.annotations.NotNull;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;

public class AerobaticFlyingLootCondition implements ILootCondition {
	public AerobaticFlyingLootCondition() {}
	
	@Override public @NotNull LootConditionType func_230419_b_() {
		return ModLootConditions.AEROBATIC_FLYING;
	}
	
	@Override public boolean test(LootContext lootContext) {
		final Entity entity = lootContext.get(LootParameters.THIS_ENTITY);
		if (!(entity instanceof PlayerEntity))
			return false;
		final IAerobaticData data = getAerobaticDataOrDefault((PlayerEntity) entity);
		return data.isFlying();
	}
	
	public static class Serializer implements ILootSerializer<AerobaticFlyingLootCondition> {
		@Override public void serialize(
		  @NotNull JsonObject json,
		  @NotNull AerobaticFlyingLootCondition condition,
		  @NotNull JsonSerializationContext serializationContext
		) {
			// Nothing to do
		}
		
		@Override public @NotNull AerobaticFlyingLootCondition deserialize(
		  @NotNull JsonObject json, @NotNull JsonDeserializationContext deserializationContext
		) {
			return new AerobaticFlyingLootCondition();
		}
	}
}
