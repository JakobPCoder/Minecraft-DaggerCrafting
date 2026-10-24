package jak0bw.daggercrafting;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Stat bundle for one dagger material tier: melee, thrown projectile, mining, durability, and enchantability.
 *
 * <p>Vanilla tools use {@code ToolMaterial}; daggers need extra thrown-attack fields and share one
 * {@link jak0bw.daggercrafting.item.DaggerItem} implementation, so tiers are defined here and
 * referenced from {@link ModItems} and {@link ModEntities}.
 *
 * <p>{@link #DAGGER_TOOL_MATERIALS} is filled in a static initializer (insertion order = tier order).
 * Keys such as {@code iron_dagger} double as item ids and throwable entity ids.
 */
public class DaggerToolMaterial {

	private final TagKey<Block> inverseTag;
	private final TagKey<Item> repairIngredientTag;
	private final float rangedDamage;
	private final float rangedVelocity;
	private final float attackDamage;
	private final float miningSpeed;
	private final int durability;
	private final int enchantability;

	/**
	 * All material tiers, keyed by registry name (e.g. {@code wooden_dagger}).
	 * Populated before any {@link ModItems} or {@link ModEntities} static logic runs.
	 */
	public static Map<String, DaggerToolMaterial> DAGGER_TOOL_MATERIALS = new LinkedHashMap<>();

	/**
	 * @param inverseTag blocks in this tag are mined slowly (vanilla incorrect-for-tool semantics)
	 * @param repairIngredientTag items that repair this tier in an anvil (usually {@code repairs_<name>})
	 * @param rangedDamage bonus damage applied when the dagger hits as a projectile
	 * @param rangedVelocity initial speed multiplier for thrown daggers
	 * @param attackDamage stored melee component; {@link jak0bw.daggercrafting.item.DaggerItem} adds vanilla base weapon damage on top
	 * @param miningSpeed offset from the dagger item's internal base dig speed
	 * @param durability max damage before break
	 * @param enchantability weight for enchantment table rolls (higher = better odds)
	 */
	public DaggerToolMaterial(
		TagKey<Block> inverseTag,
		TagKey<Item> repairIngredientTag,
		float rangedDamage,
		float rangedVelocity,
		float attackDamage,
		float miningSpeed,
		int durability,
		int enchantability
	) {
		this.inverseTag = inverseTag;
		this.repairIngredientTag = repairIngredientTag;
		this.rangedDamage = rangedDamage;
		this.rangedVelocity = rangedVelocity;
		this.attackDamage = attackDamage;
		this.miningSpeed = miningSpeed;
		this.durability = durability;
		this.enchantability = enchantability;
	}

	/** @return block tag used to penalize mining the wrong blocks for this tier */
	public TagKey<Block> getInverseTag() {
		return this.inverseTag;
	}

	/** @return item tag for anvil repair ingredients tied to this material */
	public TagKey<Item> getRepairIngredientTag() {
		return this.repairIngredientTag;
	}

	/** @return thrown-hit damage contribution before enchantments and crit rules */
	public float getRangedDamage() {
		return this.rangedDamage;
	}

	/** @return thrown launch speed factor consumed by {@link jak0bw.daggercrafting.entity.DaggerEntity} */
	public float getRangedVelocity() {
		return this.rangedVelocity;
	}

	/** @return melee damage component stored after {@link #registerDaggerMaterial} normalization */
	public float getAttackDamage() {
		return this.attackDamage;
	}

	/** @return dig-speed delta paired with the dagger item's fixed tool baseline */
	public float getMiningSpeed() {
		return this.miningSpeed;
	}

	/** @return maximum item damage capacity */
	public int getDurability() {
		return this.durability;
	}

	/** @return enchantability tier for table and loot rolls */
	public int getEnchantability() {
		return this.enchantability;
	}

	/**
	 * Defines a new tier, registers a matching {@code daggercrafting:repairs_<name>} item tag, and caches the instance.
	 *
	 * <p>The {@code attackDamage} argument is the designer-facing total before storage adjustment:
	 * one point is subtracted here so item code can add vanilla's +1 weapon baseline without double-counting.
	 *
	 * @param name registry stem shared by item and entity ({@code <name>})
	 * @param inverseTag vanilla incorrect-for-tool block tag for this tier
	 * @param rangedDamage thrown damage bonus
	 * @param rangedVelocity thrown speed multiplier
	 * @param attackDamage intended total melee damage before internal −1 normalization
	 * @param miningSpeed mining speed offset
	 * @param durability item durability
	 * @param enchantability enchantment roll weight
	 * @return the registered material (also inserted into {@link #DAGGER_TOOL_MATERIALS})
	 */
	public static DaggerToolMaterial registerDaggerMaterial(
		String name,
		TagKey<Block> inverseTag,
		float rangedDamage,
		float rangedVelocity,
		float attackDamage,
		float miningSpeed,
		int durability,
		int enchantability
	) {
		DaggerToolMaterial material = new DaggerToolMaterial(
			inverseTag,
			TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, "repairs_" + name)),
			rangedDamage,
			rangedVelocity,
			attackDamage - 1,
			miningSpeed,
			durability,
			enchantability
		);
		DAGGER_TOOL_MATERIALS.put(name, material);
		return material;
	}

	// Tier definitions mirror vanilla tool progression (wood → netherite) for familiar balance.
	static {
		registerDaggerMaterial(
			"wooden_dagger",
			BlockTags.INCORRECT_FOR_WOODEN_TOOL,
			4.0F,
			1.5F,
			2.0F,
			2.0F,
			30,
			15
		);
		registerDaggerMaterial(
			"golden_dagger",
			BlockTags.INCORRECT_FOR_GOLD_TOOL,
			4.0F,
			1.7F,
			2.0F,
			2.0F,
			20,
			25
		);
		registerDaggerMaterial(
			"stone_dagger",
			BlockTags.INCORRECT_FOR_STONE_TOOL,
			5.0F,
			1.6F,
			3.0F,
			2.0F,
			50,
			14
		);
		registerDaggerMaterial(
			"copper_dagger",
			BlockTags.INCORRECT_FOR_COPPER_TOOL,
			6.0F,
			1.7F,
			3.0F,
			3.0F,
			70,
			13
		);
		registerDaggerMaterial(
			"iron_dagger",
			BlockTags.INCORRECT_FOR_IRON_TOOL,
			7.0F,
			1.8F,
			4.0F,
			2.0F,
			100,
			11
		);
		registerDaggerMaterial(
			"diamond_dagger",
			BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
			8.0F,
			2.0F,
			5.0F,
			2.0F,
			250,
			10
		);
		registerDaggerMaterial(
			"netherite_dagger",
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
			9.0F,
			2.0F,
			6.0F,
			2.0F,
			500,
			15
		);
	}
}
