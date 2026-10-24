package jak0bw.daggercrafting.particle;

import net.minecraft.resources.Identifier;

/**
 * One preset a {@link ParticleContributor} wants active this tick.
 *
 * Group, priority, and stack/replace usually copy the {@link ParticlePreset}; they are duplicated
 * here so a contributor can offer the same preset in another group later without a second catalog entry.
 *
 * @param group exclusive channel, e.g. {@link ParticlePresets#GROUP_TRAIL}
 * @param presetId key in {@link ParticlePresets}
 * @param priority higher wins among {@code stack == false} candidates in the same group
 * @param stack when true, layers with the replace winner; when false, this is a replace candidate
 */
public record ParticleContribution(
	String group,
	Identifier presetId,
	int priority,
	boolean stack
) {

	/** Copies group, id, priority, and invert of {@link ParticlePreset#replaceGroup()} from a catalog preset. */
	public static ParticleContribution of(ParticlePreset preset) {
		return new ParticleContribution(preset.group(), preset.id(), preset.priority(), !preset.replaceGroup());
	}
}
