package endorh.aerobaticelytra.common.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import endorh.lazulib.nbt.NBTPredicate;
import endorh.lazulib.nbt.NBTPredicate.NBTPredicateParseException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static endorh.lazulib.network.PacketBufferUtil.readList;
import static endorh.lazulib.network.PacketBufferUtil.writeList;
import static endorh.lazulib.text.TextUtil.stc;

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
	private final @Nullable Map<TagKey<Item>, ResourceLocation> tags;
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
				final Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
				return new ItemSelector(item, nbtPredicate);
			}
		} else throw new IllegalArgumentException("Malformed item selector: \"" + str + "\"");
	}
	
	public ItemSelector(List<ResourceLocation> tags, @Nullable NBTPredicate nbtPredicate) {
		this.item = null;
		this.tags = tags.stream().collect(Collectors.toMap(
		  r -> TagKey.create(Registries.ITEM, r), r -> r));
		this.nbtPredicate = nbtPredicate;
	}
	
	public ItemSelector(@Nullable Item item, @Nullable NBTPredicate nbtPredicate) {
		this.item = item;
		this.tags = null;
		this.nbtPredicate = nbtPredicate;
	}

	public boolean isItemBased() {
		return item != null;
	}

	public boolean isTagBased() {
		return item == null;
	}

	public boolean isSimple() {
		return nbtPredicate == null;
	}

	public boolean testIgnoringNBT(ItemStack stack) {
		if (item != null) {
			return item.equals(stack.getItem());
		} else if (tags != null) {
			for (TagKey<Item> tag: tags.keySet())
				for (Holder<Item> holder: BuiltInRegistries.ITEM.getTagOrEmpty(tag))
					if (holder.isBound() && holder.value().equals(stack.getItem()))
						return true;
			return false;
		} else return false;
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
				String sel = GsonHelper.convertToString(arr.get(i), "ingredients[" + i + "]");
				result.add(fromString(sel));
			}
		} catch (IllegalArgumentException e) {
			throw new JsonSyntaxException(e.getLocalizedMessage(), e);
		}
		return result;
	}
	
	public static ItemSelector read(FriendlyByteBuf buf) {
		if (buf.readBoolean()) {
			Item item = buf.readItem().getItem();
			NBTPredicate nbtPredicate = buf.readBoolean()? NBTPredicate.parse(buf.readUtf()).orElse(null) : null;
			return new ItemSelector(item, nbtPredicate);
		} else {
			final List<ResourceLocation> tagNames = readList(buf, FriendlyByteBuf::readResourceLocation);
			NBTPredicate nbtPredicate =
			  buf.readBoolean()? NBTPredicate.parse(buf.readUtf()).orElse(null) : null;
			return new ItemSelector(tagNames, nbtPredicate);
		}
	}
	
	public void write(FriendlyByteBuf buf) {
		if (item != null) {
			buf.writeBoolean(true);
			buf.writeItem(new ItemStack(item));
		} else if (tags != null) {
			buf.writeBoolean(false);
			writeList(buf, tags.values(), FriendlyByteBuf::writeResourceLocation);
		}
		buf.writeBoolean(nbtPredicate != null);
		if (nbtPredicate != null)
			nbtPredicate.write(buf);
	}
	
	public static boolean any(List<ItemSelector> selectors, ItemStack stack) {
		for (ItemSelector sel: selectors) {
			if (sel.test(stack))
				return true;
		}
		return false;
	}
	
	/**
	 * Used to display in JEI
	 *
	 * @return An {@link Ingredient} close to this.
	 */
	public Ingredient similarIngredient() {
		if (tags != null && !tags.isEmpty()) {
			List<Item> list = new ArrayList<>();
			for (TagKey<Item> tag: tags.keySet()) {
				for (Holder<Item> h: BuiltInRegistries.ITEM.getTagOrEmpty(tag))
					if (h.isBound()) list.add(h.value());
			}
			return Ingredient.of(list.toArray(Item[]::new));
		} else if (item != null) return Ingredient.of(item);
		return Ingredient.EMPTY;
	}
	
	public Optional<CompoundTag> matchingNBT() {
		if (nbtPredicate != null)
			//noinspection unchecked
			return (Optional<CompoundTag>) (Optional<?>) nbtPredicate.generateValid();
		return Optional.empty();
	}
	
	public Component getDisplay() {
		MutableComponent d = stc("");
		if (tags != null) {
			for (ResourceLocation tagName: tags.values())
				d = d.append(
				  stc("{")
					 .append(stc(tagName).withStyle(ChatFormatting.GRAY))
					 .append(stc("}"))
					 .withStyle(ChatFormatting.DARK_GREEN));
		}
		if (item != null)
			d = d.append(new ItemStack(item).getHoverName());
		if (nbtPredicate != null)
			d = d.append(nbtPredicate.getDisplay());
		return d;
	}

	public @Nullable Item getItem() {
		return item;
	}

	public @Nullable Map<TagKey<Item>, ResourceLocation> getTags() {
		return Collections.unmodifiableMap(tags);
	}

	public @Nullable NBTPredicate getNbtPredicate() {
		return nbtPredicate;
	}

	@Override public String toString() {
		StringBuilder res = new StringBuilder();
		if (tags != null) {
			for (ResourceLocation tagName: tags.values())
				res.append('{').append(tagName).append('}');
		}
		if (item != null)
			res.append(ForgeRegistries.ITEMS.getKey(item));
		if (nbtPredicate != null)
			res.append(nbtPredicate);
		return res.toString();
	}
}
