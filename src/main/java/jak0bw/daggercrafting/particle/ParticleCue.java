package jak0bw.daggercrafting.particle;

/**
 * One-shot or periodic particle moments the director can be asked to handle.
 *
 * Trails use {@link #TRAIL_TICK} from {@link ParticleDirector#tick}. Block sticks use
 * {@link #HIT_BLOCK} with a {@link ParticleHitContext} captured before velocity is zeroed.
 */
public enum ParticleCue {

	/** Per-tick in-flight trail, driven from {@link jak0bw.daggercrafting.entity.DaggerEntity#tick()}. */
	TRAIL_TICK,

	/** Successful living-entity hit. Not emitted yet. */
	HIT_ENTITY_SUCCESS,

	/** Block / wall stick. Spawns {@link ParticlePlacement#IMPACT_RADIAL} crumbs on the client. */
	HIT_BLOCK
}
