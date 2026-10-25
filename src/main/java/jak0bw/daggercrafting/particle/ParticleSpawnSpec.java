package jak0bw.daggercrafting.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Immutable spawn recipe for one particle layer: what to spawn, how densely, and under which flight states.
 *
 * Lifetime, gravity, and sprite animation stay inside the vanilla particle class. This record only
 * controls spawn sampling so presets can share {@link ParticleDirector} without duplicating loops.
 *
 * @param particle vanilla options for trails; {@code null} when {@link Placement#IMPACT_RADIAL}
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
 * @param burst speed-scaled count/kick for {@link Placement#IMPACT_RADIAL}; {@code null} on trails
 */
public record ParticleSpawnSpec(
	@Nullable ParticleOptions particle,
	Placement placement,
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
	 * Where along the dagger a spec places its samples.
	 *
	 * Trail specs use {@link #MOTION_SUBSTEPS}. Block-hit bursts use {@link #IMPACT_RADIAL}.
	 */
	public enum Placement {

		/**
		 * Even samples along the segment the dagger just traveled this tick (vanilla crit-arrow trick).
		 * Density stays roughly constant in space when throw speed changes.
		 */
		MOTION_SUBSTEPS,

		/**
		 * One-shot crumbs around a block contact point. Count and kick come from
		 * {@link RadialBurst} and impact speed, not throw charge.
		 */
		IMPACT_RADIAL
	}

	/**
	 * How a spawned particle's aux velocity is derived from the dagger's motion this tick.
	 *
	 * Vanilla particle classes interpret the three doubles as initial {@code xd/yd/zd} (plus their own
	 * baked jitter). This enum only chooses those spawn args; it does not change {@code FlameParticle}
	 * lifetime or gravity.
	 */
	public enum VelocityMode {

		/** Pass zeros so baked particle physics (flame rise, crit constructor jitter) own the motion. */
		ZERO,

		/**
		 * Vanilla crit-arrow kick: opposite the projectile, with a small upward bias so sparks loft.
		 */
		OPPOSE_MOTION,

		/** Slight world-up so smoke/embers drift off the blade without following the throw. */
		UPWARD_BIAS,

		/**
		 * Calm end-rod loft for a stuck dagger. Initial {@code yd} is sized so vanilla
		 * {@code EndRodParticle} friction (~0.91, ~60 tick life) fades out around 1.5 blocks up.
		 */
		SLOW_RISE;

		/** Vanilla crit trail uses {@code -vel.y + 0.2} so sparks loft instead of matching the shot. */
		private static final double OPPOSE_Y_BOOST = 0.2;

		/** Small constant used by {@link #UPWARD_BIAS}; not a substitute for vanilla rising-particle lift. */
		private static final double UPWARD_SPEED = 0.02;

		/**
		 * Initial {@code yd} for {@link #SLOW_RISE}. Distance after friction
		 * {@code v * (1 - 0.91^n) / 0.09} is about 1.5 blocks at n ≈ 66.
		 */
		private static final double SLOW_RISE_SPEED = 0.135;

		/**
		 * @param motion dagger delta-movement for the segment being sampled
		 * @return aux velocity passed to {@link net.minecraft.world.level.Level#addParticle}
		 */
		public Vec3 auxVelocity(Vec3 motion) {
			return switch (this) {
				case ZERO -> Vec3.ZERO;
				case OPPOSE_MOTION -> new Vec3(-motion.x, -motion.y + OPPOSE_Y_BOOST, -motion.z);
				case UPWARD_BIAS -> new Vec3(0.0, UPWARD_SPEED, 0.0);
				case SLOW_RISE -> new Vec3(0.0, SLOW_RISE_SPEED, 0.0);
			};
		}
	}

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
			Placement.MOTION_SUBSTEPS,
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
			Placement.MOTION_SUBSTEPS,
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
	/**
	 * Sparse column from a stuck dagger. Motion is ~0 while in-ground, so one sample sits on the entity.
	 *
	 * @param particle type to spawn ({@code END_ROD} for a slim purple-white wisp)
	 * @param periodTicks ticks between spawns; keep high so the column stays incense-thin
	 * @param offsetRadius small jitter so wisps are not a perfect line
	 * @param velocityMode usually {@link VelocityMode#SLOW_RISE}
	 * @param spawnChance per-period chance
	 * @param quadScale client size vs vanilla default
	 */
	public static ParticleSpawnSpec groundWisp(
		ParticleOptions particle,
		int periodTicks,
		double offsetRadius,
		VelocityMode velocityMode,
		float spawnChance,
		float quadScale
	) {
		return new ParticleSpawnSpec(
			particle,
			Placement.MOTION_SUBSTEPS,
			1,
			1,
			periodTicks,
			offsetRadius,
			velocityMode,
			false,
			false,
			true,
			false,
			spawnChance,
			quadScale,
			quadScale,
			null
		);
	}

	public static ParticleSpawnSpec blockImpactRadial(
		RadialBurst burst,
		double offsetRadius,
		float quadScale
	) {
		return new ParticleSpawnSpec(
			null,
			Placement.IMPACT_RADIAL,
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
