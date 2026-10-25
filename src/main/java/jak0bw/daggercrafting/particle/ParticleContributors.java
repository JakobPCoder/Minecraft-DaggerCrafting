package jak0bw.daggercrafting.particle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jak0bw.daggercrafting.entity.DaggerEntity;

/**
 * Default presets plus optional extra factors, then replace-vs-stack per group.
 *
 * Catalog defaults always run. Extra {@link Contributor}s register at mod init (for example the
 * blazing trail while the projectile is on fire). Resolve is a small list walk; group order follows
 * first appearance.
 */
public final class ParticleContributors {

	private static final List<Contributor> CONTRIBUTORS = new ArrayList<>();

	private ParticleContributors() {
	}

	/**
	 * Extra factor that may append presets for this tick (fire, enchantment, material, …).
	 */
	@FunctionalInterface
	public interface Contributor {

		/**
		 * Append zero or more presets. Skip rather than throwing if the dagger does not match.
		 *
		 * @param dagger projectile being ticked
		 * @param out shared list already containing the catalog defaults
		 */
		void contribute(DaggerEntity dagger, List<ParticlePresets.Preset> out);
	}

	/**
	 * Appends a factor that may activate extra presets. Call from common init.
	 *
	 * @param contributor extra source of presets
	 */
	public static void register(Contributor contributor) {
		CONTRIBUTORS.add(contributor);
	}

	/**
	 * Collects defaults plus extra contributions and collapses them to the presets that should emit.
	 *
	 * Per group: the highest-priority {@code replaceGroup} preset wins; non-replace presets in that
	 * group are appended after it.
	 *
	 * @param dagger projectile whose state contributors read
	 * @return presets to emit this tick, in group-then-stack order
	 */
	public static List<ParticlePresets.Preset> resolve(DaggerEntity dagger) {
		List<ParticlePresets.Preset> gathered = new ArrayList<>();
		gathered.add(ParticlePresets.DEFAULT_TRAIL);
		gathered.add(ParticlePresets.DEFAULT_BLOCK_IMPACT);
		gathered.add(ParticlePresets.STUCK_GROUND);
		for (Contributor contributor : CONTRIBUTORS) {
			contributor.contribute(dagger, gathered);
		}

		Map<String, List<ParticlePresets.Preset>> byGroup = new LinkedHashMap<>();
		for (ParticlePresets.Preset preset : gathered) {
			byGroup.computeIfAbsent(preset.group(), key -> new ArrayList<>()).add(preset);
		}

		List<ParticlePresets.Preset> resolved = new ArrayList<>();
		for (List<ParticlePresets.Preset> group : byGroup.values()) {
			ParticlePresets.Preset replaceWinner = null;
			List<ParticlePresets.Preset> stacked = new ArrayList<>();
			for (ParticlePresets.Preset preset : group) {
				if (!preset.replaceGroup()) {
					stacked.add(preset);
				} else if (replaceWinner == null || preset.priority() > replaceWinner.priority()) {
					replaceWinner = preset;
				}
			}
			if (replaceWinner != null) {
				resolved.add(replaceWinner);
			}
			resolved.addAll(stacked);
		}
		return resolved;
	}
}
