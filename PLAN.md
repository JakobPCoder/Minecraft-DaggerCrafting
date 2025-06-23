# DaggerCrafting Mod - Comprehensive Implementation Plan

## Project Overview

**Goal**: Create a throwable dagger system that combines melee and ranged combat capabilities with full enchantment support.

**Core Vision**: Daggers should feel like a unique weapon class that bridges the gap between swords and tridents, offering tactical flexibility while maintaining balanced gameplay.

## 🔧 **CORRECTED TECHNICAL APPROACH - FINAL**
- **Custom Material Class**: `DaggerToolMaterial` as standalone class (NOT extending ToolMaterial record)
- **Vanilla Compatibility**: Use vanilla damage values and sword logic for maximum compatibility  
- **Dual Damage System**: Melee uses vanilla sword formula, ranged uses explicit material values
- **Clean Separation**: Our custom material class handles all dagger-specific properties independently

## Feature Requirements

### Primary Features
1. **Dual-Purpose Combat System**
   - Melee attacks with sword-like mechanics (copied from SwordItem)
   - Throwable projectile with trident-like behavior (inherited from TridentItem)
   - Different damage values for melee vs ranged

2. **Dual-Mode Enchantment Support**
   - Melee enchantments: Sharpness, Fire Aspect, Knockback, Looting, Smite, Bane of Arthropods (via tags)
   - Ranged enchantments: Power, Punch, Flame (via bow enchantment tags)
   - Projectile enchantments: Loyalty only (via tags, selective approach)
   - Universal enchantments: Unbreaking, Mending, Curse of Vanishing (via tags)
   - Excluded enchantments: Piercing, Channeling, Infinity (too powerful/inappropriate)

3. **Material-Based Progression**
   - Uses vanilla ToolMaterial system
   - Each material has unique stats and enchantability
   - Repair ingredients per material

4. **Balanced Gameplay**
   - Melee damage: 80% of equivalent sword damage
   - Ranged damage: Explicit values per material (stored in DaggerToolMaterial)
   - Charge-up time for throwing (inherited from trident)
   - Cooldown after throwing (inherited from trident)

## Technical Architecture - REVISED

### Core Design Decision: Extend TridentItem
**Key Insight**: Instead of building complex projectile mechanics from scratch, we extend `TridentItem` and override melee behavior. This gives us:

- ✅ **Free Complex Features**: Throwing mechanics, charge-up, projectile behavior
- ✅ **Proven Stability**: Built on extensively tested vanilla code
- ✅ **Selective Enchantment Control**: All enchantments controlled via tags for precise selection
- ✅ **Minimal Code**: Only override melee-specific methods
- ✅ **Future-Proof**: Inherits any vanilla improvements to trident mechanics

### Core Classes
1. **DaggerToolMaterial** - Custom standalone material class with all dagger properties (melee damage, ranged damage, velocity, durability, etc.)
2. **DaggerToolMaterials** - Static instances of DaggerToolMaterial for all tiers (wood, stone, iron, gold, diamond, netherite)
3. **DaggerItem** - Main weapon class (extends TridentItem, uses DaggerToolMaterial for all properties)
4. **DaggerEntity** - Projectile entity (extends TridentEntity, uses ranged damage and velocity from DaggerToolMaterial)
5. **ModEntityTypes** - Entity type registrations
6. **ModItems** - Item registrations with DaggerToolMaterial integration

### Implementation Phases - REVISED

#### Phase 0: Core Registrations & Foundation
- Create ToolMaterial instances for all dagger tiers
- Register entity types (ModEntityTypes.java)
- Create basic DaggerItem extending TridentItem
- Register single iron dagger for testing
- Ensure basic throwing works

#### Phase 1: Melee Combat Integration  
- Override TridentItem methods for sword-like melee behavior
- Implement proper damage scaling (80% of sword damage)
- Add durability damage on hit (copied from SwordItem)
- Test melee combat mechanics

#### Phase 2: Ranged Damage System
- Create DaggerEntity extending TridentEntity
- Implement explicit ranged damage from DaggerToolMaterial + Power enchantment
- Implement Flame and Punch enchantment effects  
- Ensure all enchantments work for both melee and ranged modes
- Test loyalty mechanics (piercing excluded by design)

#### Phase 3: Multi-Material Support
- Register all material variants (wood, stone, iron, gold, diamond, netherite)
- Add to creative tabs
- Create comprehensive tag system for enchantment compatibility

#### Phase 4: Assets & Data Generation
- Model files (parent + material variants)
- Texture assets
- Recipe generation
- Tag generation
- Language files

## Core Implementation Details

### 1. DaggerToolMaterial System - FINAL CORRECTED APPROACH

**Key Insight**: Create custom standalone material class since `ToolMaterial` is a record and cannot be extended.

