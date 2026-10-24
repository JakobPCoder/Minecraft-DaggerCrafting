package jak0bw.daggercrafting.particle;

import net.minecraft.core.particles.ParticleOptions;
import org.jspecify.annotations.Nullable;

/**
 * Immutable spawn recipe for one particle layer: what to spawn, how densely, and under which flight states.
 *
 * Lifetime, gravity, and sprite animation stay inside the vanilla particle class. This record only
 * controls spawn sampling so presets can share {@link ParticleDirector} without duplicating loops.
 *
 * @param particle vanilla options for trails; {@code null} when {@link ParticlePlacement#IMPACT_RADIAL}
 *     fills {@code BlockParticleOption} from the hit block
 * @param placement trail substeps or a one-shot radial burst
 * @param substepsMin motion samples at throw charge {@code 0}
 * @param substeps samples at throw charge {@code 1} (vanilla crit arrows use 4)
 * @param periodTicks emit only when {@code tickCount % periodTicks == 0}; {@code 1} is every tick
 * @param offsetRadius random cube half-extent around each sample; keep small for a thin line
 * @param velocityMode mapping from dagger motion to particle aux velocity
 * @param emitInAir outbound flight while not in water or ground
 * @param emitReturning loyalty homing ({@code returnTimer > 0} or no-physics)
 * @param emitInGround stuck in a block
 * @param emitInWater moving through water (vanilla already draws bubbles via {@code AbstractArrow})
 * @param spawnChance per-sample probability in {@code [0, 1]}
 * @param quadScaleMin client quad multiplier at throw charge {@code 0}
 * @param quadScale client quad multiplier at throw charge {@code 1}, or impact crumb size
 * @param burst speed-scaled count/kick for {@link ParticlePlacement#IMPACT_RADIAL}; {@code null} on trails
 */
public record ParticleSpawnSpec(
	@Nullable ParticleOptions particle,
	ParticlePlacement placement,
	int substepsMin,
	int substeps,
	int periodTicks,
	double offsetRadius,
	VelocityMode velocityMode,
	boolean emitInAir,
	boolean emitReturning,
	boolean emitInGround,
	boolean emitInWater,
	float spawnChance,
	float quadScaleMin,
	float quadScale,
	@Nullable RadialBurst burst
) {

	/**
	 * Count and fly-away speed for a block-hit crumb burst.
	 *
	 * {@code count = clamp(round(countMin + countPerSpeed * speed), countMin, countMax)}
	 * {@code kick = min(kickMax, kickMin + kickPerSpeed * speed)}
	 * where {@code speed} is dagger motion length in blocks per tick.
	 *
	 * @param countMin crumbs on the slowest legal hit
	 * @param countPerSpeed extra crumbs per 1.0 motion
	 * @param countMax hard cap so netherite plus speed stays readable
	 * @param kickMin aux speed at near-zero impact
	 * @param kickPerSpeed extra aux speed per 1.0 motion
	 * @param kickMax hard cap on crumb kick
	 * @param spawnBehind how far behind the contact along incoming motion to spawn
	 * @param motionInherit fraction of dagger velocity added on top of the radial kick
	 */
	public record RadialBurst(
		int countMin,
		float countPerSpeed,
		int countMax,
		float kickMin,
		float kickPerSpeed,
		float kickMax,
		double spawnBehind,
		float motionInherit
	) {
	}

	/**
	 * In-air plus loyalty-return trail line using motion substeps every tick.
	 *
	 * @param particle type to spawn
	 * @param substepsMin density at minimum legal charge
	 * @param substeps density at full charge
	 * @param offsetRadius jitter around the line
	 * @param velocityMode aux velocity mapping
	 * @param spawnChance per-sample chance
	 * @param quadScaleMin size at minimum legal charge
	 * @param quadScale size at full charge
	 */
	public static ParticleSpawnSpec airTrail(
		ParticleOptions particle,
		int substepsMin,
		int substeps,
		double offsetRadius,
		VelocityMode velocityMode,
		float spawnChance,
		float quadScaleMin,
		float quadScale
	) {
		return new ParticleSpawnSpec(
			particle,
			ParticlePlacement.MOTION_SUBSTEPS,
			substepsMin,
			substeps,
			1,
			offsetRadius,
			velocityMode,
			true,
			true,
			false,
			false,
			spawnChance,
			quadScaleMin,
			quadScale,
			null
		);
	}

	/**
	 * Same as {@link #airTrail} but only on every {@code periodTicks} tick (sparse smoke/embers).
	 *
	 * @param quadScale visual size vs vanilla default
	 */
	public static ParticleSpawnSpec airTrailPeriodic(
		ParticleOptions particle,
		int substepsMin,
		int substeps,
		int periodTicks,
		double offsetRadius,
		VelocityMode velocityMode,
		float spawnChance,
		float quadScaleMin,
		float quadScale
	) {
		return new ParticleSpawnSpec(
			particle,
			ParticlePlacement.MOTION_SUBSTEPS,
			substepsMin,
			substeps,
			periodTicks,
			offsetRadius,
			velocityMode,
			true,
			true,
			false,
			false,
			spawnChance,
			quadScaleMin,
			quadScale,
			null
		);
	}

	/**
	 * One-shot terrain crumbs at a block contact; the director supplies {@code BLOCK} from the hit state.
	 *
	 * @param burst speed-scaled count and kick
	 * @param offsetRadius jitter around the contact point
	 * @param quadScale client size vs vanilla terrain default
	 */
	public static ParticleSpawnSpec blockImpactRadial(
		RadialBurst burst,
		double offsetRadius,
		float quadScale
	) {
		return new ParticleSpawnSpec(
			null,
			ParticlePlacement.IMPACT_RADIAL,
			0,
			0,
			1,
			offsetRadius,
			VelocityMode.ZERO,
			false,
			false,
			false,
			false,
			1.0f,
			quadScale,
			quadScale,
			burst
		);
	}
}
