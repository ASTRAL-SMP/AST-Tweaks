package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorestock.AutoRestockFeature;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onInventory", at = @At("TAIL"))
    private void astTweaks$tryHideAutoRestockScreenAfterInventorySync(InventoryS2CPacket packet, CallbackInfo ci) {
        astTweaks$tryHideVisibleAutoRestockScreen();
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("TAIL"))
    private void astTweaks$tryHideAutoRestockScreenAfterSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        astTweaks$tryHideVisibleAutoRestockScreen();
    }

    @Unique
    private void astTweaks$tryHideVisibleAutoRestockScreen() {
        AutoRestockFeature autoRestock = FeatureManager.getInstance().getAutoRestockFeature();
        if (autoRestock != null) {
            autoRestock.tryHideVisibleContainerScreen();
        }
    }
}
