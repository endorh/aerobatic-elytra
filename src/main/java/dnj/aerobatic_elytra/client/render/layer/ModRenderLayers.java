package dnj.aerobatic_elytra.client.render.layer;

import dnj.aerobatic_elytra.AerobaticElytra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModRenderLayers {
	@SubscribeEvent
	public static void postSetup(FMLLoadCompleteEvent evt) {
		EntityRendererManager rendererManager = Minecraft.getInstance().getRenderManager();
		rendererManager.getSkinMap().values()
		  .forEach(renderer -> renderer.addLayer(new AerobaticElytraLayer<>(renderer)));
		AerobaticElytra.logRegistered("Render Layers");
	}
}
