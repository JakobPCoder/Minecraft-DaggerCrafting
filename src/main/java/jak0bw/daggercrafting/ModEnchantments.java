package jak0bw.daggercrafting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Holds registry keys for datapack-driven dagger enchantments.
 *
 * <p>The enchantment itself is registered from JSON under
 * {@code data/daggercrafting/enchantment/}. Java only needs the key for lookups.
 */
public class ModEnchantments {
	public static final ResourceKey<Enchantment> THROWING_SPEED = of("throwing_speed");

	/**
	 * Builds a datapack enchantment key in this mod's namespace.
	 *
	 * @param path enchantment id path, for example {@code throwing_speed}
	 * @return resource key in {@link Registries#ENCHANTMENT}
	 */
	private static ResourceKey<Enchantment> of(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, path);
		return ResourceKey.create(Registries.ENCHANTMENT, id);
	}

	/**
	 * Logs that enchantment keys are ready. JSON performs the actual registration.
	 */
	public static void registerModEnchantments() {
		DaggerCrafting.LOGGER.info("Registering Enchantments for " + DaggerCrafting.MOD_ID);
	}
}
