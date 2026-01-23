package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.scoreboard.ScoreboardFeature;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept scoreboard rendering.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(
            method = "renderScoreboardSidebar",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderScoreboard(MatrixStack matrices, ScoreboardObjective objective, CallbackInfo ci) {
        ScoreboardFeature feature = FeatureManager.getInstance().getScoreboardFeature();
        if (feature != null && feature.isEnabled()) {
            boolean handled = feature.render(matrices, objective);
            if (handled) {
                ci.cancel();
            }
        }
    }
}
