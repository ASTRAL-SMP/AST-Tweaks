package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.silktouchswitch.SilkTouchSwitchFeature;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweakeroo の toolSwitch 後にシルクタッチ付きツールへの上書きを行う Mixin。
 * attackBlock の RETURN で注入し、Tweakeroo の HEAD での選択を上書きする。
 */
@Mixin(ClientPlayerInteractionManager.class)
public class SilkTouchToolSwitchMixin {
    @Inject(method = "attackBlock", at = @At("RETURN"))
    private void asttweaks$onAttackBlock(BlockPos pos, Direction direction,
            CallbackInfoReturnable<Boolean> cir) {
        SilkTouchSwitchFeature feature = FeatureManager.getInstance()
            .getSilkTouchSwitchFeature();
        if (feature != null) {
            feature.onAttackBlock(pos);
        }
    }
}
