package endorh.aerobaticelytra.server.loot;

import com.google.gson.*;
import net.minecraft.fluid.Fluid;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ITag.INamedTag;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Vanilla's entity property predicate {@code is_swimming} can no longer
 * detect if the player has the eyes underwater
 */
public class SubmergedLootCondition implements ILootCondition {
	protected final INamedTag<Fluid> fluidTag;
	
	public SubmergedLootCondition(INamedTag<Fluid> fluidTag) {
		this.fluidTag = fluidTag;
	}
	
	@Override public @NotNull LootConditionType func_230419_b_() {
		return AerobaticLootConditions.SUBMERGED;
	}
	
	@Override public boolean test(LootContext lootContext) {
		//noinspection ConstantConditions
		return lootContext.get(LootParameters.THIS_ENTITY).areEyesInFluid(fluidTag);
	}
	
	public static class Serializer implements ILootSerializer<SubmergedLootCondition> {
		@Override public void serialize(
		  @NotNull JsonObject json, @NotNull SubmergedLootCondition condition,
		  @NotNull JsonSerializationContext context
		) {
			json.add("fluid", new JsonPrimitive(condition.fluidTag.getName().toString()));
		}
		
		@Override public @NotNull SubmergedLootCondition deserialize(
		  @NotNull JsonObject json, @NotNull JsonDeserializationContext context
		) {
			final ResourceLocation fluid = new ResourceLocation(JSONUtils.getString(json, "fluid", "water"));
			final Optional<? extends INamedTag<Fluid>> tag = FluidTags.getAllTags().stream()
			  .filter(t -> t.getName().equals(fluid)).findFirst();
			if (!tag.isPresent())
				throw new JsonParseException("Unknown fluid tag: \"" + fluid + "\"");
			return new SubmergedLootCondition(tag.get());
		}
	}
}
