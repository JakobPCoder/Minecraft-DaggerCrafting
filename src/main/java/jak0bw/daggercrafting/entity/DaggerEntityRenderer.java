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
import net.minecraft.item.ModelTransformationMode;
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

            org.joml.Quaternionf quaternion2 = new org.joml.Quaternionf().rotationXYZ(
                (float)Math.toRadians(135),
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

    
        ItemStack stack = entity.getStack();

        if (stack == null || stack.isEmpty()) 
            throw new IllegalStateException("DaggerEntityRenderer: Stack is null or empty");
    
        this.itemModelManager.updateForNonLivingEntity(
            flyingItemEntityRenderState.itemRenderState, 
            stack, // Using the null-safe stack
            ModelTransformationMode.GROUND,
            entity
        );
    }

    @Override
    public FlyingItemEntityRenderState createRenderState() {
        return new DaggerEntityRenderState();
     }
} 