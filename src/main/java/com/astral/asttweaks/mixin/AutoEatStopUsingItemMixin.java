package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autoeat.AutoEatFeature;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AutoEat中にhandleInputEvents()からのstopUsingItem()呼び出しをキャンセルし、
 * useKeyに触れずに食事を継続させるMixin。
 */
@Mixin(ClientPlayerInteractionManager.class)
public class AutoEatStopUsingItemMixin {
    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    private void asttweaks$preventStopDuringAutoEat(PlayerEntity player, CallbackInfo ci) {
        AutoEatFeature feature = FeatureManager.getInstance().getAutoEatFeature();
        if (feature != null && feature.isAutoEating()) {
            ci.cancel();
        }
    }
}
