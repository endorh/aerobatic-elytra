package dnj.aerobatic_elytra.common.capability;

public interface ILocalPlayerCapability<T extends ILocalPlayerCapability<T>> {
	/**
	 * Used when copying the player entity or syncing with the server
	 * @param cap Capability to copy
	 */
	void copy(T cap);
	
	/**
	 * Used when respawning the player
	 */
	void reset();
}
