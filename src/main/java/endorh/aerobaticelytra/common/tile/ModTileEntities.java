package endorh.aerobaticelytra.common.tile;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.block.ModBlocks;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;
import static endorh.util.common.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModTileEntities {
	
	/**
	 * @see BrokenLeavesTileEntity
	 */
	@ObjectHolder(AerobaticElytra.MOD_ID + ":" + BrokenLeavesTileEntity.NAME)
	public static final TileEntityType<?> BROKEN_LEAVES_TE = futureNotNull();
	
	@SubscribeEvent
	public static void onRegisterTileEntities(
	  RegistryEvent.Register<TileEntityType<?>> event
	) {
		final IForgeRegistry<TileEntityType<?>> reg = event.getRegistry();
		TileEntityType<?> type = TileEntityType.Builder.of(
		  BrokenLeavesTileEntity::new, ModBlocks.BROKEN_LEAVES
		).build(null);
		type.setRegistryName(prefix(BrokenLeavesTileEntity.NAME));
		reg.register(type);
		AerobaticElytra.logRegistered("Tile Entities");
	}
}
