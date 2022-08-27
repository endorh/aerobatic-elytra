package endorh.aerobaticelytra.server.loot;

import com.google.gson.*;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.IAbility;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SetAbilitiesLootFunction extends LootItemConditionalFunction {
	private final Map<IAbility, Float> abilities;
	private final Map<String, Float> unknown;
	
	protected SetAbilitiesLootFunction(
	  LootItemCondition[] conditionsIn, Map<IAbility, Float> abilities, Map<String, Float> unknown
	) {
		super(conditionsIn);
		this.abilities = abilities;
		this.unknown = unknown;
	}
	
	@Override
	protected @NotNull ItemStack run(@NotNull ItemStack stack, @NotNull LootContext context) {
		final IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(stack);
		spec.putAbilities(abilities);
		spec.getUnknownAbilities().putAll(unknown);
		return stack;
	}
	
	@Override public @NotNull LootItemFunctionType getType() {
		return ModLootFunctions.SET_ABILITIES;
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder extends LootItemConditionalFunction.Builder<SetAbilitiesLootFunction.Builder> {
		private final Map<IAbility, Float> abilities = new HashMap<>();
		private final Map<String, Float> unknown = new HashMap<>();
		
		@Override protected @NotNull Builder getThis() {
			return this;
		}
		
		public void setAbility(String name, double value) {
			if (IAbility.isDefined(name)) {
				IAbility ability = IAbility.fromName(name);
				abilities.put(ability, (float) value);
			} else unknown.put(name, (float) value);
		}
		
		public @NotNull LootItemFunction build() {
			return new SetAbilitiesLootFunction(getConditions(), abilities, unknown);
		}
	}
	
	public static class Serializer extends LootItemConditionalFunction.Serializer<SetAbilitiesLootFunction> {
		
		@Override public @NotNull SetAbilitiesLootFunction deserialize(
		  @NotNull JsonObject json,
		  @NotNull JsonDeserializationContext deserializationContext,
		  LootItemCondition @NotNull [] conditionsIn
		) {
			final Map<IAbility, Float> abilities = new HashMap<>();
			final Map<String, Float> unknown = new HashMap<>();
			
			for (Entry<String, JsonElement> entry : json.entrySet()) {
				final String name = entry.getKey();
				final JsonElement value = entry.getValue();
				if ("function".equals(name))
					continue;
				if (!value.isJsonPrimitive())
					throw new JsonSyntaxException("Ability values must be numbers");
				JsonPrimitive val = value.getAsJsonPrimitive();
				if (!val.isNumber())
					throw new JsonSyntaxException("Ability values must be numbers");
				float v = val.getAsFloat();
				if (IAbility.isDefined(name)) {
					abilities.put(IAbility.fromName(name), v);
				} else unknown.put(name, v);
			}
			
			return new SetAbilitiesLootFunction(conditionsIn, abilities, unknown);
		}
	}
}
