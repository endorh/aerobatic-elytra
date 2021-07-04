package endorh.aerobatic_elytra.common.recipe;

import endorh.aerobatic_elytra.common.item.ElytraDyementReader;
import endorh.aerobatic_elytra.common.item.ElytraDyementReader.WingSide;
import endorh.aerobatic_elytra.common.capability.ElytraSpecCapability;
import endorh.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobatic_elytra.common.item.AerobaticElytraItem;
import endorh.aerobatic_elytra.common.item.AerobaticElytraWingItem;
import endorh.aerobatic_elytra.common.item.ModItems;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.util.common.ForgeUtil.getSerializedCaps;

/**
 * Joins two {@link AerobaticElytraWingItem}s into an
 * {@link AerobaticElytraItem}, the wings must share
 * capabilities NBT, except for the trail.<br>
 */
public class JoinRecipe extends SpecialRecipe {
	
	private static final ElytraDyementReader leftDyement = new ElytraDyementReader();
	private static final ElytraDyementReader rightDyement = new ElytraDyementReader();
	
	public JoinRecipe(ResourceLocation id) {
		super(id);
	}
	
	@Override
	public boolean matches(@NotNull CraftingInventory inv, @NotNull World world) {
		ItemStack left = ItemStack.EMPTY, right = ItemStack.EMPTY;
		boolean found = false;
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack current = inv.getStackInSlot(i);
			if (current.isEmpty())
				continue;
			if (found)
				return false;
			if (!(current.getItem() instanceof AerobaticElytraWingItem)
			    || (i + 1) % inv.getWidth() == 0
			    || !(inv.getStackInSlot(i + 1).getItem() instanceof AerobaticElytraWingItem))
				return false;
			left = inv.getStackInSlot(i);
			right = inv.getStackInSlot(i + 1);
			i++;
			found = true;
		}
		if (!found)
			return false;
		assert !left.isEmpty() && !right.isEmpty();
		return matches(left, right);
	}
	
	@Override
	public @NotNull ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
		ItemStack left = ItemStack.EMPTY, right = ItemStack.EMPTY;
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack current = inv.getStackInSlot(i);
			if (current.isEmpty())
				continue;
			if (left.isEmpty())
				left = current;
			else if (right.isEmpty())
				right = current;
			else return ItemStack.EMPTY;
		}
		assert left.getItem() instanceof AerobaticElytraWingItem
		       && right.getItem() instanceof AerobaticElytraWingItem;
		assert matches(left, right);
		return join(left, right);
	}
	
	public static boolean matches(ItemStack left, ItemStack right) {
		return ElytraSpecCapability.compareNoTrail(
		  getSerializedCaps(left), getSerializedCaps(right));
	}
	
	public static ItemStack join(ItemStack left, ItemStack right) {
		ItemStack elytra = new ItemStack(ModItems.AEROBATIC_ELYTRA, 1, getSerializedCaps(left));
		leftDyement.read(left);
		rightDyement.read(right);
		if (leftDyement.getWing(WingSide.LEFT).equals(rightDyement.getWing(WingSide.RIGHT))) {
			leftDyement.getWing(WingSide.LEFT).write(elytra, null);
		} else {
			leftDyement.getWing(WingSide.LEFT).write(elytra, WingSide.LEFT);
			rightDyement.getWing(WingSide.RIGHT).write(elytra, WingSide.RIGHT);
		}
		elytra.setDamage((left.getDamage() + right.getDamage()) / 2);
		TrailData leftData = getElytraSpecOrDefault(left).getTrailData();
		TrailData rightData = getElytraSpecOrDefault(right).getTrailData();
		TrailData trailData = getElytraSpecOrDefault(elytra).getTrailData();
		trailData.set(WingSide.LEFT, leftData);
		trailData.set(WingSide.RIGHT, rightData);
		return elytra;
	}
	
	@Override
	public boolean canFit(int width, int height) {
		return width >= 2 && height >= 1;
	}
	
	@Override
	public @NotNull IRecipeSerializer<?> getSerializer() {
		return ModRecipes.JOIN_RECIPE.get();
	}
}
