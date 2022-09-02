package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.client.trail.AerobaticTrail.RocketSide;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.capability.IElytraSpec.RocketStar;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Modifies the trail of any of the 4 rockets an Aerobatic Elytra
 * has, depending on the relative position of the rocket and
 * the elytra in the crafting bench.<br>
 * Using a rocket without explosion removes the trail for that
 * rocket.<br>
 * All rockets may be modified at the same time.
 */
public class TrailRecipe extends CustomRecipe {
	public TrailRecipe(ResourceLocation id) {
		super(id);
	}
	
	@Override
	public boolean matches(@NotNull CraftingContainer inv, @NotNull Level worldIn) {
		ItemStack elytra = null;
		int e = 0;
		int rockets = 0;
		for (int k = 0; k < inv.getContainerSize(); k++) {
			ItemStack item = inv.getItem(k);
			if (item.isEmpty())
				continue;
			if (AerobaticElytraLogic.isAerobaticElytra(item)) {
				elytra = item;
				e = k;
			}
			else if (item.getItem() != Items.FIREWORK_ROCKET)
				return false;
			else rockets++;
		}
		if (elytra == null)
			return false;
		int w = inv.getWidth();
		int h = inv.getHeight();
		int i = e / w;
		int j = e % w;
		
		int count = 0;
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				if (x == j - 1 || x == j + 1) {
					if (y == i || y == i + 1) {
						if (!inv.getItem(y * w + x).isEmpty())
							count++;
						continue;
					}
				}
				if (x == j && y == i)
					continue;
				if (!inv.getItem(y * w + x).isEmpty())
					return false;
			}
		}
		
		return count > 0;
	}
	
	@NotNull @Override
	public ItemStack assemble(@NotNull CraftingContainer inv) {
		ItemStack elytra = ItemStack.EMPTY;
		int k;
		for (k = 0; k < inv.getContainerSize(); k++) {
			ItemStack item = inv.getItem(k);
			if (item.isEmpty())
				continue;
			if (AerobaticElytraLogic.isAerobaticElytra(item)) {
				elytra = item;
				break;
			}
		}
		assert !elytra.isEmpty();
		
		int w = inv.getWidth();
		int h = inv.getHeight();
		int i = k / w;
		int j = k % w;
		
		ItemStack[] rockets = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
		RocketSide[] sides = RocketSide.values();
		
		if (j > 0) {
			rockets[0] = inv.getItem(k - 1);
			if (i < h - 1)
				rockets[2] = inv.getItem(k + w - 1);
		}
		if (j < w - 1) {
			rockets[1] = inv.getItem(k + 1);
			if (i < h - 1)
				rockets[3] = inv.getItem(k + w + 1);
		}
		
		return apply(elytra, rockets);
	}
	
	
	public static ItemStack apply(ItemStack elytra, ItemStack[] rockets) {
		ItemStack result = elytra.copy();
		IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(result);
		final TrailData trailData = spec.getTrailData();
		
		for (int r = 0; r < 4; r++) {
			if (!rockets[r].isEmpty()) {
				CompoundTag rocketTag = rockets[r].getTag();
				if (rocketTag == null || !rocketTag.contains("Fireworks")
				    || !rocketTag.getCompound("Fireworks").contains("Explosions")) {
					trailData.put(RocketSide.values()[r], null);
				} else {
					trailData.put(RocketSide.values()[r], RocketStar.listFromNBT(
					  rocketTag.getCompound("Fireworks").getList("Explosions", 10)));
				}
			}
		}
		
		return result;
	}
	
	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 2 && height >= 1;
	}
	
	@NotNull @Override
	public RecipeSerializer<?> getSerializer() {
		return AerobaticRecipes.TRAIL_RECIPE.get();
	}
}
