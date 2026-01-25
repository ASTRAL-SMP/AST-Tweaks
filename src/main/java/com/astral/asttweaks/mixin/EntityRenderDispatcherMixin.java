package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.entityculling.EntityCullingConfig;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void onShouldRender(Entity entity, Frustum frustum, double camX, double camY, double camZ,
                                 CallbackInfoReturnable<Boolean> cir) {
        EntityCullingFeature feature = FeatureManager.getInstance().getEntityCullingFeature();
        if (feature == null || !feature.isEnabled()) {
            return;
        }

        EntityCullingConfig config = feature.getConfig();
        boolean isPlayer = entity instanceof PlayerEntity;

        // Disable all entity rendering (except players)
        if (config.isDisableAllEntities() && !isPlayer) {
            cir.setReturnValue(false);
            return;
        }

        // Check entity blacklist (except players)
        if (!isPlayer) {
            EntityType<?> entityType = entity.getType();
            String entityId = Registries.ENTITY_TYPE.getId(entityType).toString();
            if (config.isEntityBlacklisted(entityId)) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Disable armor stand rendering
        if (entity instanceof ArmorStandEntity && config.isDisableArmorStands()) {
            cir.setReturnValue(false);
            return;
        }

        // Disable falling block rendering
        if (entity instanceof FallingBlockEntity && config.isDisableFallingBlocks()) {
            cir.setReturnValue(false);
            return;
        }

        // Disable dead mob rendering
        if (entity instanceof LivingEntity livingEntity && config.isDisableDeadMobs()) {
            if (livingEntity.getHealth() <= 0f) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Item entity rendering control
        if (entity instanceof ItemEntity itemEntity) {
            // Check item type blacklist first
            if (config.isItemBlacklisted(itemEntity.getStack().getItem())) {
                cir.setReturnValue(false);
                return;
            }
            // Then check render limit
            if (feature.shouldSkipItemRender()) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Limit XP orb rendering
        if (entity instanceof ExperienceOrbEntity) {
            if (feature.shouldSkipXpOrbRender()) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
