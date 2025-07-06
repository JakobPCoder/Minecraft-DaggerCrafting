# DaggerCrafting Mod - Current Implementation Analysis

## Project Overview

**Goal**: A functional throwable dagger system that combines melee and ranged combat capabilities with loyalty enchantment support.

**Core Implementation**: The mod currently implements a dagger system as a custom projectile weapon, extending various Minecraft classes to provide both melee and ranged combat functionality.

## Current Technical Architecture

### Core Design Decisions
The current implementation uses a hybrid approach where daggers are standalone items with projectile entity behavior:

- ✅ **Custom Material System**: `DaggerToolMaterial` as a standalone class with dagger-specific properties
- ✅ **Custom Entity System**: `DaggerEntity` extending `PersistentProjectileEntity` to handle projectile behavior
- ✅ **Loyalty Support**: Custom mixins and data overrides to enable loyalty enchantment
- ✅ **Multi-Material Variants**: Support for wooden, stone, iron, golden, and diamond daggers
- ✅ **Renderer System**: Custom entity renderer with rotation and animation logic

### Core Classes

1. **DaggerToolMaterial** - Custom standalone material class with dagger properties:
   - Standard material properties (durability, mining speed, attack damage, enchantability)
   - Dagger-specific properties (ranged damage, ranged velocity)
   - Static instances for each material type (WOOD, STONE, GOLD, IRON, DIAMOND)

[View source: src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterial.java](src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterial.java)

2. **DaggerItem** - Main weapon class extending `Item` implementing `ProjectileItem`:
   - Uses spear-like throwing mechanics via `onStoppedUsing` method
   - Custom material integration via composition rather than inheritance
   - Supports melee damage via vanilla attributes system
   - Creates DaggerEntity instances when thrown

[View source: src/main/java/jak0bw/daggercrafting/item/DaggerItem.java](src/main/java/jak0bw/daggercrafting/item/DaggerItem.java)

3. **DaggerEntity** - Projectile entity extending `PersistentProjectileEntity`:
   - Handles loyalty behavior through `tick()` method
   - Uses material-specific ranged damage values
   - Tracks loyalty level through data trackers
   - Handles entity collision and damage application
   - Manages projectile state during flight and return

[View source: src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java](src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java)

4. **ModEntities** - Entity type registration class:
   - Registers separate entity types for each material variant
   - Sets appropriate entity dimensions and tracking parameters

[View source: src/main/java/jak0bw/daggercrafting/ModEntities.java](src/main/java/jak0bw/daggercrafting/ModEntities.java)

5. **ModItems** - Item registration class:
   - Registers all dagger variants with material-specific settings
   - Adds items to the combat creative tab
   - Handles dependency checks for registration

[View source: src/main/java/jak0bw/daggercrafting/ModItems.java](src/main/java/jak0bw/daggercrafting/ModItems.java)

6. **DaggerEntityRenderer/State** - Custom renderer for dagger projectiles:
   - Implements smooth rotation animations
   - Tracks and updates yaw/pitch during flight
   - Uses quaternion-based rotation for fluid visual representation

[View source: src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderer.java](src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderer.java)
[View source: src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderState.java](src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderState.java)

### Enchantment Integration

The mod uses multiple approaches to enable enchantment support:

1. **Mixin-Based Integration**:
   - `LoyaltyEnchantmentMixin` injects into Enchantment class to allow loyalty on daggers
   - Intercepts `isAcceptableItem` and `isPrimaryItem` methods

[View source: src/main/java/jak0bw/daggercrafting/mixin/LoyaltyEnchantmentMixin.java](src/main/java/jak0bw/daggercrafting/mixin/LoyaltyEnchantmentMixin.java)

2. **Data-Driven Integration**:
   - Custom JSON file (`data/minecraft/enchantments/loyalty.json`) to add daggers to loyalty's supported items
   - Tag-based system for recognizing dagger items (`data/daggercrafting/tags/item/daggers.json`)

[View source: src/main/resources/data/minecraft/enchantments/loyalty.json](src/main/resources/data/minecraft/enchantments/loyalty.json)
[View source: src/main/resources/data/daggercrafting/tags/item/daggers.json](src/main/resources/data/daggercrafting/tags/item/daggers.json)

3. **Code-Based Handling**:
   - Logic in DaggerEntity to process loyalty behavior during tick cycles
   - Return mechanics for loyalty-enchanted weapons

[View source: src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java](src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java)

