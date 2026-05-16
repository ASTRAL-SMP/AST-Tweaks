package com.astral.asttweaks.feature.portalprotect;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for the Portal Protect feature.
 */
public class PortalProtectConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().portalProtectEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().portalProtectEnabled = enabled;
        ModConfig.getInstance().save();
    }
}
