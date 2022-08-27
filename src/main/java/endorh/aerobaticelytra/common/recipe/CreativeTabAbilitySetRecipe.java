package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.IAbility;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;
import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;

/**
 * Choose what abilities should have the Aerobatic Elytras that
 * appear in the creative menu (and therefore JEI).<br>
 * The item will appear in the creative menu as many times as
 * recipes are found.
 */
public class CreativeTabAbilitySetRecipe extends CustomRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	public final ItemStack stack;
	public final String group;
	
	public CreativeTabAbilitySetRecipe(ResourceLocation idIn, String group, ItemStack stackIn) {
		super(idIn);
		this.group = group;
		this.stack = stackIn;
	}
	
	@Override public boolean matches(@NotNull CraftingContainer inv, @NotNull Level world) {
		return false;
	}
	
	@Override public @NotNull ItemStack assemble(@NotNull CraftingContainer inv) {
		return ItemStack.EMPTY;
	}
	
	@Override public boolean canCraftInDimensions(int width, int height) {
		return false;
	}
	
	public static class Serializer extends SimpleRecipeSerializer<CreativeTabAbilitySetRecipe> {
		public static final ResourceLocation NAME = prefix("creative_tab_ability_set");
		
		public Serializer() {
			super(id -> null);
			setRegistryName(NAME);
		}
		
		@Override public @NotNull CreativeTabAbilitySetRecipe fromJson(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			String group = GsonHelper.getAsString(json, "group");
			String itemName = GsonHelper.getAsString(json, "item");
			//noinspection deprecation
			ItemStack stack = new ItemStack(
			  Registry.ITEM.getOptional(new ResourceLocation(itemName)).orElseThrow(
				 () -> new JsonSyntaxException("Unknown item '" + itemName + "'")));
			final Pair<Map<IAbility, Float>, Map<String, Float>> pair =
			  AbilityNBTInheritingShapedRecipe.Serializer.abilitiesFromJson(
				 GsonHelper.getAsJsonObject(json, "abilities"));
			IElytraSpec spec = getElytraSpecOrDefault(stack);
			final Map<String, Float> specUnknown = spec.getUnknownAbilities();
			spec.setAbilities(pair.getLeft());
			specUnknown.clear();
			specUnknown.putAll(pair.getRight());
			return new CreativeTabAbilitySetRecipe(recipeId, group, stack);
		}
		
		@Override
		public CreativeTabAbilitySetRecipe fromNetwork(
		  @NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf
		) {
			String group = buf.readUtf();
			ItemStack stack = buf.readItem();
			return new CreativeTabAbilitySetRecipe(recipeId, group, stack);
		}
		
		@Override
		public void toNetwork(
		  @NotNull FriendlyByteBuf buf, @NotNull CreativeTabAbilitySetRecipe recipe
		) {
			buf.writeUtf(recipe.group);
			buf.writeItem(recipe.stack);
		}
	}
	
	@Override public @NotNull RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
