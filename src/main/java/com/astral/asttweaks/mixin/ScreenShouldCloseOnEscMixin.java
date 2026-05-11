package com.astral.asttweaks.mixin;

import com.astral.asttweaks.config.KeyComboEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * KeyComboEntry がリスニング中の間、Cloth Config の設定画面を ESC で閉じないようにする。
 *
 * AbstractConfigScreen.keyPressed は ESC を最初に拾って `quit()` を呼ぶため、
 * エントリの keyPressed まで ESC が届かない。`shouldCloseOnEsc()` を false に倒すと、
 * AbstractConfigScreen と Screen の keyPressed はいずれも else 分岐 (super.keyPressed) に
 * 流れ、最終的に AbstractParentElement 経由で当エントリの keyPressed が呼ばれる。
 *
 * 影響範囲を Cloth Config の設定画面に限定するため instanceof で絞る
 * (チャット・ポーズメニュー等の他画面への副作用を回避)。
 */
@Mixin(Screen.class)
public class ScreenShouldCloseOnEscMixin {
    @Inject(method = "shouldCloseOnEsc", at = @At("HEAD"), cancellable = true)
    private void astTweaks$preventEscWhileBinding(CallbackInfoReturnable<Boolean> cir) {
        if (!KeyComboEntry.isAnyListening()) return;
        if (!(((Object) this) instanceof AbstractConfigScreen)) return;
        cir.setReturnValue(false);
    }
}
