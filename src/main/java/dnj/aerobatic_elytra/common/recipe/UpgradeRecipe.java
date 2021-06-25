package dnj.aerobatic_elytra.common.recipe;

import com.google.gson.JsonObject;
import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.capability.ElytraSpecCapability;
import dnj.aerobatic_elytra.common.capability.IElytraSpec;
import dnj.aerobatic_elytra.common.capability.IElytraSpec.Upgrade;
import dnj.aerobatic_elytra.common.item.ModItems;
import dnj.aerobatic_elytra.common.registry.ModRegistries;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dnj.endor8util.network.PacketBufferUtil.readList;
import static dnj.endor8util.network.PacketBufferUtil.writeList;

// TODO: Fix this mess
public class UpgradeRecipe extends SpecialRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	private final List<ItemSelector> ingredients;
	private final List<Upgrade> upgrades;
	private final ElytraRequirement requirement;
	private final boolean lenient;
	private boolean valid;
	
	public static List<UpgradeRecipe> getUpgradeRecipes(World world) {
		final RecipeManager recipeManager = world.getRecipeManager();
		//noinspection unchecked
		return (List<UpgradeRecipe>) (List<?>) recipeManager.getRecipes().stream().filter(
		  recipe -> (recipe instanceof UpgradeRecipe) && ((UpgradeRecipe) recipe).valid
		).collect(Collectors.toList());
	}
	
	public static Optional<UpgradeRecipe> getUpgradeRecipe(
	  World world, ItemStack elytra, ItemStack ingredient
	) {
		return getUpgradeRecipes(world).stream().filter(
		  recipe -> recipe.matches(elytra, ingredient)
		).findFirst();
	}
	
	public static List<UpgradeRecipe> getUpgradeRecipes(
	  World world, ItemStack elytra, ItemStack ingredient
	) {
		return getUpgradeRecipes(world).stream().filter(
		  recipe -> recipe.matches(elytra, ingredient)
		).collect(Collectors.toList());
	}
	
	/**
	 * Weak instance set, since accessing the recipe manager from a
	 * JsonReloadListener is inconvenient
	 */
	private static final Map<ResourceLocation, WeakReference<UpgradeRecipe>> INSTANCES =
	  Collections.synchronizedMap(new WeakHashMap<>());
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void onAbilityReload() {
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
	 * @param stack Upgrade ingredient stack
	 * @return A pair containing the resulting {@link ItemStack} and
	 * the number of times the recipe was applied
	 */
	@NotNull public Pair<ItemStack, Integer> getResult(ItemStack elytra, ItemStack stack) {
		ItemStack result = elytra.copy();
		int uses;
		for (uses = 0; uses < stack.getCount(); uses++) {
			boolean used = false;
			for (Upgrade upgrade : upgrades)
				used |= upgrade.apply(ElytraSpecCapability.getElytraSpecOrDefault(result));
			if (!used)
				break;
		}
		return Pair.of(result, uses);
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
		final Pair<ItemStack, Integer> result = getResult(elytra, stack);
		player.setItemStackToSlot(EquipmentSlotType.OFFHAND, result.getLeft());
		if (!player.isCreative()) {
			stack.shrink(result.getRight());
		}
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
		final ItemStack single = stack.copy();
		single.setCount(1);
		int n = stack.getCount();
		while (stack.getCount() > 0) {
			boolean used = false;
			for (UpgradeRecipe recipe : recipes) {
				if (!recipe.matches(elytra, stack))
					continue;
				final Pair<ItemStack, Integer> result = recipe.getResult(elytra, single);
				elytra = result.getLeft();
				if (result.getRight() > 0)
					used = true;
			}
			if (used)
				stack.shrink(1);
			else break;
		}
		player.setItemStackToSlot(EquipmentSlotType.OFFHAND, elytra);
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
		for (ItemSelector sel : ingredients) {
			list.add(sel.similarIngredient());
		}
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
		
		private static final ResourceLocation NAME = new ResourceLocation(
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
	
	// Placeholder for future extension
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
