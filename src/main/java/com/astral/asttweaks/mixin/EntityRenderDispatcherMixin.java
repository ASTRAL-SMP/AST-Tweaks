package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    //? if >=26.3 {
    /*private void onShouldRender(Entity entity, Frustum frustum, double camX, double camY, double camZ, float partialTick,
                                 CallbackInfoReturnable<Boolean> cir) {
    *///?} else {
    private void onShouldRender(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                 CallbackInfoReturnable<Boolean> cir) {
    //?}
        EntityCullingFeature feature = FeatureManager.getInstance().getEntityCullingFeature();
        if (feature == null || !feature.isEnabled()) {
            return;
        }

        if (feature.isHidden(entity)) {
            cir.setReturnValue(false);
            return;
        }

        if (entity instanceof ItemEntity && feature.shouldSkipItemRender()) {
            cir.setReturnValue(false);
            return;
        }

        if (entity instanceof ExperienceOrb && feature.shouldSkipXpOrbRender()) {
            cir.setReturnValue(false);
        }
    }
}
