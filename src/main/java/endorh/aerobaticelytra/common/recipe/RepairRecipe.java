package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RepairRecipe extends CustomRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	public static List<RepairRecipe> getRepairRecipes() {
		final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return server.getRecipeManager().getRecipes().stream().map(
			  r -> r instanceof RepairRecipe? (RepairRecipe) r : null
			).filter(Objects::nonNull).collect(Collectors.toList());
		} else return Collections.emptyList();
	}
	
	public final Ingredient ingredient;
	public final int amount; // Currently unused
	
	public RepairRecipe(ResourceLocation id, CraftingBookCategory category, Ingredient ingredient, int amount) {
		super(id, category);
		this.ingredient = ingredient;
		this.amount = amount;
	}
	
	public boolean matches(ItemStack stack) {
		return ingredient.test(stack);
	}
	
	@Override public boolean matches(@NotNull CraftingContainer inv, @NotNull Level world) {
		return false;
	}
	@Override public @NotNull ItemStack assemble(@NotNull CraftingContainer inv, @NotNull RegistryAccess r) {
		return ItemStack.EMPTY;
	}
	@Override public boolean canCraftInDimensions(int width, int height) {
		return false;
	}
	
	@Override public @NotNull RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
	
	public static class Serializer implements RecipeSerializer<RepairRecipe> {
		@Override public @NotNull RepairRecipe fromJson(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
			  GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
			final Ingredient ingredient = Ingredient.fromJson(
			  GsonHelper.getAsJsonObject(json, "ingredient"));
			final int amount = GsonHelper.getAsInt(json, "amount", 0); // Unused
			return new RepairRecipe(recipeId, category, ingredient, amount);
		}
		
		@Nullable @Override public RepairRecipe fromNetwork(
		  @NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf
		) {
			return new RepairRecipe(recipeId, buf.readEnum(CraftingBookCategory.class), Ingredient.fromNetwork(buf), buf.readVarInt());
		}
		
		@Override public void toNetwork(
		  @NotNull FriendlyByteBuf buf, @NotNull RepairRecipe recipe
		) {
			recipe.ingredient.toNetwork(buf);
			buf.writeVarInt(recipe.amount);
		}
	}
}
