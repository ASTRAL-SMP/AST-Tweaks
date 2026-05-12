package com.astral.asttweaks.feature.pickprotect;

import com.astral.asttweaks.config.ModConfig;

import java.util.Set;

/**
 * Configuration wrapper for the Pick Protect feature.
 */
public class PickProtectConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().pickProtectEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().pickProtectEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public Set<Integer> getProtectedSlots() {
        return ModConfig.getInstance().pickProtectSlots;
    }

    public boolean isSlotProtected(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return false;
        return ModConfig.getInstance().pickProtectSlots.contains(hotbarSlot);
    }

    public void toggleSlot(int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return;
        Set<Integer> set = ModConfig.getInstance().pickProtectSlots;
        if (set.contains(hotbarSlot)) {
            set.remove(hotbarSlot);
        } else {
            set.add(hotbarSlot);
        }
        ModConfig.getInstance().save();
    }
}