```java
// File: src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterial.java
public class DaggerToolMaterial {
    private final TagKey<Block> inverseTag;
    private final int durability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final TagKey<Item> repairIngredientTag;
    private final float rangedDamage;
    private final float rangedVelocity;
    
    public DaggerToolMaterial(TagKey<Block> inverseTag, int durability, float miningSpeed, 
                             float attackDamage, int enchantability, TagKey<Item> repairIngredientTag,
                             float rangedDamage, float rangedVelocity) {
        this.inverseTag = inverseTag;
        this.durability = durability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.repairIngredientTag = repairIngredientTag;
        this.rangedDamage = rangedDamage;
        this.rangedVelocity = rangedVelocity;
    }
    
    // All vanilla ToolMaterial methods
    public TagKey<Block> getInverseTag() { return inverseTag; }
    public int getDurability() { return durability; }
    public float getMiningSpeed() { return miningSpeed; }
    public float getAttackDamage() { return attackDamage; }
    public int getEnchantability() { return enchantability; }
    public TagKey<Item> getRepairIngredientTag() { return repairIngredientTag; }
    
    // Custom dagger-specific methods
    public float getRangedDamage() { return rangedDamage; }
    public float getRangedVelocity() { return rangedVelocity; }
}

// File: src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterials.java
public class DaggerToolMaterials {
    // 80% of vanilla sword durability, vanilla damage values, explicit ranged damage
    public static final DaggerToolMaterial WOODEN_DAGGER = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_WOODEN_TOOL,
        47,   // 80% of wood sword durability (59)
        2.0F, // Wood mining speed
        0.0F, // Wood sword damage (matches vanilla)
        15,   // Wood enchantability  
        DaggerCraftingItemTags.REPAIRS_WOODEN_DAGGER,
        4.0F, // Ranged damage
        1.0F  // Ranged velocity
    );
    
    public static final DaggerToolMaterial STONE_DAGGER = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        104,  // 80% of stone sword durability (131)
        4.0F, // Stone mining speed
        1.0F, // Stone sword damage (matches vanilla)
        5,    // Stone enchantability
        DaggerCraftingItemTags.REPAIRS_STONE_DAGGER,
        5.0F, // Ranged damage
        1.0F  // Ranged velocity
    );
    
    public static final DaggerToolMaterial IRON_DAGGER = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_IRON_TOOL,
        200,  // 80% of iron sword durability (250)
        6.0F, // Iron mining speed
        2.0F, // Iron sword damage (matches vanilla)
        14,   // Iron enchantability
        DaggerCraftingItemTags.REPAIRS_IRON_DAGGER,
        6.0F, // Ranged damage
        1.0F  // Ranged velocity
    );
    
    public static final DaggerToolMaterial GOLDEN_DAGGER = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_GOLD_TOOL,
        25,   // 80% of gold sword durability (32)
        12.0F, // Gold mining speed
        0.0F, // Gold sword damage (matches vanilla)
        22,   // Gold enchantability
        DaggerCraftingItemTags.REPAIRS_GOLDEN_DAGGER,
        4.0F, // Ranged damage (same as wood)
        1.2F  // Ranged velocity (faster)
    );
    
    public static final DaggerToolMaterial DIAMOND_DAGGER = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        1248, // 80% of diamond sword durability (1561)
        8.0F, // Diamond mining speed
        3.0F, // Diamond sword damage (matches vanilla)
        10,   // Diamond enchantability
        DaggerCraftingItemTags.REPAIRS_DIAMOND_DAGGER,
        7.0F, // Ranged damage
        1.0F  // Ranged velocity
    );
    
    public static final DaggerToolMaterial NETHERITE_DAGGER = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
        1644, // 80% of netherite sword durability (2031)
        9.0F, // Netherite mining speed
        4.0F, // Netherite sword damage (matches vanilla)
        15,   // Netherite enchantability
        DaggerCraftingItemTags.REPAIRS_NETHERITE_DAGGER,
        8.0F, // Ranged damage
        1.0F  // Ranged velocity
    );
}
```

### 2. DaggerItem Class - FINAL CORRECTED APPROACH

```java
public class DaggerItem extends TridentItem {
    private final DaggerToolMaterial daggerMaterial;
    
    public DaggerItem(DaggerToolMaterial material, Item.Settings settings) {
        super(settings
            .component(DataComponentTypes.TOOL, createDaggerToolComponent(material))
            .component(DataComponentTypes.ATTRIBUTE_MODIFIERS, createDaggerAttributes(material)));
        this.daggerMaterial = material;
    }
    
    // ===== SWORD-LIKE MELEE BEHAVIOR (copied from SwordItem) =====
    
    @Override
    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return !miner.isCreative(); // Same as SwordItem
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true; // Enable weapon durability damage
    }
    
    @Override  
    public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, EquipmentSlot.MAINHAND); // Damage weapon on hit
    }
    
    // ===== VANILLA-COMPATIBLE DAMAGE SYSTEM =====
    
    private static ToolComponent createDaggerToolComponent(DaggerToolMaterial material) {
        return new ToolComponent(List.of(), 1.0F, material.getDurability() > 0 ? 1 : 0);
    }
    
    private static AttributeModifiersComponent createDaggerAttributes(DaggerToolMaterial material) {
        // Use vanilla damage calculation - 80% of equivalent sword
        // Sword damage = material.getAttackDamage() + 3.0F (base sword damage)
        float swordDamage = material.getAttackDamage() + 3.0F;
        float daggerMeleeDamage = swordDamage * 0.8F - 1.0F; // -1 because base entity damage is 1.0F
        
        return AttributeModifiersComponent.builder()
            .add(EntityAttributes.ATTACK_DAMAGE, 
                new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, daggerMeleeDamage, Operation.ADD_VALUE), 
                AttributeModifierSlot.MAINHAND)
            .add(EntityAttributes.ATTACK_SPEED, 
                new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, -2.0, Operation.ADD_VALUE), // Faster than sword (-2.4)
                AttributeModifierSlot.MAINHAND)
            .build();
    }
    
    // ===== PROJECTILE CREATION OVERRIDE =====
    
    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        DaggerEntity dagger = new DaggerEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
        dagger.pickupType = PickupPermission.ALLOWED;
        return dagger;
    }
    
    // ===== GETTER FOR RANGED DAMAGE =====
    
    public DaggerToolMaterial getDaggerMaterial() {
        return this.daggerMaterial;
    }
    
    // Note: We don't override getMaterial() since our DaggerToolMaterial is not a ToolMaterial
    // Instead, we provide our own getter method for accessing material properties
}
```

