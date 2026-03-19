package com.astral.asttweaks.feature.mousesensitivity;

import com.astral.asttweaks.config.ModConfig;

/**
 * マウス感度トグル機能の設定ラッパー。
 */
public class MouseSensitivityConfig {

    /**
     * マウス感度トグル機能が有効かどうか。
     */
    public boolean isEnabled() {
        return ModConfig.getInstance().mouseSensitivityEnabled;
    }

    /**
     * マウス感度トグル機能の有効/無効を設定。
     */
    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().mouseSensitivityEnabled = enabled;
        ModConfig.getInstance().save();
    }

    /**
     * 目標感度を取得（0-200の%表示値）。
     */
    public int getTargetSensitivity() {
        return ModConfig.getInstance().mouseSensitivityTargetValue;
    }

    /**
     * 目標感度を設定（0-200の%表示値）。
     */
    public void setTargetSensitivity(int value) {
        ModConfig.getInstance().mouseSensitivityTargetValue = value;
        ModConfig.getInstance().save();
    }
}
