package endorh.aerobaticelytra.common.block;

import endorh.aerobaticelytra.client.block.AerobaticBlockColors;
import endorh.aerobaticelytra.client.block.BrokenLeavesBlockModel;
import endorh.aerobaticelytra.common.block.entity.BrokenLeavesBlockEntity;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.collision.leave_breaking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

/**
 * A block that replaces temporarily other leaves blocks,
 * storing them in a Tile Entity in order to restore them
 * later, and copy their model and tint color for
 * rendering.<br>
 *
 * @see BrokenLeavesBlockEntity
 * @see BrokenLeavesBlockModel
 * @see AerobaticBlockColors
 */
public class BrokenLeavesBlock extends LeavesBlock implements EntityBlock {
	public static final String NAME = "broken_leaves";
	public static final ResourceLocation ID = prefix(NAME);
	
	public BrokenLeavesBlock() {
		super(createBlockProperties());
	}
	
	private static BlockBehaviour.Properties createBlockProperties() {
		return BlockBehaviour.Properties
		  .of(Material.LEAVES).strength(0.2F)
		  .sound(SoundType.GRASS).noOcclusion()
		  .isValidSpawn((state, reader, pos, type) -> false)
		  .isSuffocating((state, reader, pos) -> false)
		  .isViewBlocking((state, reader, pos) -> false)
		  .randomTicks()
		  .noCollission()
		  .requiresCorrectToolForDrops();
	}
	
	@Nullable @Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
		return new BrokenLeavesBlockEntity(pos, state);
	}
	
	@Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
	  @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
	) {
		return EntityBlock.super.getTicker(level, state, type);
	}
	
	@SuppressWarnings("deprecation") @Override
	public void entityInside(
	  @NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, Entity entity
	) {
		final float fallDistance = entity.fallDistance;
		entity.makeStuckInBlock(state, new Vec3(
		  Config.collision.leave_breaking.motion_multiplier,
		  Config.collision.leave_breaking.motion_multiplier,
		  Config.collision.leave_breaking.motion_multiplier));
		if (entity.getDeltaMovement().lengthSqr() > 0.25D)
			entity.fallDistance = fallDistance;
	}
	
	/**
	 * In the server, replaces a leaves block with a broken leaves one,
	 * remembering which block it replaced.<br>
	 * In the client, it simply plays a breaking sound for the leaves block
	 *
	 * @param world World in which to break the leaves
	 * @param pos Position of the leaves.
	 * @throws IllegalArgumentException if leaves aren't found at pos on the server
	 */
	public static void breakLeaves(Level world, BlockPos pos) {
		if (world.isClientSide) {
			world.playLocalSound(
			  pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GRASS_BREAK,
			  SoundSource.BLOCKS, 1F, 1F, false);
			return;
		}
		final BlockState prevBlockState = world.getBlockState(pos);
		if (!prevBlockState.is(BlockTags.LEAVES))
			throw new IllegalArgumentException(
			  "Attempt to replace non leaves block with broken leaves");
		final Integer dist = prevBlockState.getValue(DISTANCE);
		final BlockPos.MutableBlockPos cursor = new MutableBlockPos();
		for (Direction direction: Direction.values()) {
			cursor.setWithOffset(pos, direction);
			final BlockState adj = world.getBlockState(cursor);
			if (adj.is(BlockTags.LEAVES) && !adj.getValue(PERSISTENT)
			    && adj.getValue(DISTANCE) < 7 && adj.getValue(DISTANCE) > dist)
				breakLeaves(world, cursor);
		}
		world.setBlock(
		  pos, AerobaticBlocks.BROKEN_LEAVES.defaultBlockState()
			 .setValue(DISTANCE, prevBlockState.getValue(DISTANCE))
			 .setValue(PERSISTENT, prevBlockState.getValue(PERSISTENT)),
		  Block.UPDATE_ALL);
		BlockEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof BrokenLeavesBlockEntity te))
			throw new IllegalStateException(
			  "Broken leaves block did not have BrokenLeavesTileEntity");
		te.replacedLeaves = prevBlockState;
		te.setChanged();
	}
	
	public void tryRestoreBrokenLeaves(
	  ServerLevel world, BlockPos pos
	) {
		final BlockState bs = world.getBlockState(pos);
		if (!(bs.getBlock() == AerobaticBlocks.BROKEN_LEAVES))
			return;
		BlockEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof BrokenLeavesBlockEntity te))
			return;
		if (te.replacedLeaves != null) {
			if (world.isUnobstructed(te.replacedLeaves, pos, CollisionContext.empty()))
				world.setBlockAndUpdate(pos, te.replacedLeaves);
		} else {
			world.destroyBlock(pos, false);
		}
	}
	
	@Override public boolean isRandomlyTicking(@NotNull BlockState state) {
		return true;
	}
	
	@Override public void randomTick(
	  @NotNull BlockState state, @NotNull ServerLevel world,
	  @NotNull BlockPos pos, @NotNull RandomSource random
	) {
		super.randomTick(state, world, pos, random);
		if (!world.hasNeighborSignal(pos) &&
		    random.nextFloat() > leave_breaking.regrow_chance) // 0.4F)
			tryRestoreBrokenLeaves(world, pos);
	}
	
	public static Optional<BlockState> getStoredBlockState(
	  BlockAndTintGetter world, BlockPos pos
	) {
		BlockEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof BrokenLeavesBlockEntity te))
			return Optional.empty();
		return te.replacedLeaves != null? Optional.of(te.replacedLeaves) : Optional.empty();
	}
}
