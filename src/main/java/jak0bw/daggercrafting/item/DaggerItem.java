package jak0bw.daggercrafting.item;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.DaggerToolMaterial;
import jak0bw.daggercrafting.ModEnchantments;
import jak0bw.daggercrafting.entity.DaggerEntity;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

/**
 * Throwable melee weapon: one registered instance per {@link DaggerToolMaterial} tier in {@link jak0bw.daggercrafting.ModItems}.
 *
 * Right-click charges a throw (trident use animation and sound); release spawns a tier-matched
 * {@link DaggerEntity} whose damage and speed come from the material, not from duplicate stats on the item NBT.
 * Melee uses standard item attributes from {@link #applyDaggerSettings}; ranged uses
 * {@link DaggerToolMaterial#getRangedDamage()} / {@link DaggerToolMaterial#getRangedVelocity()} on the entity.
 *
 * Implements {@link ProjectileItem} for dispensers. Enchantments such as loyalty and
 * {@link ModEnchantments#THROWING_SPEED} are allowed on daggers via
 * {@link jak0bw.daggercrafting.mixin.EnchantmentMixin} and read in entity / throw code paths.
 */
public class DaggerItem extends Item implements ProjectileItem {

	/** Shared main-hand attack speed offset for all tiers (slower than bare fist, similar to swords). */
	private static final float ATTACK_SPEED = -2.0F;

	/** Shorter charge cancels the throw and does not consume durability or an item. */
	private static final int MIN_THROW_TICKS = 3;

	/** Charge duration at which {@link DaggerToolMaterial#getRangedVelocity()} is applied at 100%. */
	private static final int FULL_CHARGE_TICKS = 13;

	/** Minimum speed fraction when the player releases immediately after {@link #MIN_THROW_TICKS}. */
	private static final float MIN_CHARGE_FACTOR = 0.3f;

	/**
	 * Temporary look pitch adjustment while spawning the projectile so high-speed throws feel slightly aimed
	 * downward; restored immediately after {@link Projectile#spawnProjectileFromRotation}.
	 */
	private static final float MAX_PITCH_OFFSET = 5.0f;

	/** Tier bundle fixed at construction; drives throw speed and tooltip ranged lines. */
	private final DaggerToolMaterial material;

	/**
	 * Central place to attach durability, repair tag, enchantability, and melee attributes for every tier.
	 *
	 * {@link jak0bw.daggercrafting.ModItems} passes per-material attack damage; attack speed is shared
	 * via {@link #ATTACK_SPEED}. Ranged stats are not item attributes—they live on {@link DaggerToolMaterial}
	 * and are applied when building {@link DaggerEntity}.
	 *
	 * @param properties base registry properties from mod item registration
	 * @param material tier stats source
	 * @param attackDamage stored melee component (already normalized in {@link DaggerToolMaterial})
	 * @param attackSpeed main-hand speed modifier added to player base
	 * @return properties ready for {@code new DaggerItem(...)}
	 */
	public static Item.Properties applyDaggerSettings(
		Item.Properties properties,
		DaggerToolMaterial material,
		float attackDamage,
		float attackSpeed
	) {
		return properties
			.durability(material.getDurability())
			.repairable(material.getRepairIngredientTag())
			.enchantable(material.getEnchantability())
			.attributes(
				ItemAttributeModifiers.builder()
					.add(
						Attributes.ATTACK_DAMAGE,
						new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage, Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND
					)
					.add(
						Attributes.ATTACK_SPEED,
						new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND
					)
					.build()
			);
	}

	/**
	 * @param properties passed through {@link #applyDaggerSettings}
	 * @param material immutable tier reference for throws and tooltips
	 */
	public DaggerItem(Item.Properties properties, DaggerToolMaterial material) {
		super(applyDaggerSettings(properties, material, material.getAttackDamage(), ATTACK_SPEED));
		this.material = material;
	}

	/**
	 * Resolves material for any item reference (used by {@link DaggerEntity#getMaterial()}).
	 *
	 * @param item expected to be a {@link DaggerItem}; corrupt stacks log and fall back to diamond tier
	 * @return stats bundle for thrown damage/speed and fallbacks
	 */
	public static DaggerToolMaterial getMaterialFromItem(Item item) {
		if (item instanceof DaggerItem daggerItem) {
			return daggerItem.getMaterial();
		} else {
			DaggerCrafting.LOGGER.warn("getMaterialFromItem called with non-dagger item {}, using diamond_dagger", item);
			return DaggerToolMaterial.DAGGER_TOOL_MATERIALS.get("diamond_dagger");
		}
	}

	/**
	 * @return this instance's tier, or diamond if the field were ever null (defensive for data fixes)
	 */
	public DaggerToolMaterial getMaterial() {
		return this.material == null
			? DaggerToolMaterial.DAGGER_TOOL_MATERIALS.get("diamond_dagger")
			: this.material;
	}

