package endorh.aerobatic_elytra.common.recipe;

import com.google.gson.JsonArray;
import endorh.util.network.PacketBufferUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.util.network.PacketBufferUtil.*;
import static endorh.util.common.TextUtil.stc;

// TODO: Proper JEI integration
/**
 * Custom syntax for selecting items in the config
 * based on item name, tag and nbt predicates<br>
 * Examples:<br>
 * <ul>
 *    <li>{@code minecraft:stick}</li>
 *    <li>{@code {minecraft:leaves}}</li>
 *    <li>{@code minecraft:firework_rocket[Fireworks.Flight(B)<=2]}</li>
 * </ul>
 *
 * Any number of tag selectors and NBT predicates may be used
 */
public class ItemSelector implements Predicate<ItemStack> {
	private final String itemName;
	private final Item item;
	private final List<ITag<Item>> iTags;
	private final List<String> tags;
	private final List<NBTPredicate> nbtPredicates;
	
	private static final Pattern item_selector_pattern = Pattern.compile(
	  "(?<tags>(?:\\{.+?})+)?(?<name>(?:\\w+:)?\\w+)?(?<nbt>(?:\\[.+?])+)?");
	private static final Pattern tag_selector_pattern = Pattern.compile(
	  "\\{(?<name>\\w+:[\\w/]+)}");
	
	/**
	 * Parse an item selector
	 */
	public static ItemSelector fromString(String str) {
		Matcher matcher = item_selector_pattern.matcher(str);
		if (matcher.find()) {
			String itemName;
			List<String> tags = new ArrayList<>();
			List<NBTPredicate> nbtPredicates;
			String tagsString = matcher.group("tags");
			if (tagsString != null) {
				Matcher tag_matcher = tag_selector_pattern.matcher(tagsString);
				while (tag_matcher.find()) {
					String tag_name = tag_matcher.group("name");
					if (!tag_name.contains(":"))
						tag_name = "minecraft:" + tag_name;
					tags.add(tag_name);
				}
			}
			itemName = matcher.group("name");
			if (itemName == null || itemName.length() == 0)
				itemName = null;
			else
				itemName = itemName.contains(":")? itemName : "minecraft:" + itemName;
			String nbt = matcher.group("nbt");
			if (nbt != null) {
				nbtPredicates = NBTPredicate.parsePredicates(
				  matcher.group("nbt"));
			} else nbtPredicates = new ArrayList<>();
			return new ItemSelector(itemName, tags, nbtPredicates);
		} else throw new IllegalArgumentException(String.format(
		  "Malformed item selector: '%s'", str));
	}
	
	public ItemSelector(String itemName, List<String> tags, List<NBTPredicate> nbtPredicates) {
		this.itemName = itemName;
		this.tags = tags;
		final ResourceLocation itemLocation = new ResourceLocation(itemName);
		item =
		  Registry.ITEM.getOptional(itemLocation).orElseThrow(
		    () -> new IllegalArgumentException("Unknown item: '" + itemLocation + "'"));
		final ITagCollection<Item> itemTags = TagCollectionManager.getManager().getItemTags();
		iTags = tags.stream().map(str -> itemTags.get(new ResourceLocation(str))).collect(Collectors.toList());
		this.nbtPredicates = nbtPredicates;
	}
	
	public boolean test(ItemStack stack) {
		Item item = stack.getItem();
		ResourceLocation itemName = item.getRegistryName();
		assert itemName != null;
		if (this.itemName != null &&
		    !this.itemName.equalsIgnoreCase(item.getRegistryName().toString()))
			return false;
		Set<ResourceLocation> itemTags = item.getTags();
		for (String tag : this.tags) {
			if (!itemTags.contains(new ResourceLocation(tag)))
				return false;
		}
		for (NBTPredicate pred : this.nbtPredicates) {
			if (!pred.test(stack))
				return false;
		}
		return true;
	}
	
	public static List<ItemSelector> deserialize(JsonArray arr) {
		List<ItemSelector> result = new ArrayList<>();
		for (int i = 0; i < arr.size(); i++) {
			String sel = JSONUtils.getString(arr.get(i), "ingredients[" + i + "]");
			result.add(ItemSelector.fromString(sel));
		}
		return result;
	}
	
	public static ItemSelector read(PacketBuffer buf) {
		String itemName = readString(buf);
		List<String> tags = readList(buf, PacketBufferUtil::readString);
		List<NBTPredicate> nbtPredicates = readList(buf, NBTPredicate::read);
		return new ItemSelector(itemName, tags, nbtPredicates);
	}
	
	public void write(PacketBuffer buf) {
		buf.writeString(itemName);
		writeList(buf, tags, PacketBuffer::writeString);
		writeList(nbtPredicates, buf, NBTPredicate::write);
	}
	
	public static boolean any(List<ItemSelector> selectors, ItemStack stack) {
		for (ItemSelector sel : selectors) {
			if (sel.test(stack))
				return true;
		}
		return false;
	}
	
	// TODO: Display NBT properly
	/**
	 * Used to display in JEI
	 * @return An {@link Ingredient} close to this.
	 */
	public Ingredient similarIngredient() {
		List<Ingredient.IItemList> lists = new ArrayList<>();
		if (!iTags.isEmpty()) {
			return Ingredient.fromTag(iTags.get(0));
		} else {
			return Ingredient.fromItems(item);
		}
	}
	
	public static class NBTPredicate implements Predicate<ItemStack> {
		private final List<String> keys;
		private final String lastKey;
		private final Comparison comp;
		private final Character type;
		private final double value;
		
