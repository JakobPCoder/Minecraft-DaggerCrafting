package jak0bw.daggercrafting.entity;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.DaggerToolMaterial;
import jak0bw.daggercrafting.ModEnchantments;
import jak0bw.daggercrafting.item.DaggerItem;
import jak0bw.daggercrafting.particle.ParticleDirector;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;

/**
 * Server-authoritative thrown dagger: one {@link EntityType} per material tier, arrow physics underneath.
 *
 * Tier resolution uses matching item/entity ids per {@link jak0bw.daggercrafting.ModEntities}. Loyalty and
 * Restitution behavior mirror trident parity from 1.4.0; client trails go through {@link ParticleDirector}.
 */
public class DaggerEntity extends PersistentProjectileEntity implements FlyingItemEntity {

	private static final TrackedData<Byte> LOYALTY;
	private static final TrackedData<Boolean> ENCHANTED;
	private static final TrackedData<Float> THROW_CHARGE;

	/** Unused in current logic; kept for debugging or external mods referencing item id strings. */
	public String itemId;

	/** Airtime in seconds ({@code age / 20}); {@link DaggerEntityRenderer} uses this for ease-out spin. */
	public float seconds;

	/** Once true, entity collision is disabled and loyalty return may begin. */
	private boolean dealtDamage;

	/** Ticks spent in loyalty homing after {@link #dealtDamage}; drives model flip and return sound cadence. */
	public int returnTimer;

	private boolean approachingTargetSoundPlayed = false;

	private static final int APPROACH_SOUND_THRESHOLD_TICKS = 5;
	private static final float RETURN_SOUND_BASE_PITCH = 0.8F;
	private static final float ARRIVAL_SOUND_VOLUME = 1.4F;
	private static final double RETURN_SPEED_MULTIPLIER = 0.15;

	public DaggerEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	@SuppressWarnings("unchecked")
	public DaggerEntity(World world, LivingEntity owner, ItemStack stack) {
		super(
			(EntityType<? extends PersistentProjectileEntity>) Registries.ENTITY_TYPE.get(getIdentifierFromStack(stack)),
			owner,
			world,
			stack,
			null
		);
		this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
		this.dataTracker.set(ENCHANTED, stack.hasGlint());
	}

	@SuppressWarnings("unchecked")
	public DaggerEntity(World world, double x, double y, double z, ItemStack stack) {
		super(
			(EntityType<? extends PersistentProjectileEntity>) Registries.ENTITY_TYPE.get(getIdentifierFromStack(stack)),
			x,
			y,
			z,
			world,
			stack,
			null
		);
		this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
		this.dataTracker.set(ENCHANTED, stack.hasGlint());
	}

	private static Identifier getIdentifierFromStack(ItemStack stack) {
		return Registries.ITEM.getId(stack.getItem());
	}

	@Override
	public ItemStack getStack() {
		return this.getItemStack();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(LOYALTY, (byte) 0);
		builder.add(ENCHANTED, false);
		builder.add(THROW_CHARGE, 1.0F);
	}

	/**
	 * Per-tick simulation: loyalty return, then vanilla arrow physics, then visual orientation
	 * and the client particle trail.
	 */
	@Override
	public void tick() {
		if (this.inGroundTime > 4) {
			this.dealtDamage = true;
		}

		Entity entity = this.getOwner();
		int loyalty = this.dataTracker.get(LOYALTY);
		if (loyalty > 0 && (this.dealtDamage || this.isNoClip()) && entity != null) {
			if (!this.isOwnerAlive()) {
				if (this.getWorld() instanceof ServerWorld serverWorld && this.pickupType == PickupPermission.ALLOWED) {
					this.dropStack(serverWorld, this.asItemStack(), 0.1F);
				}
				this.discard();
			} else {
				if (!(entity instanceof PlayerEntity)
					&& this.getPos().distanceTo(entity.getEyePos()) < (double) entity.getWidth() + 1.0) {
					this.discard();
					return;
				}

				this.setNoClip(true);
				this.applyLinearReturnMotion(entity, loyalty);

				double timeToTarget = this.predictTimeToTarget(entity);
				if (timeToTarget < APPROACH_SOUND_THRESHOLD_TICKS && !approachingTargetSoundPlayed) {
					this.playReturnArrivalSound(loyalty);
					approachingTargetSoundPlayed = true;
				}

				if (this.returnTimer == 0) {
					this.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.49F, RETURN_SOUND_BASE_PITCH);
				}
				this.returnTimer++;
			}
		}

