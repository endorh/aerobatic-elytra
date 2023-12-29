package endorh.aerobaticelytra.common.block.entity;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.block.AerobaticBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;

import static endorh.lazulib.common.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class AerobaticBlockEntities {
	@NotNull public static BlockEntityType<?> BROKEN_LEAVES = futureNotNull();
	
	@SubscribeEvent
	public static void onRegisterTileEntities(RegisterEvent event) {
		event.register(ForgeRegistries.BLOCK_ENTITY_TYPES.getRegistryKey(), r -> {
			BROKEN_LEAVES = BlockEntityType.Builder.of(
			  BrokenLeavesBlockEntity::new, AerobaticBlocks.BROKEN_LEAVES
			).build(null);
			r.register(BrokenLeavesBlockEntity.NAME, BROKEN_LEAVES);
			AerobaticElytra.logRegistered("Block Entities");
		});
	}
}
