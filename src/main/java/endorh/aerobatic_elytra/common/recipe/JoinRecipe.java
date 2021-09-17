package endorh.aerobatic_elytra.common.recipe;

import endorh.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobatic_elytra.common.item.AerobaticElytraItem;
import endorh.aerobatic_elytra.common.item.AerobaticElytraWingItem;
import endorh.aerobatic_elytra.common.item.ElytraDyement;
import endorh.aerobatic_elytra.common.item.ElytraDyement.WingSide;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;

/**
 * Joins two {@link AerobaticElytraWingItem}s into an
 * {@link AerobaticElytraItem}, the wings must share
 * capabilities NBT, except for the trail.<br>
 */
public class JoinRecipe extends SpecialRecipe {
	
	private static final ElytraDyement leftDyement = new ElytraDyement();
	private static final ElytraDyement rightDyement = new ElytraDyement();
	
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
		if (!(left.getItem() instanceof AerobaticElytraWingItem) || !(right.getItem() instanceof AerobaticElytraWingItem))
			return false;
		if (((AerobaticElytraWingItem) left.getItem()).getElytraItem() != ((AerobaticElytraWingItem) right.getItem()).getElytraItem())
			return false;
		return Objects.equals(left.getChildTag(SplitRecipe.TAG_SPLIT_ELYTRA),
		                      right.getChildTag(SplitRecipe.TAG_SPLIT_ELYTRA))
		       && Objects.equals(left.getChildTag(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS),
		                         right.getChildTag(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS));
	}
	
	/**
	 * Join two aerobatic elytra wing stacks for which
	 * {@link JoinRecipe#matches(ItemStack, ItemStack)} returns true
	 */
	public static ItemStack join(ItemStack left, ItemStack right) {
		final CompoundNBT caps = left.getOrCreateChildTag(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS);
		final CompoundNBT tag = left.getOrCreateChildTag(SplitRecipe.TAG_SPLIT_ELYTRA);
		ItemStack elytra = new ItemStack(((AerobaticElytraWingItem) left.getItem()).getElytraItem(), 1, caps.copy());
		elytra.setTag(tag.copy());
		leftDyement.read(left);
		rightDyement.read(right);
		if (leftDyement.getWing(WingSide.LEFT).equals(rightDyement.getWing(WingSide.RIGHT))) {
			leftDyement.getWing(WingSide.LEFT).write(elytra, null);
		} else {
			leftDyement.getWing(WingSide.LEFT).write(elytra, WingSide.LEFT);
			rightDyement.getWing(WingSide.RIGHT).write(elytra, WingSide.RIGHT);
		}
		
		getElytraSpecOrDefault(elytra);
		TrailData trailData = getElytraSpecOrDefault(elytra).getTrailData();
		TrailData leftData = getElytraSpecOrDefault(left).getTrailData();
		TrailData rightData = getElytraSpecOrDefault(right).getTrailData();
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
