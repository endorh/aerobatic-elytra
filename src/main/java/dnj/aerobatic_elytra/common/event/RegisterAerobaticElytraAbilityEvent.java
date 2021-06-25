package dnj.aerobatic_elytra.common.event;

import dnj.aerobatic_elytra.common.item.IAbility;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;

import java.util.Collection;
import java.util.Map;

public class RegisterAerobaticElytraAbilityEvent extends Event implements IModBusEvent {
	private final Map<ResourceLocation, IAbility> map;
	public RegisterAerobaticElytraAbilityEvent(Map<ResourceLocation, IAbility> map) {
		this.map = map;
	}
	
	public void register(IAbility ability) {
		final ResourceLocation name = ability.getRegistryName();
		if (map.containsKey(name))
			throw new IllegalArgumentException(
			  "Registry entry \"" + name + "\" is already in the registry");
		map.put(name, ability);
	}
	
	public void registerAll(IAbility... abilities) {
		for (IAbility ability : abilities)
			register(ability);
	}
	
	public void registerAll(Collection<IAbility> abilities) {
		for (IAbility ability : abilities)
			register(ability);
	}
}
