package endorh.aerobaticelytra.client;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.integration.jei.gui.ShapelessDecoratedDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Centralization of client {@link ResourceLocation}s
 */
public class AerobaticElytraResources {
	public static final ResourceLocation FLIGHT_GUI_ICONS_LOCATION =
	  AerobaticElytra.prefix("textures/gui/flight_icons.png");
	public static final ResourceLocation TEXTURE_AEROBATIC_ELYTRA =
	  AerobaticElytra.prefix("textures/entity/aerobatic_elytra.png");
	public static final ResourceLocation TEXTURE_RECIPES =
	  AerobaticElytra.prefix("textures/gui/recipes.png");
	
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

