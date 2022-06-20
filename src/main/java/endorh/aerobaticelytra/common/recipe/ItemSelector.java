package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import endorh.util.nbt.NBTPredicate;
import endorh.util.nbt.NBTPredicate.NBTPredicateParseException;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.util.network.PacketBufferUtil.readList;
import static endorh.util.network.PacketBufferUtil.writeList;
import static endorh.util.text.TextUtil.stc;

/**
 * Custom syntax for selecting items in the config
 * based on item name, tag and nbt predicates<br>
 * Examples:<br>
 * <ul>
 *    <li>{@code minecraft:stick}</li>
 *    <li>{@code [minecraft:leaves]}</li>
 *    <li>{@code minecraft:firework_rocket{Fireworks.Flight >= 2}}</li>
 * </ul>
 * Multiple tags are supported beside each other, and are combined
 * with an OR operator (only one needs to match).
 * {@link NBTPredicate}s can be rather complex, and have their own documentation
 */
public class ItemSelector implements Predicate<ItemStack> {
	private final @Nullable Item item;
	// The tags' names are stored for serialization
	private final @Nullable Map<ITag<Item>, ResourceLocation> tags;
	private final @Nullable NBTPredicate nbtPredicate;
	
	private static final Pattern item_selector_pattern = Pattern.compile(
	  "\\s*(?:(?<tags>(?:\\[(?:[\\w.-]++:)?[\\w/.-]++]\\s*+)+)" +
	  "|(?<name>(?:[\\w.-]++:)?[\\w/.-]++)\\s*+)" +
	  "(?<nbt>\\{.+})?\\s*+");
	private static final Pattern tag_selector_pattern = Pattern.compile(
	  "\\[(?<name>\\w+:[\\w/.-]++)}");
	
	/**
	 * Parse an item selector
	 */
	public static ItemSelector fromString(String str) {
		Matcher m = item_selector_pattern.matcher(str);
		if (m.matches()) {
			String nbt = m.group("nbt");
			NBTPredicate nbtPredicate = null;
			try {
				if (nbt != null && !nbt.isEmpty()) {
					nbtPredicate = NBTPredicate.parse(nbt).orElseThrow(
					  () -> new IllegalArgumentException("Malformed NBT predicate: \"" + nbt + "\""));
				}
			} catch (NBTPredicateParseException e) {
				throw new IllegalArgumentException(e.getLocalizedMessage(), e);
			}
			String tagsString = m.group("tags");
			if (tagsString != null) {
				Matcher tag_matcher = tag_selector_pattern.matcher(tagsString);
				List<ResourceLocation> tags = new ArrayList<>();
				while (tag_matcher.find())
					tags.add(new ResourceLocation(tag_matcher.group("name")));
				return new ItemSelector(tags, nbtPredicate);
			} else {
				String itemName = m.group("name");
				final Item item = Registry.ITEM.getOptional(new ResourceLocation(itemName))
				  .orElseThrow(() -> new IllegalArgumentException("Unknown item: \"" + itemName + "\""));
				return new ItemSelector(item, nbtPredicate);
			}
		} else throw new IllegalArgumentException("Malformed item selector: \"" + str + "\"");
	}
	
	public ItemSelector(List<ResourceLocation> tags, @Nullable NBTPredicate nbtPredicate) {
		this.item = null;
		final ITagCollection<Item> itemTags = TagCollectionManager.getManager().getItemTags();
		this.tags = tags.stream().collect(Collectors.toMap(r -> {
			final ITag<Item> tag = itemTags.get(r);
			if (tag == null)
				throw new IllegalArgumentException("Unknown item tag name: \"" + r + "\"");
			return tag;
		}, r -> r));
		this.nbtPredicate = nbtPredicate;
	}
	
	public ItemSelector(@NotNull Item item, @Nullable NBTPredicate nbtPredicate) {
		this.item = item;
		this.tags = null;
		this.nbtPredicate = nbtPredicate;
	}
	
	public boolean testIgnoringNBT(ItemStack stack) {
		if (item != null)
			return item.equals(stack.getItem());
		else if (tags != null) {
			return tags.keySet().stream().anyMatch(t -> t.contains(stack.getItem()));
		} else throw new IllegalStateException("Both item and tags cannot be null");
	}
	
