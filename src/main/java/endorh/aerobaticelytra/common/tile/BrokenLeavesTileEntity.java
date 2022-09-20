package endorh.aerobaticelytra.common.tile;

import endorh.aerobaticelytra.client.block.BrokenLeavesBlockModel;
import endorh.aerobaticelytra.common.block.BrokenLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Tile Entity for {@link BrokenLeavesBlock}
 * used to store the leaves block it replaces.
 *
 * @see BrokenLeavesBlockModel
 */
public class BrokenLeavesTileEntity extends BlockEntity {
	public static final String NAME = "broken_leaves";
	public static final String TAG_REPLACED_LEAVES = "ReplacedLeaves";
	
	private BlockState replacedLeaves = null;
	
	public BrokenLeavesTileEntity(BlockPos pos, BlockState state) {
		super(AerobaticTileEntities.BROKEN_LEAVES_TE, pos, state);
	}
	
	@Override public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
		return new ClientboundBlockEntityDataPacket(worldPosition, 0, getUpdateTag());
	}
	
	@Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
		super.onDataPacket(net, pkt);
		CompoundTag updateNBT = pkt.getTag();
		setReplacedLeaves(NbtUtils.readBlockState(updateNBT.getCompound(TAG_REPLACED_LEAVES)));
		final BlockState state = getBlockState();
		assert level != null;
		level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL_IMMEDIATE);
	}
	
	@Override public void onLoad() {
		super.onLoad();
		final CompoundTag nbt = getTileData();
		if (nbt.contains(TAG_REPLACED_LEAVES))
			setReplacedLeaves(NbtUtils.readBlockState(nbt.getCompound(TAG_REPLACED_LEAVES)));
	}
	
	@Override public @NotNull CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		if (getReplacedLeaves() != null)
			tag.put(TAG_REPLACED_LEAVES, NbtUtils.writeBlockState(getReplacedLeaves()));
		return tag;
	}
	
	@Override public void handleUpdateTag(CompoundTag tag) {
		super.handleUpdateTag(tag);
		if (tag.contains(TAG_REPLACED_LEAVES))
			setReplacedLeaves(NbtUtils.readBlockState(tag.getCompound(TAG_REPLACED_LEAVES)));
	}
	
	public BlockState getReplacedLeaves() {
		if (replacedLeaves == null) replacedLeaves = NbtUtils.readBlockState(
		  getTileData().getCompound(TAG_REPLACED_LEAVES));
		return replacedLeaves;
	}
	
	public void setReplacedLeaves(BlockState replacedLeaves) {
		this.replacedLeaves = replacedLeaves;
		getTileData().put(TAG_REPLACED_LEAVES, NbtUtils.writeBlockState(replacedLeaves));
	}
}