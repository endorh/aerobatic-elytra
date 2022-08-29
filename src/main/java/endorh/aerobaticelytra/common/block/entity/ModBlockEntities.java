package endorh.aerobaticelytra.common.block.entity;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;

import static endorh.util.common.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class ModBlockEntities {
	
	/**
	 * @see BrokenLeavesBlockEntity
	 */
	@ObjectHolder(value = AerobaticElytra.MOD_ID + ":" + BrokenLeavesBlockEntity.NAME, registryName="block")
	public static final BlockEntityType<?> BROKEN_LEAVES_TE = futureNotNull();
	
	@SubscribeEvent
	public static void onRegisterTileEntities(RegisterEvent event) {
		event.register(ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(), r -> {
			BlockEntityType<?> type = BlockEntityType.Builder.of(
			  BrokenLeavesBlockEntity::new, ModBlocks.BROKEN_LEAVES
			).build(null);
			r.register(BrokenLeavesBlockEntity.NAME, type);
			AerobaticElytra.logRegistered("Tile Entities");
		});
	}
}
