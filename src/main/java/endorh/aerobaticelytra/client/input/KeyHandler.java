package endorh.aerobaticelytra.client.input;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.render.AerobaticOverlays;
import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IFlightData;
import endorh.aerobaticelytra.common.flight.mode.FlightModeTags;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.network.AerobaticPackets.DFlightModePacket;
import endorh.aerobaticelytra.network.AerobaticPackets.DJumpingPacket;
import endorh.aerobaticelytra.network.AerobaticPackets.DSprintingPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings.Type;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static endorh.aerobaticelytra.common.capability.AerobaticDataCapability.getAerobaticDataOrDefault;
import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightDataOrDefault;
import static net.minecraftforge.client.settings.KeyConflictContext.IN_GAME;

@EventBusSubscriber(value = Dist.CLIENT, modid = AerobaticElytra.MOD_ID)
public class KeyHandler {
	public static KeyBinding FLIGHT_MODE_KEYBINDING;
	public static final String AEROBATIC_ELYTRA_CATEGORY = "key.aerobaticelytra.category";
	
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void register() {
		FLIGHT_MODE_KEYBINDING = reg("key.aerobaticelytra.flight_mode.desc", IN_GAME, 67, AEROBATIC_ELYTRA_CATEGORY);
	}
	
	@SuppressWarnings("SameParameterValue")
	private static KeyBinding reg(
	  String translation, IKeyConflictContext context, int keyCode, String category
	) {
		final KeyBinding binding = new KeyBinding(translation, context, Type.KEYSYM, keyCode, category);
		ClientRegistry.registerKeyBinding(binding);
		return binding;
	}
	
	@SubscribeEvent
	public static void onKey(KeyInputEvent event) {
		final PlayerEntity player = Minecraft.getInstance().player;
		if (player == null)
			return;
		final IFlightData fd = getFlightDataOrDefault(player);
		
		if (FLIGHT_MODE_KEYBINDING.isPressed()) {
			fd.nextFlightMode();
			IFlightMode mode = fd.getFlightMode();
			new DFlightModePacket(mode).send();
			AerobaticOverlays.showModeToastIfRelevant(player, mode);
		}
	}
	
	@SubscribeEvent
	public static void onInputUpdateEvent(InputUpdateEvent event) {
		final PlayerEntity player = event.getPlayer();
		final IAerobaticData data = getAerobaticDataOrDefault(player);
		final MovementInput movementInput = event.getMovementInput();
		final IFlightMode mode = getFlightDataOrDefault(player).getFlightMode();
		
		if (mode.is(FlightModeTags.AEROBATIC) && player.isElytraFlying()) {
			if (data.updateJumping(movementInput.jump))
				new DJumpingPacket(data).send();
		}
		boolean sprinting =
		  mode.is(FlightModeTags.AEROBATIC) && player.isElytraFlying()
		  && Minecraft.getInstance().gameSettings.keyBindSprint.isKeyDown();
		if (sprinting)
			player.setSprinting(false);
		if (data.updateSprinting(sprinting)) {
			new DSprintingPacket(data).send();
		}
	}
}
