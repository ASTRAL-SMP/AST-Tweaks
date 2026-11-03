//? if >=26.1 {
package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.villagerlink.VillagerLinkFeature;
import net.minecraft.client.multiplayer.ClientDebugSubscriber;
import net.minecraft.util.debug.DebugSubscription;
import net.minecraft.util.debug.DebugSubscriptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Requests villager brain dumps from the server only while Villager Link is enabled.
 * The subscription set is re-evaluated every client tick.
 */
@Mixin(ClientDebugSubscriber.class)
public abstract class ClientDebugSubscriberMixin {

    @Inject(method = "requestedSubscriptions", at = @At("RETURN"))
    private void asttweaks$requestBrains(CallbackInfoReturnable<Set<DebugSubscription<?>>> cir) {
        VillagerLinkFeature feature = FeatureManager.getInstance().getVillagerLinkFeature();
        if (feature != null && feature.isEnabled()) {
            cir.getReturnValue().add(DebugSubscriptions.BRAINS);
        }
    }
}
//?}
