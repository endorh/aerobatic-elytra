package endorh.aerobaticelytra.common.item;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visibility;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability;
import endorh.aerobaticelytra.common.capability.ElytraSpecCapability.ElytraSpec;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import endorh.aerobaticelytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobaticelytra.common.capability.IFlightData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.aerobatic.propulsion;
import endorh.aerobaticelytra.common.flight.mode.FlightModeTags;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobaticelytra.common.item.ElytraDyement.WingSide;
import endorh.aerobaticelytra.common.item.IAbility.Ability;
import endorh.aerobaticelytra.common.recipe.CreativeTabAbilitySetRecipe;
import endorh.aerobaticelytra.common.recipe.RepairRecipe;
import endorh.aerobaticelytra.common.recipe.SplitRecipe;
import endorh.util.common.ColorUtil;
import endorh.util.nbt.NBTPath;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpec;
import static endorh.aerobaticelytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightData;
import static endorh.util.common.ForgeUtil.getSerializedCaps;
import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;
import static endorh.util.text.TooltipUtil.shiftToExpand;
import static java.lang.Math.*;

public class AerobaticElytraItem extends ElytraItem implements Wearable, DyeableLeatherItem {
	public AerobaticElytraItem() {
		this(new Item.Properties());
	}
	
	public static final String NAME = "aerobatic_elytra";
	public static int DEFAULT_COLOR = 0x8F9EAE;
	protected final ElytraDyement dyement = new ElytraDyement();
	
	public AerobaticElytraItem(Item.Properties builder) {
		super(
		  builder
			 //.group(ItemGroup.TRANSPORTATION)
			 .durability(432 * 3)
			 .rarity(Rarity.RARE));
		setRegistryName(NAME);
		DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
	}
	
