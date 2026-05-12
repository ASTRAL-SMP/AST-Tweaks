package com.astral.asttweaks.feature.pickprotect;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;

/**
 * Protects specific hotbar slots from being overwritten by the vanilla
 * "Pick Block" action. The actual interception is handled by PickBlockMixin.
 */
public class PickProtectFeature implements Feature {
    private final PickProtectConfig config;

    public PickProtectFeature() {
        this.config = new PickProtectConfig();
    }

    @Override
    public String getId() { return "pickprotect"; }

    @Override
    public String getName() { return "Pick Protect"; }

    @Override
    public void init() {
        ASTTweaks.LOGGER.info("Pick Protect feature initialized");
    }

    @Override
    public void tick() {}

    @Override
    public boolean isEnabled() { return config.isEnabled(); }

    @Override
    public void setEnabled(boolean enabled) { config.setEnabled(enabled); }

    public PickProtectConfig getConfig() { return config; }
}
