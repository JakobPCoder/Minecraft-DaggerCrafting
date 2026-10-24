package jak0bw.daggercrafting.particle;

import java.util.List;

import jak0bw.daggercrafting.entity.DaggerEntity;

/**
 * Always offers the default terrain burst on block hits. Other impact overlays would compete
 * inside {@link ParticlePresets#GROUP_IMPACT_BLOCK}, not via this class.
 */
public final class DefaultBlockImpactContributor implements ParticleContributor {

	@Override
	public void contribute(DaggerEntity dagger, List<ParticleContribution> out) {
		out.add(ParticleContribution.of(ParticlePresets.DEFAULT_BLOCK_IMPACT));
	}
}
