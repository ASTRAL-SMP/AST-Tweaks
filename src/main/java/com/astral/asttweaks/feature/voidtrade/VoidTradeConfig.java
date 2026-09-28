package com.astral.asttweaks.feature.voidtrade;

import com.astral.asttweaks.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration wrapper for the Void Trade feature.
 */
public class VoidTradeConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().voidTradeEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().voidTradeEnabled = enabled;
        ModConfig.getInstance().save();
    }

    /**
     * ループ回数。0 は停止するまで無限。
     */
    public int getLoopCount() {
        return Math.max(0, ModConfig.getInstance().voidTradeLoopCount);
    }

    /**
     * 待機ステップに一律で足す tick 数（ラグ対策）。
     */
    public int getExtraWaitTicks() {
        return Math.max(0, ModConfig.getInstance().voidTradeExtraWaitTicks);
    }

    public int getStepTimeoutTicks() {
        return Math.max(1, ModConfig.getInstance().voidTradeStepTimeoutSeconds) * 20;
    }

    /**
     * ワークフロー本体。編集画面はこのリストを直接書き換える。
     */
    public List<VoidTradeStep> getSteps() {
        ModConfig config = ModConfig.getInstance();
        if (config.voidTradeSteps == null) {
            config.voidTradeSteps = new ArrayList<>();
        }
        return config.voidTradeSteps;
    }

    /**
     * 名前付きで保存したワークフロー。
     */
    public List<VoidTradePreset> getPresets() {
        ModConfig config = ModConfig.getInstance();
        if (config.voidTradePresets == null) {
            config.voidTradePresets = new ArrayList<>();
        }
        return config.voidTradePresets;
    }

    public void save() {
        ModConfig.getInstance().save();
    }
}
