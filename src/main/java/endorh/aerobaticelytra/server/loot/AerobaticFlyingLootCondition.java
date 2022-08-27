package endorh.aerobaticelytra.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;

public class AerobaticFlyingLootCondition implements LootItemCondition {
	public AerobaticFlyingLootCondition() {}
	
	@Override public @NotNull LootItemConditionType getType() {
		return ModLootConditions.AEROBATIC_FLYING;
	}
	
	@Override public boolean test(LootContext lootContext) {
		final Entity entity = lootContext.getParamOrNull(LootContextParams.THIS_ENTITY);
		if (!(entity instanceof Player))
			return false;
		final IAerobaticData data = getAerobaticDataOrDefault((Player) entity);
		return data.isFlying();
	}
	
	public static class ConditionSerializer implements Serializer<AerobaticFlyingLootCondition> {
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
