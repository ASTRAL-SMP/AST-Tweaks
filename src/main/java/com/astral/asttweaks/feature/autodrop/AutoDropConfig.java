package com.astral.asttweaks.feature.autodrop;

import com.astral.asttweaks.config.ModConfig;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Configuration wrapper for the Auto Drop feature.
 */
public class AutoDropConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().autoDropEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().autoDropEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public AutoDropMode getMode() {
        AutoDropMode mode = ModConfig.getInstance().autoDropMode;
        return mode != null ? mode : AutoDropMode.EXECUTE_KEY;
    }

    public int getOperationsPerTick() {
        return ModConfig.getInstance().autoDropOperationsPerTick;
    }

    public Set<Integer> getProtectedSlots() {
        return ModConfig.getInstance().autoDropProtectedSlots;
    }

    public boolean isSlotProtected(int invIndex) {
        return getProtectedSlots().contains(invIndex);
    }

    public void toggleSlotProtection(int invIndex) {
        Set<Integer> set = getProtectedSlots();
        if (set.contains(invIndex)) {
            set.remove(invIndex);
        } else {
            set.add(invIndex);
        }
        ModConfig.getInstance().save();
    }

    public Set<String> getExcludedItems() {
        return ModConfig.getInstance().autoDropExcludedItems;
    }

    public boolean isItemExcluded(String itemId) {
        return getExcludedItems().contains(itemId);
    }

    public boolean isItemExcluded(Item item) {
        return isItemExcluded(BuiltInRegistries.ITEM.getKey(item).toString());
    }

    public void toggleItemExclusion(Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        Set<String> set = getExcludedItems();
        if (set.contains(id)) {
            set.remove(id);
        } else {
            set.add(id);
        }
        ModConfig.getInstance().save();
    }
}
