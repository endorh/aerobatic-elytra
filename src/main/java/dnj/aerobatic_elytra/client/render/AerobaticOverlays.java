package dnj.aerobatic_elytra.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dnj.aerobatic_elytra.AerobaticElytra;
import dnj.aerobatic_elytra.client.config.ClientConfig;
import dnj.aerobatic_elytra.common.AerobaticElytraLogic;
import dnj.aerobatic_elytra.common.capability.AerobaticDataCapability;
import dnj.aerobatic_elytra.common.capability.IAerobaticData;
import dnj.aerobatic_elytra.common.flight.AerobaticFlight;
import dnj.aerobatic_elytra.common.flight.mode.IFlightMode;
import dnj.aerobatic_elytra.common.config.Config;
import dnj.aerobatic_elytra.common.config.Const;
import net.minecraft.client.GameSettings;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.opengl.GL11;

import static dnj.aerobatic_elytra.client.ModResources.FLIGHT_GUI_ICONS_LOCATION;
import static dnj.aerobatic_elytra.common.flight.AerobaticFlight.isAerobaticFlying;
import static java.lang.Math.*;
import static java.lang.System.currentTimeMillis;
import static net.minecraft.client.gui.AbstractGui.blit;
import static net.minecraft.util.math.MathHelper.clamp;
import static net.minecraft.util.math.MathHelper.lerp;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class AerobaticOverlays {
	private static double popupEnd = 0D;
	private static double remainingPopupTime = 0D;
	private static IFlightMode mode = null;
	private static boolean awaitingDebugCrosshair = false;
	
	public static void showModePopupIfRelevant(PlayerEntity player, IFlightMode mode) {
		if (AerobaticElytraLogic.hasAerobaticElytra(player))
			showModePopup(mode);
	}
	
	public static void showModePopup(IFlightMode modeIn) {
		mode = modeIn;
		popupEnd = currentTimeMillis() + ClientConfig.mode_popup_length_millis;
		remainingPopupTime = popupEnd - currentTimeMillis();
	}
	
	@SubscribeEvent
	public static void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event) {
		if (event.getType() == ElementType.ALL && remainingPopupTime > 0) {
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity pl = mc.player;
			assert pl != null;
			float alpha = (float)((popupEnd - currentTimeMillis()) / ClientConfig.mode_popup_length_millis);
			renderPopup(mode, alpha, event.getMatrixStack(),
			            mc.getTextureManager(), event.getWindow());
			remainingPopupTime = popupEnd - currentTimeMillis();
		} else if (event.getType() == ElementType.CROSSHAIRS && awaitingDebugCrosshair) {
			awaitingDebugCrosshair = false;
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity pl = mc.player;
			assert pl != null;
			onPostDebugCrosshair();
		}
	}
	
	public static void renderPopup(
	  IFlightMode mode, float alpha, MatrixStack mStack,
	  TextureManager tManager, MainWindow win
	) {
		tManager.bindTexture(mode.getPopupIconLocation());
		RenderSystem.enableBlend();
		RenderSystem.color4f(1F, 1F, 1F, alpha);
		int winW = win.getScaledWidth();
		int winH = win.getScaledHeight();
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH, tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		int iW = Const.FLIGHT_MODE_POPUP_WIDTH, iH = Const.FLIGHT_MODE_POPUP_HEIGHT;
		int u = mode.getPopupIconU(), v = mode.getPopupIconV();
		if (u != -1 && v != -1) {
			int x = round((winW - iW) * ClientConfig.mode_popup_x_fraction);
			int y = round((winH - iH) * ClientConfig.mode_popup_y_fraction);
			blit(mStack, x, y, u, v, iW, iH, tW, tH);
		}
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		RenderSystem.disableBlend();
	}
	
	@SubscribeEvent
	public static void onRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.CROSSHAIRS && ClientConfig.flight_crosshair) {
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity pl = mc.player;
			assert pl != null;
			if (isAerobaticFlying(pl)) {
				GameSettings st = mc.gameSettings;
				assert mc.playerController != null;
				// Mimic logic from IngameGui#func_238456_d_
				if (st.getPointOfView().func_243192_a()
				    && mc.playerController.getCurrentGameType() != GameType.SPECTATOR
				    && !st.hideGUI) {
					if (st.showDebugInfo && !pl.hasReducedDebug() && !st.reducedDebugInfo) {
						awaitingDebugCrosshair = true;
						onPreDebugCrosshair(pl, event.getWindow());
					} else {
						awaitingDebugCrosshair = false;
						event.setCanceled(renderCrosshair(
						  pl, event.getMatrixStack(),
						  mc.getTextureManager(), event.getWindow()));
					}
				}
			}
		} else if (event.getType() == ElementType.EXPERIENCE
		           && ClientConfig.flight_bar != ClientConfig.FlightBarDisplay.HIDE) {
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity player = mc.player;
			assert player != null;
			if (isAerobaticFlying(player)) {
				event.setCanceled(renderFlightBar(
				  player, event.getMatrixStack(),
				  mc.getTextureManager(), event.getWindow(), event.getPartialTicks()));
			}
		}
	}
	
	public static boolean renderCrosshair(
	  PlayerEntity player, MatrixStack mStack,
	  TextureManager tManager, MainWindow win
	) {
		tManager.bindTexture(FLIGHT_GUI_ICONS_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.blendFuncSeparate(
		  GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
		  GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
		  GlStateManager.SourceFactor.ONE,
		  GlStateManager.DestFactor.ZERO);
		
		int winW = win.getScaledWidth();
		int winH = win.getScaledHeight();
		
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH; int tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		int cS = Const.FLIGHT_GUI_CROSSHAIR_SIZE;
		
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		
		float scaledPitch = data.getTiltPitch() / Config.tilt_range_pitch * Const.CROSSHAIR_PITCH_RANGE_PX;
		float scaledRoll = data.getTiltRoll() / Config.tilt_range_roll * Const.CROSSHAIR_ROLL_RANGE_DEG;
		// Underwater yaw tilt can exceed the range
		float scaledYaw = -clamp(data.getTiltYaw(), -Config.tilt_range_yaw, Config.tilt_range_yaw)
		                  / Config.tilt_range_yaw * Const.CROSSHAIR_YAW_RANGE_PX;
		
		GL11.glPushMatrix(); {
			// Base
			blit(mStack, (winW - cS) / 2, (winH - cS) / 2,
			                 0, 0, cS, cS, tW, tH);
			// Pitch
			GL11.glPushMatrix(); {
				GL11.glTranslatef(0F, scaledPitch, 0F);
				blit(mStack, (winW - cS) / 2, (winH - cS) / 2,
				                 cS, 0, cS, cS, tW, tH);
			} GL11.glPopMatrix();
			
			// Yaw
			GL11.glPushMatrix(); {
				GL11.glTranslatef(scaledYaw, 0F, 0F);
				blit(mStack, (winW - cS) / 2, (winH - cS) / 2,
				                 0, cS, cS, cS, tW, tH);
			} GL11.glPopMatrix();
			
			// Roll
			GL11.glPushMatrix(); {
				GL11.glTranslatef(winW / 2F, winH / 2F, 0F);
				GL11.glRotatef(scaledRoll, 0, 0, 1);
				GL11.glTranslatef(-(winW / 2F), -(winH / 2F), 0F);
				// Rotated crosshair
				blit(mStack, (winW - cS) / 2, (winH - cS) / 2,
				                 cS, cS, cS, cS, tW, tH);
			} GL11.glPopMatrix();
		}
		GL11.glPopMatrix();
		// No attack indicator when flying
		return true;
	}
	
	private static float lastBoost = 0F;
	private static float lastProp = 0F;
	private static float prevBoost = 0F;
	private static float prevProp = 0F;
	private static float lastPartialTicks = 0F;
	
	public static boolean renderFlightBar(
	  PlayerEntity player, MatrixStack mStack,
	  TextureManager tManager, MainWindow win, float partialTicks
	) {
		boolean replace = ClientConfig.flight_bar == ClientConfig.FlightBarDisplay.REPLACE_XP;
		
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
		
		tManager.bindTexture(FLIGHT_GUI_ICONS_LOCATION);
		
		int winW = win.getScaledWidth();
		int winH = win.getScaledHeight();
		
		int cap = player.xpBarCap();
		
		int x = winW / 2 - 91; // from ForgeIngameGui#renderIngameGui
		int y = winH - 32 + 3; // from IngameGui#func_238454_b_
		float barLength = 183F; // from IngameGui#func_238454_b_
		int barHeight = 5;
		if (!replace) { // OVER_XP
			y -= 3;
			barHeight = 3;
		}
		
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH; int tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		float pr = data.getPropulsionStrength();
		int prop = (int)(
		  (pr >= 0F? pr / Config.propulsion_positive_range
		           : pr / Config.propulsion_negative_range
		  ) * barLength);
		int boost = (int)(data.getBoostHeat() * barLength);
		
		if (partialTicks < lastPartialTicks) {
			lastBoost = prevBoost;
			lastProp = prevProp;
			prevBoost = boost;
			prevProp = prop;
		}
		lastPartialTicks = partialTicks;
		
		prop = round(lerp(partialTicks, lastProp, prop));
		boost = round(lerp(partialTicks, lastBoost, boost));
		
		if (cap > 0) {
			blit(mStack, x, y, 0, 50, (int)barLength - 1, barHeight, tW, tH);
			if (prop > 0) {
				blit(mStack, x, y, 0, 55, prop, barHeight, tW, tH);
			} else if (prop < 0) {
				blit(mStack, x, y, 0, 60, -prop, barHeight, tW, tH);
			}
			if (boost > 0) {
				blit(mStack, x, y, 0, 65, boost, barHeight, tW, tH);
			}
		}
		
		RenderSystem.enableBlend();
		RenderSystem.color4f(1F, 1F, 1F, 1F);
		return replace;
	}
	
	/**
	 * Pushes the matrix stack to rotate the crosshair<br>
	 * {@link AerobaticOverlays#onPostDebugCrosshair()} must be called after this on the same frame.
	 */
	private static void onPreDebugCrosshair(PlayerEntity player, MainWindow win) {
		int winW = win.getScaledWidth();
		int winH = win.getScaledHeight();
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		
		GL11.glPushMatrix();
			GL11.glTranslatef(winW / 2F, winH / 2F, 0F);
			GL11.glRotatef(-data.getRotationRoll(), 0, 0, 1);
			GL11.glTranslatef(-(winW / 2F), -(winH / 2F), 0F);
		// Warning! Ensure onPostDebugCrosshair gets called,
		//          or the matrix stack will break
	}
	
	private static void onPostDebugCrosshair() {
		GL11.glPopMatrix();
	}
}