### 3. DaggerEntity Class - FINAL CORRECTED APPROACH
```java
public class DaggerEntity extends TridentEntity {
    
    public DaggerEntity(EntityType<? extends DaggerEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public DaggerEntity(World world, LivingEntity owner, ItemStack stack) {
        super(ModEntityTypes.DAGGER, owner, world, stack, null);
    }
    
    public DaggerEntity(World world, double x, double y, double z, ItemStack stack) {
        super(ModEntityTypes.DAGGER, x, y, z, world, stack, stack);
    }
    
    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        
        // Use explicit ranged damage from DaggerToolMaterial + Power enchantment
        ItemStack weaponStack = this.getWeaponStack();
        if (weaponStack.getItem() instanceof DaggerItem daggerItem) {
            // Get base ranged damage directly from custom material
            DaggerToolMaterial material = daggerItem.getDaggerMaterial();
            float rangedDamage = material.getRangedDamage();
            
            // Apply Power enchantment (increases ranged damage)
            int powerLevel = EnchantmentHelper.getLevel(Enchantments.POWER, weaponStack);
            if (powerLevel > 0) {
                rangedDamage += (float)powerLevel * 0.5F + 0.5F; // Same formula as arrows
            }
            
            this.setDamage(rangedDamage);
        }
        
        super.onEntityHit(entityHitResult); // Handle rest with vanilla trident logic
        
        // Apply Flame enchantment (sets target on fire)
        if (entity instanceof LivingEntity target) {
            int flameLevel = EnchantmentHelper.getLevel(Enchantments.FLAME, weaponStack);
            if (flameLevel > 0) {
                target.setOnFireFor(flameLevel * 4); // Same duration as flame arrows
            }
            
            // Apply Punch enchantment (increases knockback)
            int punchLevel = EnchantmentHelper.getLevel(Enchantments.PUNCH, weaponStack);
            if (punchLevel > 0) {
                Vec3d knockback = this.getVelocity().multiply(1.0, 0.0, 1.0).normalize().multiply(punchLevel * 0.6);
                target.addVelocity(knockback.x, 0.1, knockback.z);
            }
        }
    }

    
    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(ModItems.IRON_DAGGER); // Default fallback
    }
}
```

### 4. ModEntityTypes Class
```java
public class ModEntityTypes {
    public static final EntityType<DaggerEntity> DAGGER = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(DaggerCrafting.MOD_ID, "dagger"),
        EntityType.Builder.<DaggerEntity>create(DaggerEntity::new, SpawnGroup.MISC)
            .dimensions(0.5f, 0.5f)
            .maxTrackingRange(8)
            .trackingTickInterval(20)
            .build()
    );
    
    public static void initialize() {
        DaggerCrafting.LOGGER.info("Registering entity types for " + DaggerCrafting.MOD_ID);
    }
}
```

### 5. Enhanced ModItems Class - FINAL CORRECTED APPROACH
```java
public class ModItems {
    // Register all dagger variants with DaggerToolMaterial (ordered by tier following customarrows rule)
    public static final Item WOODEN_DAGGER = registerItem("wooden_dagger", 
        DaggerItem::new, new Item.Settings(), DaggerToolMaterials.WOODEN_DAGGER);
    
    public static final Item STONE_DAGGER = registerItem("stone_dagger",
        DaggerItem::new, new Item.Settings(), DaggerToolMaterials.STONE_DAGGER);
        
    public static final Item IRON_DAGGER = registerItem("iron_dagger",
        DaggerItem::new, new Item.Settings(), DaggerToolMaterials.IRON_DAGGER);
        
    public static final Item GOLDEN_DAGGER = registerItem("golden_dagger",
        DaggerItem::new, new Item.Settings(), DaggerToolMaterials.GOLDEN_DAGGER);
        
    public static final Item DIAMOND_DAGGER = registerItem("diamond_dagger", 
        DaggerItem::new, new Item.Settings(), DaggerToolMaterials.DIAMOND_DAGGER);
        
    public static final Item NETHERITE_DAGGER = registerItem("netherite_dagger",
        DaggerItem::new, new Item.Settings(), DaggerToolMaterials.NETHERITE_DAGGER);
    
    private static Item registerItem(String name, BiFunction<DaggerToolMaterial, Item.Settings, Item> factory, 
                                   Item.Settings settings, DaggerToolMaterial material) {
        Identifier id = Identifier.of(DaggerCrafting.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        return Items.register(key, s -> factory.apply(material, s), settings);
    }
    
    public static void registerModItems() {
        // Add to combat creative tab ordered by tier (following customarrows rule)
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(WOODEN_DAGGER);
            entries.add(STONE_DAGGER);
            entries.add(IRON_DAGGER);
            entries.add(GOLDEN_DAGGER);
            entries.add(DIAMOND_DAGGER);
            entries.add(NETHERITE_DAGGER);
        });
    }
}
```

## Required JSON Files & Assets

### 1. Item Models Structure

#### Parent Model File
**Location**: `src/main/resources/assets/daggercrafting/models/item/dagger_base.json`
```json
{
  "parent": "minecraft:item/handheld",
  "textures": {
    "layer0": "#texture"
  },
  "display": {
    "thirdperson_righthand": {
      "rotation": [-90, 0, 0],
      "translation": [0, 1, -3],
      "scale": [0.55, 0.55, 0.55]
    },
    "thirdperson_lefthand": {
      "rotation": [-90, 0, 0],
      "translation": [0, 1, -3],
      "scale": [0.55, 0.55, 0.55]
    },
    "firstperson_righthand": {
      "rotation": [0, -90, 25],
      "translation": [1.13, 3.2, 1.13],
      "scale": [0.68, 0.68, 0.68]
    },
    "firstperson_lefthand": {
      "rotation": [0, 90, -25],
      "translation": [1.13, 3.2, 1.13],
      "scale": [0.68, 0.68, 0.68]
    },
    "gui": {
      "rotation": [0, 0, 0],
      "translation": [0, 0, 0],
      "scale": [1, 1, 1]
    },
    "ground": {
      "rotation": [0, 0, 0],
      "translation": [0, 2, 0],
      "scale": [0.5, 0.5, 0.5]
    },
    "fixed": {
      "rotation": [0, 180, 0],
      "translation": [0, 0, 0],
      "scale": [1, 1, 1]
    }
  }
}
```

#### Material-Specific Models
For each material (wood, stone, iron, gold, diamond, netherite):
**Location**: `src/main/resources/assets/daggercrafting/models/item/{material}_dagger.json`
```json
{
  "parent": "daggercrafting:item/dagger_base",
  "textures": {
    "texture": "daggercrafting:item/{material}_dagger"
  }
}
```

