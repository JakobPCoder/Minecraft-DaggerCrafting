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

public class DaggerEntity extends PersistentProjectileEntity implements FlyingItemEntity{

    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    public String itemId;
    public float seconds;
	/**
	 * Damping factor for dagger motion.
	 */
	private static final float field_55019 = 0.99F;

	/**
	 * Whether the dagger has dealt damage.
	 */
	private boolean dealtDamage;

	/**
	 * Timer for dagger returning.
	 */
	public int returnTimer;

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
		super(ModEntities.DIAMOND_DAGGER, x, y, z, world, stack, (ItemStack)null);
        System.out.println("DaggerEntity Constructor 3 stack: " + stack);
		this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
		this.dataTracker.set(ENCHANTED, stack.hasGlint());
        System.out.println("DaggerEntity Constructor 3");
	}


	public DaggerEntity(World world, LivingEntity owner, ItemStack stack, EntityType<? extends DaggerEntity> daggerType) {
		super(daggerType, owner, world, stack, (ItemStack)null);
        System.out.println("DaggerEntity Constructor 4 stack: " + stack);
		this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
		this.dataTracker.set(ENCHANTED, stack.hasGlint());
        System.out.println("DaggerEntity Constructor 4");
	}

	public DaggerEntity(World world, double x, double y, double z, ItemStack stack, EntityType<? extends DaggerEntity> daggerType) {
		super(daggerType, x, y, z, world, stack, (ItemStack)null);
        System.out.println("DaggerEntity Constructor 5 stack: " + stack);
		this.dataTracker.set(LOYALTY, this.getLoyalty(stack));
		this.dataTracker.set(ENCHANTED, stack.hasGlint());
        System.out.println("DaggerEntity Constructor 5");
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
				Vec3d vec3d = entity.getEyePos().subtract(this.getPos());
				this.setPos(this.getX(), this.getY() + vec3d.y * 0.015 * (double) i, this.getZ());
				double d = 0.05 * (double) i;
				this.setVelocity(this.getVelocity().multiply(0.95).add(vec3d.normalize().multiply(d)));
				if (this.returnTimer == 0) {
					this.playSound(SoundEvents.ITEM_TRIDENT_RETURN, 10.0F, 1.0F);
				}

				++this.returnTimer;
			}
		}

		super.tick();

        // Update orientation based on velocity if not in ground
        if (!this.isInGround()) {
            Vec3d velocity = this.getVelocity();
            if (velocity.lengthSquared() > 0.0001) {
                this.setYaw((float)Math.toDegrees(MathHelper.atan2(velocity.x, velocity.z)));
                this.setPitch((float)Math.toDegrees(MathHelper.atan2(velocity.y, velocity.horizontalLength())));
                this.seconds = (float)this.age / 20.0f;
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

	protected void onEntityHit(EntityHitResult entityHitResult) {
		Entity entity = entityHitResult.getEntity();
		float f = 8.0F;
		Entity entity2 = this.getOwner();
		DamageSource damageSource = this.getDamageSources().trident(this, (Entity) (entity2 == null ? this : entity2));
		World var7 = this.getWorld();
		if (var7 instanceof ServerWorld serverWorld) {
			f = EnchantmentHelper.getDamage(serverWorld, this.getWeaponStack(), entity, damageSource, f);
		}

		this.dealtDamage = true;
		if (entity.sidedDamage(damageSource, f)) {
			if (entity.getType() == EntityType.ENDERMAN) {
				return;
			}

			var7 = this.getWorld();
			if (var7 instanceof ServerWorld) {
				ServerWorld serverWorld = (ServerWorld) var7;
				EnchantmentHelper.onTargetDamaged(serverWorld, entity, damageSource, this.getWeaponStack(), (item) -> {
					this.kill(serverWorld);
				});
			}

			if (entity instanceof LivingEntity) {
				LivingEntity livingEntity = (LivingEntity) entity;
				this.knockback(livingEntity, damageSource);
				this.onHit(livingEntity);
			}
		}

		this.deflect(ProjectileDeflection.SIMPLE, entity, this.getOwner(), false);
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

	static {
		LOYALTY = DataTracker.registerData(DaggerEntity.class, TrackedDataHandlerRegistry.BYTE);
		ENCHANTED = DataTracker.registerData(DaggerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	}
}