package endorh.aerobaticelytra.client.item;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.common.item.ElytraDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.IItemProvider;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class AerobaticElytraItemColor implements IItemColor {
	public static void register(IItemProvider item) {
		Minecraft.getInstance().getItemColors().register(
		  new AerobaticElytraItemColor(), item);
	}
	private static final ElytraDyement dyement = new ElytraDyement();
	
	@Override public int getColor(@NotNull ItemStack stack, int tintLayer) {
		assert tintLayer >= 0;
		if (tintLayer >= 4)
			return Color.WHITE.getRGB();
		
		dyement.read(stack);
		if (dyement.hasWingDyement) {
			WingDyement wingDye = dyement.getWing(tintLayer <= 1 ? WingSide.LEFT : WingSide.RIGHT);
			if (wingDye.hasPattern) {
				if (tintLayer % 2 == 0)
					return wingDye.patternColorData.get(0).getSecond().getColorValue();
				return wingDye.patternColorData.get(wingDye.patternColorData.size() - 1)
				  .getSecond().getColorValue();
			} else return wingDye.color;
		} else {
			WingDyement wingDye = dyement.getFirst();
			if (wingDye.hasPattern) {
				final List<Pair<BannerPattern, DyeColor>> list = wingDye.patternColorData;
				int n = list.size();
				assert n >= 1;
				switch (tintLayer) {
					case 3: return list.get(n-1).getSecond().getColorValue();
					case 2: return list.get(max(0, n-2)).getSecond().getColorValue();
					case 1: return list.get(min(n-1, 1)).getSecond().getColorValue();
					default: return list.get(0).getSecond().getColorValue();
				}
			} else {
				return wingDye.color;
			}
		}
	}
}
