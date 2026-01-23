package com.astral.asttweaks.feature.automove;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for auto-move feature.
 * Delegates to main ModConfig for persistence.
 */
public class AutoMoveConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().autoMoveEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().autoMoveEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public MoveDirection getDirection() {
        return ModConfig.getInstance().autoMoveDirection;
    }

    public void setDirection(MoveDirection direction) {
        ModConfig.getInstance().autoMoveDirection = direction;
        ModConfig.getInstance().save();
    }
}
