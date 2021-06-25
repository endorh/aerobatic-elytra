package dnj.aerobatic_elytra.server.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import org.jetbrains.annotations.NotNull;

public class ElytraFlyingLootCondition implements ILootCondition {
	public ElytraFlyingLootCondition() {}
	
	@Override public @NotNull LootConditionType func_230419_b_() {
		return ModLootConditions.ELYTRA_FLYING;
	}
	
	@Override public boolean test(LootContext lootContext) {
		final Entity entity = lootContext.get(LootParameters.THIS_ENTITY);
		return entity instanceof LivingEntity && ((LivingEntity) entity).isElytraFlying();
	}
	
	public static class Serializer implements ILootSerializer<ElytraFlyingLootCondition> {
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
