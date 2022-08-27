package endorh.aerobaticelytra.client.block;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.aerobaticelytra.common.block.BrokenLeavesBlock;
import endorh.aerobaticelytra.common.tile.BrokenLeavesTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Model for {@link BrokenLeavesBlock}, which copies its quads
 * from the {@link BlockState} stored in the
 * {@link BrokenLeavesTileEntity}
 * attached to the block.
 */
public class BrokenLeavesBlockModel implements BakedModel {
	private final BakedModel fallbackModel;
	public static ModelProperty<Optional<BlockState>> COPIED_LEAVE_BLOCK = new ModelProperty<>();
	
	public BrokenLeavesBlockModel(BakedModel model) {
		fallbackModel = model;
	}
	
	private static ModelDataMap getEmptyIModelData() {
		ModelDataMap.Builder builder = new ModelDataMap.Builder();
		builder.withInitial(COPIED_LEAVE_BLOCK, Optional.empty());
		return builder.build();
	}
	
	@Override public @NotNull List<BakedQuad> getQuads(
	  @Nullable BlockState state, @Nullable Direction side,
	  @NotNull Random rand, @NotNull IModelData extraData
	) {
		return getActualBakedModelFromIModelData(extraData).getQuads(state, side, rand, extraData);
	}
	
	@Override public @NotNull IModelData getModelData(
	  @NotNull BlockAndTintGetter world, @NotNull BlockPos pos,
	  @NotNull BlockState state, @NotNull IModelData tileData
	) {
		Optional<BlockState> bestAdjacentBlock = BrokenLeavesBlock.getStoredBlockState(world, pos);
		ModelDataMap modelDataMap = getEmptyIModelData();
		modelDataMap.setData(COPIED_LEAVE_BLOCK, bestAdjacentBlock);
		return modelDataMap;
	}
	
	@Override public TextureAtlasSprite getParticleIcon(@NotNull IModelData data) {
		return getActualBakedModelFromIModelData(data).getParticleIcon(data);
	}
	
	private BakedModel getActualBakedModelFromIModelData(@NotNull IModelData data) {
		BakedModel ret = fallbackModel;
		if (!data.hasProperty(COPIED_LEAVE_BLOCK))
			return ret; // Happens on getParticleTexture
		Optional<BlockState> copiedBlock = data.getData(COPIED_LEAVE_BLOCK);
		//noinspection OptionalAssignedToNull
		assert copiedBlock != null;
		if (copiedBlock.isEmpty()) return ret;
		
		Minecraft mc = Minecraft.getInstance();
		BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
		ret = dispatcher.getBlockModel(copiedBlock.get());
		return ret;
	}
	
	
	@Override public @NotNull List<BakedQuad> getQuads(
	  @Nullable BlockState state, @Nullable Direction side, @NotNull Random rand
	) {
		//noinspection deprecation
		return fallbackModel.getQuads(state, side, rand);
	}
	
	@Override public @NotNull TextureAtlasSprite getParticleIcon() {
		return fallbackModel.getParticleIcon(getEmptyIModelData());
	}
	
	@Override public boolean useAmbientOcclusion() {
		return fallbackModel.useAmbientOcclusion();
	}
	
	@Override
	public boolean isGui3d() {
		return fallbackModel.isGui3d();
	}
	
	@Override public boolean usesBlockLight() {
		return fallbackModel.usesBlockLight();
	}
	
	@Override public boolean isCustomRenderer() {
		return fallbackModel.isCustomRenderer();
	}
	
	@Override public @NotNull ItemOverrides getOverrides() {
		return fallbackModel.getOverrides();
	}
	
	@Override public BakedModel handlePerspective(
	  TransformType transformType, PoseStack mStack
	) {
		return fallbackModel.handlePerspective(transformType, mStack);
	}
}
