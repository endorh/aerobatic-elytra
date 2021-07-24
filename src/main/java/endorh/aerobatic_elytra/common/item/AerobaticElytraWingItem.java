package endorh.aerobatic_elytra.common.item;

import endorh.aerobatic_elytra.common.block.BrokenLeavesBlock;
import endorh.aerobatic_elytra.common.block.ModBlocks;
import endorh.aerobatic_elytra.common.capability.IAerobaticData;
import endorh.aerobatic_elytra.common.flight.AerobaticFlight.VectorBase;
import endorh.aerobatic_elytra.common.item.ElytraDyement.WingSide;
import endorh.aerobatic_elytra.common.capability.ElytraSpecCapability;
import endorh.aerobatic_elytra.common.capability.IElytraSpec;
import endorh.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import endorh.util.math.Vec3f;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static endorh.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpec;
import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.util.common.TextUtil.stc;

public class AerobaticElytraWingItem extends Item implements IDyeableArmorItem {
	public static final String NAME = "aerobatic_elytra_wing";
	
	public AerobaticElytraWingItem() {
		this(new Properties());
	}
	
	public AerobaticElytraWingItem(Properties builder) {
		super(
		  builder
		    //.group(ItemGroup.MISC)
		);
		setRegistryName(NAME);
	}
	
	public static boolean canUseDebugWing(PlayerEntity player) {
		return player.isCreative();
	}
	
	/**
	 * Check if the player can use the Debug Wing and has it held in any hand
	 */
	public static boolean hasDebugWing(PlayerEntity player) {
		return canUseDebugWing(player) &&
		       (isDebugWing(player.getHeldItem(Hand.MAIN_HAND))
		        || isDebugWing(player.getHeldItem(Hand.OFF_HAND)));
	}
	
	/**
	 * Check if the player can use the Debug Wing and has it held in the offhand
	 */
	public static boolean hasOffhandDebugWing(PlayerEntity player) {
		return canUseDebugWing(player) && isDebugWing(player.getHeldItem(Hand.OFF_HAND));
	}
	
	public static boolean isDebugWing(ItemStack stack) {
		return stack.getItem() == ModItems.AEROBATIC_ELYTRA_WING
		  && "Debug Wing".equals(stack.getDisplayName().getString());
	}
	
	/**
	 * If in creative mode and named "Debug Wing", used to test
	 * the broken leaves block.
	 */
	@Override
	public @NotNull ActionResultType onItemUse(ItemUseContext context) {
		ItemStack stack = context.getItem();
		PlayerEntity player = context.getPlayer();
		if (isDebugWing(stack) && player != null && canUseDebugWing(player)) {
			BlockPos pos = context.getPos();
			World world = context.getWorld();
			final Block block = world.getBlockState(pos).getBlock();
			if (block.isIn(BlockTags.LEAVES)) {
				BrokenLeavesBlock.breakLeaves(world, pos);
			} else if (block == ModBlocks.BROKEN_LEAVES && context.isInside()) {
				BlockPos next = pos.subtract(context.getFace().getDirectionVec());
				if (world.getBlockState(next).getBlock().isIn(BlockTags.LEAVES)) {
					BrokenLeavesBlock.breakLeaves(world, next);
					final BlockPos down = next.down();
					if (world.getBlockState(down).getBlock().isIn(BlockTags.LEAVES))
						BrokenLeavesBlock.breakLeaves(world, down);
				}
			}
			return ActionResultType.CONSUME;
		}
		return ActionResultType.PASS;
	}
	
