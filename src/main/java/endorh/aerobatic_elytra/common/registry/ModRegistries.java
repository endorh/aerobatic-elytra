package endorh.aerobatic_elytra.common.registry;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.event.AerobaticElytraAbilitiesReloadedEvent;
import endorh.aerobatic_elytra.common.flight.mode.FlightModes;
import endorh.aerobatic_elytra.common.flight.mode.IFlightMode;
import endorh.aerobatic_elytra.common.item.AbilityReloadManager;
import endorh.aerobatic_elytra.common.item.IAbility;
import endorh.aerobatic_elytra.common.item.IDatapackAbility;
import endorh.aerobatic_elytra.common.item.IEffectAbility;
import endorh.aerobatic_elytra.common.recipe.UpgradeRecipe;
import endorh.util.math.MathHighlighter.UnicodeMathSyntaxHighlightParser;
import endorh.util.math.MathParser.ExpressionParser;
import endorh.util.math.MathParser.FixedNamespaceSet;
import endorh.util.math.MathParser.UnicodeMathDoubleExpressionParser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModRegistries {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static IForgeRegistry<IFlightMode> FLIGHT_MODE_REGISTRY;
	public static List<IFlightMode> FLIGHT_MODE_LIST;
	
	// Use getAbilities() insteaed
	private static IForgeRegistry<IAbility> ABILITY_REGISTRY;
	
	// Datapack abilities are stored in a map, since registries can't be modified at json reload time
	private static final Map<ResourceLocation, IAbility> ABILITIES = new HashMap<>();
	private static final Map<ResourceLocation, IDatapackAbility> DATAPACK_ABILITIES = new HashMap<>();
	private static final Map<ResourceLocation, IEffectAbility> EFFECT_ABILITIES = new HashMap<>();
	private static final Map<String, IAbility> JSON_TO_ABILITY = new HashMap<>();
	private static final Set<IDatapackAbility> OUTDATED_ABILITIES = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
	
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
	public static void onNewRegistry(RegistryEvent.NewRegistry event) {
		FLIGHT_MODE_REGISTRY = new RegistryBuilder<IFlightMode>()
		  .setName(prefix("flight_modes"))
		  .setType(IFlightMode.class)
		  .allowModification()
		  .onBake(ModRegistries::onFlightModeRegistryBake)
		  .create();
		
		ABILITY_REGISTRY = new RegistryBuilder<IAbility>()
		  .setName(prefix("ability"))
		  .setType(IAbility.class)
		  .allowModification()
		  .onBake(ModRegistries::onAbilityRegistryBake)
		  .create();
		
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
	
	public static void reloadDatapackAbilities(Collection<? extends IDatapackAbility> abilities) {
		OUTDATED_ABILITIES.addAll(DATAPACK_ABILITIES.values());
		DATAPACK_ABILITIES.clear();
		for (IDatapackAbility ability : abilities)
			DATAPACK_ABILITIES.put(ability.getRegistryName(), ability);
		bakeAbilities();
	}
	
	private static void bakeAbilities() {
		ABILITIES.clear();
		JSON_TO_ABILITY.clear();
		EFFECT_ABILITIES.clear();
		
		for (IAbility ability : ABILITY_REGISTRY)
			ABILITIES.put(ability.getRegistryName(), ability);
		for (Entry<ResourceLocation, IDatapackAbility> entry : DATAPACK_ABILITIES.entrySet()) {
			if (ABILITIES.containsKey(entry.getKey())) {
				LOGGER.warn("Datapack Aerobatic Elytra Ability conflicts with one already defined by " +
				            "a mod: \"" + entry.getKey() + "\". The datapack one will be ignored");
			} else ABILITIES.put(entry.getKey(), entry.getValue());
		}
		// final Map<String, Map<String, IAbility>> namespaceSet = new HashMap<>();
		final Map<String, TextFormatting> abilityColors = new HashMap<>();
		final Map<String, IFormattableTextComponent> abilityTranslations = new HashMap<>();
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
	public static void onRegisterFlightModes(RegistryEvent.Register<IFlightMode> event) {
		final IForgeRegistry<IFlightMode> reg = event.getRegistry();
		reg.registerAll(FlightModes.values());
		AerobaticElytra.logRegistered("Flight Modes");
	}
	
	@SubscribeEvent
	public static void onRegisterAbilities(RegistryEvent.Register<IAbility> event) {
		final IForgeRegistry<IAbility> reg = event.getRegistry();
		for (IAbility type : IAbility.Ability.values())
			reg.register(type);
	}
}
