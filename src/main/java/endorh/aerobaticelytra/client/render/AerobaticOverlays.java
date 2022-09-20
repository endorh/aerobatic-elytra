package endorh.aerobaticelytra.client.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.config.ClientConfig;
import endorh.aerobaticelytra.client.config.ClientConfig.style.visual;
import endorh.aerobaticelytra.common.AerobaticElytraLogic;
import endorh.aerobaticelytra.common.capability.AerobaticDataCapability;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.config.Config;
import endorh.aerobaticelytra.common.config.Config.aerobatic.tilt;
import endorh.aerobaticelytra.common.config.Const;
import endorh.aerobaticelytra.common.flight.AerobaticFlight;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.util.animation.ToggleAnimator;
import endorh.util.math.Vec3f;
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

import static endorh.aerobaticelytra.client.AerobaticElytraResources.FLIGHT_GUI_ICONS_LOCATION;
import static java.lang.Math.*;
import static java.lang.System.currentTimeMillis;
import static net.minecraft.client.gui.AbstractGui.blit;
import static net.minecraft.util.math.MathHelper.sqrt;
import static net.minecraft.util.math.MathHelper.*;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class AerobaticOverlays {
	private static long toastEnd = 0L;
	private static long remainingToastTime = 0L;
	private static IFlightMode mode = null;
	private static final Vec3f XP = Vec3f.XP.get();
	private static final Vec3f YP = Vec3f.YP.get();
	private static final Vec3f ZP = Vec3f.ZP.get();
	private static final ToggleAnimator lookAroundAnimator = ToggleAnimator.quadOut(100L);
	private static final ToggleAnimator rectifyAnimator = ToggleAnimator.quadOut(100L);
	private static boolean awaitingDebugCrosshair = false;
	
	static {
		lookAroundAnimator.setRange(-45F, 0);
		rectifyAnimator.setRange(1.2F, 1);
	}
	
	public static void showModeToastIfRelevant(PlayerEntity player, IFlightMode mode) {
		if (AerobaticElytraLogic.hasAerobaticElytra(player))
			showModeToast(mode);
	}
	
	public static void showModeToast(IFlightMode modeIn) {
		mode = modeIn;
		toastEnd = currentTimeMillis() + visual.mode_toast_length_ms;
		remainingToastTime = visual.mode_toast_length_ms;
	}
	
	@SubscribeEvent
	public static void onRenderGameOverlayPost(RenderGameOverlayEvent.Post event) {
		if (event.getType() == ElementType.ALL && remainingToastTime > 0) {
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity pl = mc.player;
			assert pl != null;
			float alpha = remainingToastTime / (float) visual.mode_toast_length_ms;
			renderToast(mode, alpha, event.getMatrixStack(),
			            mc.getTextureManager(), event.getWindow());
			final long t = currentTimeMillis();
			remainingToastTime = toastEnd - t;
		} else if (event.getType() == ElementType.CROSSHAIRS && awaitingDebugCrosshair) {
			awaitingDebugCrosshair = false;
			PlayerEntity pl = Minecraft.getInstance().player;
			assert pl != null;
			onPostDebugCrosshair();
		}
	}
	
	public static void renderToast(
	  IFlightMode mode, float alpha, MatrixStack mStack,
	  TextureManager tManager, MainWindow win
	) {
		tManager.bindTexture(mode.getToastIconLocation());
		RenderSystem.enableBlend();
		RenderSystem.color4f(1, 1, 1, alpha);
		int winW = win.getScaledWidth();
		int winH = win.getScaledHeight();
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH, tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		int iW = Const.FLIGHT_MODE_TOAST_WIDTH, iH = Const.FLIGHT_MODE_TOAST_HEIGHT;
		int u = mode.getToastIconU(), v = mode.getToastIconV();
		if (u != -1 && v != -1) {
			int x = round((winW - iW) * visual.mode_toast_x_fraction);
			int y = round((winH - iH) * visual.mode_toast_y_fraction);
			blit(mStack, x, y, u, v, iW, iH, tW, tH);
		}
		RenderSystem.color4f(1, 1, 1, 1);
		RenderSystem.disableBlend();
	}
	
	@SubscribeEvent
	public static void onRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.CROSSHAIRS && visual.flight_crosshair) {
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity pl = mc.player;
			assert pl != null;
			if (AerobaticFlight.isAerobaticFlying(pl)) {
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
						  pl, event.getMatrixStack(), mc.getTextureManager(),
						  event.getWindow(), event.getPartialTicks()));
					}
				}
			}
		} else if (event.getType() == ElementType.EXPERIENCE
		           && visual.flight_bar != ClientConfig.FlightBarDisplay.HIDE) {
			Minecraft mc = Minecraft.getInstance();
			PlayerEntity player = mc.player;
			assert player != null;
			if (AerobaticFlight.isAerobaticFlying(player)) {
				event.setCanceled(renderFlightBar(
				  player, event.getMatrixStack(),
				  mc.getTextureManager(), event.getWindow(), event.getPartialTicks()));
			}
		}
	}
	
	public static boolean renderCrosshair(
	  PlayerEntity player, MatrixStack mStack, TextureManager tManager,
	  MainWindow win, float partialTicks
	) {
		tManager.bindTexture(FLIGHT_GUI_ICONS_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.disableCull();
		RenderSystem.blendFuncSeparate(
		  SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR,
		  SourceFactor.ONE, DestFactor.ZERO);
		
		int winW = win.getScaledWidth();
		int winH = win.getScaledHeight();
		
		int tW = Const.FLIGHT_GUI_TEXTURE_WIDTH;
		int tH = Const.FLIGHT_GUI_TEXTURE_HEIGHT;
		int cS = Const.FLIGHT_GUI_CROSSHAIR_SIZE;
		
		IAerobaticData data = AerobaticDataCapability.getAerobaticDataOrDefault(player);
		
		float scaledPitch = data.getTiltPitch() / tilt.range_pitch * Const.CROSSHAIR_PITCH_RANGE_PX;
		float scaledRoll = data.getTiltRoll() / tilt.range_roll * Const.CROSSHAIR_ROLL_RANGE_DEG;
		// Underwater yaw tilt can exceed the range
		float scaledYaw = -clamp(
		  data.getTiltYaw(), -tilt.range_yaw, tilt.range_yaw
		) / tilt.range_yaw * Const.CROSSHAIR_YAW_RANGE_PX;
		
		float lookYaw = lerp(partialTicks, data.getPrevLookAroundYaw(), data.getLookAroundYaw());
		float lookPitch = lerp(partialTicks, data.getPrevLookAroundPitch(), data.getLookAroundPitch());
		boolean isLookingAround = lookYaw != 0 || lookPitch != 0 || data.isLookingAround();
		if ((lookAroundAnimator.getTarget() == 1) != isLookingAround)
			lookAroundAnimator.setTarget(isLookingAround);
		if ((rectifyAnimator.getTarget() == 1) != data.isJumping())
			rectifyAnimator.setTarget(data.isJumping());
		boolean animatingLook = lookAroundAnimator.isInProgress();
		float cX = winW / 2F;
		float cY = winH / 2F;
		int crossX = (winW - cS) / 2;
		int crossY = (winH - cS) / 2;
		mStack.push(); {
			if (animatingLook) {
				mStack.push(); {
					mStack.translate(cX, cY, 0);
					mStack.rotate(ZP.rotationDegrees(lookAroundAnimator.getProgress()));
					mStack.translate(-cX, -cY, 0);
					blit(mStack, crossX, crossY, 2 * cS, 0, cS, cS, tW, tH);
				} mStack.pop();
			}
			if (isLookingAround) {
				float rotDiag = sqrt(lookPitch * lookPitch + lookYaw * lookYaw);
				if (!animatingLook) blit(
				  mStack, crossX, crossY, 2 * cS, data.isLookAroundPersistent()? cS : 0,
				  cS, cS, tW, tH);
				if (rotDiag > 1E-4F) {
					// The rotation offset must be relative to the screen size
					float diag = sqrt(winW * winW + winH * winH);
					float rotationOffset = diag / 8F;
					// The rotation is scaled down for small rotations, to create the
					//   illusion that the rotated crosshair points into the flight direction
					//   relative to the player's POV, rather than from the aim crosshair
					// Ideally, the rotated crosshair would be rendered in the surface of a
					//   sphere centered around the aim crosshair, which would be placed some
					//   distance in front of the player, in the spot determined by a ray
					//   cast from the aim crosshair to the flight direction in the infinity,
					//   but this is good enough for most screen sizes and FOV values
					float rotationStrength = clamp(1 - (float) 1 / rotDiag, 0, 1);
					// Rotate the crosshair back around itself to make it more readable
					//   at angles near 90 degrees
					// The interpolation function used for the counter rotation is
					//   ∛(x / (1 + x⁴)), which produces a nice transition between angles
					//   before and beyond 90 degrees
					float relRot = (rotDiag - 90) / 5F;
					float counterRotationStrength = 0.4F * (float) cbrt(
					  relRot / (1 + (float) pow(relRot, 4)));
					mStack.translate(cX, cY, -rotationOffset);
					mStack.rotate(XP.rotationDegrees(lookPitch * rotationStrength));
					mStack.rotate(YP.rotationDegrees(lookYaw * rotationStrength));
					mStack.translate(0, 0, rotationOffset);
					mStack.rotate(XP.rotationDegrees(lookPitch * counterRotationStrength));
					mStack.rotate(YP.rotationDegrees(lookYaw * counterRotationStrength));
					mStack.translate(-cX, -cY, 0);
				}
			}
			// Base
			blit(mStack, crossX, crossY,
			     0, 0, cS, cS, tW, tH);
			// Pitch
			mStack.push(); {
				mStack.translate(0, scaledPitch, 0);
				blit(mStack, crossX, crossY,
				     cS, 0, cS, cS, tW, tH);
			} mStack.pop();
			
			// Yaw
			mStack.push(); {
				mStack.translate(scaledYaw, 0, 0);
				blit(mStack, crossX, crossY,
				     0, cS, cS, cS, tW, tH);
			} mStack.pop();
			
			// Roll
			mStack.push(); {
				mStack.translate(cX, cY, 0);
				mStack.rotate(ZP.rotationDegrees(scaledRoll));
				mStack.translate(-cX, -cY, 0);
				// Rotated crosshair
				blit(mStack, crossX, crossY,
				     cS, cS, cS, cS, tW, tH);
			} mStack.pop();
			
			// Rectification trigger
			if (rectifyAnimator.isInProgress()) {
				RenderSystem.color4f(1, 1, 1, rectifyAnimator.getUnitProgress());
				mStack.push(); {
					float scale = rectifyAnimator.getProgress();
					mStack.translate(cX, cY, 0);
					mStack.scale(scale, scale, 1);
					mStack.translate(-cX, -cY, 0);
					blit(mStack, crossX, crossY, 3 * cS, 0, cS, cS, tW, tH);
				} mStack.pop();
				RenderSystem.color4f(1, 1, 1, 1);
			} else if (data.isJumping())
				blit(mStack, crossX, crossY, 3 * cS, 0, cS, cS, tW, tH);
		} mStack.pop();
		
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		
		// No attack indicator when flying
		return true;
	}
	
	private static float lastBoost = 0;
	private static float lastProp = 0;
	private static float prevBoost = 0;
	private static float prevProp = 0;
	private static float lastPartialTicks = 0;
	
	public static boolean renderFlightBar(
	  PlayerEntity player, MatrixStack mStack,
	  TextureManager tManager, MainWindow win, float partialTicks
	) {
		boolean replace = visual.flight_bar == ClientConfig.FlightBarDisplay.REPLACE_XP
		                  || player.isCreative();
		
		// RenderSystem.color4f(1.0, 1.0, 1.0, 1.0);
		RenderSystem.enableBlend();
		
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
		  (pr >= 0? pr / Config.aerobatic.propulsion.positive_span
		           : pr / Config.aerobatic.propulsion.negative_span
		  ) * barLength);
		int boost = (int)(data.getBoostHeat() * barLength);
		int brake_heat = (int)((1.0 - Math.pow(1 - data.getBrakeHeat(), 1.4)) * barLength);
		boolean brake_cooldown = data.isBrakeCooling();
		
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
			// Base
			blit(mStack, x, y, 0, 50, (int)barLength - 1, barHeight, tW, tH);
			// Propulsion
			if (prop > 0)
				blit(mStack, x, y, 0, 55, prop, barHeight, tW, tH);
			else if (prop < 0)
				blit(mStack, x, y, 0, 60, -prop, barHeight, tW, tH);
			// Boost
			if (boost > 0)
				blit(mStack, x, y, 0, 65, boost, barHeight, tW, tH);
			// Brake
			if (brake_heat > 0)
				blit(mStack, x, y, 0, brake_cooldown? 75 : 70, brake_heat, barHeight, tW, tH);
			// Overlay
			blit(mStack, x, y, 0, 80, (int)barLength - 1, barHeight, tW, tH);
		}
		
		RenderSystem.disableBlend();
		// RenderSystem.color4f(1, 1, 1, 1);
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
			GL11.glTranslatef(winW / 2F, winH / 2F, 0);
			GL11.glRotatef(-CameraHandler.lastRoll, 0, 0, 1);
			GL11.glTranslatef(-(winW / 2F), -(winH / 2F), 0);
		// Warning! Ensure onPostDebugCrosshair gets called,
		//          or the matrix stack will break
	}
	
	private static void onPostDebugCrosshair() {
		GL11.glPopMatrix();
	}
}
