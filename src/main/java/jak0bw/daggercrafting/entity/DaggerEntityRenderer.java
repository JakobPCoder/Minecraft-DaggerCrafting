package jak0bw.daggercrafting.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Submits a thrown dagger as a GROUND item model, rotated by yaw/pitch and a short spin.
 */
@Environment(EnvType.CLIENT)
public class DaggerEntityRenderer extends ThrownItemRenderer<DaggerEntity> {
	private final ItemModelResolver itemModelResolver;

	public DaggerEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	/**
	 * Animates from startAngle to endAngle over duration seconds using ease-out.
	 * After duration, stays at endAngle.
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

	@Override
	public void submit(
		ThrownItemRenderState flyingItemEntityRenderState,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		CameraRenderState camera
	) {
		poseStack.pushPose();
		poseStack.scale(1.0F, 1.0F, 1.0F);
		if (flyingItemEntityRenderState instanceof DaggerEntityRenderState daggerState) {
			org.joml.Quaternionf quaternion = new org.joml.Quaternionf().rotationYXZ(
				(float) Math.toRadians(daggerState.yaw),
				(float) Math.toRadians(-daggerState.pitch),
				0.0f
			);
			poseStack.mulPose(quaternion);

			org.joml.Quaternionf quaternion2 = new org.joml.Quaternionf().rotationXYZ(
				(float) Math.toRadians(this.getAnimatedAngle(daggerState.seconds, 70, 135, 0.3f)),
				(float) Math.toRadians(90),
				0.0f
			);
			poseStack.mulPose(quaternion2);
		}
		flyingItemEntityRenderState.item.submit(
			poseStack,
			submitNodeCollector,
			flyingItemEntityRenderState.lightCoords,
			OverlayTexture.NO_OVERLAY,
			flyingItemEntityRenderState.outlineColor
		);
		poseStack.popPose();
		// ThrownItemRenderer.submit also draws the stack after mulPose(camera.orientation),
		// which is the camera-facing copy. 1.21.5 never called super.render().
	}

	@Override
	public void extractRenderState(DaggerEntity entity, ThrownItemRenderState flyingItemEntityRenderState, float f) {
		super.extractRenderState(entity, flyingItemEntityRenderState, f);

		if (!(flyingItemEntityRenderState instanceof DaggerEntityRenderState)) {
			throw new IllegalStateException("DaggerEntityRenderer: ThrownItemRenderState is not a DaggerEntityRenderState");
		}

		DaggerEntityRenderState daggerEntityRenderState = (DaggerEntityRenderState) flyingItemEntityRenderState;
		daggerEntityRenderState.yaw = entity.getYRot();
		daggerEntityRenderState.pitch = entity.getXRot();
		daggerEntityRenderState.seconds = entity.seconds;

		ItemStack stack = entity.getPickupItemStackOrigin();

		if (stack == null || stack.isEmpty()) {
			throw new IllegalStateException("DaggerEntityRenderer: Stack is null or empty");
		}

		this.itemModelResolver.updateForNonLiving(
			flyingItemEntityRenderState.item,
			stack,
			ItemDisplayContext.GROUND,
			entity
		);
	}

	@Override
	public ThrownItemRenderState createRenderState() {
		return new DaggerEntityRenderState();
	}
}
