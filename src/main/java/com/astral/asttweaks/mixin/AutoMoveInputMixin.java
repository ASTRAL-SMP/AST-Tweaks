package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.automove.MoveDirection;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * freecam中にAutoMoveのプレイヤー移動入力を直接注入するMixin。
 * LocalPlayer.aiStep()内のinput.tick()呼び出し直後に注入し、
 * freecamモジュールによる入力ゼロ化を上書きしてプレイヤー本体を移動させる。
 */
@Mixin(value = LocalPlayer.class, priority = 1100)
public class AutoMoveInputMixin {
    @Shadow public ClientInput input;

    @Inject(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/ClientInput;tick()V",
            shift = At.Shift.AFTER
        )
    )
    private void asttweaks$injectAutoMoveInput(CallbackInfo ci) {
        AutoMoveFeature feature = FeatureManager.getInstance().getAutoMoveFeature();
        if (feature == null || !feature.isFreecamActive()) return;

        if (feature.isEnabled() && feature.isMoving()) {
            // freecam中のAutoMove: プレイヤーの入力を直接注入（斜めは2軸同時）
            MoveDirection dir = feature.getConfig().getDirection();
            int fwd = dir.getForwardAxis();
            int side = dir.getSidewaysAxis();
            Input keys = this.input.keyPresses;
            this.input.keyPresses = new Input(fwd > 0, fwd < 0, side > 0, side < 0,
                    keys.jump(), keys.shift(), keys.sprint());
            ((ClientInputAccessor) this.input).asttweaks$setMoveVector(new Vec2(side, fwd).normalized());
        } else {
            // freecam中でAutoMove無効: 残留入力をクリアして停止させる
            Input keys = this.input.keyPresses;
            this.input.keyPresses = new Input(false, false, false, false,
                    keys.jump(), keys.shift(), keys.sprint());
            ((ClientInputAccessor) this.input).asttweaks$setMoveVector(Vec2.ZERO);
        }
    }
}
