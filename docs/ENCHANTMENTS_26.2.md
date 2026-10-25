# Minecraft 26.2 enchantments (A–Z)

Reference for **this repo** on **Minecraft 26.2** (official names, data pack format **107.1**). Use it instead of re-reading vanilla jars and wiki pages that mix 1.20 and 1.21.

**Authoritative sources (re-verify only on a version bump):**

- Sources: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.2/*-sources.jar` → `net/minecraft/world/item/enchantment/Enchantment.java`, `EnchantmentHelper.java`
- Data: `~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar` → `data/minecraft/enchantment/`, `data/minecraft/tags/enchantment/`, `data/minecraft/tags/item/enchantable/`
- If genSources disagrees with this file, **genSources wins**.

Yarn names such as `isAcceptableItem` are **wrong for 26.2**. The methods are `canEnchant` and `isPrimaryItem`.

---

## 1. Mental model

Enchantments are **reloadable datapack registry entries**, not Java-registered objects.

```text
JSON file  →  Registries.ENCHANTMENT  (id = namespace:path from file location)
     │
     ├─ definition: which items, weight, levels, costs, slots
     ├─ exclusive_set: incompatibility with other enchantments
     └─ effects: optional vanilla *hooks* (damage, projectile_spawned, …)

Item.Properties.enchantable(n)  →  DataComponents.ENCHANTABLE on the stack
     │
     └─ without this, the enchanting table returns cost 0 / empty rolls

Item tags (#minecraft:enchantable/…)  ←  referenced by each enchantment JSON
Enchantment tags (#minecraft:in_enchanting_table, #treasure, …)
     │
     └─ table pool, loot, trades, curse styling — not “can this item exist”
```

Java holds a **`ResourceKey<Enchantment>`** (this mod: `ModEnchantments.THROWING_SPEED`) and looks the entry up from `registryAccess` after the world/datapack is loaded. `ModEnchantments.registerModEnchantments()` does **not** register anything.

Gameplay can be:

| Kind | Where it lives | Example |
| --- | --- | --- |
| Data-driven effect | `effects` in JSON | Loyalty → `minecraft:trident_return_acceleration`; Flame → `projectile_spawned` ignite |
| Java that *reads* the enchantment | Item/entity code | Throwing Speed multipliers in `DaggerItem.releaseUsing` |
| Java that *bypasses* item tags | Mixin on `canEnchant` / `isPrimaryItem` | Vanilla Loyalty on daggers (vanilla JSON still says `#enchantable/trident`) |

Those three layers are independent. An enchantment can exist, appear in the table, and still do nothing until some effect or Java consumes it.

---

## 2. How an enchantment is registered

**Path:** `data/<namespace>/enchantment/<path>.json`  
**Id:** `<namespace>:<path>`  
Example: `data/daggercrafting/enchantment/throwing_speed.json` → `daggercrafting:throwing_speed`.

There is **no** `Registry.register` for enchantments on 26.2. Builtin Fabric datapacks (files under `src/main/resources/data/`) merge with vanilla like any other datapack.

**Do not** ship `data/minecraft/enchantment/loyalty.json` (or any vanilla enchantment id) unless you intend to **replace the entire vanilla definition**. Registry entries do not merge; the last pack wins. That breaks tridents and other mods. Tags merge; enchantment JSON does not.

Lang is separate: `assets/<namespace>/lang/en_us.json` key `enchantment.<namespace>.<path>`. Vanilla descriptions use that pattern. It is **not** how the registry identifies the enchantment (the file path is).

---

## 3. Enchantment JSON (codec)

`Enchantment` is a record:

```text
Enchantment(Component description, EnchantmentDefinition definition, HolderSet exclusiveSet, DataComponentMap effects)
```

### 3.1 Required fields

From `Enchantment.DIRECT_CODEC` and `EnchantmentDefinition.CODEC`:

| JSON key | Role |
| --- | --- |
| `description` | Text component (usually `{ "translate": "enchantment.ns.path" }`) |
| `supported_items` | Item id, `#tag`, or list — **anvil / `/enchant` / “can this apply”** |
| `weight` | Integer **1–1024** — table *weighted random* among eligible candidates |
| `max_level` | Integer **1–255** |
| `min_cost` | `{ "base": int, "per_level_above_first": int }` |
| `max_cost` | same shape |
| `anvil_cost` | Non-negative int (anvil XP cost contribution) |
| `slots` | Equipment slot groups, e.g. `["mainhand"]`, `["any"]` |

Cost for level `n` (1-based):

```text
min_enchant_level(n) = min_cost.base + (n - 1) * min_cost.per_level_above_first
max_enchant_level(n) = max_cost.base + (n - 1) * max_cost.per_level_above_first
```

A table roll at modified enchant level `L` can offer level `n` only if `min_enchant_level(n) ≤ L ≤ max_enchant_level(n)`. The helper walks **from max level down** and takes the first level that fits.

### 3.2 Optional fields

| JSON key | Default | Role |
| --- | --- | --- |
| `primary_items` | same as `supported_items` | **Enchanting table** eligibility for non-books |
| `exclusive_set` | empty | Enchantment id, `#tag`, or list of incompatibles |
| `effects` | **empty map** | Vanilla effect components |

Codec line (26.2):

```text
EnchantmentEffectComponents.CODEC.optionalFieldOf("effects", DataComponentMap.EMPTY)
```

**`effects` is not required.** Vanilla `fortune.json` has no `effects` key and still loads. An empty `projectile_spawned` / `all_of` stub does **not** register the enchantment, put it in the table, or create books. Only the **file + required fields** do that.

Unknown keys (this repo used `"treasure": false`) are **ignored**. Treasure vs table is **`#minecraft:treasure` / `#minecraft:non_treasure` / `#minecraft:in_enchanting_table`**, not a boolean on the enchantment.

### 3.3 `supported_items` vs `primary_items`

Vanilla examples:

| Enchantment | `supported_items` | `primary_items` |
| --- | --- | --- |
| Sharpness | `#enchantable/sharp_weapon` | `#enchantable/melee_weapon` |
| Smite / Bane | `#enchantable/weapon` | `#enchantable/melee_weapon` |
| Fire Aspect | `#enchantable/fire_aspect` | `#enchantable/melee_weapon` |
| Loyalty | `#enchantable/trident` | omitted (defaults to supported) |
| Flame | `#enchantable/bow` | omitted |
| Unbreaking / Mending | `#enchantable/durability` | omitted |

Axes can get Sharpness at the **anvil** (supported via `sharp_weapon`) but the **table** uses `isPrimaryItem`, which requires `melee_weapon`. Same split for any custom item: anvil and table are two different checks.

---

## 4. Vanilla Java gates (the only checks that matter)

```text
canEnchant(ItemStack)
  → definition.supportedItems().contains(itemStack.typeHolder())
  // item *type* only. No slots, no exclusive set, no primary_items.

isSupportedItem(ItemStack)
  → item.is(supportedItems)   // tag/id membership on the stack

isPrimaryItem(ItemStack)
  → isSupportedItem(item)
    && (primaryItems empty || item.is(primaryItems))

areCompatible(Holder a, Holder b)
  → a != b
    && !a.exclusiveSet.contains(b)
    && !b.exclusiveSet.contains(a)
```

Call sites (26.2):

| Surface | Method | Extra |
| --- | --- | --- |
| Anvil apply / combine | `canEnchant` | Creative / books often forced compatible in `AnvilMenu` |
| `/enchant` | `canEnchant` | |
| Enchanting table candidates | `isPrimaryItem` **or** stack is `Items.BOOK` | Pool is `#in_enchanting_table` |
| Random loot / some trades | tags `#on_random_loot`, `#tradeable` | plus `canEnchant` in several loot functions |

`exclusive_set` is **not** read by `canEnchant`. Two incompatible enchantments can both be *legal on the item type*; the anvil and table *combination* step calls `areCompatible`.

---

## 5. Item side: `DataComponents.ENCHANTABLE`

`EnchantmentHelper.getEnchantmentCost` / `selectEnchantment`:

```text
Enchantable enchantable = itemStack.get(DataComponents.ENCHANTABLE);
if (enchantable == null) → cost 0 / empty list
```

On items this is set in Java with `.enchantable(int)` on `Item.Properties` (this mod: `DaggerItem.applyDaggerSettings` → material enchantability). The integer is **enchantability** (the old “enchantability” stat), used to jitter the table cost:

```text
cost += 1 + rand(enchantability/4+1) + rand(enchantability/4+1)
then ±15% noise, clamped ≥ 1
```

Without `.enchantable(...)`, the table will not enchant the item even if every datapack tag is perfect.

Books are special: they have enchantable handling and `getAvailableEnchantmentResults` treats `Items.BOOK` as passing the primary-item filter.

---

## 6. Enchantment tags (pools, not item membership)

Vanilla `data/minecraft/tags/enchantment/`:

| Tag | 26.2 meaning |
| --- | --- |
| `#in_enchanting_table` | Vanilla file is `{ "values": ["#minecraft:non_treasure"] }`. **Table pool.** |
| `#non_treasure` | Explicit list of table-able vanilla ids (sharpness, loyalty, flame, …). **Does not include Mending.** |
| `#treasure` | Mending, curses, frost walker, soul speed, … |
| `#on_random_loot` | Chest-style random enchants |
| `#tradeable` | Librarian-style trades |
| `#curse` | Red name styling |
| `#exclusive_set/*` | Groups referenced from enchantment JSON `exclusive_set` |

This repo **extends** the table pool:

`data/minecraft/tags/enchantment/in_enchanting_table.json`:

```json
{ "replace": false, "values": [ "daggercrafting:throwing_speed" ] }
```

`replace: false` (or omitted) **merges**. `replace: true` **wipes** vanilla `#non_treasure` from that tag for this pack — never do that unless you re-list everything.

Custom enchantments are **not** automatically in `#non_treasure`. If you only add the enchantment JSON and forget this tag, it still exists (creative books, `/enchant` if `canEnchant`) but **will not appear in the enchanting table**.

Survival enchanted books from loot/librarians need `#on_random_loot` / `#tradeable` as well. This mod does **not** add throwing_speed there today; table + creative books are enough.

---

## 7. Item tags (`#minecraft:enchantable/…`)

These are **only** consumed because enchantment JSON points at them. Java does not special-case “this is a sword” for Sharpness; it checks the `HolderSet` from JSON.

26.2 nesting (simplified):

```text
#enchantable/melee_weapon  →  #swords, #spears
#enchantable/sharp_weapon  →  #melee_weapon, #axes
#enchantable/weapon        →  #sharp_weapon, mace
#enchantable/fire_aspect   →  #melee_weapon, mace
#enchantable/trident       →  trident
#enchantable/bow           →  bow
#enchantable/durability    →  armor, tools, swords, bow, trident, mace, spears, …
#enchantable/sweeping      →  #swords
```

**Tag merge:** adding `#daggercrafting:daggers` to a vanilla item tag with `replace: false` does **not** remove swords from that tag. It **does** make every enchantment whose `supported_items` / `primary_items` uses that tag legal on daggers.

That is the **leak** problem.

---

## 8. Enchanting table pipeline

1. Item must have `ENCHANTABLE`.
2. Bookshelves cap at 15; slot 0/1/2 get different `getEnchantmentCost` rolls.
3. Candidate stream = holders in `#minecraft:in_enchanting_table`.
4. Keep if `isPrimaryItem(stack)` **or** stack is a book.
5. For each remaining enchantment, pick the **highest** level whose cost window contains the modified `L`.
6. Weighted pick: `EnchantmentInstance.weight()` = JSON `weight` (not per-level).
7. Extra enchants: while `rand(50) <= remainingCost`, add another compatible candidate (`areCompatible`), then `cost /= 2`.

So **rarity** is `weight` among whatever is still eligible at that `L`, **and** `min_cost`/`max_cost` windows. Weight is **not** per level. Loyalty III is rarer than Loyalty I because III’s min cost is higher (vanilla: I `12–50`, II `19–50`, III `26–50`), not because weight changes.

---

## 9. Anvil, books, creative

- **Anvil on a tool:** `canEnchant` (`supported_items` only). Mending is treasure → not in the table tag, but still `canEnchant` durability items.
- **Anvil book → item:** same `canEnchant` plus `areCompatible` with existing enchants.
- **Creative enchanted books:** generated from the **whole enchantment registry**, not from `#in_enchanting_table`. Any successfully loaded JSON gets a creative book.
- **`/enchant`:** `canEnchant`.

---

## 10. Exclusive sets (incompatibility)

JSON `exclusive_set` is stored on **that** enchantment. `areCompatible` is **symmetric**: A blocks B if A lists B **or** B lists A.

Vanilla Loyalty does **not** list Riptide on itself; Riptide’s tag `#exclusive_set/riptide` contains Loyalty and Channeling. That is enough for both directions.

Damage family: Sharpness/Smite/Bane JSON use `"exclusive_set": "#minecraft:exclusive_set/damage"`.

To make a **custom** enchantment incompatible with Loyalty **without** editing vanilla `loyalty.json`, put `"exclusive_set": "minecraft:loyalty"` on the **custom** file. Vanilla Loyalty’s exclusive set stays empty of your id, but `areCompatible` still fails because your set contains Loyalty.

Editing vanilla `#exclusive_set/riptide` with `replace: false` to add your id would also work and would not wipe Loyalty/Channeling. Do **not** `replace: true` that tag.

---

## 11. Effects vs custom Java

`effects` maps component types such as:

- `minecraft:damage` — Sharpness/Smite (value effects; `EnchantmentHelper.modifyDamage`)
- `minecraft:trident_return_acceleration` — Loyalty (this mod reads it via `getTridentReturnToOwnerAcceleration` on the thrown dagger)
- `minecraft:projectile_spawned` — Flame ignite on bow spawn; vanilla `Projectile.spawnProjectile` calls `EnchantmentHelper.onProjectileSpawned`
- `minecraft:post_attack` — Fire Aspect (often `is_direct` so melee-only)
- `minecraft:item_damage` — Unbreaking
- `minecraft:repair_with_xp` — Mending

If there is **no** vanilla component for your mechanic (dagger charge throw speed), put **no** `effects` (or omit the key) and implement in Java by looking up the `ResourceKey` and `EnchantmentHelper.getItemEnchantmentLevel`.

Empty `all_of` stubs do not keep the enchantment “alive.” They only parse if present.

This mod’s throw path uses `Projectile.spawnProjectileFromRotation` with the thrown stack; a real `projectile_spawned` effect **would** run on player throw for enchants **on that stack**. Dispenser spawn in this codebase constructs `DaggerEntity` directly and **skips** that hook. Flame on daggers is handled in Java (`isOnFire` on the entity), not by putting daggers in `#enchantable/bow`.

---

## 12. Custom items (daggers): how to allow vanilla enchants without leaks

Vanilla enchantments will **never** see `#daggercrafting:daggers` unless you change **their** `supported_items` (forbidden: overwrite JSON) or the **item tags they already point at**.

### 12.1 Option A — Extend vanilla item tags (`replace: false`)

Example: add `#daggercrafting:daggers` to `#minecraft:enchantable/melee_weapon`.

| You wanted | You also get |
| --- | --- |
| Sharpness/Smite/Bane/Fire Aspect on the **table** (they use `primary_items: melee_weapon`) | **Knockback and Looting** (same tag) |
| Unbreaking via `#enchantable/durability` | **Vanishing Curse** on anvil (`#enchantable/vanishing` includes durability tools) |
| Loyalty via `#enchantable/trident` | **Riptide, Channeling, Impaling** |
| Flame via `#enchantable/bow` | **Power, Punch, Infinity** |

Swords/tridents/bows are **unchanged** (merge). Daggers and **any third-party enchant** that uses those tags **are** changed. This is the incompatibility surface.

Do **not** add daggers to `#minecraft:swords` unless you want Sweeping Edge and every sword-only check.

### 12.2 Option B — Mixin allow-list (what this mod does)

Inject `Enchantment#canEnchant` and `#isPrimaryItem` at `HEAD`, only if `stack.getItem() instanceof DaggerItem`, and force `true` for a **closed list** of enchantment ids.

That does **not** rewrite vanilla JSON or tags. Tridents still use vanilla Loyalty. Extra vanilla enchants stay off daggers.

**Matching:**

- **Wrong:** `toString()`, display name, or `path.contains("smite")` (false positives; locale).
- **Current code:** last segment of the **translate key** (`enchantment.minecraft.loyalty` → `loyalty`) plus `contains`. Works for vanilla/this mod; brittle.
- **Preferred:** parse `enchantment.<namespace>.<path>` to `Identifier` and `equals` a set of ids (`minecraft:loyalty`, …), or compare `ResourceKey` via a `Holder` lookup. `Enchantment` is a **record** and does not store its registry key; `equals` is value-based, so do not use a `HashMap<Enchantment, …>` keyed by value equality across reloads without care.

Keep **two lists**: table (`isPrimaryItem`) without Mending; anvil (`canEnchant`) with Mending.

A custom enchantment that already sets `supported_items` / `primary_items` to `#daggercrafting:daggers` does **not** need the mixin.

### 12.3 Option C — Override vanilla enchantment JSON

Forbidden for selective dagger support: you must copy the full definition and you replace it globally (tridents lose Loyalty if you only list daggers).

---

## 13. Custom enchantments (checklist)

1. `data/<mod>/enchantment/<id>.json` with required fields. `supported_items` / `primary_items`: your item tag.
2. `assets/<mod>/lang/en_us.json` → `enchantment.<mod>.<id>`.
3. `ResourceKey` in Java if gameplay reads the level.
4. Table: append id to `data/minecraft/tags/enchantment/in_enchanting_table.json` with `"replace": false`.
5. Optional loot/trades: `#on_random_loot`, `#tradeable`.
6. Optional `exclusive_set` pointing at vanilla ids (do not replace vanilla enchantment files).
7. Implement `effects` **or** Java. Omit `effects` if unused.
8. Target item: `.enchantable(int)` on item properties.
9. Do **not** add the item to a broad vanilla `#enchantable/*` tag just to “make custom enchants work.” Point **your** JSON at **your** tag.

---

## 14. What DaggerCrafting does today (26.2)

| Piece | Location | Role |
| --- | --- | --- |
| Item tag | `data/daggercrafting/tags/item/daggers.json` | All dagger ids |
| Custom enchant | `data/daggercrafting/enchantment/throwing_speed.json` | Metadata + costs; speed is Java |
| Table merge | `data/minecraft/tags/enchantment/in_enchanting_table.json` | Offers throwing_speed |
| Keys | `ModEnchantments.THROWING_SPEED` | Lookup only |
| Enchantability | `DaggerItem` `.enchantable(material.getEnchantability())` | Table rolls |
| Vanilla on daggers | `EnchantmentMixin` | Loyalty, Sharpness, Smite, Bane, Fire Aspect, Flame, Unbreaking, Mending (anvil) |
| Loyalty motion | `DaggerEntity` + vanilla `trident_return_acceleration` | Runtime |
| Flame on throw | Entity on-fire path | Not bow tag |

**Intentionally not allowed** on daggers: Knockback, Looting, Sweeping Edge, Riptide, Channeling, Impaling, Power, Punch, Infinity, tool enchants, etc.

**Do not** extend `#enchantable/melee_weapon`, `#trident`, `#bow`, or `#durability` unless product explicitly accepts the leaks in section 12.1.

---

## 15. Pack format

`pack.mcmeta` in this repo: resource `min_format` `[88, 0]`, data `max_format` `107` (26.2). Enchantment JSON layout above is the 1.21+ model and is what 26.2 loads. 26.3 bumps pack numbers; re-diff a vanilla enchantment JSON and mixin targets (`canEnchant`, `isPrimaryItem`) on port.

---

## 16. Quick “why is it missing / leaking?” 

| Symptom | First check |
| --- | --- |
| Custom enchant missing entirely | JSON parse error; required field; wrong path `data/.../enchantment/` |
| Exists in creative books, not on table | `#in_enchanting_table` merge; `isPrimaryItem` / `primary_items`; item `.enchantable` |
| Table empty on custom item | `DataComponents.ENCHANTABLE` missing |
| Vanilla enchant missing on custom item | Vanilla `supported_items` tag does not include you; need mixin or a **narrow** tag extend |
| Extra vanilla enchants on custom item | You extended a shared `#enchantable/*` tag |
| Anvil yes, table no | `primary_items` stricter than `supported_items` (vanilla Sharpness on axes) |
| Two enchants combine that should not | `exclusive_set` only on one side **and** the other does not list you — actually `areCompatible` is bidirectional; if they still combine, exclusive_set did not load |
| Loyalty works on dagger but you edited `loyalty.json` | You replaced vanilla; tridents may be broken — revert |
| Stub `effects` removed, enchant gone | Unrelated syntax error in the same file; `effects` is optional |

---

## 17. File map (vanilla 26.2 + this mod)

**Vanilla Java:** `Enchantment`, `EnchantmentHelper`, `EnchantmentInstance`, `AnvilMenu`, `EnchantmentMenu`  
**Vanilla data:** `data/minecraft/enchantment/*.json`, `tags/enchantment/*.json`, `tags/item/enchantable/*.json`  
**This mod:** `throwing_speed.json`, `tags/item/daggers.json`, `tags/enchantment/in_enchanting_table.json`, `ModEnchantments.java`, `EnchantmentMixin.java`, `DaggerItem.java`, `DaggerEntity.java`
