# Minecraft 26.3 attack range (melee reach)

Reference for **this repo** on **Minecraft 26.3** (official names, Fabric/Loom). Use it instead of re-reading vanilla jars that mix 1.20-era "reach" folklore (attribute `reach_distance`) with the current component model.

**Authoritative sources (re-verify only on a version bump):**

- Sources: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/*-sources.jar` → `net/minecraft/world/item/component/AttackRange.java`, `net/minecraft/world/item/Item.java`, `net/minecraft/world/entity/Mob.java`
- Data: `data/minecraft/enchantment/*` is unrelated to reach; reach has **no** JSON side in vanilla
- If genSources disagrees with this file, **genSources wins**

---

## 1. Mental model: three separate ranges

Vanilla does **not** have one "reach". Three independent values sit next to each other, and mixing them up is the usual source of confusing behaviour:

| Range | Where it lives | Survival | Creative | Gates |
| --- | --- | --- | --- | --- |
| Block interaction | `Attributes.BLOCK_INTERACTION_RANGE` | 4.5 | 5.0 | mining, placing, block use |
| Entity interaction | `Attributes.ENTITY_INTERACTION_RANGE` | 3.0 | 5.0 | right-click entity (trade, mount, leash, ...) |
| Attack range | `DataComponents.ATTACK_RANGE` or a runtime fallback | 3.0 | 5.0 | melee hits, attack indicator, crosshair entity pick |

The creative numbers exist because `ServerPlayer` adds transient modifiers: `+0.5` to block interaction and `+2.0` to entity interaction (`ServerPlayer.java`, `CREATIVE_*_INTERACTION_RANGE_MODIFIER`).

```text
Attributes (per-entity, always present)
  block_interaction_range  4.5 / 5.0   → useItemOn, mining, chests
  entity_interaction_range 3.0 / 5.0   → interactOn, trading, mounts
                                          └─ also the FALLBACK max of attack range

Item component (per-stack, optional)
  attack_range             → melee hit resolution only
```

---

## 2. The `AttackRange` record

```java
record AttackRange(float minReach, float maxReach, float minCreativeReach, float maxCreativeReach, float hitboxMargin, float mobFactor)
```

| Field | Codec default | Spear | Meaning |
| --- | --- | --- | --- |
| `min_reach` | 0.0 | 2.0 | inner dead zone (survival) |
| `max_reach` | 3.0 | 4.5 | outer limit (survival) |
| `min_creative_reach` | 0.0 | 2.0 | inner dead zone (creative) |
| `max_creative_reach` | 5.0 | 6.5 | outer limit (creative) |
| `hitbox_margin` | 0.3 | 0.125 | slack added on both sides of the window |
| `mob_factor` | 1.0 | 0.5 | scales min/max for non-player attackers |

**The defaults are not the sword.** They only apply when a datapack supplies a partial `attack_range`. A sword has no component at all and goes through `defaultFor`:

```java
// AttackRange.defaultFor(LivingEntity)
float interactionRange = (float) livingEntity.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
return new AttackRange(0.0F, interactionRange, 0.0F, interactionRange, 0.0F, 1.0F);
```

So the sword baseline is `(0, 3.0, 0, 3.0, 0, 1)`, i.e. **margin 0**, not 0.3. Any mod that writes a component should pass `hitboxMargin = 0` to keep sword semantics; leaving it out of a JSON file silently adds 0.3 of slack on each side.

The spear (`Items`/`Item.Properties.spear`) is the only vanilla item that sets one:

```java
.component(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F))
```

It is not a template for short weapons: its `min_reach` 2.0 is a deliberate dead zone for a thrusting polearm, and `mob_factor` 0.5 weakens mobs compared to the fallback's 1.0.

---

## 3. Where it is enforced

All four read the same component; there is no second copy of the value.

| Site | Class / method | Role |
| --- | --- | --- |
| Crosshair | `LocalPlayer.raycastHitResult` | picks the entity/block under the crosshair using the component, falls back to the vanilla pick on `MISS` |
| Client attack gate | `Minecraft.startAttack` | refuses `gameMode.attack(...)` when the target is out of the item's range |
| Server accept gate | `ServerGamePacketListenerImpl.handleAttack` | `player.isWithinAttackRange(mainHandItem, targetBounds, 3.0)`; the 3.0 is anti-cheat slack, not reach |
| UI and AI | `Hud` attack indicator, `Mob.isWithinMeleeAttackRange` | indicator suppression; mob melee decisions |

Resolution helper on the attacker:

```java
// LivingEntity.getAttackRangeWith(ItemStack)
AttackRange attackRange = weaponItem.get(DataComponents.ATTACK_RANGE);
return attackRange != null ? attackRange : AttackRange.defaultFor(this);
```

Distance is **eye → target bounding box**, not centre to centre:

```java
double distance = Math.sqrt(boundingBox::distanceToSqr, attacker.getEyePosition());
double minReach = effectiveMinRange(attacker) - hitboxMargin - extraBuffer;
double maxReach = effectiveMaxRange(attacker) + hitboxMargin + extraBuffer;
```

`effectiveMinRange/MaxRange` swap to the creative pair for creative players and multiply by `mobFactor` for everything else.

Mobs ignore the component entirely when it is absent and use a hardcoded box expansion instead:

```java
// Mob
private static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04F) - 0.6F; // 0.8284
```

That is why `mobFactor` must not be naively proportional: for a player it scales an eye distance, for a mob it is compared against 0.8284 of hitbox inflation.

---

## 4. Why omitting the component means "sword reach"

An item with no `attack_range` is not "ranged 0" — it falls through to `defaultFor()`, giving exactly the values a sword gets. This is why swords, axes, pickaxes, shovels, hoes, the mace and the trident all share one reach without any of them declaring it.

Corollary: attaching a component to a custom item is the only way to change melee reach for that item, and it affects the item, not the slot or the material tier.

## 5. Why a *shorter* component cannot break the crosshair or interaction

`LocalPlayer.raycastHitResult` treats a component `MISS` as "the item had no answer":

```java
if (itemAttackRange != null) {
    hitResult = itemAttackRange.getClosesetHit(cameraEntity, a, EntitySelector.CAN_BE_PICKED);
    if (hitResult instanceof BlockHitResult) {
        hitResult = filterHitResult(hitResult, cameraEntity.getEyePosition(a), blockInteractionRange);
    }
}

if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
    double entityInteractionRange = this.entityInteractionRange();
    hitResult = pick(cameraEntity, blockInteractionRange, entityInteractionRange, a);
}
```

With a radius below the attribute, the component ray clips nothing and returns `BlockHitResult.miss(...)`, so the normal `pick()` runs with the 3.0 attribute. The crosshair, right-click interaction and mining therefore stay vanilla; only the swing is refused. No mixin is needed for a *smaller* reach.

The spear shows the opposite case: a *larger* component makes the crosshair reach 4.5 while interaction stays gated at 3.0, producing a band where an entity is highlighted but right-click does nothing.

## 6. DaggerCrafting values

Every dagger tier carries one shared component, defined as `DaggerItem.DAGGER_ATTACK_RANGE`:

| Field | Dagger | Sword | Derivation |
| --- | --- | --- | --- |
| `minReach` | 0.0 | 0.0 | no dead zone (deliberately unlike the spear) |
| `maxReach` | 2.4 | 3.0 | `0.8 × Player.DEFAULT_ENTITY_INTERACTION_RANGE` |
| `minCreativeReach` | 0.0 | 0.0 | no dead zone |
| `maxCreativeReach` | 4.0 | 5.0 | `0.8 × (3.0 + 2.0)` |
| `hitboxMargin` | 0.0 | 0.0 | sword parity; 0.3 would give 2.7 effective and cancel the change |
| `mobFactor` | 0.345 | 1.0 | `2.4 × 0.345 = 0.828`, matching `Mob.DEFAULT_ATTACK_REACH` |

The player reach fields come from the `Player` constants, so a future change to those defaults re-derives survival and creative reach. `mobFactor` does not: it is the literal `0.345F`, tuned so `2.4 × 0.345` matches `Mob.DEFAULT_ATTACK_REACH` (about 0.828). It is not 0.8. Reach is a weapon-type value, not a material field, mirroring vanilla where every sword tier shares one reach.

## 7. Verification recipe

1. Survival, dagger, target at 2.3 blocks → swing connects.
2. Survival, dagger, target at 2.5 blocks → swing animation, no damage.
3. Creative, target at 3.9 / 4.1 blocks → connects / no damage.
4. Crosshair at 2.6 blocks with the dagger held → entity still highlighted (same as sword).
5. Right-click trade/mount at 2.8 blocks → works (attribute path untouched).
6. Mine a block at 4.0 blocks → breaks (attribute path untouched).
7. Attack indicator in the 2.4–3.0 band → hidden, because it reports melee range.
8. Throw a dagger including a full charge → unchanged flight and hits.
9. `/give` a dagger to a zombie → melee reach unchanged versus a sword-holding zombie.
10. Load a world saved before the change → new reach applies, no migration.

Record actual measured boundaries in the PR so the 2.4 / 4.0 claim stays evidenced.