## Current Implementation Details

### 1. Material System

The mod implements a custom material system via `DaggerToolMaterial`:

[View source: src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterial.java](src/main/java/jak0bw/daggercrafting/item/DaggerToolMaterial.java)

```java
public class DaggerToolMaterial {
    private final TagKey<Block> inverseTag;
    private final int durability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final TagKey<Item> repairIngredientTag;
    private final float rangedDamage;
    private final float rangedVelocity;
    
    // Constructor and getters...
    
    // Static material definitions
    public static final DaggerToolMaterial WOOD = new DaggerToolMaterial(
        BlockTags.INCORRECT_FOR_WOODEN_TOOL,
        30,  
        2.0F,
        0.0F,
        15,   
        ItemTags.REPAIRS_WOODEN_DAGGER,
        5.0F,
        1.5F
    );
    
    // Other material definitions (STONE, GOLD, IRON, DIAMOND)...
}
```

Material properties vary significantly between types:
- **Durabilities**: Wood (30), Stone (50), Gold (20), Iron (100), Diamond (250)
- **Ranged Damage**: Wood (5.0), Stone (7.0), Gold (7.0), Iron (9.0), Diamond (11.0)
- **Ranged Velocity**: Wood (1.5), Stone (1.7), Gold (1.7), Iron (2.0), Diamond (3.0)
- **Enchantability**: Wood (15), Stone (12), Gold (25), Iron (11), Diamond (10)

### 2. Item Implementation

The `DaggerItem` class implements both melee and ranged functionality:

[View source: src/main/java/jak0bw/daggercrafting/item/DaggerItem.java](src/main/java/jak0bw/daggercrafting/item/DaggerItem.java)

```java
public class DaggerItem extends Item implements ProjectileItem {
    public static final int MIN_DRAW_DURATION = 10;
    public static final float ATTACK_DAMAGE = 8.0F;
    private final DaggerToolMaterial material;
    
    // Projectile creation
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        DaggerEntity daggerEntity = new DaggerEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
        daggerEntity.pickupType = PickupPermission.ALLOWED;
        return daggerEntity;
    }
    
    // Melee functionality
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }
    
    public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, EquipmentSlot.MAINHAND);
    }
    
    // Throwing mechanics via onStoppedUsing
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        // Throw mechanics implementation
    }
}
```

Key features:
- Uses `UseAction.SPEAR` for visual representation when charging
- Implements spear-like throwing mechanics with minimum draw time
- Integrates with enchantment effects through data trackers
- Creates appropriate entity type based on item material

### 3. Entity System

The `DaggerEntity` class handles projectile behavior:

[View source: src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java](src/main/java/jak0bw/daggercrafting/entity/DaggerEntity.java)

```java
public class DaggerEntity extends PersistentProjectileEntity implements FlyingItemEntity {
    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    public String itemId;
    public float seconds;
    private boolean dealtDamage;
    public int returnTimer;
    
    // Entity tick method for projectile behavior
    public void tick() {
        // Handle loyalty return mechanics
        // Update entity position/rotation
        // Process collision detection
    }
    
    // Handle entity hit damage and effects
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        DaggerToolMaterial material = this.getMaterial();
        float damage = material.getRangedDamage();
        
        // Apply damage & effects
    }
}
```

Key features:
- Custom loyalty implementation that returns daggers to the thrower
- Material-based damage calculations for ranged combat
- Smooth animation during flight with seconds tracking
- Proper entity collision and hit registration

### 4. Rendering System

The mod implements a custom renderer for dagger entities:

[View source: src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderer.java](src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderer.java)
[View source: src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderState.java](src/main/java/jak0bw/daggercrafting/entity/DaggerEntityRenderState.java)

```java
public class DaggerEntityRenderer extends FlyingItemEntityRenderer<DaggerEntity> {
    // Animation helper for smooth rotation
    private float getAnimatedAngle(float seconds, float startAngle, float endAngle, float duration) {
        if (seconds <= 0) return startAngle;
        if (seconds >= duration) return endAngle;
        float t = seconds / duration;
        // Quadratic ease-out
        float easeOutT = 1f - (1f - t) * (1f - t);
        return startAngle + (endAngle - startAngle) * easeOutT;
    }
    
    // Rendering implementation with quaternion-based rotation
    @Override
    public void render(FlyingItemEntityRenderState flyingItemEntityRenderState, 
                       MatrixStack matrixStack, 
                       VertexConsumerProvider vertexConsumerProvider, 
                       int i) {
        // Apply rotations and transformations
    }
}
```

