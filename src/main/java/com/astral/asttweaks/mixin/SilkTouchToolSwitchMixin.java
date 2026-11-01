package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchFeature;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweakeroo の toolSwitch 後にシルクタッチ付きツールへの上書きを行う Mixin。
 * startDestroyBlock の RETURN で注入し、Tweakeroo の HEAD での選択を上書きする。
 */
@Mixin(MultiPlayerGameMode.class)
public class SilkTouchToolSwitchMixin {
    @Inject(method = "startDestroyBlock", at = @At("RETURN"))
    private void asttweaks$onAttackBlock(BlockPos pos, Direction direction,
            CallbackInfoReturnable<Boolean> cir) {
        SilkTouchSwitchFeature feature = FeatureManager.getInstance()
            .getSilkTouchSwitchFeature();
        if (feature != null) {
            feature.onAttackBlock(pos);
        }
    }
}
