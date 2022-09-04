package endorh.aerobaticelytra.common.event;

import endorh.aerobaticelytra.common.capability.IAerobaticData;
import endorh.aerobaticelytra.common.capability.IElytraSpec;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * @see Pre
 * @see Post
 */
public abstract class AerobaticElytraTickEvent extends Event {
	public final PlayerEntity player;
	public final IElytraSpec elytraSpec;
	public final IAerobaticData aerobaticData;
	
	protected AerobaticElytraTickEvent(
	  PlayerEntity player, IElytraSpec elytraSpec, IAerobaticData aerobaticData
	) {
		this.player = player;
		this.elytraSpec = elytraSpec;
		this.aerobaticData = aerobaticData;
	}
	
	/**
	 * Posted before every aerobatic flight tick on both sides<br>
	 * Can be cancelled, preventing the mod's logic. If cancelled, the
	 * {@link Pre#preventDefault} can be set to {@code true} to also
	 * prevent the vanilla movement logic<br>
	 *
	 * If you're replacing the mod's logic, you may be responsible
	 * of updating certain data, such as calling
	 * {@link IAerobaticData#updateFlying} by yourself<br>
	 *
	 * This event does not fire for remote player entities
	 *
	 * @see Post
	 * @see Remote.Post
	 */
	@Cancelable @HasResult
	public static class Pre extends AerobaticElytraTickEvent {
		private boolean preventDefault = false;
		
		public Pre(
		  PlayerEntity player, IElytraSpec elytraSpec, IAerobaticData aerobaticData
		) { super(player, elytraSpec, aerobaticData); }
		
		/**
		 * If true and the event is cancelled, will prevent the default movement logic<br>
		 * Otherwise, the vanilla movement logic will run
		 */
		public boolean isPreventDefault() {
			return preventDefault;
		}
		
		/**
		 * If true and the event is cancelled, will prevent the default movement logic<br>
		 * Otherwise, the vanilla movement logic will run
		 */
		public void setPreventDefault(boolean preventDefault) {
			this.preventDefault = preventDefault;
		}
	}
	
	/**
	 * Posted after every aerobatic flight tick on both sides<br>
	 * This event does not fire for remote player entities
	 *
	 * @see Pre
	 * @see Remote.Pre
	 */
	public static class Post extends AerobaticElytraTickEvent {
		public Post(
		  PlayerEntity player, IElytraSpec elytraSpec, IAerobaticData aerobaticData
		) { super(player, elytraSpec, aerobaticData); }
	}
	
	/**
	 * Posted for remote player entities.
	 * @see Remote.Pre
	 * @see Remote.Post
	 */
	public static abstract class Remote extends AerobaticElytraTickEvent {
		protected Remote(
		  PlayerEntity player, IElytraSpec elytraSpec, IAerobaticData aerobaticData
		) { super(player, elytraSpec, aerobaticData); }
		
		/**
		 * Posted before every aerobatic flight tick on clients for remote player entities<br>
		 * Can be cancelled, preventing the mod's logic.
		 * Unlike the non-remote event, there's no vanilla logic to prevent on remote players,
		 * so there is no {@code preventDefault} property.
		 * @see AerobaticElytraTickEvent.Pre
		 */
		@Cancelable
		public static class Pre extends Remote {
			public Pre(
			  PlayerEntity player, IElytraSpec elytraSpec, IAerobaticData aerobaticData
			) { super(player, elytraSpec, aerobaticData); }
		}
		
		/**
		 * Posted after every aerobatic flight tick for remote player entities.
		 *
		 * @see AerobaticElytraTickEvent.Post
		 */
		public static class Post extends Remote {
			public Post(
			  PlayerEntity player, IElytraSpec elytraSpec, IAerobaticData aerobaticData
			) { super(player, elytraSpec, aerobaticData); }
		}
	}
}
