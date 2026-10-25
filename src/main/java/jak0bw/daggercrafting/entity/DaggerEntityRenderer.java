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
 * Client renderer for {@link DaggerEntity}: draws the real item model in {@link ItemDisplayContext#GROUND}
 * with flight-aligned rotation instead of vanilla thrown-item billboarding.
 *
 * Uses the 1.21+ submit pipeline ({@link #submit}) and a custom {@link DaggerEntityRenderState} so
 * orientation and airtime from the entity drive a short ease-out tumble on top of velocity yaw/pitch.
 */
@Environment(EnvType.CLIENT)
public class DaggerEntityRenderer extends ThrownItemRenderer<DaggerEntity> {

	/** Resolves item models/components for the stack carried by the entity each frame. */
	private final ItemModelResolver itemModelResolver;

	/** Wired from mod client init alongside other entity renderers. */
	public DaggerEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemModelResolver = context.getItemModelResolver();
	}

	/**
	 * Keyframed spin helper: ease-out from {@code startAngle} to {@code endAngle} over {@code duration} seconds.
	 *
	 * After {@code duration}, holds {@code endAngle} so the dagger settles into a stable in-flight pose.
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
	 * Builds the world-space pose for the dagger model and enqueues it for the render graph.
	 *
	 * Does not call {@code super.submit}: parent would add a camera-facing duplicate after
	 * {@code rotate(camera.orientation)}, which 1.21.5+ behavior and pre-submit custom daggers avoided.
	 */
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
			// First rotation: match entity velocity / return orientation from the server.
			org.joml.Quaternionf quaternion = new org.joml.Quaternionf().rotationYXZ(
				(float) Math.toRadians(daggerState.yaw),
				(float) Math.toRadians(-daggerState.pitch),
				0.0f
			);
			poseStack.rotate(quaternion);

			// Second rotation: cosmetic tumble (70°→135° over 0.3s) plus 90° so the blade reads in GROUND context.
			org.joml.Quaternionf quaternion2 = new org.joml.Quaternionf().rotationXYZ(
				(float) Math.toRadians(this.getAnimatedAngle(daggerState.seconds, 70, 135, 0.3f)),
				(float) Math.toRadians(90),
				0.0f
			);
			poseStack.rotate(quaternion2);
		}
		flyingItemEntityRenderState.item.submit(
			poseStack,
			submitNodeCollector,
			flyingItemEntityRenderState.lightCoords,
			OverlayTexture.NO_OVERLAY,
			flyingItemEntityRenderState.outlineColor
		);
		poseStack.popPose();
		// ThrownItemRenderer.submit also draws the stack after rotate(camera.orientation),
		// which is the camera-facing copy. 1.21.5 never called super.render().
	}

	/**
	 * Copies sim state into {@link DaggerEntityRenderState} and refreshes the item model slot for this frame.
	 */
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

		// GROUND display matches thrown pose; non-living resolver skips hand/arm poses.
		this.itemModelResolver.updateForNonLiving(
			flyingItemEntityRenderState.item,
			stack,
			ItemDisplayContext.GROUND,
			entity
		);
	}

	/** Ensures extract/submit always see {@link DaggerEntityRenderState}, not plain {@link ThrownItemRenderState}. */
	@Override
	public ThrownItemRenderState createRenderState() {
		return new DaggerEntityRenderState();
	}
}
