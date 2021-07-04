package endorh.aerobatic_elytra.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.capability.IElytraSpec;
import endorh.aerobatic_elytra.common.item.IAbility;
import endorh.util.network.PacketBufferUtil;
import endorh.util.recipe.NBTInheritingShapedRecipe;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static net.minecraft.util.JSONUtils.getJsonArray;
import static net.minecraft.util.JSONUtils.getJsonObject;

public class AbilityNBTInheritingShapedRecipe extends NBTInheritingShapedRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	public final Map<IAbility, Float> abilities;
	public final Map<String, Float> unknown;
	
	public AbilityNBTInheritingShapedRecipe(
	  ResourceLocation id, String group, int width, int height,
	  NonNullList<int[]> nbtSourcesIn, NonNullList<Ingredient> items,
	  ItemStack output, CompoundNBT outputTagIn,
	  Map<IAbility, Float> upgradesIn, Map<String, Float> unknownIn
	) {
		super(id, group, width, height, nbtSourcesIn, items, output, outputTagIn);
		abilities = upgradesIn;
		unknown = unknownIn;
	}
	
	@NotNull @Override
	public ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
		ItemStack result = super.getCraftingResult(inv);
		final IElytraSpec spec = getElytraSpecOrDefault(result);
		spec.setAbilities(abilities);
		spec.getUnknownAbilities().clear();
		spec.getUnknownAbilities().putAll(unknown);
		return result;
	}
	
	public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
	  implements IRecipeSerializer<AbilityNBTInheritingShapedRecipe> {
		
		private static final ResourceLocation NAME = new ResourceLocation(
		  AerobaticElytra.MOD_ID, "ability_nbt_inheriting_shaped_recipe");
		private static final Logger LOGGER = LogManager.getLogger();
		
		Serializer() {
			setRegistryName(NAME);
		}
		
		@NotNull @Override public AbilityNBTInheritingShapedRecipe read(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			String group = JSONUtils.getString(json, "group", "");
			boolean allowUnknown = JSONUtils.getBoolean(json, "allow_unknown_items", false);
			Map<String, Ingredient> map = NBTInheritingShapedRecipe.Serializer
			  .deserializeKey(getJsonObject(json, "key"), allowUnknown);
			String[] pat = NBTInheritingShapedRecipe.Serializer.shrink(
			  NBTInheritingShapedRecipe.Serializer.patternFromJson(getJsonArray(json, "pattern")));
			int w = pat[0].length();
			int h = pat.length;
			NonNullList<int[]> nbtSources = NBTInheritingShapedRecipe.Serializer
			  .nbtSourcesFromPattern(pat);
			NonNullList<Ingredient> list = NBTInheritingShapedRecipe.Serializer
			  .deserializeIngredients(pat, map, w, h);
			ItemStack output = ShapedRecipe.deserializeItem(getJsonObject(json, "result"));
			CompoundNBT outputTag = NBTInheritingShapedRecipe.Serializer
			  .nbtFromJson(json);
			Pair<Map<IAbility, Float>, Map<String, Float>> abilities = abilitiesFromJson(
			  JSONUtils.getJsonObject(
			    JSONUtils.getJsonObject(json, "result"), "abilities")
			);
			
			return new AbilityNBTInheritingShapedRecipe(
			  recipeId, group, w, h, nbtSources, list, output, outputTag,
			  abilities.getLeft(), abilities.getRight());
		}
		
		@Nullable @Override public AbilityNBTInheritingShapedRecipe read(
		  @NotNull ResourceLocation id, @NotNull PacketBuffer buf
		) {
			int w = buf.readVarInt();
			int h = buf.readVarInt();
			String group = buf.readString(32767);
			NonNullList<Ingredient> list = NonNullList.withSize(w * h, Ingredient.EMPTY);
			
			for (int i = 0; i < list.size(); i++)
				list.set(i, Ingredient.read(buf));
			
			ItemStack output = buf.readItemStack();
			
			int l = buf.readVarInt();
			NonNullList<int[]> nbtSources = NonNullList.withSize(l, new int[] {0, 0});
			for (int i = 0; i < l; i++)
				nbtSources.set(i, buf.readVarIntArray());
			
			CompoundNBT outputTag = buf.readCompoundTag();
			
			Map<IAbility, Float> upgrades = PacketBufferUtil.readMap(
			  buf, IAbility::read, PacketBuffer::readFloat);
			Map<String, Float> unknown = PacketBufferUtil.readMap(
			  buf, PacketBufferUtil::readString, PacketBuffer::readFloat);
			
			return new AbilityNBTInheritingShapedRecipe(
			  id, group, w, h, nbtSources, list, output, outputTag, upgrades, unknown);
		}
		
		@Override public void write(
		  @NotNull PacketBuffer buf, @NotNull AbilityNBTInheritingShapedRecipe recipe
		) {
			buf.writeVarInt(recipe.recipeWidth);
			buf.writeVarInt(recipe.recipeHeight);
			buf.writeString(recipe.getGroup());
			
			for (Ingredient ing : recipe.recipeItems)
				ing.write(buf);
			
			buf.writeItemStack(recipe.getRecipeOutput());
			
			buf.writeVarInt(recipe.nbtSources.size());
			for (int[] nbtSource : recipe.nbtSources)
				buf.writeVarIntArray(nbtSource);
			
			buf.writeCompoundTag(recipe.outputTag);
			
			PacketBufferUtil.writeMap2(
			  buf, recipe.abilities, IAbility::write, PacketBuffer::writeFloat);
			PacketBufferUtil.writeMap(
			  buf, recipe.unknown, PacketBuffer::writeString, PacketBuffer::writeFloat);
		}
		
		public static Pair<Map<IAbility, Float>, Map<String, Float>> abilitiesFromJson(
		  JsonObject abilities
		) {
			Map<IAbility, Float> map = new HashMap<>();
			Map<String, Float> unknown = new HashMap<>();
			
			for (Map.Entry<String, JsonElement> entry : abilities.entrySet()) {
				final String typeError = "Ability values must be numbers";
				if (!entry.getValue().isJsonPrimitive())
					throw new JsonSyntaxException(typeError);
				JsonPrimitive val = entry.getValue().getAsJsonPrimitive();
				
				float value;
				if (val.isBoolean())
					value = val.getAsBoolean()? 1F : 0F;
				else if (val.isNumber())
					value = val.getAsFloat();
				else throw new JsonSyntaxException(typeError);
				
				final String jsonName = entry.getKey();
				if (IAbility.isDefined(jsonName)) {
					map.put(IAbility.fromJsonName(jsonName), value);
				} else {
					unknown.put(jsonName, value);
				}
			}
			if (!unknown.isEmpty()) {
				LOGGER.warn("Unknown abilities found in recipe:"
				            + unknown.keySet().stream().map(name -> "\n\t" + name)
				              .collect(Collectors.joining("")) +
				            "\nThis could be caused by missing mods or a typo in the recipes.");
			}
			return Pair.of(map, unknown);
		}
	}
}
