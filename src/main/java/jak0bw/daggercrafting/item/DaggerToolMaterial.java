package jak0bw.daggercrafting.item;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

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
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        59,   // 80% of wood sword durability (59)
        2.0F, // Wood mining speed
        0.0F, // Wood sword damage (matches vanilla)
        15,   // Wood enchantability  
        ItemTags.REPAIRS_WOODEN_DAGGER,
        4.0F, // Ranged damage
        1.0F  // Ranged velocity
    );

    public static final DaggerToolMaterial STONE = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        131,   // 80% of stone sword durability (131)
        2.0F, // Stone mining speed
        0.0F, // Stone sword damage (matches vanilla)
        15,   // Stone enchantability  
        ItemTags.REPAIRS_STONE_DAGGER,
        4.0F, // Ranged damage
        1.0F  // Ranged velocity
    );

    public static final DaggerToolMaterial GOLD = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        32,   // 80% of gold sword durability (32)
        2.0F, // Gold mining speed
        0.0F, // Gold sword damage (matches vanilla)
        15,   // Gold enchantability  
        ItemTags.REPAIRS_GOLDEN_DAGGER,
        4.0F, // Ranged damage
        1.0F  // Ranged velocity
    );

    public static final DaggerToolMaterial IRON = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        131,   // 80% of wood sword durability (59)
        2.0F, // Wood mining speed
        0.0F, // Wood sword damage (matches vanilla)
        15,   // Wood enchantability  
        ItemTags.REPAIRS_IRON_DAGGER,
        4.0F, // Ranged damage
        1.0F  // Ranged velocity
    );

    public static final DaggerToolMaterial DIAMOND = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        47,   // 80% of wood sword durability (59)
        2.0F, // Wood mining speed
        0.0F, // Wood sword damage (matches vanilla)
        15,   // Wood enchantability  
        ItemTags.REPAIRS_DIAMOND_DAGGER,
        4.0F, // Ranged damage
        1.0F  // Ranged velocity
    );

}