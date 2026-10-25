# Changelog

## [1.4.0]

### Added
- Restitution (level I): a thrown dagger that hits an entity, including a fully blocked hit, returns to the thrower's inventory the same tick. Misses stay in the world. Incompatible with Loyalty.
- Throwing Speed and Restitution can now appear on exploration loot and librarian trades, not only at the enchanting table.
- Particle Effects
  - Daggers stuck in a block emit a slow end-rod wisp so missed throws are easier to spot again.

### Changed
- Dagger enchantment compatibility now matches vanilla ids exactly, so the allowed set stays closed and other mods are less likely to leak onto daggers.
- Cleaned unused Throwing Speed datapack fields; table offers and throw behavior are unchanged.

## [1.3.0]

### Added
- Particle Effects
  - Client particle trails on thrown daggers (crit sparks; on-fire throws use flame and smoke).
  - Speed-scaled terrain crumb bursts when a thrown dagger hits a block.

## [1.2.1]

### Changed
- Replaced the placeholder Fabric mod description with a real one.
- Removed leftover debug console output from throws, entity setup, and enchant checks.

## [1.1.1]

### Fixed
- Fixed copper dagger melee damage scaling- Now does 3 instead of 2 melee damage.


## [1.1.0] 

### Added
- Copper dagger
- Netherite dagger


## [1.0.0] 

### Added
- Initial release of DaggerCrafting mod
- Throwable daggers as a new weapon type
- Blended melee and ranged combat mechanics
- Material-based dagger variants:
  - Wooden dagger
  - Stone dagger
  - Iron dagger
  - Golden dagger
  - Diamond dagger
- Vanilla enchantment support (Loyalty, Fire Aspect, Flame, Sharpness, Smite, Bane of Arthropods, Mending)
- Custom Throwing Speed enchantment (Levels I-III)
- Velocity-based knockback system
- Crafting recipes for all dagger variants 