package com.astral.asttweaks.feature.lavahighlight;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for lava highlight feature.
 */
public class LavaHighlightConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().lavaHighlightEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().lavaHighlightEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isHighlightSource() {
        return ModConfig.getInstance().lavaHighlightSource;
    }

    public void setHighlightSource(boolean highlight) {
        ModConfig.getInstance().lavaHighlightSource = highlight;
        ModConfig.getInstance().save();
    }

    public boolean isHighlightFlowing() {
        return ModConfig.getInstance().lavaHighlightFlowing;
    }

    public void setHighlightFlowing(boolean highlight) {
        ModConfig.getInstance().lavaHighlightFlowing = highlight;
        ModConfig.getInstance().save();
    }

    public int getSourceColor() {
        return ModConfig.getInstance().lavaSourceColor;
    }

    public void setSourceColor(int color) {
        ModConfig.getInstance().lavaSourceColor = color;
        ModConfig.getInstance().save();
    }

    public int getFlowingColor() {
        return ModConfig.getInstance().lavaFlowingColor;
    }

    public void setFlowingColor(int color) {
        ModConfig.getInstance().lavaFlowingColor = color;
        ModConfig.getInstance().save();
    }
}
