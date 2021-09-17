package endorh.aerobatic_elytra.common.item;

import com.mojang.datafixers.util.Pair;
import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.client.config.ClientConfig;
import endorh.aerobatic_elytra.client.config.ClientConfig.style.visibility;
import endorh.aerobatic_elytra.common.capability.ElytraSpecCapability;
import endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.Storage;
import endorh.aerobatic_elytra.common.capability.IAerobaticData;
import endorh.aerobatic_elytra.common.capability.IElytraSpec;
import endorh.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobatic_elytra.common.capability.IFlightData;
import endorh.aerobatic_elytra.common.config.Config;
import endorh.aerobatic_elytra.common.flight.mode.FlightModeTags;
import endorh.aerobatic_elytra.common.item.ElytraDyement.WingDyement;
import endorh.aerobatic_elytra.common.item.ElytraDyement.WingSide;
import endorh.aerobatic_elytra.common.recipe.CreativeTabAbilitySetRecipe;
import endorh.aerobatic_elytra.common.recipe.RepairRecipe;
import endorh.aerobatic_elytra.common.recipe.SplitRecipe;
import endorh.util.common.ColorUtil;
import endorh.util.nbt.NBTPath;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.IArmorVanishable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
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

import static endorh.aerobatic_elytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpec;
import static endorh.aerobatic_elytra.common.capability.ElytraSpecCapability.getElytraSpecOrDefault;
import static endorh.aerobatic_elytra.common.capability.FlightDataCapability.getFlightData;
import static endorh.aerobatic_elytra.common.item.IAbility.Ability.FUEL;
import static endorh.aerobatic_elytra.common.item.IAbility.Ability.MAX_FUEL;
import static endorh.util.common.ForgeUtil.getSerializedCaps;
import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;
import static endorh.util.text.TooltipUtil.shiftToExpand;
import static java.lang.Math.*;
import static net.minecraft.util.math.MathHelper.hsvToRGB;
import static net.minecraft.util.math.MathHelper.lerp;

public class AerobaticElytraItem extends ElytraItem implements IArmorVanishable, IDyeableArmorItem {
	public AerobaticElytraItem() {
		this(new Item.Properties());
	}
	public static final String NAME = "aerobatic_elytra";
	@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
	public static int DEFAULT_COLOR = 0x8F9EAE;
	protected final ElytraDyement dyement = new ElytraDyement();
	
	public AerobaticElytraItem(Item.Properties builder) {
		super(
		  builder
		    //.group(ItemGroup.TRANSPORTATION)
		    .maxDamage(432 * 3)
		    .rarity(Rarity.RARE));
		setRegistryName(NAME);
		DispenserBlock.registerDispenseBehavior(this, ArmorItem.DISPENSER_BEHAVIOR);
	}
	
