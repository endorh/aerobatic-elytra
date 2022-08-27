package endorh.aerobaticelytra.client.sound;

import endorh.aerobaticelytra.AerobaticElytra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class ModSounds {
	public static SoundEvent
	  AEROBATIC_ELYTRA_FLIGHT, AEROBATIC_ELYTRA_BRAKE,
	  AEROBATIC_ELYTRA_ROTATE, AEROBATIC_ELYTRA_WHISTLE,
	  AEROBATIC_ELYTRA_BOOST, AEROBATIC_ELYTRA_SLOWDOWN;
	
	@SubscribeEvent
	public static void onRegisterSounds(RegistryEvent.Register<SoundEvent> event) {
		final IForgeRegistry<SoundEvent> r = event.getRegistry();
		AEROBATIC_ELYTRA_FLIGHT = reg(r, prefix("aerobatic_elytra.flight"));
		AEROBATIC_ELYTRA_BRAKE = reg(r, prefix("aerobatic_elytra.brake"));
		AEROBATIC_ELYTRA_ROTATE = reg(r, prefix("aerobatic_elytra.rotate"));
		AEROBATIC_ELYTRA_WHISTLE = reg(r, prefix("aerobatic_elytra.whistle"));
		AEROBATIC_ELYTRA_BOOST = reg(r, prefix("aerobatic_elytra.boost"));
		AEROBATIC_ELYTRA_SLOWDOWN = reg(r, prefix("aerobatic_elytra.slowdown"));
		AerobaticElytra.logRegistered("Sounds");
	}
	
	public static SoundEvent reg(IForgeRegistry<SoundEvent> registry, ResourceLocation name) {
		SoundEvent event = new SoundEvent(name);
		event.setRegistryName(name);
		registry.register(event);
		return event;
	}
}
