package jak0bw.daggercrafting;

import jak0bw.daggercrafting.entity.DaggerEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaggerCraftingClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("DaggerCraftingClient");

	@Override
	public void onInitializeClient() {
		LOGGER.info("DaggerCrafting: Registering DaggerEntityRenderers");
		for (String name : DaggerToolMaterial.DAGGER_TOOL_MATERIALS.keySet()) {
			EntityRendererRegistry.register(ModEntities.DAGGER_ENTITY_TYPES.get(name), DaggerEntityRenderer::new);
		}

		LOGGER.info("DaggerCrafting client initialized");
	}
}
