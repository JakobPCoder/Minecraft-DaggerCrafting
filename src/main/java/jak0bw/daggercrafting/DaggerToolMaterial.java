package jak0bw.daggercrafting;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class DaggerToolMaterial {
    // All vanilla ToolMaterial properties
    private final TagKey<Block> inverseTag;
    private final int durability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final TagKey<Item> repairIngredientTag;
    
    // Our custom dagger-specific properties
    private final float rangedDamage;
    private final float rangedVelocity;
    
    public DaggerToolMaterial(
        TagKey<Block> inverseTag, 
        int durability, 
        float miningSpeed, 
        float attackDamage, 
        int enchantability, 
        TagKey<Item> repairIngredientTag, 
        float rangedDamage, 
        float rangedVelocity
        ) {

        this.inverseTag = inverseTag;
        this.durability = durability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.repairIngredientTag = repairIngredientTag;
        this.rangedDamage = rangedDamage;
        this.rangedVelocity = rangedVelocity;
    }


    // All getter methods for both vanilla compatibility and custom features
    // Getter for inverseTag (incorrect blocks for drops)
    public TagKey<Block> getInverseTag() {
        return this.inverseTag;
    }

    // Getter for durability
    public int getDurability() {
        return this.durability;
    }

    // Getter for miningSpeed
    public float getMiningSpeed() {
        return this.miningSpeed;
    }
    // Getter for attackDamage
    public float getAttackDamage() {
        return this.attackDamage;
    }

    // Getter for enchantability
    public int getEnchantability() {
        return this.enchantability;
    }

    // Getter for repairIngredientTag
    public TagKey<Item> getRepairIngredientTag() {
        return this.repairIngredientTag;
    }
    // Getter for rangedDamage
    public float getRangedDamage() {
        return this.rangedDamage;
    }

    // Getter for rangedVelocity
    public float getRangedVelocity() {
        return this.rangedVelocity;
    }


    public static final DaggerToolMaterial WOOD = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_WOODEN_TOOL,
        30,  
        2.0F, // Wood mining speed
        1.0F, 
        15,   
        TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_wooden_dagger")),
        3.0F, // Ranged damage
        1.4F  // Ranged velocity
    );


    public static final DaggerToolMaterial STONE = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        50,  
        2.0F, 
        2.0F, 
        12,    
        TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_stone_dagger")),
        5.0F, // Ranged damage
        1.6F  // Ranged velocity
    );

    public static final DaggerToolMaterial IRON = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_IRON_TOOL,
        100,   
        2.0F, // 
        3.0F, // 
        11,     // steel will also be 11
        TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_iron_dagger")),
        7.0F, // Ranged damage
        1.8F // Ranged velocity
    );



    public static final DaggerToolMaterial GOLD = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_GOLD_TOOL,
        20,  
        2.0F, //
        1.0F, // 
        25,  // Copper will be 20
        TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_golden_dagger")),
        3.0F, // Ranged damage
        1.6F  // Ranged velocity
    );

    public static final DaggerToolMaterial DIAMOND = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        250,   
        2.0F, // 
        4.0F, // 
        10,   
        TagKey.of(RegistryKeys.ITEM, Identifier.of("daggercrafting", "repairs_diamond_dagger")),
        8.0F, // Ranged damage
        2.0F  // Ranged velocity
    );


    public static final Map<String, DaggerToolMaterial> DAGGER_TOOL_MATERIALS;

    static {
        Map<String, DaggerToolMaterial> map = new LinkedHashMap<>();
        map.put("wooden_dagger", WOOD);
        map.put("stone_dagger", STONE);
        map.put("iron_dagger", IRON);
        map.put("golden_dagger", GOLD);
        map.put("diamond_dagger", DIAMOND);
        DAGGER_TOOL_MATERIALS = Map.copyOf(map);
    }

    /**
     * Gets the repair tag for a given dagger name (e.g., "iron_dagger").
     * @param daggerName The full dagger name ("wooden_dagger", "iron_dagger", etc.)
     * @return The TagKey for the repair ingredient, or null if not found.
     */
    public static TagKey<Item> getRepairTag(String daggerName) {
        DaggerToolMaterial mat = DAGGER_TOOL_MATERIALS.get(daggerName);
        return mat != null ? mat.getRepairIngredientTag() : null;
    }
}