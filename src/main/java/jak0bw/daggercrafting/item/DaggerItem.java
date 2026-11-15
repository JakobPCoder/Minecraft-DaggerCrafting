package jak0bw.daggercrafting.item;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.DaggerToolMaterial;
import jak0bw.daggercrafting.ModEnchantments;
import jak0bw.daggercrafting.entity.DaggerEntity;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.component.type.WeaponComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * Throwable melee weapon: one registered instance per {@link DaggerToolMaterial} tier in {@link jak0bw.daggercrafting.ModItems}.
 *
 * Right-click charges a throw; release spawns a tier-matched {@link DaggerEntity}. Implements
 * {@link ProjectileItem} for dispensers.
 */
public class DaggerItem extends Item implements ProjectileItem {

	private static final float ATTACK_SPEED = -2.0F;
	private static final int MIN_THROW_TICKS = 3;
	private static final int FULL_CHARGE_TICKS = 13;
	private static final float MIN_CHARGE_FACTOR = 0.3f;
	private static final float MAX_PITCH_OFFSET = 5.0f;

	private final DaggerToolMaterial material;

	/**
	 * Central place to attach durability, repair tag, enchantability, sword-like mining, weapon wear,
	 * and melee attributes for every tier.
	 *
	 * {@link DataComponentTypes#WEAPON} is what makes a melee hit spend durability. {@code ItemStack.postHit}
	 * returns true only when that component is present, so {@code PlayerEntity} never calls
	 * {@code postDamageEntity} without it. {@code WeaponComponent(1)} matches a sword: one durability per
	 * connecting swing. This class does not override {@code postDamageEntity}; {@code ItemStack.postDamageEntity}
	 * already spends {@code itemDamagePerAttack}, and a second {@code damage} would charge the hit twice.
	 */
	public static Item.Settings applyDaggerSettings(
		Item.Settings settings,
		DaggerToolMaterial material,
		float attackDamage,
		float attackSpeed
	) {
		return settings
			.maxDamage(material.getDurability())
			.repairable(material.getRepairIngredientTag())
			.enchantable(material.getEnchantability())
			.component(DataComponentTypes.TOOL, swordLikeMiningTool())
			.component(DataComponentTypes.WEAPON, new WeaponComponent(1))
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

	/**
	 * Sword-parity {@link DataComponentTypes#TOOL} used by every dagger tier.
	 *
	 * Fabric {@code main} and {@code runDatagen} still see unbound public block tags at
	 * {@code ModItems} construction, so {@code Registries.BLOCK.getOrThrow(TagKey)} throws
	 * {@code Tags not bound}. Vanilla swords avoid that with {@code Registries.createEntryLookup},
	 * which creates tag holders for registration without requiring tags to be bound.
	 *
	 * @return cobweb at speed 15 with drops, instant bamboo, 1.5× sword-efficient plants, default 1.0,
	 *     2 durability per mined block, no creative block break
	 */
	private static ToolComponent swordLikeMiningTool() {
		RegistryEntryLookup<Block> blocks = Registries.createEntryLookup(Registries.BLOCK);
		return new ToolComponent(
			List.of(
				ToolComponent.Rule.ofAlwaysDropping(RegistryEntryList.of(Blocks.COBWEB.getRegistryEntry()), 15.0F),
				ToolComponent.Rule.of(blocks.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE),
				ToolComponent.Rule.of(blocks.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F)
			),
			1.0F,
			2,
			false
		);
	}

	public DaggerItem(Item.Settings settings, DaggerToolMaterial material) {
		super(applyDaggerSettings(settings, material, material.getAttackDamage(), ATTACK_SPEED));
		this.material = material;
	}

	/**
	 * Resolves material for any item reference (used by {@link DaggerEntity#getMaterial()}).
	 */
	public static DaggerToolMaterial getMaterialFromItem(Item item) {
		if (item instanceof DaggerItem daggerItem) {
			return daggerItem.getMaterial();
		}
		DaggerCrafting.LOGGER.warn("getMaterialFromItem called with non-dagger item {}, using diamond_dagger", item);
		return DaggerToolMaterial.DAGGER_TOOL_MATERIALS.get("diamond_dagger");
	}

	public DaggerToolMaterial getMaterial() {
		return this.material == null
			? DaggerToolMaterial.DAGGER_TOOL_MATERIALS.get("diamond_dagger")
			: this.material;
	}

	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		return !miner.isCreative();
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.SPEAR;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	/**
	 * Server-side throw on right-click release after {@link #MIN_THROW_TICKS} charge.
	 *
	 * Throw charge {@code [0, 1]} is written on the entity so the client trail can lerp.
	 */
	@Override
	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof PlayerEntity player)) {
			return false;
		}

		int chargeTime = this.getMaxUseTime(stack, user) - remainingUseTicks;
		if (chargeTime < MIN_THROW_TICKS) {
			return false;
		}
		if (stack.willBreakNextUse()) {
			return false;
		}

		float chargeProgress = throwChargeProgress(chargeTime);
		float chargeForce = MathHelper.lerp(chargeProgress, MIN_CHARGE_FACTOR, 1.0f);

		player.incrementStat(Stats.USED.getOrCreateStat(this));

		if (world instanceof ServerWorld serverWorld) {
			stack.damage(1, player);

			float speed = this.material.getRangedVelocity() * chargeForce;

			RegistryEntry<Enchantment> throwingSpeedEnchantment = serverWorld.getRegistryManager()
				.getOrThrow(RegistryKeys.ENCHANTMENT)
				.getOptional(ModEnchantments.THROWING_SPEED)
				.orElse(null);

			int throwingSpeedLevel = throwingSpeedEnchantment != null
				? EnchantmentHelper.getLevel(throwingSpeedEnchantment, stack)
				: 0;

			if (throwingSpeedLevel > 0) {
				float multiplier = 1.5f;
				multiplier += (throwingSpeedLevel - 1) * 0.25f;
				speed *= multiplier;
			}

			float pitchOffset = MAX_PITCH_OFFSET / Math.max(Math.min(speed, MAX_PITCH_OFFSET), 1.0f);

			player.setPitch(player.getPitch() - pitchOffset);

			ItemStack thrownStack = player.isCreative() ? stack.copyWithCount(1) : stack.split(1);
			DaggerEntity daggerEntity = (DaggerEntity) ProjectileEntity.spawnWithVelocity(
				(spawnWorld, owner, projectileStack) -> {
					DaggerEntity spawned = new DaggerEntity(spawnWorld, owner, projectileStack);
					spawned.setThrowCharge(chargeProgress);
					return spawned;
				},
				serverWorld,
				thrownStack,
				player,
				0.0F,
				speed,
				0.0F
			);

			player.setPitch(player.getPitch() + pitchOffset);

			if (player.isCreative()) {
				daggerEntity.pickupType = PickupPermission.CREATIVE_ONLY;
			}

			world.playSoundFromEntity(null, daggerEntity, SoundEvents.ITEM_TRIDENT_THROW.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
			return true;
		}

		return false;
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);

		if (itemStack.willBreakNextUse()) {
			return ActionResult.FAIL;
		}
		if (EnchantmentHelper.getTridentSpinAttackStrength(itemStack, user) > 0.0F && !user.isTouchingWaterOrRain()) {
			return ActionResult.FAIL;
		}
		user.setCurrentHand(hand);
		return ActionResult.CONSUME;
	}

	@Override
	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		DaggerEntity daggerEntity = new DaggerEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
		daggerEntity.pickupType = PickupPermission.ALLOWED;
		daggerEntity.setThrowCharge(1.0f);
		return daggerEntity;
	}

	/**
	 * Linear charge fraction between {@link #MIN_THROW_TICKS} and {@link #FULL_CHARGE_TICKS}.
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

	@Override
	@Deprecated
	public void appendTooltip(
		ItemStack stack,
		TooltipContext context,
		net.minecraft.component.type.TooltipDisplayComponent displayComponent,
		Consumer<Text> textConsumer,
		TooltipType type
	) {
		textConsumer.accept(Text.translatable("item.daggercrafting.dagger.when_thrown").formatted(Formatting.GRAY));
		DaggerToolMaterial mat = this.getMaterial();
		textConsumer.accept(
			Text.literal(" ").append(
				Text.translatable("item.daggercrafting.dagger.ranged_damage", mat.getRangedDamage()).formatted(Formatting.DARK_GREEN)
			)
		);
		textConsumer.accept(
			Text.literal(" ").append(
				Text.translatable("item.daggercrafting.dagger.ranged_velocity", mat.getRangedVelocity()).formatted(Formatting.DARK_GREEN)
			)
		);
	}
}
