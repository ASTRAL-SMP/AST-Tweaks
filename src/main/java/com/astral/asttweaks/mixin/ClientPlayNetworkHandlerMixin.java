package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autorestock.AutoRestockFeature;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void astTweaks$tryHideAutoRestockScreenAfterInventorySync(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        astTweaks$tryHideVisibleAutoRestockScreen();
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void astTweaks$tryHideAutoRestockScreenAfterSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
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
