package jak0bw.daggercrafting.particle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jak0bw.daggercrafting.DaggerCrafting;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;

/**
 * Java-side catalog of trail and block-impact presets.
 *
 * Numbers live here so visual tuning does not touch {@link ParticleDirector} or {@code DaggerEntity}.
 * Datapack/JSON loading is intentionally absent in v1.
 */
public final class ParticlePresets {

	/** Exclusive channel for in-flight trails. Default and blazing both replace inside this group. */
	public static final String GROUP_TRAIL = "trail";

	/** Exclusive channel for block-stick crumbs. Separate from trails so fire cannot replace the burst. */
	public static final String GROUP_IMPACT_BLOCK = "impact.block";

	/**
	 * Crit-spark line: density and size lerp with throw charge (min tap → full).
	 * Full charge stays under the previous 8 / 0.4 look.
	 */
	public static final ParticlePreset DEFAULT_TRAIL = new ParticlePreset(
		id("trail/default"),
		GROUP_TRAIL,
		0,
		true,
		List.of(
			ParticleSpawnSpec.airTrail(
				ParticleTypes.CRIT,
				3,
				6,
				0.02,
				VelocityMode.OPPOSE_MOTION,
				1.0f,
				0.18f,
				0.32f
			)
		)
	);

	/**
	 * On-fire replacement trail: flame body plus smaller fill, sparse smoke. Near-zero aux velocity
	 * so vanilla flame rise/flicker animates in place along the path.
	 */
	public static final ParticlePreset BLAZING_TRAIL = new ParticlePreset(
		id("trail/blazing"),
		GROUP_TRAIL,
		10,
		true,
		List.of(
			ParticleSpawnSpec.airTrail(
				ParticleTypes.FLAME,
				3,
				5,
				0.02,
				VelocityMode.ZERO,
				1.0f,
				0.22f,
				0.36f
			),
			ParticleSpawnSpec.airTrail(
				ParticleTypes.SMALL_FLAME,
				2,
				3,
				0.025,
				VelocityMode.ZERO,
				1.0f,
				0.25f,
				0.40f
			),
			ParticleSpawnSpec.airTrailPeriodic(
				ParticleTypes.SMOKE,
				1,
				2,
				2,
				0.03,
				VelocityMode.UPWARD_BIAS,
				0.5f,
				0.20f,
				0.32f
			)
		)
	);

	/**
	 * Terrain crumbs slightly behind the blade along incoming motion. Count and kick scale with
	 * impact speed; crumbs also inherit a fraction of that velocity. Texture comes from the hit
	 * {@code BlockState} at spawn time.
	 */
	public static final ParticlePreset DEFAULT_BLOCK_IMPACT = new ParticlePreset(
		id("impact/block/default"),
		GROUP_IMPACT_BLOCK,
		0,
		true,
		List.of(
			ParticleSpawnSpec.blockImpactRadial(
				new ParticleSpawnSpec.RadialBurst(4, 3.5f, 18, 0.04f, 0.027f, 0.187f, 0.12, 0.30f),
				0.02,
				0.75f
			)
		)
	);

	private static final Map<Identifier, ParticlePreset> BY_ID = new HashMap<>();

	static {
		register(DEFAULT_TRAIL);
		register(BLAZING_TRAIL);
		register(DEFAULT_BLOCK_IMPACT);
	}

	private ParticlePresets() {
	}

	/**
	 * @param presetId key passed from a {@link ParticleContribution}
	 * @return preset or {@code null} if the contributor referenced an unknown id
	 */
	public static ParticlePreset get(Identifier presetId) {
		return BY_ID.get(presetId);
	}

	private static void register(ParticlePreset preset) {
		BY_ID.put(preset.id(), preset);
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, path);
	}
}
