package jak0bw.daggercrafting;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.item.DaggerItem;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.item.ItemGroups;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Handles registration and creative tab assignment for all SteelCrafting mod items.
 */
public class ModItems {
    // Holds all group addEntry actions to be performed at mod item registration
    private static List<Runnable> groupAddEntryActions = new ArrayList<>();

    /**
     * Registers an item with optional mod dependencies and creative tab groups.
     * Delays the registration of the item until the mod is fully initialized,
     * by adding it to a list of actions to be performed at mod item registration.
     *
     * @param name The name of the item to register.
     * @param factory A function that creates the item from the given Item.Settings.
     * @param settings The settings to use for the item.
     * @param groups The creative tab groups to which the item should be added.
     * @param dependencies Optional mod IDs that must be loaded for this item to be registered.
     * @return The registered Item, or null if any dependency is missing.
     */
    private static Item registerItem(String name, Function<Item.Settings, Item> factory, 
                                   Item.Settings settings, List<RegistryKey<ItemGroup>> groups, String... dependencies) {
        // Check dependencies
        if (java.util.Arrays.stream(dependencies).anyMatch(modId -> !FabricLoader.getInstance().isModLoaded(modId))) return null;
        // Register item
        Item item = Items.register(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(DaggerCrafting.MOD_ID, name)), factory, settings);
        // Registers the item in the game's item registry under the mod's namespace.
        if (groups != null) groups.forEach(group -> 
            groupAddEntryActions.add(() -> ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item))));
        return item;
    }


    /**
     * Map of dagger name (e.g., "wooden_dagger") to registered Item instance.
     */
    public static final Map<String, Item> DAGGER_ITEMS;

    static {
        Map<String, Item> map = new LinkedHashMap<>();
        for (Map.Entry<String, DaggerToolMaterial> entry : DaggerToolMaterial.DAGGER_TOOL_MATERIALS.entrySet()) {
            System.out.println("Registering dagger item: " + entry.getKey());
            map.put(entry.getKey(), registerItem(
                entry.getKey(),
                settings -> new DaggerItem(settings, entry.getValue()),
                new Item.Settings(),
                List.of(ItemGroups.COMBAT)
            ));
        }
        DAGGER_ITEMS = map;
    }

    /**
     * Registers all mod items to their respective creative tabs.
     * Should be called during mod initialization.
     */
    public static void registerModItems() {
        DaggerCrafting.LOGGER.info("Adding Mod Items to creative tabs for " + DaggerCrafting.MOD_ID);
        for (Runnable action : groupAddEntryActions) {
            action.run();
        }
    }
}

    