#### Item Model Definitions (1.21.4+)
**Location**: `src/main/resources/assets/daggercrafting/items/{material}_dagger.json`
```json
{
  "model": {
    "type": "model",
    "model": "daggercrafting:item/{material}_dagger"
  }
}
```

### 2. Dual-Mode Enchantment Strategy

**Key Insight**: Extending `TridentItem` gives us throwing mechanics, but ALL enchantments are controlled via tags for precise dual-mode combat:
- ✅ **Melee Combat**: Sword enchantments (Sharpness, Fire Aspect, Knockback, Looting, Smite, Bane of Arthropods)
- ✅ **Ranged Combat**: Bow enchantments (Power, Punch, Flame) 
- ✅ **Projectile Mechanics**: Loyalty (throwing return mechanic)
- ✅ **Universal**: Unbreaking, Mending, Curse of Vanishing
- ❌ **Excluded**: Piercing, Channeling, Infinity (too powerful/inappropriate)

#### Dual-Mode Enchantment Tags

#### Weapon Tags (for Sharpness, etc.)
**Location**: `src/main/generated/data/minecraft/tags/item/weapon_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Sword Tags (for Fire Aspect, Knockback, Looting, etc.)
**Location**: `src/main/generated/data/minecraft/tags/item/sword_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Sharp Weapon Tags (for Sharpness, Smite, Bane of Arthropods)
**Location**: `src/main/generated/data/minecraft/tags/item/sharp_weapon_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Loyalty Tags (Selective - ONLY this trident enchantment)
**Location**: `src/main/generated/data/minecraft/tags/item/loyalty_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Fire Aspect Tags
**Location**: `src/main/generated/data/minecraft/tags/item/fire_aspect_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Looting Tags
**Location**: `src/main/generated/data/minecraft/tags/item/looting_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Durability Tags (Unbreaking, Mending)
**Location**: `src/main/generated/data/minecraft/tags/item/durability_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Vanishing Curse Tags
**Location**: `src/main/generated/data/minecraft/tags/item/vanishing_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Bow Enchantment Tags (Ranged Combat)

#### Power Tags (Increases ranged damage)
**Location**: `src/main/generated/data/minecraft/tags/item/power_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Punch Tags (Increases ranged knockback)
**Location**: `src/main/generated/data/minecraft/tags/item/punch_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

#### Flame Tags (Sets ranged targets on fire)
**Location**: `src/main/generated/data/minecraft/tags/item/flame_enchantable.json`
```json
{
  "replace": false,
  "values": [
    "#daggercrafting:daggers"
  ]
}
```

**Important**: We do NOT add daggers to:
- `trident_enchantable` (would give Channeling - too powerful)
- `piercing_enchantable` (would give Piercing - potentially too powerful)  
- `riptide_enchantable` (trident-only mechanic)
- `infinity_enchantable` (conflicts with Loyalty return mechanic)

### 3. Custom Tags

#### Daggers Tag
**Location**: `src/main/generated/data/daggercrafting/tags/item/daggers.json`
```json
{
  "replace": false,
  "values": [
    "daggercrafting:wooden_dagger",
    "daggercrafting:stone_dagger",
    "daggercrafting:iron_dagger",
    "daggercrafting:golden_dagger",
    "daggercrafting:diamond_dagger",
    "daggercrafting:netherite_dagger"
  ]
}
```

### 4. Recipe Files

#### Shaped Crafting Recipes
**Location**: `src/main/generated/data/daggercrafting/recipe/{material}_dagger.json`

Example for Iron Dagger:
```json
{
  "type": "minecraft:crafting_shaped",
  "category": "equipment",
  "pattern": [
    " I ",
    " I ",
    " S "
  ],
  "key": {
    "I": {
      "item": "minecraft:iron_ingot"
    },
    "S": {
      "item": "minecraft:stick"
    }
  },
  "result": {
    "count": 1,
    "item": "daggercrafting:iron_dagger"
  }
}
```

#### Smithing Recipes
**Location**: `src/main/generated/data/daggercrafting/recipe/netherite_dagger_smithing.json`
```json
{
  "type": "minecraft:smithing_transform",
  "addition": {
    "item": "minecraft:netherite_ingot"
  },
  "base": {
    "item": "daggercrafting:diamond_dagger"
  },
  "result": {
    "item": "daggercrafting:netherite_dagger"
  },
  "template": {
    "item": "minecraft:netherite_upgrade_smithing_template"
  }
}
```

### 5. Loot Tables

#### Block Drop Loot Tables
**Location**: `src/main/generated/data/daggercrafting/loot_table/block/{material}_dagger.json`
```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ],
      "entries": [
        {
          "type": "minecraft:item",
          "name": "daggercrafting:{material}_dagger"
        }
      ],
      "rolls": 1.0
    }
  ]
}
```

### 7. Language Files

#### English Translations
**Location**: `src/main/generated/data/daggercrafting/lang/en_us.json`
```json
{
  "item.daggercrafting.wooden_dagger": "Wooden Dagger",
  "item.daggercrafting.stone_dagger": "Stone Dagger",
  "item.daggercrafting.iron_dagger": "Iron Dagger",
  "item.daggercrafting.golden_dagger": "Golden Dagger",
  "item.daggercrafting.diamond_dagger": "Diamond Dagger",
  "item.daggercrafting.netherite_dagger": "Netherite Dagger",
  
  "advancement.daggercrafting.root.title": "Dagger Mastery",
  "advancement.daggercrafting.root.description": "Master the art of dagger combat",
  "advancement.daggercrafting.first_throw.title": "First Blood",
  "advancement.daggercrafting.first_throw.description": "Throw your first dagger",
  "advancement.daggercrafting.loyalty_return.title": "Boomerang",
  "advancement.daggercrafting.loyalty_return.description": "Have a dagger return to you with Loyalty",
  "advancement.daggercrafting.precision_kill.title": "Precision Strike",
  "advancement.daggercrafting.precision_kill.description": "Kill an enemy from a distance with a thrown dagger"
}
```

## Data Generation Implementation

