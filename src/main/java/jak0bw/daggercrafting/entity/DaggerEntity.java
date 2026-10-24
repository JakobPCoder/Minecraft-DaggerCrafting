package jak0bw.daggercrafting.entity;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.DaggerToolMaterial;
import jak0bw.daggercrafting.item.DaggerItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Thrown dagger projectile: AbstractArrow with trident-like loyalty, material throw damage, and custom return motion.
 */
public class DaggerEntity extends AbstractArrow implements ItemSupplier {
	private static final EntityDataAccessor<Byte> ID_LOYALTY = SynchedEntityData.defineId(DaggerEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> ID_FOIL = SynchedEntityData.defineId(DaggerEntity.class, EntityDataSerializers.BOOLEAN);

	public String itemId;
	public float seconds;

	private boolean dealtDamage;
	public int returnTimer;

	private boolean approachingTargetSoundPlayed = false;
	private static final int APPROACH_SOUND_THRESHOLD_TICKS = 5;
	private static final double RETURN_SPEED_MULTIPLIER = 0.15;

	public DaggerEntity(EntityType<? extends DaggerEntity> type, Level level) {
		super(type, level);
		System.out.println("DaggerEntity Constructor 1");
		System.out.println("DaggerEntity Constructor 1 entityType: " + type);
	}

	@SuppressWarnings("unchecked")
	public DaggerEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(
			(EntityType<? extends AbstractArrow>) BuiltInRegistries.ENTITY_TYPE.getValue(getIdentifierFromStack(stack)),
			owner,
			level,
			stack,
			null
		);
		System.out.println("DaggerEntity Constructor 2 stack: " + stack);
		this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(stack));
		this.entityData.set(ID_FOIL, stack.hasFoil());
		System.out.println("DaggerEntity Constructor 2");
	}

	@SuppressWarnings("unchecked")
	public DaggerEntity(Level level, double x, double y, double z, ItemStack stack) {
		super(
			(EntityType<? extends AbstractArrow>) BuiltInRegistries.ENTITY_TYPE.getValue(getIdentifierFromStack(stack)),
			x,
			y,
			z,
			level,
			stack,
			null
		);
		System.out.println("DaggerEntity Constructor 3 stack: " + stack);
		this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(stack));
		this.entityData.set(ID_FOIL, stack.hasFoil());
		System.out.println("DaggerEntity Constructor 3");
	}

	private static Identifier getIdentifierFromStack(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	@Override
	public ItemStack getItem() {
		return this.getPickupItemStackOrigin();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ID_LOYALTY, (byte) 0);
		builder.define(ID_FOIL, false);
	}

	@Override
	public void tick() {
		if (this.inGroundTime > 4) {
			this.dealtDamage = true;
		}

		Entity entity = this.getOwner();
		int loyalty = this.entityData.get(ID_LOYALTY);
		if (loyalty > 0 && (this.dealtDamage || this.isNoPhysics()) && entity != null) {
			if (!this.isAcceptibleReturnOwner()) {
				if (this.level() instanceof ServerLevel serverLevel && this.pickup == Pickup.ALLOWED) {
					this.spawnAtLocation(serverLevel, this.getPickupItem(), 0.1F);
				}

				this.discard();
			} else {
				if (!(entity instanceof Player) && this.position().distanceTo(entity.getEyePosition()) < entity.getBbWidth() + 1.0) {
					this.discard();
					return;
				}

				this.setNoPhysics(true);
				this.applyLinearReturnMotion(entity, loyalty);

				double timeToTarget = this.predictTimeToTarget(entity);
				float basePitch = 0.8f;
				float shiftUpFactor = 1.0F + 0.5F * (float) loyalty;
				if (timeToTarget < APPROACH_SOUND_THRESHOLD_TICKS && !approachingTargetSoundPlayed) {
					// Arrival cue, 30% quieter than the previous 2.0 volume.
					this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.4F, basePitch * shiftUpFactor);
					approachingTargetSoundPlayed = true;
				}

				if (this.returnTimer == 0) {
					// Start-of-return cue, 30% quieter than the previous 0.7 volume.
					this.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.49F, basePitch);
				}
				this.returnTimer++;
			}
		}

		super.tick();

		if (!this.isInGround()) {
			Vec3 velocity = this.getDeltaMovement();
			if (velocity.lengthSqr() > 0.0001) {
				float yaw = (float) Math.toDegrees(Mth.atan2(velocity.x, velocity.z));
				float pitch = (float) Math.toDegrees(Mth.atan2(velocity.y, velocity.horizontalDistance()));
				if (this.returnTimer > 0) {
					yaw += 180.0f;
					pitch = -pitch;
				}
				this.setYRot(yaw);
				this.setXRot(pitch);
				this.seconds = (float) this.tickCount / 20.0f;
			}
		}

		if (!this.level().isClientSide()) {
			double velocitySquared = this.getDeltaMovement().lengthSqr();
			if (velocitySquared > 2.0) {
				this.hurtMarked = true;
			}
		}
	}

	private boolean isAcceptibleReturnOwner() {
		Entity entity = this.getOwner();
		if (entity != null && entity.isAlive()) {
			return !(entity instanceof ServerPlayer) || !entity.isSpectator();
		} else {
			return false;
		}
	}

	public boolean isFoil() {
		return this.entityData.get(ID_FOIL);
	}

	@Override
	protected @Nullable EntityHitResult findHitEntity(Vec3 currentPosition, Vec3 nextPosition) {
		return this.dealtDamage ? null : super.findHitEntity(currentPosition, nextPosition);
	}

	public DaggerToolMaterial getMaterial() {
		Item itemRef = this.getPickupItemStackOrigin().getItem();
		return DaggerItem.getMaterialFromItem(itemRef);
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		Entity targetEntity = entityHitResult.getEntity();
		DaggerToolMaterial material = this.getMaterial();
		float damageAmount = material.getRangedDamage();

		Entity ownerEntity = this.getOwner();
		DamageSource damageSource = this.damageSources().trident(this, ownerEntity == null ? this : ownerEntity);

		if (this.level() instanceof ServerLevel serverLevel) {
			damageAmount = EnchantmentHelper.modifyDamage(serverLevel, this.getWeaponItem(), targetEntity, damageSource, damageAmount);
		}

		this.dealtDamage = true;
		if (targetEntity.hurtOrSimulate(damageSource, damageAmount)) {
			if (targetEntity.is(EntityTypes.ENDERMAN)) {
				return;
			}

			if (this.level() instanceof ServerLevel serverLevel) {
				EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(
					serverLevel,
					targetEntity,
					damageSource,
					this.getWeaponItem(),
					item -> this.kill(serverLevel)
				);
			}

			if (targetEntity instanceof LivingEntity livingEntity) {
				this.doKnockback(livingEntity, damageSource);
				this.doPostHurtEffects(livingEntity);
			}
		}

		if (!this.level().isClientSide() && this.isOnFire() && targetEntity instanceof LivingEntity living && !living.fireImmune()) {
			living.igniteForSeconds(4);
		}

		this.deflect(ProjectileDeflection.REVERSE, targetEntity, this.owner, false);
		this.setDeltaMovement(this.getDeltaMovement().multiply(0.02, 0.2, 0.02));
		this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
	}

	@Override
	protected void hitBlockEnchantmentEffects(ServerLevel level, BlockHitResult blockHitResult, ItemStack weaponStack) {
		Vec3 hit = blockHitResult.getBlockPos().clampLocationWithin(blockHitResult.getLocation());
		LivingEntity user = this.getOwner() instanceof LivingEntity livingEntity ? livingEntity : null;

		EnchantmentHelper.onHitBlock(
			level,
			weaponStack,
			user,
			this,
			null,
			hit,
			level.getBlockState(blockHitResult.getBlockPos()),
			item -> this.kill(level)
		);
	}

	@Override
	public ItemStack getWeaponItem() {
		return this.getPickupItemStackOrigin();
	}

	@Override
	protected boolean tryPickup(Player player) {
		return super.tryPickup(player)
			|| this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		String itemPath = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
		Identifier itemId = Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, itemPath);
		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		if (item != null && item != Items.AIR) {
			System.out.println("DaggerEntity getDefaultPickupItem item SUCCESS");
			return new ItemStack(item);
		} else {
			System.out.println("DaggerEntity getDefaultPickupItem item FAIL");
			return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, "diamond_dagger")));
		}
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	@Override
	public void playerTouch(Player player) {
		if (this.ownedBy(player) || this.getOwner() == null) {
			super.playerTouch(player);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.dealtDamage = input.getBooleanOr("DealtDamage", false);
		this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(this.getPickupItemStackOrigin()));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("DealtDamage", this.dealtDamage);
	}

	private byte getLoyaltyFromItem(ItemStack stack) {
		if (this.level() instanceof ServerLevel serverLevel) {
			return (byte) Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, this), 0, 127);
		} else {
			return 0;
		}
	}

	@Override
	public void tickDespawn() {
		int loyalty = this.entityData.get(ID_LOYALTY);
		if (this.pickup != Pickup.ALLOWED || loyalty <= 0) {
			super.tickDespawn();
		}
	}

	@Override
	protected float getWaterInertia() {
		return 0.99F;
	}

	@Override
	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		return true;
	}

	private void applyLinearReturnMotion(Entity target, int loyaltyLevel) {
		Vec3 toTarget = target.getEyePosition().subtract(this.position());
		double distance = toTarget.length();

		DaggerToolMaterial material = this.getMaterial();
		double baseSpeed = material != null ? material.getRangedVelocity() : 1.0;
		double maxspeed = Math.min(RETURN_SPEED_MULTIPLIER * baseSpeed * loyaltyLevel, distance);
		double currentspeed = this.getDeltaMovement().length();

		double acceleration = 0.05 * baseSpeed * loyaltyLevel;
		double nextSpeed;
		if (currentspeed < maxspeed) {
			nextSpeed = Math.min(currentspeed + acceleration, maxspeed);
		} else if (currentspeed > maxspeed) {
			nextSpeed = Math.max(currentspeed - acceleration, maxspeed);
		} else {
			nextSpeed = maxspeed;
		}

		if (distance < 0.01) {
			this.setDeltaMovement(Vec3.ZERO);
			return;
		}

		Vec3 velocity = toTarget.normalize().scale(nextSpeed);
		this.setDeltaMovement(velocity);
	}

	private double predictTimeToTarget(Entity target) {
		Vec3 toTarget = target.getEyePosition().subtract(this.position());
		double distance = toTarget.length();
		double speed = this.getDeltaMovement().length();
		if (speed < 0.0001) {
			return Double.POSITIVE_INFINITY;
		} else {
			return distance / speed;
		}
	}

	@Override
	protected void doKnockback(LivingEntity target, DamageSource source) {
		double baseKnockback = this.getWeaponItem() != null && this.level() instanceof ServerLevel serverWorld
			? EnchantmentHelper.modifyKnockback(serverWorld, this.getWeaponItem(), target, source, 0.5f)
			: 0.0F;
		if (baseKnockback > 0.0) {
			double resistanceFactor = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
			Vec3 velocity = this.getDeltaMovement();
			velocity = new Vec3(velocity.x * Math.abs(velocity.x), velocity.y * Math.abs(velocity.y), velocity.z * Math.abs(velocity.z));
			velocity = velocity.scale(baseKnockback * resistanceFactor * 0.15f);

			System.out.println("DaggerEntity knockback length: " + velocity.length());
			if (velocity.lengthSqr() > 0.0) {
				target.push(velocity.x, Math.max(0.05, velocity.y), velocity.z);
			}
		}
	}
}
