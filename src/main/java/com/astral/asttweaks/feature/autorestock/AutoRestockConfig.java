package com.astral.asttweaks.feature.autorestock;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.util.VanillaItemData;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Dynamic;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Configuration wrapper for the Auto Restock feature.
 */
public class AutoRestockConfig {
    public static final int OFFHAND_SLOT = 40;
    // Data version of Minecraft 1.19.4, which the previous builds of this mod targeted
    private static final int LEGACY_DATA_VERSION = 3337;

    private static boolean legacyEntriesMigrated = false;
    private static WeakReference<RegistryAccess> canonicalizedFor = new WeakReference<>(null);

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

    public ContainerPickupOrder getContainerPickupOrder() {
        ContainerPickupOrder order = ModConfig.getInstance().autoRestockContainerPickupOrder;
        return order != null ? order : ContainerPickupOrder.FIRST_SLOT;
    }

    public Set<Integer> getProtectedSlots() {
        return ModConfig.getInstance().autoRestockProtectedSlots;
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

    public List<AutoRestockEntry> getEntries() {
        if (ModConfig.getInstance().autoRestockEntries == null) {
            ModConfig.getInstance().autoRestockEntries = new ArrayList<>();
        }
        List<AutoRestockEntry> entries = ModConfig.getInstance().autoRestockEntries;
        if (!legacyEntriesMigrated) {
            legacyEntriesMigrated = true;
            if (migrateLegacyEntries(entries)) {
                ModConfig.getInstance().save();
            }
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && canonicalizedFor.get() != client.level.registryAccess()) {
            canonicalizedFor = new WeakReference<>(client.level.registryAccess());
            if (canonicalizeEntries(entries)) {
                ModConfig.getInstance().save();
            }
        }
        return entries;
    }

    /**
     * Entries saved by the 1.19.4 builds hold the item's raw NBT ("tag"). Runs them through
     * the vanilla DataFixer to get the data-component form this version stores.
     */
    private static boolean migrateLegacyEntries(List<AutoRestockEntry> entries) {
        boolean changed = false;
        for (AutoRestockEntry entry : entries) {
            if (entry == null || entry.itemId == null || entry.nbt == null || entry.nbt.isBlank()) {
                continue;
            }
            CompoundTag legacyTag;
            try {
                legacyTag = TagParser.parseCompoundFully(entry.nbt);
            } catch (CommandSyntaxException e) {
                continue;
            }
            if (!isLegacyItemTag(legacyTag)) {
                continue;
            }

            CompoundTag legacyStack = new CompoundTag();
            legacyStack.putString("id", entry.itemId);
            legacyStack.putByte("Count", (byte) 1);
            legacyStack.put("tag", legacyTag);
            try {
                Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
                        new Dynamic<>(NbtOps.INSTANCE, legacyStack),
                        LEGACY_DATA_VERSION, SharedConstants.getCurrentVersion().dataVersion().version());
                if (!(fixed.getValue() instanceof CompoundTag stack)) {
                    continue;
                }
                CompoundTag components = stack.getCompoundOrEmpty("components").copy();
                components.remove("minecraft:damage");
                entry.itemId = stack.getStringOr("id", entry.itemId);
                entry.nbt = components.isEmpty() ? "" : components.toString();
                changed = true;
                ASTTweaks.LOGGER.info("Auto Restock: migrated 1.19.4 entry {} to {}", legacyStack, entry.nbt);
            } catch (RuntimeException e) {
                ASTTweaks.LOGGER.warn("Auto Restock: failed to migrate 1.19.4 entry {}", legacyStack, e);
            }
        }
        return changed;
    }

    // Component patch keys are namespaced ("minecraft:enchantments", "!minecraft:food");
    // 1.19.4 item NBT keys are not ("Enchantments", "display").
    private static boolean isLegacyItemTag(CompoundTag tag) {
        for (String key : tag.keySet()) {
            if (key.indexOf(':') < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Re-encodes stored patches the same way {@link #getStackKey} does, so entries coming from
     * the DataFixer (or older codec versions) keep matching. Needs the world's registries.
     */
    private boolean canonicalizeEntries(List<AutoRestockEntry> entries) {
        boolean changed = false;
        for (AutoRestockEntry entry : entries) {
            if (entry == null || entry.nbt == null || entry.nbt.isBlank()) {
                continue;
            }
            ItemStack stack = parseConfiguredStack(entry);
            if (stack == null) {
                continue;
            }
            String canonical = normalizeNbt(stack);
            if (!canonical.equals(entry.nbt)) {
                entry.nbt = canonical;
                changed = true;
            }
        }
        return changed;
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
            return clampDesiredCount(stack, stack.getMaxStackSize());
        }
        return clampDesiredCount(stack, entry.desiredCount);
    }

    public void addStack(ItemStack stack) {
        if (stack.isEmpty() || isStackConfigured(stack)) {
            return;
        }

        getEntries().add(new AutoRestockEntry(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                normalizeNbt(stack),
                List.of(findNextDefaultSlot()),
                clampDesiredCount(stack, stack.getMaxStackSize())));
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
        Item item = resolveItem(entry);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = parseConfiguredStack(entry);
        return stack != null ? stack : new ItemStack(item);
    }

    private static Item resolveItem(AutoRestockEntry entry) {
        Identifier identifier = Identifier.tryParse(entry.itemId);
        if (identifier == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        if (item == Items.AIR && !"minecraft:air".equals(entry.itemId)) {
            return null;
        }
        return item;
    }

    /**
     * The entry's stack with its stored components applied, or null if the item or the
     * components can't be resolved.
     */
    private static ItemStack parseConfiguredStack(AutoRestockEntry entry) {
        Item item = resolveItem(entry);
        if (item == null) {
            return null;
        }
        ItemStack stack = new ItemStack(item);
        if (entry.nbt == null || entry.nbt.isBlank()) {
            return stack;
        }
        try {
            Tag tag = TagParser.parseCompoundFully(entry.nbt);
            Optional<DataComponentPatch> patch = DataComponentPatch.CODEC.parse(registryOps(), tag).result();
            if (patch.isEmpty()) {
                return null;
            }
            stack.applyComponents(patch.get());
            return stack;
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    public static String getStackKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + normalizeNbt(stack);
    }

    public static boolean matches(ItemStack expected, ItemStack actual) {
        return !expected.isEmpty()
                && !actual.isEmpty()
                && expected.is(actual.getItem())
                && normalizeNbt(expected).equals(normalizeNbt(actual));
    }

    public static FeatureFlagSet getDefaultEnabledFeatures() {
        return FeatureFlags.VANILLA_SET;
    }

    public static RegistryAccess getDefaultRegistryLookup() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) {
            return client.level.registryAccess();
        }
        RegistryAccess vanilla = VanillaItemData.registries();
        return vanilla != null ? vanilla : RegistryAccess.EMPTY;
    }

    private static RegistryOps<Tag> registryOps() {
        return getDefaultRegistryLookup().createSerializationContext(NbtOps.INSTANCE);
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
        DataComponentPatch patch = stack.getComponentsPatch().forget(type -> type == DataComponents.DAMAGE);
        if (patch.isEmpty()) {
            return "";
        }
        return DataComponentPatch.CODEC.encodeStart(registryOps(), patch).result()
                .map(Tag::toString)
                .orElse("");
    }
}
