package com.astral.asttweaks.feature.updatechecker;

/**
 * Enum representing how often the update checker should run.
 */
public enum CheckFrequency {
    STARTUP_ONLY("startup_only"),
    DAILY("daily"),
    WEEKLY("weekly");

    private final String id;

    CheckFrequency(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Get the interval in milliseconds for this frequency.
     * Returns 0 for STARTUP_ONLY (check every startup).
     */
    public long getIntervalMs() {
        return switch (this) {
            case STARTUP_ONLY -> 0;
            case DAILY -> 24L * 60 * 60 * 1000;
            case WEEKLY -> 7L * 24 * 60 * 60 * 1000;
        };
    }
}
