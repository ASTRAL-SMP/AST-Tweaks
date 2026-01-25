package com.astral.asttweaks.feature.entityculling;

import com.astral.asttweaks.config.ModConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Configuration wrapper for entity culling feature.
 * Delegates to main ModConfig for persistence.
 */
public class EntityCullingConfig {

    public boolean isEnabled() {
        return ModConfig.getInstance().entityCullingEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().entityCullingEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isDisableAllEntities() {
        return ModConfig.getInstance().disableAllEntityRendering;
    }

    public void setDisableAllEntities(boolean disable) {
        ModConfig.getInstance().disableAllEntityRendering = disable;
        ModConfig.getInstance().save();
    }

    public boolean isDisableArmorStands() {
        return ModConfig.getInstance().disableArmorStandRendering;
    }

    public void setDisableArmorStands(boolean disable) {
        ModConfig.getInstance().disableArmorStandRendering = disable;
        ModConfig.getInstance().save();
    }

    public boolean isDisableFallingBlocks() {
        return ModConfig.getInstance().disableFallingBlockRendering;
    }

    public void setDisableFallingBlocks(boolean disable) {
        ModConfig.getInstance().disableFallingBlockRendering = disable;
        ModConfig.getInstance().save();
    }

    public boolean isDisableDeadMobs() {
        return ModConfig.getInstance().disableDeadMobRendering;
    }

    public void setDisableDeadMobs(boolean disable) {
        ModConfig.getInstance().disableDeadMobRendering = disable;
        ModConfig.getInstance().save();
    }

    public int getItemRenderLimit() {
        return ModConfig.getInstance().itemRenderLimit;
    }

    public void setItemRenderLimit(int limit) {
        ModConfig.getInstance().itemRenderLimit = limit;
        ModConfig.getInstance().save();
    }

    public int getXpOrbRenderLimit() {
        return ModConfig.getInstance().xpOrbRenderLimit;
    }

    public void setXpOrbRenderLimit(int limit) {
        ModConfig.getInstance().xpOrbRenderLimit = limit;
        ModConfig.getInstance().save();
    }

    // Entity blacklist methods
    public boolean isEntityBlacklisted(EntityType<?> entityType) {
        Identifier id = Registries.ENTITY_TYPE.getId(entityType);
        return ModConfig.getInstance().entityBlacklist.contains(id.toString());
    }

    public boolean isEntityBlacklisted(String entityId) {
        return ModConfig.getInstance().entityBlacklist.contains(entityId);
    }

    public void addToEntityBlacklist(EntityType<?> entityType) {
        Identifier id = Registries.ENTITY_TYPE.getId(entityType);
        ModConfig.getInstance().entityBlacklist.add(id.toString());
        ModConfig.getInstance().save();
    }

    public void removeFromEntityBlacklist(EntityType<?> entityType) {
        Identifier id = Registries.ENTITY_TYPE.getId(entityType);
        ModConfig.getInstance().entityBlacklist.remove(id.toString());
        ModConfig.getInstance().save();
    }

    public void toggleEntityBlacklist(EntityType<?> entityType) {
        if (isEntityBlacklisted(entityType)) {
            removeFromEntityBlacklist(entityType);
        } else {
            addToEntityBlacklist(entityType);
        }
    }

    public Set<String> getBlacklistedEntities() {
        return ModConfig.getInstance().entityBlacklist;
    }

    // Item entity blacklist methods
    public boolean isItemBlacklisted(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        return ModConfig.getInstance().itemEntityBlacklist.contains(id.toString());
    }

    public boolean isItemBlacklisted(String itemId) {
        return ModConfig.getInstance().itemEntityBlacklist.contains(itemId);
    }

    public void addToItemBlacklist(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        ModConfig.getInstance().itemEntityBlacklist.add(id.toString());
        ModConfig.getInstance().save();
    }

    public void removeFromItemBlacklist(Item item) {
        Identifier id = Registries.ITEM.getId(item);
        ModConfig.getInstance().itemEntityBlacklist.remove(id.toString());
        ModConfig.getInstance().save();
    }

    public void toggleItemBlacklist(Item item) {
        if (isItemBlacklisted(item)) {
            removeFromItemBlacklist(item);
        } else {
            addToItemBlacklist(item);
        }
    }

    public Set<String> getBlacklistedItems() {
        return ModConfig.getInstance().itemEntityBlacklist;
    }
}
