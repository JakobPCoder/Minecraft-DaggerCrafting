package jak0bw.daggercrafting.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;

/**
 * Side-specific spawn: common code uses {@link Level#addParticle} (ignores scale); the client
 * replaces this with engine {@code createParticle} plus {@link net.minecraft.client.particle.Particle#scale}.
 */
@FunctionalInterface
public interface ParticleSpawner {

	/**
	 * @param quadScale multiplier on the vanilla quad after construction; {@code 1} leaves default size
	 */
	void spawn(
		Level level,
		ParticleOptions particle,
		double x,
		double y,
		double z,
		double xd,
		double yd,
		double zd,
		float quadScale
	);
}
