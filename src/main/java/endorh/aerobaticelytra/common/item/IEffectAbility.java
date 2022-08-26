package endorh.aerobaticelytra.common.item;

import com.google.gson.*;
import endorh.util.common.LogUtil;
import endorh.util.common.ObfuscationReflectionUtil;
import endorh.util.common.ObfuscationReflectionUtil.SoftField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static endorh.util.text.TextUtil.stc;
import static endorh.util.text.TextUtil.ttc;

public interface IEffectAbility extends IDatapackAbility {
	boolean testConditions(LootContext context);
	void applyEffect(ServerPlayerEntity player);
	void reapplyEffect(ServerPlayerEntity player);
	void undoEffect(ServerPlayerEntity player);
	float getConsumption();
	
	class EffectAbility implements IEffectAbility {
		private static final String REFLECTION_ERROR_MESSAGE = "Aerobatic Elytra Effect Abilities (if provided by datapacks) may not apply correctly";
		private static final Logger LOGGER = LogManager.getLogger();
		
		private static final SoftField<EffectInstance, Integer> EffectInstance$duration =
		  ObfuscationReflectionUtil.getSoftField(
		    EffectInstance.class, "duration", "duration",
		    LOGGER::error, REFLECTION_ERROR_MESSAGE);
		
		private static final SoftField<EffectInstance, EffectInstance> EffectInstance$hiddenEffects =
		  ObfuscationReflectionUtil.getSoftField(
		    EffectInstance.class, "hiddenEffect", "hiddenEffects",
		    LOGGER::error, REFLECTION_ERROR_MESSAGE);
		
		public final String jsonName;
		public final TextFormatting color;
		public final float defValue;
		public final float consumption;
		public final Map<Effect, Integer> effects;
		public final Predicate<LootContext> condition;
		
		protected String translationKey;
		protected ResourceLocation registryName = null;
		
		public EffectAbility(
		  String jsonName, TextFormatting color, float defValue,
		  Map<Effect, Integer> effects,
		  Predicate<LootContext> condition,
		  float consumption
		) {
			this.jsonName = Objects.requireNonNull(jsonName);
			this.color = color != null? color : TextFormatting.GRAY;
			this.defValue = defValue;
			this.effects = effects != null? effects : new HashMap<>();
			this.condition = condition != null? condition : c -> true;
			this.consumption = consumption;
		}
		
		@Override public boolean testConditions(LootContext context) {
			return condition.test(context);
		}
		
		@Override public void reapplyEffect(ServerPlayerEntity player) {
			applyEffect(player);
		}
		
		@Override public void applyEffect(ServerPlayerEntity player) {
			for (Effect effect : effects.keySet()) {
				final EffectInstance active = player.getEffect(effect);
				final Integer level = effects.get(effect);
				if (active != null) {
					if (level <= active.getAmplifier()) {
						if (active.getDuration() > Integer.MAX_VALUE / 2) // Assume it's our effect
							EffectInstance$duration.set(active, Integer.MAX_VALUE);
						return;
					}
					final EffectInstance instance = new EffectInstance(effect, 0, level, true, false, false);
					instance.setNoCounter(true);
					player.addEffect(instance); // This updates `active` with `instance`s values and queues a copy of `active` in its hiddenEffects values, thus, the next line
					EffectInstance$duration.set(active, Integer.MAX_VALUE);
				} else {
					final EffectInstance instance = new EffectInstance(effect, Integer.MAX_VALUE, level, true, false, false);
					instance.setNoCounter(true);
					player.addEffect(instance);
				}
			}
		}
		
