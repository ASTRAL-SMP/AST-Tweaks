package com.astral.asttweaks.feature.lavahighlight;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tints lava source / flowing blocks. Used by both the vanilla fluid renderer and Sodium,
 * which reads the tint source of the registered fluid model.
 */
public class LavaTintSource implements BlockTintSource {
    private final LavaHighlightConfig config;

    public LavaTintSource(LavaHighlightConfig config) {
        this.config = config;
    }

    @Override
    public int color(BlockState state) {
        // Return white (no tint) if feature is disabled
        if (!config.isEnabled()) {
            return 0xFFFFFFFF;
        }

        boolean isSource = state.getFluidState().isSource();

        if (isSource && config.isHighlightSource()) {
            return config.getSourceColor() | 0xFF000000; // Force opaque alpha
        } else if (!isSource && config.isHighlightFlowing()) {
            return config.getFlowingColor() | 0xFF000000;
        }

        // Default: white (no tint)
        return 0xFFFFFFFF;
    }
}
