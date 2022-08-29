package endorh.aerobaticelytra.client.render.overlay;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.common.capability.AerobaticDataCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config.aerobatic.propulsion;
import endorh.aerobaticelytra.common.config.Const;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;

import static endorh.aerobaticelytra.client.ModResources.FLIGHT_GUI_ICONS_LOCATION;
import static java.lang.Math.round;

public class FlightBarOverlay implements IIngameOverlay {
	public static final String NAME = AerobaticElytra.MOD_ID + ":flight_bar";
	
	private static float lastBoost = 0F;
	private static float lastProp = 0F;
	private static float prevBoost = 0F;
	private static float prevProp = 0F;
	private static float lastPartialTicks = 0F;
	
	@Override public void render(
	  ForgeIngameGui gui, PoseStack mStack, float partialTicks, int width, int height
	) {
		LocalPlayer player = Minecraft.getInstance().player;
		assert player != null;
		Window win = Minecraft.getInstance().getWindow();
		
		boolean replace = visual.flight_bar == ClientConfig.FlightBarDisplay.REPLACE_XP || player.isCreative();
		
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.enableBlend();
		
		RenderSystem.setShaderTexture(0, FLIGHT_GUI_ICONS_LOCATION);
		
		int winW = win.getGuiScaledWidth();
		int winH = win.getGuiScaledHeight();
		
		int cap = player.getXpNeededForNextLevel();
		
		int x = winW / 2 - 91; // from ForgeIngameGui#renderIngameGui
		int y = winH - 32 + 3; // from IngameGui#renderExperienceBar
		float barLength = 183F; // from IngameGui#renderExperienceBar
		int barHeight = 5;
		if (!replace) { // OVER_XP
			y -= 3;
			barHeight = 3;
		}
		
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH; int tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		float pr = data.getPropulsionStrength();
		int prop = (int)(
		  (pr >= 0F? pr / propulsion.positive_span : pr / propulsion.negative_span) * barLength);
		int boost = (int)(data.getBoostHeat() * barLength);
		int brake_heat = (int)((1.0 - Math.pow(1F - data.getBrakeHeat(), 1.4)) * barLength);
		boolean brake_cooldown = data.isBrakeCooling();
		
		if (partialTicks < lastPartialTicks) {
			lastBoost = prevBoost;
			lastProp = prevProp;
			prevBoost = boost;
			prevProp = prop;
		}
		lastPartialTicks = partialTicks;
		
		prop = round(Mth.lerp(partialTicks, lastProp, prop));
		boost = round(Mth.lerp(partialTicks, lastBoost, boost));
		
		if (cap > 0) {
			// Base
			GuiComponent.blit(mStack, x, y, 0, 50, (int)barLength - 1, barHeight, tW, tH);
			// Propulsion
			if (prop > 0)
				GuiComponent.blit(mStack, x, y, 0, 55, prop, barHeight, tW, tH);
			else if (prop < 0)
				GuiComponent.blit(mStack, x, y, 0, 60, -prop, barHeight, tW, tH);
			// Boost
			if (boost > 0)
				GuiComponent.blit(mStack, x, y, 0, 65, boost, barHeight, tW, tH);
			// Brake
			if (brake_heat > 0)
				GuiComponent.blit(mStack, x, y, 0, brake_cooldown? 75 : 70, brake_heat, barHeight, tW, tH);
			// Overlay
			GuiComponent.blit(mStack, x, y, 0, 80, (int)barLength - 1, barHeight, tW, tH);
		}
		
		RenderSystem.disableBlend();
	}
}
