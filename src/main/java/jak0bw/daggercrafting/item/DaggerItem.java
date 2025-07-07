package jak0bw.daggercrafting.item;

import jak0bw.daggercrafting.entity.DaggerEntity;
import jak0bw.daggercrafting.ModEntities;
import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.ModEnchantments;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.block.BlockState;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.item.ProjectileItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.util.Formatting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;


public class DaggerItem extends Item implements ProjectileItem {
	public static final int MIN_DRAW_DURATION = 10;
	private static final float ATTACK_SPEED = -2.0F;

	private final DaggerToolMaterial material;


	/**
	 * Helper method to apply sword-like settings for daggers, using DaggerToolMaterial.
	 */
	public static Item.Settings applyDaggerSettings(Item.Settings settings, DaggerToolMaterial material, float attackDamage, float attackSpeed) {
		// Set up tool component (like swords: efficient on cobwebs, etc.)
		// For now, we skip block efficiency rules, but you can add them if needed.
		return settings
			.maxDamage(material.getDurability())
			.repairable(material.getRepairIngredientTag())
			.enchantable(material.getEnchantability())
			.attributeModifiers(
				AttributeModifiersComponent.builder()
					.add(
						EntityAttributes.ATTACK_DAMAGE,
						new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID, attackDamage, Operation.ADD_VALUE),
						AttributeModifierSlot.MAINHAND
					)
					.add(
						EntityAttributes.ATTACK_SPEED,
						new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID, attackSpeed, Operation.ADD_VALUE),
						AttributeModifierSlot.MAINHAND
					)
					.build()
				);
	}

	public DaggerItem(Item.Settings settings, DaggerToolMaterial material) {
		super(applyDaggerSettings(settings, material, material.getAttackDamage(), ATTACK_SPEED)); // Set attack damage from material, attack speed can be customized
		this.material = material;
	}

	/**
	 * Gets the DaggerToolMaterial for a given item instance.
	 * @param item The dagger item
	 * @return The corresponding DaggerToolMaterial
	 */
	public static DaggerToolMaterial getMaterialFromItem(Item item) {
		if (item instanceof DaggerItem daggerItem) {
			return daggerItem.getMaterial();
		}
		else {
			System.out.println("getMaterialFromItem failed");
			return DaggerToolMaterial.DIAMOND; // fallback to diamond if not a DaggerItem
		}
	}

	/**
	 * Gets the material for this dagger item.
	 * @return The DaggerToolMaterial
	 */
	public DaggerToolMaterial getMaterial() {
		return this.material == null ? DaggerToolMaterial.DIAMOND : this.material;
	}

	

	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		return !miner.isCreative();
	}

	public UseAction getUseAction(ItemStack stack) {
		return UseAction.SPEAR; // This dictates the animation and sound when charging the dagger, mimicking the trident's pull-back animation.
	}

	/**
	 * This method defines the maximum duration (in ticks) that a player can "charge" the dagger by holding down the right-click button.
	 *
	 * **Purpose of 72000 Ticks (20 minutes):**
	 * This seemingly arbitrary high number is a common Minecraft pattern for items that have a "charge-up" mechanic but no actual maximum charge time.
	 * By setting it to a very large value, we ensure that:
	 * 1. The player can hold the right-click button indefinitely without the item automatically activating or being consumed.
	 * 2. The `onStoppedUsing` method will always be called when the player *releases* the right-click, rather than the item's `getMaxUseTime` expiring.
	 * This allows us to calculate the *actual* duration the player held the button (the "charge time") and use it for conditional logic.
	 *
	 * @param stack The ItemStack representing the dagger being used.
	 * @param user The LivingEntity (typically a PlayerEntity) using the dagger.
	 * @return The maximum use time in ticks.
	 */
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	/**
	 * This is the central method that orchestrates what happens when a player *finishes* using the dagger.
	 * This method is invoked when:
	 * 1. The player releases the right-click button (the most common scenario for "throwing").
	 * 2. The item's `getMaxUseTime` is reached (less relevant here due to the very high value, but part of the vanilla item use loop).
	 *
	 * The entire logic, variable naming conventions, and flow within this method are intentionally
	 * mirrored from Minecraft's vanilla `TridentItem`'s `onStoppedUsing` method. This approach
	 * minimizes unexpected behavior and ensures compatibility with vanilla enchantment and physics systems
	 * (especially for Riptide).
	 *
	 * **Detailed Item Use Loop & Brainstormed Options:**
	 * When a player right-clicks a dagger:
	 *   - The `use()` method (further below) is called first. It performs initial checks and sets the player's current hand.
	 *   - The game continuously tracks how long the item is being "used" based on `getMaxUseTime`.
	 *   - When the player *releases* right-click (or `getMaxUseTime` is hit), `onStoppedUsing` is called.
	 *
	 * Within `onStoppedUsing`, the following scenarios are handled:
	 *
	 * **1. Initial Guard Clauses (Early Exits):**
	 *    - **`user instanceof PlayerEntity`**: Ensures only actual players can trigger the dagger's special abilities. Mobs holding daggers will not throw them this way.
	 *    - **`chargeTime < 10`**: Checks if the player held the button for at least 10 ticks (0.5 seconds). This is a crucial "charge-up" mechanic. If the dagger is merely "tapped" or clicked too quickly, it prevents accidental throws. This creates a deliberate feel, similar to drawing a bow.
	 *    - **Riptide Environmental Check (`riptideLevel > 0.0F && !playerEntity.isTouchingWaterOrRain()`):**
	 *      If the dagger has the Riptide enchantment, it *requires* the player to be in water or rain to activate. If Riptide is present but these environmental conditions aren't met, the action is cancelled. This prevents players from "Riptiding" on land without water, which is a vanilla trident behavior.
	 *    - **Durability Check (`stack.willBreakNextUse()`):**
	 *      If the dagger has only one durability point left, attempting to use it would break it. This check prevents the item from being used on its last durability, preserving the broken item rather than consuming it. This is a design choice to allow players to repair almost-broken daggers.
	 *
	 * **2. Core Logic (Server-Side Execution):**
	 *    - **Sound Event Retrieval (`throwSound`)**: Determines which sound to play (e.g., normal throw sound, or a specific Riptide sound if applicable).
	 *    - **Statistics Tracking**: Updates the player's "items used" statistic, a standard Minecraft interaction.
	 *    - **Server-Side Gate (`world instanceof ServerWorld`)**: This is paramount for multiplayer consistency. All actions that alter game state (spawning entities, damaging items, applying velocity) MUST occur on the logical server to prevent desynchronization between clients and server, and to prevent cheating.
	 *    - **Durability Damage**: The dagger loses 1 durability point upon use (whether thrown or Riptide-launched), consistent with tridents.
	 *
	 * **3. Branching Behavior (Normal Throw vs. Riptide Launch):**
	 *    - **Normal Throw (`riptideLevel == 0.0F`):**
	 *        - **DaggerEntity Spawning**: A new `DaggerEntity` (our custom projectile) is created and launched from the player.
	 *          - `DaggerEntity.spawnWithVelocity(...)`: This helper handles the complex task of creating the entity, setting its initial position (from player), and applying the initial velocity based on the dagger's material (`this.material.getRangedVelocity()`). The 0.0F pitch and inaccuracy values mean it flies straight ahead without initial vertical angle or random deviation.
	 *        - **Item Consumption (Survival) / Retention (Creative)**:
	 *          - **Survival Players**: The dagger is removed from the player's inventory, simulating a traditional throw where the item is "used up."
	 *          - **Creative Players**: The dagger remains in the player's inventory, and the thrown `DaggerEntity` is marked `PickupPermission.CREATIVE_ONLY`, allowing only creative players to retrieve it. This mirrors vanilla behavior for items like tridents in creative mode.
	 *        - **Sound Playback**: The appropriate throwing sound is played at the dagger's spawn location.
	 
	
	 * **4. Final Return Values:**
	 *    - `return true;`: Indicates that the item's use was successfully processed (either a throw or a Riptide launch).
	 *    - `return false;`: Indicates that the use was cancelled due to insufficient charge, environmental conditions, or durability.
	 *    - The final `else { return false; }` at the very end is a catch-all for cases where the user is not a player, ensuring non-player entities cannot trigger these special item mechanics.
	 *
	 * @param stack The ItemStack of the dagger being used.
	 * @param world The World the action is taking place in (server-side for throwing).
	 * @param user The LivingEntity (PlayerEntity) that stopped using the dagger.
	 * @param remainingUseTicks The number of ticks remaining from getMaxUseTime. This is used to calculate the 'charge time'.
	 * @return True if the item was successfully used (thrown/riptided), false otherwise.
	 */
	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		// Ensure the user is a player, as this item's mechanics are designed for player interaction (throwing, Riptide).
		if (user instanceof PlayerEntity playerEntity) {
			// Calculate the actual duration the player held down the right-click button.
			// A higher 'chargeTime' means the dagger was held longer.
			int chargeTime = this.getMaxUseTime(stack, user) - remainingUseTicks;
			
			// If the dagger was held for less than 10 ticks, it's considered an accidental click
			// or an insufficient charge. In this case, the throwing action is cancelled.
			if (chargeTime < 5) {
				return false; // Not charged enough, prevent throw or Riptide.
			} else {
				if (stack.willBreakNextUse()) {
					return false; // Prevent breaking on use; preserve the broken dagger.
				} else {
					// Retrieve the specific sound event to play when the dagger is used. This can be
					// overridden by enchantments (like Riptide changing the sound from a normal throw).
					RegistryEntry<SoundEvent> throwSound = (RegistryEntry)EnchantmentHelper.getEffect(stack, EnchantmentEffectComponentTypes.TRIDENT_SOUND).orElse(SoundEvents.ITEM_TRIDENT_THROW);
					
					// Increment the player's statistics for using this item. This is a standard Minecraft mechanic.
					playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
					
					// All significant game logic for throwing or Riptide should only happen on the server-side
					// to prevent desynchronization and ensure consistent behavior for all players.
					if (world instanceof ServerWorld) {
						ServerWorld serverWorld = (ServerWorld)world;
						
						// Daggers lose durability when thrown or used with Riptide, similar to tridents.
						stack.damage(1, playerEntity);
						
						float speed = this.material.getRangedVelocity();

						// Get the level of the throwing speed enchantment
						Identifier throwingSpeedId = Identifier.of(DaggerCrafting.MOD_ID, "throwing_speed");
						RegistryEntry<Enchantment> throwingSpeedEnchantment = serverWorld.getRegistryManager()
							.getOrThrow(RegistryKeys.ENCHANTMENT)
							.getEntry(throwingSpeedId)
							.orElse(null);
						
						int throwingSpeedLevel = throwingSpeedEnchantment != null ? EnchantmentHelper.getLevel(throwingSpeedEnchantment, stack) : 0;
						System.out.println("throwingSpeedLevel: " + throwingSpeedLevel);

						if (throwingSpeedLevel > 0) {
							float multiplier = 1.5f;
							multiplier += (throwingSpeedLevel - 1) * 0.25f;
							speed *= multiplier;
							// No cap - let the synchronization system handle any speed dynamically
						}


						float heightOffset = 0.2f;
						float pitchOffset = 3.0f / speed;
						// playerEntity.setPosition(playerEntity.getX(), playerEntity.getY() + heightOffset, playerEntity.getZ());
						// playerEntity.setPitch(playerEntity.getPitch() - pitchOffset);

						DaggerEntity daggerEntity = (DaggerEntity)(DaggerEntity.spawnWithVelocity(DaggerEntity::new, serverWorld, stack, playerEntity, 0.0F, speed, 0.0F));
						System.out.println("DaggerItem onStoppedUsing daggerEntity: " + daggerEntity.getItemStack()); // Debug print for the thrown dagger

						// playerEntity.setPitch(playerEntity.getPitch() + pitchOffset);
						// playerEntity.setPosition(playerEntity.getX(), playerEntity.getY() - heightOffset, playerEntity.getZ());

						// If the player is in creative mode, the dagger is not removed from their inventory,
						// and the thrown entity can only be picked up by creative players.
						if (playerEntity.isCreative()) {
							daggerEntity.pickupType = PickupPermission.CREATIVE_ONLY;
						} else {
							// For survival players, the dagger is consumed from their inventory upon throwing.
							playerEntity.getInventory().removeOne(stack);
						}

						// Play the distinct throwing sound effect at the location of the thrown dagger.
						world.playSoundFromEntity((PlayerEntity)null, daggerEntity, SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
						return true; // Successfully performed a normal dagger throw.
					}
				}
			}
		}
		return false; 
	}

	/**
	 * This method is called when a player *initiates* using the dagger (e.g., right-clicking).
	 * Its primary responsibility is to perform initial validation and set up the game state
	 * so that the dagger can be "charged" and subsequently used (thrown or Riptided).
	 *
	 * **Role in the Item Use Loop:**
	 * 1. **Initial Validation**: Checks are made before any animation or charging begins.
	 * 2. **`user.setCurrentHand(hand)`**: This is the crucial line. It tells the game that the player is
	 *    currently "using" this item. This triggers the use animation (`UseAction.SPEAR`) and starts
	 *    the internal timer that eventually leads to `onStoppedUsing` being called when the player
	 *    releases the button or the `getMaxUseTime` is reached.
	 *
	 * @param world The World the action is taking place in.
	 * @param user The PlayerEntity attempting to use the dagger.
	 * @param hand The Hand (MAIN_HAND or OFF_HAND) holding the dagger.
	 * @return An ActionResult indicating whether the use was successfully initiated, failed, or consumed.
	 */
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);

		// Prevent starting to use the dagger if it has only 1 durability left.
		// This is an early exit to avoid the animation and then a failed throw/riptide.
		if (itemStack.willBreakNextUse()) {
			return ActionResult.FAIL; // Cannot start using if it will break on next use.
		}
		// If Riptide is present but the player is not in water or rain,
		// prevent the use action from even beginning. This mirrors vanilla trident behavior
		// where you can't "charge" a Riptide trident on land.
		else if (EnchantmentHelper.getTridentSpinAttackStrength(itemStack, user) > 0.0F && !user.isTouchingWaterOrRain()) {
			return ActionResult.FAIL; // Riptide conditions not met, prevent use animation from starting.
		} else {
			// If all preliminary checks pass, set the player's current active hand to the one
			// holding the dagger. This is essential for triggering the charging animation
			// and activating the `getMaxUseTime` timer, which ultimately leads to `onStoppedUsing`.
			user.setCurrentHand(hand);
			return ActionResult.CONSUME; // Successfully started using the item; item is "consumed" into the use state.
		}
	}

	/**
	 * This method is invoked when the dagger makes a melee hit on a LivingEntity.
	 * It's part of the standard Minecraft item interaction for melee weapons.
	 *
	 * **Purpose:**
	 * - Returning `true` signals to the game that the hit event has been processed by this item.
	 * - The actual damage dealt during melee combat is typically handled by the item's
	 *   configured `ATTACK_DAMAGE` and `ATTACK_SPEED` attributes (defined in `applyDaggerSettings`).
	 *   This method primarily serves as a callback point if additional effects were needed on hit.
	 *
	 * @param stack The ItemStack of the dagger.
	 * @param target The LivingEntity that was hit in melee.
	 * @param attacker The LivingEntity (player) that performed the melee attack.
	 * @return True to indicate the hit was processed.
	 */
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		return true; // Indicate that the hit event was processed.
	}

	/**
	 * This method is called after the dagger has successfully dealt damage to an entity in melee combat.
	 *
	 * **Purpose:**
	 * - Its main function here is to apply durability damage to the dagger when it's used as a melee weapon.
	 *   Unlike throwing, where durability is lost in `onStoppedUsing`, melee hits have a separate damage mechanism.
	 *
	 * @param stack The ItemStack of the dagger.
	 * @param target The LivingEntity that was damaged by the dagger.
	 * @param attacker The LivingEntity (player) that dealt the damage.
	 */
	public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.damage(1, attacker, EquipmentSlot.MAINHAND); // Reduce dagger durability by 1 for melee attack.
	}

	/**
	 * This method is a contract from the `ProjectileItem` interface.
	 * It acts as a "factory" method for creating the actual projectile entity (`DaggerEntity`)
	 * that flies through the air when the dagger is thrown.
	 *
	 * **Role in Projectile Spawning:**
	 * - When `DaggerEntity.spawnWithVelocity` (called in `onStoppedUsing`) needs to create a new `DaggerEntity`,
	 *   it calls this `createEntity` method on the `DaggerItem` to get a fresh instance of the projectile.
	 * - This allows the `DaggerItem` class to define *what kind* of projectile it throws.
	 * - `stack.copyWithCount(1)` ensures that the newly created projectile entity correctly carries
	 *   over the NBT data (like enchantments, custom name, etc.) from the original dagger, but as a single item stack.
	 * - `daggerEntity.pickupType = PickupPermission.ALLOWED` sets the default behavior: anyone can pick up the thrown dagger.
	 *   (This is overridden for creative mode players in `onStoppedUsing` for their specific pickup rules).
	 *
	 * @param world The World the entity will be spawned in.
	 * @param pos The Position (x, y, z coordinates) where the entity will spawn.
	 * @param stack The ItemStack of the dagger being thrown (used to copy NBT to the projectile).
	 * @param direction The direction the projectile should initially face (often derived from player look).
	 * @return A new instance of the `DaggerEntity` ready to be launched.
	 */
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		// Create a new DaggerEntity instance. The `copyWithCount(1)` ensures only one dagger is spawned.
		DaggerEntity daggerEntity = new DaggerEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
		// Set the pickup type, allowing any player to pick up the thrown dagger by default.
		daggerEntity.pickupType = PickupPermission.ALLOWED;
		return daggerEntity;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		// Call the super method first to add all standard item tooltips (e.g., enchantments, attack damage/speed).
		super.appendTooltip(stack, context, tooltip, type);

		// Add custom tooltips specific to the dagger's thrown properties, making them visible to the player.
		// These include "Ranged Damage" and "Ranged Velocity," which are derived from the dagger's material
		// (e.g., `DaggerToolMaterial.DIAMOND` will have specific ranged stats).
		// The translatable text keys (`item.daggercrafting.dagger.when_thrown`, etc.) refer to entries in the language files (`en_us.json`).
		tooltip.add(Text.translatable("item.daggercrafting.dagger.when_thrown").formatted(Formatting.GRAY));
		DaggerToolMaterial mat = this.getMaterial();
		tooltip.add(Text.literal(" ").append(Text.translatable("item.daggercrafting.dagger.ranged_damage", mat.getRangedDamage()).formatted(Formatting.DARK_GREEN)));
		tooltip.add(Text.literal(" ").append(Text.translatable("item.daggercrafting.dagger.ranged_velocity", mat.getRangedVelocity()).formatted(Formatting.DARK_GREEN)));

	}
}
