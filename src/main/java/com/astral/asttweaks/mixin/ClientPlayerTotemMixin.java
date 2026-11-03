package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.autototem.AutoTotemFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect totem of undying activation via network packet.
 * EntityStatus 35 is sent when a totem is consumed.
 */
@Mixin(ClientPacketListener.class)
public class ClientPlayerTotemMixin {
    private static final byte TOTEM_USE_STATUS = 35;

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void onEntityStatus(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() == TOTEM_USE_STATUS) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && client.player != null) {
                Entity entity = packet.getEntity(client.level);
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
