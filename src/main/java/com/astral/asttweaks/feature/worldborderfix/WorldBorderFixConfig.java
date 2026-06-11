package com.astral.asttweaks.feature.worldborderfix;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for the World Border Fix feature.
 */
public class WorldBorderFixConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().worldBorderFixEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().worldBorderFixEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isXrayFixEnabled() {
        return ModConfig.getInstance().worldBorderFixXray;
    }

    public boolean isFarCoordFixEnabled() {
        return ModConfig.getInstance().worldBorderFixFarCoords;
    }

    public boolean isAutoReenable() {
        return ModConfig.getInstance().worldBorderFixAutoReenable;
    }

    public int getBorderDistance() {
        return Math.max(1, ModConfig.getInstance().worldBorderFixDistance);
    }

    public int getCoordThreshold() {
        return Math.max(1000, ModConfig.getInstance().worldBorderFixCoordThreshold);
    }
}
