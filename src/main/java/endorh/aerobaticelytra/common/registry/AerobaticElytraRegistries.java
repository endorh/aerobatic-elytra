package endorh.aerobaticelytra.common.registry;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.event.AerobaticElytraAbilitiesReloadedEvent;
import endorh.aerobaticelytra.common.flight.mode.FlightModes;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.common.item.AbilityReloadManager;
import endorh.aerobaticelytra.common.item.IAbility;
import endorh.aerobaticelytra.common.item.IAbility.Ability;
import endorh.aerobaticelytra.common.item.IDatapackAbility;
import endorh.aerobaticelytra.common.item.IEffectAbility;
import endorh.aerobaticelytra.common.recipe.UpgradeRecipe;
import endorh.lazulib.math.MathHighlighter.UnicodeMathSyntaxHighlightParser;
import endorh.lazulib.math.MathParser.ExpressionParser;
import endorh.lazulib.math.MathParser.FixedNamespaceSet;
import endorh.lazulib.math.MathParser.UnicodeMathDoubleExpressionParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraRegistries {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static IForgeRegistry<IFlightMode> FLIGHT_MODE_REGISTRY;
	public static List<IFlightMode> FLIGHT_MODE_LIST;
	public static ResourceKey<Registry<IFlightMode>> FLIGHT_MODE_REGISTRY_KEY;
	
	// Use getAbilities() insteaed
	private static IForgeRegistry<IAbility> ABILITY_REGISTRY;
	public static ResourceKey<Registry<IAbility>> ABILITY_REGISTRY_KEY;
	
	// Datapack abilities are stored in a map, since registries can't be modified at json reload time
	private static final Map<ResourceLocation, IAbility> ABILITIES = new HashMap<>();
	private static final Map<ResourceLocation, IDatapackAbility> DATAPACK_ABILITIES = new HashMap<>();
	private static final Map<ResourceLocation, IEffectAbility> EFFECT_ABILITIES = new HashMap<>();
	private static final Map<IAbility, ResourceLocation> ABILITY_NAMES = new HashMap<>();
	private static final Map<String, IAbility> JSON_TO_ABILITY = new HashMap<>();
	private static final Set<IDatapackAbility> OUTDATED_ABILITIES = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
	
	public static @Nullable ResourceLocation getAbilityKey(IAbility ability) {
		return ABILITY_NAMES.get(ability);
	}
	
	public static @Nullable IAbility getAbility(ResourceLocation id) {
		return ABILITIES.get(id);
	}
	
	public static Map<ResourceLocation, IAbility> getAbilities() {
		return Collections.unmodifiableMap(ABILITIES);
	}
	
	@SuppressWarnings("unused")
	public static Map<ResourceLocation, IEffectAbility> getEffectAbilities() {
		return Collections.unmodifiableMap(EFFECT_ABILITIES);
	}
	
	public static @Nullable IAbility getAbilityByName(String name) {
		return JSON_TO_ABILITY.get(name);
	}
	
	public static boolean hasAbilityName(String jsonName) {
		return JSON_TO_ABILITY.containsKey(jsonName);
	}
	
	@SuppressWarnings("unused")
	public static Map<String, IAbility> getAbilitiesByName() {
		return Collections.unmodifiableMap(JSON_TO_ABILITY);
	}
	
	public static Set<IDatapackAbility> getOutdatedAbilities() {
		return Collections.unmodifiableSet(OUTDATED_ABILITIES);
	}
	
	public static Map<ResourceLocation, IDatapackAbility> getDatapackAbilities() {
		return Collections.unmodifiableMap(DATAPACK_ABILITIES);
	}
	
	public static ExpressionParser<Double> ABILITY_EXPRESSION_PARSER;
	public static UnicodeMathSyntaxHighlightParser ABILITY_EXPRESSION_HIGHLIGHTER;
	
	@SubscribeEvent
	public static void onNewRegistry(NewRegistryEvent event) {
		event.create(
		  new RegistryBuilder<IFlightMode>()
		    .setName(prefix("flight_modes"))
		    .allowModification()
		    .onBake(AerobaticElytraRegistries::onFlightModeRegistryBake),
		  r -> {
			  FLIGHT_MODE_REGISTRY = r;
			  FLIGHT_MODE_REGISTRY_KEY = r.getRegistryKey();
		  });
		
		event.create(
		  new RegistryBuilder<IAbility>()
		    .setName(prefix("ability"))
		    .allowModification()
		    .onBake(AerobaticElytraRegistries::onAbilityRegistryBake),
		  r -> {
			  ABILITY_REGISTRY = r;
			  ABILITY_REGISTRY_KEY = r.getRegistryKey();
		  });
		
		AerobaticElytra.logRegistered("Registries");
	}
	
	public static void onFlightModeRegistryBake(
	  IForgeRegistryInternal<IFlightMode> owner, RegistryManager stage
	) {
		FLIGHT_MODE_LIST = new ArrayList<>(owner.getValues());
		FLIGHT_MODE_LIST.sort(Comparator.comparingInt(IFlightMode::getRegistryOrder));
	}
	
	public static void onAbilityRegistryBake(
	  IForgeRegistryInternal<IAbility> owner, RegistryManager stage
	) { bakeAbilities(); }
	
	public static void reloadDatapackAbilities(Map<ResourceLocation, ? extends IDatapackAbility> abilities) {
		OUTDATED_ABILITIES.addAll(DATAPACK_ABILITIES.values());
		DATAPACK_ABILITIES.clear();
		DATAPACK_ABILITIES.putAll(abilities);
		bakeAbilities();
	}
	
	private static void bakeAbilities() {
		ABILITIES.clear();
		ABILITY_NAMES.clear();
		JSON_TO_ABILITY.clear();
		EFFECT_ABILITIES.clear();
		
		for (Entry<ResourceKey<IAbility>, IAbility> e: ABILITY_REGISTRY.getEntries()) {
			ABILITIES.put(e.getKey().location(), e.getValue());
			ABILITY_NAMES.put(e.getValue(), e.getKey().location());
		}
		for (Entry<ResourceLocation, IDatapackAbility> entry : DATAPACK_ABILITIES.entrySet()) {
			if (ABILITIES.containsKey(entry.getKey())) {
				LOGGER.warn("Datapack Aerobatic Elytra Ability conflicts with one already defined by " +
				            "a mod: \"" + entry.getKey() + "\". The datapack one will be ignored");
			} else {
				ABILITIES.put(entry.getKey(), entry.getValue());
				ABILITY_NAMES.put(entry.getValue(), entry.getKey());
			}
		}
		// final Map<String, Map<String, IAbility>> namespaceSet = new HashMap<>();
		final Map<String, ChatFormatting> abilityColors = new HashMap<>();
		final Map<String, MutableComponent> abilityTranslations = new HashMap<>();
		for (ResourceLocation id : ABILITIES.keySet()) {
			final String namespace = id.getNamespace();
			final IAbility ability = ABILITIES.get(id);
			final String fullName = namespace + ':' + ability.getName();
			JSON_TO_ABILITY.put(fullName, ability);
			abilityColors.put(fullName, ability.getColor());
			abilityTranslations.put(fullName, ability.getDisplayName());
			if (ability instanceof IEffectAbility)
				EFFECT_ABILITIES.put(id, (IEffectAbility) ability);
		}
		final FixedNamespaceSet<Double> namespaceSet = FixedNamespaceSet.of(JSON_TO_ABILITY.keySet());
		ABILITY_EXPRESSION_PARSER = new UnicodeMathDoubleExpressionParser(namespaceSet);
		ABILITY_EXPRESSION_HIGHLIGHTER = new UnicodeMathSyntaxHighlightParser(abilityColors, abilityTranslations);
		
		// Add shortcuts
		for (Entry<String, Pair<String, String>> entry : namespaceSet.getShortcuts().entrySet()) {
			final Pair<String, String> pair = entry.getValue();
			final String name = pair.getKey().replace('`', '_') + ':' + pair.getValue();
			JSON_TO_ABILITY.put(entry.getKey(), JSON_TO_ABILITY.get(name));
		}
		
		// Reparse upgrade recipes
		UpgradeRecipe.onAbilityReload();
		AbilityReloadManager.onAbilityReload();
		MinecraftForge.EVENT_BUS.post(new AerobaticElytraAbilitiesReloadedEvent());
	}
	
	@SubscribeEvent
	public static void onRegisterFlightModes(RegisterEvent event) {
		event.register(FLIGHT_MODE_REGISTRY_KEY, h -> {
			for (FlightModes mode: FlightModes.values())
				h.register(prefix(mode.name().toLowerCase()), mode);
			AerobaticElytra.logRegistered("Flight Modes");
		});
		event.register(ABILITY_REGISTRY_KEY, h -> {
			for (Ability ability: Ability.values())
				h.register(prefix(ability.name().toLowerCase()), ability);
			AerobaticElytra.logRegistered("Abilities");
		});
	}
}
