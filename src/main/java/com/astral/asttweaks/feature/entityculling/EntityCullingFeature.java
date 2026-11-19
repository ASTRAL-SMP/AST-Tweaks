package com.astral.asttweaks.feature.entityculling;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Entity culling feature that disables rendering of certain entities.
 */
public class EntityCullingFeature implements Feature {
    private final EntityCullingConfig config;

    // Render counters for limiting entity rendering per frame
    private int itemRenderCount = 0;
    private int xpOrbRenderCount = 0;

    public EntityCullingFeature() {
        this.config = new EntityCullingConfig();
    }

    @Override
    public String getId() {
        return "entityculling";
    }

    @Override
    public String getName() {
        return "Entity Culling";
    }

    @Override
    public void init() {
        ASTTweaks.LOGGER.info("EntityCulling feature initialized");
    }

    @Override
    public void tick() {
        // Reset render counters each tick
        itemRenderCount = 0;
        xpOrbRenderCount = 0;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public EntityCullingConfig getConfig() {
        return config;
    }

    /**
     * Whether a configured rule hides this entity. Render limits are not included
     * because they depend on how many entities were already drawn this tick.
     */
    public boolean isHidden(Entity entity) {
        boolean isPlayer = entity instanceof Player;
        if (!isPlayer && (config.isDisableAllEntities() || config.isEntityBlacklisted(entity.getType()))) {
            return true;
        }
        if (entity instanceof ArmorStand && config.isDisableArmorStands()) {
            return true;
        }
        if (entity instanceof FallingBlockEntity && config.isDisableFallingBlocks()) {
            return true;
        }
        if (entity instanceof LivingEntity livingEntity && config.isDisableDeadMobs() && livingEntity.getHealth() <= 0f) {
            return true;
        }
        return entity instanceof ItemEntity itemEntity && config.isItemBlacklisted(itemEntity.getItem().getItem());
    }

    /**
     * Increment item render count and check if limit exceeded.
     * @return true if should skip rendering
     */
    public boolean shouldSkipItemRender() {
        int limit = config.getItemRenderLimit();
        if (limit < 0) {
            return false;
        }
        return ++itemRenderCount > limit;
    }

    /**
     * Increment XP orb render count and check if limit exceeded.
     * @return true if should skip rendering
     */
    public boolean shouldSkipXpOrbRender() {
        int limit = config.getXpOrbRenderLimit();
        if (limit < 0) {
            return false;
        }
        return ++xpOrbRenderCount > limit;
    }

    /**
     * Reset render counters. Called at the start of each frame.
     */
    public void resetRenderCounters() {
        itemRenderCount = 0;
        xpOrbRenderCount = 0;
    }
}
