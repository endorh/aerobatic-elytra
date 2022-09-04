package endorh.aerobaticelytra.common.particle;

import com.google.common.collect.ImmutableList;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.particle.TrailParticle;
import endorh.aerobaticelytra.common.particle.TrailParticleData.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(bus = Bus.MOD, modid = AerobaticElytra.MOD_ID)
public class AerobaticParticles {
	
	public static TrailParticleType TRAIL_PARTICLE;
	public static StarTrailParticleType STAR_TRAIL_PARTICLE;
	public static CreeperTrailParticleType CREEPER_TRAIL_PARTICLE;
	public static BurstTrailParticleType BURST_TRAIL_PARTICLE;
	public static BubbleTrailParticleType BUBBLE_TRAIL_PARTICLE;
	
	public static List<TrailParticleType> TRAIL_PARTICLES;
	
	@SubscribeEvent
	public static void onParticleTypeRegistration(RegistryEvent.Register<ParticleType<?>> event) {
		IForgeRegistry<ParticleType<?>> r = event.getRegistry();
		
		TRAIL_PARTICLE = reg(r, TrailParticleType::new, "trail_particle");
		STAR_TRAIL_PARTICLE = reg(r, StarTrailParticleType::new, "star_trail_particle");
		CREEPER_TRAIL_PARTICLE = reg(r, CreeperTrailParticleType::new, "creeper_trail_particle");
		BURST_TRAIL_PARTICLE = reg(r, BurstTrailParticleType::new, "burst_trail_particle");
		BUBBLE_TRAIL_PARTICLE = reg(r, BubbleTrailParticleType::new, "bubble_trail_particle");
		
		TRAIL_PARTICLES = ImmutableList.of(
		  TRAIL_PARTICLE, TRAIL_PARTICLE, STAR_TRAIL_PARTICLE,
		  CREEPER_TRAIL_PARTICLE, BURST_TRAIL_PARTICLE, BUBBLE_TRAIL_PARTICLE);
		
		AerobaticElytra.logRegistered("Particles");
	}
	
	private static <T extends ParticleType<?>> T reg(
	  IForgeRegistry<ParticleType<?>> registry, Supplier<T> constructor, String name
	) {
		T particleType = constructor.get();
		particleType.setRegistryName(AerobaticElytra.prefix(name));
		registry.register(particleType);
		return particleType;
	}
	
	@SubscribeEvent
	public static void onParticleFactoryRegistration(ParticleFactoryRegisterEvent event) {
		ParticleManager p = Minecraft.getInstance().particles;
		
		p.registerFactory(TRAIL_PARTICLE, TrailParticle.Factory::new);
		p.registerFactory(STAR_TRAIL_PARTICLE, TrailParticle.Factory::new);
		p.registerFactory(CREEPER_TRAIL_PARTICLE, TrailParticle.Factory::new);
		p.registerFactory(BURST_TRAIL_PARTICLE, TrailParticle.Factory::new);
		p.registerFactory(BUBBLE_TRAIL_PARTICLE, TrailParticle.Factory::new);
		
		AerobaticElytra.logRegistered("Particle Factories");
	}
}
