package endorh.aerobaticelytra.client.item;

import endorh.util.common.ObfuscationReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

import java.util.HashMap;
import java.util.Map;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

@OnlyIn(Dist.CLIENT)
public class AerobaticElytraBannerTextureManager extends TextureAtlasHolder {
	
	public static final ResourceLocation LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS = prefix("textures/atlas/aerobatic_elytra_banner_patterns.png");
	public static final ResourceLocation AEROBATIC_ELYTRA_BANNER_ATLAS = prefix("aerobatic_elytra_banner_patterns");
	protected final TextureAtlas atlas = new TextureAtlas(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS);

	private final Map<BannerPattern, Material> MATERIAL_CACHE = new HashMap<>();

	// Reflection
	protected static final ObfuscationReflectionUtil.SoftField<ModelManager, Map<ResourceLocation, ResourceLocation>>
		ModelManager$VANILLA_ATLASES = ObfuscationReflectionUtil.getSoftField(
			ModelManager.class, "f_244614_");
	protected static final ObfuscationReflectionUtil.SoftField<AtlasSet, Map<ResourceLocation, AtlasSet.AtlasEntry>>
		AtlasSet$atlas = ObfuscationReflectionUtil.getSoftField(AtlasSet.class, "f_244518_");
	protected static final ObfuscationReflectionUtil.SoftField<ModelManager, AtlasSet> ModelManager$atlases =
		ObfuscationReflectionUtil.getSoftField(ModelManager.class, "f_119398_");

	/**
	 * Must be constructed during the Minecraft constructor
	 * (e.g. in the {@link RegisterParticleProvidersEvent}).
	 */
	public AerobaticElytraBannerTextureManager(TextureManager manager) {
		super(manager, LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS, AEROBATIC_ELYTRA_BANNER_ATLAS);

		// Add atlas (not thread-safe)
		ModelManager modelManager = Minecraft.getInstance().getModelManager();
		Map<ResourceLocation, ResourceLocation> original = ModelManager$VANILLA_ATLASES.get(modelManager);
		if (original == null) throw new IllegalStateException("Could not access ModelManager$VANILLA_ATLASES");
		Map<ResourceLocation, ResourceLocation> copy = new HashMap<>(original);
		copy.put(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS, AEROBATIC_ELYTRA_BANNER_ATLAS);
		ModelManager$VANILLA_ATLASES.set(modelManager, copy);

		AtlasSet atlasSet = ModelManager$atlases.get(modelManager);
		if (atlasSet == null) throw new IllegalStateException("Could not access ModelManager$atlases");
		Map<ResourceLocation, AtlasSet.AtlasEntry> atlases = AtlasSet$atlas.get(atlasSet);
		if (atlases == null) throw new IllegalStateException("Could not access AtlasSet$atlas");
		manager.register(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS, atlas);
		atlases.put(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS, new AtlasSet.AtlasEntry(atlas, AEROBATIC_ELYTRA_BANNER_ATLAS));
	}

	public TextureAtlasSprite getBannerSprite(BannerPattern pattern) {
		ResourceLocation key = BuiltInRegistries.BANNER_PATTERN.getKey(pattern);
		if (key == null) key = new ResourceLocation("missing");
		return getSprite(key);
	}

	public Material getBannerMaterial(BannerPattern pattern) {
		return MATERIAL_CACHE.computeIfAbsent(pattern, p -> new Material(
			LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS,
			"b".equals(pattern.getHashname())
				? prefix("entity/aerobatic_elytra/base")
				: BuiltInRegistries.BANNER_PATTERN.getResourceKey(pattern).map(
					path -> prefix("entity/aerobatic_elytra/banner/" + path.location().getPath())
			).orElse(MissingTextureAtlasSprite.getLocation())));
	}
}
