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
 * Registers dagger items and attaches them to creative tabs.
 *
 * <p>Items enter the game registry from this class's static initializer (one {@link DaggerItem}
 * per {@link DaggerToolMaterial} key). Fabric's creative tab API must be subscribed during mod
 * init, not at class load, so tab mutations are queued and executed in
 * {@link #registerModItems()} when {@link DaggerCrafting#onInitialize()} runs.
 */
public class ModItems {

	/**
	 * Deferred {@link CreativeModeTabEvents#modifyOutputEvent} subscriptions, filled by
	 * {@link #registerItem} and drained once at mod startup.
	 */
	private static List<Runnable> groupAddEntryActions = new ArrayList<>();

	/**
	 * Registers an item under {@link DaggerCrafting#MOD_ID} and optionally schedules creative-tab output.
	 *
	 * @param name registry path segment (e.g. {@code iron_dagger})
	 * @param factory constructs the item after {@code properties} receive a registry id
	 * @param properties base item properties; {@code setId} is applied before the factory runs
	 * @param groups tabs that should list this item; {@code null} skips tab wiring
	 * @param dependencies if any mod id is absent, registration is skipped and {@code null} is returned
	 * @return the registered item, or {@code null} when a dependency is missing
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
	 * Every throwable dagger tier keyed the same way as {@link DaggerToolMaterial#DAGGER_TOOL_MATERIALS}
	 * and {@link ModEntities#DAGGER_ENTITY_TYPES}.
	 */
	public static final Map<String, Item> DAGGER_ITEMS;

	// Build all dagger items as soon as this class loads (before onInitialize).
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
	 * Subscribes queued creative-tab listeners so daggers appear under Combat.
	 *
	 * <p>Must run on the mod initializer thread during {@link DaggerCrafting#onInitialize()};
	 * calling earlier would run before Fabric's tab event bus is ready.
	 */
	public static void registerModItems() {
		DaggerCrafting.LOGGER.info("Adding Mod Items to creative tabs for " + DaggerCrafting.MOD_ID);
		for (Runnable action : groupAddEntryActions) {
			action.run();
		}
	}
}