### Tag Generation Provider
```java
public class DaggerTagProvider extends FabricTagProvider<Item> {
    public DaggerTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.ITEM, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup lookup) {
        // Custom dagger tag
        getOrCreateTagBuilder(ModTags.DAGGERS)
            .add(ModItems.WOODEN_DAGGER)
            .add(ModItems.STONE_DAGGER)
            .add(ModItems.IRON_DAGGER)
            .add(ModItems.GOLDEN_DAGGER)
            .add(ModItems.DIAMOND_DAGGER)
            .add(ModItems.NETHERITE_DAGGER);

        // SELECTIVE enchantment support - only the ones we want
        getOrCreateTagBuilder(ItemTags.WEAPON_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);

        getOrCreateTagBuilder(ItemTags.SWORD_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);

        getOrCreateTagBuilder(ItemTags.SHARP_WEAPON_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);
            
        // Only Loyalty from trident enchantments
        getOrCreateTagBuilder(ItemTags.LOYALTY_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);
            
        getOrCreateTagBuilder(ItemTags.FIRE_ASPECT_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);
            
        getOrCreateTagBuilder(ItemTags.LOOTING_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);

        getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);

        getOrCreateTagBuilder(ItemTags.VANISHING_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);
            
        // Bow enchantments for ranged combat
        getOrCreateTagBuilder(ItemTags.POWER_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);
            
        getOrCreateTagBuilder(ItemTags.PUNCH_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);
            
        getOrCreateTagBuilder(ItemTags.FLAME_ENCHANTABLE)
            .addTag(ModTags.DAGGERS);
            
        // IMPORTANT: We do NOT add to these tags:
        // - ItemTags.TRIDENT_ENCHANTABLE (would give Channeling)
        // - ItemTags.PIERCING_ENCHANTABLE (would give Piercing)
        // - ItemTags.RIPTIDE_ENCHANTABLE (trident-only)
        // - ItemTags.INFINITY_ENCHANTABLE (conflicts with Loyalty)
    }
}
```

### Recipe Generation Provider
```java
public class DaggerRecipeProvider extends FabricRecipeProvider {
    public DaggerRecipeProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void generate(RecipeExporter exporter) {
        // Wooden Dagger
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.WOODEN_DAGGER)
            .pattern(" W ")
            .pattern(" W ")
            .pattern(" S ")
            .input('W', Items.WOODEN_PLANKS)
            .input('S', Items.STICK)
            .criterion(hasItem(Items.WOODEN_PLANKS), conditionsFromItem(Items.WOODEN_PLANKS))
            .offerTo(exporter);

        // Stone Dagger
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.STONE_DAGGER)
            .pattern(" C ")
            .pattern(" C ")
            .pattern(" S ")
            .input('C', Items.COBBLESTONE)
            .input('S', Items.STICK)
            .criterion(hasItem(Items.COBBLESTONE), conditionsFromItem(Items.COBBLESTONE))
            .offerTo(exporter);

        // Iron Dagger
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.IRON_DAGGER)
            .pattern(" I ")
            .pattern(" I ")
            .pattern(" S ")
            .input('I', Items.IRON_INGOT)
            .input('S', Items.STICK)
            .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
            .offerTo(exporter);

        // Golden Dagger
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.GOLDEN_DAGGER)
            .pattern(" G ")
            .pattern(" G ")
            .pattern(" S ")
            .input('G', Items.GOLD_INGOT)
            .input('S', Items.STICK)
            .criterion(hasItem(Items.GOLD_INGOT), conditionsFromItem(Items.GOLD_INGOT))
            .offerTo(exporter);

        // Diamond Dagger
        ShapedRecipeJsonBuilder.create(RecipeCategory.COMBAT, ModItems.DIAMOND_DAGGER)
            .pattern(" D ")
            .pattern(" D ")
            .pattern(" S ")
            .input('D', Items.DIAMOND)
            .input('S', Items.STICK)
            .criterion(hasItem(Items.DIAMOND), conditionsFromItem(Items.DIAMOND))
            .offerTo(exporter);

        // Netherite Dagger (Smithing)
        SmithingTransformRecipeJsonBuilder.create(
            Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            Ingredient.ofItems(ModItems.DIAMOND_DAGGER),
            Ingredient.ofItems(Items.NETHERITE_INGOT),
            RecipeCategory.COMBAT,
            ModItems.NETHERITE_DAGGER
        )
        .criterion(hasItem(Items.NETHERITE_INGOT), conditionsFromItem(Items.NETHERITE_INGOT))
        .offerTo(exporter, getRecipeName(ModItems.NETHERITE_DAGGER) + "_smithing");
    }
}
```

### Model Generation Provider
```java
public class DaggerModelProvider extends FabricModelProvider {
    public DaggerModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        // No blocks to generate
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        // Generate handheld models for all daggers
        itemModelGenerator.register(ModItems.WOODEN_DAGGER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.STONE_DAGGER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.IRON_DAGGER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.GOLDEN_DAGGER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.DIAMOND_DAGGER, Models.HANDHELD);
        itemModelGenerator.register(ModItems.NETHERITE_DAGGER, Models.HANDHELD);
    }
}
```

## Performance Considerations

1. **Entity Tracking**: Limit number of active dagger entities
2. **Loyalty Pathfinding**: Efficient return trajectory calculation
3. **Entity Cleanup**: Proper despawning of stuck/lost daggers
4. **Rendering**: LOD system for distant projectiles

## Testing Strategy

1. **Unit Tests**: Material properties, damage calculations
2. **Integration Tests**: Enchantment interactions, loyalty behavior
3. **Performance Tests**: Multiple projectiles, large-scale combat
4. **Compatibility Tests**: With other mods, vanilla mechanics

## Balancing Framework

