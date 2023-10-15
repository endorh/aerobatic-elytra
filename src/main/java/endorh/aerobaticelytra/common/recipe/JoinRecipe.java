package endorh.aerobaticelytra.common.recipe;

import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;

/**
 * Joins two {@link AerobaticElytraWingItem}s into an
 * {@link AerobaticElytraItem}, the wings must share
 * capabilities NBT, except for the trail.<br>
 */
public class JoinRecipe extends CustomRecipe {
	
	private static final ElytraDyement leftDyement = new ElytraDyement();
	private static final ElytraDyement rightDyement = new ElytraDyement();
	
	public JoinRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}
	
	@Override
	public boolean matches(@NotNull CraftingContainer inv, @NotNull Level world) {
		ItemStack left = ItemStack.EMPTY, right = ItemStack.EMPTY;
		boolean found = false;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack current = inv.getItem(i);
			if (current.isEmpty())
				continue;
			if (found)
				return false;
			if (!(current.getItem() instanceof AerobaticElytraWingItem)
			    || (i + 1) % inv.getWidth() == 0
			    || !(inv.getItem(i + 1).getItem() instanceof AerobaticElytraWingItem))
				return false;
			left = inv.getItem(i);
			right = inv.getItem(i + 1);
			i++;
			found = true;
		}
		if (!found)
			return false;
		assert !left.isEmpty() && !right.isEmpty();
		return matches(left, right);
	}
	
	@Override
	public @NotNull ItemStack assemble(@NotNull CraftingContainer inv, @NotNull RegistryAccess r) {
		ItemStack left = ItemStack.EMPTY, right = ItemStack.EMPTY;
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack current = inv.getItem(i);
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
		return Objects.equals(left.getTagElement(SplitRecipe.TAG_SPLIT_ELYTRA),
		                      right.getTagElement(SplitRecipe.TAG_SPLIT_ELYTRA))
		       && Objects.equals(left.getTagElement(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS),
		                         right.getTagElement(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS));
	}
	
	/**
	 * Join two aerobatic elytra wing stacks for which
	 * {@link JoinRecipe#matches(ItemStack, ItemStack)} returns true
	 */
	public static ItemStack join(ItemStack left, ItemStack right) {
		final CompoundTag caps = left.getOrCreateTagElement(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS);
		final CompoundTag tag = left.getOrCreateTagElement(SplitRecipe.TAG_SPLIT_ELYTRA);
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
	public boolean canCraftInDimensions(int width, int height) {
		return width >= 2 && height >= 1;
	}
	
	@Override
	public @NotNull RecipeSerializer<?> getSerializer() {
		return AerobaticRecipes.JOIN_RECIPE.get();
	}
}
