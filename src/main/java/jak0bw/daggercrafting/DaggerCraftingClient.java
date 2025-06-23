package jak0bw.daggercrafting;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import jak0bw.daggercrafting.entity.DaggerEntityRenderer;
import jak0bw.daggercrafting.entity.DaggerEntity;
import jak0bw.daggercrafting.ModEntities;

public class DaggerCraftingClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DaggerCraftingClient");

    @Override
    public void onInitializeClient() {
        LOGGER.info("DaggerCrafting client initialized");
        EntityRendererRegistry.register(ModEntities.DAGGER, DaggerEntityRenderer::new);
    }
} 