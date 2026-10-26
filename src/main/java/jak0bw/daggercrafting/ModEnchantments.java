package jak0bw.daggercrafting;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Stable {@link RegistryKey} handles for dagger enchantments defined in datapacks.
 *
 * Enchantment behavior and levels live under {@code data/daggercrafting/enchantment/*.json}.
 * Java code references {@link #THROWING_SPEED} and {@link #RESTITUTION} for checks in
 * {@link jak0bw.daggercrafting.item.DaggerItem} or entity logic without hard-coding raw ids.
 */
public class ModEnchantments {

	/**
	 * Datapack enchantment that modifies thrown dagger speed or related ballistics.
	 *
	 * @see RegistryKeys#ENCHANTMENT
	 */
	public static final RegistryKey<Enchantment> THROWING_SPEED = of("throwing_speed");

	/**
	 * Datapack enchantment that returns a thrown dagger to the thrower on an entity hit.
	 *
	 * @see RegistryKeys#ENCHANTMENT
	 */
	public static final RegistryKey<Enchantment> RESTITUTION = of("restitution");

	/**
	 * @param path enchantment id path within {@link DaggerCrafting#MOD_ID}
	 * @return key suitable for registry lookups once the world/datapack is loaded
	 */
	private static RegistryKey<Enchantment> of(String path) {
		Identifier id = Identifier.of(DaggerCrafting.MOD_ID, path);
		return RegistryKey.of(RegistryKeys.ENCHANTMENT, id);
	}

	/**
	 * Startup hook reserved for future runtime enchantment setup.
	 *
	 * Today, registration is entirely JSON-driven; this method only logs so initialization
	 * order in {@link DaggerCrafting#onInitialize()} stays symmetric with other {@code Mod*} classes.
	 */
	public static void registerModEnchantments() {
		DaggerCrafting.LOGGER.info("Registering Enchantments for " + DaggerCrafting.MOD_ID);
	}
}