Paired with `DaggerEntityRenderState`:

```java
public class DaggerEntityRenderState extends FlyingItemEntityRenderState {
    public float yaw;
    public float pitch;
    public float seconds;
    public boolean hasHit;
}
```

This system provides:
- Smooth in-flight rotation animation
- Proper orientation based on velocity vector
- Angle interpolation during flight
- Proper model rendering with item appearances

### 5. Recipe & Language Support

The mod includes recipe definitions and language files:

[View source: src/main/resources/data/daggercrafting/recipe/diamond_dagger.json](src/main/resources/data/daggercrafting/recipe/diamond_dagger.json)
[View source: src/main/resources/assets/daggercrafting/lang/en_us.json](src/main/resources/assets/daggercrafting/lang/en_us.json)

```json
// Diamond dagger crafting recipe
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    " D",
    "S "
  ],
  "key": {
    "D": "minecraft:diamond",
    "S": "minecraft:stick"
  },
  "result": {
    "id": "daggercrafting:diamond_dagger",
    "count": 1
  }
}

// Language entries
{
  "item.daggercrafting.wooden_dagger": "Wooden Dagger",
  "item.daggercrafting.stone_dagger": "Stone Dagger",
  "item.daggercrafting.golden_dagger": "Golden Dagger",
  "item.daggercrafting.iron_dagger": "Iron Dagger",
  "item.daggercrafting.diamond_dagger": "Diamond Dagger"
}
```

## Current Enchantment Support

### 1. Loyalty Support

Loyalty is implemented through both code and data:

[View source: src/main/java/jak0bw/daggercrafting/mixin/LoyaltyEnchantmentMixin.java](src/main/java/jak0bw/daggercrafting/mixin/LoyaltyEnchantmentMixin.java)
[View source: src/main/resources/data/minecraft/enchantments/loyalty.json](src/main/resources/data/minecraft/enchantments/loyalty.json)

```java
// Mixin for enabling loyalty
@Mixin(Enchantment.class)
public class LoyaltyEnchantmentMixin {
    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void allowDaggerForLoyalty(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Allow loyalty on daggers
    }
    
    @Inject(method = "isPrimaryItem", at = @At("HEAD"), cancellable = true)
    private void allowDaggerAsPrimaryForLoyalty(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // Allow daggers as primary item for loyalty
    }
}

// JSON for loyalty enchantment
{
    "description": { "translate": "enchantment.minecraft.loyalty" },
    "slots": ["mainhand"],
    "supported_items": [
      "#minecraft:tridents",
      "#daggercrafting:daggers"
    ],
    "primary_items": [
      "#minecraft:tridents",
      "#daggercrafting:daggers"
    ],
    "max_level": 3,
    "weight": 5,
    "curse": false,
    "treasure": false
}
```

The loyalty implementation provides:
- Return mechanics for thrown daggers
- Proper sound effects during return
- Velocity adjustment towards player when returning
- Player inventory insertion when returning

## Strengths of Current Implementation

1. **Material Progression**: Clear progression across materials with balanced properties
2. **Dual Combat Modes**: Functional melee and ranged attack capabilities
3. **Loyalty Integration**: Working loyalty enchantment support via mixins and JSON
4. **Visual Representation**: Custom rendering with smooth rotations and animations
5. **Tag-Based System**: Extensible tag system for repair ingredients and enchantment support

## Technical Gaps & Future Improvements

1. **Limited Enchantment Support**: Currently only supports loyalty, not other enchantments
2. **Netherite Tier**: Missing netherite dagger implementation
3. **Enchantment Interaction**: No special handling for bow or sword enchantment interactions
4. **Attack Damage Calculation**: Could improve melee damage calculation system
5. **Advanced Effects**: Missing specialized effects like flame or knockback integration
6. **Documentation**: Limited JavaDoc documentation in some classes
7. **Testing Coverage**: Appears to lack comprehensive test coverage

## Required Improvements

1. **Expand Enchantment Support**: Add support for additional enchantments:
   - Melee: Sharpness, Fire Aspect, Knockback, Smite, Bane of Arthropods
   - Ranged: Power, Punch, Flame
   - Universal: Unbreaking, Mending, Curse of Vanishing

2. **Add Netherite Tier**: Implement netherite dagger with appropriate stats:
   - Higher durability (~400)
   - Increased ranged damage (~13.0)
   - Enhanced ranged velocity (~3.0)
   - Better enchantability (15)

