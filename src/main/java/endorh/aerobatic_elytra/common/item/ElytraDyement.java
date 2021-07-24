package endorh.aerobatic_elytra.common.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.text.IFormattableTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static endorh.util.common.TextUtil.ttc;

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
	public Map<WingSide, WingDyement> sides = new HashMap<>();
	public boolean hasWingDyement;
	public int defaultColor;
	
	public ElytraDyement() {
		this(AerobaticElytraItem.DEFAULT_COLOR);
	}
	
	public ElytraDyement(int defaultColor) {
		this.defaultColor = defaultColor;
		for (WingSide side : WingSide.values())
			sides.put(side, new WingDyement());
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
		hasWingDyement = elytra.getChildTag("WingInfo") != null;
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
	
	/**
	 * Utility for picking a wing without caring which
	 */
	public WingDyement getFirst() {
		return sides.get(WingSide.LEFT);
	}
	
	public WingDyement getWing(WingSide side) {
		return sides.get(side);
	}
	
	/**
	 * Writes to an item stack
	 * @param stack Stack to write
	 */
	public void write(ItemStack stack) {
		if (hasWingDyement) {
			for (WingSide side : WingSide.values())
				sides.get(side).write(stack, side);
		} else {
			getFirst().write(stack, null);
		}
	}
	
	/**
	 * Holder for dyement info for a specific wing
	 */
	public static class WingDyement {
		public boolean hasColor;
		public boolean hasPattern;
		public int color;
		public DyeColor basePatternColor = null;
		public List<Pair<BannerPattern, DyeColor>> patternColorData;
		
		public WingDyement() { }
		
		/**
		 * Read from NBT
		 * @param elytra Elytra stack
		 * @param side Wing side
		 * @param defaultColor Default color used when no color is present
		 */
		public void read(ItemStack elytra, WingSide side, int defaultColor) {
			CompoundNBT wingInfo = elytra.getChildTag("WingInfo");
			CompoundNBT data;
			if (wingInfo == null) {
				data = elytra.getChildTag("BlockEntityTag");
				if (data == null) {
					hasPattern = false;
					patternColorData = null;
					CompoundNBT tag = elytra.getTag();
					if (tag == null || !tag.contains("display")) {
						hasColor = false;
						color = defaultColor;
					} else {
						CompoundNBT display = tag.getCompound("display");
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
		
		/**
		 * Writes the current wing to an item
		 */
		public void write(ItemStack stack, @Nullable WingSide side) {
			if (hasPattern) {
				CompoundNBT nbt;
				if (side != null) {
					nbt = stack.getOrCreateChildTag("WingInfo");
					nbt.put(side.tag, new CompoundNBT());
					nbt = nbt.getCompound(side.tag);
				} else {
					nbt = stack.getOrCreateChildTag("BlockEntityTag");
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
				if (side != null) {
					CompoundNBT wing = stack.getOrCreateChildTag("WingInfo");
					wing.put(side.tag, new CompoundNBT());
					wing = wing.getCompound(side.tag);
					wing.putInt("color", color);
				} else {
					CompoundNBT tag = stack.getOrCreateTag();
					if (!tag.contains("display"))
						tag.put("display", new CompoundNBT());
					CompoundNBT display = tag.getCompound("display");
					display.putInt("color", color);
				}
			}
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
	
	public enum WingSide {
		LEFT("left", "aerobatic-elytra.sides.left"),
		RIGHT("right", "aerobatic-elytra.sides.right");
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
