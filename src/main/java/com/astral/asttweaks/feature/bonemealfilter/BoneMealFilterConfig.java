package com.astral.asttweaks.feature.bonemealfilter;

import com.astral.asttweaks.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

import java.util.Set;

/**
 * Configuration wrapper for bone meal filter feature.
 * Delegates to main ModConfig for persistence.
 */
public class BoneMealFilterConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().boneMealFilterEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().boneMealFilterEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isWhitelisted(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        return ModConfig.getInstance().boneMealFilterWhitelist.contains(id);
    }

    public void addToWhitelist(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        ModConfig.getInstance().boneMealFilterWhitelist.add(id);
        ModConfig.getInstance().save();
    }

    public void removeFromWhitelist(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        ModConfig.getInstance().boneMealFilterWhitelist.remove(id);
        ModConfig.getInstance().save();
    }

    public void toggleWhitelist(Block block) {
        if (isWhitelisted(block)) {
            removeFromWhitelist(block);
        } else {
            addToWhitelist(block);
        }
    }

    public Set<String> getWhitelistedBlocks() {
        return ModConfig.getInstance().boneMealFilterWhitelist;
    }
}
