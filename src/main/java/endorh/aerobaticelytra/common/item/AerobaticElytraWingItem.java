package endorh.aerobaticelytra.common.item;

import endorh.aerobaticelytra.client.config.ClientConfig.style.visibility;
import endorh.aerobaticelytra.common.block.BrokenLeavesBlock;
import endorh.aerobaticelytra.common.block.ModBlocks;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.flight.AerobaticFlight.VectorBase;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.util.math.Vec3f;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpec;
import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;

public class AerobaticElytraWingItem extends Item implements DyeableLeatherItem {
	public static final String NAME = "aerobatic_elytra_wing";
	
	public AerobaticElytraWingItem() {
		this(new Properties());
	}
	
	public AerobaticElytraWingItem(Properties builder) {
		super(builder.tab(CreativeModeTab.TAB_MISC));
		setRegistryName(NAME);
	}
	
	public static ItemStack createDebugWing() {
		final ItemStack stack = new ItemStack(ModItems.AEROBATIC_ELYTRA_WING, 1);
		stack.setHoverName(new TextComponent("Debug Wing"));
		return stack;
	}
	
	public static boolean canUseDebugWing(Player player) {
		return player.isCreative();
	}
	
	/**
	 * Check if the player can use the Debug Wing and has it held in any hand
	 */
	public static boolean hasDebugWing(Player player) {
		return canUseDebugWing(player) &&
		       (isDebugWing(player.getItemInHand(InteractionHand.MAIN_HAND))
		        || isDebugWing(player.getItemInHand(InteractionHand.OFF_HAND)));
	}
	
	/**
	 * Check if the player can use the Debug Wing and has it held in the offhand
	 */
	public static boolean hasOffhandDebugWing(Player player) {
		return canUseDebugWing(player) && isDebugWing(player.getItemInHand(InteractionHand.OFF_HAND));
	}
	
	public static boolean isDebugWing(ItemStack stack) {
		return stack.getItem() == ModItems.AEROBATIC_ELYTRA_WING
		       && "Debug Wing".equals(stack.getHoverName().getString());
	}
	
	/**
	 * If in creative mode and named "Debug Wing", used to test
	 * the broken leaves block.
	 */
	@Override public @NotNull InteractionResult useOn(UseOnContext context) {
		Level world = context.getLevel();
		final ItemStack stack = context.getItemInHand();
		final Player player = context.getPlayer();
		if (isDebugWing(stack) && player != null && canUseDebugWing(player)) {
			final BlockPos pos = context.getClickedPos();
			final Block block = world.getBlockState(pos).getBlock();
			if (block.getTags().contains(BlockTags.LEAVES.getName())) {
				BrokenLeavesBlock.breakLeaves(world, pos);
			} else if (block == ModBlocks.BROKEN_LEAVES && context.isInside()) {
				final BlockPos next = pos.subtract(context.getClickedFace().getNormal());
				if (world.getBlockState(next).getBlock().getTags()
				  .contains(BlockTags.LEAVES.getName())) {
					BrokenLeavesBlock.breakLeaves(world, next);
					final BlockPos down = next.below();
					if (world.getBlockState(down).getBlock().getTags()
					  .contains(BlockTags.LEAVES.getName()))
						BrokenLeavesBlock.breakLeaves(world, down);
				}
			}
			return InteractionResult.sidedSuccess(world.isClientSide());
		}
		return super.useOn(context);
	}
	
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(
	  @NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand
	) {
		IAerobaticData data = getAerobaticDataOrDefault(player);
		if (data.isFlying() && hasDebugWing(player)) {
			if (player.isShiftKeyDown()) {
				data.setTiltPitch(0F);
				data.setTiltRoll(0F);
				data.setTiltYaw(0F);
			} else {
				VectorBase base = data.getRotationBase();
				final Vec3f ax = Vec3f.forAxis(Axis.X);
				base.mirror(ax);
				Vec3f motionVec = new Vec3f(player.getDeltaMovement());
				motionVec.reflect(ax);
				player.setDeltaMovement(motionVec.toVector3d());
				data.setLastBounceTime(System.currentTimeMillis());
				data.getPreBounceBase().set(data.getCameraBase());
				data.getPosBounceBase().set(base);
			}
			return InteractionResultHolder.consume(player.getItemInHand(hand));
		}
		return super.use(world, player, hand);
	}
	
