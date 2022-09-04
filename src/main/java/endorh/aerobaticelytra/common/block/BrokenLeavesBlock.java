package endorh.aerobaticelytra.common.block;

import endorh.aerobaticelytra.client.block.AerobaticBlockColors;
import endorh.aerobaticelytra.client.block.BrokenLeavesBlockModel;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.collision.leave_breaking;
import endorh.aerobaticelytra.common.tile.BrokenLeavesTileEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
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
import static net.minecraft.tags.BlockTags.LEAVES;

/**
 * A block that replaces temporarily other leaves blocks,
 * storing them in a Tile Entity in order to restore them
 * later, and copy their model and tint color for
 * rendering.<br>
 *
 * @see BrokenLeavesTileEntity
 * @see BrokenLeavesBlockModel
 * @see AerobaticBlockColors
 */
public class BrokenLeavesBlock extends LeavesBlock {
	public static final String NAME = "broken_leaves";
	
	public BrokenLeavesBlock() {
		super(createBlockProperties());
		setRegistryName(prefix(NAME));
	}
	
	private static AbstractBlock.Properties createBlockProperties() {
		return AbstractBlock.Properties
		  .create(Material.LEAVES).hardnessAndResistance(0.2F)
		  .sound(SoundType.PLANT).notSolid()
		  .setAllowsSpawn((state, reader, pos, type) -> false)
		  .setSuffocates((state, reader, pos) -> false)
		  .setBlocksVision((state, reader, pos) -> false)
		  .tickRandomly()
		  .doesNotBlockMovement().noDrops();
	}
	
	@Override public boolean hasTileEntity(BlockState state) {
		return true;
	}
	
	@Nullable @Override public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new BrokenLeavesTileEntity();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onEntityCollision(
	  @NotNull BlockState state, @NotNull World world, @NotNull BlockPos pos, Entity entity
	) {
		final float fallDistance = entity.fallDistance;
		entity.setMotionMultiplier(state, new Vector3d(
		  Config.collision.leave_breaking.motion_multiplier,
		  Config.collision.leave_breaking.motion_multiplier,
		  Config.collision.leave_breaking.motion_multiplier));
		if (entity.getMotion().lengthSquared() > 0.25D)
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
		if (world.isRemote) {
			world.playSound(
			  pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_GRASS_BREAK,
			  SoundCategory.BLOCKS, 1F, 1F, false);
			return;
		}
		final BlockState prevBlockState = world.getBlockState(pos);
		if (!prevBlockState.getBlock().isIn(LEAVES))
			throw new IllegalArgumentException(
			  "Attempt to replace non leaves block with broken leaves");
		final Integer dist = prevBlockState.get(DISTANCE);
		final BlockPos.Mutable cursor = new Mutable();
		for (Direction direction : Direction.values()) {
			cursor.setAndMove(pos, direction);
			final BlockState adj = world.getBlockState(cursor);
			if (adj.getBlock().isIn(LEAVES) && !adj.get(PERSISTENT)
			    && adj.get(DISTANCE) < 7 && adj.get(DISTANCE) > dist)
				breakLeaves(world, cursor);
		}
		world.setBlockState(
		  pos, AerobaticBlocks.BROKEN_LEAVES.getDefaultState()
			 .with(DISTANCE, prevBlockState.get(DISTANCE))
			 .with(PERSISTENT, prevBlockState.get(PERSISTENT)),
		  BlockFlags.DEFAULT);
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof BrokenLeavesTileEntity))
			throw new IllegalStateException(
			  "Broken leaves block did not have BrokenLeavesTileEntity");
		BrokenLeavesTileEntity te = (BrokenLeavesTileEntity) tile;
		te.replacedLeaves = prevBlockState;
		te.markDirty();
	}
	
	public void tryRestoreBrokenLeaves(
	  ServerWorld world, BlockPos pos
	) {
		final BlockState bs = world.getBlockState(pos);
		if (!(bs.getBlock() == AerobaticBlocks.BROKEN_LEAVES))
			return;
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof BrokenLeavesTileEntity))
			return;
		BrokenLeavesTileEntity te = (BrokenLeavesTileEntity) tile;
		if (te.replacedLeaves != null) {
			if (world.placedBlockCollides(te.replacedLeaves, pos, ISelectionContext.dummy()))
				world.setBlockState(pos, te.replacedLeaves);
		} else {
			world.destroyBlock(pos, false);
		}
	}
	
	@Override public boolean ticksRandomly(@NotNull BlockState state) {
		return true;
	}
	
	@Override public void randomTick(
	  @NotNull BlockState state, @NotNull ServerWorld world,
	  @NotNull BlockPos pos, @NotNull Random random
	) {
		super.randomTick(state, world, pos, random);
		if (!world.isBlockPowered(pos) && world.rand.nextFloat() > leave_breaking.regrow_chance) // 0.4F)
			tryRestoreBrokenLeaves(world, pos);
	}
	
	public static Optional<BlockState> getStoredBlockState(
	  IBlockDisplayReader world, BlockPos pos
	) {
		TileEntity tile = world.getTileEntity(pos);
		if (!(tile instanceof BrokenLeavesTileEntity))
			return Optional.empty();
		BrokenLeavesTileEntity te = (BrokenLeavesTileEntity) tile;
		return te.replacedLeaves != null? Optional.of(te.replacedLeaves) : Optional.empty();
	}
}
