package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.scoreboard.ScoreboardFeature;
//? if >=26.2 {
/*import net.minecraft.client.gui.Hud;
*///?} else {
import net.minecraft.client.gui.Gui;
//?}
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else if >=1.20 {
/*import net.minecraft.client.gui.GuiGraphics;
*///?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?}
import com.astral.asttweaks.util.gui.Gfx;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to intercept scoreboard rendering.
 */
// 26.2 でサイドバー描画が Gui から Hud に移った
//? if >=26.2 {
/*@Mixin(Hud.class)
*///?} else {
@Mixin(Gui.class)
//?}
public class InGameHudMixin {
    @Inject(
            method = "displayScoreboardSidebar",
            at = @At("HEAD"),
            cancellable = true
    )
    //? if >=26.1 {
    private void onRenderScoreboard(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
    //?} else if >=1.20 {
    /*private void onRenderScoreboard(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
    *///?} else {
    /*private void onRenderScoreboard(PoseStack graphics, Objective objective, CallbackInfo ci) {
    *///?}
        ScoreboardFeature feature = FeatureManager.getInstance().getScoreboardFeature();
        if (feature != null && feature.isEnabled()) {
            boolean handled = feature.render(new Gfx(graphics), objective);
            if (handled) {
                ci.cancel();
            }
        }
    }
}
