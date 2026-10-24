package jak0bw.daggercrafting;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Fabric datagen entry point (see {@code fabric.mod.json} {@code entrypoints}).
 *
 * <p>When providers are added here, {@link FabricDataGenerator#createPack()} can emit tags,
 * recipes, and models that stay in sync with {@link DaggerToolMaterial} and {@link ModItems}.
 * The body is intentionally empty until datagen is wired for this version.
 */
public class DaggerCraftingDataGenerator implements DataGeneratorEntrypoint {

	/**
	 * @param fabricDataGenerator Gradle-invoked generator; register {@code Pack} extensions inside
	 */
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
	}
}
