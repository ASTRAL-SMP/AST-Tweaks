package com.astral.asttweaks.compat;

import com.astral.asttweaks.ASTTweaks;

import java.lang.reflect.Field;

/**
 * Compatibility layer for Nvidium mod.
 * Nvidium (0.1.x) recomputes IS_ENABLED from IS_COMPATIBLE every time Sodium's
 * RenderSectionManager is constructed (= world renderer reload), so flipping
 * IS_COMPATIBLE and reloading the world renderer cleanly switches the terrain
 * backend between Nvidium and Sodium at runtime.
 * Uses reflection to avoid hard dependency on Nvidium.
 */
public class NvidiumCompat {
    private static boolean available = false;
    private static Field isCompatibleField = null;
    private static Field isEnabledField = null;

    // 初回 suppress 時に GPU 判定済みの元値を控えておき、restore で戻す
    private static boolean originalCaptured = false;
    private static boolean originallyCompatible = false;
    private static boolean suppressed = false;

    /**
     * Initialize Nvidium compatibility.
     * Should be called during mod initialization.
     */
    public static void init() {
        try {
            Class<?> nvidiumClass = Class.forName("me.cortex.nvidium.Nvidium");
            isCompatibleField = nvidiumClass.getField("IS_COMPATIBLE");
            isEnabledField = nvidiumClass.getField("IS_ENABLED");
            available = true;
            ASTTweaks.LOGGER.info("Nvidium compatibility initialized successfully");
        } catch (ClassNotFoundException e) {
            // Nvidium not installed - this is expected and fine
            ASTTweaks.LOGGER.info("Nvidium not found - world border fix will be a no-op");
            available = false;
        } catch (NoSuchFieldException e) {
            ASTTweaks.LOGGER.warn("Nvidium API changed - world border fix disabled: {}", e.getMessage());
            available = false;
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to initialize Nvidium compatibility: {}", e.getMessage());
            available = false;
        }
    }

    /**
     * Check if Nvidium is available.
     */
    public static boolean isAvailable() {
        return available;
    }

    /**
     * Whether Nvidium is currently suppressed by this mod.
     */
    public static boolean isSuppressed() {
        return suppressed;
    }

    /**
     * Whether Nvidium's renderer is currently active (IS_ENABLED).
     * IS_ENABLED is recomputed only when the RenderSectionManager is constructed,
     * so this reflects the state of the renderer actually in use right now.
     */
    public static boolean isCurrentlyEnabled() {
        if (!available) {
            return false;
        }
        try {
            return isEnabledField.getBoolean(null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Temporarily mark Nvidium as incompatible so the next world renderer reload
     * falls back to Sodium's default terrain path.
     * Returns true if the state actually changed (caller should reload the renderer).
     */
    public static boolean suppress() {
        if (!available || suppressed) {
            return false;
        }
        try {
            if (!originalCaptured) {
                originallyCompatible = isCompatibleField.getBoolean(null);
                originalCaptured = true;
            }
            if (!originallyCompatible) {
                // GPU 非対応等で元々 Nvidium が動いていないなら何もしない
                return false;
            }
            isCompatibleField.setBoolean(null, false);
            suppressed = true;
            return true;
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to suppress Nvidium: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Restore Nvidium's original compatibility flag.
     * Returns true if the state actually changed (caller should reload the renderer).
     */
    public static boolean restore() {
        if (!available || !suppressed) {
            return false;
        }
        try {
            isCompatibleField.setBoolean(null, originallyCompatible);
            suppressed = false;
            return true;
        } catch (Exception e) {
            ASTTweaks.LOGGER.warn("Failed to restore Nvidium: {}", e.getMessage());
            suppressed = false; // Reset flag even on error to prevent repeated attempts
            return false;
        }
    }
}
