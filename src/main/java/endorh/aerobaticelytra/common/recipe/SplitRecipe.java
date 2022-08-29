package endorh.aerobaticelytra.common.recipe;

import com.google.gson.*;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.util.network.PacketBufferUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Splits an Aerobatic Elytra in two wings, preserving their
 * colors/patterns/trails/abilities.
 */
public class SplitRecipe extends CustomRecipe {
	public static int MAX_WIDTH = 3;
	public static int MAX_HEIGHT = 3;
	
	public static final String TAG_SPLIT_ELYTRA = "SplitElytra";
	public static final String TAG_SPLIT_ELYTRA_CAPS = "SplitElytraCaps";
	
	public static final Serializer SERIALIZER = new Serializer();
	
	protected final NonNullList<Ingredient> recipeItems;
	public final NonNullList<Pair<Ingredient, LeaveData>> ingredients;
	
	/**
	 * Contains the leave behaviour for an ingredient
	 */
	public static class LeaveData {
		public final boolean leave;
		public final int damage;
		public static final LeaveData DO_NOT_LEAVE = new LeaveData();
		
		public LeaveData() {
			this(false);
		}
		
		public LeaveData(boolean leave) {
			this(leave, 0);
		}
		
		public LeaveData(boolean leave, int damage) {
			this.leave = leave;
			this.damage = damage;
		}
		
		public void write(FriendlyByteBuf buf) {
			buf.writeBoolean(leave);
			if (leave)
				buf.writeVarInt(damage);
		}
		
		public static LeaveData read(FriendlyByteBuf buf) {
			boolean leave = buf.readBoolean();
			int damage = leave? buf.readVarInt() : 0;
			return new LeaveData(leave, damage);
		}
		
		public static LeaveData from(JsonObject obj) {
			return new LeaveData(
			  GsonHelper.getAsBoolean(obj, "leave", false),
			  GsonHelper.getAsInt(obj, "damage", 0));
		}
	}
	
