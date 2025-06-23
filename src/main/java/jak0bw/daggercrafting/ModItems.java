package jak0bw.daggercrafting;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.item.DaggerToolMaterial;
import jak0bw.daggercrafting.item.DaggerItem;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;
import net.minecraft.item.ItemGroups;

/**
 * Handles registration and creative tab assignment for all SteelCrafting mod items.
 */
public class ModItems {

    /**
     * Registers an item with the given name, factory, and settings.
     *
     * @param name     Path part of the identifier (e.g., "steel_ingot")
     * @param factory  Function to create the item from settings
     * @param settings Item settings
     * @return The registered Item
     */
    private static Item registerItem(String name, Function<Item.Settings, Item> factory, Item.Settings settings, String... dependencies) {
        for (String modId : dependencies) {
            if (!FabricLoader.getInstance().isModLoaded(modId)) {
                return null; 
            }
        }
    
        Identifier id = Identifier.of(DaggerCrafting.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        return Items.register(key, factory, settings);
    }

    // Diamond Arrow item
    public static final Item WOODEN_DAGGER = registerItem(
        "wooden_dagger",
        settings -> new DaggerItem(settings, DaggerToolMaterial.WOOD),
        new Item.Settings()
    );

    public static final Item STONE_DAGGER = registerItem(
        "stone_dagger",
        settings -> new DaggerItem(settings, DaggerToolMaterial.STONE),
        new Item.Settings()
    );

    public static final Item GOLDEN_DAGGER = registerItem(
        "golden_dagger",
        settings -> new DaggerItem(settings, DaggerToolMaterial.GOLD),
        new Item.Settings()
    );

    public static final Item IRON_DAGGER = registerItem(
        "iron_dagger",
        settings -> new DaggerItem(settings, DaggerToolMaterial.IRON),
        new Item.Settings()
    );

    public static final Item DIAMOND_DAGGER = registerItem(
        "diamond_dagger",
        settings -> new DaggerItem(settings, DaggerToolMaterial.DIAMOND),
        new Item.Settings()
    );

    private static void registerCombatTabItems() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(WOODEN_DAGGER);
            entries.add(STONE_DAGGER);
            entries.add(GOLDEN_DAGGER);
            entries.add(IRON_DAGGER);
            entries.add(DIAMOND_DAGGER);

        });

        // if (FabricLoader.getInstance().isModLoaded("coppercrafting")) 
        //     ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> { entries.add(COPPER_ARROW); });
    }

    /**
     * Registers all mod items to their respective creative tabs.
     * Should be called during mod initialization.
     */
    public static void registerModItems() {
        DaggerCrafting.LOGGER.info("Adding Mod Items to creative tabs for " + DaggerCrafting.MOD_ID);
        registerCombatTabItems();
    }
}

    