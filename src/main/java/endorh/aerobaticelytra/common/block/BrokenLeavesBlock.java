package endorh.aerobaticelytra.common.block;

import endorh.aerobaticelytra.client.block.BrokenLeavesBlockModel;
import endorh.aerobaticelytra.client.block.ModBlockColors;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.collision.leave_breaking;
import endorh.aerobaticelytra.common.tile.BrokenLeavesTileEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants.BlockFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Random;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

/**
 * A block that replaces temporarily other leaves blocks,
 * storing them in a Tile Entity in order to restore them
 * later, and copy their model and tint color for
 * rendering.<br>
 *
 * @see BrokenLeavesTileEntity
 * @see BrokenLeavesBlockModel
 * @see ModBlockColors
 */
public class BrokenLeavesBlock extends LeavesBlock {
	public static final String NAME = "broken_leaves";
	
	public BrokenLeavesBlock() {
		super(createBlockProperties());
		setRegistryName(prefix(NAME));
	}
	
	private static AbstractBlock.Properties createBlockProperties() {
		return AbstractBlock.Properties
		  .of(Material.LEAVES).strength(0.2F)
		  .sound(SoundType.GRASS).noOcclusion()
		  .isValidSpawn((state, reader, pos, type) -> false)
		  .isSuffocating((state, reader, pos) -> false)
		  .isViewBlocking((state, reader, pos) -> false)
		  .randomTicks()
		  .noCollission().noDrops();
	}
	
	@Override public boolean hasTileEntity(BlockState state) {
		return true;
	}
	
	@Nullable @Override public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new BrokenLeavesTileEntity();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void entityInside(
	  @NotNull BlockState state, @NotNull World world, @NotNull BlockPos pos, Entity entity
	) {
		final float fallDistance = entity.fallDistance;
		entity.makeStuckInBlock(state, new Vector3d(
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
	 * @param world World in which to break the leaves
	 * @param pos Position of the leaves.
	 * @throws IllegalArgumentException if leaves aren't found at pos on the server
	 */
	public static void breakLeaves(World world, BlockPos pos) {
		if (world.isClientSide) {
			world.playLocalSound(
			  pos.getX(), pos.getY(), pos.getZ(), SoundEvents.GRASS_BREAK,
			  SoundCategory.BLOCKS, 1F, 1F, false);
			return;
		}
		final BlockState prevBlockState = world.getBlockState(pos);
		if (!prevBlockState.getBlock().is(BlockTags.LEAVES))
			throw new IllegalArgumentException(
			  "Attempt to replace non leaves block with broken leaves");
		final Integer dist = prevBlockState.getValue(DISTANCE);
		final BlockPos.Mutable cursor = new Mutable();
		for (Direction direction : Direction.values()) {
			cursor.setWithOffset(pos, direction);
			final BlockState adj = world.getBlockState(cursor);
			if (adj.getBlock().is(BlockTags.LEAVES) && !adj.getValue(PERSISTENT)
			    && adj.getValue(DISTANCE) < 7 && adj.getValue(DISTANCE) > dist)
				breakLeaves(world, cursor);
		}
		world.setBlock(
		  pos, ModBlocks.BROKEN_LEAVES.defaultBlockState()
			 .setValue(DISTANCE, prevBlockState.getValue(DISTANCE))
			 .setValue(PERSISTENT, prevBlockState.getValue(PERSISTENT)),
		  BlockFlags.DEFAULT);
		TileEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof BrokenLeavesTileEntity))
			throw new IllegalStateException(
			  "Broken leaves block did not have BrokenLeavesTileEntity");
		BrokenLeavesTileEntity te = (BrokenLeavesTileEntity) tile;
		te.replacedLeaves = prevBlockState;
		te.setChanged();
	}
	
	public void tryRestoreBrokenLeaves(
	  ServerWorld world, BlockPos pos
	) {
		final BlockState bs = world.getBlockState(pos);
		if (!(bs.getBlock() == ModBlocks.BROKEN_LEAVES))
			return;
		TileEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof BrokenLeavesTileEntity))
			return;
		BrokenLeavesTileEntity te = (BrokenLeavesTileEntity) tile;
		if (te.replacedLeaves != null) {
			if (world.isUnobstructed(te.replacedLeaves, pos, ISelectionContext.empty()))
				world.setBlockAndUpdate(pos, te.replacedLeaves);
		} else {
			world.destroyBlock(pos, false);
		}
	}
	
	@Override public boolean isRandomlyTicking(@NotNull BlockState state) {
		return true;
	}
	
	@Override public void randomTick(
	  @NotNull BlockState state, @NotNull ServerWorld world,
	  @NotNull BlockPos pos, @NotNull Random random
	) {
		super.randomTick(state, world, pos, random);
		if (!world.hasNeighborSignal(pos) && world.random.nextFloat() > leave_breaking.regrow_chance) // 0.4F)
			tryRestoreBrokenLeaves(world, pos);
	}
	
	public static Optional<BlockState> getStoredBlockState(
	  IBlockDisplayReader world, BlockPos pos
	) {
		TileEntity tile = world.getBlockEntity(pos);
		if (!(tile instanceof BrokenLeavesTileEntity))
			return Optional.empty();
		BrokenLeavesTileEntity te = (BrokenLeavesTileEntity) tile;
		return te.replacedLeaves != null? Optional.of(te.replacedLeaves) : Optional.empty();
	}
}
