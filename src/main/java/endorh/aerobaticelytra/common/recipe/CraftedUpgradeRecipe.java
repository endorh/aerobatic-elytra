package endorh.aerobaticelytra.common.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.common.AerobaticElytraLogic.isAerobaticElytra;

/**
 * Allows applying upgrade recipes on crafting table interfaces,
 * which makes automation possible through other mods
 */
public class CraftedUpgradeRecipe extends SpecialRecipe {
	private Collection<UpgradeRecipe> recipes = new ArrayList<>();
	
	public CraftedUpgradeRecipe(ResourceLocation idIn) {
		super(idIn);
	}
	
	/**
	 * Match a single item above an aerobatic elytra item
	 */
	@Override public boolean matches(
	  @NotNull CraftingInventory inv, @NotNull World world
	) {
		ItemStack upgrade = ItemStack.EMPTY, elytra = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack cur = inv.getItem(i);
			if (cur.isEmpty())
				continue;
			upgrade = cur;
			int j;
			for (j = 1; j < inv.getWidth() && i + j < inv.getContainerSize(); j++) {
				if (!inv.getItem(i + j).isEmpty())
					return false;
			}
			if (i + j < inv.getContainerSize()) {
				elytra = inv.getItem(i + j);
				if (!isAerobaticElytra(elytra))
					return false;
			}
			for (i += j + 1; i < inv.getContainerSize(); i++)
				if (!inv.getItem(i).isEmpty())
					return false;
		}
		if (upgrade.isEmpty() || elytra.isEmpty())
			return false;
		recipes = UpgradeRecipe.getUpgradeRecipes();
		return !UpgradeRecipe.getUpgradeRecipes(elytra, upgrade).isEmpty();
	}
	
	@Override public @NotNull ItemStack assemble(
	  @NotNull CraftingInventory inv
	) {
		ItemStack upgrade = ItemStack.EMPTY, elytra = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack cur = inv.getItem(i);
			if (cur.isEmpty())
				continue;
			upgrade = cur;
			elytra = inv.getItem(i + inv.getWidth());
			break;
		}
		final ItemStack elytraStack = elytra;
		final ItemStack upgradeStack = upgrade.copy();
		upgradeStack.setCount(1);
		return UpgradeRecipe.apply(elytra, upgradeStack, recipes.stream().filter(
		  r -> r.matches(elytraStack, upgradeStack)
		).collect(Collectors.toList()));
	}
	
	@Override public boolean canCraftInDimensions(int width, int height) {
		return width >= 1 && height >= 2;
	}
	
	@Override public @NotNull IRecipeSerializer<?> getSerializer() {
		return ModRecipes.CRAFTED_UPGRADE_RECIPE.get();
	}
}
