package com.astral.asttweaks.feature.lavahighlight;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.fluid.Fluids;

/**
 * Feature to highlight lava source blocks and flowing lava.
 * Uses Fabric API's FluidRenderHandler for stable fluid color customization.
 */
public class LavaHighlightFeature implements Feature {
    private final LavaHighlightConfig config;

    public LavaHighlightFeature() {
        this.config = new LavaHighlightConfig();
    }

    @Override
    public String getId() {
        return "lavahighlight";
    }

    @Override
    public String getName() {
        return "Lava Highlight";
    }

    @Override
    public void init() {
        // Register handler after client starts (when textures are available)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            registerFluidHandler();
        });
        ASTTweaks.LOGGER.info("LavaHighlight feature initialized");
    }

    private void registerFluidHandler() {
        LavaFluidRenderHandler customHandler = new LavaFluidRenderHandler(config);

        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, customHandler);
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, customHandler);

        ASTTweaks.LOGGER.info("Registered custom lava render handler");
    }

    @Override
    public void tick() {
        // No tick needed - rendering is handled by FluidRenderHandler
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public LavaHighlightConfig getConfig() {
        return config;
    }
}
