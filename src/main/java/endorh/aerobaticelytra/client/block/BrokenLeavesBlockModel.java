package endorh.aerobaticelytra.client.block;

import endorh.aerobaticelytra.common.block.BrokenLeavesBlock;
import endorh.aerobaticelytra.common.block.entity.BrokenLeavesBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Model for {@link BrokenLeavesBlock}, which copies its quads
 * from the {@link BlockState} stored in the
 * {@link BrokenLeavesBlockEntity}
 * attached to the block.
 */
public class BrokenLeavesBlockModel extends BakedModelWrapper<BakedModel> {
	public static ModelProperty<Optional<BlockState>> COPIED_LEAVES_BLOCK = new ModelProperty<>();
	
	public BrokenLeavesBlockModel(BakedModel model) {
		super(model);
	}
	
	private static ModelData getEmptyModelData() {
		ModelData.Builder builder = ModelData.builder();
		builder.with(COPIED_LEAVES_BLOCK, Optional.empty());
		return builder.build();
	}
	
	@Override public @NotNull List<BakedQuad> getQuads(
	  @Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
	  @NotNull ModelData data, @Nullable RenderType renderType
	) {
		return getActualBakedModelFromIModelData(data).getQuads(state, side, rand, data, renderType);
	}
	
	@Override public @NotNull ModelData getModelData(
	  @NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
	  @NotNull BlockState state, @NotNull ModelData modelData
	) {
		Optional<BlockState> bestAdjacentBlock = BrokenLeavesBlock.getStoredBlockState(level, pos);
		return getEmptyModelData().derive()
		  .with(COPIED_LEAVES_BLOCK, bestAdjacentBlock).build();
	}
	
	@Override public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
		return getActualBakedModelFromIModelData(data).getParticleIcon(data);
	}
	
	private BakedModel getActualBakedModelFromIModelData(@NotNull ModelData data) {
		BakedModel ret = originalModel;
		if (!data.has(COPIED_LEAVES_BLOCK))
			return ret; // Happens on getParticleTexture
		Optional<BlockState> copiedBlock = data.get(COPIED_LEAVES_BLOCK);
		//noinspection OptionalAssignedToNull
		assert copiedBlock != null;
		if (copiedBlock.isEmpty()) return ret;
		
		Minecraft mc = Minecraft.getInstance();
		BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
		ret = dispatcher.getBlockModel(copiedBlock.get());
		return ret;
	}
	
	@Override public @NotNull ChunkRenderTypeSet getRenderTypes(
	  @NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data
	) {
		// return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
		return super.getRenderTypes(state, rand, data);
	}
}
