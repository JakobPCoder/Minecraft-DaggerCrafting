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
        EntityRendererRegistry.register(ModEntities.WOODEN_DAGGER, DaggerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.STONE_DAGGER, DaggerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.GOLDEN_DAGGER, DaggerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.IRON_DAGGER, DaggerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.DIAMOND_DAGGER, DaggerEntityRenderer::new);
    }
} 