	/**
	 * Creative instabuild players still swing; this only blocks breaking blocks while using the dagger as a tool.
	 */
	@Override
	public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
		return !(user instanceof Player player && player.getAbilities().instabuild);
	}

	/** Reuses trident charge pose so players recognize the hold-to-throw input. */
	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.TRIDENT;
	}

	/**
	 * Very long duration so the item never “runs out” of use on its own; only button release ends the charge.
	 *
	 * Actual throw timing is computed from elapsed ticks in {@link #releaseUsing}.
	 */
	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	/**
	 * Server-side throw on right-click release after {@link #MIN_THROW_TICKS} charge.
	 *
	 * Consumes one stack count, applies throw durability, scales speed by charge and
	 * {@link ModEnchantments#THROWING_SPEED}, spawns {@link DaggerEntity} via
	 * {@link Projectile#spawnProjectileFromRotation} (entity type still resolved from stack).
	 * Throw charge {@code [0, 1]} is written on the entity so the client trail can lerp.
	 *
	 * @return {@code true} only when a projectile was created on the server; client returns {@code false}
	 */
	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof Player player)) {
			return false;
		}

		int chargeTime = this.getUseDuration(stack, user) - remainingUseTicks;
		if (chargeTime < MIN_THROW_TICKS) {
			return false;
		}
		if (stack.nextDamageWillBreak()) {
			return false;
		}

		// 0 at the first legal release tick, 1 at full charge; speed uses a 0.3–1.0 remap of this.
		float chargeProgress = throwChargeProgress(chargeTime);
		float chargeForce = Mth.lerp(chargeProgress, MIN_CHARGE_FACTOR, 1.0f);

		player.awardStat(Stats.ITEM_USED.get(this));

		if (level instanceof ServerLevel serverLevel) {
			stack.hurtWithoutBreaking(1, player);

			float speed = this.material.getRangedVelocity() * chargeForce;

			// Mod throwing_speed stacks on charge-scaled base (see enchantment json for max level).
			Holder<Enchantment> throwingSpeedEnchantment = serverLevel.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.get(ModEnchantments.THROWING_SPEED)
				.orElse(null);

			int throwingSpeedLevel = throwingSpeedEnchantment != null
				? EnchantmentHelper.getItemEnchantmentLevel(throwingSpeedEnchantment, stack)
				: 0;

			if (throwingSpeedLevel > 0) {
				float multiplier = 1.5f;
				multiplier += (throwingSpeedLevel - 1) * 0.25f;
				speed *= multiplier;
			}

			float pitchOffset = MAX_PITCH_OFFSET / Math.max(Math.min(speed, MAX_PITCH_OFFSET), 1.0f);

			// Brief pitch dip affects spawn direction only; restored after projectile creation.
			player.setXRot(player.getXRot() - pitchOffset);
			ItemStack thrownStack = stack.consumeAndReturn(1, player);
			DaggerEntity daggerEntity = Projectile.spawnProjectileFromRotation(
				(spawnLevel, owner, projectileStack) -> {
					DaggerEntity spawned = new DaggerEntity(spawnLevel, owner, projectileStack);
					spawned.setThrowCharge(chargeProgress);
					return spawned;
				},
				serverLevel,
				thrownStack,
				player,
				0.0F,
				speed,
				0.0F
			);
			player.setXRot(player.getXRot() + pitchOffset);

			// Creative: visual throw without giving survival players a dupe via pickup.
			if (player.hasInfiniteMaterials()) {
				daggerEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			}

			level.playSound(null, daggerEntity, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
			return true;
		}

		return false;
	}

	/**
	 * Starts charge on right-click. Blocks broken stacks and dry-land riptide (trident enchant parity).
	 *
	 * Throw execution is deferred to {@link #releaseUsing} when the player releases use.
	 */
	@Override
	public InteractionResult use(Level level, Player user, InteractionHand hand) {
		ItemStack itemStack = user.getItemInHand(hand);

		if (itemStack.nextDamageWillBreak()) {
			return InteractionResult.FAIL;
		} else if (EnchantmentHelper.getTridentSpinAttackStrength(itemStack, user) > 0.0F && !user.isInWaterOrRain()) {
			return InteractionResult.FAIL;
		} else {
			user.startUsingItem(hand);
			return InteractionResult.CONSUME;
		}
	}

	/** Standard weapon durability loss on melee hit (throw durability is handled in {@link #releaseUsing}). */
	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
	}

	/**
	 * Dispenser hook: spawns a pickup-allowed {@link DaggerEntity} at the nozzle with a single stack copy.
	 */
	@Override
	public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
		DaggerEntity daggerEntity = new DaggerEntity(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
		daggerEntity.pickup = AbstractArrow.Pickup.ALLOWED;
		daggerEntity.setThrowCharge(1.0f);
		return daggerEntity;
	}

	/**
	 * Linear charge fraction between {@link #MIN_THROW_TICKS} and {@link #FULL_CHARGE_TICKS}.
	 *
	 * @param chargeTime ticks the use button was held
	 * @return {@code 0} at the earliest legal throw, {@code 1} at or past full charge
	 */
	private static float throwChargeProgress(int chargeTime) {
		if (chargeTime >= FULL_CHARGE_TICKS) {
			return 1.0f;
		}
		if (chargeTime <= MIN_THROW_TICKS) {
			return 0.0f;
		}
		return (float) (chargeTime - MIN_THROW_TICKS) / (float) (FULL_CHARGE_TICKS - MIN_THROW_TICKS);
	}

	/** Appends tier-specific thrown damage and velocity lines below the shared “when thrown” header. */
	@Override
	@Deprecated
	public void appendHoverText(
		ItemStack stack,
		Item.TooltipContext context,
		TooltipDisplay display,
		Consumer<Component> textConsumer,
		TooltipFlag type
	) {
		textConsumer.accept(Component.translatable("item.daggercrafting.dagger.when_thrown").withStyle(ChatFormatting.GRAY));
		DaggerToolMaterial mat = this.getMaterial();
		textConsumer.accept(
			Component.literal(" ").append(
				Component.translatable("item.daggercrafting.dagger.ranged_damage", mat.getRangedDamage()).withStyle(ChatFormatting.DARK_GREEN)
			)
		);
		textConsumer.accept(
			Component.literal(" ").append(
				Component.translatable("item.daggercrafting.dagger.ranged_velocity", mat.getRangedVelocity()).withStyle(ChatFormatting.DARK_GREEN)
			)
		);
	}
}