### Damage Scaling - CORRECTED APPROACH
- **Melee Damage**: `melee_damage = (material.getAttackDamage() + 3.0F) * 0.8F` (80% of equivalent sword)
  - Wood: (0.0 + 3.0) * 0.8 = 2.4F damage
  - Stone: (1.0 + 3.0) * 0.8 = 3.2F damage  
  - Iron: (2.0 + 3.0) * 0.8 = 4.0F damage
  - Gold: (0.0 + 3.0) * 0.8 = 2.4F damage
  - Diamond: (3.0 + 3.0) * 0.8 = 4.8F damage
  - Netherite: (4.0 + 3.0) * 0.8 = 5.6F damage
- **Ranged Damage**: `ranged_damage = material.getRangedDamage()` (explicit values from DaggerToolMaterial)
  - Wood: 4.0F, Stone: 5.0F, Iron: 6.0F, Gold: 4.0F, Diamond: 7.0F, Netherite: 8.0F  
- **Enchantment Scaling**: 
  - Melee: Sharpness, Fire Aspect, Knockback, Looting, Smite, Bane of Arthropods
  - Ranged: Power (+0.5F + 0.5F per level), Flame (fire duration), Punch (knockback)

### Throwing Mechanics
- **Charge Time**: 1.5 seconds for maximum distance
- **Range**: 24 blocks maximum
- **Accuracy**: Decreases with distance and charge time
- **Cooldown**: 0.5 seconds after throwing

### Loyalty Behavior
- **Return Speed**: 2x throw speed
- **Return Accuracy**: Always returns to thrower
- **Damage on Return**: No damage dealt during return

## File Structure Summary

```
src/main/
├── java/jak0bw/daggercrafting/
│   ├── item/
│   │   ├── DaggerMaterial.java (enhanced)
│   │   └── DaggerItem.java
│   ├── entity/
│   │   ├── DaggerEntity.java
│   │   └── DaggerRenderer.java
│   ├── datagen/
│   │   ├── DaggerTagProvider.java
│   │   ├── DaggerRecipeProvider.java
│   │   ├── DaggerModelProvider.java
│   │   └── DaggerLootTableProvider.java
│   └── ModTags.java
├── resources/assets/daggercrafting/
│   ├── models/item/
│   │   ├── dagger_base.json (parent model)
│   │   ├── wooden_dagger.json
│   │   ├── stone_dagger.json
│   │   ├── iron_dagger.json
│   │   ├── golden_dagger.json
│   │   ├── diamond_dagger.json
│   │   └── netherite_dagger.json
│   ├── items/ (1.21.4+)
│   │   ├── wooden_dagger.json
│   │   ├── stone_dagger.json
│   │   ├── iron_dagger.json
│   │   ├── golden_dagger.json
│   │   ├── diamond_dagger.json
│   │   └── netherite_dagger.json
│   └── textures/item/
│       ├── wooden_dagger.png
│       ├── stone_dagger.png
│       ├── iron_dagger.png
│       ├── golden_dagger.png
│       ├── diamond_dagger.png
│       └── netherite_dagger.png
└── generated/data/
    ├── minecraft/tags/item/
    │   ├── weapon_enchantable.json
    │   ├── sword_enchantable.json
    │   ├── sharp_weapon_enchantable.json
    │   ├── durability_enchantable.json
    │   └── vanishing_enchantable.json
    ├── daggercrafting/
    │   ├── tags/item/daggers.json
    │   ├── recipes/
    │   ├── loot_tables/
    │   ├── advancements/
    │   └── lang/en_us.json
    └── fabric/tags/item/
        └── enchantable/
            └── weapon.json
```

## Updated File Structure Summary - FINAL CORRECTED APPROACH

```
src/main/
├── java/jak0bw/daggercrafting/
│   ├── item/
│   │   ├── DaggerToolMaterial.java (custom standalone material class with all properties)
│   │   ├── DaggerToolMaterials.java (static DaggerToolMaterial instances)
│   │   └── DaggerItem.java (extends TridentItem, uses DaggerToolMaterial)
│   ├── entity/
│   │   └── DaggerEntity.java (extends TridentEntity, uses ranged damage and velocity from material)
│   ├── datagen/
│   │   ├── DaggerTagProvider.java
│   │   ├── DaggerRecipeProvider.java
│   │   ├── DaggerModelProvider.java
│   │   └── DaggerLootTableProvider.java
│   ├── ModEntityTypes.java (entity registrations)
│   ├── ModItems.java (DaggerToolMaterial integration)
│   ├── ModTags.java (custom tags)
│   └── DaggerCraftingItemTags.java (repair ingredient tags)
├── resources/assets/daggercrafting/
│   ├── models/item/
│   │   ├── dagger_base.json (parent model)
│   │   ├── wooden_dagger.json
│   │   ├── stone_dagger.json
│   │   ├── iron_dagger.json
│   │   ├── golden_dagger.json
│   │   ├── diamond_dagger.json
│   │   └── netherite_dagger.json
│   ├── items/ (1.21.4+)
│   │   ├── wooden_dagger.json
│   │   ├── stone_dagger.json
│   │   ├── iron_dagger.json
│   │   ├── golden_dagger.json
│   │   ├── diamond_dagger.json
│   │   └── netherite_dagger.json
│   └── textures/item/
│       ├── wooden_dagger.png
│       ├── stone_dagger.png
│       ├── iron_dagger.png
│       ├── golden_dagger.png
│       ├── diamond_dagger.png
│       └── netherite_dagger.png
└── generated/data/
    ├── minecraft/tags/item/
    │   ├── weapon_enchantable.json
    │   ├── sword_enchantable.json
    │   ├── sharp_weapon_enchantable.json
    │   ├── loyalty_enchantable.json
    │   ├── fire_aspect_enchantable.json
    │   ├── looting_enchantable.json
    │   ├── durability_enchantable.json
    │   ├── vanishing_enchantable.json
    │   ├── power_enchantable.json
    │   ├── punch_enchantable.json
    │   └── flame_enchantable.json
    ├── daggercrafting/
    │   ├── tags/item/daggers.json
    │   ├── recipes/ (shaped + smithing)
    │   ├── loot_tables/
    │   ├── advancements/
    │   └── lang/en_us.json
    └── fabric/tags/item/
        └── enchantable/
            └── weapon.json
```

