package com.astral.asttweaks.feature;

/**
 * Base interface for all features in AST-Tweaks.
 * Each feature should implement this interface to be managed by FeatureManager.
 */
public interface Feature {
    /**
     * Returns the unique identifier for this feature.
     */
    String getId();

    /**
     * Returns the display name of this feature.
     */
    String getName();

    /**
     * Initialize the feature. Called once during mod initialization.
     */
    void init();

    /**
     * Called every tick when the feature is enabled.
     */
    void tick();

    /**
     * Check if the feature is currently enabled.
     */
    boolean isEnabled();

    /**
     * Enable or disable the feature.
     */
    void setEnabled(boolean enabled);
}
