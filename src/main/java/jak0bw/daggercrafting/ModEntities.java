package jak0bw.daggercrafting;

import jak0bw.daggercrafting.entity.DaggerEntity;

import java.util.LinkedHashMap;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Handles registration of custom dagger entities for the DaggerCrafting mod.
 * Uses a static list of dagger type names and a static map for entity types, registered via a helper method.
 */
public class ModEntities {
	/**
	 * Map of dagger entity type name to registered EntityType.
	 * Populated during mod initialization.
	 */
	public static final LinkedHashMap<String, EntityType<DaggerEntity>> DAGGER_ENTITY_TYPES = new LinkedHashMap<>();

	/**
	 * Helper to register a DaggerEntity type for a given name.
	 *
	 * @param name The dagger entity type name (e.g., "iron_dagger").
	 * @return The registered EntityType.
	 */
	private static EntityType<DaggerEntity> registerDaggerEntityType(String name) {
		Identifier id = Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, name);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			key,
			EntityType.Builder.<DaggerEntity>of(DaggerEntity::new, MobCategory.MISC)
				.sized(0.5f, 0.5f)
				.clientTrackingRange(4)
				.updateInterval(20)
				.build(key)
		);
	}

	/**
	 * Registers all dagger entity types and populates the DAGGER_ENTITY_TYPES map.
	 * Should be called during mod initialization.
	 */
	public static void registerModEntities() {
		DaggerCrafting.LOGGER.info("Registering Mod Entities for " + DaggerCrafting.MOD_ID);
		for (String name : DaggerToolMaterial.DAGGER_TOOL_MATERIALS.keySet()) {
			DAGGER_ENTITY_TYPES.put(name, registerDaggerEntityType(name));
		}
	}
}
