package endorh.aerobaticelytra.client.block;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.block.AerobaticBlocks;
import endorh.aerobaticelytra.common.tile.BrokenLeavesTileEntity;
import endorh.util.common.ColorUtil;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.awt.Color;

@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticBlockColors {
	@SubscribeEvent
	public static void registerBlockColors(ColorHandlerEvent.Block event) {
		BlockColors colors = event.getBlockColors();
		int defaultColor = ColorUtil.multiply(new Color(FoliageColor.getEvergreenColor()), 0.6F).getRGB();
		// Return the color of the leaves block stored under the Tile Entity, or a fallback
		colors.register(
		  (state, world, pos, layer) -> {
			  if (world == null || pos == null)
				  return defaultColor;
			  final BlockEntity tile = world.getBlockEntity(pos);
			  if (!(tile instanceof BrokenLeavesTileEntity te))
				  return defaultColor;
			  if (te.replacedLeaves == null)
				  return defaultColor;
			  return ColorUtil.multiply(
			    new Color(colors.getColor(te.replacedLeaves, world, pos, layer)), 0.75F).getRGB();
		  }, AerobaticBlocks.BROKEN_LEAVES);
		AerobaticElytra.logRegistered("Block Colors");
	}
}
