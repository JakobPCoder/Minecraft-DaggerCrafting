package jak0bw.daggercrafting;

import jak0bw.daggercrafting.entity.DaggerEntity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Handles registration of custom entities for the SteelCrafting mod.
 */
public class ModEntities {
    
    /**
     * Wood Arrow Entity Type - extends ArrowEntity with increased damage
     */
    public static final EntityType<DaggerEntity> IRON_DAGGER = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(DaggerCrafting.MOD_ID, "iron_dagger"),
        EntityType.Builder.<DaggerEntity>create(DaggerEntity::new,  SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(4)
            .trackingTickInterval(20)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(DaggerCrafting.MOD_ID, "iron_dagger")))
    );

    
    public static final EntityType<DaggerEntity> DIAMOND_DAGGER = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(DaggerCrafting.MOD_ID, "diamond_dagger"),
        EntityType.Builder.<DaggerEntity>create(DaggerEntity::new,  SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(4)
            .trackingTickInterval(20)
            .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(DaggerCrafting.MOD_ID, "diamond_dagger")))
    );

    /**
     * Registers all mod entities.
     * Should be called during mod initialization.
     */
    public static void registerModEntities() {
        DaggerCrafting.LOGGER.info("Registering Mod Entities for " + DaggerCrafting.MOD_ID);
        // The entity is already registered above via the static field initialization
        // This method just logs that registration has completed
    }
} 