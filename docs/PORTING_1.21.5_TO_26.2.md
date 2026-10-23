# DaggerCrafting porting reference: Minecraft / Fabric 1.21.5 → 26.2

This file is the project’s lookup document for the port from **Yarn + Fabric on Minecraft 1.21.5** to **unobfuscated official names + Fabric on Minecraft 26.2**.

It is written for *this* repo, not as a generic Fabric encyclopedia. Every Minecraft and Fabric type that `src/main/java` actually imports or subclasses is tracked. Datapack / resource files this mod ships are tracked the same way. Gameplay drops that do not touch thrown items, enchantments, entities, or item models are mentioned only as version context.

**How to use it**

1. Read the [timeline](#2-timeline-of-relevant-releases) when you need “what happened in which drop”.
2. Read [section 3](#3-class-and-api-evolution-for-everything-this-mod-uses) when a compile error names a type or method. Search the Yarn name you have in the source today.
3. Read [section 4](#4-current-state-on-262-what-this-mod-must-look-like) when implementing. That section is the target, not the history.
4. Treat [section 5](#5-file-by-file-port-checklist) as the work order.

Toolchain (Gradle, Loom plugin id, JDK 25, Fabric 26.2 profile) is already done. This document is the remaining *code and data* map.

Sources (official / primary): Fabric blog posts for 1.21.6, 1.21.9, 1.21.11, 26.1, 26.2; Fabric Docs porting pages; Minecraft Wiki version pages and pack-format history; NeoForged vanilla primers for 1.21.8→1.21.9, 1.21.11, 26.1, 26.2; Stonecutter Minecraft-changes summary. Yarn names below are as in this repo. Official names below are Mojang’s names as used by Fabric after 1.21.11 / 26.1.

---

## 0. Baseline: what 1.21.5 already was

**1.21.5 “Spring to Life”** — 25 March 2025.

| Field | Value |
| --- | --- |
| Protocol | 770 |
| Data version | 4325 |
| Data pack format | 71 |
| Resource pack format | 55 |
| Java | 21 |
| Obfuscation | Yes (Yarn + Intermediary) |
| This repo’s mappings | Yarn |

Things this mod already depended on in 1.21.5 (do not “discover” them again as 26.2 news):

- Item **data components** (`DataComponentTypes`, `AttributeModifiersComponent`, `maxDamage`, `repairable`, `enchantable`). Unstructured item NBT was already gone (1.20.5).
- **Data-driven enchantments** as JSON under `data/<mod>/enchantment/` (1.21). Java only holds a `RegistryKey`.
- **Item models** split into `assets/<mod>/items/<id>.json` (definition) + `models/item/` (geometry). This repo already uses that 1.21.4+ format.
- **Entity render states**: `FlyingItemEntityRenderState`, `updateRenderState`, `createRenderState` (started 1.21.2). The renderer still *drew* with `render(..., VertexConsumerProvider, light)` in 1.21.5.
- `Item.appendTooltip` already taking `TooltipDisplayComponent` + `Consumer<Text>` (1.21.5 tooltip overhaul). Fabric already considered this deprecated in favour of component tooltip appenders by 1.21.6.
- NBT `getBoolean` already returning `Optional` in this codebase (`.orElse(false)`).
- Item registration via `Items.register(RegistryKey, factory, settings)` (not the old `Registry.register` + `Identifier` only).
- Tags already depluralised: `tags/item/`, `recipe/` (not `recipes/`).
- `Identifier.of(namespace, path)` (no public `new Identifier(...)`).
- Projectile contract: `ProjectileItem` + `PersistentProjectileEntity` (trident-like) + `spawnWithVelocity`.

Java 21, Gradle 8.x, plugin id `fabric-loom`, Yarn `mappings` dependency, `modImplementation` for Fabric API: that is the **old** toolchain. The new one is recorded in `gradle.properties` / `build.gradle`.

---

## 1. Inventory of Minecraft / Fabric types this mod uses (1.21.5 Yarn)

These are the *only* Minecraft/Fabric types that must be ported by name. If a type is not listed, it is unused here (except transitive Minecraft internals).

### 1.1 Fabric Loader / API

| This repo (Yarn 1.21.5) | Role |
| --- | --- |
| `net.fabricmc.api.ModInitializer` | `DaggerCrafting` |
| `net.fabricmc.api.ClientModInitializer` | `DaggerCraftingClient` |
| `net.fabricmc.api.EnvType` / `@Environment` | `DaggerEntityRenderer` |
| `net.fabricmc.loader.api.FabricLoader` | optional-mod checks in `ModItems` |
| `net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents` | combat tab entries |
| `net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry` | client entity renderers |
| `net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint` | empty datagen entry |
| `net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator` | unused body |

Mixin: `org.spongepowered.asm.mixin.*` on `Enchantment` and `MinecraftServer`.

### 1.2 Registration, ids, tags

`Identifier`, `RegistryKey`, `RegistryKeys`, `Registries`, `Registry`, `RegistryEntry`, `TagKey`, `BlockTags`, `Items`, `ItemGroups`, `ItemGroup`.

### 1.3 Items / components / attributes / combat use

`Item`, `Item.Settings`, `Item.TooltipContext`, `ItemStack`, `ProjectileItem`, `TridentItem` (imported, used as behaviour reference), `UseAction`, `TooltipType`, `TooltipDisplayComponent`, `DataComponentTypes`, `EnchantmentEffectComponentTypes` (imported), `AttributeModifierSlot`, `AttributeModifiersComponent`, `ToolComponent` (imported, unused in body), `EntityAttributeModifier`, `EntityAttributes`, `EquipmentSlot`, `ActionResult`, `Hand`, `Stats`.

Methods that will move: `canMine`, `getUseAction`, `getMaxUseTime`, `onStoppedUsing`, `use`, `postDamageEntity`, `createEntity`, `appendTooltip`, `willBreakNextUse`, `damage`, `copyWithCount`, `hasGlint`, `getStackInHand`, `setCurrentHand`, `incrementStat`.

### 1.4 Enchantments

`Enchantment`, `EnchantmentHelper` (`getLevel`, `getTridentSpinAttackStrength`, `getDamage`, `onTargetDamaged`, `onHitBlock`, `getTridentReturnAcceleration`, `modifyKnockback`).

Mixin targets: `Enchantment.isAcceptableItem`, `Enchantment.isPrimaryItem`. JSON: `data/daggercrafting/enchantment/throwing_speed.json`, tag `data/minecraft/tags/enchantment/in_enchanting_table.json`.

### 1.5 Entities / projectiles / damage / tracking

`Entity`, `EntityType`, `EntityType.Builder`, `SpawnGroup`, `LivingEntity`, `PlayerEntity`, `ServerPlayerEntity`, `FlyingItemEntity`, `ProjectileEntity`, `PersistentProjectileEntity`, `PersistentProjectileEntity.PickupPermission`, `TridentEntity` (imported), `ProjectileDeflection`, `DamageSource`, `DataTracker`, `TrackedData`, `TrackedDataHandlerRegistry`, `EquipmentSlot`.

Methods: constructors with owner/xyz, `initDataTracker`, `tick`, `getOwner`, `getWorld`, `getItemStack` / `asItemStack` / `getStack`, `dropStack`, `discard`, `isNoClip` / `setNoClip`, `inGroundTime`, `isInGround`, `pickupType`, `getPos` / `getEyePos` / `getVelocity` / `setVelocity`, `getYaw` / `setYaw` / `getPitch` / `setPitch`, `age`, `velocityModified` / `velocityDirty`, `onEntityHit`, `getEntityCollision`, `tryPickup`, `onPlayerCollision`, `getDefaultItemStack`, `getHitSound`, `getWeaponStack`, `knockback`, `readCustomDataFromNbt`, `writeCustomDataToNbt`, `spawnWithVelocity`, `playSound`, `playSoundFromEntity`, `clientDamage`, `damage(ServerWorld, ...)`, `kill`, `isOnFire` / `setOnFireFor`.

### 1.6 Worlds / math / text / sound / NBT

`World`, `ServerWorld`, `BlockPos`, `BlockState`, `Direction`, `Position`, `Vec3d`, `MathHelper`, `BlockHitResult`, `EntityHitResult`, `NbtCompound`, `Text`, `Formatting`, `SoundEvent`, `SoundEvents`, `SoundCategory`.

### 1.7 Client rendering (Yarn packages)

`FlyingItemEntityRenderer`, `FlyingItemEntityRenderState`, `EntityRendererFactory`, `ItemModelManager`, `OverlayTexture`, `VertexConsumerProvider`, `MatrixStack`, `ItemDisplayContext`.

Methods: `render(state, matrices, consumers, light)`, `updateRenderState`, `createRenderState`, `ItemModelManager.updateForNonLivingEntity`, `itemRenderState.render(...)`.

### 1.8 JSON this mod owns

- `fabric.mod.json`, `daggercrafting.mixins.json`
- `assets/daggercrafting/items/*.json`, `models/item/*.json`, `textures/item/*.png`, `lang/en_us.json`
- `data/daggercrafting/recipe/*.json`, `tags/item/*.json`, `enchantment/throwing_speed.json`
- `data/minecraft/tags/enchantment/in_enchanting_table.json`

---

## 2. Timeline of relevant releases

Versions with **no extra Java-API work for this mod** are still listed so pack formats and toolchain numbers stay honest. “Relevant to this mod” means items, thrown projectiles, entity render, enchantments, registries, NBT, or Fabric APIs we call.

| Date | Version | Drop | Java | Obfuscated | DP | RP | Proto | Data | Fabric notes (at time of drop) | This mod |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2025-03-25 | **1.21.5** | Spring to Life | 21 | yes | 71 | 55 | 770 | 4325 | Yarn default | **Start** |
| 2025-06-17 | **1.21.6** | Chase the Skies | 21 | yes | 80 | 63 | 771 | 4435 | Loom 1.10, Loader ~0.16.14 | Render extract/draw split (GUI/chunks); tooltip API; BlockEntity `ReadView`/`WriteView`; HUD API rewrite; some Fabric modules removed |
| 2025-06-30 | 1.21.7 | hotfix | 21 | yes | 81 | 64 | | | same line as 1.21.6 | None beyond pack bump |
| 2025-07-17 | 1.21.8 | hotfix | 21 | yes | 81 | 64 | | | same | None |
| 2025-09-30 | **1.21.9** | Copper Age | 21 | yes | 88.0 | 69.0 | | | Loom 1.11, Loader ~0.17.2 | **Entity `render` → `submit`**; world/entity/particle command queue; `Entity#getWorld` → `getEntityWorld`; pack.mcmeta `min_format`/`max_format`; OpenGL 3.3; ResourceLoader v1; MixinExtras 5 |
| 2025-10-07 | 1.21.10 | hotfix | 21 | yes | 88.0 | 69.0 | | | Fabric re-added world render events | No extra item/entity work |
| 2025-12-09 | **1.21.11** | Mounts of Mayhem | 21 | **last obfuscated** (experimental unobf jars existed) | 94.1 | 75.0 | 774 | 4671 | Loom 1.14, Loader ~0.18.1 | **Yarn/Intermediary stop after this**; Mojmap `ResourceLocation` → `Identifier`; projectile package split (`AbstractArrow` → `...projectile.arrow`); gamerules registry; item textures **items atlas**; JSpecify; `attack_range` component |
| 2026-03-24 | **26.1** | Tiny Takeover | **25** | **no** | 101.1 | 84.0 | 775 | 4786 | Loom 1.15, Gradle 9.4+, Loader ~0.18.4, plugin `net.fabricmc.fabric-loom` | **Hard break**: unobfuscated names; no Yarn; `ItemStackTemplate`; Fabric API Yarn names rewritten (`ItemGroupEvents` → `CreativeModeTabEvents`); world storage change; last OpenGL-only release |
| 2026-04-01 / 04-09 | 26.1.1 / 26.1.2 | hotfixes | 25 | no | 101.1 | 84.0 | | | | None expected |
| 2026-06-16 | **26.2** | Chaos Cubed | 25 | no | 107.1 | 88.0 | 776 | 4903 | Loom 1.17+ (this repo: 1.18-SNAPSHOT), Gradle 9.5.1+, Loader 0.19.3+ (this repo: 0.19.5), FAPI `0.161.0+26.2` | Vulkan *or* OpenGL backend; `BlockIds`/`ItemIds`/`EntityTypeIds`; `Gui`/`Hud` screen move; entity spawn `ValueInput`; enum-extension API (unused here) |

**Version numbering:** 1.21.11 was the last `1.x` Java Edition. From 26.1 the scheme is `YY.drop[.hotfix]`. Fabric’s supported “most played” target for this project is **26.2**, not 26.3.

**Mapping rule of thumb**

- Through 1.21.11: compiled mods use Intermediary. Source in this repo is Yarn.
- 26.1+: the game jar *is* official names (with parameter names). No `mappings { }` line. No `modImplementation`. No `remapJar`. Yarn is not maintained.

A 1.21.5 jar **cannot** run on 26.1+ even if APIs were identical. Recompile is mandatory.

---

## 3. Class and API evolution for everything this mod uses

Each subsection: Yarn 1.21.5 name → what changed at each drop → official 26.2 name and what to call in code.

Packages below use **official 26.2** names in the “now” column. Yarn packages are what the Java files still import.

### 3.1 Identifiers and registries

| Yarn 1.21.5 | Official (Mojmap mid-1.21) | Official 26.2 |
| --- | --- | --- |
| `net.minecraft.util.Identifier` | `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` |
| `Identifier.of(ns, path)` | `ResourceLocation.fromNamespaceAndPath` / `withDefaultNamespace` | `Identifier.fromNamespaceAndPath` / `withDefaultNamespace` (Yarn `of` is gone if you are on official names; this repo’s `Identifier.of` is Yarn) |
| `RegistryKey` | `ResourceKey` | `ResourceKey` |
| `RegistryKeys` | `net.minecraft.core.registries.Registries` | same |
| `Registries` (Yarn built-in registry holder) | `BuiltInRegistries` | `BuiltInRegistries` |
| `Registry.register` | `Registry.register` | `Registry.register` |
| `RegistryEntry<T>` | `Holder<T>` | `Holder<T>` |
| `TagKey` | `TagKey` | `TagKey` |

**When**

- **1.21.11:** every Mojmap `ResourceLocation` was renamed to `Identifier` (class, methods `readResourceLocation` → `readIdentifier`, `ResourceKey#location` → `identifier`, etc.). Yarn mods already said `Identifier`, so a Yarn→Mojmap migrate *before* 1.21.11 produced `ResourceLocation`, then 1.21.11 flipped it back to `Identifier`. Direct Yarn → 26.2 can keep thinking “Identifier”.
- **26.2:** vanilla stores keys for blocks/items/entity types in `ItemIds`, `BlockIds`, `BlockItemIds`, `EntityTypeIds` rather than hanging identity only on the object. Custom items still `ResourceKey.create(Registries.ITEM, id)` then register; for datagen, prefer keeping the key next to the item like vanilla.

**This repo:** `ModItems`, `ModEntities`, `ModEnchantments`, `DaggerToolMaterial` all use `Identifier.of` + `RegistryKey.of(RegistryKeys.*, ...)`. After port: `Identifier.fromNamespaceAndPath` + `ResourceKey.create(Registries.ITEM|ENTITY_TYPE|ENCHANTMENT, id)`.

`Items.register(RegistryKey, factory, settings)` stays conceptually. `Item.Settings` → `Item.Properties`. Confirm the 26.2 `Items.register` signature against genSources (factory vs instance).

### 3.2 Fabric entrypoints (almost stable)

| Type | Changes |
| --- | --- |
| `ModInitializer` / `ClientModInitializer` | Unchanged through 26.2. |
| `@Environment(EnvType.CLIENT)` | Unchanged. |
| `fabric.mod.json` `depends` | Must become `fabricloader >=0.19.5`, `minecraft ~26.2`, `java >=25`, `fabric-api` (not the deprecated `fabric` mod id; **26.1 removed** the `fabric` alias). Already updated in this repo. |
| `compatibilityLevel` in mixins json | `JAVA_21` → `JAVA_25` (already done). |
| Datagen entry | `DataGeneratorEntrypoint` remains. `FabricDataGenerator` remains. Internal provider class names Yarn→official in **26.1** (`FabricTagProvider` → `FabricTagsProvider`, `FabricDataOutput` → `FabricPackOutput`, …). Empty class here, low risk. |

### 3.3 Creative tabs (Fabric + vanilla)

| Yarn 1.21.5 | 26.2 official / Fabric |
| --- | --- |
| `ItemGroups` | `CreativeModeTabs` |
| `ItemGroup` | `CreativeModeTab` |
| `ItemGroupEvents` `net.fabricmc.fabric.api.itemgroup.v1` | `CreativeModeTabEvents` `net.fabricmc.fabric.api.creativetab.v1` |
| `modifyEntriesEvent(group).register(entries -> entries.add(item))` | `modifyOutputEvent(tab).register(output -> output.accept(item))` (Fabric rename list: `modifyEntries` → `modifyOutput`, `addAfter`/`addBefore` → `insertAfter`/`insertBefore`) |

**When:** Fabric rename is **26.1** (Yarn-shaped Fabric API → official-shaped). Vanilla `ItemGroups` → `CreativeModeTabs` is the Yarn vs Mojmap difference; it applies as soon as you leave Yarn.

### 3.4 `Item`, `Item.Settings`, tool-like defaults

| Yarn | Official 26.2 |
| --- | --- |
| `Item` | `Item` |
| `Item.Settings` | `Item.Properties` |
| `.maxDamage` / `.repairable(tag)` / `.enchantable(int)` / `.attributeModifiers(...)` | same ideas on `Properties`; verify method names in genSources |
| `AttributeModifiersComponent` | `ItemAttributeModifiers` |
| `AttributeModifierSlot` | `EquipmentSlotGroup` |
| `EntityAttributeModifier` | `AttributeModifier` |
| `EntityAttributeModifier.Operation.ADD_VALUE` | `AttributeModifier.Operation.ADD_VALUE` (confirm enum constant) |
| `EntityAttributes.ATTACK_DAMAGE` / `ATTACK_SPEED` | `Attributes.ATTACK_DAMAGE` / `ATTACK_SPEED` |
| `Item.BASE_ATTACK_DAMAGE_MODIFIER_ID` | `Item.BASE_ATTACK_DAMAGE_ID` (Mojmap historically); confirm 26.2 |
| `DataComponentTypes` | `DataComponents` |
| `ToolComponent` | `Tool` |
| `canMine(BlockState, World, BlockPos, PlayerEntity)` | `canDestroyBlock` / `mineBlock` family — **must check 26.2 `Item`**. Yarn `canMine` often mapped to `canAttackBlock`. |
| `UseAction` (`item.consume.UseAction`) | `ItemUseAnimation` (`SPEAR` still the trident pose) |
| `getUseAction` | `getUseAnimation` |
| `getMaxUseTime` | `getUseDuration` |
| `onStoppedUsing` → `boolean` in this 1.21.5 code | return type has flipped between `boolean` and `boolean`/`void` across versions; **1.21.2+** often `boolean`. Confirm 26.2 `Item.releaseUsing`. |
| `use(World, Player, Hand)` → `ActionResult` | `use(Level, Player, InteractionHand)` → `InteractionResult` |
| `ActionResult.FAIL` / `CONSUME` | `InteractionResult.FAIL` / `CONSUME` (or `SUCCESS`/`CONSUME` equivalents — InteractionResult was enum-merged around 1.21.2; 26.2 still an enum) |
| `Hand` | `InteractionHand` |
| `postDamageEntity` | `hurtEnemy` / `postHurtEnemy` — Yarn `postHit` / `postDamageEntity` shifted when item functions became components. **Must match 26.2 `Item`.** |
| `PlayerEntity.getInventory().removeOne(stack)` | `Player.getInventory().removeItem(stack)` / `removeItemNoUpdate` — confirm |
| `stack.willBreakNextUse()` | still a thing on `ItemStack` through 1.21.x; confirm 26.2 |
| `stack.damage(1, player)` / `stack.damage(1, attacker, EquipmentSlot.MAINHAND)` | `hurtAndBreak` family; Fabric `CustomDamageHandler.damage` → `hurtAndBreak` in 26.1 |
| `Stats.USED.getOrCreateStat(this)` | `Stats.ITEM_USED.get(this)` historically; confirm |

**1.21.6:** `Item#appendTooltip` deprecated as the extension point. Fabric added `ComponentTooltipAppenderRegistry` (26.1 rename: `ItemComponentTooltipProviderRegistry`). Daggers currently override `appendTooltip` with thrown damage/velocity lines. Port options: (a) keep override if vanilla still calls it; (b) move lines into a custom data component + tooltip appender; (c) put stats in item lore component JSON. Prefer (a) only if genSources still has the method.

**26.1 `ItemStack` rules (important):**

- You cannot construct a usable `ItemStack` until a world/registry access is live. Pre-world immutable form is `ItemStackTemplate` (item holder + count + `DataComponentPatch`).
- `ItemStack` implements `ItemInstance`. `EnchantmentHelper.getItemEnchantmentLevel` takes `ItemInstance`.
- `new ItemStack(item)` in entity `getDefaultItemStack` is OK on a live entity (world exists). Static item registration must not build stacks at class-init for components.
- Recipes / advancements / hover events use templates, not stacks. This mod’s recipes are JSON, so the Java recipe-serializer rewrite does not apply unless you add custom recipe types.

**26.2:** `ItemIds` for vanilla-style registration. `ItemStackTemplate.fromStack`. `ItemStack#getDamageSource` signature simplified.

`DaggerToolMaterial` is *our* class, not vanilla `ToolMaterial`. Keep it. `BlockTags.INCORRECT_FOR_*_TOOL` still exist as of 1.21.x; confirm copper: this mod’s copper dagger uses `INCORRECT_FOR_STONE_TOOL`. If 26.2 added official copper tools, there may be `INCORRECT_FOR_COPPER_TOOL` — optional improvement, not a compile requirement.

### 3.5 `ProjectileItem`, thrown spawn, trident-like use

| Yarn | Official |
| --- | --- |
| `ProjectileItem` | `ProjectileItem` |
| `createEntity(World, Position, ItemStack, Direction)` | `asProjectile` / `createProjectile` — **confirm 26.2 interface** |
| `ProjectileEntity.spawnWithVelocity(...)` | `Projectile.spawnProjectileFromRotation` / similar on `Projectile` |
| `TridentItem` | `TridentItem` |
| `SoundEvents.ITEM_TRIDENT_THROW` | `SoundEvents.TRIDENT_THROW` (Yarn `ITEM_*` vs Mojmap). Always compare to vanilla `TridentItem` in genSources rather than guessing prefixes. |
| `world.playSoundFromEntity` | `level.playSound` / `playSoundFromEntity` |

**Port method:** open 26.2 `TridentItem` and copy the current throw/riptide skeleton. DaggerItem is a trident clone with extra charge curve and `throwing_speed`. Vanilla will have moved method names; behaviour (charge, riptide check `EnchantmentHelper.getTridentSpinAttackStrength`, consume stack, pickup permission) is still the template.

Riptide helper: Yarn `EnchantmentHelper.getTridentSpinAttackStrength(stack, user)` — still the right idea; official name may be `getTridentSpinAttackStrength` or `getSpinAttackStrength`.

### 3.6 `ItemStack` (entity + item)

Already covered templates in 3.4. Extra methods this repo calls:

| Yarn | Notes across versions |
| --- | --- |
| `getItem()` | stable |
| `isEmpty()` | stable |
| `copyWithCount(1)` | stable |
| `hasGlint()` | official `hasFoil()` |
| `getBoolean` NBT on the *entity*, not the stack | see 3.10 |

`FlyingItemEntity.getStack()` in Yarn implemented as `getItemStack()` on the projectile. Official interface was `ItemSupplier.getItem()`. 26.2 name: check whether the interface is still `ItemSupplier` or unobfuscated `FlyingItemEntity`. Yarn file `DaggerEntity implements FlyingItemEntity` with `getStack()` → map to whatever 26.2 thrown-item interface `ThrownTrident` implements.

### 3.7 Enchantments (Java, JSON, mixin)

**Java**

| Yarn | Official |
| --- | --- |
| `Enchantment` | `Enchantment` |
| `RegistryKeys.ENCHANTMENT` | `Registries.ENCHANTMENT` |
| `EnchantmentHelper.getLevel(entry, stack)` | `getItemEnchantmentLevel(enchantment, ItemInstance)` as of **26.1** |
| `serverWorld.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(id)` | `level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(id)` / `getHolder` |

**1.21.9:** Fabric added access wideners for `EnchantmentHelper` / `Enchantment` utility methods (easier custom effect components). Not required for throwing_speed as JSON.

**1.21.11:** `Enchantment#doLunge` added (spears / mounts of mayhem). Irrelevant unless you want spear enchant compatibility. New `minecraft:attack_range` component on items.

**Mixin `EnchantmentMixin` (high risk)**

Yarn methods:

- `isAcceptableItem(ItemStack)` — anvil / “can this enchantment apply”
- `isPrimaryItem(ItemStack)` — enchanting table primary items

Official names were typically `canEnchant` and something like `isPrimaryItem` / matching-item helpers. **Targets must be re-bound against 26.2 `Enchantment` in genSources.** `self.toString().toLowerCase().contains("sharpness")` is illegal-ish as a design (depends on `Enchantment.toString()` format: Yarn used a readable string; official/unobf may print the registry id). After port, match `ResourceKey` / `Holder` id path (`minecraft:sharpness`) instead of English `toString()`.

Supported-item JSON already lists `#daggercrafting:daggers`. Vanilla datapack enchantments (sharpness, loyalty, …) will **not** apply just because the mixin returns true if the enchantment’s `supported_items` tag excludes daggers. The mixin was compensating for vanilla tags. Prefer **datapack tags** (`minecraft:enchantable/weapon`, `enchantable/durability`, loyalty’s supported items, etc.) plus `primary_items`, and delete the mixin if tags suffice. That is more 1.21+ style and survives mapping changes.

**JSON `throwing_speed.json`**

1.21.5 format this repo has (`supported_items`, `primary_items`, `weight`, `max_level`, `min_cost`/`max_cost`, `anvil_cost`, `slots`, `effects`) is still the data-driven enchantment model. Pack format bumps 71 → 107.1 added many *other* effect fields; throwing_speed’s empty `minecraft:projectile_spawned` + `all_of` stub should still parse, but **validate** against 26.2 vanilla enchantment JSON (e.g. `loyalty.json`) for renamed effect types.

`data/minecraft/tags/enchantment/in_enchanting_table.json` still the table list; pack folder remains `tags/enchantment/`.

### 3.8 Entity types and spawn groups

| Yarn | Official |
| --- | --- |
| `EntityType` | `EntityType` |
| `SpawnGroup.MISC` | `MobCategory.MISC` |
| `EntityType.Builder.create(ctor, group)` | `EntityType.Builder.of(ctor, category)` |
| `.dimensions(w, h)` | `.sized(w, h)` historically; unobf may say `dimensions` — **check 26.2** |
| `.maxTrackingRange(4)` | `.clientTrackingRange(4)` |
| `.trackingTickInterval(20)` | `.updateInterval(20)` |
| `.build(RegistryKey)` | `.build(ResourceKey)` |
| `Registry.register(Registries.ENTITY_TYPE, id, type)` | `Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type)` |

**1.21.11:** `ThrownTrident` / `AbstractArrow` moved to `net.minecraft.world.entity.projectile.arrow.*`. `DaggerEntity extends PersistentProjectileEntity` → `extends AbstractArrow` in that package.

**26.2:** `EntityTypeIds`. Spawn: `EntityType.create(ValueInput, Level, EntitySpawnRequest)` replaced older `(ValueInput, Level, EntitySpawnReason)`.

### 3.9 Projectile entity (`PersistentProjectileEntity`)

This is the largest vanilla subclass in the mod (`DaggerEntity`).

| Yarn 1.21.5 | Official 26.2 (expected) |
| --- | --- |
| `PersistentProjectileEntity` | `net.minecraft.world.entity.projectile.arrow.AbstractArrow` |
| `PickupPermission` | `AbstractArrow.Pickup` (`ALLOWED`, `CREATIVE_ONLY`, `DISALLOWED`) |
| `pickupType` | `pickup` |
| `FlyingItemEntity` | `ItemSupplier` or unobf equivalent |
| `ProjectileEntity` | `Projectile` |
| `TridentEntity` | `ThrownTrident` (same `arrow` package) |
| `ProjectileDeflection.SIMPLE` | `ProjectileDeflection.REVERSE` / `SIMPLE` — copy `ThrownTrident` |
| `DataTracker` | `SynchedEntityData` |
| `TrackedData<T>` | `EntityDataAccessor<T>` |
| `TrackedDataHandlerRegistry` | `EntityDataSerializers` |
| `DataTracker.registerData` | `SynchedEntityData.defineId` |
| `initDataTracker(Builder)` | `defineSynchedData(Builder)` |
| `dataTracker` | `entityData` |
| `getWorld()` | **1.21.9 Yarn:** `getEntityWorld()`. Official: `level()` |
| `ServerWorld` | `ServerLevel` |
| `World` | `Level` |
| `PlayerEntity` | `Player` |
| `ServerPlayerEntity` | `ServerPlayer` |
| `LivingEntity` | `LivingEntity` |
| `isClient` | `isClientSide` |
| `getPos()` | `position()` |
| `getEyePos()` | `getEyePosition()` |
| `getVelocity()` / `setVelocity` | `getDeltaMovement` / `setDeltaMovement` |
| `getYaw` / `setYaw` / `getPitch` / `setPitch` | `getYRot` / `setYRot` / `getXRot` / `setXRot` |
| `age` | `tickCount` |
| `inGroundTime` | `inGroundTime` (often same) |
| `isInGround()` | `isInGround()` |
| `isNoClip` / `setNoClip` | `isNoPhysics` / `setNoPhysics` |
| `discard()` | `discard()` |
| `dropStack(world, stack, offset)` | `spawnAtLocation` family |
| `asItemStack()` / `getItemStack()` | `getPickupItem()` / `getPickupItemStackOrigin()` — copy `ThrownTrident` |
| `getDefaultItemStack()` | `getDefaultPickupItem()` |
| `tryPickup(Player)` | `tryPickup` / `attemptPickup` |
| `onPlayerCollision` | `playerTouch` |
| `onEntityHit` | `onHitEntity` |
| `getEntityCollision` | `findHitEntity` (**1.21.11** also added `findHitEntities`) |
| `getHitSound` | `getHitGroundSoundEvent` |
| `getWeaponStack` | `getWeaponItem` |
| `getDamageSources().trident(...)` | `damageSources().trident(...)` |
| `entity.damage(serverWorld, source, amount)` | `hurtServer(serverLevel, source, amount)` (1.21.2+ server hurt) |
| `clientDamage` | `handleDamageEvent` / client `hurt` — copy current `ThrownTrident` |
| `EnchantmentHelper.getDamage` | `modifyDamage` / `getDamage` — copy trident |
| `EnchantmentHelper.onTargetDamaged` | `doPostAttackEffects` / `onLivingDamaged` |
| `EnchantmentHelper.onHitBlock` | `onHitBlock` |
| `EnchantmentHelper.getTridentReturnAcceleration` | loyalty helper on `EnchantmentHelper` |
| `EnchantmentHelper.modifyKnockback` | `modifyKnockback` |
| `velocityModified` / `velocityDirty` | `hasImpulse` / `hurtMarked` historically |
| `readCustomDataFromNbt` / `writeCustomDataToNbt` | see 3.10 |
| `getAttributeValue(KNOCKBACK_RESISTANCE)` | `getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)` |
| `addVelocity` | `push` / `addDeltaMovement` |

**1.21.6:** extract/render split did **not** yet rewrite entity `render` (that is 1.21.9). Entity NBT views: BlockEntities first; entities followed later (definitely by 26.x `ValueInput`).

**1.21.9:** entity rendering submission (section 3.11). Logic methods above still on the entity.

**Always port `DaggerEntity` by diffing 26.2 `ThrownTrident`**, then re-apply dagger-specific loyalty motion, charge-independent ranged damage from `DaggerToolMaterial`, and fire-on-hit.

`SpectralArrowItem` import in `DaggerEntity` is unused — drop it.

### 3.10 NBT / `NbtCompound`

| Yarn | Official |
| --- | --- |
| `NbtCompound` | `CompoundTag` |
| `readCustomDataFromNbt` / `writeCustomDataToNbt` | Mojmap `readAdditionalSaveData` / `addAdditionalSaveData` |

**1.21.5:** many getters already `Optional` (`getBoolean("DealtDamage").orElse(false)` is already in this repo).

**1.21.6:** BlockEntities use `ReadView` / `WriteView` (`read`/`put` + codecs). Fabric serialization module in 1.21.9.

**26.1:** those views renamed toward `ValueInput` / `ValueOutput` (Fabric: `FabricReadView` → `FabricValueInput`).

**26.2:** entity spawn path `create(ValueInput, Level, EntitySpawnRequest)`. If `DaggerEntity` still has nbt methods, they likely look like `readAdditionalSaveData(ValueInput)` / `addAdditionalSaveData(ValueOutput)` with `input.getBooleanOr("DealtDamage", false)`.

Do not write raw `CompoundTag` for new code if vanilla entity uses `ValueInput`.

### 3.11 Client entity renderer (the other high-risk file)

**1.21.2–1.21.5 (current source):** extract state + `render(State, MatrixStack, VertexConsumerProvider, light)`.

**1.21.6:** GUI/HUD/chunks converted to extract-then-draw. `RenderSystem` methods removed; `RenderPipeline` + `RenderLayer`. Entity renderers still old-style.

**1.21.9 (mandatory for `DaggerEntityRenderer`):**

Yarn / then official:

- `EntityRenderer.render(S, PoseStack, MultiBufferSource, light)` → **`submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)`**
- `VertexConsumerProvider` → not passed; geometry goes through `SubmitNodeCollector` (Yarn `OrderedRenderCommandQueue` in the Fabric 1.21.9 blog)
- `ItemStackRenderState.render(...)` → `submit(..., SubmitNodeCollector, outlineColor)`
- `EntityRendererFactory` → `EntityRendererProvider`
- `FlyingItemEntityRenderer` → official `ThrownItemRenderer` (confirm 26.2 simple-name; unobf may differ)
- `FlyingItemEntityRenderState` → `ThrownItemRenderState` (confirm)
- `MatrixStack` → `PoseStack`
- `ItemModelManager` → `ItemModelResolver` (Mojmap) — `updateForNonLivingEntity` must be re-found on 26.2 context (`EntityRendererProvider.Context`)
- `ItemDisplayContext.GROUND` → same or `ItemDisplayContext.GROUND`
- `OverlayTexture.DEFAULT_UV` → `OverlayTexture.NO_OVERLAY`

`updateRenderState` / `createRenderState` stay. Custom `DaggerEntityRenderState` fields (`yaw`, `pitch`, `seconds`, `hasHit`) stay. Apply spin with `PoseStack` + JOML as now, then **submit** the item render state instead of calling `render` on it.

**1.21.11:** item textures live on the **`items` atlas**, not `blocks`. Item models may only reference item-atlas textures. This repo’s `textures/item/*.png` + `models/item` is already the right layout; do not point item models at `textures/block`.

**26.1:** more extract/submit naming (`GuiGraphics` → `GuiGraphicsExtractor` in Stonecutter’s notes). `EntityRenderer#submitNameTag` → `submitNameDisplay`. Fluid/terrain layers automatic — not used here.

**26.2:** `extractNameplates` → `extractNameTags`. Vulkan backend: **do not call raw OpenGL**. This renderer only uses Minecraft’s item submit path, so it should be backend-safe. Screen: `Minecraft.getInstance().setScreen` → `minecraft.gui.setScreen` (only if we ever open a screen).

**Fabric `EntityRendererRegistry`:** still the registration API in 1.21.5–1.21.11. No rename showed up in the 26.1 Fabric API rename dump (unlike item groups). Confirm import; if missing, use vanilla `EntityRenderers.register`.

### 3.12 Sounds, text, stats, math

| Yarn | Official |
| --- | --- |
| `SoundCategory` | `SoundSource` |
| `SoundEvents.*` | same class, many constant names drop `ITEM_` / `BLOCK_` prefixes — **diff against ThrownTrident** |
| `Text` | `Component` |
| `Text.translatable` / `literal` | `Component.translatable` / `literal` |
| `Formatting` | `ChatFormatting` |
| `MathHelper` | `Mth` |
| `Vec3d` | `Vec3` |
| `Vec3d.ZERO` | `Vec3.ZERO` |
| `horizontalLength()` | `horizontalDistance()` |
| `BlockPos.clampToWithin` | keep; verify |

**1.21.6:** new `ui` sound category. Unrelated.

### 3.13 Mixins other than enchantment

`ExampleMixin` injects `MinecraftServer.loadWorld`. Official: `loadLevel`. The method may have been removed or split with the **26.1 world storage rewrite**. This mixin is template noise and does nothing useful — **delete it** during the port rather than chasing the new world-load method.

`daggercrafting.mixins.json`: `"package"`, `"mixins": ["EnchantmentMixin"]`. After deleting ExampleMixin, do not list it. `JAVA_25` already set. MixinExtras 5.0 (1.21.9 / Loader 0.17) is available if you rewrite the enchant mixin with expressions; not required.

### 3.14 Fabric APIs we do *not* use (do not port)

World render events (removed 1.21.9, restored 1.21.10 with extract/submit), HUD `HudElementRegistry`, ResourceLoader v1, game-rule registry, villager trade JSON (26.1), fluid models, packet splitter, `ItemEvents#USE` (26.1 — only if you want to replace `Item.use` mixin-style). Ignore unless you add features.

---

## 4. Current state on 26.2 (what this mod must look like)

This is the “if we implemented the port correctly” snapshot.

### 4.1 Toolchain (already in repo)

- JDK **25** (`org.gradle.java.home` + toolchain 25 + `options.release = 25`)
- Gradle **9.7.1** wrapper
- Plugin **`net.fabricmc.fabric-loom`** (not `fabric-loom` / remap)
- No Yarn, no `mappings` dependency, no `modImplementation`
- `implementation` Fabric Loader **0.19.5**, Fabric API **0.161.0+26.2**
- Minecraft **26.2**, Java `>=25`, `minecraft ~26.2`

### 4.2 Language / names

Source uses **Mojang unobfuscated names**. Typical replacements for this repo:

```
Identifier.of                  → Identifier.fromNamespaceAndPath
RegistryKey                    → ResourceKey
RegistryKeys                   → net.minecraft.core.registries.Registries
Registries (Yarn)              → BuiltInRegistries
RegistryEntry                  → Holder
Item.Settings                  → Item.Properties
ItemGroups / ItemGroupEvents   → CreativeModeTabs / CreativeModeTabEvents
World / ServerWorld            → Level / ServerLevel
PlayerEntity                   → Player
getWorld()                     → level()
PersistentProjectileEntity     → AbstractArrow (package ...projectile.arrow)
FlyingItemEntityRenderer       → ThrownItemRenderer (verify)
render(state, pose, buffers, light) → submit(state, pose, SubmitNodeCollector, CameraRenderState)
NbtCompound                    → CompoundTag / ValueInput
Text                           → Component
MatrixStack                    → PoseStack
ActionResult / Hand            → InteractionResult / InteractionHand
SpawnGroup                     → MobCategory
DataTracker                    → SynchedEntityData
```

### 4.3 Runtime constraints

- Java 25 only.
- Game is unobfuscated; crash reports show real names.
- Rendering may be OpenGL or experimental Vulkan; only Blaze3D/submit APIs.
- `ItemStack` needs registry/world; use `ItemStackTemplate` for recipe/advancement/datagen results.
- Packs: data **107.1**, resources **88.0**. `pack.mcmeta` uses `min_format` / `max_format` (since 1.21.9), not a single `pack_format` as the only field.
- Item GUI models: `assets/.../items/*.json` + models; item textures on **items** atlas.
- Enchantments remain datapack-driven.

### 4.4 Behaviour to preserve (product, not API)

Unchanged design from README / current code:

- Per-material melee damage, throw damage, throw velocity, durability, enchantability, repair tags.
- Charge throw (min ticks, lerp to full charge), pitch compensation, throwing_speed multiplier.
- Loyalty-style return with custom motion, pickup rules, melee durability, trident-like use animation.
- Creative tab: combat.
- Recipes: diagonal stick + material (netherite smithing JSON already).

26.2 does not require changing those numbers. Copper may now have vanilla tools; optional later.

---

## 5. File-by-file port checklist

Do these in order. After mappings/renames, compile with `gradlew.bat compileJava` and fix against genSources (Vineflower 26.2).

### 5.1 `DaggerCrafting.java`

Entrypoint only. No Minecraft types besides the Fabric initializer. Likely compiles once Fabric imports resolve.

### 5.2 `ModEnchantments.java`

`RegistryKey` → `ResourceKey`, `RegistryKeys.ENCHANTMENT` → `Registries.ENCHANTMENT`, `Identifier.of` → `fromNamespaceAndPath`. Registration method can stay a log line; JSON registers the enchantment.

### 5.3 `DaggerToolMaterial.java`

Tag / identifier / `BlockTags` renames only. Confirm `INCORRECT_FOR_*` still exist.

### 5.4 `ModItems.java`

- `Item.Settings` → `Item.Properties`
- `Items.register` signature
- `ItemGroups.COMBAT` → `CreativeModeTabs.COMBAT`
- `ItemGroupEvents.modifyEntriesEvent` → `CreativeModeTabEvents.modifyOutputEvent` + `accept`
- `FabricLoader.isModLoaded` unchanged

### 5.5 `item/DaggerItem.java`

Highest Java complexity after the entity.

- Extend `Item`, implement 26.2 `ProjectileItem` (or whatever Trident uses).
- Copy **current** `TridentItem` for use / release / riptide / spawn.
- Re-apply charge curve, throwing_speed lookup via `Holder<Enchantment>`, inventory consume, tooltip.
- Replace every Yarn type in the import list (section 1.3).
- Tooltip: verify `appendTooltip` still exists; else component appender.

### 5.6 `ModEntities.java`

Builder method names + `BuiltInRegistries.ENTITY_TYPE` + `MobCategory.MISC` + `ResourceKey`. Keep 1:1 entity type per dagger id (renderer map keys).

### 5.7 `entity/DaggerEntity.java`

- Superclass `AbstractArrow` from `...projectile.arrow`.
- Diff `ThrownTrident` for tracker, loyalty, hit, pickup, save data.
- Re-apply material ranged damage, custom return motion, fire, knockback, orientation from velocity, high-speed sync flags.
- NBT → `ValueInput`/`ValueOutput` if that is what `AbstractArrow` uses.
- Remove unused imports (`SpectralArrowItem`, `TridentEntity` if unused).

### 5.8 `entity/DaggerEntityRenderState.java`

Extend 26.2 thrown-item render state class. Keep extra floats.

### 5.9 `entity/DaggerEntityRenderer.java`

- `submit` not `render`.
- `SubmitNodeCollector` + `CameraRenderState`.
- Item state `submit` not `render`.
- Resolver from `EntityRendererProvider.Context`.
- Fabric `EntityRendererRegistry.register` in client init.

### 5.10 `DaggerCraftingClient.java`

Registry class name only.

### 5.11 `mixin/EnchantmentMixin.java`

Rebind method names from 26.2 `Enchantment`. Prefer replacing with item tags. If mixin stays, match enchantment id, not `toString()`.

### 5.12 `mixin/ExampleMixin.java`

Delete. Remove from `daggercrafting.mixins.json` if listed (currently not listed).

### 5.13 JSON / assets

| File | Action |
| --- | --- |
| `fabric.mod.json` | Already 26.2. Keep `fabric-api` not `fabric`. |
| `daggercrafting.mixins.json` | `JAVA_25`. Mixin list. |
| `assets/.../items/*.json` | Keep `type: model`. Optional `oversized_in_gui` (1.21.6+) only if needed. |
| `models/item/*.json` | Keep. Item atlas rule (1.21.11): textures from `textures/item`. |
| `textures/item/*.png` | Keep. `.xcf` is not shipped as a game asset requirement. |
| `lang/en_us.json` | Keep. |
| `recipe/*.json` | Shaped `type: minecraft:crafting_shaped` + `result.id` still valid; confirm 26.2 vanilla recipe examples. Netherite smithing JSON: match 26.2 vanilla netherite upgrade recipe (template field names moved more than once since 1.20). |
| `tags/item/*.json` | Keep `values`. Optional Fabric `fabric:remove` (26.2) unused. |
| `enchantment/throwing_speed.json` | Diff a 26.2 vanilla enchantment JSON. |
| `tags/enchantment/in_enchanting_table.json` | Keep. |
| `pack.mcmeta` if present | `min_format` / `max_format` for 88.0 resources / 107.1 data as required by Loom-generated pack metadata. |

### 5.14 Datagen

Empty. Ignore until you generate tags/recipes. Then use 26.1 names (`FabricTagsProvider`, …) and `ItemIds`-style keys if you mirror vanilla datagen.

---

## 6. Pack format cheat sheet (data this mod cares about)

| Version | DP | RP | Notes that can break *this* mod’s JSON |
| --- | --- | --- | --- |
| 1.21.5 | 71 | 55 | Baseline. Item models already “new”. Enchantments data-driven. |
| 1.21.6 | 80 | 63 | `attribute_modifiers` entries may include `display`. Item model `oversized_in_gui`. Strict JSON. Core shader uniform blocks (only if we shipped shaders — we don’t). |
| 1.21.7–8 | 81 | 64 | No item/enchant schema change that we use. |
| 1.21.9–10 | 88.0 | 69.0 | **`pack.mcmeta` `min_format`/`max_format`**. Minor pack versions. OpenGL 3.3. `chain` → `iron_chain` (not our textures). |
| 1.21.11 | 94.1 | 75.0 | **Items atlas split.** Multi-axis model rotation allowed. `attack_range` component exists (we don’t have to use it). |
| 26.1 | 101.1 | 84.0 | Item model `transformation` on sub-models. Entity texture path reshuffle (our item textures unaffected). Trades data-driven (unused). |
| 26.2 | 107.1 | 88.0 | Entity predicate strictness (our datapack has none). Shader merges (unused). |

---

## 7. Fabric-only timeline (modules / build)

| Drop | Loom (then) | Loader (then) | Breaking Fabric bits that affect this repo |
| --- | --- | --- | --- |
| 1.21.6 | 1.10 | 0.16.14 | Removed ancient modules (commands v0, keybindings v0, …) — unused. `BlockRenderLayerMap` move — unused. HUD rewrite — unused. `ComponentTooltipAppenderRegistry` — optional for dagger tooltip. `FabricTrackedDataRegistry` — **optional upgrade** for `LOYALTY`/`ENCHANTED` handlers if we registered custom serializers (we use built-in BYTE/BOOLEAN, so vanilla registry is fine). |
| 1.21.9 | 1.11 | 0.17.2 | `Entity#getWorld` → `getEntityWorld`. ResourceLoader rewrite — unused. MixinExtras 5. World render events temporarily gone — unused. |
| 1.21.11 | 1.14 | 0.18.1 | Last Yarn. Start migrating mappings *before* 26.1 if you want a two-step port. |
| 26.1 | 1.15 | 0.18.4 | **Must** switch Loom plugin id. **Must** Java 25. **Must** official names. Fabric API class rename dump (item groups, tracked data registry → `FabricEntityDataRegistry`, `FabricItem.Settings` → `Properties`, world→level event names). `fabric` mod id gone. Conventional tags v1 / loot v2 gone — unused. |
| 26.2 | 1.17+ | 0.19.3+ | Enum extensions — unused. Tag `fabric:remove` — unused. `Gui`/`Hud` split — unused unless we set screens. |

Recommended port strategy for *this* repo: **one jump** 1.21.5 Yarn → 26.2 official, using this file + 26.2 genSources + vanilla `TridentItem` / `ThrownTrident` / `ThrownItemRenderer` as templates. An intermediate 1.21.11 Yarn port would duplicate renderer `submit` work and still require a mappings migrate.

If you *do* two-step: (1) 1.21.9/1.21.11 Yarn: `getEntityWorld`, `submit` renderer, pack.mcmeta; (2) Loom migrateMappings / Ravel Yarn→official; (3) 26.1 plugin + `ItemStackTemplate` + Fabric API renames; (4) 26.2 Ids/Gui if compile errors remain.

---

## 8. Suggested lookup: vanilla 26.2 files to keep open

After `genSources`, these are the classes to read while editing. Names are official:

- `net.minecraft.world.item.TridentItem`
- `net.minecraft.world.entity.projectile.arrow.ThrownTrident`
- `net.minecraft.world.entity.projectile.arrow.AbstractArrow`
- Thrown item renderer + render state (search `ThrownItem` / `FlyingItem` under `client.renderer.entity`)
- `net.minecraft.world.item.Item` / `Item.Properties`
- `net.minecraft.world.item.enchantment.Enchantment` (mixin targets)
- `net.minecraft.world.item.enchantment.EnchantmentHelper`
- `net.minecraft.world.item.Items` (register helpers)
- `net.minecraft.core.registries.BuiltInRegistries` / `Registries`
- One vanilla enchantment JSON under the 26.2 jar `data/minecraft/enchantment/`
- One vanilla item definition `assets/minecraft/items/trident.json`

---

## 9. Sources

- Fabric: [1.21.6 post](https://fabricmc.net/2025/06/15/1216.html), [1.21.9](https://fabricmc.net/2025/09/23/1219.html), [1.21.11](https://fabricmc.net/2025/12/05/12111.html), [26.1](https://fabricmc.net/2026/03/14/261.html), [26.2](https://fabricmc.net/2026/06/15/262.html)
- Fabric Docs: [Porting to 26.2](https://docs.fabricmc.net/develop/porting/), [Loom plugin ids](https://docs.fabricmc.net/develop/loom/), [26.1 Fabric API renames](https://github.com/FabricMC/fabric-docs/blob/32c322e4/26.1/develop/porting/fabric-api.md), [mappings / Yarn retirement](https://github.com/FabricMC/fabric-docs/blob/ca505cdd/develop/porting/mappings/index.md)
- Minecraft Wiki: [version history](https://minecraft.wiki/w/Java_Edition_version_history), [1.21.5](https://minecraft.wiki/w/Java_Edition_1.21.5), [1.21.6](https://minecraft.wiki/w/Java_Edition_1.21.6), [1.21.9](https://minecraft.wiki/w/Java_Edition_1.21.9), [1.21.11](https://minecraft.wiki/w/Java_Edition_1.21.11), [26.1](https://minecraft.wiki/w/Java_Edition_26.1), [26.2](https://minecraft.wiki/w/Java_Edition_26.2), [pack format](https://minecraft.wiki/w/Pack_format)
- NeoForged primers (vanilla names): [1.21.8→1.21.9](https://docs.neoforged.net/primer/docs/1.21.9/), [1.21.11](https://docs.neoforged.net/primer/docs/1.21.11/), [26.1](https://docs.neoforged.net/primer/docs/26.1/), [26.2](https://docs.neoforged.net/primer/docs/26.2/)
- [Stonecutter Minecraft changes](https://stonecutter.kikugie.dev/wiki/v2/guides/tools/minecraft-changes)
- [MCUtility version table](https://mcutility.cc/versions/) (protocol / pack numbers)

When genSources disagrees with this file, **genSources wins**. Vanilla refactors after a primer was written are possible; 26.2 source in your Loom cache is the compiler’s truth.

---

## 10. Honest gaps

These were not fully line-diffed against 26.2 jars (primers + Fabric/Wiki cover them, signatures must still be checked):

- Exact `ProjectileItem` method names on 26.2.
- Exact `Item.canMine` / `postDamageEntity` / `onStoppedUsing` official names and return types.
- Exact thrown-item renderer class simple name (ThrownItem vs FlyingItem) in unobfuscated 26.2.
- Exact `Enchantment` mixin method descriptors.
- Exact `Items.register` overload.
- Netherite dagger smithing recipe field names vs 26.2 vanilla upgrade recipe.

Those are “open the vanilla file” items, not missing historical drops. Nothing else this repository imports was left off the timeline on purpose.
