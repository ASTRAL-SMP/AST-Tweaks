package com.astral.asttweaks.feature.notepad;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for the notepad feature.
 */
public class NotepadConfig {

    /**
     * Check if the notepad feature is enabled.
     */
    public boolean isEnabled() {
        return ModConfig.getInstance().notepadEnabled;
    }

    /**
     * Enable or disable the notepad feature.
     */
    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().notepadEnabled = enabled;
        ModConfig.getInstance().save();
    }
}
