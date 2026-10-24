package jak0bw.daggercrafting.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;

/**
 * Client spawn that keeps vanilla particle types (crit, flame, smoke) but can shrink their quads.
 *
 * {@link ParticleEngine#createParticle} already inserts the instance; {@link Particle#scale} then
 * multiplies {@code quadSize} on {@code SingleQuadParticle} before the first render.
 */
@Environment(EnvType.CLIENT)
public final class ClientParticleSpawner implements ParticleSpawner {

	@Override
	public void spawn(
		Level level,
		ParticleOptions particle,
		double x,
		double y,
		double z,
		double xd,
		double yd,
		double zd,
		float quadScale
	) {
		ParticleEngine engine = Minecraft.getInstance().particleEngine;
		Particle spawned = engine.createParticle(particle, x, y, z, xd, yd, zd);
		if (spawned != null && quadScale != 1.0f) {
			spawned.scale(quadScale);
		}
	}
}
