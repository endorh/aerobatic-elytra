package endorh.aerobaticelytra.common.block;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.block.BrokenLeavesBlockModel;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static endorh.util.common.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class ModBlocks {
	/**
	 * @see BrokenLeavesBlock
	 */
	@ObjectHolder(value=AerobaticElytra.MOD_ID + ":" + BrokenLeavesBlock.NAME, registryName="block")
	public static Block BROKEN_LEAVES = futureNotNull();
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	@SubscribeEvent
	public static void onRegisterBlocks(RegisterEvent event) {
		event.register(ForgeRegistries.BLOCKS.getRegistryKey(), r -> {
			
			r.register(BrokenLeavesBlock.ID, new BrokenLeavesBlock());
			AerobaticElytra.logRegistered("Blocks");
		});
	}
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onModelBake(ModelEvent.BakingCompleted event) {
		final Map<ResourceLocation, BakedModel> reg = event.getModels();
		for (BlockState bs: BROKEN_LEAVES.getStateDefinition().getPossibleStates()) {
			ModelResourceLocation variantMRL = BlockModelShaper.stateToModelLocation(bs);
			BakedModel existingModel = reg.get(variantMRL);
			if (existingModel == null)
				throw new IllegalStateException("Missing fallback model for Broken Leaves block");
			if (existingModel instanceof BrokenLeavesBlockModel) {
				LOGGER.warn("Tried to replace BrokenLeavesBlockBakedModel twice");
			} else {
				BrokenLeavesBlockModel customModel = new BrokenLeavesBlockModel(existingModel);
				reg.put(variantMRL, customModel);
			}
		}
	}
}
