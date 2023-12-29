package endorh.aerobaticelytra.client.item;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.lazulib.events.RegisterTextureAtlasEvent;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.Map;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value=Dist.CLIENT, modid=AerobaticElytra.MOD_ID, bus=Bus.MOD)
public class AerobaticElytraBannerTextureManager extends TextureAtlasHolder {
	
	public static final ResourceLocation AEROBATIC_ELYTRA_BANNER_SHEET = prefix("textures/atlas/aerobatic_elytra_banner_patterns.png");
	public static final ResourceLocation AEROBATIC_ELYTRA_BANNER_ATLAS = prefix("aerobatic_elytra_banner_patterns");

	private final Map<BannerPattern, Material> MATERIAL_CACHE = new HashMap<>();


	/**
	 * Must be constructed during the Minecraft constructor
	 * (e.g. in the {@link RegisterParticleProvidersEvent}).
	 */
	public AerobaticElytraBannerTextureManager(TextureManager manager) {
		super(manager, AEROBATIC_ELYTRA_BANNER_SHEET, AEROBATIC_ELYTRA_BANNER_ATLAS);
	}

	@SubscribeEvent
	public static void onRegisterTextureAtlas(RegisterTextureAtlasEvent event) {
		LogManager.getLogger().warn("Registering texture atlas");
		event.add(AEROBATIC_ELYTRA_BANNER_SHEET, AEROBATIC_ELYTRA_BANNER_ATLAS);
	}

	public TextureAtlasSprite getBannerSprite(BannerPattern pattern) {
		ResourceLocation key = BuiltInRegistries.BANNER_PATTERN.getKey(pattern);
		if (key == null) key = new ResourceLocation("missing");
		return getSprite(key);
	}

	public Material getBannerMaterial(BannerPattern pattern) {
		return MATERIAL_CACHE.computeIfAbsent(pattern, p -> new Material(
			AEROBATIC_ELYTRA_BANNER_SHEET,
			"b".equals(pattern.getHashname())
				? prefix("entity/aerobatic_elytra/base")
				: BuiltInRegistries.BANNER_PATTERN.getResourceKey(pattern).map(
					path -> prefix("entity/aerobatic_elytra/banner/" + path.location().getPath())
			).orElse(MissingTextureAtlasSprite.getLocation())));
	}
}
