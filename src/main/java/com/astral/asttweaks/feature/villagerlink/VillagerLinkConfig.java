package com.astral.asttweaks.feature.villagerlink;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for villager link feature.
 */
public class VillagerLinkConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().villagerLinkEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().villagerLinkEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public int getRange() {
        return ModConfig.getInstance().villagerLinkRange;
    }

    public void setRange(int range) {
        ModConfig.getInstance().villagerLinkRange = range;
        ModConfig.getInstance().save();
    }

    public int getLineColor() {
        return ModConfig.getInstance().villagerLinkLineColor;
    }

    public void setLineColor(int color) {
        ModConfig.getInstance().villagerLinkLineColor = color;
        ModConfig.getInstance().save();
    }

    public boolean isSeeThrough() {
        return ModConfig.getInstance().villagerLinkSeeThrough;
    }

    public void setSeeThrough(boolean seeThrough) {
        ModConfig.getInstance().villagerLinkSeeThrough = seeThrough;
        ModConfig.getInstance().save();
    }

    public boolean isShowUnemployed() {
        return ModConfig.getInstance().villagerLinkShowUnemployed;
    }

    public void setShowUnemployed(boolean show) {
        ModConfig.getInstance().villagerLinkShowUnemployed = show;
        ModConfig.getInstance().save();
    }
}
