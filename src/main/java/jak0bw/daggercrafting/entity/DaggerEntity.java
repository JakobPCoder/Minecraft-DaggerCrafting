package jak0bw.daggercrafting.entity;

import jak0bw.daggercrafting.DaggerCrafting;
import jak0bw.daggercrafting.DaggerToolMaterial;
import jak0bw.daggercrafting.item.DaggerItem;
import jak0bw.daggercrafting.particle.ParticleCue;
import jak0bw.daggercrafting.particle.ParticleDirector;
import jak0bw.daggercrafting.particle.ParticleHitContext;

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
 * Server-authoritative thrown dagger: one {@link EntityType} per material tier, arrow physics underneath.
 *
 * Why {@link AbstractArrow}
 * Vanilla arrows already solve pickup rules, owner tracking, in-ground sticking, enchantment-as-weapon,
 * and projectile collision. Daggers reuse that pipeline and only override damage source, return flight, and visuals.
 *
 * Tier resolution
 * {@link jak0bw.daggercrafting.ModEntities} registers {@code daggercrafting:iron_dagger} entities with the same
 * id stem as items. Stack-backed constructors look up {@link EntityType} from the thrown item id so
 * {@link DaggerToolMaterial#getRangedDamage()} / {@link DaggerToolMaterial#getRangedVelocity()} match the tier
 * without storing material on the entity NBT beyond the pickup stack.
 *
 * Spawn paths
 *
 * - {@link jak0bw.daggercrafting.item.DaggerItem#releaseUsing} → {@code Projectile.spawnProjectileFromRotation}
 *   with {@code DaggerEntity::new} (player throw).
 * - {@link jak0bw.daggercrafting.item.DaggerItem#asProjectile} → position constructor (dispensers).
 * - Registry constructor for summoning / tests.
 *
 * Loyalty (trident parity, custom motion)
 * Loyalty level is read from the thrown stack via
 * {@link EnchantmentHelper#getTridentReturnToOwnerAcceleration} (allowed on daggers via
 * {@link jak0bw.daggercrafting.mixin.EnchantmentMixin}). Return starts after the dagger has
 * {@link #dealtDamage dealt damage} or sat in the ground ({@link AbstractArrow#inGroundTime}), mirroring trident
 * behavior. Homing uses {@link #applyLinearReturnMotion} (ease toward owner eyes) instead of vanilla trident curves.
 *
 * Client coupling
 * {@link ItemSupplier} + synced {@link #ID_FOIL} drive the item model; {@link #seconds} and yaw/pitch set in
 * {@link #tick()} feed {@link DaggerEntityRenderer} tumble animation. {@link #shouldRender} forces draw despite
 * short entity tracking range from {@link jak0bw.daggercrafting.ModEntities}.
 */
public class DaggerEntity extends AbstractArrow implements ItemSupplier {

	// --- Synced entity data (client sees loyalty level + enchant glint) ---

	/** Trident loyalty level (0–127); synced so clients know a return is imminent. */
	private static final EntityDataAccessor<Byte> ID_LOYALTY = SynchedEntityData.defineId(DaggerEntity.class, EntityDataSerializers.BYTE);

	/** Copy of {@link ItemStack#hasFoil()} at throw time; avoids re-resolving enchantments on the client. */
	private static final EntityDataAccessor<Boolean> ID_FOIL = SynchedEntityData.defineId(DaggerEntity.class, EntityDataSerializers.BOOLEAN);

	/**
	 * Hold-to-throw charge in {@code [0, 1]} at release (0 = minimum throw ticks, 1 = full charge).
	 * Synced so the client trail can lerp density and size without reading use ticks.
	 */
	private static final EntityDataAccessor<Float> ID_THROW_CHARGE = SynchedEntityData.defineId(DaggerEntity.class, EntityDataSerializers.FLOAT);

	// --- Public fields read by the client renderer (not worth extra sync keys) ---

	/** Unused in current logic; kept for debugging or external mods referencing item id strings. */
	public String itemId;

	/**
	 * Airtime in seconds ({@code tickCount / 20}), updated while airborne.
	 * {@link DaggerEntityRenderer} uses this for the ease-out spin keyframe.
	 */
	public float seconds;

	// --- Server-side loyalty / combat state ---

	/**
	 * Once true, entity collision is disabled ({@link #findHitEntity}) and loyalty return may begin.
	 * Set on first hit, and after {@link AbstractArrow#inGroundTime} exceeds a few ticks while stuck.
	 */
	private boolean dealtDamage;

	/**
	 * Ticks spent in loyalty homing after {@link #dealtDamage}; {@code 0} during outbound flight.
	 * Drives 180° model flip and return sound cadence in {@link #tick()}.
	 */
	public int returnTimer;

	/** Guards the louder “about to catch” sound so it only plays once per return trip. */
	private boolean approachingTargetSoundPlayed = false;

	/**
	 * When predicted time-to-owner drops below this (see {@link #predictTimeToTarget}), play approach sound.
	 * Value is in ticks under the constant-speed assumption used for audio only.
	 */
	private static final int APPROACH_SOUND_THRESHOLD_TICKS = 5;

	/**
	 * Scales {@link DaggerToolMaterial#getRangedVelocity()} into max return speed per loyalty level.
	 * Kept &lt; 1 so return feels controlled and does not teleport through the owner hitbox.
	 */
	private static final double RETURN_SPEED_MULTIPLIER = 0.15;

	/**
	 * EntityType factory entry used by {@link jak0bw.daggercrafting.ModEntities} registration.
	 * Does not set loyalty; stack-backed constructors are used for gameplay spawns.
	 */
	public DaggerEntity(EntityType<? extends DaggerEntity> type, Level level) {
		super(type, level);
	}

	/**
	 * Player or mob throw: entity type is resolved from the stack so tier-specific registration is honored.
	 *
	 * @param owner shooter; stored as arrow owner for damage attribution and pickup
	 * @param stack single dagger stack (count should be 1); becomes pickup origin and weapon for enchantments
	 */
	@SuppressWarnings("unchecked")
	public DaggerEntity(Level level, LivingEntity owner, ItemStack stack) {
		super(
			(EntityType<? extends AbstractArrow>) BuiltInRegistries.ENTITY_TYPE.getValue(getIdentifierFromStack(stack)),
			owner,
			level,
			stack,
			null
		);
		this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(stack));
		this.entityData.set(ID_FOIL, stack.hasFoil());
	}

	/**
	 * Fixed-position spawn (dispensers). Same type resolution as owner constructor.
	 */
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
		this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(stack));
		this.entityData.set(ID_FOIL, stack.hasFoil());
	}

	/**
	 * Item registry key doubles as entity registry key ({@link jak0bw.daggercrafting.ModEntities} convention).
	 */
	private static Identifier getIdentifierFromStack(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem());
	}

	/** {@link ItemSupplier}: flying item renderer displays this stack (model + components). */
	@Override
	public ItemStack getItem() {
		return this.getPickupItemStackOrigin();
	}

	/** Registers dagger-specific sync fields on top of arrow defaults (in-ground, shake, etc.). */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ID_LOYALTY, (byte) 0);
		builder.define(ID_FOIL, false);
		builder.define(ID_THROW_CHARGE, 1.0F);
	}

	/**
	 * Per-tick simulation: loyalty return, then vanilla arrow physics, then visual orientation
	 * and the client particle trail.
	 *
	 * Order matters: homing velocity is applied before {@code super.tick()} so arrow base movement
	 * integrates the returned {@link #getDeltaMovement()} this frame. {@link ParticleDirector}
	 * runs after that move so the trail samples the segment just traveled.
	 */
	@Override
	public void tick() {
		// Long stick in block without hitting an entity still enables loyalty (vanilla trident rule of thumb).
		if (this.inGroundTime > 4) {
			this.dealtDamage = true;
		}

		Entity entity = this.getOwner();
		int loyalty = this.entityData.get(ID_LOYALTY);
		if (loyalty > 0 && (this.dealtDamage || this.isNoPhysics()) && entity != null) {
			if (!this.isAcceptibleReturnOwner()) {
				// Owner gone or invalid: drop item entity if pickup allowed, otherwise vanish.
				if (this.level() instanceof ServerLevel serverLevel && this.pickup == Pickup.ALLOWED) {
					this.spawnAtLocation(serverLevel, this.getPickupItem(), 0.1F);
				}

				this.discard();
			} else {
				// Mobs are not given inventory pickup on return; despawn when close enough.
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
		ParticleDirector.tick(this);

		// While flying: orient entity rotation to velocity so renderer aligns blade with motion.
		if (!this.isInGround()) {
			Vec3 velocity = this.getDeltaMovement();
			if (velocity.lengthSqr() > 0.0001) {
				float yaw = (float) Math.toDegrees(Mth.atan2(velocity.x, velocity.z));
				float pitch = (float) Math.toDegrees(Mth.atan2(velocity.y, velocity.horizontalDistance()));
				if (this.returnTimer > 0) {
					// Return flight: flip so handle trails toward owner (reads as “reversed” throw).
					yaw += 180.0f;
					pitch = -pitch;
				}
				this.setYRot(yaw);
				this.setXRot(pitch);
				this.seconds = (float) this.tickCount / 20.0f;
			}
		}

		// Loyalty can exceed default sync thresholds; flag forces position packet to clients.
		if (!this.level().isClientSide()) {
			double velocitySquared = this.getDeltaMovement().lengthSqr();
			if (velocitySquared > 2.0) {
				this.hurtMarked = true;
			}
		}
	}

	/**
	 * Loyalty targets living owners only; spectators cannot catch returning daggers on the server.
	 */
	private boolean isAcceptibleReturnOwner() {
		Entity entity = this.getOwner();
		if (entity != null && entity.isAlive()) {
			return !(entity instanceof ServerPlayer) || !entity.isSpectator();
		} else {
			return false;
		}
	}

	/** Exposed for render layers that need glint without reading the full stack on client. */
	public boolean isFoil() {
		return this.entityData.get(ID_FOIL);
	}

	/**
	 * @param throwCharge hold-charge fraction in {@code [0, 1]} captured at spawn
	 */
	public void setThrowCharge(float throwCharge) {
		this.entityData.set(ID_THROW_CHARGE, Mth.clamp(throwCharge, 0.0F, 1.0F));
	}

	/**
	 * @return charge fraction used to lerp trail density and quad size; dispensers default to full ({@code 1})
	 */
	public float getThrowCharge() {
		return this.entityData.get(ID_THROW_CHARGE);
	}

	/**
	 * Widens {@link AbstractArrow#isInGround()} so {@link ParticleDirector} can stop the trail when stuck.
	 */
	@Override
	public boolean isInGround() {
		return super.isInGround();
	}

	/**
	 * After the first damaging hit, stop scanning for entities so loyalty return does not re-pierce targets.
	 */
	@Override
	protected @Nullable EntityHitResult findHitEntity(Vec3 currentPosition, Vec3 nextPosition) {
		return this.dealtDamage ? null : super.findHitEntity(currentPosition, nextPosition);
	}

	/**
	 * Tier stats from the pickup stack; falls back to diamond in {@link DaggerItem#getMaterialFromItem} if corrupt.
	 */
	public DaggerToolMaterial getMaterial() {
		Item itemRef = this.getPickupItemStackOrigin().getItem();
		return DaggerItem.getMaterialFromItem(itemRef);
	}

	/**
	 * Thrown hit: material ranged damage, trident damage type (for loyalty/enchant hooks), then slow for return.
	 *
	 * Using {@code damageSources().trident} ties into the same enchantment and immunity paths as tridents
	 * without spawning a trident entity.
	 */
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
			// Endermen teleport away; skip knockback/enchant follow-up that assumes a stable target.
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

		// Nearly stop forward motion so loyalty pull can take over on the next tick.
		this.deflect(ProjectileDeflection.REVERSE, targetEntity, this.owner, false);
		this.setDeltaMovement(this.getDeltaMovement().multiply(0.02, 0.2, 0.02));
		this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
	}

	/**
	 * Snapshots motion and contact before stick logic zeros velocity, then emits client terrain crumbs.
	 */
	@Override
	protected void onHitBlock(BlockHitResult blockHitResult) {
		Vec3 motion = this.getDeltaMovement();
		if (this.level().isClientSide()) {
			ParticleDirector.emit(
				ParticleCue.HIT_BLOCK,
				this,
				new ParticleHitContext(
					blockHitResult.getLocation(),
					motion,
					this.level().getBlockState(blockHitResult.getBlockPos()),
					blockHitResult.getDirection()
				)
			);
		}
		super.onHitBlock(blockHitResult);
	}

	/**
	 * Block impacts run enchantment callbacks (e.g. effects that need a living user and weapon stack).
	 */
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

	/** EnchantmentHelper and combat use the thrown stack as the weapon, not a separate item type. */
	@Override
	public ItemStack getWeaponItem() {
		return this.getPickupItemStackOrigin();
	}

	/**
	 * Pickup on ground via arrow logic, plus mid-air catch while no-physics homing intersects the owner.
	 */
	@Override
	protected boolean tryPickup(Player player) {
		return super.tryPickup(player)
			|| this.isNoPhysics() && this.ownedBy(player) && player.getInventory().add(this.getPickupItem());
	}

	/**
	 * Rebuilds a clean stack when the entity has no pickup origin (summon / data fix).
	 * Entity type path equals item id under {@link DaggerCrafting#MOD_ID}.
	 */
	@Override
	protected ItemStack getDefaultPickupItem() {
		String itemPath = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()).getPath();
		Identifier itemId = Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, itemPath);
		Item item = BuiltInRegistries.ITEM.getValue(itemId);
		if (item != null && item != Items.AIR) {
			return new ItemStack(item);
		} else {
			DaggerCrafting.LOGGER.warn("No pickup item for dagger entity type {}, falling back to diamond_dagger", itemPath);
			return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(DaggerCrafting.MOD_ID, "diamond_dagger")));
		}
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	/**
	 * Prevents other players from stealing a homing dagger by walking through its hitbox before the owner.
	 */
	@Override
	public void playerTouch(Player player) {
		if (this.ownedBy(player) || this.getOwner() == null) {
			super.playerTouch(player);
		}
	}

	/**
	 * Chunk reload: restore post-hit flag and recompute loyalty from saved stack (enchantments may have changed).
	 */
	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.dealtDamage = input.getBooleanOr("DealtDamage", false);
		this.entityData.set(ID_LOYALTY, this.getLoyaltyFromItem(this.getPickupItemStackOrigin()));
		this.setThrowCharge(input.getFloatOr("ThrowCharge", 1.0F));
	}

	/** Persist {@link #dealtDamage} and throw charge; loyalty is derived from stack on load. */
	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("DealtDamage", this.dealtDamage);
		output.putFloat("ThrowCharge", this.getThrowCharge());
	}

	/**
	 * Maps loyalty enchantment on the stack to a byte for sync and motion scaling.
	 * Client dimension returns 0 until entity data syncs from server.
	 */
	private byte getLoyaltyFromItem(ItemStack stack) {
		if (this.level() instanceof ServerLevel serverLevel) {
			return (byte) Mth.clamp(EnchantmentHelper.getTridentReturnToOwnerAcceleration(serverLevel, stack, this), 0, 127);
		} else {
			return 0;
		}
	}

	/**
	 * Loyalty daggers with normal pickup never despawn while waiting to return; others use arrow lifetime.
	 */
	@Override
	public void tickDespawn() {
		int loyalty = this.entityData.get(ID_LOYALTY);
		if (this.pickup != Pickup.ALLOWED || loyalty <= 0) {
			super.tickDespawn();
		}
	}

	/** Slightly less water drag than default arrows so throws remain readable underwater. */
	@Override
	protected float getWaterInertia() {
		return 0.99F;
	}

	/**
	 * {@link jak0bw.daggercrafting.ModEntities} uses tracking range 4; always render so short throws stay visible.
	 */
	@Override
	public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
		return true;
	}

	/**
	 * Homing step: accelerate toward owner eye position, capped by loyalty and material speed.
	 *
	 * Speed eases toward {@code maxspeed} each tick so return does not snap instantly after a hit.
	 * {@code maxspeed} is also clamped to remaining distance so the dagger does not overshoot in one tick.
	 */
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

	/**
	 * Rough ticks until intercept at current speed; used only for approach sound timing, not physics.
	 */
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

	/**
	 * Softer than default arrow knockback: direction from flight velocity, magnitude from enchantments × 0.15.
	 */
	@Override
	protected void doKnockback(LivingEntity target, DamageSource source) {
		double baseKnockback = this.getWeaponItem() != null && this.level() instanceof ServerLevel serverWorld
			? EnchantmentHelper.modifyKnockback(serverWorld, this.getWeaponItem(), target, source, 0.5f)
			: 0.0F;
		if (baseKnockback > 0.0) {
			double resistanceFactor = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
			Vec3 velocity = this.getDeltaMovement();
			// Squaring components keeps push aligned with throw direction while damping weak axes.
			velocity = new Vec3(velocity.x * Math.abs(velocity.x), velocity.y * Math.abs(velocity.y), velocity.z * Math.abs(velocity.z));
			velocity = velocity.scale(baseKnockback * resistanceFactor * 0.15f);

			if (velocity.lengthSqr() > 0.0) {
				target.push(velocity.x, Math.max(0.05, velocity.y), velocity.z);
			}
		}
	}
}
