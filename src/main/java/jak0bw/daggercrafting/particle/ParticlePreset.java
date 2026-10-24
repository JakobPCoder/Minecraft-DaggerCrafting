package jak0bw.daggercrafting.particle;

import java.util.List;

import net.minecraft.resources.Identifier;

/**
 * Named bundle of spawn specs that compete or layer inside one exclusive {@link #group}.
 *
 * Contributors activate presets by id. {@link ParticleContributors} then resolves each group:
 * {@link #replaceGroup()} true means only the highest {@link #priority()} preset in that group
 * runs; {@code false} stacks on top of the winner (future overlays).
 *
 * @param id registry key, e.g. {@code daggercrafting:trail/default}
 * @param group exclusive channel such as {@code trail} or later {@code impact.entity}
 * @param priority higher wins when {@code replaceGroup} is set
 * @param replaceGroup when true, this preset is a mutually exclusive candidate for {@code group}
 * @param specs layers spawned together (flame + small flame + smoke)
 */
public record ParticlePreset(
	Identifier id,
	String group,
	int priority,
	boolean replaceGroup,
	List<ParticleSpawnSpec> specs
) {
}
