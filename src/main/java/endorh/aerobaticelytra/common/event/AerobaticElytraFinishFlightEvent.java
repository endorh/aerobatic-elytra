package endorh.aerobaticelytra.common.event;

import endorh.aerobaticelytra.common.capability.IAerobaticData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Posted when a player stops aerobatic flying<br>
 * Does not fire for remote client player entities
 * @see Remote
 */
public class AerobaticElytraFinishFlightEvent extends Event {
	public final PlayerEntity player;
	public final IAerobaticData aerobaticData;
	
	public AerobaticElytraFinishFlightEvent(
	  PlayerEntity player, IAerobaticData aerobaticData
	) {
		this.player = player;
		this.aerobaticData = aerobaticData;
	}
	
	/**
	 * Posted when a remote client player entity stops aerobatic flying
	 * @see AerobaticElytraFinishFlightEvent
	 */
	public static class Remote extends AerobaticElytraFinishFlightEvent {
		public Remote(PlayerEntity player, IAerobaticData aerobaticData) {
			super(player, aerobaticData);
		}
	}
}
