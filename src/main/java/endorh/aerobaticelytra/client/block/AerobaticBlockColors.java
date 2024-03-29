package endorh.aerobaticelytra.client.block;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.block.AerobaticBlocks;
import endorh.aerobaticelytra.common.block.entity.BrokenLeavesBlockEntity;
import endorh.lazulib.common.ColorUtil;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.awt.*;

@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticBlockColors {
	@SubscribeEvent
	public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
		BlockColors colors = event.getBlockColors();
		int defaultColor = ColorUtil.multiply(new Color(FoliageColor.getEvergreenColor()), 0.6F).getRGB();
		// Return the color of the leaves block stored under the Tile Entity, or a fallback
		event.register((state, world, pos, layer) -> {
			if (world == null || pos == null)
				return defaultColor;
			final BlockEntity tile = world.getBlockEntity(pos);
			if (!(tile instanceof BrokenLeavesBlockEntity te))
				return defaultColor;
			BlockState replacedLeaves = te.getReplacedLeaves();
			if (replacedLeaves == null) return defaultColor;
			return ColorUtil.multiply(
			  new Color(colors.getColor(replacedLeaves, world, pos, layer)), 0.75F).getRGB();
		}, AerobaticBlocks.BROKEN_LEAVES);
		AerobaticElytra.logRegistered("Block Colors");
	}
}
