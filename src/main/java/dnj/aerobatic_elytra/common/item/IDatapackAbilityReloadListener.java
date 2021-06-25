package dnj.aerobatic_elytra.common.item;

/**
 * Listener interface for AerobaticElytraDatapackAbilityReload events<br>
 * All instances must call {@link IDatapackAbilityReloadListener#registerAerobaticElytraDatapackAbilityReloadListener()}
 * before receiving any callback<br>
 */
public interface IDatapackAbilityReloadListener {
	default void registerAerobaticElytraDatapackAbilityReloadListener() {
		AbilityReloadManager.registerAbilityReloadListener(this);
	}
	default void onAerobaticElytraDatapackAbilityReload() {}
}
