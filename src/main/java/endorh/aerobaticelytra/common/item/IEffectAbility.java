package endorh.aerobaticelytra.common.item;

import com.google.gson.*;
import endorh.util.common.LogUtil;
import endorh.util.common.ObfuscationReflectionUtil;
import endorh.util.common.ObfuscationReflectionUtil.SoftField;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
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
	
	void applyEffect(ServerPlayer player);
	
	void reapplyEffect(ServerPlayer player);
	
	void undoEffect(ServerPlayer player);
	
	float getConsumption();
	
	class EffectAbility implements IEffectAbility {
		private static final String REFLECTION_ERROR_MESSAGE =
		  "Aerobatic Elytra Effect Abilities (if provided by datapacks) may not apply correctly";
		private static final Logger LOGGER = LogManager.getLogger();
		
		private static final SoftField<MobEffectInstance, Integer> EffectInstance$duration =
		  ObfuscationReflectionUtil.getSoftField(
			 MobEffectInstance.class, "duration", "duration",
			 LOGGER::error, REFLECTION_ERROR_MESSAGE);
		
		private static final SoftField<MobEffectInstance, MobEffectInstance>
		  EffectInstance$hiddenEffects =
		  ObfuscationReflectionUtil.getSoftField(
			 MobEffectInstance.class, "hiddenEffect", "hiddenEffects",
			 LOGGER::error, REFLECTION_ERROR_MESSAGE);
		
		public final String jsonName;
		public final ChatFormatting color;
		public final float defValue;
		public final float consumption;
		public final Map<MobEffect, Integer> effects;
		public final Predicate<LootContext> condition;
		
		protected String translationKey;
		protected ResourceLocation registryName = null;
		
		public EffectAbility(
		  String jsonName, ChatFormatting color, float defValue,
		  Map<MobEffect, Integer> effects,
		  Predicate<LootContext> condition,
		  float consumption
		) {
			this.jsonName = Objects.requireNonNull(jsonName);
			this.color = color != null? color : ChatFormatting.GRAY;
			this.defValue = defValue;
			this.effects = effects != null? effects : new HashMap<>();
			this.condition = condition != null? condition : c -> true;
			this.consumption = consumption;
		}
		
		@Override public boolean testConditions(LootContext context) {
			return condition.test(context);
		}
		
		@Override public void reapplyEffect(ServerPlayer player) {
			applyEffect(player);
		}
		
		@Override public void applyEffect(ServerPlayer player) {
			for (MobEffect effect: effects.keySet()) {
				final MobEffectInstance active = player.getEffect(effect);
				final Integer level = effects.get(effect);
				if (active != null) {
					if (level <= active.getAmplifier()) {
						if (active.getDuration() > Integer.MAX_VALUE / 2) // Assume it's our effect
							EffectInstance$duration.set(active, Integer.MAX_VALUE);
						return;
					}
					final MobEffectInstance instance =
					  new MobEffectInstance(effect, 0, level, true, false, false);
					instance.setNoCounter(true);
					player.addEffect(
					  instance); // This updates `active` with `instance`s values and queues a copy of `active` in its hiddenEffects values, thus, the next line
					EffectInstance$duration.set(active, Integer.MAX_VALUE);
				} else {
					final MobEffectInstance instance =
					  new MobEffectInstance(effect, Integer.MAX_VALUE, level, true, false, false);
					instance.setNoCounter(true);
					player.addEffect(instance);
				}
			}
		}
		
		@Override public void undoEffect(ServerPlayer player) {
			final Map<MobEffect, MobEffectInstance> potionMap = player.getActiveEffectsMap();
			effects:
			for (MobEffect effect: effects.keySet()) {
				MobEffectInstance instance = potionMap.get(effect);
				if (instance.getAmplifier() == effects.get(effect)
				    && instance.getDuration() > Integer.MAX_VALUE / 2
				    && instance.isAmbient() &&
				    !instance.isVisible()) { // && instance.getIsPotionDurationMax()) {
					EffectInstance$duration.set(instance, 1);
					continue;
				}
				MobEffectInstance prev;
				for (int i = 0xFF; i >= 0; i--) { // I can't sleep leaving this uncapped
					prev = instance;
					instance = EffectInstance$hiddenEffects.get(prev);
					if (instance == null)
						continue effects;
					if (instance.getAmplifier() == effects.get(effect)
					    && instance.getDuration() > Integer.MAX_VALUE / 2
					    && instance.isAmbient() &&
					    !instance.isVisible()) { // && instance.getIsPotionDurationMax()) {
						// Remove instance from the linked array
						EffectInstance$hiddenEffects.set(
						  prev, EffectInstance$hiddenEffects.get(instance));
						continue effects;
					}
				}
				LogUtil.errorOnce(
				  LOGGER,
				  "Aborted infinite (?) recursion in effect linked array\nSomeone messed up potions");
			}
		}
		
		@Override public float getConsumption() {
			return consumption;
		}
		
		@Override public String getName() {
			return jsonName;
		}
		
		@OnlyIn(Dist.CLIENT) @Override
		public MutableComponent getDisplayName() {
			if (registryName == null)
				throw new IllegalStateException(
				  "Cannot get display name of unregistered IEffectAbility");
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
					throw new JsonParseException(
					  "Aerobatic elytra effect abilities must be JSON objects");
				final JsonObject obj = json.getAsJsonObject();
				final String type = GsonHelper.getAsString(obj, "type");
				if (!type.equals(EFFECT_ABILITY_TYPE))
					throw new JsonParseException(
					  "Unknown aerobatic elytra effect ability: '" + type + "'. Only known value is '" +
					  EFFECT_ABILITY_TYPE + "'");
				final String jsonName = GsonHelper.getAsString(obj, "id");
				final String colorName = GsonHelper.getAsString(obj, "color", "GRAY");
				final ChatFormatting color = ChatFormatting.getByName(colorName);
				if (color == null || !color.isColor())
					throw new JsonParseException(
					  "Invalid aerobatic elytra effect ability: '" + colorName + "'");
				final float defValue = GsonHelper.getAsFloat(obj, "default", 0F);
				
				final JsonObject effectsObj =
				  GsonHelper.getAsJsonObject(obj, "effects", new JsonObject());
				final Map<MobEffect, Integer> effects = new HashMap<>();
				for (Entry<String, JsonElement> entry: effectsObj.entrySet()) {
					final MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(entry.getKey()));
					final JsonObject effectData = entry.getValue().getAsJsonObject();
					effects.put(effect, GsonHelper.getAsInt(effectData, "amplifier", 1) - 1);
				}
				
				final JsonArray conditionsArr =
				  GsonHelper.getAsJsonArray(obj, "conditions", new JsonArray());
				// This makes re-serialization impossible
				//noinspection unchecked
				final Predicate<LootContext> condition = ((Stream<Predicate<LootContext>>) (Stream<?>)
				  Arrays.stream(
				    context.<LootItemCondition[]>deserialize(conditionsArr, LootItemCondition[].class))
				).reduce(Predicate::and).orElse(c -> true);
				
				final float consumption = GsonHelper.getAsFloat(obj, "consumption", 0F);
				
				return new EffectAbility(jsonName, color, defValue, effects, condition, consumption);
			}
		}
	}
}
