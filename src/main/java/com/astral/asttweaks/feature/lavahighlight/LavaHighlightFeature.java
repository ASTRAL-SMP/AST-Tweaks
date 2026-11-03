package com.astral.asttweaks.feature.lavahighlight;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
//?} else {
/*import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
*///?}
import net.minecraft.world.level.material.Fluids;

/**
 * Feature to highlight lava source blocks and flowing lava.
 * Registers a lava fluid model with a tint source (works with vanilla and Sodium).
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
        //? if >=26.1 {
        // Same textures as vanilla's lava model; only the tint source is added
        FluidModel.Unbaked lavaModel = new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/lava_still")),
                new Material(Identifier.withDefaultNamespace("block/lava_flow")),
                null,
                new LavaTintSource(config));
        FluidRenderingRegistry.register(Fluids.LAVA, Fluids.FLOWING_LAVA, lavaModel);
        //?} else {
        /*// Register handler after client starts (when textures are available)
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            LavaFluidRenderHandler customHandler = new LavaFluidRenderHandler(config);
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, customHandler);
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, customHandler);
        });
        *///?}

        ASTTweaks.LOGGER.info("LavaHighlight feature initialized");
    }

    @Override
    public void tick() {
        // No tick needed - rendering is handled by the fluid model's tint source
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