This comprehensive plan leverages vanilla trident mechanics for maximum stability while providing the flexibility of sword-like melee combat. The TridentItem extension approach ensures robust, maintainable code with minimal custom complexity.

## Detailed Implementation Roadmap

### Phase 0: Complete Foundation & All Registrations (First Priority)

#### Step 0.1: Create DaggerToolMaterial System - FINAL CORRECTED
```java
// File: src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterial.java
// Create custom standalone material class (NOT extending ToolMaterial record)
// Include all vanilla ToolMaterial properties plus ranged damage and velocity
// Add all necessary getter methods for vanilla compatibility
// Add getRangedDamage() and getRangedVelocity() methods for dagger-specific features

// File: src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterials.java
// Create DaggerToolMaterial static instances for all tiers
// Use vanilla damage values (0.0F, 1.0F, 2.0F, 0.0F, 3.0F, 4.0F)
// 80% of vanilla sword durability per tier
// Explicit ranged damage values per tier (4.0F, 5.0F, 6.0F, 4.0F, 7.0F, 8.0F)
// Add ranged velocity values per tier (1.0F base, 1.2F for golden for speed)
```

#### Step 0.2: Create All Tag Classes
```java
// File: src/main/java/jak0bw/daggercrafting/ModTags.java
// Custom dagger tags (DAGGERS tag)
// File: src/main/java/jak0bw/daggercrafting/DaggerCraftingItemTags.java
// Repair ingredient tags for all materials
// REPAIRS_WOODEN_DAGGER, REPAIRS_STONE_DAGGER, etc.
```

#### Step 0.3: Create DaggerItem & DaggerEntity - FINAL CORRECTED
```java
// File: src/main/java/jak0bw/daggercrafting/item/DaggerItem.java
// Extend TridentItem with DaggerToolMaterial parameter (custom standalone class)
// Use vanilla damage calculation: (material.getAttackDamage() + 3.0F) * 0.8F
// Override postHit() and postDamageEntity() for sword-like durability damage
// Add getDaggerMaterial() method returning DaggerToolMaterial
// Don't override getMaterial() since DaggerToolMaterial is not a ToolMaterial

// File: src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java
// Extend TridentEntity with ModEntityTypes.DAGGER entity type
// Override onEntityHit() to use material.getRangedDamage() directly
// Apply Power enchantment to ranged damage (not melee damage)
// Implement Flame and Punch enchantment effects for ranged combat
// Use material.getRangedVelocity() if compatible with vanilla trident velocity system
```

#### Step 0.4: Register Entity Types
```java
// File: src/main/java/jak0bw/daggercrafting/ModEntityTypes.java
// Register DaggerEntity type with proper dimensions and tracking
// Call ModEntityTypes.initialize() in main mod class
```

#### Step 0.5: Register All Items
```java
// File: src/main/java/jak0bw/daggercrafting/ModItems.java
// Register ALL dagger variants (wood, stone, iron, gold, diamond, netherite)
// Add all to combat creative tab
// Implement registerItem() helper method
// Call ModItems.registerModItems() in main mod class
```

#### Step 0.6: Setup Data Generation
```java
// File: src/main/java/jak0bw/daggercrafting/datagen/DaggerTagProvider.java
// Configure all enchantment tags (sword, bow, loyalty, universal)
// Add daggers to appropriate vanilla enchantment tags

// File: src/main/java/jak0bw/daggercrafting/datagen/DaggerRecipeProvider.java
// Generate shaped crafting recipes for all materials
// Generate netherite smithing recipe

// File: src/main/java/jak0bw/daggercrafting/datagen/DaggerModelProvider.java
// Generate handheld models for all daggers

// File: src/main/java/jak0bw/daggercrafting/datagen/DaggerLootTableProvider.java
// Generate item loot tables for all daggers
```

#### Step 0.7: Main Mod Class Integration
```java
// File: src/main/java/jak0bw/daggercrafting/DaggerCrafting.java
// Call ModEntityTypes.initialize()
// Call ModItems.registerModItems()
// Register all data generation providers
// Setup proper mod initialization order
```

#### Step 0.8: Create Essential Assets
```
// Create basic texture placeholders for all materials
// src/main/resources/assets/daggercrafting/textures/item/
//   - wooden_dagger.png, stone_dagger.png, iron_dagger.png
//   - golden_dagger.png, diamond_dagger.png, netherite_dagger.png

// Create base model file
// src/main/resources/assets/daggercrafting/models/item/dagger_base.json

// Create basic language file
// src/main/resources/assets/daggercrafting/lang/en_us.json
```

#### Step 0.9: Initial Testing
- Run data generation to create all tags and recipes
- Give all dagger variants via `/give` command
- Test throwing mechanics work for all materials
- Test basic melee combat with different materials  
- Verify all daggers appear in creative tab
- Verify loyalty enchantment works on all variants

---

### Phase 1: Melee Combat Integration

#### Step 1.1: Implement Sword-like Damage System
```java
// In DaggerItem.java
// Implement createDaggerAttributes() method
// Calculate 80% of sword damage
// Set attack speed to -2.0 (faster than sword)
```

#### Step 1.2: Add Tool Component
```java
// Implement createDaggerToolComponent() method
// Enable proper durability behavior
```

#### Step 1.3: Test Melee Combat
- Test damage values against different entities
- Verify durability decreases on hit
- Test enchanting with sword enchantments

---

### Phase 2: Ranged Damage System

#### Step 2.1: Create DaggerEntity
```java
// File: src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java
// Extend TridentEntity
// Override onEntityHit() for explicit ranged damage from DaggerToolMaterial + Power enchantment
// Implement Flame and Punch enchantment effects
// Use material.getRangedDamage() for base ranged damage
```

#### Step 2.2: Update DaggerItem Projectile Creation
```java
// Override createEntity() in DaggerItem
// Return DaggerEntity instead of TridentEntity
```

#### Step 2.3: Update Entity Registration
```java
// Update ModEntityTypes to use DaggerEntity
// Update DaggerEntity constructors to use correct entity type
```

