package endorh.aerobaticelytra.common.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStack.TooltipDisplayFlags;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;

import static endorh.util.text.TextUtil.ttc;

/**
 * Helper for reading/writing dye info from/to NBT<br>
 * The format is, in order of descending priority:
 * <pre>{@code
 *    Item
 *     |- [Tag] WingInfo
 *     |   |- [Compound] left [WingDyement]
 *     |   |- [Compound] right [WingDyement]
 *     |- [Tag] BlockEntityTag [WingDyement]
 *     |- [Compound] display
 *     |   |- [Int] color (rgb color) (= defaultColor)
 *    where:
 *    WingDyement (similar to banners)
 *     |- [Int] Base (DyeColor id)
 *     |- [List<Compound>] Patterns
 *     |          (or alternatively, simple coloring)
 *     |- [Int] color (rgb color)
 *     |- [Compound] display
 *         |- [Int] color (rgb color)
 * }</pre>
 * Where the {@code [Tag]} type annotations denote child tags to the item<br>
 * If separate wing dyement is present, uniform coloring is ignored<br>
 * For each dyement, if banner coloring is present, simple coloring is ignored<br>
 * The pattern format is the same used by banners, that is:
 * <pre>{@code
 *    [List<Compound>] Patterns
 *     |- - [Compound]
 *     |     |- [String] Pattern (pattern hash)
 *     |     |- [Int] Color (DyeColor id)
 *     |- - ...
 * }</pre>
 * From the bottommost layer to the topmost. Only 16 layers are rendered
 * at most.
 */
public class ElytraDyement {
	protected static ElytraDyement dyement = new ElytraDyement();
	public Map<WingSide, WingDyement> sides = new HashMap<>();
	public boolean hasWingDyement;
	public int defaultColor;
	
	public ElytraDyement() {
		this(AerobaticElytraItem.DEFAULT_COLOR);
	}
	
	public ElytraDyement(int defaultColor) {
		this.defaultColor = defaultColor;
		for (WingSide side : WingSide.values())
			sides.put(side, new WingDyement(this));
	}
	
	/**
	 * Read from NBT
	 * @param elytra Elytra stack
	 */
	public void read(ItemStack elytra) {
		read(elytra, defaultColor, true);
	}
	
	/**
	 * Read from NBT
	 * @param elytra Elytra stack
	 * @param readWings False to only read hasWingDyement
	 */
	public void read(ItemStack elytra, boolean readWings) {
		read(elytra, defaultColor, readWings);
	}
	
	/**
	 * Read from NBT
	 * @param elytra Elytra stack
	 * @param defaultColor False to only read hasWingDyement
	 */
	public void read(ItemStack elytra, int defaultColor) {
		read(elytra, defaultColor, true);
	}
	
	/**
	 * Read from NBT
	 * @param elytra Elytra stack
	 * @param defaultColor Default color used when no color is present
	 * @param readWings False to only read hasWingDyement
	 */
	public void read(ItemStack elytra, int defaultColor, boolean readWings) {
		hasWingDyement = elytra.getTagElement("WingInfo") != null;
		if (readWings) {
			if (hasWingDyement) {
				for (WingSide side : sides.keySet())
					sides.get(side).read(elytra, side, defaultColor);
			} else {
				sides.get(WingSide.LEFT).read(elytra, WingSide.LEFT, defaultColor);
				for (WingSide side : WingSide.values()) {
					if (side != WingSide.LEFT)
						sides.get(side).read(elytra, side, defaultColor);
				}
			}
		}
	}
	
	public boolean isClear() {
		return sides.values().stream().noneMatch(d -> d.hasColor || d.hasPattern);
	}
	
	/**
	 * Utility for picking a wing without caring which
	 */
	public WingDyement getFirst() {
		return sides.get(WingSide.LEFT);
	}
	
	public void setColor(int color) {
		getFirst().setColor(color);
		this.hasWingDyement = false;
	}
	
	public void setPattern(
	  DyeColor base, List<Pair<BannerPattern, DyeColor>> bannerData
	) { setPattern(base, bannerData, true); }
	
	public void setPattern(
	  DyeColor base, List<Pair<BannerPattern, DyeColor>> bannerData, boolean addBase
	) {
		getFirst().setPattern(base, bannerData, addBase);
		this.hasWingDyement = false;
	}
	
	public void clear() {
		sides.values().forEach(WingDyement::clear);
		hasWingDyement = false;
	}
	
	public void setWing(WingSide side, WingDyement dye) {
		final ElytraDyement parent = dye.parent.get();
		if (parent != null && parent != this)
			parent.setWing(side, dye.copy());
		dye.parent = new WeakReference<>(this);
		sides.put(side, dye);
		hasWingDyement = true;
	}
	
	public WingDyement getWing(WingSide side) {
		if (!hasWingDyement)
			sides.get(side).copy(getFirst());
		return sides.get(side);
	}
	
