package com.astral.asttweaks.compat;

import com.astral.asttweaks.ASTTweaks;

import java.lang.reflect.Method;

/**
 * Compatibility layer for Tweakeroo mod.
 * Handles disabling/restoring AlmostBrokenTools feature during auto-repair.
 * Uses reflection to avoid hard dependency on Tweakeroo.
 */
public class TweakerooCompat {
    private static boolean tweakerooAvailable = false;
    private static Object almostBrokenToolsToggle = null;
    private static Object toolSwitchToggle = null;
    private static Method getBooleanValueMethod = null;
    private static Method setBooleanValueMethod = null;
    private static boolean tweakerMoreAvailable = false;
    private static Object autoCollectMaterialListItemToggle = null;
    private static Method tweakerMoreGetBooleanValueMethod = null;

    // ServerDataSyncer integer configs (TweakerMore mod_tweaks)
    private static Object serverDataSyncerQueryIntervalConfig = null;
    private static Object serverDataSyncerQueryLimitConfig = null;
    private static Method tweakerMoreGetIntegerValueMethod = null;
    private static Method tweakerMoreSetIntegerValueMethod = null;

    // Stored state for restoration
    private static boolean previousState = false;
    private static boolean stateModified = false;

    /**
     * Initialize Tweakeroo compatibility.
     * Should be called during mod initialization.
     */
    public static void init() {
        initTweakeroo();
        initTweakerMore();
    }

    private static void initTweakeroo() {
        try {
            // Try to load Tweakeroo's FeatureToggle class
            Class<?> featureToggleClass = Class.forName("fi.dy.masa.tweakeroo.config.FeatureToggle");

            // Get the TWEAK_SWAP_ALMOST_BROKEN_TOOLS enum constant
            almostBrokenToolsToggle = Enum.valueOf(
                (Class<Enum>) featureToggleClass,
                "TWEAK_SWAP_ALMOST_BROKEN_TOOLS"
            );

            // Get the TWEAK_TOOL_SWITCH enum constant
            try {
                toolSwitchToggle = Enum.valueOf(
                    (Class<Enum>) featureToggleClass,
                    "TWEAK_TOOL_SWITCH"
                );
            } catch (IllegalArgumentException e) {
                ASTTweaks.LOGGER.warn("Tweakeroo TWEAK_TOOL_SWITCH not found: {}", e.getMessage());
            }

            // Get the methods we need
            // IConfigBoolean interface from maLiLib
            getBooleanValueMethod = featureToggleClass.getMethod("getBooleanValue");
            setBooleanValueMethod = featureToggleClass.getMethod("setBooleanValue", boolean.class);

            tweakerooAvailable = true;
            ASTTweaks.LOGGER.info("Tweakeroo compatibility initialized successfully");

        } catch (ClassNotFoundException e) {
            // Tweakeroo not installed - this is expected and fine
            ASTTweaks.LOGGER.info("Tweakeroo not found - AlmostBrokenTools bypass disabled");
            tweakerooAvailable = false;
        } catch (NoSuchMethodException e) {
            // API changed - warn but continue
            ASTTweaks.LOGGER.warn("Tweakeroo API changed - AlmostBrokenTools bypass disabled: {}", e.getMessage());
            tweakerooAvailable = false;
        } catch (Exception e) {
            // Unexpected error
            ASTTweaks.LOGGER.warn("Failed to initialize Tweakeroo compatibility: {}", e.getMessage());
            tweakerooAvailable = false;
        }
    }

    private static void initTweakerMore() {
        try {
            Class<?> configsClass = Class.forName("me.fallenbreath.tweakermore.config.TweakerMoreConfigs");
            autoCollectMaterialListItemToggle = configsClass.getField("AUTO_COLLECT_MATERIAL_LIST_ITEM").get(null);
            tweakerMoreGetBooleanValueMethod = autoCollectMaterialListItemToggle.getClass().getMethod("getBooleanValue");

            // ServerDataSyncer の Integer 設定（マップエンティティ等の問い合わせ間隔・上限）
            try {
                serverDataSyncerQueryIntervalConfig = configsClass.getField("SERVER_DATA_SYNCER_QUERY_INTERVAL").get(null);
                serverDataSyncerQueryLimitConfig = configsClass.getField("SERVER_DATA_SYNCER_QUERY_LIMIT").get(null);
                // TweakerMoreConfigInteger は maLiLib ConfigInteger を継承し、getIntegerValue/setIntegerValue を持つ
                tweakerMoreGetIntegerValueMethod = serverDataSyncerQueryIntervalConfig.getClass().getMethod("getIntegerValue");
                tweakerMoreSetIntegerValueMethod = serverDataSyncerQueryIntervalConfig.getClass().getMethod("setIntegerValue", int.class);
            } catch (NoSuchFieldException | NoSuchMethodException e) {
                ASTTweaks.LOGGER.warn("TweakerMore ServerDataSyncer integer config unavailable: {}", e.getMessage());
                serverDataSyncerQueryIntervalConfig = null;
                serverDataSyncerQueryLimitConfig = null;
            }

            tweakerMoreAvailable = true;
            ASTTweaks.LOGGER.info("TweakerMore compatibility initialized successfully");
        } catch (ClassNotFoundException e) {
            ASTTweaks.LOGGER.info("TweakerMore not found - compatibility disabled");
            tweakerMoreAvailable = false;
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            ASTTweaks.LOGGER.warn("TweakerMore API changed - compatibility disabled: {}", e.getMessage());
            tweakerMoreAvailable = false;
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to initialize TweakerMore compatibility: {}", e.getMessage());
            tweakerMoreAvailable = false;
        }
    }