	@Override
	public boolean isFoil(@NotNull ItemStack stack) {
		return super.isFoil(stack) && visibility.enchantment_glint_visibility.test()
		       || isDebugWing(stack);
	}
	
	@Override public @NotNull Component getName(@NotNull ItemStack stack) {
		MutableComponent name = super.getName(stack).plainCopy();
		return "Debug Wing".equals(name.getString())? name.withStyle(ChatFormatting.OBFUSCATED)
		  .withStyle(
			 ChatFormatting.LIGHT_PURPLE) : name.withStyle(ChatFormatting.BLUE);
	}
	
	@Override public int getMaxDamage(ItemStack stack) {
		//noinspection ConstantConditions
		if (ModItems.AEROBATIC_ELYTRA != null)
			return ModItems.AEROBATIC_ELYTRA.getMaxDamage(stack);
		return super.getMaxDamage(stack);
	}
	
	@Override public int getColor(ItemStack stack) {
		CompoundTag display = stack.getTagElement("display");
		if (display != null) {
			return display.contains("color", 99)? display.getInt("color")
			                                    : AerobaticElytraItem.DEFAULT_COLOR;
		}
		display = stack.getTagElement("BlockEntityTag");
		if (display != null) {
			return DyeColor.byId(display.getInt("Base")).getTextColor();
		}
		return AerobaticElytraItem.DEFAULT_COLOR;
	}
	
	@Override
	public boolean hasCustomColor(ItemStack stack) {
		CompoundTag tag = stack.getTagElement("BlockEntityTag");
		return DyeableLeatherItem.super.hasCustomColor(stack) || tag != null;
	}
	
	@Override
	public void clearColor(@NotNull ItemStack stack) {
		DyeableLeatherItem.super.clearColor(stack);
		CompoundTag tag = stack.getTagElement("BlockEntityTag");
		if (tag != null)
			stack.removeTagKey("BlockEntityTag");
	}
	
	@Override
	public void appendHoverText(
	  @NotNull ItemStack stack, @Nullable Level world, @NotNull List<Component> tooltip,
	  @NotNull TooltipFlag flag
	) {
		if (!isDebugWing(stack)) {
			tooltip.addAll(getTooltipInfo(stack, flag));
			if (!stack.getEnchantmentTags().isEmpty())
				tooltip.add(stc("")); // Separator
		} else tooltip.add(
		  ttc("item.aerobaticelytra.aerobatic_elytra_wing.debug_wing.tooltip")
			 .withStyle(ChatFormatting.GRAY));
	}
	
	public AerobaticElytraItem getElytraItem() {
		return ModItems.AEROBATIC_ELYTRA;
	}
	
	public List<Component> getTooltipInfo(ItemStack stack, TooltipFlag flag) {
		return getTooltipInfo(stack, flag, "");
	}
	
	public List<Component> getTooltipInfo(ItemStack stack, TooltipFlag flag, String indent) {
		List<Component> tooltip = new ArrayList<>();
		AerobaticElytraItem aerobaticElytra = getElytraItem();
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
	@Nullable @Override public CompoundTag getShareTag(ItemStack stack) {
		CompoundTag shareTag = new CompoundTag();
		CompoundTag tag = stack.getTag();
		if (tag != null) {shareTag.put("tag", tag);}
		getElytraSpec(stack).ifPresent(
		  (spec) -> shareTag.put("cap", ElytraSpecCapability.asNBT(spec)));
		return shareTag;
	}
	
	/**
	 * Read capability and NBT from the shared tag
	 */
	@Override public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
		if (nbt != null) {
			stack.setTag(nbt.contains("tag")? nbt.getCompound("tag") : null);
			if (nbt.contains("cap")) {
				getElytraSpecOrDefault(stack).copy(
				  ElytraSpecCapability.fromNBT(nbt.getCompound("cap")));
			}
		} else
			stack.setTag(null);
	}
	
	@Nullable @Override public ICapabilityProvider initCapabilities(
	  ItemStack stack, @Nullable CompoundTag nbt
	) {
		if (nbt == null)
			return ElytraSpecCapability.createProvider();
		return ElytraSpecCapability.createProvider(
		  ElytraSpecCapability.fromNBT(nbt.getCompound("Parent")));
	}
}
