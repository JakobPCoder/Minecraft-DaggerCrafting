package jak0bw.daggercrafting.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpectralArrowItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
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

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.ModEntities;
import jak0bw.daggercrafting.ModItems;
import jak0bw.daggercrafting.item.DaggerItem;
import jak0bw.daggercrafting.item.DaggerToolMaterial;

public class DaggerEntity extends PersistentProjectileEntity implements FlyingItemEntity{

    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    public String itemId;
    public float seconds;

	/**
	 * Whether the dagger has dealt damage.
	 */
	private boolean dealtDamage;

	/**
	 * Timer for dagger returning.
	 */
	public int returnTimer;

    private boolean approachingTargetSoundPlayed = false;
    private static final int APPROACH_SOUND_THRESHOLD_TICKS = 5;
    private static final double RETURN_SPEED_MULTIPLIER = 0.1;


	public DaggerEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
		super(entityType, world);
        System.out.println("DaggerEntity Constructor 1");
        System.out.println("DaggerEntity Constructor 1 entityType: " + entityType);
	}

	public DaggerEntity(World world, LivingEntity owner, ItemStack stack) {
        super((EntityType<? extends PersistentProjectileEntity>)Registries.ENTITY_TYPE.get(Identifier.of(DaggerCrafting.MOD_ID, Registries.ITEM.getId(stack.getItem()).getPath())), owner, world, stack, (ItemStack)null);
        System.out.println("DaggerEntity Constructor 2 stack: " + stack);
		this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
		this.dataTracker.set(ENCHANTED, stack.hasGlint());
        System.out.println("DaggerEntity Constructor 2");
	}

	public DaggerEntity(World world, double x, double y, double z, ItemStack stack) {
		super((EntityType<? extends PersistentProjectileEntity>)Registries.ENTITY_TYPE.get(Identifier.of(DaggerCrafting.MOD_ID, Registries.ITEM.getId(stack.getItem()).getPath())), x, y, z, world, stack, (ItemStack)null);
        System.out.println("DaggerEntity Constructor 3 stack: " + stack);
		this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
		this.dataTracker.set(ENCHANTED, stack.hasGlint());
        System.out.println("DaggerEntity Constructor 3");
	}


    @Override
    public ItemStack getStack() {
        return this.getItemStack();
    }

	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(LOYALTY, (byte) 0);
		builder.add(ENCHANTED, false);
	}

        
	public void tick() {
		if (this.inGroundTime > 4) {
			this.dealtDamage = true;
		}

		Entity entity = this.getOwner();
		int i = (Byte) this.dataTracker.get(LOYALTY);
		if (i > 0 && (this.dealtDamage || this.isNoClip()) && entity != null) {
			if (!this.isOwnerAlive()) {
				World var4 = this.getWorld();
                
				if (var4 instanceof ServerWorld) {
					ServerWorld serverWorld = (ServerWorld) var4;
					if (this.pickupType == PickupPermission.ALLOWED) {
						this.dropStack(serverWorld, this.asItemStack(), 0.1F);
					}
				}

				this.discard();
			} else {
				if (!(entity instanceof PlayerEntity)
						&& this.getPos().distanceTo(entity.getEyePos()) < (double) entity.getWidth() + 1.0) {
					this.discard();
					return;
				}

				this.setNoClip(true);
				this.applyLinearReturnMotion(entity, i);

				// Predict time to target and play sound if needed
				double timeToTarget = this.predictTimeToTarget(entity);
				float basePitch = 0.8f;
				float shiftUpFactor = 1.0F + 0.5F * (float)i;
				if (timeToTarget < APPROACH_SOUND_THRESHOLD_TICKS && !approachingTargetSoundPlayed) {
					this.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 2.0F, basePitch * shiftUpFactor);
					approachingTargetSoundPlayed = true;
				}

				if (this.returnTimer == 0) {
					this.playSound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.7F, basePitch);
				}
				this.returnTimer++;
			}
		}

		super.tick();

        // Update orientation based on velocity unless in ground
        if (!this.isInGround()) {
            Vec3d velocity = this.getVelocity();
            if (velocity.lengthSquared() > 0.0001) {
                float yaw = (float)Math.toDegrees(MathHelper.atan2(velocity.x, velocity.z));
                float pitch = (float)Math.toDegrees(MathHelper.atan2(velocity.y, velocity.horizontalLength()));
                if (this.returnTimer > 0) {
                    // Flip 180 degrees for hilt-first return
                    yaw += 180.0f;
                    pitch = -pitch;
                }
                this.setYaw(yaw);
                this.setPitch(pitch);
                this.seconds = (float)this.age / 20.0f;
            }
        }

        // Dynamic synchronization based on velocity
        if (!this.getWorld().isClient) {
            double velocitySquared = this.getVelocity().lengthSquared();
            // Force sync more frequently as speed increases
            // Base threshold: 1.0 blocks/tick, increases sync frequency with speed
            if (velocitySquared > 1.0) {
                this.velocityModified = true; // Force position sync to clients
                
                // For very high speeds, force more frequent updates
                if (velocitySquared > 8.0) { // > 4.0 blocks/tick
                    this.velocityDirty = true; // Force velocity sync every tick
                }
            }
        }

	}

	private boolean isOwnerAlive() {
		Entity entity = this.getOwner();
		if (entity != null && entity.isAlive()) {
			return !(entity instanceof ServerPlayerEntity) || !entity.isSpectator();
		} else {
			return false;
		}
	}

	public boolean isEnchanted() {
		return (Boolean) this.dataTracker.get(ENCHANTED);
	}

	@Nullable
	protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
		return this.dealtDamage ? null : super.getEntityCollision(currentPosition, nextPosition);
	}

	public DaggerToolMaterial getMaterial() {
		ItemStack itemStack = this.getItemStack();
		Item itemRef = itemStack.getItem();
		return DaggerItem.getMaterialFromItem(itemRef);
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		Entity targetEntity = entityHitResult.getEntity();
		DaggerToolMaterial material = this.getMaterial();
		float damageAmount = material.getRangedDamage();

		Entity ownerEntity = this.getOwner();
		DamageSource damageSource = this.getDamageSources().trident(this, ownerEntity == null ? this : ownerEntity);

		// 1. Damage, enchantment effects, and removal: SERVER ONLY
		if (this.getWorld() instanceof ServerWorld serverWorld) {
			damageAmount = EnchantmentHelper.getDamage(serverWorld, this.getWeaponStack(), targetEntity, damageSource, damageAmount);

			this.dealtDamage = true;
			if (targetEntity.damage(serverWorld, damageSource, damageAmount)) {
				if (targetEntity.getType() == EntityType.ENDERMAN) return;

				EnchantmentHelper.onTargetDamaged(serverWorld, targetEntity, damageSource, this.getWeaponStack(), (item) -> this.kill(serverWorld));

				if (targetEntity instanceof LivingEntity livingEntity) {
					this.knockback(livingEntity, damageSource); // Knockback: both sides, but server is authoritative
					this.onHit(livingEntity);
				}
			}
		} else {
			// 2. Client: play hit animation, knockback for visuals
			targetEntity.clientDamage(damageSource);
			if (targetEntity instanceof LivingEntity livingEntity) {
				this.knockback(livingEntity, damageSource); // Visual only, server will correct if needed
			}
		}

		// 3. Set target on fire if dagger is burning: SERVER ONLY
		if (!this.getWorld().isClient && this.isOnFire() && targetEntity instanceof LivingEntity living && !living.isFireImmune()) {
			living.setOnFireFor(4);
		}

		// 4. Deflection and velocity: BOTH SIDES for smooth visuals
		this.deflect(ProjectileDeflection.SIMPLE, targetEntity, this.getOwner(), false);
		this.setVelocity(this.getVelocity().multiply(0.02, 0.2, 0.02));
		this.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.0F, 1.0F);
	}

	protected void onBlockHitEnchantmentEffects(ServerWorld world, BlockHitResult blockHitResult, ItemStack weaponStack) {
		Vec3d vec3d = blockHitResult.getBlockPos().clampToWithin(blockHitResult.getPos());
		Entity var6 = this.getOwner();
		LivingEntity var10002;
		if (var6 instanceof LivingEntity livingEntity) {
			var10002 = livingEntity;
		} else {
			var10002 = null;
		}

		EnchantmentHelper.onHitBlock(world, weaponStack, var10002, this, (EquipmentSlot) null, vec3d,
				world.getBlockState(blockHitResult.getBlockPos()), (item) -> {
					this.kill(world);
				});
	}

	public ItemStack getWeaponStack() {
		return this.getItemStack();
	}

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
            System.out.println("DaggerEntity getDefaultItemStack item SUCCESS");
			return new ItemStack(item);

		} else {
            System.out.println("DaggerEntity getDefaultItemStack item FAIL");
			return new ItemStack(Registries.ITEM.get(Identifier.of(DaggerCrafting.MOD_ID, "diamond_dagger")));
		}
	}

	protected SoundEvent getHitSound() {
		return SoundEvents.ITEM_TRIDENT_HIT_GROUND;
	}

	public void onPlayerCollision(PlayerEntity player) {
		if (this.isOwner(player) || this.getOwner() == null) {
			super.onPlayerCollision(player);
		}

	}

	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		this.dealtDamage = nbt.getBoolean("DealtDamage");
		this.dataTracker.set(LOYALTY, this.getLoyalty(this.getItemStack()));
	}

	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putBoolean("DealtDamage", this.dealtDamage);
	}

	private byte getLoyalty(ItemStack stack) {
		World var3 = this.getWorld();
		if (var3 instanceof ServerWorld serverWorld) {
			return (byte) MathHelper.clamp(EnchantmentHelper.getTridentReturnAcceleration(serverWorld, stack, this), 0,
					127);
		} else {
			return 0;
		}
	}

	public void age() {
		int i = (Byte) this.dataTracker.get(LOYALTY);
		if (this.pickupType != PickupPermission.ALLOWED || i <= 0) {
			super.age();
		}

	}

	protected float getDragInWater() {
		return 0.99F;
	}

	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		return true;
	}

	/**
	 * Override to provide better synchronization for high-speed projectiles.
	 * This method ensures that fast-moving daggers send position updates more frequently
	 * to prevent visual lag between client and server.
	 */
	@Override
	public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
		// Dynamic interpolation scaling based on velocity
		double velocitySquared = this.getVelocity().lengthSquared();
		double velocity = Math.sqrt(velocitySquared);
		
		// Scale interpolation steps inversely with velocity
		// Faster projectiles need less interpolation for more responsive visuals
		if (velocity > 1.0) {
			// Reduce interpolation steps based on velocity
			// At 2.0 blocks/tick: steps = interpolationSteps / 2
			// At 4.0 blocks/tick: steps = interpolationSteps / 4
			// At 8.0 blocks/tick: steps = interpolationSteps / 8
			double reductionFactor = Math.max(1.0, velocity);
			interpolationSteps = Math.max(1, (int)(interpolationSteps / reductionFactor));
		}
		
		super.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, interpolationSteps);
	}

	private void applyLinearReturnMotion(Entity target, int loyaltyLevel) {
		Vec3d toTarget = target.getEyePos().subtract(this.getPos());
		double distance = toTarget.length();

		DaggerToolMaterial material = this.getMaterial();
		double baseSpeed = material != null ? material.getRangedVelocity() : 1.0;
		double maxspeed = Math.min(RETURN_SPEED_MULTIPLIER * baseSpeed * loyaltyLevel, distance);
		double currentspeed = this.getVelocity().length();

		double acceleration = 0.1 * baseSpeed * loyaltyLevel;
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

		// Always set velocity in the direction of the target, at the calculated speed
		Vec3d velocity = toTarget.normalize().multiply(nextSpeed);
		this.setVelocity(velocity);
	}

	private double predictTimeToTarget(Entity target) {
		Vec3d toTarget = target.getEyePos().subtract(this.getPos());
		double distance = toTarget.length();
		double speed = this.getVelocity().length();
		if (speed < 0.0001) return Double.POSITIVE_INFINITY;
		return distance / speed;
	}

	@Override
	protected void knockback(LivingEntity target, DamageSource source) {
		double baseKnockback = this.getWeaponStack() != null && this.getWorld() instanceof ServerWorld serverWorld
			? EnchantmentHelper.modifyKnockback(serverWorld, this.getWeaponStack(), target, source, 1.0f)
			: 0.0F;
		if (baseKnockback > 0.0) {
			double resistanceFactor = Math.max(0.0, 1.0 - target.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.KNOCKBACK_RESISTANCE));
			net.minecraft.util.math.Vec3d vec3d = this.getVelocity().multiply(baseKnockback * resistanceFactor * 0.1f);
			if (vec3d.lengthSquared() > 0.0) {
				target.addVelocity(vec3d.x, Math.max(0.1, vec3d.y + 0.1), vec3d.z);
			}
		}
	}

	static {
		LOYALTY = DataTracker.registerData(DaggerEntity.class, TrackedDataHandlerRegistry.BYTE);
		ENCHANTED = DataTracker.registerData(DaggerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	}
}