package jak0bw.daggercrafting.particle;

import java.util.List;

import jak0bw.daggercrafting.entity.DaggerEntity;

/**
 * External factor that may activate particle presets on a thrown dagger.
 *
 * Implementors must not live in {@link DaggerEntity}: enchantments, fire, materials, and future
 * datapack flags each get their own contributor registered at mod init.
 */
@FunctionalInterface
public interface ParticleContributor {

	/**
	 * Append zero or more contributions for this tick. Skip rather than throwing if the dagger
	 * does not match (e.g. not on fire).
	 *
	 * @param dagger projectile being ticked
	 * @param out mutable list shared across all contributors this tick
	 */
	void contribute(DaggerEntity dagger, List<ParticleContribution> out);
}
