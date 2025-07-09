package jak0bw.daggercrafting;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import jak0bw.daggercrafting.entity.DaggerEntityRenderer;

public class DaggerCraftingClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DaggerCraftingClient");

    @Override
    public void onInitializeClient() {
        LOGGER.info("DaggerCrafting client initialized");
        for (String name : DaggerToolMaterial.DAGGER_TOOL_MATERIALS.keySet()) {
            EntityRendererRegistry.register(ModEntities.DAGGER_ENTITY_TYPES.get(name), DaggerEntityRenderer::new);
        }
    }
} 