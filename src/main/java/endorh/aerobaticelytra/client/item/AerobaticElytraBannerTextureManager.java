package endorh.aerobaticelytra.client.item;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.util.common.ObfuscationReflectionUtil;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.AtlasTexture.SheetData;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public class AerobaticElytraBannerTextureManager extends ReloadListener<SheetData> {
	
	public static final ResourceLocation LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS = AerobaticElytra.prefix("aerobatic_elytra_banner");
	protected final AtlasTexture atlas = new AtlasTexture(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS);
	
	protected static final Set<RenderMaterial> ModelBakery$LOCATIONS_BUILTIN_TEXTURES;
	
	static {
		//noinspection unchecked
		ModelBakery$LOCATIONS_BUILTIN_TEXTURES =
		  (Set<RenderMaterial>) ObfuscationReflectionUtil.getStaticFieldValue(
		    ModelBakery.class, "UNREFERENCED_TEXTURES"
		  ).orElseThrow(() -> new IllegalStateException(
		    "Could not access ModelBakery$LOCATIONS_BUILTIN_TEXTURES"));
	}
	
	public AerobaticElytraBannerTextureManager(IReloadableResourceManager resourceManager) {
		resourceManager.registerReloadListener(this);
		// Add render materials (not thread-safe)
		for(BannerPattern pattern : BannerPattern.values())
			ModelBakery$LOCATIONS_BUILTIN_TEXTURES.add(
			  new RenderMaterial(LOCATION_AEROBATIC_ELYTRA_BANNER_ATLAS, getTextureLocation(pattern)));
		// For some reason, the
	}
	
	public ResourceLocation getTextureLocation(BannerPattern pattern) {
		return new ResourceLocation(AerobaticElytra.MOD_ID, "entity/aerobatic_elytra/" + pattern.getFilename());
	}
	
	@Override protected @NotNull SheetData prepare(@NotNull IResourceManager resourceManager, IProfiler profiler) {
		profiler.startTick();
		profiler.push("stitching");
		final SheetData sheetData = atlas.prepareToStitch(
		  resourceManager, Arrays.stream(BannerPattern.values()).map(this::getTextureLocation),
		  profiler, 2);
		profiler.pop();
		profiler.endTick();
		return sheetData;
	}
	
	@Override protected void apply(
	  @NotNull SheetData sheetData, @NotNull IResourceManager resourceManager,
	  @NotNull IProfiler profiler
	) {
		profiler.startTick();
		profiler.push("upload");
		atlas.reload(sheetData);
		profiler.pop();
		profiler.endTick();
	}
}
