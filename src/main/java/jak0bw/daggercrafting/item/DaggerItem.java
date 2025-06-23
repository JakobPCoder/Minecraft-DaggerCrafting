package jak0bw.daggercrafting.item;

import jak0bw.daggercrafting.entity.DaggerEntity;
import jak0bw.daggercrafting.ModEntities;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ToolComponent;
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
import net.minecraft.item.ProjectileItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DaggerItem extends Item implements ProjectileItem {
	public static final int MIN_DRAW_DURATION = 10;
	public static final float ATTACK_DAMAGE = 8.0F;
	public static final float THROW_SPEED = 2.5F;

	public DaggerItem(Item.Settings settings) {
		super(settings);
	}

	public static AttributeModifiersComponent createAttributeModifiers() {
		return AttributeModifiersComponent.builder()
			.add(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, 8.0, Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
			.add(EntityAttributes.ATTACK_SPEED, new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, -2.9000000953674316, Operation.ADD_VALUE), AttributeModifierSlot.MAINHAND)
			.build();
	}

	public static ToolComponent createToolComponent() {
		return new ToolComponent(List.of(), 1.0F, 2);
	}

	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		return !miner.isCreative();
	}

	public UseAction getUseAction(ItemStack stack) {
		return UseAction.SPEAR;
	}

	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return 72000;
	}

	public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		if (user instanceof PlayerEntity playerEntity) {
			int var6 = this.getMaxUseTime(stack, user) - remainingUseTicks;
			if (var6 < 10) {
				return false;
			} else {
				float f = EnchantmentHelper.getTridentSpinAttackStrength(stack, playerEntity);
				if (f > 0.0F && !playerEntity.isTouchingWaterOrRain()) {
					return false;
				} else if (stack.willBreakNextUse()) {
					return false;
				} else {
					RegistryEntry<SoundEvent> registryEntry = (RegistryEntry)EnchantmentHelper.getEffect(stack, EnchantmentEffectComponentTypes.TRIDENT_SOUND).orElse(SoundEvents.ITEM_TRIDENT_THROW);
					playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
					if (world instanceof ServerWorld) {
						ServerWorld serverWorld = (ServerWorld)world;
						stack.damage(1, playerEntity);
						if (f == 0.0F) {
							DaggerEntity daggerEntity = (DaggerEntity)ProjectileEntity.spawnWithVelocity(DaggerEntity::new, serverWorld, stack.copy(), playerEntity, 0.0F, 2.5F, 1.0F);
							if (playerEntity.isCreative()) {
								daggerEntity.pickupType = PickupPermission.CREATIVE_ONLY;
							} else {
								playerEntity.getInventory().removeOne(stack);
							}

							world.playSoundFromEntity((PlayerEntity)null, daggerEntity, (SoundEvent)registryEntry.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
							return true;
						}
					}

					if (f > 0.0F) {
						float g = playerEntity.getYaw();
						float h = playerEntity.getPitch();
						float j = -MathHelper.sin(g * 0.017453292F) * MathHelper.cos(h * 0.017453292F);
						float k = -MathHelper.sin(h * 0.017453292F);
						float l = MathHelper.cos(g * 0.017453292F) * MathHelper.cos(h * 0.017453292F);
						float m = MathHelper.sqrt(j * j + k * k + l * l);
						j *= f / m;
						k *= f / m;
						l *= f / m;
						playerEntity.addVelocity((double)j, (double)k, (double)l);
						playerEntity.useRiptide(20, 8.0F, stack);
						if (playerEntity.isOnGround()) {
							float n = 1.1999999F;
							playerEntity.move(MovementType.SELF, new Vec3d(0.0, 1.1999999284744263, 0.0));
						}

						world.playSoundFromEntity((PlayerEntity)null, playerEntity, (SoundEvent)registryEntry.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
						return true;
					} else {
						return false;
					}
				}
			}
		} else {
			return false;
		}
	}

	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);
		if (itemStack.willBreakNextUse()) {
			return ActionResult.FAIL;
		} else if (EnchantmentHelper.getTridentSpinAttackStrength(itemStack, user) > 0.0F && !user.isTouchingWaterOrRain()) {
			return ActionResult.FAIL;
		} else {
			user.setCurrentHand(hand);
			return ActionResult.CONSUME;
		}
	}

	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		return true;
	}

	public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.damage(1, attacker, EquipmentSlot.MAINHAND);
	}

	public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
		DaggerEntity daggerEntity = new DaggerEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1));
		daggerEntity.pickupType = PickupPermission.ALLOWED;
		return daggerEntity;
	}
}
