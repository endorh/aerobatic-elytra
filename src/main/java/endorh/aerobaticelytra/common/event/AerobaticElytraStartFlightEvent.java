package endorh.aerobaticelytra.common.event;

import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Posted when aerobatic flight starts<br>
 * Does not fire for remote client player entities
 * @see Remote
 */
public class AerobaticElytraStartFlightEvent extends Event {
	public final PlayerEntity player;
	public final IElytraSpec elytraSpec;
	public final IAerobaticData aerobaticData;
	
	public AerobaticElytraStartFlightEvent(
	  PlayerEntity player, IElytraSpec elytraSpec, IAerobaticData aerobaticData
	) {
		this.player = player;
		this.elytraSpec = elytraSpec;
		this.aerobaticData = aerobaticData;
	}
	
	/**
	 * Posted when aerobatic flight starts for remote client player entities
	 * @see AerobaticElytraStartFlightEvent
	 */
	public static class Remote extends AerobaticElytraStartFlightEvent {
		public Remote(
		  PlayerEntity player, IElytraSpec spec, IAerobaticData data
		) { super(player, spec, data); }
	}
}
