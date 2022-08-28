package endorh.aerobaticelytra.common.block.entity;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;
import static endorh.util.common.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class ModBlockEntities {
	
	/**
	 * @see BrokenLeavesBlockEntity
	 */
	@ObjectHolder(AerobaticElytra.MOD_ID + ":" + BrokenLeavesBlockEntity.NAME)
	public static final BlockEntityType<?> BROKEN_LEAVES_TE = futureNotNull();
	
	@SubscribeEvent
	public static void onRegisterTileEntities(
	  RegistryEvent.Register<BlockEntityType<?>> event
	) {
		final IForgeRegistry<BlockEntityType<?>> reg = event.getRegistry();
		BlockEntityType<?> type = BlockEntityType.Builder.of(
		  BrokenLeavesBlockEntity::new, ModBlocks.BROKEN_LEAVES
		).build(null);
		type.setRegistryName(prefix(BrokenLeavesBlockEntity.NAME));
		reg.register(type);
		AerobaticElytra.logRegistered("Tile Entities");
	}
}