#### Step 2.4: Test Ranged Combat
- Test thrown damage uses explicit ranged damage values from material
- Test Power enchantment increases ranged damage correctly
- Test Flame enchantment sets targets on fire  
- Test Punch enchantment increases knockback
- Verify loyalty enchantment works
- Confirm piercing and infinity are properly excluded

---

### Phase 3: Enchantment System & Advanced Features

#### Step 3.1: Verify Enchantment Compatibility
```java
// Test all sword enchantments work (Sharpness, Fire Aspect, Knockback, etc.)
// Test all bow enchantments work (Power, Punch, Flame)
// Test loyalty enchantment works correctly
// Verify excluded enchantments don't appear (Piercing, Channeling, Infinity)
```

#### Step 3.2: Advanced Enchantment Effects
```java
// Test dual-mode enchantment scenarios:
// - Fire Aspect (melee) + Flame (ranged) on same dagger
// - Knockback (melee) + Punch (ranged) combinations
// - Sharpness vs Power damage scaling
// - Looting enchantment with thrown kills
```

#### Step 3.3: Material Progression Testing
```java
// Verify damage progression feels right across materials
// Test durability progression
// Test enchantability differences
// Verify repair ingredients work correctly
```

#### Step 3.4: Recipe & Tag Validation
- Test all crafting recipes work correctly
- Test netherite smithing upgrade
- Verify all enchantment tags are working
- Test repair functionality with correct ingredients

---

### Phase 4: Professional Assets & Polish

#### Step 4.1: High-Quality Textures
```
// Design professional dagger textures for all materials
// Ensure consistent art style across all tiers
// Add proper highlights, shadows, and material characteristics
// Create 16x16 textures that fit vanilla style
```

#### Step 4.2: Advanced Models & Display
```java
// Fine-tune dagger_base.json for optimal display
// Adjust rotation, translation, and scale for all views
// Create proper GUI display settings
// Test in-hand, on-ground, and item frame appearances
```

#### Step 4.3: Complete Language Support
```json
// Comprehensive language files with flavor text
// Item tooltips and descriptions
// Advancement titles and descriptions
// Error messages and feedback text
```

#### Step 4.4: Advancement System
```java
// Create advancement tree for dagger mastery
// "First Blood" - throw first dagger
// "Boomerang" - retrieve with loyalty
// "Dual Wielder" - use both melee and ranged in combat
// "Master Crafter" - craft all dagger tiers
```

#### Step 4.5: Advanced Data Features
```java
// Create loot table modifications (add daggers to structures)
// Add optional recipe variants (alternative crafting patterns)
// Create damage prediction tooltips
// Add sound effect integration (if needed)
```

---

### Phase 5: Testing & Polish

#### Step 5.1: Comprehensive Testing
- Test all material variants
- Test all enchantment combinations (sword + bow + trident)
- Test dual-mode combat (melee Fire Aspect + ranged Flame)
- Test Power vs Sharpness damage scaling
- Test Punch vs Knockback effects
- Test loyalty return mechanics
- Test with other mods (compatibility)

#### Step 5.2: Performance Testing
- Multiple daggers in flight
- Loyalty pathfinding stress test
- Entity tracking limits

#### Step 5.3: Balance Validation
- Verify damage calculations
- Test progression feels right
- Adjust values if needed

#### Step 5.4: Final Polish
- Add advancement system
- Improve sound effects (if needed)
- Add particle effects (optional)
- Documentation and comments

---

### Critical Success Criteria

**Phase 0 Success**: Complete mod foundation - all daggers registered, all tags working, data generation functional, all materials throwable with loyalty
**Phase 1 Success**: Melee combat feels sword-like with correct damage for all materials
**Phase 2 Success**: Ranged damage uses explicit material values with enchantment support (Power, Flame, Punch)
**Phase 3 Success**: All material variants work with proper progression and enchantment compatibility
**Phase 4 Success**: Professional assets and complete data generation system
**Phase 5 Success**: System is stable, performant, and production-ready

---

### Emergency Fallbacks

**If TridentEntity is too complex**: Use TridentEntity directly, override damage in DaggerItem's createEntity()
**If enchantment conflicts occur**: Remove bow enchantments, focus on core sword + loyalty enchantments
**If performance issues**: Limit active dagger entities, add despawn timers
**If mod compatibility fails**: Add configuration to disable problematic features

This roadmap provides a clear, step-by-step path to a robust dagger system that leverages vanilla code for maximum stability and minimum maintenance.

## 🎯 **Revolutionary Dual-Mode Enchantment System**

This implementation creates a **unique weapon class** that no vanilla item possesses:

### **🗡️ Melee Combat Enchantments**
- **Sharpness**: Increases melee damage against all entities
- **Fire Aspect**: Sets melee targets on fire  
- **Knockback**: Increases melee knockback
- **Looting**: Increases drops from melee kills
- **Smite**: Bonus melee damage vs undead
- **Bane of Arthropods**: Bonus melee damage vs arthropods

### **🏹 Ranged Combat Enchantments**  
- **Power**: Increases thrown damage (like arrow damage boost)
- **Flame**: Sets thrown targets on fire (complements Fire Aspect)
- **Punch**: Increases thrown knockback (complements Knockback)

### **🔄 Projectile Mechanics**
- **Loyalty**: Returns dagger to thrower after hit/miss

### **🔧 Universal Enchantments**
- **Unbreaking**: Reduces durability damage
- **Mending**: Repairs with XP
- **Curse of Vanishing**: Disappears on death

### **Perfect Synergy Examples:**
- **Fire Master**: Fire Aspect (melee) + Flame (ranged) = complete fire coverage
- **Knockback King**: Knockback (melee) + Punch (ranged) = crowd control mastery  
- **Damage Dealer**: Sharpness (melee) + Power (ranged) = maximum damage output
- **Loyal Warrior**: Loyalty + any combat enchantments = never lose your weapon

This creates **tactical depth** where players can specialize in melee, ranged, or hybrid combat styles while maintaining the unique returning mechanic that defines daggers as a weapon class. 