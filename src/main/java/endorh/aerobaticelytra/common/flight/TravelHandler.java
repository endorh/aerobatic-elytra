package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.common.registry.ModRegistries;
import endorh.flight_core.events.PlayerEntityTravelEvent;
import endorh.flight_core.events.PlayerEntityTravelEvent.RemotePlayerEntityTravelEvent;
import endorh.util.common.ObfuscationReflectionUtil;
import endorh.util.common.ObfuscationReflectionUtil.SoftField;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static endorh.aerobaticelytra.common.capability.FlightDataCapability.getFlightData;
import static endorh.util.common.LogUtil.oneTimeLogger;

@EventBusSubscriber(modid = AerobaticElytra.MOD_ID)
public class TravelHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	
	/**
	 * {@code private static final AttributeModifier LivingEntity#SLOW_FALLING}
	 * <br>Accessed by reflection
	 */
	public static final AttributeModifier SLOW_FALLING =
	  ObfuscationReflectionUtil.getStaticFieldValueOrLog(
	    LivingEntity.class, "SLOW_FALLING", "SLOW_FALLING",
	    LOGGER::error, "Slow falling effect may not interact properly with aerobatic elytras");
	
	/**
	 * {@code private int ServerPlayNetHandler#floatingTickCount}<br>
	 * Accessed by reflection
	 */
	public static final SoftField<ServerPlayNetHandler, Integer> ServerPlayNetHandler$floatingTickCount =
	  ObfuscationReflectionUtil.getSoftField(
	    ServerPlayNetHandler.class, "field_147365_f", "floatingTickCount",
	    oneTimeLogger(LOGGER::error),
	    "Some flight modes may kick players for flying",
	    "A flight mode tried to prevent a player from being kicked for flying, but reflection failed.");
	
	/**
	 * Event filter for the player travel tick<br>
	 * @see PlayerEntity#travel
	 * @see LivingEntity#travel
	 */
	@SubscribeEvent
	public static void onPlayerEntityTravelEvent(PlayerEntityTravelEvent event) {
		PlayerEntity player = event.player;
		getFlightData(player).ifPresent(fd -> {
			final IFlightMode mode = fd.getFlightMode();
			boolean cancel = mode.getFlightHandler().test(player, event.travelVector);
			for (IFlightMode m : ModRegistries.FLIGHT_MODE_REGISTRY) {
				if (mode != m) {
					final BiConsumer<PlayerEntity, Vector3d> handler = m.getNonFlightHandler();
					if (handler != null)
						handler.accept(player, event.travelVector);
				}
			}
			event.setCanceled(cancel);
		});
	}
	
	@SubscribeEvent
	public static void onRemotePlayerEntityTravelEvent(RemotePlayerEntityTravelEvent event) {
		PlayerEntity player = event.player;
		getFlightData(player).ifPresent(fd -> {
			final IFlightMode mode = fd.getFlightMode();
			final Consumer<PlayerEntity> flightHandler = mode.getRemoteFlightHandler();
			if (flightHandler != null)
				flightHandler.accept(player);
			for (IFlightMode m : ModRegistries.FLIGHT_MODE_REGISTRY) {
				if (mode != m) {
					final Consumer<PlayerEntity> handler = m.getRemoteNonFlightHandler();
					if (handler != null)
						handler.accept(player);
				}
			}
		});
	}
	
	/**
	 * Mimics the logic performed in {@link LivingEntity#travel},
	 * applying the SLOW_FALLING potion effect and returning the
	 * resulting gravity.
	 * @param player Player travelling
	 * @return The default gravity applied to the player on this tick
	 */
	public static double travelGravity(PlayerEntity player) {
		double grav = 0.08D;
		ModifiableAttributeInstance gravity = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
		boolean flag = player.getMotion().y <= 0.0D;
		if (SLOW_FALLING != null) {
			assert gravity != null;
			// Directly extracted from LivingEntity#travel
			if (flag && player.isPotionActive(Effects.SLOW_FALLING)) {
				if (!gravity.hasModifier(SLOW_FALLING))
					gravity.applyNonPersistentModifier(SLOW_FALLING);
				player.fallDistance = 0.0F;
			} else if (gravity.hasModifier(SLOW_FALLING)) {
				gravity.removeModifier(SLOW_FALLING);
			}
			grav = gravity.getValue();
		} else if (flag && player.isPotionActive(Effects.SLOW_FALLING)) {
			// Reflection failed, defaulting to direct computation ignoring AttributeModifier
			grav = 0.01F;
			player.fallDistance = 0.0F;
		}
		return grav;
	}
	
	/**
	 * Resets the player's tick count through reflection upon its
	 * {@link ServerPlayNetHandler}.<br>
	 * Useful for flight modes which keep the player from falling
	 * without using elytra flight or creative flight.
	 * @param player Server player instance
	 * @return False if there was a reflection exception.
	 */
	@SuppressWarnings("unused")
	public static boolean resetFloatingTickCount(ServerPlayerEntity player) {
		return ServerPlayNetHandler$floatingTickCount.set(player.connection, 0);
	}
}
