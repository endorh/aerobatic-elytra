package endorh.aerobaticelytra.common.item;

import com.mojang.datafixers.util.Pair;
import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStack.TooltipPart;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static endorh.util.common.ColorUtil.getTextureDiffuseColor;
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
@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
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
		for (WingSide side: WingSide.values())
			sides.put(side, new WingDyement(this));
	}
	
	/**
	 * Read from NBT
	 *
	 * @param elytra Elytra stack
	 */
	public void read(ItemStack elytra) {
		read(elytra, defaultColor, true);
	}
	
	/**
	 * Read from NBT
	 *
	 * @param elytra Elytra stack
	 * @param readWings False to only read hasWingDyement
	 */
	public void read(ItemStack elytra, boolean readWings) {
		read(elytra, defaultColor, readWings);
	}
	
	/**
	 * Read from NBT
	 *
	 * @param elytra Elytra stack
	 * @param defaultColor False to only read hasWingDyement
	 */
	public void read(ItemStack elytra, int defaultColor) {
		read(elytra, defaultColor, true);
	}
	
	/**
	 * Read from NBT
	 *
	 * @param elytra Elytra stack
	 * @param defaultColor Default color used when no color is present
	 * @param readWings False to only read hasWingDyement
	 */
	public void read(ItemStack elytra, int defaultColor, boolean readWings) {
		hasWingDyement = elytra.getTagElement("WingInfo") != null;
		if (readWings) {
			if (hasWingDyement) {
				for (WingSide side: sides.keySet())
					sides.get(side).read(elytra, side, defaultColor);
			} else {
				sides.get(WingSide.LEFT).read(elytra, WingSide.LEFT, defaultColor);
				for (WingSide side: WingSide.values()) {
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
		hasWingDyement = false;
	}
	
	public void setPattern(
	  DyeColor base, List<Pair<BannerPattern, DyeColor>> bannerData
	) {setPattern(base, bannerData, true);}
	
	public void setPattern(
	  DyeColor base, List<Pair<BannerPattern, DyeColor>> bannerData, boolean addBase
	) {
		getFirst().setPattern(base, bannerData, addBase);
		hasWingDyement = false;
	}
	
	public void clear() {
		sides.values().forEach(WingDyement::clear);
		hasWingDyement = false;
	}
	
	public void setWing(WingSide side, WingDyement dye) {
		final ElytraDyement parent = dye.parent;
		if (parent != null && parent != this)
			parent.setWing(side, dye.copy());
		dye.parent = this;
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
	 *
	 * @param stack Stack to write
	 */
	public void write(ItemStack stack) {
		if (hasWingDyement) {
			stack.removeTagKey("BlockEntityTag");
			for (WingSide side: WingSide.values())
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
		protected ElytraDyement parent;
		public boolean hasColor;
		public boolean hasPattern;
		public int color;
		public DyeColor basePatternColor = null;
		public List<Pair<BannerPattern, DyeColor>> patternColorData;
		
		public WingDyement() {
			parent = null;
		}
		
		public WingDyement(ElytraDyement dyement) {
			parent = dyement;
		}
		
		public void setColor(int color) {
			hasPattern = false;
			hasColor = true;
			this.color = color;
			if (parent != null) parent.hasWingDyement = true;
		}
		
		public void setPattern(
		  DyeColor base, List<Pair<BannerPattern, DyeColor>> patternData
		) {
			setPattern(base, patternData, true);
		}
		
		public void setPattern(
		  DyeColor base, List<Pair<BannerPattern, DyeColor>> patternData, boolean addBase
		) {
			if (addBase) {
				patternData = new ArrayList<>(patternData);
				patternData.add(0, Pair.of(BuiltInRegistries.BANNER_PATTERN.get(BannerPatterns.BASE), base));
			}
			hasPattern = true;
			basePatternColor = base;
			patternColorData = patternData;
			color = getTextureDiffuseColor(base);
			if (parent != null)
				parent.hasWingDyement = true;
		}
		
		public void clear() {
			hasColor = false;
			hasPattern = false;
			color = parent != null? parent.defaultColor : AerobaticElytraItem.DEFAULT_COLOR;
			if (parent != null)
				parent.hasWingDyement = parent.isClear();
		}
		
		/**
		 * Read from NBT
		 *
		 * @param elytra Elytra stack
		 * @param side Wing side
		 * @param defaultColor Default color used when no color is present
		 */
		public void read(ItemStack elytra, WingSide side, int defaultColor) {
			CompoundTag wingInfo = elytra.getTagElement("WingInfo");
			CompoundTag data;
			if (wingInfo == null) {
				data = elytra.getTagElement("BlockEntityTag");
				if (data == null) {
					hasPattern = false;
					patternColorData = null;
					CompoundTag display = elytra.getTagElement("display");
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
			if (data.contains("Base")) {
				hasColor = false;
				hasPattern = true;
				basePatternColor = DyeColor.byId(data.getInt("Base"));
				color = getTextureDiffuseColor(basePatternColor);
				patternColorData = getPatternColorData(
					basePatternColor, data.getList("Patterns", Tag.TAG_COMPOUND).copy());
			} else {
				hasPattern = false;
				patternColorData = null;
				if (data.contains("color")) {
					hasColor = true;
					color = data.getInt("color");
				} else if (data.contains("display")) {
					CompoundTag display = data.getCompound("display");
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
		  DyeColor color, @Nullable ListTag nbtList
		) {
			List<Pair<BannerPattern, DyeColor>> list = new ArrayList<>();
			list.add(Pair.of(BuiltInRegistries.BANNER_PATTERN.get(BannerPatterns.BASE), color));
			if (nbtList != null) {
				for (int i = 0; i < nbtList.size(); ++i) {
					CompoundTag elem = nbtList.getCompound(i);
					Holder<BannerPattern> holder = BannerPattern.byHash(elem.getString("Pattern"));
					if (holder != null) {
						int j = elem.getInt("Color");
						list.add(Pair.of(holder.value(), DyeColor.byId(j)));
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
				CompoundTag nbt;
				if (side != null) {
					nbt = stack.getOrCreateTagElement("WingInfo");
					nbt.put(side.tag, new CompoundTag());
					nbt = nbt.getCompound(side.tag);
				} else {
					nbt = stack.getOrCreateTagElement("BlockEntityTag");
				}
				nbt.putInt("Base", basePatternColor.getId());
				ListTag list = new ListTag();
				for (int i = 1; i < patternColorData.size(); i++) {
					BannerPattern pattern = patternColorData.get(i).getFirst();
					DyeColor color = patternColorData.get(i).getSecond();
					CompoundTag item = new CompoundTag();
					item.putString("Pattern", pattern.getHashname());
					item.putInt("Color", color.getId());
					list.add(item);
				}
				nbt.put("Patterns", list);
			} else if (hasColor) {
				CompoundTag nbt;
				if (side != null) {
					nbt = stack.getOrCreateTagElement("WingInfo");
					nbt.put(side.tag, new CompoundTag());
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
				final CompoundTag display = stack.getTagElement("display");
				if (display != null)
					display.remove("color");
				if (side != null) {
					final CompoundTag nbt = stack.getTagElement("WingInfo");
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
		CompoundTag tag = stack.getOrCreateTag();
		int flags = tag.getInt("HideFlags");
		flags |= TooltipPart.DYE.getMask();
		tag.putInt("HideFlags", flags);
	}
	
	public static CauldronInteraction CLEAR_AEROBATIC_ELYTRA_DYE = (state, level, pos, player, hand, stack) -> {
		dyement.read(stack);
		if (dyement.isClear()) {
			return InteractionResult.PASS;
		} else {
			if (!level.isClientSide) {
				dyement.clear();
				dyement.write(stack);
				player.awardStat(Stats.CLEAN_ARMOR);
				LayeredCauldronBlock.lowerFillLevel(state, level, pos);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
	};
	
	@SubscribeEvent
	public static void registerCauldronInteractions(FMLCommonSetupEvent event) {
		CauldronInteraction.WATER.put(AerobaticElytraItems.AEROBATIC_ELYTRA, CLEAR_AEROBATIC_ELYTRA_DYE);
		CauldronInteraction.WATER.put(AerobaticElytraItems.AEROBATIC_ELYTRA_WING, CLEAR_AEROBATIC_ELYTRA_DYE);
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
		
		public MutableComponent getTranslation() {
			return ttc(key);
		}
	}
}
