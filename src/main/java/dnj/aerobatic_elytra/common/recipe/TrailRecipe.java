package dnj.aerobatic_elytra.common.recipe;

import dnj.aerobatic_elytra.client.trail.AerobaticTrail.RocketSide;
import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.capability.ElytraSpecCapability;
import dnj.aerobatic_elytra.common.capability.IElytraSpec;
import dnj.aerobatic_elytra.common.capability.IElytraSpec.RocketExplosion;
import dnj.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.item.ItemStack.EMPTY;

/**
 * Modifies the trail of any of the 4 rockets an Aerobatic Elytra
 * has, depending on the relative position of the rocket and
 * the elytra in the crafting bench.<br>
 * Using a rocket without explosion removes the trail for that
 * rocket.<br>
 * All rockets may be modified at the same time.
 */
public class TrailRecipe extends SpecialRecipe {
	public TrailRecipe(ResourceLocation id) {
		super(id);
	}
	
	@Override
	public boolean matches(@NotNull CraftingInventory inv, @NotNull World worldIn) {
		ItemStack elytra = null;
		int e = 0;
		int rockets = 0;
		for (int k = 0; k < inv.getSizeInventory(); k++) {
			ItemStack item = inv.getStackInSlot(k);
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
						if (!inv.getStackInSlot(y * w + x).isEmpty())
							count++;
						continue;
					}
				}
				if (x == j && y == i)
					continue;
				if (!inv.getStackInSlot(y * w + x).isEmpty())
					return false;
			}
		}
		
		return count > 0;
	}
	
	@NotNull @Override
	public ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
		ItemStack elytra = EMPTY;
		int k;
		for (k = 0; k < inv.getSizeInventory(); k++) {
			ItemStack item = inv.getStackInSlot(k);
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
		
		ItemStack[] rockets = {EMPTY, EMPTY, EMPTY, EMPTY};
		RocketSide[] sides = RocketSide.values();
		String[] tagNames = {
		  RocketSide.LEFT.tagName, RocketSide.RIGHT.tagName,
		  RocketSide.CENTER_LEFT.tagName, RocketSide.CENTER_RIGHT.tagName};
		
		if (j > 0) {
			rockets[0] = inv.getStackInSlot(k - 1);
			if (i < h - 1)
				rockets[2] = inv.getStackInSlot(k + w - 1);
		}
		if (j < w - 1) {
			rockets[1] = inv.getStackInSlot(k + 1);
			if (i < h - 1)
				rockets[3] = inv.getStackInSlot(k + w + 1);
		}
		
		ItemStack result = elytra.copy();
		IElytraSpec spec = ElytraSpecCapability.getElytraSpecOrDefault(result);
		final TrailData trailData = spec.getTrailData();
		
		for (int r = 0; r < 4; r++) {
			if (!rockets[r].isEmpty()) {
				CompoundNBT rocketTag = rockets[r].getTag();
				if (rocketTag == null || !rocketTag.contains("Fireworks")
				    || !rocketTag.getCompound("Fireworks").contains("Explosions")) {
					trailData.put(sides[r], null);
				} else {
					trailData.put(sides[r], RocketExplosion.listFromNBT(
					  rocketTag.getCompound("Fireworks").getList("Explosions", 10)));
				}
			}
		}
		
		return result;
	}
	
	@Override
	public boolean canFit(int width, int height) {
		return width >= 2 && height >= 1;
	}
	
	@NotNull @Override
	public IRecipeSerializer<?> getSerializer() {
		return ModRecipes.TRAIL_RECIPE.get();
	}
}
