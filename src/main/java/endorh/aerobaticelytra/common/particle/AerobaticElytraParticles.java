package endorh.aerobaticelytra.common.particle;

import com.google.common.collect.ImmutableList;
import endorh.aerobaticelytra.AerobaticElytra;
import endorh.aerobaticelytra.client.particle.TrailParticle;
import endorh.aerobaticelytra.common.particle.TrailParticleData.*;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegisterEvent.RegisterHelper;

import java.util.List;
import java.util.function.Supplier;

import static endorh.aerobaticelytra.AerobaticElytra.prefix;

@EventBusSubscriber(bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
public class AerobaticElytraParticles {
	
	public static TrailParticleType TRAIL_PARTICLE;
	public static StarTrailParticleType STAR_TRAIL_PARTICLE;
	public static CreeperTrailParticleType CREEPER_TRAIL_PARTICLE;
	public static BurstTrailParticleType BURST_TRAIL_PARTICLE;
	public static BubbleTrailParticleType BUBBLE_TRAIL_PARTICLE;
	
	public static List<TrailParticleType> TRAIL_PARTICLES;
	
	@SubscribeEvent
	public static void onParticleTypeRegistration(RegisterEvent event) {
		event.register(ForgeRegistries.PARTICLE_TYPES.getRegistryKey(), r -> {
			TRAIL_PARTICLE = reg(r, TrailParticleType::new, "trail_particle");
			STAR_TRAIL_PARTICLE = reg(r, StarTrailParticleType::new, "star_trail_particle");
			CREEPER_TRAIL_PARTICLE = reg(r, CreeperTrailParticleType::new, "creeper_trail_particle");
			BURST_TRAIL_PARTICLE = reg(r, BurstTrailParticleType::new, "burst_trail_particle");
			BUBBLE_TRAIL_PARTICLE = reg(r, BubbleTrailParticleType::new, "bubble_trail_particle");
			
			TRAIL_PARTICLES = ImmutableList.of(
			  TRAIL_PARTICLE, TRAIL_PARTICLE, STAR_TRAIL_PARTICLE,
			  CREEPER_TRAIL_PARTICLE, BURST_TRAIL_PARTICLE, BUBBLE_TRAIL_PARTICLE);
			
			AerobaticElytra.logRegistered("Particles");
		});
	}
	
	private static <T extends ParticleType<?>> T reg(
	  RegisterHelper<ParticleType<?>> registry, Supplier<T> constructor, String name
	) {
		T particleType = constructor.get();
		registry.register(prefix(name), particleType);
		return particleType;
	}
	
	
	@EventBusSubscriber(value=Dist.CLIENT, bus=Bus.MOD, modid=AerobaticElytra.MOD_ID)
	@OnlyIn(Dist.CLIENT)
	public static class ClientRegistrar {
		@SubscribeEvent
		public static void onParticleFactoryRegistration(RegisterParticleProvidersEvent e) {
			e.registerSpriteSet(TRAIL_PARTICLE, TrailParticle.Factory::new);
			e.registerSpriteSet(STAR_TRAIL_PARTICLE, TrailParticle.Factory::new);
			e.registerSpriteSet(CREEPER_TRAIL_PARTICLE, TrailParticle.Factory::new);
			e.registerSpriteSet(BURST_TRAIL_PARTICLE, TrailParticle.Factory::new);
			e.registerSpriteSet(BUBBLE_TRAIL_PARTICLE, TrailParticle.Factory::new);
			
			AerobaticElytra.logRegistered("Particle Factories");
		}
	}
}