	/**
	 * Writes to an item stack
	 * @param stack Stack to write
	 */
	public void write(ItemStack stack) {
		if (hasWingDyement) {
			stack.removeTagKey("BlockEntityTag");
			for (WingSide side : WingSide.values())
				sides.get(side).write(stack, side);
		} else {
			stack.removeTagKey("WingInfo");
			getFirst().write(stack, null);
		}
	}
	
	/**
	 * Holder for dyement info for a specific wing
	 */
	public static class WingDyement {
		protected WeakReference<ElytraDyement> parent;
		public boolean hasColor;
		public boolean hasPattern;
		public int color;
		public DyeColor basePatternColor = null;
		public List<Pair<BannerPattern, DyeColor>> patternColorData;
		
		public WingDyement() {
			this.parent = new WeakReference<>(null);
		}
		
		public WingDyement(ElytraDyement dyement) {
			this.parent = new WeakReference<>(dyement);
		}
		
		public void setColor(int color) {
			hasPattern = false;
			hasColor = true;
			this.color = color;
			final ElytraDyement parent = this.parent.get();
			if (parent != null)
				parent.hasWingDyement = true;
		}
		
		public void setPattern(
		  DyeColor base, List<Pair<BannerPattern, DyeColor>> patternData
		) { setPattern(base, patternData, true); }
		
		public void setPattern(
		  DyeColor base, List<Pair<BannerPattern, DyeColor>> patternData, boolean addBase
		) {
			if (addBase) {
				patternData = new ArrayList<>(patternData);
				patternData.add(0, Pair.of(BannerPattern.BASE, base));
			}
			hasPattern = true;
			basePatternColor = base;
			patternColorData = patternData;
			color = base.getColorValue();
			final ElytraDyement parent = this.parent.get();
			if (parent != null)
				parent.hasWingDyement = true;
		}
		
		public void clear() {
			hasColor = false;
			hasPattern = false;
			final ElytraDyement parent = this.parent.get();
			color = parent != null? parent.defaultColor : AerobaticElytraItem.DEFAULT_COLOR;
			if (parent != null)
				parent.hasWingDyement = parent.isClear();
		}
		
		/**
		 * Read from NBT
		 * @param elytra Elytra stack
		 * @param side Wing side
		 * @param defaultColor Default color used when no color is present
		 */
		public void read(ItemStack elytra, WingSide side, int defaultColor) {
			CompoundNBT wingInfo = elytra.getTagElement("WingInfo");
			CompoundNBT data;
			if (wingInfo == null) {
				data = elytra.getTagElement("BlockEntityTag");
				if (data == null) {
					hasPattern = false;
					patternColorData = null;
					CompoundNBT display = elytra.getTagElement("display");
					if (display == null) {
						hasColor = false;
						color = defaultColor;
					} else {
						if (display.contains("color")) {
							hasColor = true;
							color = display.getInt("color");
						} else {
							hasColor = false;
							color = defaultColor;
						}
					}
					return;
				}
			} else {
				data = wingInfo.getCompound(side.tag);
			}
			if (data.contains("Patterns")) {
				hasColor = false;
				hasPattern = true;
				basePatternColor = DyeColor.byId(data.getInt("Base"));
				color = basePatternColor.getColorValue();
				patternColorData = getPatternColorData(
				  basePatternColor, data.getList("Patterns", 10).copy());
			} else {
				hasPattern = false;
				patternColorData = null;
				if (data.contains("color")) {
					hasColor = true;
					color = data.getInt("color");
				} else if (data.contains("display")) {
					CompoundNBT display = data.getCompound("display");
					if (display.contains("color")) {
						hasColor = true;
						color = display.getInt("color");
					} else {
						hasColor = false;
						color = defaultColor;
					}
				} else {
					hasColor = false;
					color = defaultColor;
				}
			}
		}
		
		// Mimic BannerTileEntity#getPatternColorData, which is only present on the client
		public static List<Pair<BannerPattern, DyeColor>> getPatternColorData(
		  DyeColor color, @Nullable ListNBT nbtList
		) {
			List<Pair<BannerPattern, DyeColor>> list = new ArrayList<>();
			list.add(Pair.of(BannerPattern.BASE, color));
			if (nbtList != null) {
				for(int i = 0; i < nbtList.size(); ++i) {
					CompoundNBT compoundnbt = nbtList.getCompound(i);
					BannerPattern bannerpattern = BannerPattern.byHash(compoundnbt.getString("Pattern"));
					if (bannerpattern != null) {
						int j = compoundnbt.getInt("Color");
						list.add(Pair.of(bannerpattern, DyeColor.byId(j)));
					}
				}
			}
			return list;
		}
		