	@Override public void fillItemGroup(
	  @NotNull ItemGroup group, @NotNull NonNullList<ItemStack> items) {
		super.fillItemGroup(group, items);
		if (group == ItemGroup.TRANSPORTATION || group == ItemGroup.SEARCH) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () ->
			  () -> fillItemGroup(group.getPath(), items));
		}
	}
	
	public void fillItemGroup(
	  String groupLabel, NonNullList<ItemStack> items
	) {
		final ClientWorld world = Minecraft.getInstance().world;
		if (world == null)
			return;
		//noinspection unchecked
		final List<CreativeTabAbilitySetRecipe> abilitySets =
		  (List<CreativeTabAbilitySetRecipe>) (List<?>) world.getRecipeManager()
			 .getRecipes().stream().filter(
				recipe -> recipe instanceof CreativeTabAbilitySetRecipe
			 ).collect(Collectors.toList());
		for (CreativeTabAbilitySetRecipe abilitySet : abilitySets) {
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
	
	@Override public boolean isDamageable() {
		return !Config.item.undamageable;
	}
	
	// Display info
	public boolean shouldFuelReplaceDurability(ItemStack stack) {
		return shouldFuelReplaceDurability(stack, getElytraSpecOrDefault(stack));
	}
	
	public boolean shouldFuelReplaceDurability(ItemStack stack, IElytraSpec spec) {
		return (visibility.fuel_display == ClientConfig.FuelDisplay.DURABILITY_BAR
		        || visibility.fuel_display == ClientConfig.FuelDisplay.DURABILITY_BAR_IF_LOWER
		           && spec.getAbility(FUEL) / spec.getAbility(MAX_FUEL) < 1F - (float)stack.getDamage() / stack.getMaxDamage())
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
		return spec.getAbility(MAX_FUEL) == 0 ? 0F : spec.getAbility(FUEL) / spec.getAbility(MAX_FUEL);
	}
	
	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		if (shouldFuelReplaceDurability(stack, spec)) {
			return spec.getAbility(MAX_FUEL) == 0 || spec.getAbility(FUEL) < spec.getAbility(MAX_FUEL);
		} else return super.showDurabilityBar(stack);
	}
	
	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		if (shouldFuelReplaceDurability(stack, spec)) {
			return 1F - getFuelFraction(stack, spec);
		} else return super.getDurabilityForDisplay(stack);
	}
	
	@Override
	public int getRGBDurabilityForDisplay(ItemStack stack) {
		if (shouldFuelReplaceDurability(stack)) {
			return hsvToRGB(lerp(
			  1F - getFuelFraction(stack), 0.58F, 0.7F), 0.8F, 1F);
		} else return super.getRGBDurabilityForDisplay(stack);
	}
	
	/**
	 * Adds information to the tooltip
	 */
	@Override
	public void addInformation(
	  @NotNull ItemStack stack, @Nullable World world,
	  @NotNull List<ITextComponent> tooltip, @NotNull ITooltipFlag flag) {
		tooltip.addAll(getTooltipInfo(stack, flag));
		
		if (!stack.getEnchantmentTagList().isEmpty())
			tooltip.add(stc("")); // Separator
	}
	
	public List<ITextComponent> getTooltipInfo(ItemStack stack, ITooltipFlag flag) {
		return getTooltipInfo(stack, flag, "");
	}
	
	public List<ITextComponent> getTooltipInfo(ItemStack stack, ITooltipFlag flag, String indent) {
		List<ITextComponent> tooltip = new ArrayList<>();
		
		addFuelTooltipInfo(tooltip, stack, flag, indent);
		
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		spec.addAbilityTooltipInfo(tooltip, indent);
		
		addDyementTooltipInfo(stack, flag, indent, tooltip);
		
		TrailData.addTooltipInfo(tooltip, spec.getTrailData(), indent);
		
		return tooltip;
	}
	
	public void addFuelTooltipInfo(
	  List<ITextComponent> tooltip, ItemStack stack, ITooltipFlag flag, String indent
	) {
		IElytraSpec spec = getElytraSpecOrDefault(stack);
		tooltip.add(
		  stc(indent).append(
		    ttc("aerobatic-elytra.item.fuel",
		        stc(String.format("%.1f", spec.getAbility(FUEL)))
		          .mergeStyle(TextFormatting.AQUA),
		        String.format("%.1f", spec.getAbility(MAX_FUEL)))
		      .mergeStyle(TextFormatting.GRAY))
		);
		if (!flag.isAdvanced()) {
			tooltip.add(
			  stc(indent).append(
			    ttc("item.durability",
			        stc(String.format("%d", getMaxDamage(stack) - getDamage(stack)))
			          .mergeStyle(TextFormatting.GOLD),
			        String.format("%d", getMaxDamage(stack)))
			      .mergeStyle(TextFormatting.GRAY))
			);
		}
	}
	
	@NotNull @Override public ITextComponent getDisplayName(@NotNull ItemStack stack) {
		return ttc(getTranslationKey(stack)).mergeStyle(TextFormatting.DARK_AQUA);
	}
	
	@Override
	public boolean hasEffect(@NotNull ItemStack stack) {
		return super.hasEffect(stack) && visibility.enchantment_glint_visibility.test();
	}
	
	public boolean hasModelEffect(@NotNull ItemStack stack) {
		return super.hasEffect(stack);
	}
	
	// Behaviour
	
	@Nullable @Override
	public EquipmentSlotType getEquipmentSlot(ItemStack stack) {
		return EquipmentSlotType.CHEST;
	}
	
	@Override public boolean getIsRepairable(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
		return RepairRecipe.getRepairRecipes().stream().anyMatch(r -> r.ingredient.test(repair));
	}
	
	/**
	 * Equips the elytra
	 */
	@NotNull public ActionResult<ItemStack> onItemRightClick(
	  @NotNull World world, PlayerEntity player, @NotNull Hand hand
	) {
		ItemStack itemStack = player.getHeldItem(hand);
		EquipmentSlotType equipmentSlotType = MobEntity.getSlotForItemStack(itemStack);
		ItemStack equippedStack = player.getItemStackFromSlot(equipmentSlotType);
		if (equippedStack.isEmpty()) {
			player.setItemStackToSlot(equipmentSlotType, itemStack.copy());
			itemStack.setCount(0);
			return ActionResult.func_233538_a_(itemStack, world.isRemote());
		} else {
			return ActionResult.resultFail(itemStack);
		}
	}
	
	/**
	 * Clean banner on filled cauldron<br>
	 * The dye is already handled by the {@link IDyeableArmorItem} interface
	 */
	@NotNull @Override
	public ActionResultType onItemUse(ItemUseContext context) {
		World world = context.getWorld();
		if (ElytraDyement.clearDyesWithCauldron(context))
			return ActionResultType.func_233537_a_(world.isRemote());
		return super.onItemUse(context);
	}
	
	// Elytra stuff
	
	@Override
	public boolean canElytraFly(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
		if (entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity)entity;
			Optional<IFlightData> dat = getFlightData(player);
			if (!dat.isPresent())
				return false;
			IFlightData fd = dat.get();
			if (!fd.getFlightMode().is(FlightModeTags.ELYTRA))
				return false;
			if (player.isCreative())
				return true;
		}
		return stack.getDamage() < stack.getMaxDamage() - 1;
		//return AerobaticElytraLogic.canFallFly(stack, entity);
	}
	
	@Override
	public boolean elytraFlightTick(@NotNull ItemStack stack, LivingEntity entity, int flightTicks) {
		if (!entity.world.isRemote && (flightTicks + 1) % 20 == 0 && !Config.item.undamageable)
			stack.damageItem(1, entity, e -> e.sendBreakAnimation(EquipmentSlotType.CHEST));
		if (entity instanceof PlayerEntity) {
			IAerobaticData data = getAerobaticDataOrDefault((PlayerEntity) entity);
			if (data.isFlying() && !((PlayerEntity) entity).isCreative()) {
				float rel_prop = abs(data.getPropulsionStrength()) /
				                 max(abs(Config.aerobatic.propulsion.max_tick),
				                     abs(Config.aerobatic.propulsion.min_tick));
				float fuel_usage = rel_prop * Config.fuel.usage_linear_tick +
				                   rel_prop * rel_prop * Config.fuel.usage_quad_tick +
				                   MathHelper.sqrt(rel_prop) * Config.fuel.usage_sqrt_tick;
				if (data.isBoosted())
					fuel_usage *= Config.fuel.usage_boost_multiplier;
				IElytraSpec spec = getElytraSpecOrDefault(stack);
				spec.setAbility(FUEL, max(0F, min(spec.getAbility(MAX_FUEL), spec.getAbility(FUEL) - fuel_usage)));
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
	public CompoundNBT getShareTag(ItemStack stack) {
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
	@Override
	public void readShareTag(ItemStack stack, @Nullable CompoundNBT nbt) {
		if (nbt != null) {
			stack.setTag(nbt.contains("tag") ? nbt.getCompound("tag") : null);
			if (nbt.contains("cap")) {
				getElytraSpecOrDefault(stack).copy(
				  ElytraSpecCapability.fromNBT(nbt.getCompound("cap")));
			}
		} else
			stack.setTag(null);
	}
	
	@Nullable @Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
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
		return new ResourceLocation(AerobaticElytra.MOD_ID, "entity/aerobatic_elytra/" + pattern.getFileName());
	}
	
	@OnlyIn(Dist.CLIENT)
	public void addDyementTooltipInfo(
	  ItemStack stack, ITooltipFlag flag, String indent, List<ITextComponent> tooltip
	) {
		dyement.read(stack, DEFAULT_COLOR);
		
		if (dyement.hasWingDyement) {
			for (WingSide side : WingSide.values()) {
				WingDyement wingDye = dyement.getWing(side);
				if (wingDye.hasPattern) {
					addBannerTooltipInfo(tooltip, wingDye, "aerobatic-elytra.side." + side.tag, indent);
				} else {
					addColorTooltipInfo(
					  tooltip, "aerobatic-elytra.side.color." + side.tag,
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
	  List<ITextComponent> tooltip, String key, WingDyement wing,
	  ITooltipFlag flag, String indent
	) {
		if (wing.hasColor) {
			Color color = new Color(wing.color);
			ITextComponent colorName =
			  flag.isAdvanced()
			  ? stc(String.format("#%6h", color)).mergeStyle(TextFormatting.GRAY)
			  : ColorUtil.closestDyeColor(color)
				 .map(dyeColor -> ttc("color.minecraft." + dyeColor.getTranslationKey()))
				 .orElseGet(() -> ttc("item.minecraft.firework_star.custom_color"))
				 .mergeStyle(TextFormatting.GRAY);
			tooltip.add(
			  stc(indent).append(
				 ttc(key, stc("≈").append(colorName)
				   .mergeStyle(ColorUtil.discardBlack(
				     ColorUtil.closestTextColor(color)
				       .orElse(TextFormatting.GRAY))))
			  ).mergeStyle(TextFormatting.GRAY));
		} else {
			tooltip.add(
			  stc(indent).append(
				 ttc(key, ttc("gui.none").mergeStyle(TextFormatting.DARK_GRAY))
			  ).mergeStyle(TextFormatting.GRAY));
		}
	}
	
	public void addBannerTooltipInfo(
	  List<ITextComponent> tooltip, WingDyement wing, String key, String indent) {
		List<Pair<BannerPattern, DyeColor>> layers = wing.patternColorData;
		ITextComponent sideParenthesis = key != null
		  ? stc(" (").append(ttc(key)).appendString(")") : stc("");
		if (layers.size() == 1) {
			tooltip.add(
			  stc(indent).append(
			    ttc("block.minecraft." + wing.basePatternColor.getTranslationKey() + "_banner")
			  ).append(sideParenthesis).mergeStyle(TextFormatting.GRAY));
		} else if (Screen.hasShiftDown()) {
			tooltip.add(
			  stc(indent).append(
				 ttc("block.minecraft." + wing.basePatternColor.getTranslationKey() + "_banner")
			  ).append(sideParenthesis).appendString(": ").append(shiftToExpand()).mergeStyle(TextFormatting.GRAY));
			String extraIndent = indent + "  ";
			for (int i = 1; i < wing.patternColorData.size(); i++) {
				BannerPattern pattern = wing.patternColorData.get(i).getFirst();
				DyeColor color = wing.patternColorData.get(i).getSecond();
				tooltip.add(
				  stc(extraIndent).append(
					 ttc("block.minecraft.banner."
					     + pattern.getFileName() + '.'
					     + color.getTranslationKey())
						.mergeStyle(TextFormatting.GRAY)
				  ));
			}
		} else {
			tooltip.add(
			  stc(indent).append(
				 ttc("block.minecraft." + wing.basePatternColor.getTranslationKey() + "_banner")
				   .append(sideParenthesis)
					.appendString(": ")
					.append(shiftToExpand())
			  ).mergeStyle(TextFormatting.GRAY));
		}
	}
	
	// Split wings
	@SuppressWarnings("unused")
	public AerobaticElytraWingItem getWingItem(ItemStack elytra, WingSide side) {
		return ModItems.AEROBATIC_ELYTRA_WING;
	}
	
	public ItemStack getWing(ItemStack elytra, WingSide side) {
		final CompoundNBT elytraCaps = getSerializedCaps(elytra);
		ItemStack wing = new ItemStack(getWingItem(elytra, side), 1, elytraCaps.copy());
		new NBTPath("Parent." + Storage.TAG_BASE + "." + Storage.TAG_TRAIL).delete(elytraCaps);
		dyement.read(elytra);
		dyement.getWing(side).write(wing, null);
		final CompoundNBT elytraTag = elytra.getOrCreateTag().copy();
		elytraTag.remove("BlockEntityTag");
		elytraTag.remove("WingInfo");
		final CompoundNBT wingTag = wing.getOrCreateTag();
		wingTag.put(SplitRecipe.TAG_SPLIT_ELYTRA, elytraTag);
		wingTag.put(SplitRecipe.TAG_SPLIT_ELYTRA_CAPS, elytraCaps);
		if (elytraTag.contains("Enchantments", 9))
			wingTag.put("Enchantments", elytraTag.getList("Enchantments", 10));
		getElytraSpecOrDefault(wing).getTrailData().keep(side);
		return wing;
	}
}