3. **Balance Material Properties**: Fine-tune material properties for balanced gameplay:
   - Review durabilities for better progression
   - Adjust ranged damage to ensure balanced progression
   - Fine-tune velocities for distinct material feel

4. **Improve Melee Combat**: Enhance melee combat mechanics:
   - Implement proper attack damage attributes
   - Add attack speed modifications
   - Include sword-like mining capabilities

5. **Enhance Projectile Effects**: Add special effects for thrown daggers:
   - Particle effects during flight
   - Impact effects on hit
   - Sound variations between materials

6. **Complete Recipe System**: Implement full crafting recipe system:
   - All material variations
   - Smithing recipes for netherite
   - Tag-based ingredient replacement

7. **Advanced Animations**: Refine projectile animations:
   - Material-specific rotation speeds
   - Enhanced rotation during loyalty return
   - Impact animations on hit

## Implementation Roadmap

### Phase 1: Core Enhancements
- Implement netherite dagger
- Balance all material properties
- Improve melee damage calculation
- Fix any rendering issues

### Phase 2: Enchantment Expansion
- Add support for sword enchantments
- Add support for bow enchantments
- Implement enchantment effect handling

### Phase 3: Visual & Audio Improvements
- Add particle effects
- Implement material-specific sounds
- Enhance animation system

### Phase 4: Optimization & Testing
- Performance optimization for multiple daggers
- Comprehensive testing for all enchantments
- Balance passes for all material tiers

## Current Environment & Integration

### Mod Information
[View source: src/main/resources/fabric.mod.json](src/main/resources/fabric.mod.json)

```json
{
    "schemaVersion": 1,
    "id": "daggercrafting",
    "version": "${version}",
    "name": "DaggerCrafting",
    "description": "This is an example description! Tell everyone what your mod is about!",
    "authors": ["Jak0bW"],
    "license": "CC BY-NC 4.0",
    "environment": "*",
    "entrypoints": {
        "main": ["jak0bw.daggercrafting.DaggerCrafting"],
        "fabric-datagen": ["jak0bw.daggercrafting.DaggerCraftingDataGenerator"],
        "client": ["jak0bw.daggercrafting.DaggerCraftingClient"]
    },
    "built_in_datapacks": [{
        "name": "DaggerCraftingPack",
        "description": "Built-in enchantment overrides for dagger support",
        "path": "data",
        "default_enabled": true
    }],
    "mixins": ["daggercrafting.mixins.json"],
    "depends": {
        "fabricloader": ">=0.16.14",
        "minecraft": "~1.21.4",
        "java": ">=21",
        "fabric-api": "*"
    }
}
```

### Dependencies
- Fabric Loader 0.16.14+
- Minecraft 1.21.4
- Java 21+
- Fabric API

## File Structure Overview

```
src/main/
├── java/jak0bw/daggercrafting/
│   ├── item/
│   │   ├── DaggerToolMaterial.java (custom material class with properties)
│   │   ├── DaggerItem.java (main item implementation)
│   │   └── ItemTags.java (repair ingredient tags)
│   ├── entity/
│   │   ├── DaggerEntity.java (projectile entity implementation)
│   │   ├── DaggerEntityRenderer.java (custom renderer)
│   │   └── DaggerEntityRenderState.java (render state tracking)
│   ├── mixin/
│   │   └── LoyaltyEnchantmentMixin.java (enchantment support)
│   ├── ModEntities.java (entity registration)
│   ├── ModItems.java (item registration)
│   ├── DaggerCrafting.java (main mod class)
│   └── DaggerCraftingClient.java (client initialization)
├── resources/
│   ├── assets/daggercrafting/
│   │   ├── lang/
│   │   │   └── en_us.json (language file)
│   │   ├── models/
│   │   └── textures/
│   ├── data/
│   │   ├── daggercrafting/
│   │   │   ├── recipe/ (crafting recipes)
│   │   │   └── tags/item/
│   │   │       └── daggers.json (item tag)
│   │   └── minecraft/
│   │       └── enchantments/
│   │           └── loyalty.json (enchantment override)
│   ├── daggercrafting.mixins.json (mixin configuration)
│   ├── fabric.mod.json (mod metadata)
│   └── pack.mcmeta (data pack configuration)
```

This analysis reflects the current state of the DaggerCrafting mod, highlighting its current implementation, strengths, and areas for improvement. 