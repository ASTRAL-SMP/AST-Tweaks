package com.astral.asttweaks.feature.autorestock;

import com.astral.asttweaks.config.ModConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration wrapper for the Auto Restock feature.
 */
public class AutoRestockConfig {
    public static final int OFFHAND_SLOT = 40;

    public boolean isEnabled() {
        return ModConfig.getInstance().autoRestockEnabled;
    }

    public void setEnabled(boolean enabled) {
        ModConfig.getInstance().autoRestockEnabled = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isInventoryRestockEnabled() {
        return ModConfig.getInstance().autoRestockFromInventory;
    }

    public void setInventoryRestockEnabled(boolean enabled) {
        ModConfig.getInstance().autoRestockFromInventory = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isShulkerRestockEnabled() {
        return ModConfig.getInstance().autoRestockFromShulker;
    }

    public void setShulkerRestockEnabled(boolean enabled) {
        ModConfig.getInstance().autoRestockFromShulker = enabled;
        ModConfig.getInstance().save();
    }

    public boolean isContainerRestockEnabled() {
        return isShulkerRestockEnabled();
    }

    public void setContainerRestockEnabled(boolean enabled) {
        setShulkerRestockEnabled(enabled);
    }

    public boolean shouldPrioritizeOverExternalAutoCollect() {
        return ModConfig.getInstance().autoRestockPreferOverExternalAutoCollect;
    }

    public void setPrioritizeOverExternalAutoCollect(boolean enabled) {
        ModConfig.getInstance().autoRestockPreferOverExternalAutoCollect = enabled;
        ModConfig.getInstance().save();
    }

    public int getOperationsPerTick() {
        return Math.max(1, ModConfig.getInstance().autoRestockOperationsPerTick);
    }

    public List<AutoRestockEntry> getEntries() {
        if (ModConfig.getInstance().autoRestockEntries == null) {
            ModConfig.getInstance().autoRestockEntries = new ArrayList<>();
        }
        return ModConfig.getInstance().autoRestockEntries;
    }

    public AutoRestockEntry getEntry(ItemStack stack) {
        String key = getStackKey(stack);
        for (AutoRestockEntry entry : getEntries()) {
            if (key.equals(getEntryKey(entry))) {
                return entry;
            }
        }
        return null;
    }

    public boolean isStackConfigured(ItemStack stack) {
        return getEntry(stack) != null;
    }

    public List<Integer> getTargetSlots(ItemStack stack) {
        AutoRestockEntry entry = getEntry(stack);
        return entry != null ? normalizeTargetSlots(entry) : List.of();
    }

    public List<Integer> getTargetSlots(AutoRestockEntry entry) {
        return entry != null ? normalizeTargetSlots(entry) : List.of();
    }

    public int getPrimaryTargetSlot(ItemStack stack) {
        List<Integer> targetSlots = getTargetSlots(stack);
        return targetSlots.isEmpty() ? 0 : targetSlots.get(0);
    }

    public boolean isTargetSlotSelected(ItemStack stack, int targetSlot) {
        return getTargetSlots(stack).contains(sanitizeTargetSlot(targetSlot));
    }

    public int getDesiredCount(ItemStack stack) {
        AutoRestockEntry entry = getEntry(stack);
        if (entry == null) {
            return clampDesiredCount(stack, stack.getMaxCount());
        }
        return clampDesiredCount(stack, entry.desiredCount);
    }

    public void addStack(ItemStack stack) {
        if (stack.isEmpty() || isStackConfigured(stack)) {
            return;
        }

        getEntries().add(new AutoRestockEntry(
                Registries.ITEM.getId(stack.getItem()).toString(),
                normalizeNbt(stack),
                List.of(findNextDefaultSlot()),
                clampDesiredCount(stack, stack.getMaxCount())));
        ModConfig.getInstance().save();
    }

    public void removeStack(ItemStack stack) {
        String key = getStackKey(stack);
        getEntries().removeIf(entry -> key.equals(getEntryKey(entry)));
        ModConfig.getInstance().save();
    }

    public void toggleTargetSlot(ItemStack stack, int targetSlot) {
        AutoRestockEntry entry = getEntry(stack);
        if (entry == null) {
            return;
        }

        int sanitizedSlot = sanitizeTargetSlot(targetSlot);
        List<Integer> targetSlots = normalizeTargetSlots(entry);

        if (targetSlots.contains(sanitizedSlot)) {
            targetSlots.remove((Integer) sanitizedSlot);
        } else {
            targetSlots.add(sanitizedSlot);
        }

        entry.targetSlots = new ArrayList<>(targetSlots);
        entry.targetSlot = targetSlots.isEmpty() ? null : targetSlots.get(0);
        ModConfig.getInstance().save();
    }

    public int setDesiredCount(ItemStack stack, int desiredCount) {
        AutoRestockEntry entry = getEntry(stack);
        if (entry == null) {
            return clampDesiredCount(stack, desiredCount);
        }

        entry.desiredCount = clampDesiredCount(stack, desiredCount);
        ModConfig.getInstance().save();
        return entry.desiredCount;
    }

    public ItemStack getConfiguredStack(AutoRestockEntry entry) {
        Identifier identifier = Identifier.tryParse(entry.itemId);
        if (identifier == null) {
            return ItemStack.EMPTY;
        }

        Item item = Registries.ITEM.get(identifier);
        if (item == Items.AIR && !"minecraft:air".equals(entry.itemId)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        if (entry.nbt != null && !entry.nbt.isBlank()) {
            try {
                stack.setNbt(StringNbtReader.parse(entry.nbt));
            } catch (CommandSyntaxException ignored) {
                return new ItemStack(item);
            }
        }
        return stack;
    }

    public static String getStackKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        return Registries.ITEM.getId(stack.getItem()) + "|" + normalizeNbt(stack);
    }

    public static boolean matches(ItemStack expected, ItemStack actual) {
        return !expected.isEmpty()
                && !actual.isEmpty()
                && expected.isOf(actual.getItem())
                && normalizeNbt(expected).equals(normalizeNbt(actual));
    }

    public static FeatureSet getDefaultEnabledFeatures() {
        return FeatureFlags.VANILLA_FEATURES;
    }

    public static DynamicRegistryManager getDefaultRegistryLookup() {
        return DynamicRegistryManager.EMPTY;
    }

    private String getEntryKey(AutoRestockEntry entry) {
        return entry.itemId + "|" + (entry.nbt != null ? entry.nbt : "");
    }

    private int findNextDefaultSlot() {
        boolean[] used = new boolean[41];
        for (AutoRestockEntry entry : getEntries()) {
            for (int slot : normalizeTargetSlots(entry)) {
                if (slot >= 0 && slot <= 35) {
                    used[slot] = true;
                } else if (slot == OFFHAND_SLOT) {
                    used[OFFHAND_SLOT] = true;
                }
            }
        }

        for (int slot = 0; slot <= 8; slot++) {
            if (!used[slot]) {
                return slot;
            }
        }

        for (int slot = 9; slot <= 35; slot++) {
            if (!used[slot]) {
                return slot;
            }
        }

        return 0;
    }

    private int sanitizeTargetSlot(int slot) {
        if ((slot >= 0 && slot <= 35) || slot == OFFHAND_SLOT) {
            return slot;
        }
        return 0;
    }

    private List<Integer> normalizeTargetSlots(AutoRestockEntry entry) {
        Set<Integer> uniqueSlots = new LinkedHashSet<>();

        if (entry.targetSlots != null) {
            for (Integer slot : entry.targetSlots) {
                if (slot != null) {
                    uniqueSlots.add(sanitizeTargetSlot(slot));
                }
            }
        }

        if (uniqueSlots.isEmpty() && entry.targetSlot != null) {
            uniqueSlots.add(sanitizeTargetSlot(entry.targetSlot));
        }

        List<Integer> normalized = new ArrayList<>(uniqueSlots);
        entry.targetSlots = new ArrayList<>(normalized);
        entry.targetSlot = normalized.isEmpty() ? null : normalized.get(0);
        return normalized;
    }

    private int clampDesiredCount(ItemStack stack, int desiredCount) {
        return Math.max(1, desiredCount);
    }

    private static String normalizeNbt(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            return "";
        }

        NbtCompound normalized = nbt.copy();
        normalized.remove(ItemStack.DAMAGE_KEY);
        return normalized.isEmpty() ? "" : normalized.toString();
    }
}
