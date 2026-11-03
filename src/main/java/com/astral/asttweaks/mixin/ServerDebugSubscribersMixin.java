//? if >=26.1 {
package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.villagerlink.VillagerLinkFeature;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.debug.ServerDebugSubscribers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla only sends debug values (villager brains) to operators, so in a
 * singleplayer world without cheats Villager Link would never get brain data.
 * Lets the singleplayer owner of the integrated server receive them while the
 * feature is enabled; dedicated servers are unaffected (the mod isn't loaded there).
 */
@Mixin(ServerDebugSubscribers.class)
public abstract class ServerDebugSubscribersMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "hasRequiredPermissions", at = @At("HEAD"), cancellable = true)
    private void asttweaks$allowSingleplayerOwner(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!this.server.isSingleplayer() || !this.server.isSingleplayerOwner(player.nameAndId())) {
            return;
        }
        VillagerLinkFeature feature = FeatureManager.getInstance().getVillagerLinkFeature();
        if (feature != null && feature.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
//?}
