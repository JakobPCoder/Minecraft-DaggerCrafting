package jak0bw.daggercrafting.particle;

import java.util.List;

import jak0bw.daggercrafting.entity.DaggerEntity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Spawns client particles for the presets contributors selected this tick or hit.
 *
 * Default spawn is {@link Level#addParticle} (server no-op, vanilla size). The client replaces
 * {@link #SPAWNER} so {@link ParticleSpawnSpec#quadScale()} can shrink the same vanilla sprites.
 * Trails sample the segment just simulated: {@code position - delta} toward {@code position}.
 * Block hits spawn radial {@code BLOCK} crumbs from a pre-stick {@link ParticleHitContext}.
 */
public final class ParticleDirector {

	/** Extra outward pull along the hit face so crumbs leave the wall more than they sink into it. */
	private static final double FACE_BIAS = 0.75;

	/** Nudge spawn origin off the surface so the first frame is not inside the block. */
	private static final double SURFACE_NUDGE = 0.02;

	private static ParticleSpawner SPAWNER = (
		level,
		particle,
		x,
		y,
		z,
		xd,
		yd,
		zd,
		quadScale
	) -> level.addParticle(particle, x, y, z, xd, yd, zd);

	private ParticleDirector() {
	}

	/**
	 * Installs the client engine path that honors {@code quadScale}. Called from client init only.
	 */
	public static void setSpawner(ParticleSpawner spawner) {
		SPAWNER = spawner;
	}

	/** In-flight trail hook; call after {@code super.tick()} so position/velocity include this frame's move. */
	public static void tick(DaggerEntity dagger) {
		emit(ParticleCue.TRAIL_TICK, dagger, null);
	}

	/**
	 * Cue entry without a hit snapshot (trails).
	 *
	 * @param cue moment being requested
	 * @param dagger source projectile
	 */
	public static void emit(ParticleCue cue, DaggerEntity dagger) {
		emit(cue, dagger, null);
	}

	/**
	 * Cue entry for trails and block impacts.
	 *
	 * @param cue moment being requested
	 * @param dagger source projectile
	 * @param hit pre-stick contact snapshot; required for {@link ParticleCue#HIT_BLOCK}
	 */
	public static void emit(ParticleCue cue, DaggerEntity dagger, @Nullable ParticleHitContext hit) {
		if (!dagger.level().isClientSide()) {
			return;
		}
		if (cue == ParticleCue.HIT_BLOCK) {
			emitBlockHit(dagger, hit);
			return;
		}
		if (cue != ParticleCue.TRAIL_TICK) {
			return;
		}

		List<ParticlePreset> presets = ParticleContributors.resolve(dagger);
		if (presets.isEmpty()) {
			return;
		}

		Vec3 end = dagger.position();
		Vec3 motion = dagger.getDeltaMovement();
		Vec3 start = end.subtract(motion);

		for (ParticlePreset preset : presets) {
			for (ParticleSpawnSpec spec : preset.specs()) {
				emitTrailSpec(dagger, spec, start, motion);
			}
		}
	}

	/**
	 * One-shot radial crumbs at the contact point. Skips air and blocks that hide terrain particles.
	 */
	private static void emitBlockHit(DaggerEntity dagger, @Nullable ParticleHitContext hit) {
		if (hit == null) {
			return;
		}
		BlockState state = hit.state();
		if (state.isAir() || !state.shouldSpawnTerrainParticles()) {
			return;
		}

		List<ParticlePreset> presets = ParticleContributors.resolve(dagger);
		if (presets.isEmpty()) {
			return;
		}

		ParticleOptions particle = new BlockParticleOption(ParticleTypes.BLOCK, state);
		for (ParticlePreset preset : presets) {
			for (ParticleSpawnSpec spec : preset.specs()) {
				if (spec.placement() == ParticlePlacement.IMPACT_RADIAL) {
					emitRadialBurst(dagger, spec, hit, particle);
				}
			}
		}
	}

	private static void emitTrailSpec(DaggerEntity dagger, ParticleSpawnSpec spec, Vec3 start, Vec3 motion) {
		if (!shouldEmit(dagger, spec)) {
			return;
		}
		int period = Math.max(1, spec.periodTicks());
		if (dagger.tickCount % period != 0) {
			return;
		}
		if (spec.placement() != ParticlePlacement.MOTION_SUBSTEPS) {
			return;
		}
		if (spec.particle() == null) {
			return;
		}

		float charge = Mth.clamp(dagger.getThrowCharge(), 0.0F, 1.0F);
		int substepsMin = Math.max(1, spec.substepsMin());
		int substepsMax = Math.max(substepsMin, spec.substeps());
		// Charge 0 uses the spec mins; charge 1 uses the spec maxes (full throw).
		int substeps = Math.max(1, Math.round(Mth.lerp(charge, (float) substepsMin, (float) substepsMax)));
		float quadScale = Mth.lerp(charge, spec.quadScaleMin(), spec.quadScale());
		Vec3 aux = spec.velocityMode().auxVelocity(motion);
		RandomSource random = dagger.getRandom();
		Level level = dagger.level();
		double offset = spec.offsetRadius();
		float chance = spec.spawnChance();

		for (int i = 0; i < substeps; i++) {
			if (chance < 1.0f && random.nextFloat() > chance) {
				continue;
			}

			double t = (double) i / (double) substeps;
			double x = start.x + motion.x * t;
			double y = start.y + motion.y * t;
			double z = start.z + motion.z * t;
			if (offset > 0.0) {
				x += (random.nextDouble() - 0.5) * 2.0 * offset;
				y += (random.nextDouble() - 0.5) * 2.0 * offset;
				z += (random.nextDouble() - 0.5) * 2.0 * offset;
			}
			SPAWNER.spawn(level, spec.particle(), x, y, z, aux.x, aux.y, aux.z, quadScale);
		}
	}

	/**
	 * Spawns crumbs in a sphere biased out of the hit face. Count and kick scale with impact speed.
	 */
	private static void emitRadialBurst(
		DaggerEntity dagger,
		ParticleSpawnSpec spec,
		ParticleHitContext hit,
		ParticleOptions particle
	) {
		ParticleSpawnSpec.RadialBurst burst = spec.burst();
		if (burst == null) {
			return;
		}

		float speed = (float) hit.motion().length();
		int countMin = Math.max(1, burst.countMin());
		int countMax = Math.max(countMin, burst.countMax());
		int count = Mth.clamp(
			Math.round(countMin + burst.countPerSpeed() * speed),
			countMin,
			countMax
		);
		float kick = Math.min(burst.kickMax(), burst.kickMin() + burst.kickPerSpeed() * speed);

		Vec3 motion = hit.motion();
		Vec3 normal = hit.face().getUnitVec3();
		Vec3 origin = hit.origin().add(normal.scale(SURFACE_NUDGE));
		if (motion.lengthSqr() > 1.0E-8 && burst.spawnBehind() > 0.0) {
			origin = origin.subtract(motion.normalize().scale(burst.spawnBehind()));
		}
		Vec3 inherited = motion.scale(burst.motionInherit());
		RandomSource random = dagger.getRandom();
		Level level = dagger.level();
		double offset = spec.offsetRadius();
		float quadScale = spec.quadScale();

		for (int i = 0; i < count; i++) {
			Vec3 dir = randomUnit(random);
			dir = dir.add(normal.scale(FACE_BIAS));
			if (dir.lengthSqr() < 1.0E-8) {
				dir = normal;
			} else {
				dir = dir.normalize();
			}
			Vec3 aux = dir.scale(kick).add(inherited);

			double x = origin.x;
			double y = origin.y;
			double z = origin.z;
			if (offset > 0.0) {
				x += (random.nextDouble() - 0.5) * 2.0 * offset;
				y += (random.nextDouble() - 0.5) * 2.0 * offset;
				z += (random.nextDouble() - 0.5) * 2.0 * offset;
			}
			SPAWNER.spawn(level, particle, x, y, z, aux.x, aux.y, aux.z, quadScale);
		}
	}

	/**
	 * Gaussian sample on the sphere; falls back to +Y if the vector collapses.
	 */
	private static Vec3 randomUnit(RandomSource random) {
		Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
		if (dir.lengthSqr() < 1.0E-8) {
			return new Vec3(0.0, 1.0, 0.0);
		}
		return dir.normalize();
	}

	/**
	 * Ground and water win over air/return so stuck or submerged daggers do not keep drawing a flight line.
	 */
	private static boolean shouldEmit(DaggerEntity dagger, ParticleSpawnSpec spec) {
		if (dagger.isInGround()) {
			return spec.emitInGround();
		}
		if (dagger.isInWater()) {
			return spec.emitInWater();
		}
		if (dagger.returnTimer > 0 || dagger.isNoPhysics()) {
			return spec.emitReturning();
		}
		return spec.emitInAir();
	}
}
