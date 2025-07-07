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

    // meele attack damage is + 1
    
    public static final DaggerToolMaterial WOOD = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_WOODEN_TOOL,
        30,  
        2.0F, // Wood mining speed
        1.0F, 
        15,   
        ItemTags.REPAIRS_WOODEN_DAGGER,
        3.0F, // Ranged damage
        1.4F  // Ranged velocity
    );


    public static final DaggerToolMaterial STONE = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        50,  
        2.0F, 
        2.0F, 
        12,    
        ItemTags.REPAIRS_STONE_DAGGER,
        5.0F, // Ranged damage
        1.6F  // Ranged velocity
    );

    public static final DaggerToolMaterial IRON = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        100,   
        2.0F, // 
        3.0F, // 
        11,     // steel will also be 11
        ItemTags.REPAIRS_IRON_DAGGER,
        7.0F, // Ranged damage
        1.8F // Ranged velocity
    );

    public static final DaggerToolMaterial GOLD = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        20,  
        2.0F, //
        1.0F, // 
        25,  // Copper will be 20
        ItemTags.REPAIRS_GOLDEN_DAGGER,
        3.0F, // Ranged damage
        1.6F  // Ranged velocity
    );

    public static final DaggerToolMaterial DIAMOND = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        250,   
        2.0F, // 
        4.0F, // 
        10,   
        ItemTags.REPAIRS_DIAMOND_DAGGER,
        8.0F, // Ranged damage
        2.0F  // Ranged velocity
    );

}