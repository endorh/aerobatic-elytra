package dnj.aerobatic_elytra.common.item;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static dnj.aerobatic_elytra.AerobaticElytra.prefix;

public class PotionEffectAbility implements IAbility {
	public final String jsonName;
	public final TranslationTextComponent displayName;
	public final DisplayType type;
	public final TextFormatting color;
	public final float defValue;
	public final ResourceLocation registryName;
	
	public final Set<ConditionalEffect> effects = null;
	
	public PotionEffectAbility(
	  String jsonName, TranslationTextComponent displayName,
	  DisplayType type, TextFormatting color, float defValue
	) {
		this.jsonName = jsonName;
		this.displayName = displayName;
		this.type = type;
		this.color = color;
		this.defValue = defValue;
		this.registryName = prefix(jsonName.toLowerCase());
	}
	
	@Override public String jsonName() {
		return jsonName;
	}
	
	@Override public IFormattableTextComponent getDisplayName() {
		return displayName;
	}
	
	@Override public DisplayType getDisplayType() {
		return type;
	}
	
	@Override public float getDefault() {
		return defValue;
	}
	
	@Nullable @Override public TextFormatting getColor() {
		return color;
	}
	
	@Override
	public IAbility setRegistryName(ResourceLocation name) {
		throw new IllegalStateException("Cannot set registry name twice");
	}
	
	@Nullable @Override public ResourceLocation getRegistryName() {
		return registryName;
	}
	
	public static PotionEffectAbility parse(JsonObject object) {
		return null; // TODO
	}
	
	public static List<PotionEffectAbility> gather() {
		return null; // TODO
	}
	
	public static class ConditionalEffect {
		//public ConditionalEffect(Set<Effect>, Set<EntityPredicate>)
	}
}
