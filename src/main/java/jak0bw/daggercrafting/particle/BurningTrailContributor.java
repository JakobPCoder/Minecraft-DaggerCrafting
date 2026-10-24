package jak0bw.daggercrafting.particle;

import java.util.List;

import jak0bw.daggercrafting.entity.DaggerEntity;

/**
 * Replaces the default trail while the projectile is on fire.
 *
 * Uses {@link DaggerEntity#isOnFire()} rather than reading Flame/Fire Aspect. Vanilla Flame already
 * ignites the dagger via {@code projectile_spawned}; Fire Aspect does not, so melee fire stays off
 * the trail. Any other ignite source gets the blazing trail for free.
 */
public final class BurningTrailContributor implements ParticleContributor {

	@Override
	public void contribute(DaggerEntity dagger, List<ParticleContribution> out) {
		if (dagger.isOnFire()) {
			out.add(ParticleContribution.of(ParticlePresets.BLAZING_TRAIL));
		}
	}
}
