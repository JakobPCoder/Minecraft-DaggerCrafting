package jak0bw.daggercrafting;

import jak0bw.daggercrafting.entity.DaggerEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entry point: binds renderers for throwable dagger entities.
 *
 * <p>Runs after common init. Each material tier registered in {@link ModEntities} gets the same
 * {@link DaggerEntityRenderer} factory; the renderer distinguishes visuals from the entity's item stack.
 */
public class DaggerCraftingClient implements ClientModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(DaggerCrafting.MOD_ID + "-client");

	/**
	 * Associates every {@link ModEntities#DAGGER_ENTITY_TYPES} entry with {@link DaggerEntityRenderer}.
	 *
	 * <p>The loop mirrors {@link DaggerToolMaterial#DAGGER_TOOL_MATERIALS} keys so a new tier only
	 * needs material + item + entity registration—no extra client switch statement.
	 */
	@Override
	public void onInitializeClient() {
		LOGGER.info("Registering dagger entity renderers");
		for (String name : DaggerToolMaterial.DAGGER_TOOL_MATERIALS.keySet()) {
			EntityRenderers.register(ModEntities.DAGGER_ENTITY_TYPES.get(name), DaggerEntityRenderer::new);
		}

		LOGGER.info("DaggerCrafting client initialized");
	}
}
