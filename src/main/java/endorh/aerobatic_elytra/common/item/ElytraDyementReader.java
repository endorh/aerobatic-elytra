package endorh.aerobatic_elytra.common.item;

import com.mojang.datafixers.util.Pair;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.tileentity.BannerTileEntity;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static endorh.util.common.TextUtil.ttc;

/**
 * Helper for reading dye info from NBT all at once.
 */
public class ElytraDyementReader {
	public Map<WingSide, WingDyement> sides = new HashMap<>();
	public boolean hasWingDyement;
	public int defaultColor;
	
	public ElytraDyementReader() {
		this(AerobaticElytraItem.DEFAULT_COLOR);
	}
	
	public ElytraDyementReader(int defaultColor) {
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
				patternColorData = BannerTileEntity.getPatternColorData(
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
		
		public TranslationTextComponent getTranslation() {
			return ttc(key);
		}
	}
}
