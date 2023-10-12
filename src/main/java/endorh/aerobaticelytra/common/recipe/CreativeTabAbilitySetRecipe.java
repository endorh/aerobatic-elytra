package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.item.IAbility;
import endorh.util.nbt.JsonToNBTUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;

/**
 * Choose what abilities should have the Aerobatic Elytras that
 * appear in the creative menu (and therefore JEI).<br>
 * The item will appear in the creative menu as many times as
 * recipes are found.<br>
 * <br>
 * The natural comparison order of this class is inconsistent with equality,
 * as it's used to sort ability sets based on their {@link #order}.
 */
public class CreativeTabAbilitySetRecipe extends CustomRecipe implements Comparable<CreativeTabAbilitySetRecipe> {
	public static final Serializer SERIALIZER = new Serializer();

	public final ItemStack stack;
	public final String group;
	public final TabVisibility visibility;
	public final int order;

	public CreativeTabAbilitySetRecipe(ResourceLocation id, String group, CraftingBookCategory category, ItemStack stackIn, TabVisibility visibility, int order) {
		super(id, category);
		this.group = group;
		stack = stackIn;
		this.visibility = visibility;
		this.order = order;
	}

	public boolean matchesTab(CreativeModeTab tab) {
		String name = getTabName(tab);
		if (name.startsWith("itemGroup.") && !group.startsWith("itemGroup."))
			return name.equals("itemGroup." + group);
		return name.equals(group);
	}

	private static String getTabName(CreativeModeTab tab) {
		Component name = tab.getDisplayName();
		if (name.getContents() instanceof TranslatableContents tc)
			return tc.getKey();
		return name.getString();
	}

	public ItemStack getElytraStack() {
		return stack;
	}

	public ItemStack getWingStack() {
		ItemStack wing = new ItemStack(AerobaticElytraItems.AEROBATIC_ELYTRA_WING);
		CompoundTag tag = stack.getTag();
		if (tag != null)
			wing.setTag(tag.copy());
		return wing;
	}

	public TabVisibility getVisibility() {
		return visibility;
	}

	public int getOrder() {
		return order;
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
	
	public static class Serializer extends SimpleCraftingRecipeSerializer<CreativeTabAbilitySetRecipe> {
		public Serializer() {
			super((id, cat) -> null);
		}
		
		@Override public @NotNull CreativeTabAbilitySetRecipe fromJson(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", (String) null), CraftingBookCategory.MISC);
			String group = GsonHelper.getAsString(json, "group");
			String itemName = GsonHelper.getAsString(json, "item");
			ItemStack stack = new ItemStack(
			  BuiltInRegistries.ITEM.getOptional(new ResourceLocation(itemName)).orElseThrow(
				 () -> new JsonSyntaxException("Unknown item '" + itemName + "'")));
			final Pair<Map<IAbility, Float>, Map<String, Float>> pair =
			  AbilityNBTInheritingShapedRecipe.Serializer.abilitiesFromJson(
				 GsonHelper.getAsJsonObject(json, "abilities"));
			if (json.has("tag")) {
				CompoundTag tag = JsonToNBTUtil.getTagFromJson(json.getAsJsonObject("tag"));
				stack.setTag(tag);
			}
			IElytraSpec spec = getElytraSpecOrDefault(stack);
			final Map<String, Float> specUnknown = spec.getUnknownAbilities();
			spec.setAbilities(pair.getLeft());
			specUnknown.clear();
			specUnknown.putAll(pair.getRight());
			String visibilityName = GsonHelper.getAsString(json, "visibility", "parent_and_search_tabs");
			TabVisibility visibility;
			try {
				visibility = TabVisibility.valueOf(visibilityName.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new JsonSyntaxException("Unknown visibility type '" + visibilityName + "' (Must be one of " +
					Arrays.stream(TabVisibility.values()).map(TabVisibility::name)
						.map(String::toLowerCase).collect(Collectors.joining(", ")) + ")");
			}
			int order = GsonHelper.getAsInt(json, "order", 0);
			return new CreativeTabAbilitySetRecipe(recipeId, group, category, stack, visibility, order);
		}
		
		@Override
		public CreativeTabAbilitySetRecipe fromNetwork(
		  @NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf
		) {
			CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
			String group = buf.readUtf();
			ItemStack stack = buf.readItem();
			TabVisibility visibility = buf.readEnum(TabVisibility.class);
			int order = buf.readVarInt();
			return new CreativeTabAbilitySetRecipe(recipeId, group, category, stack, visibility, order);
		}
		
		@Override
		public void toNetwork(
		  @NotNull FriendlyByteBuf buf, @NotNull CreativeTabAbilitySetRecipe recipe
		) {
			buf.writeEnum(recipe.category());
			buf.writeUtf(recipe.group);
			buf.writeItem(recipe.stack);
			buf.writeEnum(recipe.visibility);
			buf.writeVarInt(recipe.order);
		}
	}
	
	@Override public @NotNull RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	@Override public int compareTo(@NotNull CreativeTabAbilitySetRecipe other) {
		return Integer.compare(order, other.order);
	}
}
