package endorh.aerobatic_elytra.common.recipe;

import com.mojang.datafixers.util.Pair;
import endorh.aerobatic_elytra.common.item.ElytraDyement;
import endorh.aerobatic_elytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobatic_elytra.common.item.ElytraDyement.WingSide;
import endorh.aerobatic_elytra.common.item.AerobaticElytraItem;
import endorh.aerobatic_elytra.common.item.AerobaticElytraWingItem;
import endorh.util.common.ColorUtil;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.*;
import net.minecraft.item.ItemStack.TooltipDisplayFlags;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.nbt.CompoundNBT;
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
		List<DyeItem> tintList = new ArrayList<>();
		
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
				tintList.add((DyeItem) item);
			}
		}
		if (elytra.isEmpty() || tintList.isEmpty())
			return ItemStack.EMPTY;
		
		ItemStack res = elytra.copy();
		dyement.read(res);
		res.removeChildTag("WingInfo");
		res.removeChildTag("BlockEntityTag");
		
		List<Integer> colors = new ArrayList<>();
		for (WingSide side : WingSide.values()) {
			WingDyement wingDyement = dyement.getWing(side);
			if (wingDyement.hasPattern) {
				for (Pair<BannerPattern, DyeColor> pair : wingDyement.patternColorData)
					colors.add(pair.getSecond().getColorValue());
			} else if (wingDyement.hasColor) {
				colors.add(wingDyement.color);
			}
		}
		if (!colors.isEmpty()) {
			int color = ColorUtil.mix(colors);
			res.getOrCreateChildTag("display").putInt("color", color);
		} else {
			res.getOrCreateChildTag("display").remove("color");
		}
		res = IDyeableArmorItem.dyeItem(res, tintList);
		
		CompoundNBT tag = res.getOrCreateTag();
		int flags = tag.contains("HideFlags", 99)
		  ? tag.getInt("HideFlags") : 0;
		flags |= TooltipDisplayFlags.DYE.func_242397_a();
		tag.putInt("HideFlags", flags);
		return res;
	}
	
	@Override
	public boolean canFit(int width, int height) {
		return width * height >= 2;
	}
	
	@NotNull @Override
	public IRecipeSerializer<?> getSerializer() {
		return ModRecipes.DYE_RECIPE.get();
	}
}
