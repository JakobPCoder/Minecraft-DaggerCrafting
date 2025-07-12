package jak0bw.daggercrafting;

import java.util.Map;
import java.util.LinkedHashMap;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class DaggerToolMaterial {
    /**
     * The tag for blocks that are incorrect for this material.
     */
    private final TagKey<Block> inverseTag;
    /**
     * The tag for items that can repair this material.
     */
    private final TagKey<Item> repairIngredientTag;
    /**
     * The damage of the dagger when thrown.
     */
    private final float rangedDamage;
    /**
     * The velocity of the dagger when thrown.
     */
    private final float rangedVelocity;
    /**
     * The damage of the dagger when used in melee combat. A base damage of 1.0 is added on top of this.
     */
    private final float attackDamage;
    /**
     * The mining speed of the dagger. 4.0 is the base value, whatever we put here is added/subtracted from this.
     */
    private final float miningSpeed;
    /**
     * The durability of the dagger.
     */ 
    private final int durability;
    /**
     * The enchantability of the dagger.
     */
    private final int enchantability;

    /**
     * A map of all registered dagger materials.
     */
    public static Map<String, DaggerToolMaterial> DAGGER_TOOL_MATERIALS = new LinkedHashMap<>();

    /**
     * Constructor for the DaggerToolMaterial class.
     * @param inverseTag The tag for blocks that are incorrect for this material.
     * @param repairIngredientTag The tag for items that can repair this material.
     * @param rangedDamage The damage of the dagger when thrown.
     * @param rangedVelocity The velocity of the dagger when thrown.
     * @param attackDamage The damage of the dagger when used in melee combat.
     * @param miningSpeed The mining speed of the dagger.
     * @param durability The durability of the dagger.
     * @param enchantability The enchantability of the dagger.
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

    // All getter methods for both vanilla compatibility and custom features
    public TagKey<Block> getInverseTag() { return this.inverseTag; }
    public TagKey<Item> getRepairIngredientTag() { return this.repairIngredientTag; }
    public float getRangedDamage() { return this.rangedDamage; }
    public float getRangedVelocity() { return this.rangedVelocity; }
    public float getAttackDamage() { return this.attackDamage; }
    public float getMiningSpeed() { return this.miningSpeed; }
    public int getDurability() { return this.durability; }
    public int getEnchantability() { return this.enchantability; }



    /**
     * Registers a new dagger material with the given name, inverse tag, ranged damage, ranged velocity, attack damage, mining speed, durability, and enchantability.
     * @param name The name of the dagger material.
     * @param inverseTag The tag for blocks that are incorrect for this material.
     * @param rangedDamage The damage of the dagger when thrown.
     * @param rangedVelocity The velocity of the dagger when thrown.
     * @param attackDamage The damage of the dagger when used in melee combat.
     * @param miningSpeed The mining speed of the dagger.
     * @param durability The durability of the dagger.
     * @param enchantability The enchantability of the dagger.
     * @return The registered dagger material.
     */
    public static DaggerToolMaterial registerDaggerMaterial(String name, TagKey<Block> inverseTag, float rangedDamage, float rangedVelocity, float attackDamage, float miningSpeed, int durability, int enchantability) {
        DaggerToolMaterial material = new DaggerToolMaterial(
            inverseTag,
            TagKey.of(RegistryKeys.ITEM, Identifier.of(DaggerCrafting.MOD_ID, "repairs_" + name)),
            rangedDamage,
            rangedVelocity,
            attackDamage, // Changed from attackDamage - 1
            miningSpeed,
            durability,
            enchantability
        );
        DAGGER_TOOL_MATERIALS.put(name, material);
        return material;
    }

    static {
        registerDaggerMaterial(
            "golden_dagger",
            BlockTags.INCORRECT_FOR_GOLD_TOOL,
            4.0F,
            1.7F, // Changed from 1.6F
            2.0F,
            2.0F,
            20,
            25
        );
        registerDaggerMaterial(
            "wooden_dagger",
            BlockTags.INCORRECT_FOR_WOODEN_TOOL,
            4.0F,
            1.5F, // Changed from 1.4F
            2.0F,
            2.0F,
            30,
            15 // Changed from 14
        );
        registerDaggerMaterial(
            "stone_dagger",
            BlockTags.INCORRECT_FOR_STONE_TOOL,
            5.0F, // Changed from 6.0F
            1.6F,
            3.0F,
            2.0F,
            50,
            14 // Changed from 13
        );
        registerDaggerMaterial(
            "copper_dagger",
            BlockTags.INCORRECT_FOR_STONE_TOOL,
            6.0F,
            1.7F,
            2.0F,
            2.0F,
            70,
            13 // Changed from 12
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