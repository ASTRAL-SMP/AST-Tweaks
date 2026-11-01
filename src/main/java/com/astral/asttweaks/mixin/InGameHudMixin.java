package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.scoreboard.ScoreboardFeature;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept scoreboard rendering.
 */
@Mixin(Gui.class)
public class InGameHudMixin {
    @Inject(
            method = "displayScoreboardSidebar",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderScoreboard(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        ScoreboardFeature feature = FeatureManager.getInstance().getScoreboardFeature();
        if (feature != null && feature.isEnabled()) {
            boolean handled = feature.render(graphics, objective);
            if (handled) {
                ci.cancel();
            }
        }
    }
}
