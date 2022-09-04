package endorh.aerobaticelytra.common.flight;

import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.common.flight.mode.IFlightMode;
import endorh.aerobaticelytra.common.registry.ModRegistries;
import endorh.flightcore.events.PlayerTravelEvent;
import endorh.flightcore.events.PlayerTravelEvent.RemotePlayerTravelEvent;
import endorh.util.common.ObfuscationReflectionUtil;
import endorh.util.common.ObfuscationReflectionUtil.SoftField;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
	public static final SoftField<ServerGamePacketListenerImpl, Integer> ServerGamePacketListener$aboveGroundTickCount =
	  ObfuscationReflectionUtil.getSoftField(
	    ServerGamePacketListenerImpl.class, "aboveGroundTickCount", "floatingTickCount",
	    oneTimeLogger(LOGGER::error),
	    "Some flight modes may kick players for flying",
	    "A flight mode tried to prevent a player from being kicked for flying, but reflection failed.");
	
	/**
	 * Event filter for the player travel tick<br>
	 * @see Player#travel
	 * @see LivingEntity#travel
	 */
	@SubscribeEvent
	public static void onPlayerEntityTravelEvent(PlayerTravelEvent event) {
		Player player = event.player;
		getFlightData(player).ifPresent(fd -> {
			final IFlightMode mode = fd.getFlightMode();
			boolean cancel = mode.getFlightHandler().test(player, event.travelVector);
			for (IFlightMode m : ModRegistries.FLIGHT_MODE_REGISTRY) {
				if (mode != m) {
					final BiConsumer<Player, Vec3> handler = m.getNonFlightHandler();
					if (handler != null)
						handler.accept(player, event.travelVector);
				}
			}
			event.setCanceled(cancel);
		});
	}
	
	@SubscribeEvent
	public static void onRemotePlayerEntityTravelEvent(RemotePlayerTravelEvent event) {
		Player player = event.player;
		getFlightData(player).ifPresent(fd -> {
			final IFlightMode mode = fd.getFlightMode();
			final Consumer<Player> flightHandler = mode.getRemoteFlightHandler();
			if (flightHandler != null)
				flightHandler.accept(player);
			for (IFlightMode m : ModRegistries.FLIGHT_MODE_REGISTRY) {
				if (mode != m) {
					final Consumer<Player> handler = m.getRemoteNonFlightHandler();
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
	public static double travelGravity(Player player) {
		double grav = 0.08D;
		AttributeInstance gravity = player.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
		boolean flag = player.getDeltaMovement().y <= 0.0D;
		if (SLOW_FALLING != null) {
			assert gravity != null;
			// Directly extracted from LivingEntity#travel
			if (flag && player.hasEffect(MobEffects.SLOW_FALLING)) {
				if (!gravity.hasModifier(SLOW_FALLING))
					gravity.addTransientModifier(SLOW_FALLING);
				player.fallDistance = 0.0F;
			} else if (gravity.hasModifier(SLOW_FALLING)) {
				gravity.removeModifier(SLOW_FALLING);
			}
			grav = gravity.getValue();
		} else if (flag && player.hasEffect(MobEffects.SLOW_FALLING)) {
			// Reflection failed, defaulting to direct computation ignoring AttributeModifier
			grav = 0.01F;
			player.fallDistance = 0.0F;
		}
		return grav;
	}
	
	/**
	 * Resets the player's tick count through reflection upon its
	 * {@link ServerGamePacketListener}.<br>
	 * Useful for flight modes which keep the player from falling
	 * without using elytra flight or creative flight.
	 * @param player Server player instance
	 * @return False if there was a reflection exception.
	 */
	@SuppressWarnings("unused")
	public static boolean resetFloatingTickCount(ServerPlayer player) {
		return ServerGamePacketListener$aboveGroundTickCount.set(player.connection, 0);
	}
}
