package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Applies a banner to an Aerobatic Elytra, or a single wing
 */
public class BannerRecipe extends CustomRecipe {
	
	public BannerRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}
	
	@Override
	public boolean matches(CraftingContainer inv, @NotNull Level world) {
		boolean foundElytra = false;
		boolean foundBanner = false;
		
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack current = inv.getItem(i);
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
	public ItemStack assemble(CraftingContainer inv) {
		ItemStack elytra = ItemStack.EMPTY;
		ItemStack banner = ItemStack.EMPTY;
		
		for (int i = 0; i < inv.getContainerSize(); ++i) {
			ItemStack current = inv.getItem(i);
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
		CompoundTag source = banner.getTagElement("BlockEntityTag");
		CompoundTag tag = source == null ? new CompoundTag() : source.copy();
		
		tag.putInt("Base", ((BannerItem) banner.getItem()).getColor().getId());
		result.addTagElement("BlockEntityTag", tag);
		
		// Remove dye if it has one
		result.removeTagKey("display");
		return result;
	}
	
	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}
	
	@NotNull @Override
	public RecipeSerializer<?> getSerializer() {
		return AerobaticRecipes.BANNER_RECIPE.get();
	}
}
