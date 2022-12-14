package endorh.aerobaticelytra.client.item;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.util.common.ObfuscationReflectionUtil;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlas.Preparations;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class AerobaticElytraBannerTextureManager extends SimplePreparableReloadListener<Preparations> {
	
	public static final ResourceLocation LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS = AerobaticElytra.prefix("aerobatic_elytra_banner");
	protected final TextureAtlas atlas = new TextureAtlas(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS);
	
	protected static final Set<Material> ModelBakery$UNREFERENCED_TEXTURES;
	
	static {
		//noinspection unchecked
		ModelBakery$UNREFERENCED_TEXTURES =
		  (Set<Material>) ObfuscationReflectionUtil.getStaticFieldValue(
		    ModelBakery.class, "f_119234_"
		  ).orElseThrow(() -> new IllegalStateException(
		    "Could not access ModelBakery$UNREFERENCED_TEXTURES"));
	}
	
	public AerobaticElytraBannerTextureManager(ReloadableResourceManager resourceManager) {
		resourceManager.registerReloadListener(this);
		// Add render materials (not thread-safe)
		for (BannerPattern pattern: Registry.BANNER_PATTERN) {
			ModelBakery$UNREFERENCED_TEXTURES.add(
			  new Material(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS, getTextureLocation(pattern)));
		}
	}
	
	public ResourceLocation getTextureLocation(BannerPattern pattern) {
		return new ResourceLocation(
		  AerobaticElytra.MOD_ID, "entity/aerobatic_elytra/" + Registry.BANNER_PATTERN.getResourceKey(pattern)
		  .map(k -> k.location().getPath()).orElse("missing"));
	}
	
	@Override protected @NotNull Preparations prepare(@NotNull ResourceManager resourceManager, ProfilerFiller profiler) {
		profiler.startTick();
		profiler.push("stitching");
		final Preparations sheetData = atlas.prepareToStitch(
		  resourceManager, Registry.BANNER_PATTERN.stream().map(this::getTextureLocation),
		  profiler, 2);
		profiler.pop();
		profiler.endTick();
		return sheetData;
	}
	
	@Override protected void apply(
	  @NotNull Preparations sheetData, @NotNull ResourceManager resourceManager,
	  @NotNull ProfilerFiller profiler
	) {
		profiler.startTick();
		profiler.push("upload");
		atlas.reload(sheetData);
		profiler.pop();
		profiler.endTick();
	}
}
