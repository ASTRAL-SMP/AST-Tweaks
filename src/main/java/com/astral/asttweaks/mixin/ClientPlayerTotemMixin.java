package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autototem.AutoTotemFeature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect totem of undying activation via network packet.
 * EntityStatus 35 is sent when a totem is consumed.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayerTotemMixin {
    private static final byte TOTEM_USE_STATUS = 35;

    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() == TOTEM_USE_STATUS) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null) {
                Entity entity = packet.getEntity(client.world);
                if (entity == client.player) {
                    AutoTotemFeature autoTotem = FeatureManager.getInstance().getAutoTotemFeature();
                    if (autoTotem != null) {
                        autoTotem.onTotemUsed();
                    }
                }
            }
        }
    }
}
