package com.astral.asttweaks.feature.massgrindstone;

import com.astral.asttweaks.config.ModConfig;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Configuration wrapper for the mass grindstone feature.
 */
public class MassGrindstoneConfig {

    /**
     * Check if the mass grindstone feature is enabled.
     */
    public boolean isEnabled() {
        return ModConfig.getInstance().massGrindstoneEnabled;
    }

    /**
     * Enable or disable the mass grindstone feature.
     */
    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().massGrindstoneEnabled = enabled;
        ModConfig.getInstance().save();
    }

    /**
     * Check if whitelist mode is enabled.
     * When true, only items in the list will be processed.
     * When false (blacklist mode), all items except those in the list will be processed.
     */
    public boolean isWhitelistMode() {
        return ModConfig.getInstance().massGrindstoneWhitelistMode;
    }

    /**
     * Get the item list (for whitelist or blacklist).
     */
    public Set<String> getItemList() {
        return ModConfig.getInstance().massGrindstoneItemList;
    }

    /**
     * Get the number of operations per tick.
     */
    public int getOperationsPerTick() {
        return ModConfig.getInstance().massGrindstoneOperationsPerTick;
    }

    /**
     * Check if results should be dropped (MassCraft style).
     */
    public boolean shouldDropResults() {
        return ModConfig.getInstance().massGrindstoneDropResults;
    }

    /**
     * Check if an item is in the list (whitelist or blacklist).
     */
    public boolean isInList(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return getItemList().contains(id.toString());
    }

    /**
     * Check if an item should be processed based on whitelist/blacklist mode.
     */
    public boolean shouldProcessItem(Item item) {
        boolean inList = isInList(item);
        if (isWhitelistMode()) {
            // Whitelist mode: only process items in the list
            return inList;
        } else {
            // Blacklist mode: process all items except those in the list
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
