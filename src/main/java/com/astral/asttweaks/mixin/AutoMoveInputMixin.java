package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.automove.AutoMoveFeature;
import com.astral.asttweaks.feature.automove.MoveDirection;
//? if >=26.1 {
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
//?} else {
/*import net.minecraft.client.player.Input;
*///?}
import net.minecraft.client.player.LocalPlayer;
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
    //? if >=26.1 {
    @Shadow public ClientInput input;
    //?} else {
    /*@Shadow public Input input;
    *///?}

    @Inject(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            //? if >=26.1 {
            target = "Lnet/minecraft/client/player/ClientInput;tick()V",
            //?} else {
            /*target = "Lnet/minecraft/client/player/Input;tick(ZF)V",
            *///?}
            shift = At.Shift.AFTER
        )
    )
    private void asttweaks$injectAutoMoveInput(CallbackInfo ci) {
        AutoMoveFeature feature = FeatureManager.getInstance().getAutoMoveFeature();
        if (feature == null || !feature.isFreecamActive()) return;

        if (feature.isEnabled() && feature.isMoving()) {
            // freecam中のAutoMove: プレイヤーの入力を直接注入（斜めは2軸同時）
            MoveDirection dir = feature.getConfig().getDirection();
            setInput(dir.getForwardAxis(), dir.getSidewaysAxis());
        } else {
            // freecam中でAutoMove無効: 残留入力をクリアして停止させる
            setInput(0, 0);
        }
    }

    private void setInput(int fwd, int side) {
        //? if >=26.1 {
        Input keys = this.input.keyPresses;
        this.input.keyPresses = new Input(fwd > 0, fwd < 0, side > 0, side < 0,
                keys.jump(), keys.shift(), keys.sprint());
        ((ClientInputAccessor) this.input).asttweaks$setMoveVector(
                fwd == 0 && side == 0 ? Vec2.ZERO : new Vec2(side, fwd).normalized());
        //?} else {
        /*this.input.up = fwd > 0;
        this.input.down = fwd < 0;
        this.input.left = side > 0;
        this.input.right = side < 0;
        this.input.forwardImpulse = fwd;
        this.input.leftImpulse = side;
        *///?}
    }
}