    /**
     * Check if Tweakeroo is available.
     */
    public static boolean isTweakerooAvailable() {
        return tweakerooAvailable;
    }

    /**
     * Disable AlmostBrokenTools feature and save the previous state.
     * Safe to call even if Tweakeroo is not installed.
     */
    public static void disableAlmostBrokenTools() {
        if (!tweakerooAvailable || stateModified) {
            return;
        }

        try {
            // Get current state
            previousState = (boolean) getBooleanValueMethod.invoke(almostBrokenToolsToggle);

            if (previousState) {
                // Disable the feature
                setBooleanValueMethod.invoke(almostBrokenToolsToggle, false);
                stateModified = true;
                ASTTweaks.LOGGER.info("Disabled Tweakeroo AlmostBrokenTools for auto-repair");
            }
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to disable AlmostBrokenTools: {}", e.getMessage());
        }
    }

    /**
     * Restore AlmostBrokenTools to its previous state.
     * Safe to call even if Tweakeroo is not installed or state wasn't modified.
     */
    public static void restoreAlmostBrokenTools() {
        if (!tweakerooAvailable || !stateModified) {
            return;
        }

        try {
            // Restore previous state
            setBooleanValueMethod.invoke(almostBrokenToolsToggle, previousState);
            stateModified = false;
            ASTTweaks.LOGGER.info("Restored Tweakeroo AlmostBrokenTools to previous state: {}", previousState);
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to restore AlmostBrokenTools: {}", e.getMessage());
            stateModified = false; // Reset flag even on error to prevent repeated attempts
        }
    }

    /**
     * Ensure AlmostBrokenTools is restored.
     * Use this as a safety measure when repair is interrupted or feature is disabled.
     */
    public static void ensureRestored() {
        if (stateModified) {
            restoreAlmostBrokenTools();
        }
    }

    /**
     * Check if Tweakeroo's Tool Switch feature is currently enabled.
     */
    public static boolean isToolSwitchEnabled() {
        if (!tweakerooAvailable || toolSwitchToggle == null) return false;
        try {
            return (boolean) getBooleanValueMethod.invoke(toolSwitchToggle);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Whether TweakerMore's ServerDataSyncer integer configs are accessible via reflection.
     */
    public static boolean isServerDataSyncerConfigAvailable() {
        return tweakerMoreAvailable
                && serverDataSyncerQueryIntervalConfig != null
                && serverDataSyncerQueryLimitConfig != null
                && tweakerMoreGetIntegerValueMethod != null
                && tweakerMoreSetIntegerValueMethod != null;
    }

    /**
     * Read TweakerMore's serverDataSyncerQueryInterval. Returns -1 if unavailable.
     */
    public static int getServerDataSyncerQueryInterval() {
        if (!isServerDataSyncerConfigAvailable()) return -1;
        try {
            return (int) tweakerMoreGetIntegerValueMethod.invoke(serverDataSyncerQueryIntervalConfig);
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to read serverDataSyncerQueryInterval: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Read TweakerMore's serverDataSyncerQueryLimit. Returns -1 if unavailable.
     */
    public static int getServerDataSyncerQueryLimit() {
        if (!isServerDataSyncerConfigAvailable()) return -1;
        try {
            return (int) tweakerMoreGetIntegerValueMethod.invoke(serverDataSyncerQueryLimitConfig);
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to read serverDataSyncerQueryLimit: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Write TweakerMore's serverDataSyncerQueryInterval. Returns false if unavailable.
     */
    public static boolean setServerDataSyncerQueryInterval(int value) {
        if (!isServerDataSyncerConfigAvailable()) return false;
        try {
            tweakerMoreSetIntegerValueMethod.invoke(serverDataSyncerQueryIntervalConfig, value);
            return true;
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to set serverDataSyncerQueryInterval: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Write TweakerMore's serverDataSyncerQueryLimit. Returns false if unavailable.
     */
    public static boolean setServerDataSyncerQueryLimit(int value) {
        if (!isServerDataSyncerConfigAvailable()) return false;
        try {
            tweakerMoreSetIntegerValueMethod.invoke(serverDataSyncerQueryLimitConfig, value);
            return true;
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to set serverDataSyncerQueryLimit: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if TweakerMore's autoCollectMaterialListItem feature is currently enabled.
     */
    public static boolean isAutoCollectMaterialListItemEnabled() {
        if (!tweakerMoreAvailable || autoCollectMaterialListItemToggle == null || tweakerMoreGetBooleanValueMethod == null) {
            return false;
        }

        try {
            return (boolean) tweakerMoreGetBooleanValueMethod.invoke(autoCollectMaterialListItemToggle);
        } catch (Exception e) {
            return false;
        }
    }
}
