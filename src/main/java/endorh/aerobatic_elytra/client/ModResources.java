package endorh.aerobatic_elytra.client;

import endorh.aerobatic_elytra.AerobaticElytra;
import endorh.aerobatic_elytra.common.item.ModItems;
import endorh.aerobatic_elytra.integration.jei.gui.ShapelessDecoratedDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
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

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

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
	public static final ResourceLocation TEXTURE_RECIPES =
	  prefix("textures/gui/recipes.png");
	
	public static IDrawable[] upgradeRecipeBg(IGuiHelper guiHelper) {
		return new IDrawable[]{
		  guiHelper.createDrawable(TEXTURE_RECIPES, 0, 0, 138, 18),
		  guiHelper.createDrawable(TEXTURE_RECIPES, 0, 18, 138, 18)};
	}
	
	public static IDrawable[] regular3x3RecipeBg(IGuiHelper guiHelper) {
		return new IDrawable[]{
		  guiHelper.createDrawable(TEXTURE_RECIPES, 0, 36, 116, 54),
		  guiHelper.createDrawable(TEXTURE_RECIPES, 0, 90, 116, 54)};
	}
	
	public static IDrawable[] byproduct3x3RecipeBg(IGuiHelper guiHelper) {
		return new IDrawable[]{
		  guiHelper.createDrawable(TEXTURE_RECIPES, 138, 0, 116, 117),
		  guiHelper.createDrawable(TEXTURE_RECIPES, 138, 117, 116, 117)};
	}
	
	public static Function<IGuiHelper, IDrawable[]> shapeless(Function<IGuiHelper, IDrawable[]> backgroundProvider) {
		return guiHelper -> {
			final IDrawable[] res = backgroundProvider.apply(guiHelper);
			return new IDrawable[]{
			  new ShapelessDecoratedDrawable(res[0], guiHelper, false),
			  new ShapelessDecoratedDrawable(res[1], guiHelper, true)};
		};
	}
}

