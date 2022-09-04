package endorh.aerobaticelytra.common.tile;

import endorh.aerobaticelytra.client.block.BrokenLeavesBlockModel;
import endorh.aerobaticelytra.common.block.BrokenLeavesBlock;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants.BlockFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Tile Entity for {@link BrokenLeavesBlock}
 * used to store the leaves block it replaces.
 *
 * @see BrokenLeavesBlockModel
 */
public class BrokenLeavesTileEntity extends TileEntity {
	public static final String NAME = "broken_leaves";
	public static final String TAG_REPLACED_LEAVES = "ReplacedLeaves";
	
	private BlockState replacedLeaves = null;
	
	public BrokenLeavesTileEntity() {
		super(AerobaticTileEntities.BROKEN_LEAVES_TE);
	}
	
	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(pos, 0, getUpdateTag());
	}
	
	@Override public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		super.onDataPacket(net, pkt);
		CompoundNBT updateNBT = pkt.getNbtCompound();
		setReplacedLeaves(NBTUtil.readBlockState(updateNBT.getCompound(TAG_REPLACED_LEAVES)));
		final BlockState state = getBlockState();
		assert world != null;
		world.notifyBlockUpdate(pos, state, state, BlockFlags.DEFAULT_AND_RERENDER);
	}
	
	@Override public void onLoad() {
		super.onLoad();
		final CompoundNBT nbt = getTileData();
		if (nbt.contains(TAG_REPLACED_LEAVES))
			setReplacedLeaves(NBTUtil.readBlockState(nbt.getCompound(TAG_REPLACED_LEAVES)));
	}
	
	@Override public @NotNull CompoundNBT getUpdateTag() {
		CompoundNBT tag = super.getUpdateTag();
		if (getReplacedLeaves() != null)
			tag.put(TAG_REPLACED_LEAVES, NBTUtil.writeBlockState(getReplacedLeaves()));
		return tag;
	}
	
	@Override public void handleUpdateTag(BlockState state, CompoundNBT tag) {
		super.handleUpdateTag(state, tag);
		if (tag.contains(TAG_REPLACED_LEAVES))
			setReplacedLeaves(NBTUtil.readBlockState(tag.getCompound(TAG_REPLACED_LEAVES)));
	}
	
	public BlockState getReplacedLeaves() {
		if (replacedLeaves == null) replacedLeaves = NBTUtil.readBlockState(
		  getTileData().getCompound(TAG_REPLACED_LEAVES));
		return replacedLeaves;
	}
	
	public void setReplacedLeaves(BlockState replacedLeaves) {
		this.replacedLeaves = replacedLeaves;
		getTileData().put(TAG_REPLACED_LEAVES, NBTUtil.writeBlockState(replacedLeaves));
	}
}
