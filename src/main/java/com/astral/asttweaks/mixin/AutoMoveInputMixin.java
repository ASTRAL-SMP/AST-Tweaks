package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.automove.MoveDirection;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * freecam中にAutoMoveのプレイヤー移動入力を直接注入するMixin。
 * ClientPlayerEntity.tickMovement()内のinput.tick()呼び出し直後に注入し、
 * freecamモジュールによる入力ゼロ化を上書きしてプレイヤー本体を移動させる。
 */
@Mixin(value = ClientPlayerEntity.class, priority = 1100)
public class AutoMoveInputMixin {
    @Shadow public Input input;

    @Inject(
        method = "tickMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/input/Input;tick(ZF)V",
            shift = At.Shift.AFTER
        )
    )
    private void asttweaks$injectAutoMoveInput(CallbackInfo ci) {
        AutoMoveFeature feature = FeatureManager.getInstance().getAutoMoveFeature();
        if (feature == null || !feature.isFreecamActive()) return;

        if (feature.isEnabled() && feature.isMoving()) {
            // freecam中のAutoMove: プレイヤーの入力を直接注入
            MoveDirection dir = feature.getConfig().getDirection();
            switch (dir) {
                case FORWARD -> {
                    this.input.pressingForward = true;
                    this.input.movementForward = 1.0f;
                }
                case BACKWARD -> {
                    this.input.pressingBack = true;
                    this.input.movementForward = -1.0f;
                }
                case LEFT -> {
                    this.input.pressingLeft = true;
                    this.input.movementSideways = 1.0f;
                }
                case RIGHT -> {
                    this.input.pressingRight = true;
                    this.input.movementSideways = -1.0f;
                }
            }
        } else {
            // freecam中でAutoMove無効: 残留入力をクリアして停止させる
            this.input.pressingForward = false;
            this.input.pressingBack = false;
            this.input.pressingLeft = false;
            this.input.pressingRight = false;
            this.input.movementForward = 0.0f;
            this.input.movementSideways = 0.0f;
        }
    }
}
