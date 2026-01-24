package com.astral.asttweaks.feature.autototem;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for the auto totem restock feature.
 */
public class AutoTotemConfig {

    /**
     * Check if the auto totem feature is enabled.
     */
    public boolean isEnabled() {
        return ModConfig.getInstance().autoTotemEnabled;
    }

    /**
     * Enable or disable the auto totem feature.
     */
    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().autoTotemEnabled = enabled;
        ModConfig.getInstance().save();
    }
}
