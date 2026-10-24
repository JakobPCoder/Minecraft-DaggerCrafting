package jak0bw.daggercrafting.entity;

import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;

/**
 * Extra thrown-item render fields used to spin the dagger in flight.
 */
public class DaggerEntityRenderState extends ThrownItemRenderState {
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
