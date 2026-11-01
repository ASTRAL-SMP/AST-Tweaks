package com.astral.asttweaks.mixin;

import com.astral.asttweaks.feature.FeatureManager;
import com.astral.asttweaks.feature.entityculling.EntityCullingConfig;
import com.astral.asttweaks.feature.entityculling.EntityCullingFeature;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
        boolean isPlayer = entity instanceof Player;

        // Disable all entity rendering (except players)
        if (config.isDisableAllEntities() && !isPlayer) {
            cir.setReturnValue(false);
            return;
        }

        // Check entity blacklist (except players)
        if (!isPlayer) {
            EntityType<?> entityType = entity.getType();
            String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
            if (config.isEntityBlacklisted(entityId)) {
                cir.setReturnValue(false);
                return;
            }
        }

        // Disable armor stand rendering
        if (entity instanceof ArmorStand && config.isDisableArmorStands()) {
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
            if (config.isItemBlacklisted(itemEntity.getItem().getItem())) {
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
        if (entity instanceof ExperienceOrb) {
            if (feature.shouldSkipXpOrbRender()) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
