package com.astral.asttweaks.feature.lavahighlight;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.fluid.FluidState;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

/**
 * Custom FluidRenderHandler for lava that applies highlight colors.
 * Uses Fabric API for stable, crash-free fluid rendering customization.
 */
public class LavaFluidRenderHandler implements FluidRenderHandler {
    private static final Identifier LAVA_STILL = new Identifier("minecraft", "block/lava_still");
    private static final Identifier LAVA_FLOW = new Identifier("minecraft", "block/lava_flow");

    private final LavaHighlightConfig config;
    private Sprite[] sprites;

    public LavaFluidRenderHandler(LavaHighlightConfig config) {
        this.config = config;
    }

    private void loadSprites() {
        SpriteAtlasTexture atlas = MinecraftClient.getInstance()
            .getBakedModelManager()
            .getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        sprites = new Sprite[] {
            atlas.getSprite(LAVA_STILL),
            atlas.getSprite(LAVA_FLOW)
        };
    }

    @Override
    public Sprite[] getFluidSprites(@Nullable BlockRenderView view,
                                     @Nullable BlockPos pos,
                                     FluidState state) {
        if (sprites == null) {
            loadSprites();
        }
        return sprites;
    }

    @Override
    public int getFluidColor(@Nullable BlockRenderView view,
                             @Nullable BlockPos pos,
                             FluidState state) {
        // Return white (no tint) if feature is disabled
        if (!config.isEnabled()) {
            return 0xFFFFFFFF;
        }

        boolean isSource = state.isStill();

        if (isSource && config.isHighlightSource()) {
            return config.getSourceColor() | 0xFF000000; // Force opaque alpha
        } else if (!isSource && config.isHighlightFlowing()) {
            return config.getFlowingColor() | 0xFF000000;
        }

        // Default: white (no tint)
        return 0xFFFFFFFF;
    }
}
