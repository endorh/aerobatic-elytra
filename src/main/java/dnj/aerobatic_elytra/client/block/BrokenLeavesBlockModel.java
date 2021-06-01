package dnj.aerobatic_elytra.client.block;

import com.mojang.blaze3d.matrix.MatrixStack;
import dnj.aerobatic_elytra.common.block.BrokenLeavesBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
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
 * {@link dnj.aerobatic_elytra.common.tile.BrokenLeavesTileEntity}
 * attached to the block.
 */
public class BrokenLeavesBlockModel implements IBakedModel {
	private final IBakedModel fallbackModel;
	public static ModelProperty<Optional<BlockState>> COPIED_LEAVE_BLOCK = new ModelProperty<>();
	
	public BrokenLeavesBlockModel(IBakedModel model) {
		fallbackModel = model;
	}
	
	public static ModelDataMap getEmptyIModelData() {
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
	  @NotNull IBlockDisplayReader world, @NotNull BlockPos pos,
	  @NotNull BlockState state, @NotNull IModelData tileData
	) {
		Optional<BlockState> bestAdjacentBlock = BrokenLeavesBlock.getStoredBlockState(world, pos);
		ModelDataMap modelDataMap = getEmptyIModelData();
		modelDataMap.setData(COPIED_LEAVE_BLOCK, bestAdjacentBlock);
		return modelDataMap;
	}
	
	@Override
	public TextureAtlasSprite getParticleTexture(@NotNull IModelData data) {
		return getActualBakedModelFromIModelData(data).getParticleTexture(data);
	}
	
	private IBakedModel getActualBakedModelFromIModelData(@NotNull IModelData data) {
		IBakedModel ret = fallbackModel;
		if (!data.hasProperty(COPIED_LEAVE_BLOCK))
			return ret; // Happens on getParticleTexture
		Optional<BlockState> copiedBlock = data.getData(COPIED_LEAVE_BLOCK);
		//noinspection OptionalAssignedToNull
		assert copiedBlock != null;
		if (!copiedBlock.isPresent()) return ret;
		
		Minecraft mc = Minecraft.getInstance();
		BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
		ret = dispatcher.getModelForState(copiedBlock.get());
		return ret;
	}
	
	
	
	@Override public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random rand) {
		throw new AssertionError("IBakedModel::getQuads should never be called, only IForgeBakedModel::getQuads");
	}
	
	@Override public @NotNull TextureAtlasSprite getParticleTexture() {
		return fallbackModel.getParticleTexture(getEmptyIModelData());
	}
	
	@Override public boolean isAmbientOcclusion() {
		return fallbackModel.isAmbientOcclusion();
	}
	
	@Override
	public boolean isGui3d() {
		return fallbackModel.isGui3d();
	}
	
	@Override public boolean isSideLit() {
		return fallbackModel.isSideLit();
	}
	
	@Override public boolean isBuiltInRenderer() {
		return fallbackModel.isBuiltInRenderer();
	}
	
	@Override public @NotNull ItemOverrideList getOverrides() {
		return fallbackModel.getOverrides();
	}
	
	@Override public IBakedModel handlePerspective(
	  TransformType transformType, MatrixStack mStack
	) {
		return fallbackModel.handlePerspective(transformType, mStack);
	}
}
