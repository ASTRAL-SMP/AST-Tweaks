package com.astral.asttweaks.feature.autoeat;

import com.astral.asttweaks.config.ModConfig;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Configuration wrapper for auto-eat feature.
 * Delegates to main ModConfig for persistence.
 */
public class AutoEatConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().autoEatEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().autoEatEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public int getHungerThreshold() {
        return ModConfig.getInstance().autoEatHungerThreshold;
    }

    public void setHungerThreshold(int threshold) {
        ModConfig.getInstance().autoEatHungerThreshold = Math.max(0, Math.min(20, threshold));
        ModConfig.getInstance().save();
    }

    public boolean isEatWhileAction() {
        return ModConfig.getInstance().autoEatWhileAction;
    }

    public void setEatWhileAction(boolean eatWhileAction) {
        ModConfig.getInstance().autoEatWhileAction = eatWhileAction;
        ModConfig.getInstance().save();
    }

    public boolean isBlacklisted(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return ModConfig.getInstance().autoEatBlacklist.contains(id.toString());
    }

    public void addToBlacklist(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        ModConfig.getInstance().autoEatBlacklist.add(id.toString());
        ModConfig.getInstance().save();
    }

    public void removeFromBlacklist(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        ModConfig.getInstance().autoEatBlacklist.remove(id.toString());
        ModConfig.getInstance().save();
    }

    public void toggleBlacklist(Item item) {
        if (isBlacklisted(item)) {
            removeFromBlacklist(item);
        } else {
            addToBlacklist(item);
        }
    }

    public Set<String> getBlacklistedItems() {
        return ModConfig.getInstance().autoEatBlacklist;
    }
}
