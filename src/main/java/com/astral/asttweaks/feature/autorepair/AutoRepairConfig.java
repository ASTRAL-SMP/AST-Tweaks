package com.astral.asttweaks.feature.autorepair;

import com.astral.asttweaks.config.ModConfig;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Configuration wrapper for the auto repair feature.
 */
public class AutoRepairConfig {

    /**
     * Check if the auto repair feature is enabled.
     */
    public boolean isEnabled() {
        return ModConfig.getInstance().autoRepairEnabled;
    }

    /**
     * Enable or disable the auto repair feature.
     */
    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().autoRepairEnabled = enabled;
        ModConfig.getInstance().save();
    }

    /**
     * Get the number of clicks per tick for fast use.
     */
    public int getClicksPerTick() {
        return ModConfig.getInstance().autoRepairClicksPerTick;
    }

    /**
     * Get the target hotbar slot for repairing items (0-8).
     */
    public int getTargetSlot() {
        return ModConfig.getInstance().autoRepairTargetSlot;
    }

    /**
     * Check if whitelist mode is enabled.
     */
    public boolean isWhitelistMode() {
        return ModConfig.getInstance().autoRepairWhitelistMode;
    }

    /**
     * Get the item list (for whitelist or blacklist).
     */
    public Set<String> getItemList() {
        return ModConfig.getInstance().autoRepairItemList;
    }

    /**
     * Check if an item is in the list (whitelist or blacklist).
     */
    public boolean isInList(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return getItemList().contains(id.toString());
    }

    /**
     * Check if an item should be repaired based on whitelist/blacklist mode.
     */
    public boolean shouldRepairItem(Item item) {
        boolean inList = isInList(item);
        if (isWhitelistMode()) {
            // Whitelist mode: only repair items in the list
            return inList;
        } else {
            // Blacklist mode: repair all items except those in the list
            return !inList;
        }
    }

    /**
     * Toggle an item in the list.
     */
    public void toggleItem(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        String idString = id.toString();
        Set<String> list = getItemList();
        if (list.contains(idString)) {
            list.remove(idString);
        } else {
            list.add(idString);
        }
        ModConfig.getInstance().save();
    }
}
