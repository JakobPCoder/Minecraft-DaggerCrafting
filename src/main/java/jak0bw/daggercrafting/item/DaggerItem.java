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
 * Charge-to-throw dagger: trident use animation, material-based melee and projectile stats.
 */
public class DaggerItem extends Item implements ProjectileItem {
	private static final float ATTACK_SPEED = -2.0F;

	private static final int MIN_THROW_TICKS = 3;
	private static final int FULL_CHARGE_TICKS = 13;
	private static final float MIN_CHARGE_FACTOR = 0.3f;
	private static final float MAX_PITCH_OFFSET = 5.0f;

	private final DaggerToolMaterial material;

	/**
	 * Applies sword-like durability, repair, enchantability, and attack attributes from a material.
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

	public DaggerItem(Item.Properties properties, DaggerToolMaterial material) {
		super(applyDaggerSettings(properties, material, material.getAttackDamage(), ATTACK_SPEED));
		this.material = material;
	}

	/**
	 * Resolves the material for a dagger item instance.
	 *
	 * @param item The dagger item
	 * @return The corresponding DaggerToolMaterial
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
	 * Gets the material for this dagger item.
	 *
	 * @return The DaggerToolMaterial
	 */
	public DaggerToolMaterial getMaterial() {
		return this.material == null
			? DaggerToolMaterial.DAGGER_TOOL_MATERIALS.get("diamond_dagger")
			: this.material;
	}

	@Override
	public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
		return !(user instanceof Player player && player.getAbilities().instabuild);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.TRIDENT;
	}

	/**
	 * Long use duration so releasing the button always calls {@link #releaseUsing} instead of auto-firing.
	 */
	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	/**
	 * Throws the dagger when the player releases right-click after a minimum charge.
	 *
	 * @return true if a throw actually happened on the server
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

		float chargeForce;
		if (chargeTime >= FULL_CHARGE_TICKS) {
			chargeForce = 1.0f;
		} else if (chargeTime <= MIN_THROW_TICKS) {
			chargeForce = MIN_CHARGE_FACTOR;
		} else {
			float chargeProgress = (float) (chargeTime - MIN_THROW_TICKS) / (float) (FULL_CHARGE_TICKS - MIN_THROW_TICKS);
			chargeForce = Mth.lerp(chargeProgress, MIN_CHARGE_FACTOR, 1.0f);
		}

		player.awardStat(Stats.ITEM_USED.get(this));

		if (level instanceof ServerLevel serverLevel) {
			stack.hurtWithoutBreaking(1, player);

			float speed = this.material.getRangedVelocity() * chargeForce;

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

			player.setXRot(player.getXRot() - pitchOffset);
			ItemStack thrownStack = stack.consumeAndReturn(1, player);
			DaggerEntity daggerEntity = Projectile.spawnProjectileFromRotation(
				DaggerEntity::new,
				serverLevel,
				thrownStack,
				player,
				0.0F,
				speed,
				0.0F
			);
			player.setXRot(player.getXRot() + pitchOffset);

			if (player.hasInfiniteMaterials()) {
				daggerEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			}

			level.playSound(null, daggerEntity, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
			return true;
		}

		return false;
	}

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

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
	}

	@Override
	public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
		DaggerEntity daggerEntity = new DaggerEntity(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
		daggerEntity.pickup = AbstractArrow.Pickup.ALLOWED;
		return daggerEntity;
	}

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
