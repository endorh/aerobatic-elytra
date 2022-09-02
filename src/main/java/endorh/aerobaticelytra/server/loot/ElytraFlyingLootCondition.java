package endorh.aerobaticelytra.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

public class ElytraFlyingLootCondition implements LootItemCondition {
	public ElytraFlyingLootCondition() {}
	
	@Override public @NotNull LootItemConditionType getType() {
		return AerobaticLootConditions.ELYTRA_FLYING;
	}
	
	@Override public boolean test(LootContext lootContext) {
		final Entity entity = lootContext.getParamOrNull(LootContextParams.THIS_ENTITY);
		return entity instanceof LivingEntity && ((LivingEntity) entity).isFallFlying();
	}
	
	public static class ConditionSerializer implements Serializer<ElytraFlyingLootCondition> {
		@Override public void serialize(
		  @NotNull JsonObject json,
		  @NotNull ElytraFlyingLootCondition condition,
		  @NotNull JsonSerializationContext serializationContext
		) {
			// Nothing to do
		}
		
		@Override public @NotNull ElytraFlyingLootCondition deserialize(
		  @NotNull JsonObject json, @NotNull JsonDeserializationContext deserializationContext
		) {
			return new ElytraFlyingLootCondition();
		}
	}
}
