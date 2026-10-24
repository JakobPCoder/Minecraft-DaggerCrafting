package jak0bw.daggercrafting;

import jak0bw.daggercrafting.item.DaggerItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Handles registration and creative tab assignment for all DaggerCrafting items.
 */
public class ModItems {
	/**
	 * Creative-tab mutations are deferred until {@link #registerModItems()} so the
	 * Fabric tab event is subscribed during mod init, not class-load.
	 */
	private static List<Runnable> groupAddEntryActions = new ArrayList<>();

	/**
	 * Registers an item with optional mod dependencies and creative tab groups.
	 *
	 * @param name The name of the item to register.
	 * @param factory A function that creates the item from the given {@link Item.Properties}.
	 * @param properties The properties to use for the item (id is applied before the factory).
	 * @param groups The creative tabs to which the item should be added.
	 * @param dependencies Optional mod IDs that must be loaded for this item to be registered.
	 * @return The registered Item, or null if any dependency is missing.
	 */
	private static Item registerItem(
		String name,
		Function<Item.Properties, Item> factory,
		Item.Properties properties,
		List<ResourceKey<CreativeModeTab>> groups,
		String... dependencies
	) {
		if (java.util.Arrays.stream(dependencies).anyMatch(modId -> !FabricLoader.getInstance().isModLoaded(modId))) {
			return null;
		}

		ResourceKey<Item> key = ResourceKey.create(
			Registries.ITEM,
			Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, name)
		);
		Item item = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);

		if (groups != null) {
			groups.forEach(group ->
				groupAddEntryActions.add(() ->
					CreativeModeTabEvents.modifyOutputEvent(group).register(output -> output.accept(item))
				)
			);
		}
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
				properties -> new DaggerItem(properties, entry.getValue()),
				new Item.Properties(),
				List.of(CreativeModeTabs.COMBAT)
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