		/**
		 * Copy values from other WingDyement
		 */
		public void copy(WingDyement wingDyement) {
			hasColor = wingDyement.hasColor;
			hasPattern = wingDyement.hasPattern;
			color = wingDyement.color;
			basePatternColor = wingDyement.basePatternColor;
			patternColorData = wingDyement.patternColorData;
		}
		
		public WingDyement copy() {
			final WingDyement wing = new WingDyement();
			wing.copy(this);
			return wing;
		}
		
		/**
		 * Writes the current wing to an item
		 */
		public void write(ItemStack stack, @Nullable WingSide side) {
			if (hasPattern) {
				CompoundNBT nbt;
				if (side != null) {
					nbt = stack.getOrCreateTagElement("WingInfo");
					nbt.put(side.tag, new CompoundNBT());
					nbt = nbt.getCompound(side.tag);
				} else {
					nbt = stack.getOrCreateTagElement("BlockEntityTag");
				}
				nbt.putInt("Base", basePatternColor.getId());
				ListNBT list = new ListNBT();
				for (int i = 1; i < patternColorData.size(); i++) {
					BannerPattern pattern = patternColorData.get(i).getFirst();
					DyeColor color = patternColorData.get(i).getSecond();
					CompoundNBT item = new CompoundNBT();
					item.putString("Pattern", pattern.getHashname());
					item.putInt("Color", color.getId());
					list.add(item);
				}
				nbt.put("Patterns", list);
			} else if (hasColor) {
				CompoundNBT nbt;
				if (side != null) {
					nbt = stack.getOrCreateTagElement("WingInfo");
					nbt.put(side.tag, new CompoundNBT());
					nbt = nbt.getCompound(side.tag);
					nbt.remove("Base");
					nbt.remove("Patterns");
				} else {
					stack.removeTagKey("BlockEntityTag");
					nbt = stack.getOrCreateTagElement("display");
				}
				nbt.putInt("color", color);
			} else {
				stack.removeTagKey("BlockEntityTag");
				final CompoundNBT display = stack.getTagElement("display");
				if (display != null)
					display.remove("color");
				if (side != null) {
					final CompoundNBT nbt = stack.getTagElement("WingInfo");
					if (nbt != null) {
						nbt.remove(side.tag);
						if (nbt.isEmpty())
							stack.removeTagKey("WingInfo");
					}
				} else stack.removeTagKey("WingInfo");
			}
			hideDyedFlag(stack);
		}
		
		@Override public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			WingDyement that = (WingDyement) o;
			return hasColor == that.hasColor && hasPattern == that.hasPattern
			       && color == that.color
			       && basePatternColor == that.basePatternColor
			       && Objects.equals(patternColorData, that.patternColorData);
		}
		
		@Override public int hashCode() {
			return Objects.hash(hasColor, hasPattern, color, basePatternColor, patternColorData);
		}
	}
	
	public static void hideDyedFlag(ItemStack stack) {
		CompoundNBT tag = stack.getOrCreateTag();
		int flags = tag.getInt("HideFlags");
		flags |= TooltipDisplayFlags.DYE.getMask();
		tag.putInt("HideFlags", flags);
	}
	
	public static boolean clearDyesWithCauldron(ItemUseContext context) {
		final World world = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		final BlockState state = world.getBlockState(pos);
		final Block block = state.getBlock();
		
		if (block instanceof CauldronBlock) {
			final PlayerEntity player = context.getPlayer();
			final ItemStack stack = context.getItemInHand();
			
			int i = state.getValue(CauldronBlock.LEVEL);
			dyement.read(stack);
			if (i > 0 && !dyement.isClear()) {
				final CauldronBlock cauldron = (CauldronBlock) block;
				final ItemStack result = stack.copy();
				result.setCount(1);
				dyement.clear();
				dyement.write(result);
				
				if (player != null && !player.abilities.instabuild) {
					stack.shrink(1);
					cauldron.setWaterLevel(world, pos, state, i - 1);
				}
				
				if (player != null) {
					if (stack.isEmpty()) {
						player.setItemInHand(context.getHand(), result);
					} else if (!player.inventory.add(result)) {
						player.drop(result, false);
					} else if (player instanceof ServerPlayerEntity) {
						((ServerPlayerEntity) player).refreshContainer(player.inventoryMenu);
					}
				} else {
					world.addFreshEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), result));
				}
				return true;
			}
		}
		return false;
	}
	
	public static ItemStack clearDyes(ItemStack elytra) {
		final ItemStack result = elytra.copy();
		dyement.read(result);
		dyement.clear();
		dyement.write(result);
		return result;
	}
	
	public enum WingSide {
		LEFT("left", "aerobaticelytra.sides.left"),
		RIGHT("right", "aerobaticelytra.sides.right");
		public final String tag;
		public final String key;
		WingSide(String tag, String key) {
			this.tag = tag;
			this.key = key;
		}
		
		public IFormattableTextComponent getTranslation() {
			return ttc(key);
		}
	}
}
