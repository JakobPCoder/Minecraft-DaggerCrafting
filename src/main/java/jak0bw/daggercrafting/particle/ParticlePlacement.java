package jak0bw.daggercrafting.particle;

/**
 * Where along the dagger a spec places its samples.
 *
 * Trail specs use {@link #MOTION_SUBSTEPS}. Block-hit bursts use {@link #IMPACT_RADIAL}.
 */
public enum ParticlePlacement {

	/**
	 * Even samples along the segment the dagger just traveled this tick (vanilla crit-arrow trick).
	 * Density stays roughly constant in space when throw speed changes.
	 */
	MOTION_SUBSTEPS,

	/**
	 * One-shot crumbs around a block contact point. Count and kick come from
	 * {@link ParticleSpawnSpec.RadialBurst} and impact speed, not throw charge.
	 */
	IMPACT_RADIAL
}
