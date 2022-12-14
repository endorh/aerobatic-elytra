package endorh.aerobaticelytra.client.input;

import com.mojang.blaze3d.platform.InputConstants.Type;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.render.AerobaticOverlays;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IFlightData;
import endorh.aerobaticelytra.common.flight.mode.FlightModeTags;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.network.AerobaticPackets.DFlightModePacket;
import endorh.aerobaticelytra.network.AerobaticPackets.DJumpingPacket;
import endorh.aerobaticelytra.network.AerobaticPackets.DSprintingPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.lwjgl.glfw.GLFW;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static net.minecraftforge.client.settings.KeyConflictContext.IN_GAME;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class KeyHandler {
	public static KeyMapping FLIGHT_MODE;
	public static KeyMapping LOOK_AROUND;
	public static final String AEROBATIC_ELYTRA_CATEGORY = "key.aerobaticelytra.category";
	
	private static long lastLookAroundPress = 0L;
	
	@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID, bus = Bus.MOD)
	public static class Registrar {
		@SubscribeEvent public static void onRegisterKeyMappings(RegisterKeyMappingsEvent e) {
			FLIGHT_MODE = reg(e, "key.aerobaticelytra.flight_mode", IN_GAME, GLFW.GLFW_KEY_C, AEROBATIC_ELYTRA_CATEGORY);
			LOOK_AROUND = reg(e, "key.aerobaticelytra.look_around", IN_GAME, GLFW.GLFW_KEY_LEFT_ALT, AEROBATIC_ELYTRA_CATEGORY);
			AerobaticElytra.logRegistered("Key Mappings");
		}
	}
	
	@SuppressWarnings("SameParameterValue")
	private static KeyMapping reg(
	  RegisterKeyMappingsEvent e, String translation, IKeyConflictContext context, int keyCode, String category
	) {
		final KeyMapping binding = new KeyMapping(translation, context, Type.KEYSYM, keyCode, category);
		e.register(binding);
		return binding;
	}
	
	@SubscribeEvent
	public static void onKey(InputEvent.Key event) {
		Player player = Minecraft.getInstance().player;
		if (player == null) return;
		IFlightData fd = getFlightDataOrDefault(player);
		IAerobaticData data = getAerobaticDataOrDefault(player);
		
		boolean lookDown = LOOK_AROUND.isDown();
		if (FLIGHT_MODE.consumeClick()) {
			fd.nextFlightMode();
			IFlightMode mode = fd.getFlightMode();
			new DFlightModePacket(mode).send();
			AerobaticOverlays.showModeToastIfRelevant(player, mode);
		} else if (lookDown && !data.isLookingAround()) {
			long time = System.currentTimeMillis();
			if (time - lastLookAroundPress < 200L) {
				lastLookAroundPress = 0L;
				boolean persistent = data.isLookAroundPersistent();
				data.setLookAroundPersistent(!persistent);
				if (persistent) lookDown = false;
			} else lastLookAroundPress = time;
		}
		data.setLookingAround(lookDown);
	}
	
	@SubscribeEvent
	public static void onMovementInputUpdateEvent(MovementInputUpdateEvent event) {
		final Player player = event.getEntity();
		final IAerobaticData data = getAerobaticDataOrDefault(player);
		final Input movementInput = event.getInput();
		final IFlightMode mode = getFlightDataOrDefault(player).getFlightMode();
		
		if (mode.is(FlightModeTags.AEROBATIC) && player.isFallFlying()) {
			if (data.updateJumping(movementInput.jumping))
				new DJumpingPacket(data).send();
		}
		boolean sprinting =
		  mode.is(FlightModeTags.AEROBATIC) && player.isFallFlying()
		  && Minecraft.getInstance().options.keySprint.isDown();
		if (sprinting)
			player.setSprinting(false);
		if (data.updateSprinting(sprinting)) {
			new DSprintingPacket(data).send();
		}
	}
}
