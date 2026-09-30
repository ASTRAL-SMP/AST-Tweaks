package com.astral.asttweaks.feature.nvidiumchunkfix;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for the Nvidium Chunk Fix feature.
 */
public class NvidiumChunkFixConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().nvidiumChunkFixEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().nvidiumChunkFixEnabled = enabled;
        ModConfig.getInstance().save();
    }
}
