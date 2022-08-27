package endorh.aerobaticelytra.server.loot;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.Tag;
import net.minecraft.tags.Tag.Named;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.jetbrains.annotations.NotNull;

/**
 * Vanilla's entity property predicate {@code is_swimming} can no longer
 * detect if the player has the eyes underwater
 */
public class SubmergedLootCondition implements LootItemCondition {
	protected final Named<Fluid> fluidTag;
	
	public SubmergedLootCondition(Named<Fluid> fluidTag) {
		this.fluidTag = fluidTag;
	}
	
	@Override public @NotNull LootItemConditionType getType() {
		return ModLootConditions.SUBMERGED;
	}
	
	@Override public boolean test(LootContext lootContext) {
		//noinspection ConstantConditions
		return lootContext.getParamOrNull(LootContextParams.THIS_ENTITY).isEyeInFluid(fluidTag);
	}
	
	public static class ConditionSerializer implements Serializer<SubmergedLootCondition> {
		@Override public void serialize(
		  @NotNull JsonObject json, @NotNull SubmergedLootCondition condition,
		  @NotNull JsonSerializationContext context
		) {
			json.add("fluid", new JsonPrimitive(condition.fluidTag.getName().toString()));
		}
		
		@Override public @NotNull SubmergedLootCondition deserialize(
		  @NotNull JsonObject json, @NotNull JsonDeserializationContext context
		) {
			final ResourceLocation fluid =
			  new ResourceLocation(GsonHelper.getAsString(json, "fluid", "water"));
			final Tag<Fluid> fluidTag = FluidTags.getAllTags().getTag(fluid);
			if (!(fluidTag instanceof Tag.Named<Fluid>)) {
				throw new JsonParseException("Unknown fluid tag: \"" + fluid + "\"");
			} else {return new SubmergedLootCondition((Named<Fluid>) fluidTag);}
		}
	}
}
