package dnj.aerobatic_elytra.common;

import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.client.config.ClientConfig;
import dnj.aerobatic_elytra.client.input.KeyHandler;
import dnj.aerobatic_elytra.client.item.AerobaticElytraItemColor;
import dnj.aerobatic_elytra.client.item.AerobaticElytraWingItemColor;
import dnj.aerobatic_elytra.client.item.ModItemProperties;
import dnj.aerobatic_elytra.common.item.AerobaticElytraItem;
import dnj.aerobatic_elytra.common.item.ModItems;
import dnj.aerobatic_elytra.common.recipe.ModRecipes;
import dnj.aerobatic_elytra.common.config.Config;
import dnj.aerobatic_elytra.integration.colytra.ClientColytraIntegration;
import dnj.aerobatic_elytra.integration.colytra.ColytraIntegration;
import dnj.aerobatic_elytra.integration.curios.CuriosIntegration;
import dnj.aerobatic_elytra.server.command.QualifiedNameArgumentType;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModInit {
	
	/**
	 * Register deferred registries and configs
	 */
	public static void setup() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);
		
		Config.register();
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientConfig::register);
		registerIntegrations();
		AerobaticElytra.logRegistered("Config");
		registerArgumentTypes();
		
		MinecraftForge.EVENT_BUS.addListener(ModInit::onRegisterReloadListeners);
	}
	
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		registerClient();
		setupClient();
	}
	
	public static void onRegisterReloadListeners(AddReloadListenerEvent event) {
		event.addListener(AerobaticElytra.JSON_ABILITY_MANAGER);
	}
	
	public static void registerClient() {
		AerobaticElytraItemColor.register(ModItems.AEROBATIC_ELYTRA);
		AerobaticElytraWingItemColor.register(ModItems.AEROBATIC_ELYTRA_WING);
		AerobaticElytra.logRegistered("Item Colors");
		ModItemProperties.register();
		AerobaticElytra.logRegistered("Item Properties");
		KeyHandler.register();
	}
	
	public static void setupClient() {
		AerobaticElytraItem.onClientSetup();
	}
	
	public static void registerIntegrations() {
	    final IEventBus eventBus = MinecraftForge.EVENT_BUS;
	    if (AerobaticElytra.caelusLoaded && AerobaticElytra.colytraLoaded) {
	        eventBus.register(ColytraIntegration.class);
	        if (FMLEnvironment.dist == Dist.CLIENT)
	            eventBus.register(ClientColytraIntegration.class);
	    }
	    if (AerobaticElytra.caelusLoaded && AerobaticElytra.curiosLoaded)
	        eventBus.register(CuriosIntegration.class);
	}
	
	public static void registerArgumentTypes() {
		ArgumentTypes.register(
		  "aerobatic-elytra:qualified_name", QualifiedNameArgumentType.class,
		  new QualifiedNameArgumentType.Serializer());
	}
}
