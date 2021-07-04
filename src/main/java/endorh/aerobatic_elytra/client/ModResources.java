package endorh.aerobatic_elytra.client;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.item.ModItems;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.aerobatic_elytra.AerobaticElytra.prefix;

/**
 * Centralization of client {@link ResourceLocation}s
 */
@EventBusSubscriber(value=Dist.CLIENT, bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class ModResources {
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static final ResourceLocation FLIGHT_GUI_ICONS_LOCATION =
	  prefix("textures/gui/flight_icons.png");
	public static final ResourceLocation TEXTURE_AEROBATIC_ELYTRA =
	  prefix("textures/entity/aerobatic_elytra.png");
	public static final ResourceLocation TEXTURE_CHEST_ROCKETS =
	  prefix("textures/models/armor/aerobatic_elytra_chest_rockets.png");
	
	@SubscribeEvent
	public static void onAtlasStitching(TextureStitchEvent.Pre event) {
		ResourceLocation stitching = event.getMap().getTextureLocation();
		// TODO: Stitch to separate atlas just like shields & banners
		if (stitching.equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE)) {
			for (BannerPattern pattern : BannerPattern.values()) {
				ResourceLocation texture = ModItems.AEROBATIC_ELYTRA
				  .getTextureLocation(pattern);
				boolean succeeded = event.addSprite(texture);
				if (!succeeded) {
					LOGGER.error("Failed to add " + texture + " to atlas!");
				} else {
					LOGGER.debug("Added " + texture + " to atlas");
				}
			}
			AerobaticElytra.logRegistered("Banner Textures");
		}
	}
}