		super.tick();
		ParticleDirector.tick(this);

		if (!this.isInGround()) {
			Vec3d velocity = this.getVelocity();
			if (velocity.lengthSquared() > 0.0001) {
				float yaw = (float) Math.toDegrees(MathHelper.atan2(velocity.x, velocity.z));
				float pitch = (float) Math.toDegrees(MathHelper.atan2(velocity.y, velocity.horizontalLength()));
				if (this.returnTimer > 0) {
					yaw += 180.0f;
					pitch = -pitch;
				}
				this.setYaw(yaw);
				this.setPitch(pitch);
				this.seconds = (float) this.age / 20.0f;
			}
		}

		if (!this.getWorld().isClient) {
			double velocitySquared = this.getVelocity().lengthSquared();
			if (velocitySquared > 2.0) {
				this.velocityModified = true;
			}
		}
	}

	private boolean isOwnerAlive() {
		Entity entity = this.getOwner();
		if (entity != null && entity.isAlive()) {
			return !(entity instanceof ServerPlayerEntity) || !entity.isSpectator();
		}
		return false;
	}

	/** Exposed for render layers that need glint without reading the full stack on client. */
	public boolean isEnchanted() {
		return this.dataTracker.get(ENCHANTED);
	}

	/**
	 * @param throwCharge hold-charge fraction in {@code [0, 1]} captured at spawn
	 */
	public void setThrowCharge(float throwCharge) {
		this.dataTracker.set(THROW_CHARGE, MathHelper.clamp(throwCharge, 0.0F, 1.0F));
	}

	/**
	 * @return charge fraction used to lerp trail density and quad size; dispensers default to full ({@code 1})
	 */
	public float getThrowCharge() {
		return this.dataTracker.get(THROW_CHARGE);
	}

	@Override
	@Nullable
	protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
		return this.dealtDamage ? null : super.getEntityCollision(currentPosition, nextPosition);
	}

	public DaggerToolMaterial getMaterial() {
		Item itemRef = this.getItemStack().getItem();
		return DaggerItem.getMaterialFromItem(itemRef);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		Entity targetEntity = entityHitResult.getEntity();
		DaggerToolMaterial material = this.getMaterial();
		float damageAmount = material.getRangedDamage();

		Entity ownerEntity = this.getOwner();
		DamageSource damageSource = this.getDamageSources().trident(this, ownerEntity == null ? this : ownerEntity);

		if (this.getWorld() instanceof ServerWorld serverWorld) {
			damageAmount = EnchantmentHelper.getDamage(serverWorld, this.getWeaponStack(), targetEntity, damageSource, damageAmount);
		}

		this.dealtDamage = true;
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			if (targetEntity.damage(serverWorld, damageSource, damageAmount)) {
				EnchantmentHelper.onTargetDamaged(
					serverWorld,
					targetEntity,
					damageSource,
					this.getWeaponStack(),
					item -> this.kill(serverWorld)
				);

				if (targetEntity.getType() != EntityType.ENDERMAN && targetEntity instanceof LivingEntity livingEntity) {
					this.knockback(livingEntity, damageSource);
					this.onHit(livingEntity);
				}
			}
		} else if (targetEntity instanceof LivingEntity livingEntity) {
			targetEntity.clientDamage(damageSource);
			this.knockback(livingEntity, damageSource);
		}

		if (!this.getWorld().isClient && this.isOnFire() && targetEntity instanceof LivingEntity living && !living.isFireImmune()) {
			// Vanilla arrow parity: Flame burns the struck target for 5 seconds (PersistentProjectileEntity.onEntityHit).
			living.setOnFireFor(5.0F);
		}

		this.deflect(ProjectileDeflection.SIMPLE, targetEntity, this.getOwner(), false);
		this.setVelocity(this.getVelocity().multiply(0.02, 0.2, 0.02));
		this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);
		this.tryRestitutionReturn();
	}

	/**
	 * True when the thrown stack has Restitution. Safe on client and server via world registries.
	 *
	 * @return {@code false} if the datapack enchantment is missing or the stack is unenchanted
	 */
	public boolean hasRestitution() {
		RegistryEntry<Enchantment> restitution = this.getWorld().getRegistryManager()
			.getOrThrow(RegistryKeys.ENCHANTMENT)
			.getOptional(ModEnchantments.RESTITUTION)
			.orElse(null);
		if (restitution == null) {
			return false;
		}
		return EnchantmentHelper.getLevel(restitution, this.getItemStack()) > 0;
	}

	private void tryRestitutionReturn() {
		if (!(this.getWorld() instanceof ServerWorld serverWorld) || this.isRemoved()) {
			return;
		}
		if (!this.hasRestitution()) {
			return;
		}

		Entity owner = this.getOwner();
		if (!(owner instanceof ServerPlayerEntity player) || !player.isAlive() || player.isSpectator()) {
			return;
		}

		this.playReturnArrivalSound(1);

		if (!player.isCreative()) {
			ItemStack stack = this.asItemStack();
			if (!player.getInventory().insertStack(stack)) {
				this.dropStack(serverWorld, stack, 0.1F);
			}
		}

		this.discard();
	}

	/**
	 * Last-moment catch cue used by Loyalty approach and Restitution instant return.
	 *
	 * @param loyaltyLevel 1–3 for Loyalty, or 1 for Restitution so the catch is audible
	 */
	private void playReturnArrivalSound(int loyaltyLevel) {
		float shiftUpFactor = 1.0F + 0.5F * (float) loyaltyLevel;
		this.playSound(
			SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
			ARRIVAL_SOUND_VOLUME,
			RETURN_SOUND_BASE_PITCH * shiftUpFactor
		);
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		Vec3d motion = this.getVelocity();
		if (this.getWorld().isClient) {
			ParticleDirector.hitBlock(
				this,
				new ParticleDirector.HitContext(
					blockHitResult.getPos(),
					motion,
					this.getWorld().getBlockState(blockHitResult.getBlockPos()),
					blockHitResult.getSide()
				)
			);
		}
		super.onBlockHit(blockHitResult);
	}

	@Override
	protected void onBlockHitEnchantmentEffects(ServerWorld world, BlockHitResult blockHitResult, ItemStack weaponStack) {
		Vec3d hit = blockHitResult.getBlockPos().clampToWithin(blockHitResult.getPos());
		LivingEntity user = this.getOwner() instanceof LivingEntity livingEntity ? livingEntity : null;

		EnchantmentHelper.onHitBlock(
			world,
			weaponStack,
			user,
			this,
			null,
			hit,
			world.getBlockState(blockHitResult.getBlockPos()),
			item -> this.kill(world)
		);
	}

	@Override
	public ItemStack getWeaponStack() {
		return this.getItemStack();
	}

	@Override
	protected boolean tryPickup(PlayerEntity player) {
		return super.tryPickup(player)
			|| this.isNoClip() && this.isOwner(player) && player.getInventory().insertStack(this.asItemStack());
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		String itemPath = Registries.ENTITY_TYPE.getId(this.getType()).getPath();
		Identifier itemId = Identifier.of(DaggerCrafting.MOD_ID, itemPath);
		Item item = Registries.ITEM.get(itemId);
		if (item != null && item != Items.AIR) {
			return new ItemStack(item);
		}
		DaggerCrafting.LOGGER.warn("No pickup item for dagger entity type {}, falling back to diamond_dagger", itemPath);
		return new ItemStack(Registries.ITEM.get(Identifier.of(DaggerCrafting.MOD_ID, "diamond_dagger")));
	}

	@Override
	protected SoundEvent getHitSound() {
		return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (this.isOwner(player) || this.getOwner() == null) {
			super.onPlayerCollision(player);
		}
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.dealtDamage = nbt.getBoolean("DealtDamage").orElse(false);
		this.dataTracker.set(LOYALTY, this.getLoyalty(this.getItemStack()));
		this.setThrowCharge(nbt.getFloat("ThrowCharge").orElse(1.0F));
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("DealtDamage", this.dealtDamage);
		nbt.putFloat("ThrowCharge", this.getThrowCharge());
	}

	private byte getLoyalty(ItemStack stack) {
		World world = this.getWorld();
		if (world instanceof ServerWorld serverWorld) {
			return (byte) MathHelper.clamp(EnchantmentHelper.getTridentReturnAcceleration(serverWorld, stack, this), 0, 127);
		}
		return 0;
	}

	@Override
	public void age() {
		int loyalty = this.dataTracker.get(LOYALTY);
		if (this.pickupType != PickupPermission.ALLOWED || loyalty <= 0) {
			super.age();
		}
	}

	@Override
	protected float getDragInWater() {
		return 0.99F;
	}

	@Override
	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		return true;
	}

	/**
	 * Public wrapper for {@link #isInGround()} so {@link ParticleDirector} can gate trail presets.
	 */
	public boolean isStuckInGround() {
		return this.isInGround();
	}

	private void applyLinearReturnMotion(Entity target, int loyaltyLevel) {
		Vec3d toTarget = target.getEyePos().subtract(this.getPos());
		double distance = toTarget.length();

		DaggerToolMaterial material = this.getMaterial();
		double baseSpeed = material != null ? material.getRangedVelocity() : 1.0;
		double maxspeed = Math.min(RETURN_SPEED_MULTIPLIER * baseSpeed * loyaltyLevel, distance);
		double currentspeed = this.getVelocity().length();

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
			this.setVelocity(Vec3d.ZERO);
			return;
		}

		Vec3d velocity = toTarget.normalize().multiply(nextSpeed);
		this.setVelocity(velocity);
	}

	private double predictTimeToTarget(Entity target) {
		Vec3d toTarget = target.getEyePos().subtract(this.getPos());
		double distance = toTarget.length();
		double speed = this.getVelocity().length();
		if (speed < 0.0001) {
			return Double.POSITIVE_INFINITY;
		}
		return distance / speed;
	}

	@Override
	protected void knockback(LivingEntity target, DamageSource source) {
		double baseKnockback = this.getWeaponStack() != null && this.getWorld() instanceof ServerWorld serverWorld
			? EnchantmentHelper.modifyKnockback(serverWorld, this.getWeaponStack(), target, source, 0.5f)
			: 0.0F;
		if (baseKnockback > 0.0) {
			double resistanceFactor = Math.max(
				0.0,
				1.0 - target.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.KNOCKBACK_RESISTANCE)
			);
			Vec3d velocity = this.getVelocity();
			velocity = new Vec3d(
				velocity.x * Math.abs(velocity.x),
				velocity.y * Math.abs(velocity.y),
				velocity.z * Math.abs(velocity.z)
			);
			velocity = velocity.multiply(baseKnockback * resistanceFactor * 0.15f);

			if (velocity.lengthSquared() > 0.0) {
				target.addVelocity(velocity.x, Math.max(0.05, velocity.y), velocity.z);
			}
		}
	}

	static {
		LOYALTY = DataTracker.registerData(DaggerEntity.class, TrackedDataHandlerRegistry.BYTE);
		ENCHANTED = DataTracker.registerData(DaggerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
		THROW_CHARGE = DataTracker.registerData(DaggerEntity.class, TrackedDataHandlerRegistry.FLOAT);
	}
}
