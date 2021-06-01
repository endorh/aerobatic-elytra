package dnj.aerobatic_elytra.common.block;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.client.block.BrokenLeavesBlockModel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static dnj.endor8util.util.ForgeUtil.futureNotNull;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModBlocks {
	/**
	 * @see BrokenLeavesBlock
	 */
	@ObjectHolder(AerobaticElytra.MOD_ID + ":" + BrokenLeavesBlock.NAME)
	public static Block BROKEN_LEAVES = futureNotNull();
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	@SubscribeEvent
	public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
		final IForgeRegistry<Block> reg = event.getRegistry();
		reg.register(new BrokenLeavesBlock());
		AerobaticElytra.logRegistered("Blocks");
	}
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onModelBake(ModelBakeEvent event) {
		final Map<ResourceLocation, IBakedModel> reg = event.getModelRegistry();
		for (BlockState bs : BROKEN_LEAVES.getStateContainer().getValidStates()) {
			ModelResourceLocation variantMRL = BlockModelShapes.getModelLocation(bs);
			IBakedModel existingModel = reg.get(variantMRL);
			if (existingModel == null) {
				LOGGER.warn("Did not find the expected vanilla baked model(s) for BROKEN_LEAVES in registry");
			} else if (existingModel instanceof BrokenLeavesBlockModel) {
				LOGGER.warn("Tried to replace BrokenLeavesBlockBakedModel twice");
			} else {
				BrokenLeavesBlockModel customModel = new BrokenLeavesBlockModel(existingModel);
				reg.put(variantMRL, customModel);
			}
		}
	}
	
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		RenderTypeLookup.setRenderLayer(BROKEN_LEAVES, RenderType.getCutoutMipped());
	}
}
