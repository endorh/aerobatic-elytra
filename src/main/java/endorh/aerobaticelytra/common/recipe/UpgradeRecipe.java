package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonObject;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.capability.IElytraSpec.Upgrade;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.registry.ModRegistries;
import endorh.util.recipe.RecipeManagerHelper.CachedRecipeProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static endorh.util.network.PacketBufferUtil.readList;
import static endorh.util.network.PacketBufferUtil.writeList;

public class UpgradeRecipe extends SpecialRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	protected static final CachedRecipeProvider<Collection<UpgradeRecipe>> recipeProvider =
	  new CachedRecipeProvider<Collection<UpgradeRecipe>>() {
		  @Override protected Collection<UpgradeRecipe> onReload(RecipeManager manager) {
			  return manager.getRecipes().stream().filter(
			    r -> r instanceof UpgradeRecipe && ((UpgradeRecipe) r).isValid()
			  ).map(r -> (UpgradeRecipe) r).sorted(
			    Comparator.comparing(
			      r -> r.getSelectors().stream().map(ItemSelector::toString)
			        .collect(Collectors.joining(";")))
			  ).collect(Collectors.toList());
		  }
	  };
	
	protected final List<ItemSelector> ingredients;
	protected final List<Upgrade> upgrades;
	protected final ElytraRequirement requirement;
	protected final boolean lenient;
	protected boolean valid;
	
	public static Collection<UpgradeRecipe> getUpgradeRecipes() {
		return recipeProvider.get();
		// final RecipeManager recipeManager = getCurrentServer().getRecipeManager();
		// //noinspection unchecked
		// return (List<UpgradeRecipe>) (List<?>) recipeManager.getRecipes().stream().filter(
		//   recipe -> (recipe instanceof UpgradeRecipe) && ((UpgradeRecipe) recipe).valid
		// ).collect(Collectors.toList());
	}
	
	public static Optional<UpgradeRecipe> getUpgradeRecipe(
	  ItemStack elytra, ItemStack ingredient
	) {
		return getUpgradeRecipes().stream().filter(
		  recipe -> recipe.matches(elytra, ingredient)
		).findFirst();
	}
	
	public static List<UpgradeRecipe> getUpgradeRecipes(
	  ItemStack elytra, ItemStack ingredient
	) {
		return getUpgradeRecipes().stream().filter(
		  recipe -> recipe.matches(elytra, ingredient)
		).collect(Collectors.toList());
	}
	
	/**
	 * Weak instance set, since accessing the recipe manager from a
	 * JsonReloadListener is inconvenient
	 */
	protected static final Map<ResourceLocation, WeakReference<UpgradeRecipe>> INSTANCES =
	  Collections.synchronizedMap(new WeakHashMap<>());
	private static final Logger LOGGER = LogManager.getLogger();
	
	@Internal public static void onAbilityReload() {
		synchronized (INSTANCES) {
			for (WeakReference<UpgradeRecipe> rep : INSTANCES.values()) {
				UpgradeRecipe recipe = rep.get();
				if (recipe != null)
					recipe.reloadAbilities();
			}
			if (!ModRegistries.getDatapackAbilities().isEmpty()) {
				boolean any = false;
				for (ResourceLocation id : INSTANCES.keySet()) {
					UpgradeRecipe recipe = INSTANCES.get(id).get();
					if (recipe != null && !recipe.isValid()) {
						any = true;
						LOGGER.warn(
						  "The Aerobatic Elytra upgrade recipe \"" + id + "\" " +
						  "was ignored because it uses abilities which aren't loaded");
					}
				}
				if (any) LOGGER.warn("A datapack may be missing/malformed");
			}
		}
	}
	
	public UpgradeRecipe(
	  ResourceLocation id, List<ItemSelector> ingredients, List<Upgrade> upgrades, ElytraRequirement requirement
	) { this(id, ingredients, upgrades, requirement, true); }
	
	public UpgradeRecipe(
	  ResourceLocation id, List<ItemSelector> ingredients, List<Upgrade> upgrades,
	  ElytraRequirement requirement, boolean lenient
	) {
		super(id);
		this.ingredients = ingredients;
		this.upgrades = upgrades;
		this.requirement = requirement;
		this.lenient = lenient;
		valid = !lenient;
		if (lenient) for (Upgrade upgrade : upgrades)
			valid |= upgrade.isValid();
		else for (Upgrade upgrade : upgrades)
			valid &= upgrade.isValid();
		// Add to the instance set
		INSTANCES.put(id, new WeakReference<>(this));
	}
	
	/**
	 * Perform ability parsing again<br>
	 * @return The new valid state
	 */
	@SuppressWarnings("UnusedReturnValue")
	public boolean reloadAbilities() {
		boolean res = !lenient;
		if (lenient) for (Upgrade upgrade : upgrades)
			res |= upgrade.reloadAbilities();
		else for (Upgrade upgrade : upgrades)
			res &= upgrade.reloadAbilities();
		valid = res;
		return valid;
	}
	
	/**
	 * Check if the upgrade recipe matches
	 * @param elytra Aerobatic elytra stack
	 * @param stack Upgrade ingredient stack
	 */
	public boolean matches(ItemStack elytra, ItemStack stack) {
		return valid && ItemSelector.any(ingredients, stack) && requirement.test(elytra);
	}
	
	/**
	 * Compute the result of the upgrade recipe
	 *
	 * @param elytra Aerobatic elytra stack
	 * @param maxUses Max times applied
	 * @return A pair containing the resulting {@link ItemStack} and
	 * the number of times the recipe was applied
	 */
	@NotNull public Pair<ItemStack, Integer> getResult(ItemStack elytra, int maxUses) {
		ItemStack result = elytra.copy();
		int uses;
		for (uses = 0; uses < maxUses; uses++) {
			boolean used = false;
			for (Upgrade upgrade : upgrades)
				used |= upgrade.apply(ElytraSpecCapability.getElytraSpecOrDefault(result));
			if (!used)
				break;
		}
		return Pair.of(result, uses);
	}
	
	/**
	 * Compute the result of applying the upgrade recipe once
	 */
	@NotNull public ItemStack getResult(ItemStack elytra) {
		ItemStack result = elytra.copy();
		for (Upgrade upgrade : upgrades)
			upgrade.apply(ElytraSpecCapability.getElytraSpecOrDefault(result));
		return result;
	}
	
	/**
	 * Applies a single recipe to the ingredients held by the player<br><br>
	 * Using this method is discouraged, instead use the static alternative,
	 * which accepts a collection of upgrades to be applied concurrently.
	 * @param player Player holding the ingredients
	 */
	public void apply(PlayerEntity player) {
		ItemStack elytra = player.getItemStackFromSlot(EquipmentSlotType.OFFHAND);
		ItemStack stack = player.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
		if (!matches(elytra, stack))
			return;
		final Pair<ItemStack, Integer> result = getResult(elytra, stack.getCount());
		player.setItemStackToSlot(EquipmentSlotType.OFFHAND, result.getLeft());
		if (!player.isCreative()) {
			stack.shrink(result.getRight());
		}
	}
	
	/**
	 * Apply a collection of upgrades to an elytra
	 */
	public static ItemStack apply(
	  ItemStack elytra, ItemStack upgrade, Collection<UpgradeRecipe> recipes
	) {
		while (upgrade.getCount() > 0) {
			boolean used = false;
			for (UpgradeRecipe recipe : recipes) {
				if (!recipe.matches(elytra, upgrade))
					continue;
				final Pair<ItemStack, Integer> result = recipe.getResult(elytra, 1);
				elytra = result.getLeft();
				if (result.getRight() > 0)
					used = true;
			}
			if (used)
				upgrade.shrink(1);
			else break;
		}
		return elytra;
	}
	
	/**
	 * Applies a collection of upgrades to the ingredients held by the player<br><br>
	 *
	 * Before consuming each item from the ingredient stack, all upgrades are applied,
	 * so upgrades can stack properly.
	 * @param player Player holding the ingredients
	 * @param recipes Recipe collection
	 */
	public static void apply(PlayerEntity player, Collection<UpgradeRecipe> recipes) {
		ItemStack elytra = player.getItemStackFromSlot(EquipmentSlotType.OFFHAND);
		final ItemStack stack = player.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
		int n = stack.getCount();
		final ItemStack result = apply(elytra, stack, recipes);
		player.setItemStackToSlot(EquipmentSlotType.OFFHAND, result);
		if (player.isCreative())
			stack.setCount(n);
	}
	
	@NotNull @Override public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
	
	// Forced methods
	@Deprecated @Override public boolean canFit(int width, int height) {
		return false;
	}
	@Deprecated @Override public boolean matches(@NotNull CraftingInventory inv, @NotNull World worldIn) {
		return false;
	}
	@Deprecated @NotNull @Override public ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
		return ItemStack.EMPTY;
	}
	
	public List<ItemSelector> getSelectors() {
		return ingredients;
	}
	
	@Override public @NotNull NonNullList<Ingredient> getIngredients() {
		final NonNullList<Ingredient> list = NonNullList.create();
		for (ItemSelector sel : ingredients)
			list.add(sel.similarIngredient());
		return list;
	}
	
	public List<Upgrade> getUpgrades() {
		return upgrades;
	}
	
	public ElytraRequirement getRequirement() {
		return requirement;
	}
	
	@Override
	public @NotNull ItemStack getRecipeOutput() {
		return new ItemStack(ModItems.AEROBATIC_ELYTRA);
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
	  implements IRecipeSerializer<UpgradeRecipe> {
		
		public static final ResourceLocation NAME = new ResourceLocation(
		  AerobaticElytra.MOD_ID, "upgrade_recipe");
		
		Serializer() {
			setRegistryName(NAME);
		}
		
		@NotNull @Override public UpgradeRecipe read(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			final List<ItemSelector> ing = ItemSelector.deserialize(
			  JSONUtils.getJsonArray(json, "ingredients"));
			final List<Upgrade> upgrades = IElytraSpec.Upgrade.deserialize(
			  JSONUtils.getJsonArray(json, "upgrades"));
			final ElytraRequirement req =
			  JSONUtils.hasField(json, "requirement")
			  ? ElytraRequirement.deserialize(JSONUtils.getJsonObject(json, "requirement"))
			  : ElytraRequirement.NONE;
			final boolean lenient = JSONUtils.getBoolean(json, "lenient", true);
			return new UpgradeRecipe(recipeId, ing, upgrades, req, lenient);
		}
		
		@Nullable @Override public UpgradeRecipe read(
		  @NotNull ResourceLocation id, @NotNull PacketBuffer buf
		) {
			return new UpgradeRecipe(id,
			  readList(buf, ItemSelector::read),
			  readList(buf, Upgrade::read),
			  ElytraRequirement.read(buf),
			  buf.readBoolean());
		}
		
		@Override public void write(
		  @NotNull PacketBuffer buf, @NotNull UpgradeRecipe recipe
		) {
			writeList(recipe.ingredients, buf, ItemSelector::write);
			writeList(recipe.upgrades, buf, Upgrade::write);
			recipe.requirement.write(buf);
			buf.writeBoolean(recipe.lenient);
		}
	}
	
	// Placeholder
	public static class ElytraRequirement implements Predicate<ItemStack> {
		public static final ElytraRequirement NONE = new ElytraRequirement();
		
		public ElytraRequirement() {}
		
		@Override public boolean test(ItemStack stack) {
			return AerobaticElytraLogic.isAerobaticElytra(stack);
		}
		
		public static ElytraRequirement deserialize(JsonObject json) {
			return new ElytraRequirement();
		}
		public void write(PacketBuffer buf) {}
		public static ElytraRequirement read(PacketBuffer buf) {
			return new ElytraRequirement();
		}
	}
}