	@Override public void fillItemCategory(
	  @NotNull CreativeModeTab group, @NotNull NonNullList<ItemStack> items
	) {
		super.fillItemCategory(group, items);
		if (group == CreativeModeTab.TAB_TRANSPORTATION || group == CreativeModeTab.TAB_SEARCH) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
			  () -> fillItemGroup(group.getRecipeFolderName(), items));
		}
	}
	
	public void fillItemGroup(
	  String groupLabel, NonNullList<ItemStack> items
	) {
		final ClientLevel world = Minecraft.getInstance().level;
		if (world == null)
			return;
		//noinspection unchecked
		final List<CreativeTabAbilitySetRecipe> abilitySets =
		  (List<CreativeTabAbilitySetRecipe>) (List<?>) world.getRecipeManager()
		    .getRecipes().stream().filter(
			   recipe -> recipe instanceof CreativeTabAbilitySetRecipe
		    ).toList();
		for (CreativeTabAbilitySetRecipe abilitySet: abilitySets) {
			if (groupLabel.equals(abilitySet.group) || groupLabel.equals("search")) {
				items.add(abilitySet.stack);
			}
		}
	}
	
	@Override public int getDamage(ItemStack stack) {
		return min(super.getDamage(stack), getMaxDamage(stack) - 1);
	}
	
	@Override public int getMaxDamage(ItemStack stack) {
		return Config.item.durability;
	}
	
	@Override public boolean canBeDepleted() {
		return !Config.item.undamageable;
	}
	
	// Display info
	public boolean shouldFuelReplaceDurability(ItemStack stack) {
		return shouldFuelReplaceDurability(stack, getElytraSpecOrDefault(stack));
	}
	
	public boolean shouldFuelReplaceDurability(ItemStack stack, IElytraSpec spec) {
		return (visibility.fuel_display == ClientConfig.FuelDisplay.DURABILITY_BAR
		        || visibility.fuel_display == ClientConfig.FuelDisplay.DURABILITY_BAR_IF_LOWER
		           && spec.getAbility(Ability.FUEL) / spec.getAbility(Ability.MAX_FUEL) <
		              1F - (float) stack.getDamageValue() / stack.getMaxDamage())
		       && visibility.fuel_visibility.test();
	}
	
	@SuppressWarnings("unused")
	public boolean shouldFuelRenderOverRockets(ItemStack stack) {
		return visibility.fuel_display == ClientConfig.FuelDisplay.ROCKETS;
	}
	
	public float getFuelFraction(ItemStack stack) {
		return getFuelFraction(stack, getElytraSpecOrDefault(stack));
	}
	
	@SuppressWarnings("unused")
	public float getFuelFraction(ItemStack stack, IElytraSpec spec) {
		return spec.getAbility(Ability.MAX_FUEL) == 0? 0F : spec.getAbility(Ability.FUEL) /
		                                                    spec.getAbility(
		                                                      Ability.MAX_FUEL);
	}
	
	@Override
	public boolean isBarVisible(@NotNull ItemStack stack) {
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		if (shouldFuelReplaceDurability(stack, spec)) {
			return spec.getAbility(Ability.MAX_FUEL) == 0 ||
			       spec.getAbility(Ability.FUEL) < spec.getAbility(
			         Ability.MAX_FUEL);
		} else return super.isBarVisible(stack);
	}
	
	@Override
	public int getBarWidth(@NotNull ItemStack stack) {
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		if (shouldFuelReplaceDurability(stack, spec)) {
			return Math.round(getFuelFraction(stack, spec) * 13F);
		} else return super.getBarWidth(stack);
	}
	
	@Override
	public int getBarColor(@NotNull ItemStack stack) {
		if (shouldFuelReplaceDurability(stack)) {
			return Mth.hsvToRgb(Mth.lerp(
			  1F - getFuelFraction(stack), 0.58F, 0.7F), 0.8F, 1F);
		} else return super.getBarColor(stack);
	}
	
	/**
	 * Adds information to the tooltip
	 */
	@Override
	public void appendHoverText(
	  @NotNull ItemStack stack, @Nullable Level world,
	  @NotNull List<Component> tooltip, @NotNull TooltipFlag flag
	) {
		tooltip.addAll(getTooltipInfo(stack, flag));
		
		if (!stack.getEnchantmentTags().isEmpty())
			tooltip.add(stc("")); // Separator
	}
	
	public List<Component> getTooltipInfo(ItemStack stack, TooltipFlag flag) {
		return getTooltipInfo(stack, flag, "");
	}
	
	public List<Component> getTooltipInfo(ItemStack stack, TooltipFlag flag, String indent) {
		List<Component> tooltip = new ArrayList<>();
		
		addFuelTooltipInfo(tooltip, stack, flag, indent);
		
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		spec.addAbilityTooltipInfo(tooltip, indent);
		
		addDyementTooltipInfo(stack, flag, indent, tooltip);
		
		TrailData.addTooltipInfo(tooltip, spec.getTrailData(), indent);
		
		return tooltip;
	}
	
	public void addFuelTooltipInfo(
	  List<Component> tooltip, ItemStack stack, TooltipFlag flag, String indent
	) {
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		tooltip.add(
		  stc(indent).append(
			 ttc(
			   "aerobaticelytra.item.fuel",
			   stc(String.format("%.1f", spec.getAbility(Ability.FUEL)))
				  .withStyle(ChatFormatting.AQUA),
			   String.format("%.1f", spec.getAbility(Ability.MAX_FUEL)))
				.withStyle(ChatFormatting.GRAY))
		);
		if (!flag.isAdvanced()) {
			tooltip.add(
			  stc(indent).append(
				 ttc(
				   "item.durability",
				   stc(String.format("%d", getMaxDamage(stack) - getDamage(stack)))
					  .withStyle(ChatFormatting.GOLD),
				   String.format("%d", getMaxDamage(stack)))
					.withStyle(ChatFormatting.GRAY))
			);
		}
	}
	
	@NotNull @Override public Component getName(@NotNull ItemStack stack) {
		return ttc(getDescriptionId(stack)).withStyle(ChatFormatting.DARK_AQUA);
	}
	
	@Override
	public boolean isFoil(@NotNull ItemStack stack) {
		return super.isFoil(stack) && visibility.enchantment_glint_visibility.test();
	}
	
	public boolean hasModelEffect(@NotNull ItemStack stack) {
		return super.isFoil(stack);
	}
	
	// Behaviour
	
	@Nullable @Override
	public EquipmentSlot getEquipmentSlot(ItemStack stack) {
		return EquipmentSlot.CHEST;
	}
	
	@Override
	public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
		return RepairRecipe.getRepairRecipes().stream().anyMatch(r -> r.ingredient.test(repair));
	}
	
	/**
	 * Equips the elytra
	 */
	@NotNull public InteractionResultHolder<ItemStack> use(
	  @NotNull Level world, Player player, @NotNull InteractionHand hand
	) {
		ItemStack itemStack = player.getItemInHand(hand);
		EquipmentSlot equipmentSlotType = Mob.getEquipmentSlotForItem(itemStack);
		ItemStack equippedStack = player.getItemBySlot(equipmentSlotType);
		if (equippedStack.isEmpty()) {
			player.setItemSlot(equipmentSlotType, itemStack.copy());
			itemStack.setCount(0);
			return InteractionResultHolder.sidedSuccess(itemStack, world.isClientSide());
		} else {
			return InteractionResultHolder.fail(itemStack);
		}
	}
	
	// /**
	//  * Clean banner on filled cauldron<br>
	//  * The dye is already handled by the {@link DyeableLeatherItem} interface
	//  */
	// @NotNull @Override
	// public InteractionResult useOn(UseOnContext context) {
	// 	Level world = context.getLevel();
	// 	if (ElytraDyement.clearDyesWithCauldron(context))
	// 		return InteractionResult.sidedSuccess(world.isClientSide());
	// 	return super.useOn(context);
	// }
	
	// Elytra stuff
	
	@Override
	public boolean canElytraFly(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
		if (entity instanceof Player player) {
			Optional<IFlightData> dat = getFlightData(player);
			if (dat.isEmpty())
				return false;
			IFlightData fd = dat.get();
			if (!fd.getFlightMode().is(FlightModeTags.ELYTRA))
				return false;
			if (player.isCreative())
				return true;
		}
		return stack.getDamageValue() < stack.getMaxDamage() - 1;
	}
	
	@Override
	public boolean elytraFlightTick(@NotNull ItemStack stack, LivingEntity entity, int flightTicks) {
		if (!entity.level.isClientSide && (flightTicks + 1) % 20 == 0 && !Config.item.undamageable)
			stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(EquipmentSlot.CHEST));
		if (entity instanceof Player) {
			IAerobaticData data = getAerobaticDataOrDefault((Player) entity);
			if (data.isFlying() && !((Player) entity).isCreative()) {
				float rel_prop = abs(data.getPropulsionStrength()) /
				                 max(
				                   abs(propulsion.range_tick.getFloatMax()),
				                   abs(propulsion.range_tick.getFloatMin()));
				float fuel_usage = rel_prop * Config.fuel.usage_linear_tick +
				                   rel_prop * rel_prop * Config.fuel.usage_quad_tick +
				                   Mth.sqrt(rel_prop) * Config.fuel.usage_sqrt_tick;
				if (data.isBoosted())
					fuel_usage *= Config.fuel.usage_boost_multiplier;
				IElytraSpec spec = getElytraSpecOrDefault(stack);
				spec.setAbility(
				  Ability.FUEL, max(0F, min(spec.getAbility(Ability.MAX_FUEL), spec.getAbility(
					 Ability.FUEL) - fuel_usage)));
			}
		}
		return true;
	}
	
	// Capabilities handling
	
	// FIXME: Solve issue of CCreativeInventoryActionPacket not encoding ItemStack capabilities
	//        on writePacketData. Possibly requires changes to PacketBuffer#writeItemStack
	//        and possibly PacketBuffer#readItemStack and IForgeItem.
	//        Currently, item capabilities are reset on any Creative Inventory actions
	//        (including moving items) on multiplayer worlds.
	
	/**
	 * Add serialized capability to the shared tag
	 */
	@Nullable @Override
	public CompoundTag getShareTag(ItemStack stack) {
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
	@Override
	public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
		if (nbt != null) {
			stack.setTag(nbt.contains("tag")? nbt.getCompound("tag") : null);
			if (nbt.contains("cap")) {
				getElytraSpecOrDefault(stack).copy(
				  ElytraSpecCapability.fromNBT(nbt.getCompound("cap")));
			}
		} else
			stack.setTag(null);
	}
	
	@Nullable @Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		if (nbt == null)
			return ElytraSpecCapability.createProvider();
		return ElytraSpecCapability.createProvider(
		  ElytraSpecCapability.fromNBT(nbt.getCompound("Parent")));
	}
	
	// Dyes and banners
	
	@SuppressWarnings("unused")
	public boolean shouldRenderAerobaticElytraLayer(ItemStack stack, LivingEntity entity) {
		return true;
	}
	
	@Override
	public int getColor(@NotNull ItemStack stack) {
		dyement.read(stack, DEFAULT_COLOR);
		if (dyement.hasWingDyement) {
			return ColorUtil.mix(
			  dyement.getWing(WingSide.LEFT).color, dyement.getWing(WingSide.RIGHT).color);
		} else {
			return dyement.getWing(WingSide.LEFT).color;
		}
	}
	
	public ResourceLocation getTextureLocation(BannerPattern pattern) {
		return new ResourceLocation(
		  AerobaticElytra.MOD_ID, "entity/aerobatic_elytra/" + pattern.getFilename());
	}
	
	@OnlyIn(Dist.CLIENT)
	public void addDyementTooltipInfo(
	  ItemStack stack, TooltipFlag flag, String indent, List<Component> tooltip
	) {
		dyement.read(stack, DEFAULT_COLOR);
		
		if (dyement.hasWingDyement) {
			for (WingSide side: WingSide.values()) {
				WingDyement wingDye = dyement.getWing(side);
				if (wingDye.hasPattern) {
					addBannerTooltipInfo(tooltip, wingDye, "aerobaticelytra.side." + side.tag, indent);
				} else {
					addColorTooltipInfo(
					  tooltip, "aerobaticelytra.side.color." + side.tag,
					  wingDye, flag, indent);
				}
			}
		} else {
			WingDyement wingDye = dyement.getFirst();
			if (wingDye.hasPattern) {
				addBannerTooltipInfo(tooltip, wingDye, null, indent);
			} else {
				addColorTooltipInfo(tooltip, "item.color", wingDye, flag, indent);
			}
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public void addColorTooltipInfo(
	  List<Component> tooltip, String key, WingDyement wing,
	  TooltipFlag flag, String indent
	) {
		if (wing.hasColor) {
			Color color = new Color(wing.color);
			Component colorName =
			  flag.isAdvanced()
			  ? stc(String.format("#%6h", color)).withStyle(ChatFormatting.GRAY)
			  : ColorUtil.closestDyeColor(color)
				 .map(dyeColor -> ttc("color.minecraft." + dyeColor.getName()))
				 .orElseGet(() -> ttc("item.minecraft.firework_star.custom_color"))
				 .withStyle(ChatFormatting.GRAY);
			tooltip.add(
			  stc(indent).append(
				 ttc(key, stc("≈").append(colorName)
					.withStyle(ColorUtil.discardBlack(
					  ColorUtil.closestTextColor(color)
						 .orElse(ChatFormatting.GRAY))))
			  ).withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(
			  stc(indent).append(
				 ttc(key, ttc("gui.none").withStyle(ChatFormatting.DARK_GRAY))
			  ).withStyle(ChatFormatting.GRAY));
		}
	}
	
	public void addBannerTooltipInfo(
	  List<Component> tooltip, WingDyement wing, String key, String indent
	) {
		List<Pair<BannerPattern, DyeColor>> layers = wing.patternColorData;
		Component sideParenthesis = key != null
		                            ? stc(" (").append(ttc(key)).append(")") : stc("");
		if (layers.size() == 1) {
			tooltip.add(
			  stc(indent).append(
				 ttc("block.minecraft." + wing.basePatternColor.getName() + "_banner")
			  ).append(sideParenthesis).withStyle(ChatFormatting.GRAY));
		} else if (Screen.hasShiftDown()) {
			tooltip.add(
			  stc(indent).append(
				   ttc("block.minecraft." + wing.basePatternColor.getName() + "_banner")
			    ).append(sideParenthesis).append(": ").append(shiftToExpand())
			    .withStyle(ChatFormatting.GRAY));
			String extraIndent = indent + "  ";
			for (int i = 1; i < wing.patternColorData.size(); i++) {
				BannerPattern pattern = wing.patternColorData.get(i).getFirst();
				DyeColor color = wing.patternColorData.get(i).getSecond();
				tooltip.add(
				  stc(extraIndent).append(
					 ttc("block.minecraft.banner."
					     + pattern.getFilename() + '.'
					     + color.getName())
						.withStyle(ChatFormatting.GRAY)
				  ));
			}
		} else {
			tooltip.add(
			  stc(indent).append(
				 ttc("block.minecraft." + wing.basePatternColor.getName() + "_banner")
					.append(sideParenthesis)
					.append(": ")
					.append(shiftToExpand())
			  ).withStyle(ChatFormatting.GRAY));
		}
	}
	
	// Split wings
	@SuppressWarnings("unused")
	public AerobaticElytraWingItem getWingItem(ItemStack elytra, WingSide side) {
		return ModItems.AEROBATIC_ELYTRA_WING;
	}
	
	public ItemStack getWing(ItemStack elytra, WingSide side) {
		final CompoundTag elytraCaps = getSerializedCaps(elytra);
		ItemStack wing = new ItemStack(getWingItem(elytra, side), 1, elytraCaps.copy());
		new NBTPath("Parent." + ElytraSpec.TAG_BASE + "." + ElytraSpec.TAG_TRAIL).delete(elytraCaps);
		dyement.read(elytra);
		dyement.getWing(side).write(wing, null);
		final CompoundTag elytraTag = elytra.getOrCreateTag().copy();
		elytraTag.remove("BlockEntityTag");
		elytraTag.remove("WingInfo");
		final CompoundTag wingTag = wing.getOrCreateTag();
		wingTag.put(SplitRecipe.TAG_SPLIT_ELYTRA, elytraTag);
		wingTag.put(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS, elytraCaps);
		if (elytraTag.contains("Enchantments", 9))
			wingTag.put("Enchantments", elytraTag.getList("Enchantments", 10));
		getElytraSpecOrDefault(wing).getTrailData().keep(side);
		return wing;
	}
}
