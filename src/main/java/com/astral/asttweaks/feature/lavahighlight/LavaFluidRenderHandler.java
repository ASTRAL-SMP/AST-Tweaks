//? if <26.1 {
/*package com.astral.asttweaks.feature.lavahighlight;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/^*
 * Custom FluidRenderHandler for lava that applies highlight colors.
 * Uses Fabric API for stable, crash-free fluid rendering customization.
 ^/
public class LavaFluidRenderHandler implements FluidRenderHandler {
    private static final Identifier LAVA_STILL = new Identifier("minecraft", "block/lava_still");
    private static final Identifier LAVA_FLOW = new Identifier("minecraft", "block/lava_flow");

    private final LavaHighlightConfig config;
    private TextureAtlasSprite[] sprites;

    public LavaFluidRenderHandler(LavaHighlightConfig config) {
        this.config = config;
    }

    private void loadSprites() {
        TextureAtlas atlas = Minecraft.getInstance()
            .getModelManager()
            .getAtlas(InventoryMenu.BLOCK_ATLAS);
        sprites = new TextureAtlasSprite[] {
            atlas.getSprite(LAVA_STILL),
            atlas.getSprite(LAVA_FLOW)
        };
    }

    @Override
    public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view,
                                     @Nullable BlockPos pos,
                                     FluidState state) {
        if (sprites == null) {
            loadSprites();
        }
        return sprites;
    }

    @Override
    public int getFluidColor(@Nullable BlockAndTintGetter view,
                             @Nullable BlockPos pos,
                             FluidState state) {
        // Return white (no tint) if feature is disabled
        if (!config.isEnabled()) {
            return 0xFFFFFFFF;
        }

        boolean isSource = state.isSource();

        if (isSource && config.isHighlightSource()) {
            return config.getSourceColor() | 0xFF000000; // Force opaque alpha
        } else if (!isSource && config.isHighlightFlowing()) {
            return config.getFlowingColor() | 0xFF000000;
        }

        // Default: white (no tint)
        return 0xFFFFFFFF;
    }
}
*///?}
