package com.astral.asttweaks.feature.voidtrade;

import java.util.ArrayList;
import java.util.List;

/**
 * 名前を付けて保存した Void Trade ワークフロー。Gson でそのまま保存される。
 */
public class VoidTradePreset {
    public String name = "";
    public List<VoidTradeStep> steps = new ArrayList<>();

    public VoidTradePreset() {
    }

    public VoidTradePreset(String name, List<VoidTradeStep> steps) {
        this.name = name;
        this.steps = copySteps(steps);
    }

    public static List<VoidTradeStep> copySteps(List<VoidTradeStep> steps) {
        List<VoidTradeStep> copy = new ArrayList<>(steps.size());
        for (VoidTradeStep step : steps) {
            copy.add(step.copy());
        }
        return copy;
    }
}
