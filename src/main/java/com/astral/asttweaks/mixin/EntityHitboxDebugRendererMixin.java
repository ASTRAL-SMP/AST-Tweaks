//? if >=26.1 && <26.2 {
package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On 26.1 the F3+B hitbox renderer walks every entity without asking shouldRender,
 * so entities hidden by Entity Culling need to be skipped here as well.
 * From 26.2 on, vanilla goes through LevelExtractor.isEntityVisible and this is not needed.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {

    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
    private void asttweaks$skipCulledEntity(Entity entity, float partialTick, boolean serverEntity, CallbackInfo ci) {
        EntityCullingFeature feature = FeatureManager.getInstance().getEntityCullingFeature();
        if (feature != null && feature.isEnabled() && feature.isHidden(entity)) {
            ci.cancel();
        }
    }
}
//?}
