package jak0bw.daggercrafting.entity;

import net.minecraft.client.render.entity.state.FlyingItemEntityRenderState;

public class DaggerEntityRenderState extends FlyingItemEntityRenderState {
    public float yaw;
    public float pitch;
    public float seconds;
    public boolean hasHit;

    public DaggerEntityRenderState() {
        super();
        this.yaw = 0;
        this.pitch = 0;
        this.seconds = 0;
    }
}
