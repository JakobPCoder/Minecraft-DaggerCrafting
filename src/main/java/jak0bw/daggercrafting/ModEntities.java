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
 * Registers one {@link DaggerEntity} type per material tier.
 *
 * Entity ids align with item ids ({@code wooden_dagger}, etc.) so thrown stacks can resolve
 * the correct projectile behavior via {@link ModItems#DAGGER_ITEMS} and
 * {@link DaggerToolMaterial}. Client rendering is bound in {@link DaggerCraftingClient}.
 */
public class ModEntities {

	/**
	 * Material name → registered entity type, filled in {@link #registerModEntities()}.
	 * Order matches {@link DaggerToolMaterial#DAGGER_TOOL_MATERIALS}.
	 */
	public static final LinkedHashMap<String, EntityType<DaggerEntity>> DAGGER_ENTITY_TYPES = new LinkedHashMap<>();

	/**
	 * Creates and registers a small misc projectile with conservative sync settings.
	 *
	 * @param name shared registry stem with the matching dagger item
	 * @return the type registered in {@link BuiltInRegistries#ENTITY_TYPE}
	 */
	private static EntityType<DaggerEntity> registerDaggerEntityType(String name) {
		Identifier id = Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, name);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		// MISC + half-block hitbox matches vanilla thrown items; low tracking range is enough for short flights.
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
	 * Registers every tier listed in {@link DaggerToolMaterial#DAGGER_TOOL_MATERIALS}.
	 *
	 * Invoked from {@link DaggerCrafting#onInitialize()} after enchantment keys are set up
	 * and before creative-tab hooks run.
	 */
	public static void registerModEntities() {
		DaggerCrafting.LOGGER.info("Registering Mod Entities for " + DaggerCrafting.MOD_ID);
		for (String name : DaggerToolMaterial.DAGGER_TOOL_MATERIALS.keySet()) {
			DAGGER_ENTITY_TYPES.put(name, registerDaggerEntityType(name));
		}
	}
}