	@Override
	public @NotNull ActionResult<ItemStack> onItemRightClick(
	  @NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.isFlying() && hasDebugWing(player)) {
			if (player.isSneaking()) {
				data.setTiltPitch(0F);
				data.setTiltRoll(0F);
				data.setTiltYaw(0F);
			} else {
				VectorBase base = data.getRotationBase();
				final Vec3f ax = Vec3f.forAxis(Axis.X);
				base.mirror(ax);
				Vec3f motionVec = new Vec3f(player.getMotion());
				motionVec.reflect(ax);
				player.setMotion(motionVec.toVector3d());
				data.setLastBounceTime(System.currentTimeMillis());
				data.getPreBounceBase().set(data.getCameraBase());
				data.getPosBounceBase().set(base);
			}
			return ActionResult.resultConsume(player.getHeldItem(hand));
		}
		return super.onItemRightClick(world, player, hand);
	}
	
	@Override
	public boolean hasEffect(@NotNull ItemStack stack) {
		return super.hasEffect(stack) || isDebugWing(stack);
	}
	
	@Override public @NotNull ITextComponent getDisplayName(@NotNull ItemStack stack) {
		IFormattableTextComponent name = super.getDisplayName(stack).copyRaw();
		return "Debug Wing".equals(name.getString())? name.mergeStyle(TextFormatting.OBFUSCATED).mergeStyle(
		  TextFormatting.LIGHT_PURPLE) : name.mergeStyle(TextFormatting.BLUE);
	}
	
	@Override public int getMaxDamage(ItemStack stack) {
		//noinspection ConstantConditions
		if (ModItems.AEROBATIC_ELYTRA != null)
			return ModItems.AEROBATIC_ELYTRA.getMaxDamage(stack);
		return super.getMaxDamage(stack);
	}
	
	@Override public int getColor(ItemStack stack) {
		CompoundNBT display = stack.getChildTag("display");
		if (display != null) {
			return display.contains("color", 99) ? display.getInt("color") : AerobaticElytraItem.DEFAULT_COLOR;
		}
		display = stack.getChildTag("BlockEntityTag");
		if (display != null) {
			return DyeColor.byId(display.getInt("Base")).getColorValue();
		}
		return AerobaticElytraItem.DEFAULT_COLOR;
	}
	
	@Override
	public boolean hasColor(ItemStack stack) {
		CompoundNBT tag = stack.getChildTag("BlockEntityTag");
		return IDyeableArmorItem.super.hasColor(stack) || tag != null;
	}
	
	@Override
	public void removeColor(@NotNull ItemStack stack) {
		IDyeableArmorItem.super.removeColor(stack);
		CompoundNBT tag = stack.getChildTag("BlockEntityTag");
		if (tag != null) {
			stack.removeChildTag("BlockEntityTag");
		}
	}
	
	@Override
	public void addInformation(
	  @NotNull ItemStack stack, @Nullable World world, @NotNull List<ITextComponent> tooltip, @NotNull ITooltipFlag flag
	) {
		tooltip.addAll(getTooltipInfo(stack, flag));
		
		if (!stack.getEnchantmentTagList().isEmpty())
			tooltip.add(stc("")); // Separator
	}
	
	
	public List<ITextComponent> getTooltipInfo(ItemStack stack, ITooltipFlag flag) {
		return getTooltipInfo(stack, flag, "");
	}
	
	public List<ITextComponent> getTooltipInfo(ItemStack stack, ITooltipFlag flag, String indent) {
		List<ITextComponent> tooltip = new ArrayList<>();
		AerobaticElytraItem aerobaticElytra = ModItems.AEROBATIC_ELYTRA;
		//noinspection ConstantConditions
		assert aerobaticElytra != null;
		aerobaticElytra.addFuelTooltipInfo(tooltip, stack, flag, indent);
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		spec.addAbilityTooltipInfo(tooltip, indent);
		
		aerobaticElytra.addDyementTooltipInfo(stack, flag, indent, tooltip);
		
		TrailData.addTooltipInfo(tooltip, spec.getTrailData(), WingSide.LEFT, indent);
		
		return tooltip;
	}
	
	/**
	 * Add serialized capability to the shared tag
	 */
	@Nullable @Override public CompoundNBT getShareTag(ItemStack stack) {
		CompoundNBT shareTag = new CompoundNBT();
		CompoundNBT tag = stack.getTag();
		if (tag != null) {shareTag.put("tag", tag);}
		getElytraSpec(stack).ifPresent(
		  (spec) -> shareTag.put("cap", ElytraSpecCapability.asNBT(spec)));
		return shareTag;
	}
	
	/**
	 * Read capability and NBT from the shared tag
	 */
	@Override public void readShareTag(ItemStack stack, @Nullable CompoundNBT nbt) {
		if (nbt != null) {
			stack.setTag(nbt.contains("tag") ? nbt.getCompound("tag") : null);
			if (nbt.contains("cap")) {
				getElytraSpecOrDefault(stack).copy(
				  ElytraSpecCapability.fromNBT(nbt.getCompound("cap")));
			}
		} else
			stack.setTag(null);
	}
	
	@Nullable @Override public ICapabilityProvider initCapabilities(
	  ItemStack stack, @Nullable CompoundNBT nbt
	) {
		if (nbt == null)
			return ElytraSpecCapability.createProvider();
		return ElytraSpecCapability.createProvider(
		  ElytraSpecCapability.fromNBT(nbt.getCompound("Parent")));
	}
}
