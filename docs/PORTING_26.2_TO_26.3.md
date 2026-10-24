# DaggerCrafting porting reference: Minecraft / Fabric 26.2 → 26.3

Short lookup for bumping **this** repo from **Minecraft 26.2** (Chaos Cubed) to **26.3** (Wilderness Bound). For the full 1.21.5 → 26.2 history, see [PORTING_1.21.5_TO_26.2.md](./PORTING_1.21.5_TO_26.2.md). For which Git branch to use (`base/26.2`, `base/26.3`, `feature/*`), see [BRANCHING_WORKFLOW.md](./BRANCHING_WORKFLOW.md).

26.3 is a large vanilla drop (worldgen, registries, rendering stack, SDL input). Most of that does **not** touch thrown daggers. This file filters to what `src/main/java` and our datapack actually use.

**How to use it**

1. Bump toolchain versions in `gradle.properties` / `build.gradle` (section 2).
2. Fix compile errors using section 3 (known renames for this mod).
3. Re-validate JSON in section 4.
4. Run `gradlew.bat compileJava` and in-game smoke test (throw, return, enchant, creative tab).

When genSources disagrees with this file, **genSources wins**.

Sources: [Fabric for 26.3](https://fabricmc.net/2026/09/15/263.html), [NeoForged 26.2→26.3 primer](https://docs.neoforged.net/primer/docs/26.3/), [Fabric API releases](https://github.com/FabricMC/fabric-api/releases), [26.3 release article](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3), community notes ([SpicesFire](https://spicesfire.com/blog/porting-a-fabric-mod-to-minecraft-26-3/)).

---

## 0. Baseline: what 26.2 already is (this repo)

| Field | 26.2 (current) | 26.3 (target) |
| --- | --- | --- |
| Game drop | Chaos Cubed | Wilderness Bound |
| Java | 25 | 25 |
| Obfuscation | No (official names) | Same |
| Data pack format | 107.1 | **118.0** (snap 10+) |
| Resource pack format | 88.0 | **97.0** (snap 10+) |
| Fabric Loader | 0.19.5 | 0.19.5+ (check [develop](https://fabricmc.net/develop)) |
| Fabric API | `0.161.0+26.2` | **`0.165.0+26.3`** (or newer on branch) |
| Loom | `1.18-SNAPSHOT` | Keep **1.18-SNAPSHOT** (or latest 1.18.x); needs **Gradle 9.7.1** wrapper |
| Plugin id | `net.fabricmc.fabric-loom` | Unchanged |

Already correct on 26.2 and usually unchanged for 26.3: datapack enchantments, item model definitions under `assets/.../items/`, `ThrownItemRenderer` + `submit`, `EntityTypes` constants, `CreativeModeTabEvents`, `EnchantmentMixin`.

---

## 1. What 26.3 changes that we can ignore

Safe to skip unless you add features later:

- **Fuel / compost / brewing fuel** → `DataComponents.COOKING_FUEL`, `COMPOSTABLE`, `BREWING_FUEL`; Fabric removed `FuelRegistry`, `CompostingChanceRegistry`, `FabricPotionBrewingBuilder`.
- **Block transformers** (hoe/shovel/axe) → datapack `block_transformer/` + `DataComponents.BLOCK_TRANSFORMER`; Fabric till/strip/flatten registries removed.
- **Worldgen** (configured features path, material rules, surface rules, number providers as registries).
- **SDL instead of GLFW** (keybinds, `TextInputManager`) — no custom screens or GLFW constants here.
- **Renderpearl split** (GPU pipelines, custom shaders, OIT) — we only use vanilla item submit path; see section 3.2 for the one `PoseStack` rename we hit.
- **Fluid flow API**, custom loot/datagen rewrites, explorer map items, new biomes/blocks.

---

## 2. Toolchain bump (work order)

1. `gradle.properties`: `minecraft_version=26.3`, `fabric_version=0.165.0+26.3` (or current from Fabric develop page).
2. Confirm `gradle/wrapper/gradle-wrapper.properties` stays on **9.7.1** (`gradle-9.7.1-bin.zip` — plain `9.7` is not a valid distribution).
3. `gradlew.bat genSources` (or full `build`) against 26.3.
4. `src/main/resources/pack.mcmeta`: raise **`max_format`** to cover 26.3 (data **118**, resources **97**). Example: `"max_format": 118` if you only track the data ceiling, or set both min/max consistently with how 26.2 was done (`min_format` tuple for resources, integer max for data).

Loader **0.19.5** is still cited for 26.3 on the Fabric blog; bump only if develop recommends a newer patch.

---

## 3. Java API changes that affect this mod

### 3.1 `entity/DaggerEntity.java` (expected compile break)

| 26.2 | 26.3 | Notes |
| --- | --- | --- |
| `this.hurtMarked = true` | `this.syncVelocity()` (or equivalent on `Entity`) | NeoForged primer: `hurtMarked` → `syncVelocity`. Used when delta movement is large so clients get velocity sync. Compare **`ThrownTrident#tick`** on 26.3 after genSources. |

No other entity fields from the 26.3 migration lists appear in our entity code (`invulnerableTime`, swing APIs, etc. are unused).

`EntityTypes.ENDERMAN` and registration via `EntityType.Builder` are already the 26.2 style; no `EntityType.VILLAGER`-style constant migration needed.

### 3.2 `entity/DaggerEntityRenderer.java`

| 26.2 | 26.3 |
| --- | --- |
| `poseStack.mulPose(Quaternionf)` | `poseStack.rotate(Quaternionf)` (NeoForged: `PoseStack#mulPose` → `rotate`) |

Two call sites (yaw/pitch + spin). Re-diff **`ThrownItemRenderer`** if `submit` signature or `ItemStackRenderState#submit` changes.

Low-level types (`SubmitNodeCollector`, `ItemModelResolver`, `CameraRenderState`) stayed on the Minecraft client packages in 26.2; only chase compile errors if Loom moves a import to `com.mojang.renderpearl.*` (unlikely for this renderer).

### 3.3 Everything else in `src/main/java`

| File | 26.3 expectation |
| --- | --- |
| `item/DaggerItem.java` | Re-copy patterns from **`TridentItem`** if `use` / `releaseUsing` / `ProjectileItem` changed. Watch **`LivingEntity#swing`** if you add arm swing with `SwingAnimation` (not used today). |
| `mixin/EnchantmentMixin.java` | Re-check `@Inject` targets on **`Enchantment`** after compile; vanilla enchantment Java changed around registry holders but our mixin is `isAcceptableItem` / `isPrimaryItem`. |
| `ModItems`, `ModEntities`, `ModEnchantments`, `DaggerToolMaterial`, clients | Usually version bump only. |
| `DaggerCraftingDataGenerator` | Empty stub — ignore until you generate data; 26.3 datagen for reloadable registries is a different beast (loot/advancements/recipes as registries). |

### 3.4 Fabric API (this mod)

Still used:

- `CreativeModeTabEvents` — unchanged in 26.3 blog.
- `EntityRendererRegistry` — confirm still present; fallback `EntityRenderers.register`.

Optional: `TooltipFlag#shouldDisplayAllInformation` if you gate dagger tooltip lines behind keybinds (not required).

Not used: removed fuel/compost/brewing/block-transformer Fabric registries.

---

## 4. Datapack and assets

| Asset | Action |
| --- | --- |
| `data/daggercrafting/enchantment/throwing_speed.json` | **Validate** against a 26.3 vanilla enchantment JSON (e.g. `loyalty.json` in the jar). Reloadable-registry work mostly affects loot/recipes/advancements, not our simple enchantment file — but re-run `/reload` and enchanting table test. |
| `data/daggercrafting/recipe/*.json` | Recipes are reloadable registries in 26.3; inline JSON should still load. If something fails, check log for holder/provider schema changes (unlikely for shaped recipes). |
| `data/minecraft/tags/enchantment/in_enchanting_table.json` | Unchanged pattern; verify tag still applies. |
| `assets/daggercrafting/items/*.json`, `models/item/*` | No explorer-map-style item split. Bump pack format only. |
| Custom shaders | **None** — ignore Renderpearl / OIT shader sections. |

Gameplay note (not a port task): 26.3 changes sun-burn protection enchant behavior on items; unrelated to throwing daggers unless you depend on that edge case.

---

## 5. File-by-file checklist (minimal)

1. **Gradle** — `minecraft_version`, `fabric_version`; build once.
2. **`pack.mcmeta`** — formats for 26.3.
3. **`DaggerEntity.java`** — `hurtMarked` → `syncVelocity` (or vanilla equivalent).
4. **`DaggerEntityRenderer.java`** — `mulPose` → `rotate`.
5. **Compile** — `gradlew.bat compileJava`; fix any new `TridentItem` / `AbstractArrow` / `Enchantment` diffs.
6. **Manual test** — creative combat tab, throw + pickup + loyalty return, `throwing_speed` levels, melee hit, enchant mixin (daggers in table).

---

## 6. Pack format cheat sheet

| Version | Data pack | Resource pack | Notes for this mod |
| --- | --- | --- | --- |
| 26.2 | 107.1 | 88.0 | Current `pack.mcmeta` targets through 107. |
| 26.3 | **118.0** | **97.0** | Snapshot 10+; update `max_format` / metadata before release. |

---

## 7. Vanilla classes to diff after genSources

Open beside our code while fixing:

- `net.minecraft.world.entity.projectile.arrow.ThrownTrident`
- `net.minecraft.world.entity.Entity` (velocity sync)
- `net.minecraft.world.item.TridentItem`
- `net.minecraft.client.renderer.entity.ThrownItemRenderer`
- `net.minecraft.world.item.enchantment.Enchantment` (mixin)
- One vanilla enchantment JSON under `data/minecraft/enchantment/`

---

## 8. Honest gaps

Not line-verified against a 26.3 jar in this workspace (primer + Fabric post + community port notes only):

- Exact signature of `Entity#syncVelocity` vs old `hurtMarked` assignment semantics.
- Whether `ThrownItemRenderer#submit` or `extractRenderState` renamed again.
- Whether `EnchantmentMixin` descriptor strings need updating.

Treat those as “open vanilla on 26.3 genSources” items — typically minutes, not a second full port.
