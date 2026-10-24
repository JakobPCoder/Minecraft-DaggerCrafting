package jak0bw.daggercrafting.entity;

import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;

/**
 * Client render snapshot for a flying {@link DaggerEntity}, extending vanilla thrown-item state.
 *
 * {@link ThrownItemRenderState} already carries the resolved item model and lighting; these fields
 * mirror server-side orientation and airtime so {@link DaggerEntityRenderer} can aim the stack along
 * velocity and run a short ease-out tumble instead of billboarding like a snowball.
 */
public class DaggerEntityRenderState extends ThrownItemRenderState {

	/** Entity yaw copied in {@link DaggerEntityRenderer#extractRenderState}; flipped while returning. */
	public float yaw;

	/** Entity pitch copied in extract; inverted during loyalty return for a “blade leading” look. */
	public float pitch;

	/** Seconds since spawn ({@link DaggerEntity#seconds}), drives the spin keyframe in the renderer. */
	public float seconds;

	/** Reserved for future hit-stop or impact animation; not read yet. */
	public boolean hasHit;

	/** Zeroes orientation fields until the first extract from a live entity. */
	public DaggerEntityRenderState() {
		super();
		this.yaw = 0;
		this.pitch = 0;
		this.seconds = 0;
	}
}
