# DaggerCrafting

[LICENSE](LICENSE.md)

[GITHUB](https://github.com/JakobPCoder/Minecraft-DaggerCrafting)

This is an early version, feedback and ideas are welcome!

DaggerCrafting is a Minecraft mod that introduces throwable daggers as a new weapon type, blending fast-paced melee and ranged combat. Each dagger's stats and behavior are determined by its material, offering new strategic options. Also its a lot of fun!

### Blended Combat: Redefining Your Playstyle

DaggerCrafting transcends traditional weapon types, offering a unique blended combat experience. Engage enemies up close with rapid melee strikes, then seamlessly transition to ranged assaults by precisely throwing your dagger. This dual functionality encourages dynamic combat strategies, allowing you to adapt on the fly whether clearing crowds or picking off distant foes.

## Daggers

Daggers are crafted using a stick and a piece of the corresponding material, placed diagonally in a crafting grid.

![Items](images/items.png)![Items](images/iron.png)![Items](images/netherite.png)

## Ranged Combat

When holding and releasing the right mouse button, the dagger will be thrown, similar to tridents.

*   The **damage** the dagger deals on impact and the **speed** with which the dagger is thrown are based on the dagger's material.
*   The dagger is thrown with a **speed** based on the **material**, its **throwing speed enchantment** and the **draw/charge time**.
*   The **knockback**: Knockback is essential for repeated attacks with a dagger, so _Creepers_ etc. are kept at range. The force of a dagger's **knockback** is directly proportional to its **velocity** when it hits a target. This means daggers thrown with greater initial speed will send enemies flying further!

## Melee Combat

When in the main hand, the dagger will work just like a sword, but with less damage and a faster attack speed.

## Enchantments

Daggers support the following vanilla enchantments:

### Vanilla

*   **Loyalty**
    *   Unlike vanilla tridents, a dagger's Loyalty enchantment scales directly with its material's base throwing velocity! High-tier daggers, like Diamond, return significantly faster, enabling quicker follow-up throws and a fluid ranged combat experience. Essential for viability as a ranged weapon.
*   **Fire Aspect**
    *   Works just like in vanilla. Sets the target on fire for 5 seconds on melee attacks.
*   **Flame**
    *   Works just like in vanilla. Sets the dagger and target on fire for 5 seconds on ranged attacks.

_These enchantments work just like in vanilla. Their modifiers are applied on **melee and ranged attacks**._ All listed vanilla enchantments, except Mending, can be obtained through an enchanting table. Mending can be applied via an anvil.

*   **Sharpness**
*   **Smite**
*   **Bane of Arthropods**
*   **Mending**: Compatible with daggers, obtained via an anvil.

### Custom enchantments

*   **Throwing Speed**
    *   The **Throwing Speed** enchantment (Levels I-III) is a game-changer for ranged dagger combat. Each level significantly boosts your dagger's initial throwing velocity, leading to:
    *   **Extended Range**: Hit targets from much further away.
    *   **Increased Knockback**: Amplify the unique velocity-based knockback, sending foes reeling. This enchantment truly unlocks the full potential of daggers as a primary ranged weapon.

## Stats


| **Material** | **Ranged Damage** | **Ranged Velocity** | **Durability** | **Melee Damage** | **Enchantability** |
| -------- |------------- |--------------- |---------- |------------- |-------------- |
| Golden |4 |**1.7** |20 |2 |**25** |
| Wooden |4 |1.5 |30 |2 |15 |
| Stone |5 |1.6 |50 |3 |14 |
| Copper |6 |1.7 |70 |3 |13 |
| Iron |7 |1.8 |100 |4 |11 |
| Diamond |8 |**2.0** |250 |5 |10 |
| Netherite |9 |**2.0** |500 |6 |15 |
---
