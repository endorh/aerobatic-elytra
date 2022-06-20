package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.IAbility;
import endorh.util.network.PacketBufferUtil;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
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
public class CreativeTabAbilitySetRecipe extends SpecialRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	public final ItemStack stack;
	public final String group;
	
	public CreativeTabAbilitySetRecipe(ResourceLocation idIn, String group, ItemStack stackIn) {
		super(idIn);
		this.group = group;
		this.stack = stackIn;
	}
	
	@Override public boolean matches(@NotNull CraftingInventory inv, @NotNull World world) {
		return false;
	}
	@Override public @NotNull ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
		return ItemStack.EMPTY;
	}
	@Override public boolean canFit(int width, int height) {
		return false;
	}
	
	public static class Serializer extends SpecialRecipeSerializer<CreativeTabAbilitySetRecipe> {
		public static final ResourceLocation NAME = prefix("creative_tab_ability_set");
		public Serializer() {
			super(id -> null);
			setRegistryName(NAME);
		}
		
		@Override public @NotNull CreativeTabAbilitySetRecipe read(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			String group = JSONUtils.getString(json, "group");
			String itemName = JSONUtils.getString(json, "item");
			//noinspection deprecation
			ItemStack stack = new ItemStack(
			  Registry.ITEM.getOptional(new ResourceLocation(itemName)).orElseThrow(
				 () -> new JsonSyntaxException("Unknown item '" + itemName + "'")));
			final Pair<Map<IAbility, Float>, Map<String, Float>> pair = AbilityNBTInheritingShapedRecipe.Serializer.abilitiesFromJson(
			  JSONUtils.getJsonObject(json, "abilities"));
			IElytraSpec spec = getElytraSpecOrDefault(stack);
			final Map<String, Float> specUnknown = spec.getUnknownAbilities();
			spec.setAbilities(pair.getLeft());
			specUnknown.clear();
			specUnknown.putAll(pair.getRight());
			return new CreativeTabAbilitySetRecipe(recipeId, group, stack);
		}
		
		@Override
		public CreativeTabAbilitySetRecipe read(@NotNull ResourceLocation recipeId, @NotNull PacketBuffer buf) {
			String group = PacketBufferUtil.readString(buf);
			ItemStack stack = buf.readItemStack();
			return new CreativeTabAbilitySetRecipe(recipeId, group, stack);
		}
		
		@Override
		public void write(@NotNull PacketBuffer buf, @NotNull CreativeTabAbilitySetRecipe recipe) {
			buf.writeString(recipe.group);
			buf.writeItemStack(recipe.stack);
		}
	}
	
	@Override public @NotNull IRecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
