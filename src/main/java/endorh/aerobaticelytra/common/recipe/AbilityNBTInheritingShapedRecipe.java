package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.IAbility;
import endorh.util.network.PacketBufferUtil;
import endorh.util.recipe.NBTInheritingShapedRecipe;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static net.minecraft.util.GsonHelper.getAsJsonArray;
import static net.minecraft.util.GsonHelper.getAsJsonObject;

public class AbilityNBTInheritingShapedRecipe extends NBTInheritingShapedRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	public final Map<IAbility, Float> abilities;
	public final Map<String, Float> unknown;
	
	public AbilityNBTInheritingShapedRecipe(
	  ResourceLocation id, String group, int width, int height,
	  NonNullList<int[]> nbtSourcesIn, NonNullList<Ingredient> items,
	  ItemStack output, CompoundTag outputTagIn,
	  Map<IAbility, Float> upgradesIn, Map<String, Float> unknownIn
	) {
		super(id, group, width, height, nbtSourcesIn, items, output, outputTagIn);
		abilities = upgradesIn;
		unknown = unknownIn;
	}
	
	@NotNull @Override
	public ItemStack assemble(@NotNull CraftingContainer inv) {
		ItemStack result = super.assemble(inv);
		final IElytraSpec spec = getElytraSpecOrDefault(result);
		spec.setAbilities(abilities);
		spec.getUnknownAbilities().clear();
		spec.getUnknownAbilities().putAll(unknown);
		return result;
	}
	
	public static class Serializer extends ForgeRegistryEntry<RecipeSerializer<?>>
	  implements RecipeSerializer<AbilityNBTInheritingShapedRecipe> {
		
		private static final ResourceLocation NAME = new ResourceLocation(
		  AerobaticElytra.MOD_ID, "ability_nbt_inheriting_shaped_recipe");
		private static final Logger LOGGER = LogManager.getLogger();
		
		Serializer() {
			setRegistryName(NAME);
		}
		
		@NotNull @Override public AbilityNBTInheritingShapedRecipe fromJson(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			String group = GsonHelper.getAsString(json, "group", "");
			boolean allowUnknown = GsonHelper.getAsBoolean(json, "allow_unknown_items", false);
			Map<String, Ingredient> map = NBTInheritingShapedRecipe.Serializer
			  .deserializeKey(getAsJsonObject(json, "key"), allowUnknown);
			String[] pat = NBTInheritingShapedRecipe.Serializer.shrink(
			  NBTInheritingShapedRecipe.Serializer.patternFromJson(getAsJsonArray(json, "pattern")));
			int w = pat[0].length();
			int h = pat.length;
			NonNullList<int[]> nbtSources = NBTInheritingShapedRecipe.Serializer
			  .nbtSourcesFromPattern(pat);
			NonNullList<Ingredient> list = NBTInheritingShapedRecipe.Serializer
			  .deserializeIngredients(pat, map, w, h);
			ItemStack output = ShapedRecipe.itemStackFromJson(getAsJsonObject(json, "result"));
			CompoundTag outputTag = NBTInheritingShapedRecipe.Serializer
			  .nbtFromJson(json);
			Pair<Map<IAbility, Float>, Map<String, Float>> abilities = abilitiesFromJson(
			  getAsJsonObject(
				 getAsJsonObject(json, "result"), "abilities")
			);
			
			return new AbilityNBTInheritingShapedRecipe(
			  recipeId, group, w, h, nbtSources, list, output, outputTag,
			  abilities.getLeft(), abilities.getRight());
		}
		
		@Nullable @Override public AbilityNBTInheritingShapedRecipe fromNetwork(
		  @NotNull ResourceLocation id, @NotNull FriendlyByteBuf buf
		) {
			int w = buf.readVarInt();
			int h = buf.readVarInt();
			String group = buf.readUtf(32767);
			NonNullList<Ingredient> list = NonNullList.withSize(w * h, Ingredient.EMPTY);
			
			list.replaceAll(ignored -> Ingredient.fromNetwork(buf));
			
			ItemStack output = buf.readItem();
			
			int l = buf.readVarInt();
			NonNullList<int[]> nbtSources = NonNullList.withSize(l, new int[]{0, 0});
			for (int i = 0; i < l; i++)
				nbtSources.set(i, buf.readVarIntArray());
			
			CompoundTag outputTag = buf.readNbt();
			
			Map<IAbility, Float> upgrades = PacketBufferUtil.readMap(
			  buf, IAbility::read, FriendlyByteBuf::readFloat);
			Map<String, Float> unknown = PacketBufferUtil.readMap(
			  buf, FriendlyByteBuf::readUtf, FriendlyByteBuf::readFloat);
			
			return new AbilityNBTInheritingShapedRecipe(
			  id, group, w, h, nbtSources, list, output, outputTag, upgrades, unknown);
		}
		
		@Override public void toNetwork(
		  @NotNull FriendlyByteBuf buf, @NotNull AbilityNBTInheritingShapedRecipe recipe
		) {
			buf.writeVarInt(recipe.recipeWidth);
			buf.writeVarInt(recipe.recipeHeight);
			buf.writeUtf(recipe.getGroup());
			
			for (Ingredient ing: recipe.recipeItems)
				ing.toNetwork(buf);
			
			buf.writeItem(recipe.getResultItem());
			
			buf.writeVarInt(recipe.nbtSources.size());
			for (int[] nbtSource: recipe.nbtSources)
				buf.writeVarIntArray(nbtSource);
			
			buf.writeNbt(recipe.outputTag);
			
			PacketBufferUtil.writeMap2(
			  buf, recipe.abilities, IAbility::write, FriendlyByteBuf::writeFloat);
			PacketBufferUtil.writeMap(
			  buf, recipe.unknown, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeFloat);
		}
		
		public static Pair<Map<IAbility, Float>, Map<String, Float>> abilitiesFromJson(
		  JsonObject abilities
		) {
			Map<IAbility, Float> map = new HashMap<>();
			Map<String, Float> unknown = new HashMap<>();
			
			for (Map.Entry<String, JsonElement> entry: abilities.entrySet()) {
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
					map.put(IAbility.fromName(jsonName), value);
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
