package jak0bw.daggercrafting.particle;

import java.util.List;

import jak0bw.daggercrafting.entity.DaggerEntity;

import net.minecraft.block.BlockState;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Spawns client particles for the presets {@link ParticleContributors} selected this tick or hit.
 *
 * Default spawn is a no-op until the client replaces {@link #SPAWNER} (vanilla size). The client replaces
 * {@link #SPAWNER} so {@link ParticleSpawnSpec#quadScale()} can shrink the same vanilla sprites.
 * Trails sample the segment just simulated: {@code position - delta} toward {@code position}.
 * Block hits spawn radial {@code BLOCK} crumbs from a pre-stick {@link HitContext}.
 */
public final class ParticleDirector {

	/** Extra outward pull along the hit face so crumbs leave the wall more than they sink into it. */
	private static final double FACE_BIAS = 0.75;

	/** Nudge spawn origin off the surface so the first frame is not inside the block. */
	private static final double SURFACE_NUDGE = 0.02;

	private static Spawner SPAWNER = (
		world,
		particle,
		x,
		y,
		z,
		xd,
		yd,
		zd,
		quadScale
	) -> {
		// Client init installs {@link jak0bw.daggercrafting.DaggerCraftingClient#spawnScaledParticle}.
	};

	private ParticleDirector() {
	}

	/**
	 * Side-specific spawn: common code uses {@link World#addParticle} (ignores scale); the client
	 * replaces this with engine {@code createParticle} plus {@link net.minecraft.client.particle.Particle#scale}.
	 */
	@FunctionalInterface
	public interface Spawner {

		/**
		 * @param quadScale multiplier on the vanilla quad after construction; {@code 1} leaves default size
		 */
		void spawn(
			World world,
			ParticleEffect particle,
			double x,
			double y,
			double z,
			double xd,
			double yd,
			double zd,
			float quadScale
		);
	}

	/**
	 * Snapshot of a block strike taken before arrow stick logic zeros velocity.
	 *
	 * @param origin world contact point from the hit result
	 * @param motion dagger delta-movement at impact (pre-stick)
	 * @param state block that was hit; used to build {@code BlockStateParticleEffect}
	 * @param face struck face; crumbs bias along its outward normal
	 */
	public record HitContext(
		Vec3d origin,
		Vec3d motion,
		BlockState state,
		Direction face
	) {
	}

	/**
	 * Installs the client engine path that honors {@code quadScale}. Called from client init only.
	 *
	 * @param spawner client implementation
	 */
	public static void setSpawner(Spawner spawner) {
		SPAWNER = spawner;
	}

	/**
	 * In-flight trail hook. Call after {@code super.tick()} so position and velocity include this
	 * frame's move.
	 *
	 * @param dagger source projectile
	 */
	public static void tick(DaggerEntity dagger) {
		if (!dagger.getWorld().isClient) {
			return;
		}

		List<ParticlePresets.Preset> presets = ParticleContributors.resolve(dagger);
		if (presets.isEmpty()) {
			return;
		}

		Vec3d end = dagger.getPos();
		Vec3d motion = dagger.getVelocity();
		Vec3d start = end.subtract(motion);

		for (ParticlePresets.Preset preset : presets) {
			for (ParticleSpawnSpec spec : preset.specs()) {
				emitTrailSpec(dagger, spec, start, motion);
			}
		}
	}

	/**
	 * One-shot radial crumbs at the contact point. Skips air and blocks that hide terrain particles.
	 *
	 * @param dagger source projectile
	 * @param hit pre-stick contact snapshot
	 */
	public static void hitBlock(DaggerEntity dagger, HitContext hit) {
		if (!dagger.getWorld().isClient) {
			return;
		}

		BlockState state = hit.state();
		if (state.isAir()) {
			return;
		}

		List<ParticlePresets.Preset> presets = ParticleContributors.resolve(dagger);
		if (presets.isEmpty()) {
			return;
		}

		ParticleEffect particle = new BlockStateParticleEffect(ParticleTypes.BLOCK, state);
		for (ParticlePresets.Preset preset : presets) {
			for (ParticleSpawnSpec spec : preset.specs()) {
				if (spec.placement() == ParticleSpawnSpec.Placement.IMPACT_RADIAL) {
					emitRadialBurst(dagger, spec, hit, particle);
				}
			}
		}
	}

	private static void emitTrailSpec(DaggerEntity dagger, ParticleSpawnSpec spec, Vec3d start, Vec3d motion) {
		if (!shouldEmit(dagger, spec)) {
			return;
		}
		int period = Math.max(1, spec.periodTicks());
		if (dagger.age % period != 0) {
			return;
		}
		if (spec.placement() != ParticleSpawnSpec.Placement.MOTION_SUBSTEPS) {
			return;
		}
		if (spec.particle() == null) {
			return;
		}

		float charge = MathHelper.clamp(dagger.getThrowCharge(), 0.0F, 1.0F);
		int substepsMin = Math.max(1, spec.substepsMin());
		int substepsMax = Math.max(substepsMin, spec.substeps());
		// Charge 0 uses the spec mins; charge 1 uses the spec maxes (full throw).
		int substeps = Math.max(1, Math.round(MathHelper.lerp(charge, (float) substepsMin, (float) substepsMax)));
		float quadScale = MathHelper.lerp(charge, spec.quadScaleMin(), spec.quadScale());
		Vec3d aux = spec.velocityMode().auxVelocity(motion);
		Random random = dagger.getRandom();
		World world = dagger.getWorld();
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
			SPAWNER.spawn(world, spec.particle(), x, y, z, aux.x, aux.y, aux.z, quadScale);
		}
	}

	/**
	 * Spawns crumbs in a sphere biased out of the hit face. Count and kick scale with impact speed.
	 */
	private static void emitRadialBurst(
		DaggerEntity dagger,
		ParticleSpawnSpec spec,
		HitContext hit,
		ParticleEffect particle
	) {
		ParticleSpawnSpec.RadialBurst burst = spec.burst();
		if (burst == null) {
			return;
		}

		float speed = (float) hit.motion().length();
		int countMin = Math.max(1, burst.countMin());
		int countMax = Math.max(countMin, burst.countMax());
		int count = MathHelper.clamp(
			Math.round(countMin + burst.countPerSpeed() * speed),
			countMin,
			countMax
		);
		float kick = Math.min(burst.kickMax(), burst.kickMin() + burst.kickPerSpeed() * speed);

		Vec3d motion = hit.motion();
		Vec3d normal = new Vec3d(hit.face().getOffsetX(), hit.face().getOffsetY(), hit.face().getOffsetZ());
		Vec3d origin = hit.origin().add(normal.multiply(SURFACE_NUDGE));
		if (motion.lengthSquared() > 1.0E-8 && burst.spawnBehind() > 0.0) {
			origin = origin.subtract(motion.normalize().multiply(burst.spawnBehind()));
		}
		Vec3d inherited = motion.multiply(burst.motionInherit());
		Random random = dagger.getRandom();
		World world = dagger.getWorld();
		double offset = spec.offsetRadius();
		float quadScale = spec.quadScale();

		for (int i = 0; i < count; i++) {
			Vec3d dir = randomUnit(random);
			dir = dir.add(normal.multiply(FACE_BIAS));
			if (dir.lengthSquared() < 1.0E-8) {
				dir = normal;
			} else {
				dir = dir.normalize();
			}
			Vec3d aux = dir.multiply(kick).add(inherited);

			double x = origin.x;
			double y = origin.y;
			double z = origin.z;
			if (offset > 0.0) {
				x += (random.nextDouble() - 0.5) * 2.0 * offset;
				y += (random.nextDouble() - 0.5) * 2.0 * offset;
				z += (random.nextDouble() - 0.5) * 2.0 * offset;
			}
			SPAWNER.spawn(world, particle, x, y, z, aux.x, aux.y, aux.z, quadScale);
		}
	}

	/**
	 * Gaussian sample on the sphere; falls back to +Y if the vector collapses.
	 */
	private static Vec3d randomUnit(Random random) {
		Vec3d dir = new Vec3d(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
		if (dir.lengthSquared() < 1.0E-8) {
			return new Vec3d(0.0, 1.0, 0.0);
		}
		return dir.normalize();
	}

	/**
	 * Ground and water win over air/return so stuck or submerged daggers do not keep drawing a flight line.
	 */
	private static boolean shouldEmit(DaggerEntity dagger, ParticleSpawnSpec spec) {
		if (dagger.isStuckInGround()) {
			return spec.emitInGround();
		}
		if (dagger.isTouchingWater()) {
			return spec.emitInWater();
		}
		if (dagger.returnTimer > 0 || dagger.isNoClip()) {
			return spec.emitReturning();
		}
		return spec.emitInAir();
	}
}
