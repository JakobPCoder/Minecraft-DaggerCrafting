package jak0bw.daggercrafting.particle;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Snapshot of a block strike taken before arrow stick logic zeros velocity.
 *
 * @param origin world contact point from the hit result
 * @param motion dagger delta-movement at impact (pre-stick)
 * @param state block that was hit; used to build {@code BlockParticleOption}
 * @param face struck face; crumbs bias along its outward normal
 */
public record ParticleHitContext(
	Vec3 origin,
	Vec3 motion,
	BlockState state,
	Direction face
) {
}
