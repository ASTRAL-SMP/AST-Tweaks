package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AutoEat中にhandleKeybinds()からのreleaseUsingItem()呼び出しをキャンセルし、
 * useKeyに触れずに食事を継続させるMixin。
 */
@Mixin(MultiPlayerGameMode.class)
public class AutoEatStopUsingItemMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void asttweaks$preventStopDuringAutoEat(Player player, CallbackInfo ci) {
        AutoEatFeature feature = FeatureManager.getInstance().getAutoEatFeature();
        if (feature != null && feature.isAutoEating()) {
            ci.cancel();
        }
    }
}
