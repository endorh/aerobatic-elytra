package endorh.aerobaticelytra.common.recipe;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.util.common.ColorUtil;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.*;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DyeRecipe extends SpecialRecipe {
	public DyeRecipe(ResourceLocation id)
	{
		super(id);
	}
	private static final ElytraDyement dyement = new ElytraDyement();
	
	@Override
	public boolean matches(CraftingInventory inv, @NotNull World worldIn) {
		boolean found = false;
		int tints = 0;
		
		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			ItemStack current = inv.getStackInSlot(i);
			if (current.isEmpty())
				continue;
			final Item item = current.getItem();
			if (item instanceof AerobaticElytraItem
			    || item instanceof AerobaticElytraWingItem) {
				if (found)
					return false;
				else found = true;
			} else {
				if (!(item instanceof DyeItem))
					return false;
				tints++;
			}
		}
		
		return found && tints > 0;
	}
	
	@NotNull @Override
	public ItemStack getCraftingResult(CraftingInventory inv) {
		ItemStack elytra = ItemStack.EMPTY;
		List<DyeItem> dyeList = new ArrayList<>();
		
		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			ItemStack current = inv.getStackInSlot(i);
			if (current.isEmpty())
				continue;
			Item item = current.getItem();
			if (item instanceof AerobaticElytraItem
			    || item instanceof AerobaticElytraWingItem) {
				if (!elytra.isEmpty())
					return ItemStack.EMPTY;
				elytra = current.copy();
			} else {
				if (!(item instanceof DyeItem))
					return ItemStack.EMPTY;
				dyeList.add((DyeItem) item);
			}
		}
		if (elytra.isEmpty() || dyeList.isEmpty())
			return ItemStack.EMPTY;
		
		return dye(elytra, dyeList);
	}
	
	public static ItemStack dye(ItemStack elytra, List<DyeItem> dyes) {
		ItemStack res = elytra.copy();
		dyement.read(res);
		res.removeChildTag("WingInfo");
		res.removeChildTag("BlockEntityTag");
		
		List<Integer> colors = new ArrayList<>();
		if (dyement.hasWingDyement) {
			for (WingSide side : WingSide.values()) {
				WingDyement wing = dyement.getWing(side);
				if (wing.hasPattern) {
					for (Pair<BannerPattern, DyeColor> pair : wing.patternColorData)
						colors.add(pair.getSecond().getColorValue());
				} else if (wing.hasColor)
					colors.add(wing.color);
			}
		} else {
			WingDyement wing = dyement.getFirst();
			if (wing.hasPattern) {
				for (Pair<BannerPattern, DyeColor> pair : wing.patternColorData)
					colors.add(pair.getSecond().getColorValue());
			} else if (wing.hasColor)
				colors.add(wing.color);
		}
		if (!colors.isEmpty()) {
			int color = ColorUtil.mix(colors);
			res.getOrCreateChildTag("display").putInt("color", color);
		} else {
			res.getOrCreateChildTag("display").remove("color");
		}
		res = IDyeableArmorItem.dyeItem(res, dyes);
		
		ElytraDyement.hideDyedFlag(res);
		return res;
	}
	
	@Override public boolean canFit(int width, int height) {
		return width * height >= 2;
	}
	
	@NotNull @Override
	public IRecipeSerializer<?> getSerializer() {
		return ModRecipes.DYE_RECIPE.get();
	}
}