		@Override public void undoEffect(ServerPlayerEntity player) {
			final Map<Effect, EffectInstance> potionMap = player.getActiveEffectsMap();
			effects:for (Effect effect : effects.keySet()) {
				EffectInstance instance = potionMap.get(effect);
				if (instance.getAmplifier() == effects.get(effect)
				    && instance.getDuration() > Integer.MAX_VALUE / 2
				    && instance.isAmbient() && !instance.isVisible()) { // && instance.getIsPotionDurationMax()) {
					EffectInstance$duration.set(instance, 1);
					continue;
				}
				EffectInstance prev;
				for (int i = 0xFF; i >= 0; i--) { // I can't sleep leaving this uncapped
					prev = instance;
					instance = EffectInstance$hiddenEffects.get(prev);
					if (instance == null)
						continue effects;
					if (instance.getAmplifier() == effects.get(effect)
					    && instance.getDuration() > Integer.MAX_VALUE / 2
					    && instance.isAmbient() && !instance.isVisible()) { // && instance.getIsPotionDurationMax()) {
						// Remove instance from the linked array
						EffectInstance$hiddenEffects.set(prev, EffectInstance$hiddenEffects.get(instance));
						continue effects;
					}
				}
				LogUtil.errorOnce(LOGGER, "Aborted infinite (?) recursion in effect linked array\nSomeone messed up potions");
			}
		}
		
		@Override public float getConsumption() {
			return consumption;
		}
		
		@Override public String getName() {
			return jsonName;
		}
		
		@OnlyIn(Dist.CLIENT) @Override
		public IFormattableTextComponent getDisplayName() {
			if (registryName == null)
				throw new IllegalStateException("Cannot get display name of unregistered IEffectAbility");
			if (translationKey == null)
				translationKey = "aerobaticelytra.effect-abilities." + fullName().replace(':', '.');
			return I18n.exists(translationKey)? ttc(translationKey) : stc(jsonName);
		}
		
		@Override public DisplayType getDisplayType() {
			return DisplayType.BOOL;
		}
		
		@Override public float getDefault() {
			return defValue;
		}
		
		@Override public IAbility setRegistryName(ResourceLocation name) {
			if (registryName != null)
				throw new IllegalStateException("Cannot set the registry name of an EffectAbility");
			registryName = name;
			return this;
		}
		
		@Nullable @Override public ResourceLocation getRegistryName() {
			return registryName;
		}
		
		@Override public String toString() {
			return jsonName;
		}
		
		public static class Deserializer implements JsonDeserializer<EffectAbility> {
			public static final String EFFECT_ABILITY_TYPE = "aerobaticelytra:effect";
			
			@Override public EffectAbility deserialize(
			  JsonElement json, Type typeOfT, JsonDeserializationContext context
			) {
				if (!json.isJsonObject())
					throw new JsonParseException("Aerobatic elytra effect abilities must be JSON objects");
				final JsonObject obj = json.getAsJsonObject();
				final String type = JSONUtils.getAsString(obj, "type");
				if (!type.equals(EFFECT_ABILITY_TYPE))
					throw new JsonParseException("Unknown aerobatic elytra effect ability: '" + type + "'. Only known value is '" + EFFECT_ABILITY_TYPE + "'");
				final String jsonName = JSONUtils.getAsString(obj, "id");
				final String colorName = JSONUtils.getAsString(obj, "color", "GRAY");
				final TextFormatting color = TextFormatting.getByName(colorName);
				if (color == null || !color.isColor())
					throw new JsonParseException("Invalid aerobatic elytra effect ability: '" + colorName + "'");
				final float defValue = JSONUtils.getAsFloat(obj, "default", 0F);
				
				final JsonObject effectsObj = JSONUtils.getAsJsonObject(obj, "effects", new JsonObject());
				final Map<Effect, Integer> effects = new HashMap<>();
				for (Entry<String, JsonElement> entry : effectsObj.entrySet()) {
					final Effect effect = ForgeRegistries.POTIONS.getValue(new ResourceLocation(entry.getKey()));
					final JsonObject effectData = entry.getValue().getAsJsonObject();
					effects.put(effect, JSONUtils.getAsInt(effectData, "amplifier", 1) - 1);
				}
				
				final JsonArray conditionsArr = JSONUtils.getAsJsonArray(obj, "conditions", new JsonArray());
				// This makes re-serialization impossible
				//noinspection unchecked
				final Predicate<LootContext> condition = ((Stream<Predicate<LootContext>>) (Stream<?>)
				  Arrays.stream(context.<ILootCondition[]>deserialize(conditionsArr, ILootCondition[].class))
				).reduce(Predicate::and).orElse(c -> true);
				
				final float consumption = JSONUtils.getAsFloat(obj, "consumption", 0F);
				
				return new EffectAbility(jsonName, color, defValue, effects, condition, consumption);
			}
		}
	}
}