	public SplitRecipe(
	  ResourceLocation idIn,
	  NonNullList<Pair<Ingredient, LeaveData>> recipeItems
	) {
		super(idIn);
		this.recipeItems = addElytra(recipeItems);
		ingredients = recipeItems;
		ItemStack elytra = new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA, 1);
		if (recipeItems.stream().anyMatch(p -> p.getLeft().test(elytra)))
			throw new JsonSyntaxException(
			  "An AerobaticElytraSplitRecipe cannot contain any aerobatic elytra ingredient");
	}
	
	private static NonNullList<Ingredient> addElytra(
	  NonNullList<Pair<Ingredient, LeaveData>> ingredients
	) {
		final NonNullList<Ingredient> res = NonNullList.withSize(ingredients.size() + 1, Ingredient.EMPTY);
		res.set(0, Ingredient.of(AerobaticElytraItems.AEROBATIC_ELYTRA));
		for (int i = 0, s = ingredients.size(); i < s; i++)
			res.set(i + 1, ingredients.get(i).getLeft());
		return res;
	}
	
	@Override public boolean matches(
	  @NotNull CraftingContainer inv, @NotNull Level world
	) {
		ItemStack elytra = ItemStack.EMPTY;
		final List<ItemStack> inputs = new ArrayList<>();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack current = inv.getItem(i);
			if (current.isEmpty())
				continue;
			if (current.getItem() instanceof AerobaticElytraItem) {
				if (!elytra.isEmpty())
					return false;
				elytra = current;
			}
			inputs.add(current);
		}
		if (elytra.isEmpty())
			return false;
		return RecipeMatcher.findMatches(inputs, recipeItems) != null;
	}
	
	@Override
	public @NotNull ItemStack assemble(
	  @NotNull CraftingContainer inv
	) {
		ItemStack elytra = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack current = inv.getItem(i);
			if (current.getItem() instanceof AerobaticElytraItem) {
				elytra = current;
				break;
			}
		}
		assert !elytra.isEmpty();
		return getWing(elytra, WingSide.RIGHT);
	}
	
	@Override
	public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer inv) {
		NonNullList<ItemStack> rem = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
		
		List<ItemStack> inputs = new java.util.ArrayList<>();
		List<Integer> inputMap = new ArrayList<>();
		for(int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack current = inv.getItem(i);
			if (!current.isEmpty()) {
				inputs.add(current);
				inputMap.add(i);
			}
		}
		
		int[] map = RecipeMatcher.findMatches(inputs, getIngredients());
		if (map == null)
			throw new IllegalStateException("Split recipe should not have matched");
		
		int j;
		for (int i = 0; i < inputMap.size(); i++) {
			j = inputMap.get(i);
			final ItemStack stack = inv.getItem(j);
			if (stack.getItem() instanceof AerobaticElytraItem) {
				rem.set(j, getWing(stack, WingSide.LEFT));
			} else {
				final LeaveData data = ingredients.get(map[i - 1]).getRight();
				if (data.leave) {
					ItemStack left = stack.copy();
					left.setDamageValue(left.getDamageValue() + data.damage);
					rem.set(j, left);
				} else if (stack.hasCraftingRemainingItem())
					rem.set(j, stack.getCraftingRemainingItem());
			}
		}
		return rem;
	}
	
	public static ItemStack getWing(ItemStack elytra, WingSide side) {
		assert elytra.getItem() instanceof AerobaticElytraItem;
		return ((AerobaticElytraItem) elytra.getItem()).getWing(elytra, side);
	}
	
	@Override public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}
	
	@Override public @NotNull NonNullList<Ingredient> getIngredients() {
		return recipeItems;
	}
	
	public static class Serializer extends SimpleRecipeSerializer<SplitRecipe> {
		public Serializer() {
			super(id -> null);
		}
		
		@Override
		public @NotNull SplitRecipe fromJson(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			NonNullList<Pair<Ingredient, LeaveData>> list =
			  readIngredients(GsonHelper.getAsJsonArray(json, "ingredients"));
			if (list.size() > MAX_WIDTH * MAX_HEIGHT - 1) {
				throw new JsonParseException("Too many ingredients for split recipe, the max is " + (SplitRecipe.MAX_WIDTH * SplitRecipe.MAX_HEIGHT - 1));
			} else {
				ItemStack elytra = new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA);
				if (list.stream().anyMatch(p -> p.getLeft().test(elytra)))
					throw new JsonParseException(
					  "Aerobatic elytra split recipes cannot contain any aerobatic elytra ingredient");
				return new SplitRecipe(recipeId, list);
			}
		}
		
		public static NonNullList<Pair<Ingredient, LeaveData>> readIngredients(JsonArray arr) {
			NonNullList<Pair<Ingredient, LeaveData>> list = NonNullList.create();
			for (JsonElement elem : arr) {
				if (elem.isJsonObject()) {
					JsonObject obj = elem.getAsJsonObject();
					if (!obj.has("ingredient")) {
						list.add(Pair.of(
						  Ingredient.fromValues(Stream.of(
						    Ingredient.valueFromJson(obj))),
						  LeaveData.from(obj)));
					} else {
						JsonElement ing = obj.get("ingredient");
						LeaveData data = LeaveData.from(obj);
						if (ing.isJsonObject()) {
							list.add(Pair.of(
							  Ingredient.fromValues(Stream.of(
							    Ingredient.valueFromJson(ing.getAsJsonObject())
							  )), data));
						} else if (ing.isJsonArray()) {
							list.add(Pair.of(ingredientFromJsonArray(ing.getAsJsonArray()), data));
						} else {
							throw new JsonSyntaxException(
							  "Expected item to be object or array of objects");
						}
					}
				} else if (elem.isJsonArray()) {
					list.add(Pair.of(ingredientFromJsonArray(elem.getAsJsonArray()),
					                 LeaveData.DO_NOT_LEAVE));
				} else {
					throw new JsonSyntaxException(
					  "Expected item to be object or array of objects");
				}
			}
			return list;
		}
		
		public static Ingredient ingredientFromJsonArray(JsonArray arr) {
			if (arr.size() == 0) {
				throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
			} else {
				return Ingredient.fromValues(
				  StreamSupport.stream(arr.spliterator(), false).map(
				    (element) -> Ingredient.valueFromJson(GsonHelper.convertToJsonObject(element, "item"))));
			}
		}
		
		@Override
		public SplitRecipe fromNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
			NonNullList<Pair<Ingredient, LeaveData>> ingredients =
			  PacketBufferUtil.readNonNullList(
			    buf, b -> Pair.of(Ingredient.fromNetwork(b), LeaveData.read(b)),
			    Pair.of(Ingredient.EMPTY, LeaveData.DO_NOT_LEAVE));
			return new SplitRecipe(recipeId, ingredients);
		}
		
		@Override
		public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull SplitRecipe recipe) {
			PacketBufferUtil.writeList(
			  recipe.ingredients, buf, (p, b) -> {
			  	   p.getLeft().toNetwork(b);
			  	   p.getRight().write(b);
			  });
		}
	}
	
	@Override public @NotNull RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
