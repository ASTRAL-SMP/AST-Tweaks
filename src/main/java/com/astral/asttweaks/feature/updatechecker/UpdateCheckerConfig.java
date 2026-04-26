package com.astral.asttweaks.feature.updatechecker;

import com.astral.asttweaks.config.ModConfig;

/**
 * Configuration wrapper for the Update Checker feature.
 */
public class UpdateCheckerConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().updateCheckerEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().updateCheckerEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public String getGithubRepo() {
        return ModConfig.getInstance().updateCheckerGithubRepo;
    }

    public void setGithubRepo(String repo) {
        ModConfig.getInstance().updateCheckerGithubRepo = repo;
        ModConfig.getInstance().save();
    }

    public CheckFrequency getFrequency() {
        return ModConfig.getInstance().updateCheckerFrequency;
    }

    public void setFrequency(CheckFrequency frequency) {
        ModConfig.getInstance().updateCheckerFrequency = frequency;
        ModConfig.getInstance().save();
    }

    public long getLastCheck() {
        return ModConfig.getInstance().updateCheckerLastCheck;
    }

    public void setLastCheck(long timestamp) {
        ModConfig.getInstance().updateCheckerLastCheck = timestamp;
        ModConfig.getInstance().save();
    }

    public boolean isShowNotification() {
        return ModConfig.getInstance().updateCheckerShowNotification;
    }

    public void setShowNotification(boolean show) {
        ModConfig.getInstance().updateCheckerShowNotification = show;
        ModConfig.getInstance().save();
    }

    public boolean isShowOnTitleScreen() {
        return ModConfig.getInstance().updateCheckerShowOnTitleScreen;
    }

    public void setShowOnTitleScreen(boolean show) {
        ModConfig.getInstance().updateCheckerShowOnTitleScreen = show;
        ModConfig.getInstance().save();
    }

    public String getSkippedVersion() {
        String v = ModConfig.getInstance().updateCheckerSkippedVersion;
        return v == null ? "" : v;
    }

    public void setSkippedVersion(String version) {
        ModConfig.getInstance().updateCheckerSkippedVersion = version == null ? "" : version;
        ModConfig.getInstance().save();
    }

    /**
     * Check if enough time has passed since the last check based on the frequency setting.
     */
    public boolean shouldCheck() {
        if (!isEnabled()) {
            return false;
        }

        String repo = getGithubRepo();
        if (repo == null || repo.isBlank()) {
            return false;
        }

        CheckFrequency frequency = getFrequency();
        if (frequency == CheckFrequency.STARTUP_ONLY) {
            return true;
        }

        long intervalMs = frequency.getIntervalMs();
        long lastCheck = getLastCheck();
        long now = System.currentTimeMillis();

        return (now - lastCheck) >= intervalMs;
    }
}
