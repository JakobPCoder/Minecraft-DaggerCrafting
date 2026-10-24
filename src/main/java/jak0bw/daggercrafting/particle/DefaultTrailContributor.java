package jak0bw.daggercrafting.particle;

import java.util.List;

import jak0bw.daggercrafting.entity.DaggerEntity;

/**
 * Always offers the default spark trail. Blazing (or a future overlay) wins via group replace/stack,
 * not by this class knowing about fire or enchantments.
 */
public final class DefaultTrailContributor implements ParticleContributor {

	@Override
	public void contribute(DaggerEntity dagger, List<ParticleContribution> out) {
		out.add(ParticleContribution.of(ParticlePresets.DEFAULT_TRAIL));
	}
}
