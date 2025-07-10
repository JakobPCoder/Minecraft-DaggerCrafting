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
import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import jak0bw.daggercrafting.ModItems;

@Environment(EnvType.CLIENT)
public class DaggerEntityRenderer extends FlyingItemEntityRenderer<DaggerEntity> {
    private final ItemModelManager itemModelManager;

    public DaggerEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemModelManager = context.getItemModelManager();
    }

    /**
     * Animates from startAngle to endAngle over duration seconds using ease-in/ease-out.
     * After duration, stays at endAngle.
     */
    private float getAnimatedAngle(float seconds, float startAngle, float endAngle, float duration) {
        if (seconds <= 0) return startAngle;
        if (seconds >= duration) return endAngle;
        float t = seconds / duration;
        // Quadratic ease-out
        float easeOutT = 1f - (1f - t) * (1f - t);
        return startAngle + (endAngle - startAngle) * easeOutT;
    }

    @Override
    public void render(FlyingItemEntityRenderState flyingItemEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        matrixStack.scale(1.0F, 1.0F, 1.0F);
        if (flyingItemEntityRenderState instanceof DaggerEntityRenderState daggerState) {
            // Construct quaternion from yaw, pitch, roll=0
            org.joml.Quaternionf quaternion = new org.joml.Quaternionf().rotationYXZ(
                (float)Math.toRadians(daggerState.yaw),
                (float)Math.toRadians(-daggerState.pitch),
                0.0f // Roll (Z axis)
            );
            matrixStack.multiply(quaternion);

            // Example: animate from 135deg to 405deg (135+270) over 1.5 seconds
            org.joml.Quaternionf quaternion2 = new org.joml.Quaternionf().rotationXYZ(
                (float)Math.toRadians(this.getAnimatedAngle(daggerState.seconds,70, 135, 0.3f)),
                (float)Math.toRadians(90),
                0.0f
            );
            matrixStack.multiply(quaternion2);

        } 
        flyingItemEntityRenderState.itemRenderState.render(matrixStack, vertexConsumerProvider, i, OverlayTexture.DEFAULT_UV);
        matrixStack.pop();
    }

     
    @Override
    public void updateRenderState(DaggerEntity entity, FlyingItemEntityRenderState flyingItemEntityRenderState, float f) {
        super.updateRenderState(entity, flyingItemEntityRenderState, f);

        if (!(flyingItemEntityRenderState instanceof DaggerEntityRenderState)) {
            throw new IllegalStateException("DaggerEntityRenderer: FlyingItemEntityRenderState is not a DaggerEntityRenderState");
        }

        DaggerEntityRenderState daggerEntityRenderState = (DaggerEntityRenderState) flyingItemEntityRenderState;
        daggerEntityRenderState.yaw = entity.getYaw();
        daggerEntityRenderState.pitch = entity.getPitch();
        daggerEntityRenderState.seconds = entity.seconds;

        ItemStack stack = entity.getItemStack();

        if (stack == null || stack.isEmpty()) 
            throw new IllegalStateException("DaggerEntityRenderer: Stack is null or empty");
      


            
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