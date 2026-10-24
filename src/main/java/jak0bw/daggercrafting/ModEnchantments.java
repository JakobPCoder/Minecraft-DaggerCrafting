package jak0bw.daggercrafting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Stable {@link ResourceKey} handles for dagger enchantments defined in datapacks.
 *
 * <p>Enchantment behavior and levels live under {@code data/daggercrafting/enchantment/*.json}.
 * Java code references {@link #THROWING_SPEED} (and future keys) for checks in
 * {@link jak0bw.daggercrafting.item.DaggerItem} or entity logic without hard-coding raw ids.
 */
public class ModEnchantments {

	/**
	 * Datapack enchantment that modifies thrown dagger speed or related ballistics.
	 *
	 * @see Registries#ENCHANTMENT
	 */
	public static final ResourceKey<Enchantment> THROWING_SPEED = of("throwing_speed");

	/**
	 * @param path enchantment id path within {@link DaggerCrafting#MOD_ID}
	 * @return key suitable for registry lookups once the world/datapack is loaded
	 */
	private static ResourceKey<Enchantment> of(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, path);
		return ResourceKey.create(Registries.ENCHANTMENT, id);
	}

	/**
	 * Startup hook reserved for future runtime enchantment setup.
	 *
	 * <p>Today, registration is entirely JSON-driven; this method only logs so initialization
	 * order in {@link DaggerCrafting#onInitialize()} stays symmetric with other {@code Mod*} classes.
	 */
	public static void registerModEnchantments() {
		DaggerCrafting.LOGGER.info("Registering Enchantments for " + DaggerCrafting.MOD_ID);
	}
}
