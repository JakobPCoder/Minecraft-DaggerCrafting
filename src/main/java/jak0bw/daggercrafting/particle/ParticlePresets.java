package jak0bw.daggercrafting.particle;

import java.util.List;

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

	/** Exclusive channel for stuck-in-block idle wisps. */
	public static final String GROUP_GROUND = "ground.idle";

	/**
	 * Named bundle of spawn specs that compete or layer inside one exclusive {@link #group}.
	 *
	 * {@link ParticleContributors} resolves each group: {@link #replaceGroup()} true means only the
	 * highest {@link #priority()} preset in that group runs; {@code false} stacks on top of the winner.
	 *
	 * @param id registry-style key, e.g. {@code daggercrafting:trail/default}
	 * @param group exclusive channel such as {@code trail} or {@code impact.block}
	 * @param priority higher wins when {@code replaceGroup} is set
	 * @param replaceGroup when true, this preset is a mutually exclusive candidate for {@code group}
	 * @param specs layers spawned together (flame + small flame + smoke)
	 */
	public record Preset(
		Identifier id,
		String group,
		int priority,
		boolean replaceGroup,
		List<ParticleSpawnSpec> specs
	) {
	}

	/**
	 * Crit-spark line: density and size lerp with throw charge (min tap → full).
	 * Full charge stays under the previous 8 / 0.4 look.
	 */
	public static final Preset DEFAULT_TRAIL = new Preset(
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
				ParticleSpawnSpec.VelocityMode.OPPOSE_MOTION,
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
	public static final Preset BLAZING_TRAIL = new Preset(
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
				ParticleSpawnSpec.VelocityMode.ZERO,
				1.0f,
				0.22f,
				0.36f
			),
			ParticleSpawnSpec.airTrail(
				ParticleTypes.SMALL_FLAME,
				2,
				3,
				0.025,
				ParticleSpawnSpec.VelocityMode.ZERO,
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
				ParticleSpawnSpec.VelocityMode.UPWARD_BIAS,
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
	public static final Preset DEFAULT_BLOCK_IMPACT = new Preset(
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

	/**
	 * Slim end-rod wisp while any dagger sits in a block.
	 *
	 * {@code END_ROD} is the one vanilla type that is already a thin, faintly purple spark with a
	 * long fade (~3s) and no extra burst sprites. Spawn is sparse so overlapping lives read as one
	 * calm column, not a fountain.
	 */
	public static final Preset STUCK_GROUND = new Preset(
		id("ground/stuck"),
		GROUP_GROUND,
		0,
		true,
		List.of(
			ParticleSpawnSpec.groundWisp(
				ParticleTypes.END_ROD,
				40,
				0.05,
				ParticleSpawnSpec.VelocityMode.SLOW_RISE,
				1.0f,
				0.38f
			)
		)
	);

	private ParticlePresets() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, path);
	}
}
