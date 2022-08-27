package endorh.aerobaticelytra.common;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.input.KeyHandler;
import endorh.aerobaticelytra.client.item.AerobaticElytraBannerTextureManager;
import endorh.aerobaticelytra.client.item.AerobaticElytraItemColor;
import endorh.aerobaticelytra.client.item.AerobaticElytraWingItemColor;
import endorh.aerobaticelytra.client.item.ModItemProperties;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.item.ModItems;
import endorh.aerobaticelytra.common.recipe.ModRecipes;
import endorh.aerobaticelytra.integration.colytra.ClientColytraIntegration;
import endorh.aerobaticelytra.integration.colytra.ColytraIntegration;
import endorh.aerobaticelytra.integration.curios.CuriosIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
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
		AerobaticElytra.logRegistered("Config");
		
		
		registerIntegrations();
		MinecraftForge.EVENT_BUS.addListener(ModInit::onRegisterReloadListeners);
	}
	
	/**
	 * Reload listeners must be registered before the {@link Minecraft} constructor
	 * calls Minecraft#resourceManager#reloadResources.<br>
	 * The best event for this is {@link ParticleFactoryRegisterEvent},
	 * even if unrelated to its intended usage
	 */
	@SubscribeEvent
	public static void onMinecraftConstructed(ParticleFactoryRegisterEvent event) {
		final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		if (resourceManager instanceof ReloadableResourceManager)
			AerobaticElytra.BANNER_TEXTURE_MANAGER = new AerobaticElytraBannerTextureManager((ReloadableResourceManager) resourceManager);
	}
	
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		// These registering methods are not thread-safe
		event.enqueueWork(ModInit::registerClient);
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
		AerobaticElytra.logRegistered("Key Bindings");
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
}
