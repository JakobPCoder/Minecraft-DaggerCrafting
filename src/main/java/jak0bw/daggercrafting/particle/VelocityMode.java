package jak0bw.daggercrafting.particle;

import net.minecraft.world.phys.Vec3;

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
	UPWARD_BIAS;

	/** Vanilla crit trail uses {@code -vel.y + 0.2} so sparks loft instead of matching the shot. */
	private static final double OPPOSE_Y_BOOST = 0.2;

	/** Small constant used by {@link #UPWARD_BIAS}; not a substitute for vanilla rising-particle lift. */
	private static final double UPWARD_SPEED = 0.02;

	/**
	 * @param motion dagger delta-movement for the segment being sampled
	 * @return aux velocity passed to {@link net.minecraft.world.level.Level#addParticle}
	 */
	public Vec3 auxVelocity(Vec3 motion) {
		return switch (this) {
			case ZERO -> Vec3.ZERO;
			case OPPOSE_MOTION -> new Vec3(-motion.x, -motion.y + OPPOSE_Y_BOOST, -motion.z);
			case UPWARD_BIAS -> new Vec3(0.0, UPWARD_SPEED, 0.0);
		};
	}
}