	public boolean test(ItemStack stack) {
		if (!testIgnoringNBT(stack))
			return false;
		return nbtPredicate == null || nbtPredicate.test(stack);
	}
	
	public static List<ItemSelector> deserialize(JsonArray arr) {
		List<ItemSelector> result = new ArrayList<>();
		try {
			for (int i = 0; i < arr.size(); i++) {
				String sel = JSONUtils.getString(arr.get(i), "ingredients[" + i + "]");
				result.add(ItemSelector.fromString(sel));
			}
		} catch (IllegalArgumentException e) {
			throw new JsonSyntaxException(e.getLocalizedMessage(), e);
		}
		return result;
	}
	
	public static ItemSelector read(PacketBuffer buf) {
		if (buf.readBoolean()) {
			final ResourceLocation itemName = buf.readResourceLocation();
			Item item = Registry.ITEM.getOptional(itemName).orElseThrow(
			  () -> new IllegalStateException("Unknown item name found in packet: \"" + itemName + "\""));
			NBTPredicate nbtPredicate =
			  buf.readBoolean() ? NBTPredicate.parse(PacketBufferUtil.readString(buf)).orElse(null) : null;
			return new ItemSelector(item, nbtPredicate);
		} else {
			final List<ResourceLocation> tagNames = readList(buf, PacketBuffer::readResourceLocation);
			final ITagCollection<Item> itemTags = TagCollectionManager.getManager().getItemTags();
			for (ResourceLocation tagName : tagNames) {
				if (itemTags.get(tagName) == null)
					throw new IllegalStateException("Unknown item tag name found in packet: \"" + tagName + "\"");
			}
			NBTPredicate nbtPredicate =
			  buf.readBoolean() ? NBTPredicate.parse(PacketBufferUtil.readString(buf)).orElse(null) : null;
			return new ItemSelector(tagNames, nbtPredicate);
		}
	}
	
	public void write(PacketBuffer buf) {
		if (item != null) {
			buf.writeBoolean(true);
			//noinspection ConstantConditions
			buf.writeResourceLocation(item.getRegistryName());
		} else if (tags != null) {
			buf.writeBoolean(false);
			writeList(buf, tags.values(), PacketBuffer::writeResourceLocation);
		}
		buf.writeBoolean(nbtPredicate != null);
		if (nbtPredicate != null)
			nbtPredicate.write(buf);
	}
	
	public static boolean any(List<ItemSelector> selectors, ItemStack stack) {
		for (ItemSelector sel : selectors) {
			if (sel.test(stack))
				return true;
		}
		return false;
	}
	
	/**
	 * Used to display in JEI
	 * @return An {@link Ingredient} close to this.
	 */
	public Ingredient similarIngredient() {
		if (tags != null && !tags.isEmpty()) {
			return Ingredient.fromItems(tags.keySet().stream().flatMap(t -> t.getAllElements().stream()).toArray(Item[]::new));
		} else if (item != null) {
			return Ingredient.fromItems(item);
		}
		return Ingredient.EMPTY;
	}
	
	public Optional<CompoundNBT> matchingNBT() {
		if (nbtPredicate != null)
			//noinspection unchecked
			return (Optional<CompoundNBT>) (Optional<?>) nbtPredicate.generateValid();
		return Optional.empty();
	}
	
	public ITextComponent getDisplay() {
		IFormattableTextComponent d = stc("");
		if (tags != null) {
			for (ResourceLocation tagName : tags.values())
				d = d.append(
				  stc("{")
					 .append(stc(tagName).mergeStyle(TextFormatting.GRAY))
					 .append(stc("}"))
					 .mergeStyle(TextFormatting.DARK_GREEN));
		}
		if (item != null)
			d = d.append(new ItemStack(item).getDisplayName());
		if (nbtPredicate != null)
			d = d.append(nbtPredicate.getDisplay());
		return d;
	}
	
	@Override public String toString() {
		StringBuilder res = new StringBuilder();
		if (tags != null) {
			for (ResourceLocation tagName : tags.values())
				res.append('{').append(tagName).append('}');
		}
		if (item != null)
			res.append(item.getRegistryName());
		if (nbtPredicate != null)
			res.append(nbtPredicate);
		return res.toString();
	}
}
