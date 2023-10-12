package endorh.aerobaticelytra.common.recipe;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.common.item.AerobaticElytraItem;
import endorh.aerobaticelytra.common.item.AerobaticElytraWingItem;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.util.common.ColorUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DyeRecipe extends CustomRecipe {
	public DyeRecipe(ResourceLocation id, CraftingBookCategory category) {
		super(id, category);
	}
	
	private static final ElytraDyement dyement = new ElytraDyement();
	
	@Override
	public boolean matches(CraftingContainer inv, @NotNull Level worldIn) {
		boolean found = false;
		int tints = 0;
		
		for (int i = 0; i < inv.getContainerSize(); ++i) {
			ItemStack current = inv.getItem(i);
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
	public ItemStack assemble(CraftingContainer inv) {
		ItemStack elytra = ItemStack.EMPTY;
		List<DyeItem> dyeList = new ArrayList<>();
		
		for (int i = 0; i < inv.getContainerSize(); ++i) {
			ItemStack current = inv.getItem(i);
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
		res.removeTagKey("WingInfo");
		res.removeTagKey("BlockEntityTag");
		
		List<Integer> colors = new ArrayList<>();
		if (dyement.hasWingDyement) {
			for (WingSide side: WingSide.values()) {
				WingDyement wing = dyement.getWing(side);
				if (wing.hasPattern) {
					for (Pair<BannerPattern, DyeColor> pair: wing.patternColorData)
						colors.add(pair.getSecond().getTextColor());
				} else if (wing.hasColor)
					colors.add(wing.color);
			}
		} else {
			WingDyement wing = dyement.getFirst();
			if (wing.hasPattern) {
				for (Pair<BannerPattern, DyeColor> pair: wing.patternColorData)
					colors.add(pair.getSecond().getTextColor());
			} else if (wing.hasColor)
				colors.add(wing.color);
		}
		if (!colors.isEmpty()) {
			int color = ColorUtil.mix(colors);
			res.getOrCreateTagElement("display").putInt("color", color);
		} else {
			res.getOrCreateTagElement("display").remove("color");
		}
		res = DyeableLeatherItem.dyeArmor(res, dyes);
		
		ElytraDyement.hideDyedFlag(res);
		return res;
	}
	
	@Override public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}
	
	@NotNull @Override
	public RecipeSerializer<?> getSerializer() {
		return AerobaticRecipes.DYE_RECIPE.get();
	}
}
