package com.astral.asttweaks.feature.silktouchswitch;

import com.astral.asttweaks.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;

import java.util.Set;

/**
 * シルクタッチスイッチ機能の設定ラッパー。
 * ModConfigに永続化を委譲する。
 */
public class SilkTouchSwitchConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().silkTouchSwitchEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().silkTouchSwitchEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isInSilkTouchList(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        return ModConfig.getInstance().silkTouchSwitchBlockList.contains(id);
    }

    public void addToList(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        ModConfig.getInstance().silkTouchSwitchBlockList.add(id);
        ModConfig.getInstance().save();
    }

    public void removeFromList(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        ModConfig.getInstance().silkTouchSwitchBlockList.remove(id);
        ModConfig.getInstance().save();
    }

    public void toggleList(Block block) {
        if (isInSilkTouchList(block)) {
            removeFromList(block);
        } else {
            addToList(block);
        }
    }

    public Set<String> getBlockList() {
        return ModConfig.getInstance().silkTouchSwitchBlockList;
    }
}
