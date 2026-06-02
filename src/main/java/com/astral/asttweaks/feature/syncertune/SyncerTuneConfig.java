package com.astral.asttweaks.feature.syncertune;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for the Syncer Tune feature.
 */
public class SyncerTuneConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().syncerTuneEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().syncerTuneEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public int getDetectionChunkRadius() {
        return Math.max(1, Math.min(16, ModConfig.getInstance().syncerTuneDetectionChunkRadius));
    }

    public int getDetectionIntervalTicks() {
        return Math.max(1, ModConfig.getInstance().syncerTuneDetectionIntervalTicks);
    }

    public int getHighDensityThreshold() {
        return Math.max(1, ModConfig.getInstance().syncerTuneHighThreshold);
    }

    public int getHighInterval() {
        return clampQueryInterval(ModConfig.getInstance().syncerTuneHighInterval);
    }

    public int getHighLimit() {
        return clampQueryLimit(ModConfig.getInstance().syncerTuneHighLimit);
    }

    public int getNormalInterval() {
        return clampQueryInterval(ModConfig.getInstance().syncerTuneNormalInterval);
    }

    public int getNormalLimit() {
        return clampQueryLimit(ModConfig.getInstance().syncerTuneNormalLimit);
    }

    /** TweakerMore serverDataSyncerQueryInterval の許容範囲は 1..100 */
    private static int clampQueryInterval(int value) {
        if (value < 1) return 1;
        if (value > 100) return 100;
        return value;
    }

    /** TweakerMore serverDataSyncerQueryLimit の許容範囲は 1..8192 */
    private static int clampQueryLimit(int value) {
        if (value < 1) return 1;
        if (value > 8192) return 8192;
        return value;
    }
}
