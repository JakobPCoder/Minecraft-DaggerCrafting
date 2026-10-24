package jak0bw.daggercrafting;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common (dedicated-server and client) entry point for the Dagger Crafting Fabric mod.
 *
 * <p>Fabric invokes {@link #onInitialize()} once at mod load. This class does not register
 * content itself; it delegates to the {@code Mod*} registries so initialization order stays
 * explicit and grep-friendly.
 */
public class DaggerCrafting implements ModInitializer {

	/** Namespace used for all registry ids, resource paths, and log attribution. */
	public static final String MOD_ID = "daggercrafting";

	/**
	 * Mod-scoped SLF4J logger. Using {@link #MOD_ID} as the logger name keeps log lines
	 * attributable to this mod in multi-mod packs.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * Wires mod content after Fabric has constructed the mod container.
	 *
	 * <p>Enchantment keys are initialized first (datapack-backed), then throwable entity types
	 * (one per {@link DaggerToolMaterial} tier), then creative-tab hooks for items. Item
	 * instances are created earlier in {@link ModItems}'s static initializer when that class
	 * is first referenced; {@link ModItems#registerModItems()} only flushes deferred tab events.
	 */
	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		ModEnchantments.registerModEnchantments();
		ModEntities.registerModEntities();
		ModItems.registerModItems();
	}
}
