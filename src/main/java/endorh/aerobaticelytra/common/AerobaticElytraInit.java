package endorh.aerobaticelytra.common;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.item.AerobaticElytraBannerTextureManager;
import endorh.aerobaticelytra.client.item.AerobaticElytraItemColor;
import endorh.aerobaticelytra.client.item.AerobaticElytraWingItemColor;
import endorh.aerobaticelytra.client.item.AerobaticItemProperties;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.item.AerobaticElytraItems;
import endorh.aerobaticelytra.common.recipe.AerobaticRecipes;
import endorh.aerobaticelytra.integration.caelus.CaelusIntegration;
import endorh.aerobaticelytra.integration.colytra.ClientColytraIntegration;
import endorh.aerobaticelytra.integration.colytra.ColytraIntegration;
import endorh.aerobaticelytra.integration.curios.CuriosIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticElytraInit {
	
	/**
	 * Register deferred registries and configs
	 */
	public static void setup() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		AerobaticRecipes.RECIPE_SERIALIZERS.register(modEventBus);
		
		Config.register();
		DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientConfig::register);
		AerobaticElytra.logRegistered("Config");
		
		registerIntegrations();
	}
	
	@SubscribeEvent public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(AerobaticElytraInit::registerClient);
	}
	
	public static void registerClient() {
		AerobaticElytraItemColor.register(AerobaticElytraItems.AEROBATIC_ELYTRA);
		AerobaticElytraWingItemColor.register(AerobaticElytraItems.AEROBATIC_ELYTRA_WING);
		AerobaticElytra.logRegistered("Item Colors");
		AerobaticItemProperties.register();
		AerobaticElytra.logRegistered("Item Properties");
	}
	
	public static void registerIntegrations() {
	    final IEventBus eventBus = MinecraftForge.EVENT_BUS;
	    if (AerobaticElytra.caelusLoaded && AerobaticElytra.colytraLoaded) {
	        eventBus.register(ColytraIntegration.class);
	        if (FMLEnvironment.dist == Dist.CLIENT)
	            eventBus.register(ClientColytraIntegration.class);
	    }
	    if (AerobaticElytra.curiosLoaded) {
		    eventBus.register(CuriosIntegration.class);
			 if (AerobaticElytra.caelusLoaded)
				 eventBus.register(CaelusIntegration.class);
	    }
	}
	
	@SubscribeEvent public static void enqueueIMC(InterModEnqueueEvent event) {
		if (AerobaticElytra.curiosLoaded) InterModComms.sendTo(
		  CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
		  () -> SlotTypePreset.BACK.getMessageBuilder().build());
	}
	
	@EventBusSubscriber(value=Dist.CLIENT, bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
	@OnlyIn(Dist.CLIENT)
	public static class ClientRegistrar {
		/**
		 * Texture atlases are injected during the {@link Minecraft} constructor, after the
		 * {@link ModelManager} has been created.<br>
		 * <br>
		 * The best event for this is {@link RegisterParticleProvidersEvent},
		 * even if unrelated to its intended usage
		 */
		@SubscribeEvent
		public static void onMinecraftConstructed(RegisterParticleProvidersEvent event) {
			Minecraft mc = Minecraft.getInstance();
			AerobaticElytra.BANNER_TEXTURE_MANAGER = new AerobaticElytraBannerTextureManager(
				mc.getTextureManager());
			if (mc.getResourceManager() instanceof ReloadableResourceManager rm)
            rm.registerReloadListener(AerobaticElytra.BANNER_TEXTURE_MANAGER);
		}
	}
	
	@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
	public static class GameEventRegistrar {
		@SubscribeEvent
		public static void onRegisterReloadListeners(AddReloadListenerEvent event) {
			event.addListener(AerobaticElytra.JSON_ABILITY_MANAGER);
		}
	}
}
