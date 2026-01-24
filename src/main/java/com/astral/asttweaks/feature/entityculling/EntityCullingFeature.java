package com.astral.asttweaks.feature.entityculling;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;

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
