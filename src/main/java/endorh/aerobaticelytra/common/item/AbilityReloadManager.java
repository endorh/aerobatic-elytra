package endorh.aerobaticelytra.common.item;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class AbilityReloadManager {
	// This froze twice, thus the synchronizedMap
	private static final Set<IDatapackAbilityReloadListener> LISTENERS = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));
	public static void registerAbilityReloadListener(IDatapackAbilityReloadListener listener) {
		LISTENERS.add(listener);
	}
	
	public static void onAbilityReload() {
		LISTENERS.forEach(IDatapackAbilityReloadListener::onAerobaticElytraDatapackAbilityReload);
	}
}
