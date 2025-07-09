package jak0bw.daggercrafting;

import jak0bw.daggercrafting.entity.DaggerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

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
     * @param name The dagger entity type name (e.g., "iron_dagger").
     * @return The registered EntityType.
     */
    private static EntityType<DaggerEntity> registerDaggerEntityType(String name) {
        Identifier id = Identifier.of(DaggerCrafting.MOD_ID, name);
        return Registry.register(
            Registries.ENTITY_TYPE,
            id,
            EntityType.Builder.<DaggerEntity>create(DaggerEntity::new, SpawnGroup.MISC)
                .dimensions(0.5f, 0.5f)
                .maxTrackingRange(4)
                .trackingTickInterval(20)
                .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, id))
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