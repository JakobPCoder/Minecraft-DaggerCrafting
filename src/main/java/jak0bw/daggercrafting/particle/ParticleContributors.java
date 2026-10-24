package jak0bw.daggercrafting.particle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.entity.DaggerEntity;

/**
 * Global contributor list and per-tick resolve (replace vs stack per group).
 *
 * Registration is append-only at mod init. Resolve is cheap: two contributors and two presets in v1.
 */
public final class ParticleContributors {

	private static final List<ParticleContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

	private ParticleContributors() {
	}

	/** Appends a factor that may activate presets. Call from {@link DaggerCrafting#onInitialize()}. */
	public static void register(ParticleContributor contributor) {
		CONTRIBUTORS.add(contributor);
	}

	/**
	 * Collects contributions and collapses them to presets.
	 *
	 * Per group: the highest-priority non-stack (replace) contribution wins; all stack contributions
	 * in that group are appended after it. Unknown preset ids are skipped with a warning.
	 *
	 * @param dagger projectile whose state contributors read
	 * @return presets to emit this tick, in group-then-stack order
	 */
	public static List<ParticlePreset> resolve(DaggerEntity dagger) {
		List<ParticleContribution> gathered = new ArrayList<>();
		for (ParticleContributor contributor : CONTRIBUTORS) {
			contributor.contribute(dagger, gathered);
		}

		Map<String, List<ParticleContribution>> byGroup = new LinkedHashMap<>();
		for (ParticleContribution contribution : gathered) {
			byGroup.computeIfAbsent(contribution.group(), key -> new ArrayList<>()).add(contribution);
		}

		List<ParticlePreset> resolved = new ArrayList<>();
		for (List<ParticleContribution> group : byGroup.values()) {
			ParticleContribution replaceWinner = null;
			List<ParticleContribution> stacked = new ArrayList<>();
			for (ParticleContribution contribution : group) {
				if (contribution.stack()) {
					stacked.add(contribution);
				} else if (replaceWinner == null || contribution.priority() > replaceWinner.priority()) {
					replaceWinner = contribution;
				}
			}
			addPreset(resolved, replaceWinner);
			for (ParticleContribution contribution : stacked) {
				addPreset(resolved, contribution);
			}
		}
		return resolved;
	}

	private static void addPreset(List<ParticlePreset> out, ParticleContribution contribution) {
		if (contribution == null) {
			return;
		}
		ParticlePreset preset = ParticlePresets.get(contribution.presetId());
		if (preset == null) {
			DaggerCrafting.LOGGER.warn("Unknown particle preset {}", contribution.presetId());
			return;
		}
		out.add(preset);
	}
}
