package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.voidtrade.VoidTradeFeature;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Void Trade の移動ステップ中にプレイヤーの移動入力を上書きするMixin。
 * 取引画面を開いたままでも歩けるよう、KeyBinding ではなく Input#tick 直後の入力値を直接書き換える。
 */
@Mixin(ClientPlayerEntity.class)
public class VoidTradeInputMixin {
    @Shadow public Input input;

    @Inject(
        method = "tickMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/input/Input;tick(ZF)V",
            shift = At.Shift.AFTER
        )
    )
    private void asttweaks$applyVoidTradeMovement(CallbackInfo ci) {
        VoidTradeFeature feature = FeatureManager.getInstance().getVoidTradeFeature();
        if (feature != null) {
            feature.applyMovementInput((ClientPlayerEntity) (Object) this, this.input);
        }
    }
}
