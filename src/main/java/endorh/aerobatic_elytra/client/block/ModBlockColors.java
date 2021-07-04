package endorh.aerobatic_elytra.client.block;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.block.ModBlocks;
import endorh.aerobatic_elytra.common.tile.BrokenLeavesTileEntity;
import endorh.util.common.ColorUtil;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.FoliageColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import java.awt.*;

@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModBlockColors {
	@SubscribeEvent
	public static void registerBlockColors(ColorHandlerEvent.Block event) {
		BlockColors colors = event.getBlockColors();
		int defaultColor = ColorUtil.multiply(new Color(FoliageColors.getSpruce()), 0.6F).getRGB();
		// Return the color of the leaves block stored under the Tile Entity, or a fallback
		colors.register(
		  (state, world, pos, layer) -> {
			  if (world == null || pos == null)
				  return defaultColor;
			  final TileEntity tile = world.getTileEntity(pos);
			  if (!(tile instanceof BrokenLeavesTileEntity))
				  return defaultColor;
			  BrokenLeavesTileEntity te = (BrokenLeavesTileEntity) tile;
			  if (te.replacedLeaves == null)
				  return defaultColor;
			  return ColorUtil.multiply(
			    new Color(colors.getColor(te.replacedLeaves, world, pos, layer)), 0.75F).getRGB();
		  }, ModBlocks.BROKEN_LEAVES);
		AerobaticElytra.logRegistered("Block Colors");
	}
}
