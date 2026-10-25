package jak0bw.daggercrafting;

import jak0bw.daggercrafting.entity.DaggerEntityRenderer;
import jak0bw.daggercrafting.particle.ParticleDirector;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entry point: binds renderers for throwable dagger entities.
 *
 * Runs after common init. Each material tier registered in {@link ModEntities} gets the same
 * {@link DaggerEntityRenderer} factory; the renderer distinguishes visuals from the entity's item stack.
 */
public class DaggerCraftingClient implements ClientModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(DaggerCrafting.MOD_ID + "-client");

	/**
	 * Associates every {@link ModEntities#DAGGER_ENTITY_TYPES} entry with {@link DaggerEntityRenderer}.
	 *
	 * The loop mirrors {@link DaggerToolMaterial#DAGGER_TOOL_MATERIALS} keys so a new tier only
	 * needs material + item + entity registration—no extra client switch statement.
	 */
	@Override
	public void onInitializeClient() {
		ParticleDirector.setSpawner(DaggerCraftingClient::spawnScaledParticle);
		LOGGER.info("Registering dagger entity renderers");
		for (String name : DaggerToolMaterial.DAGGER_TOOL_MATERIALS.keySet()) {
			EntityRenderers.register(ModEntities.DAGGER_ENTITY_TYPES.get(name), DaggerEntityRenderer::new);
		}

		LOGGER.info("DaggerCrafting client initialized");
	}

	/**
	 * Client spawn that keeps vanilla particle types (crit, flame, smoke) but can shrink their quads.
	 *
	 * {@link ParticleEngine#createParticle} already inserts the instance; {@link Particle#scale} then
	 * multiplies {@code quadSize} on {@code SingleQuadParticle} before the first render.
	 * {@code level} is unused because the engine is a client singleton, not per-world.
	 */
	private static void spawnScaledParticle(
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
