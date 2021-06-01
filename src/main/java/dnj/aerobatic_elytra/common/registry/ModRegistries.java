package dnj.aerobatic_elytra.common.registry;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.common.flight.mode.FlightModes;
import dnj.aerobatic_elytra.common.flight.mode.IFlightMode;
import dnj.aerobatic_elytra.common.item.IAbility;
import dnj.endor8util.math.MathParser.ExpressionParser;
import dnj.endor8util.math.MathParser.UnicodeMathDoubleExpressionParser;
import dnj.endor8util.util.TextUtil.UnicodeMathSyntaxHighlightParser;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryInternal;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryManager;

import java.util.*;
import java.util.stream.Collectors;

import static dnj.aerobatic_elytra.AerobaticElytra.prefix;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModRegistries {
	public static IForgeRegistry<IFlightMode> FLIGHT_MODE_REGISTRY;
	public static List<IFlightMode> FLIGHT_MODE_LIST;
	
	public static IForgeRegistry<IAbility> ABILITY_REGISTRY;
	private static Map<String, IAbility> JSON_TO_ABILITY;
	
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
	) {
		final Set<String> abilities = owner.getValues().stream().map(IAbility::jsonName)
		  .collect(Collectors.toSet());
		ABILITY_EXPRESSION_PARSER = new UnicodeMathDoubleExpressionParser(abilities);
		JSON_TO_ABILITY = new HashMap<>();
		final Map<String, TextFormatting> abilityColors = new HashMap<>();
		final Map<String, IFormattableTextComponent> abilityTranslations = new HashMap<>();
		for (IAbility ability : owner.getValues()) {
			final String name = ability.jsonName();
			abilityColors.put(name, ability.getColor());
			abilityTranslations.put(name, ability.getDisplayName());
			JSON_TO_ABILITY.put(name, ability);
		}
		ABILITY_EXPRESSION_HIGHLIGHTER = new UnicodeMathSyntaxHighlightParser(
		  abilityColors
		);
		ABILITY_EXPRESSION_HIGHLIGHTER.translations.putAll(abilityTranslations);
	}
	
	public static IAbility abilityFromJsonName(String jsonName) {
		return JSON_TO_ABILITY.get(jsonName);
	}
	
	public static boolean hasAbility(String jsonName) {
		return JSON_TO_ABILITY.containsKey(jsonName);
	}
	
	@SubscribeEvent
	public static void onRegisterFlightModes(RegistryEvent.Register<IFlightMode> event) {
		final IForgeRegistry<IFlightMode> reg = event.getRegistry();
		reg.registerAll(FlightModes.values());
		AerobaticElytra.logRegistered("Flight Modes");
	}
	
	@SubscribeEvent
	public static void onRegisterUpgradeTypes(RegistryEvent.Register<IAbility> event) {
		final IForgeRegistry<IAbility> reg = event.getRegistry();
		for (IAbility type : IAbility.Ability.values())
			reg.register(type);
	}
}
