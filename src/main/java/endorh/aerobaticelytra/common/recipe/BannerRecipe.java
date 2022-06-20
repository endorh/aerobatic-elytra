package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

/**
 * Applies a banner to an Aerobatic Elytra, or a single wing
 */
public class BannerRecipe extends SpecialRecipe {
	
	public BannerRecipe(ResourceLocation id) {
		super(id);
	}
	
	@Override
	public boolean matches(CraftingInventory inv, @NotNull World world) {
		boolean foundElytra = false;
		boolean foundBanner = false;
		
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack current = inv.getStackInSlot(i);
			if (current.isEmpty())
				continue;
			final Item item = current.getItem();
			if (item instanceof BannerItem) {
				if (foundBanner)
					return false;
				foundBanner = true;
			} else if (item instanceof AerobaticElytraItem
			           || item instanceof AerobaticElytraWingItem) {
				if (foundElytra)
					return false;
				foundElytra = true;
			}
		}
		return foundElytra && foundBanner;
	}
	
	@NotNull @Override
	public ItemStack getCraftingResult(CraftingInventory inv) {
		ItemStack elytra = ItemStack.EMPTY;
		ItemStack banner = ItemStack.EMPTY;
		
		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			ItemStack current = inv.getStackInSlot(i);
			if (current.isEmpty())
				continue;
			final Item item = current.getItem();
			if (item instanceof BannerItem) {
				banner = current.copy();
			} else if (item instanceof AerobaticElytraItem
			           || item instanceof AerobaticElytraWingItem) {
				elytra = current;
			}
		}
		
		assert !elytra.isEmpty() && !banner.isEmpty();
		
		return apply(elytra, banner);
	}
	
	public static ItemStack apply(ItemStack elytra, ItemStack banner) {
		ItemStack result = elytra.copy();
		result.setCount(1);
		CompoundNBT source = banner.getChildTag("BlockEntityTag");
		CompoundNBT tag = source == null ? new CompoundNBT() : source.copy();
		
		tag.putInt("Base", ((BannerItem) banner.getItem()).getColor().getId());
		result.setTagInfo("BlockEntityTag", tag);
		
		// Remove dye if it has one
		result.removeChildTag("display");
		return result;
	}
	
	@Override
	public boolean canFit(int width, int height) {
		return width * height >= 2;
	}
	
	@NotNull @Override
	public IRecipeSerializer<?> getSerializer() {
		return ModRecipes.BANNER_RECIPE.get();
	}
}