		private static final Pattern pattern = Pattern.compile(
		  "(?i)\\[([\\w.]+)\\s*\\(([BSILFD])\\)\\s*([<>]=?|=)\\s*((?:\\d*\\.)?\\d+)]");
		public static List<NBTPredicate> parsePredicates(String src) {
			List<NBTPredicate> list = new ArrayList<>();
			Matcher matcher = pattern.matcher(src);
			while (matcher.find()) {
				list.add(new NBTPredicate(
				  matcher.group(1),
				  Comparison.fromSymbol(matcher.group(3)),
				  matcher.group(2).toUpperCase().charAt(0),
				  Double.parseDouble(matcher.group(4))
				));
			}
			return list;
		}
		
		public NBTPredicate(
		  Comparison comp, List<String> keys, String lastKey, Character type, double value
		) {
			this.comp = comp;
			this.keys = keys;
			this.lastKey = lastKey;
			this.type = type;
			this.value = value;
		}
		
		public NBTPredicate(String path, Comparison comp, Character type, double value) {
			this.comp = comp;
			this.keys = new ArrayList<>(
			  Arrays.asList(path.split("\\.")));
			this.lastKey = this.keys.remove(this.keys.size() - 1);
			this.type = type;
			this.value = value;
		}
		
		public static NBTPredicate read(PacketBuffer buf) {
			Comparison comp = buf.readEnumValue(Comparison.class);
			List<String> keys = readList(buf, PacketBufferUtil::readString);
			String lastKey = readString(buf);
			Character type = buf.readChar();
			double value = buf.readDouble();
			return new NBTPredicate(comp, keys, lastKey, type, value);
		}
		
		public void write(PacketBuffer buf) {
			buf.writeEnumValue(comp);
			writeList(buf, keys, PacketBuffer::writeString);
			buf.writeString(lastKey);
			buf.writeChar(type);
			buf.writeDouble(value);
		}
		
		public boolean test(ItemStack stack) {
			CompoundNBT nbt = stack.copy().getOrCreateTag();
			for (String key : this.keys)
				nbt = nbt.getCompound(key);
			double value = getDouble(nbt, lastKey, type);
			return comp.test(value, this.value);
		}
		
		private static double getDouble(CompoundNBT nbt, String key, Character type) {
			switch (type) {
				case 'B': return nbt.getByte(key);
				case 'S': return nbt.getShort(key);
				case 'I': return nbt.getInt(key);
				case 'L': return (double)nbt.getLong(key);
				case 'F': return nbt.getFloat(key);
				case 'D': return nbt.getDouble(key);
				default: return Double.NaN;
			}
		}
		
		enum Comparison {
			EQ("="),
			LEQ("<="),
			GEQ(">="),
			LE("<"),
			GE(">");
			
			private final String symbol;
			Comparison(String symbol) {
				this.symbol = symbol;
			}
			
			public boolean test(double left, double right) {
				switch (this) {
					case LEQ: return left <= right;
					case GEQ: return left >= right;
					case LE: return left < right;
					case GE: return left > right;
					default: return Double.compare(left, right) == 0;
				}
			}
			
			public static Comparison fromSymbol(String symbol) {
				for (Comparison comp : Comparison.values())
					if (symbol.equals(comp.symbol))
						return comp;
				if ("==".equals(symbol))
					return EQ;
				throw new IllegalArgumentException(
				  String.format("Unknown Comparison symbol: \"%s\"", symbol));
			}
			
			@Override public String toString() {
				return symbol;
			}
		}
		
		public ITextComponent getDisplay() {
			IFormattableTextComponent d = stc("[");
			for (String key : keys)
				d = d.append(stc(key).mergeStyle(TextFormatting.GRAY))
				  .append(stc(".").mergeStyle(TextFormatting.DARK_GRAY));
			return d.append(stc(lastKey).mergeStyle(TextFormatting.GRAY))
			  .append(stc(" " + comp + " ").mergeStyle(TextFormatting.GOLD))
			  .append(stc(String.format("%.2f", value)).mergeStyle(TextFormatting.AQUA))
			  .append(stc("]")).mergeStyle(TextFormatting.DARK_PURPLE);
		}
		
		@Override public String toString() {
			return '[' + String.join(".", keys)
			  + '.' + lastKey + '(' + type + ')' + comp
			  + String.format("%.2f", value) + ']';
		}
	}
	
	public ITextComponent getDisplay() {
		IFormattableTextComponent d = stc("");
		for (String tag : tags)
			d = d.append(
			  stc("{")
			    .append(stc(tag).mergeStyle(TextFormatting.GRAY))
			    .append(stc("}"))
			    .mergeStyle(TextFormatting.DARK_GREEN));
		if (itemName.startsWith("minecraft:")) {
			d.append(stc(itemName.substring(10)).mergeStyle(TextFormatting.WHITE));
		} else if (itemName.contains(":")) {
			final int split = itemName.indexOf(':') + 1;
			d = d.append(stc(itemName.substring(0, split)).mergeStyle(TextFormatting.GRAY))
			  .append(stc(itemName.substring(split)).mergeStyle(TextFormatting.WHITE));
		} else {
			d = d.append(stc(itemName).mergeStyle(TextFormatting.WHITE));
		}
		for (NBTPredicate pr : nbtPredicates)
			d = d.append(pr.getDisplay());
		return d;
	}
	
	@Override public String toString() {
		StringBuilder res = new StringBuilder();
		for (String tag : tags)
			res.append('{').append(tag).append('}');
		res.append(itemName);
		for (NBTPredicate pred : nbtPredicates)
			res.append(pred);
		return res.toString();
	}
}
