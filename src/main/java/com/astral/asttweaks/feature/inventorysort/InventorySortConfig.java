package com.astral.asttweaks.feature.inventorysort;

import com.astral.asttweaks.config.ModConfig;

import java.util.Set;

/**
 * Configuration wrapper for inventory sort feature.
 */
public class InventorySortConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().inventorySortEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().inventorySortEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public SortMode getSortMode() {
        return ModConfig.getInstance().inventorySortMode;
    }

    public void setSortMode(SortMode mode) {
        ModConfig.getInstance().inventorySortMode = mode;
        ModConfig.getInstance().save();
    }

    public SortRange getSortRange() {
        return SortRange.MAIN_ONLY;  // 常にメインインベントリのみ
    }

    public Set<Integer> getExcludedSlots() {
        return ModConfig.getInstance().inventorySortExcludedSlots;
    }

    public boolean isSlotExcluded(int slot) {
        return ModConfig.getInstance().inventorySortExcludedSlots.contains(slot);
    }

    public void toggleSlotExclusion(int slot) {
        Set<Integer> excluded = ModConfig.getInstance().inventorySortExcludedSlots;
        if (excluded.contains(slot)) {
            excluded.remove(slot);
        } else {
            excluded.add(slot);
        }
        ModConfig.getInstance().save();
    }

    public void setSlotExcluded(int slot, boolean excluded) {
        if (excluded) {
            ModConfig.getInstance().inventorySortExcludedSlots.add(slot);
        } else {
            ModConfig.getInstance().inventorySortExcludedSlots.remove(slot);
        }
        ModConfig.getInstance().save();
    }

    public SortTarget getSortTarget() {
        return ModConfig.getInstance().inventorySortTarget;
    }

    public void setSortTarget(SortTarget target) {
        ModConfig.getInstance().inventorySortTarget = target;
        ModConfig.getInstance().save();
    }

    public boolean isShowButton() {
        return ModConfig.getInstance().inventorySortShowButton;
    }

    public void setShowButton(boolean show) {
        ModConfig.getInstance().inventorySortShowButton = show;
        ModConfig.getInstance().save();
    }
}
