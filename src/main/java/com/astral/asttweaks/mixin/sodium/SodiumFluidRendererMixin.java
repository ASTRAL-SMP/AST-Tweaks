package com.astral.asttweaks.mixin.sodium;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.lavahighlight.LavaHighlightConfig;
import com.astral.asttweaks.feature.lavahighlight.LavaHighlightFeature;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Sodium's FluidRenderer to apply lava highlight colors.
 */
@Mixin(value = FluidRenderer.class, remap = false)
public abstract class SodiumFluidRendererMixin {

    @Shadow
    @Final
    private int[] quadColors;

    @Inject(
        method = "updateQuad",
        at = @At("TAIL")
    )
    private void onUpdateQuad(ModelQuadView quad, BlockRenderView world, BlockPos pos,
                              LightPipeline lighter, Direction dir, float brightness,
                              ColorSampler<FluidState> colorSampler, FluidState fluidState,
                              CallbackInfo ci) {
        // Check if this is lava
        if (!fluidState.isOf(Fluids.LAVA) && !fluidState.isOf(Fluids.FLOWING_LAVA)) {
            return;
        }

        LavaHighlightFeature feature = FeatureManager.getInstance().getLavaHighlightFeature();
        if (feature == null || !feature.isEnabled()) {
            return;
        }

        LavaHighlightConfig config = feature.getConfig();
        boolean isSource = fluidState.isStill();

        int color = -1;
        if (isSource && config.isHighlightSource()) {
            color = config.getSourceColor();
        } else if (!isSource && config.isHighlightFlowing()) {
            color = config.getFlowingColor();
        }

        if (color != -1) {
            // Apply custom color to all 4 vertices
            // Color format: ARGB (we need to keep brightness applied)
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            for (int i = 0; i < 4; i++) {
                // Extract current brightness from existing color
                int existing = this.quadColors[i];
                float existingBrightness = ((existing >> 16) & 0xFF) / 255.0f;

                // Apply brightness to our color
                int finalR = (int)(r * existingBrightness);
                int finalG = (int)(g * existingBrightness);
                int finalB = (int)(b * existingBrightness);

                this.quadColors[i] = 0xFF000000 | (finalR << 16) | (finalG << 8) | finalB;
            }
        }
    }
}
