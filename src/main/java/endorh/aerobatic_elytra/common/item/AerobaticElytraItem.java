package endorh.aerobatic_elytra.common.item;

import com.mojang.datafixers.util.Pair;
import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.client.config.ClientConfig;
import endorh.aerobatic_elytra.common.capability.IFlightData;
import endorh.aerobatic_elytra.common.flight.mode.FlightModeTags;
import endorh.aerobatic_elytra.common.item.ElytraDyementReader.WingDyement;
import endorh.aerobatic_elytra.common.item.ElytraDyementReader.WingSide;
import endorh.aerobatic_elytra.common.capability.ElytraSpecCapability;
import endorh.aerobatic_elytra.common.capability.IAerobaticData;
import endorh.aerobatic_elytra.common.capability.IElytraSpec;
import endorh.aerobatic_elytra.common.capability.IElytraSpec.TrailData;
import endorh.aerobatic_elytra.common.config.Config;
import endorh.aerobatic_elytra.common.recipe.CreativeTabAbilitySetRecipe;
import endorh.aerobatic_elytra.common.recipe.RepairRecipe;
import endorh.util.common.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.IArmorVanishable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.tileentity.BannerTileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
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
import static endorh.util.common.TextUtil.stc;
import static endorh.util.common.TextUtil.ttc;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static net.minecraft.util.math.MathHelper.hsvToRGB;
import static net.minecraft.util.math.MathHelper.lerp;

public class AerobaticElytraItem extends ElytraItem implements IArmorVanishable, IDyeableArmorItem {
	public AerobaticElytraItem() {
		this(new Item.Properties());
	}
	public static final String NAME = "aerobatic_elytra";
	@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
	public static int DEFAULT_COLOR = 0xBAC1DB;
	//private static final Logger LOGGER = LogManager.getLogger();
	protected static final HashMap<BannerPattern, ResourceLocation> bannerTextures = new HashMap<>();
	protected final ElytraDyementReader dyement = new ElytraDyementReader();
	
	public static void onClientSetup() {
		for (BannerPattern pattern : BannerPattern.values()) {
			bannerTextures.put(pattern, new ResourceLocation(
			  AerobaticElytra.MOD_ID, "entity/aerobatic_elytra/" + pattern.getFileName()));
		}
	}
	
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
		return (ClientConfig.fuel_display == ClientConfig.FuelDisplay.DURABILITY_BAR
		        || ClientConfig.fuel_display == ClientConfig.FuelDisplay.DURABILITY_BAR_IF_LOWER
		           && spec.getAbility(FUEL) / spec.getAbility(MAX_FUEL) < 1F - (float)stack.getDamage() / stack.getMaxDamage())
		       && ClientConfig.fuel_visibility.test();
	}
	
	public boolean shouldFuelRenderOverRockets(ItemStack stack) {
		return ClientConfig.fuel_display == ClientConfig.FuelDisplay.ROCKETS;
	}
	
	public float getFuelFraction(ItemStack stack) {
		return getFuelFraction(stack, getElytraSpecOrDefault(stack));
	}
	
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
		return super.hasEffect(stack) && ClientConfig.glint_visibility.test();
	}
	
	public boolean hasModelEffect(@NotNull ItemStack stack) {
		return super.hasEffect(stack);
	}
	
	// Behaviour
	
	@Nullable @Override
	public EquipmentSlotType getEquipmentSlot(ItemStack stack) {
		return EquipmentSlotType.CHEST;
	}
	
	@Override public boolean getIsRepairable(@NotNull ItemStack toRepair, ItemStack repair) {
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
		PlayerEntity player = context.getPlayer();
		assert player != null;
		BlockPos pos = context.getPos();
		ItemStack stack = context.getItem();
		Block block = world.getBlockState(pos).getBlock();
		BlockState state = world.getBlockState(pos);
		if (block instanceof CauldronBlock) {
			int i = state.get(CauldronBlock.LEVEL);
			if (i > 0 && BannerTileEntity.getPatterns(stack) > 0) {
				CauldronBlock cauldron = (CauldronBlock) block;
				ItemStack result = stack.copy();
				result.setCount(1);
				// Remove all banner layers
				result.removeChildTag("BlockEntityTag");
				
				if (!player.abilities.isCreativeMode) {
					stack.shrink(1);
					cauldron.setWaterLevel(world, pos, state, i - 1);
				}
				
				if (stack.isEmpty()) {
					player.setHeldItem(context.getHand(), result);
				} else if (!player.inventory.addItemStackToInventory(result)) {
					player.dropItem(result, false);
				} else if (player instanceof ServerPlayerEntity) {
					((ServerPlayerEntity) player).sendContainerToPlayer(player.container);
				}
				return ActionResultType.func_233537_a_(world.isRemote());
			}
		}
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
			if (data.isFlying()) {
				float rel_prop = abs(data.getPropulsionStrength()) /
				                 max(abs(Config.aerobatic.propulsion.max_tick),
				                     abs(Config.aerobatic.propulsion.min_tick));
				float fuel_usage = rel_prop * Config.fuel.usage_linear_tick +
				                   rel_prop * rel_prop * Config.fuel.usage_quad_tick +
				                   MathHelper.sqrt(rel_prop) * Config.fuel.usage_sqrt_tick;
				IElytraSpec spec = getElytraSpecOrDefault(stack);
				spec.setAbility(FUEL, max(0F, spec.getAbility(FUEL) - fuel_usage));
			}
		}
		return true;
	}
	
	// Capabilities handling
	
	// FIXME: Solve issue of CCreativeInventoryActionPacket not encoding ItemStack capabilities
	//        on writePacketData. Possibly requires changes to PacketBuffer#writeItemStack
	//        and possibly PacketBuffer#readItemStack and IForgeItem.
	//        Currently, item capabilities are reset on any Creative Inventory actions on
	//        multiplayer worlds.
	
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
	
	public ResourceLocation getTextureLocation(BannerPattern banner) {
		return bannerTextures.get(banner);
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
			  ).append(sideParenthesis).mergeStyle(TextFormatting.GRAY));
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
					.append(
					  ttc("aerobatic-elytra.gui.shift_to_expand")
						 .mergeStyle(TextFormatting.DARK_GRAY))
			  ).mergeStyle(TextFormatting.GRAY));
		}
	}
	
	// Split wings
	public AerobaticElytraWingItem getWingItem(ItemStack elytra, WingSide side) {
		return ModItems.AEROBATIC_ELYTRA_WING;
	}
	
	public ItemStack getWing(ItemStack elytra, WingSide side) {
		ItemStack wing = new ItemStack(getWingItem(elytra, side), 1, getSerializedCaps(elytra));
		dyement.read(elytra);
		dyement.getWing(side).write(wing, null);
		wing.setDamage(elytra.getDamage());
		CompoundNBT tag = elytra.getTag();
		if (tag != null && tag.contains("HideFlags", 99))
			wing.getOrCreateTag().putInt("HideFlags", tag.getInt("HideFlags"));
		getElytraSpecOrDefault(wing).getTrailData().keep(side);
		return wing;
	}
}
