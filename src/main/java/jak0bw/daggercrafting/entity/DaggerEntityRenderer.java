package jak0bw.daggercrafting.entity;

import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;

/**
 * Client renderer for {@link DaggerEntity}: draws the real item model in {@link ItemDisplayContext#GROUND}
 * with flight-aligned rotation instead of vanilla thrown-item billboarding.
 */
@Environment(EnvType.CLIENT)
public class DaggerEntityRenderer extends FlyingItemEntityRenderer<DaggerEntity> {
	private final ItemModelManager itemModelManager;

	public DaggerEntityRenderer(EntityRendererFactory.Context context) {
		super(context);
		this.itemModelManager = context.getItemModelManager();
	}

	/**
	 * Keyframed spin helper: ease-out from {@code startAngle} to {@code endAngle} over {@code duration} seconds.
	 */
	private float getAnimatedAngle(float seconds, float startAngle, float endAngle, float duration) {
		if (seconds <= 0) {
			return startAngle;
		}
		if (seconds >= duration) {
			return endAngle;
		}
		float t = seconds / duration;
		float easeOutT = 1f - (1f - t) * (1f - t);
		return startAngle + (endAngle - startAngle) * easeOutT;
	}

	/**
	 * Builds the world-space pose for the dagger model. Does not call {@code super.render} so the stack
	 * is not drawn twice with camera billboarding.
	 */
	@Override
	public void render(
		FlyingItemEntityRenderState flyingItemEntityRenderState,
		MatrixStack matrixStack,
		VertexConsumerProvider vertexConsumerProvider,
		int light
	) {
		matrixStack.push();
		matrixStack.scale(1.0F, 1.0F, 1.0F);
		if (flyingItemEntityRenderState instanceof DaggerEntityRenderState daggerState) {
			org.joml.Quaternionf quaternion = new org.joml.Quaternionf().rotationYXZ(
				(float) Math.toRadians(daggerState.yaw),
				(float) Math.toRadians(-daggerState.pitch),
				0.0f
			);
			matrixStack.multiply(quaternion);

			org.joml.Quaternionf quaternion2 = new org.joml.Quaternionf().rotationXYZ(
				(float) Math.toRadians(this.getAnimatedAngle(daggerState.seconds, 70, 135, 0.3f)),
				(float) Math.toRadians(90),
				0.0f
			);
			matrixStack.multiply(quaternion2);
		}
		flyingItemEntityRenderState.itemRenderState.render(matrixStack, vertexConsumerProvider, light, OverlayTexture.DEFAULT_UV);
		matrixStack.pop();
	}

	@Override
	public void updateRenderState(DaggerEntity entity, FlyingItemEntityRenderState flyingItemEntityRenderState, float tickDelta) {
		super.updateRenderState(entity, flyingItemEntityRenderState, tickDelta);

		if (!(flyingItemEntityRenderState instanceof DaggerEntityRenderState daggerEntityRenderState)) {
			throw new IllegalStateException("DaggerEntityRenderer: FlyingItemEntityRenderState is not a DaggerEntityRenderState");
		}

		daggerEntityRenderState.yaw = entity.getYaw();
		daggerEntityRenderState.pitch = entity.getPitch();
		daggerEntityRenderState.seconds = entity.seconds;

		ItemStack stack = entity.getItemStack();
		if (stack == null || stack.isEmpty()) {
			throw new IllegalStateException("DaggerEntityRenderer: Stack is null or empty");
		}

		this.itemModelManager.updateForNonLivingEntity(
			flyingItemEntityRenderState.itemRenderState,
			stack,
			ItemDisplayContext.GROUND,
			entity
		);
	}

	@Override
	public FlyingItemEntityRenderState createRenderState() {
		return new DaggerEntityRenderState();
	}
}
