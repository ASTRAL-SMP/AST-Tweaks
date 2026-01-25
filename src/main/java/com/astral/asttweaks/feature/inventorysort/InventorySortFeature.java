package com.astral.asttweaks.feature.inventorysort;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.KeyBindings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Simple and fast inventory sort feature.
 */
public class InventorySortFeature implements Feature {
    private final InventorySortConfig config;
    private boolean keyWasPressed = false;

    public InventorySortFeature() {
        this.config = new InventorySortConfig();
    }

    @Override
    public String getId() { return "inventorysort"; }

    @Override
    public String getName() { return "Inventory Sort"; }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("Inventory Sort feature initialized");
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) return;
        if (client.player == null || client.world == null) return;

        if (client.currentScreen instanceof HandledScreen) {
            boolean keyPressed = isKeyPressed(client);
            if (keyPressed && !keyWasPressed) {
                performSort();
            }
            keyWasPressed = keyPressed;
        } else {
            keyWasPressed = false;
        }
    }

    private boolean isKeyPressed(MinecraftClient client) {
        if (client.getWindow() == null) return false;
        InputUtil.Key key = KeyBindings.inventorySortExecute.boundKey;
        if (key.getCode() == InputUtil.UNKNOWN_KEY.getCode()) return false;
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), key.getCode());
    }

    @Override public void tick() {}
    @Override public boolean isEnabled() { return config.isEnabled(); }
    @Override public void setEnabled(boolean enabled) { config.setEnabled(enabled); }
    public InventorySortConfig getConfig() { return config; }
    public boolean isSorting() { return false; } // Instant sort, no ongoing state

    public void performSort() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!canSort(client)) return;

        ScreenHandler handler = client.player.currentScreenHandler;
        int playerInvStart = findPlayerInventoryStart(handler);
        int containerSlots = getContainerSlotCount(handler, playerInvStart);

        SortTarget target = config.getSortTarget();
        boolean hasContainer = containerSlots > 0;

        if (!hasContainer) {
            sortPlayerInventory(client, playerInvStart);
        } else {
            switch (target) {
                case PLAYER_ONLY -> sortPlayerInventory(client, playerInvStart);
                case CONTAINER_ONLY -> sortContainer(client, containerSlots);
                case BOTH -> {
                    sortContainer(client, containerSlots);
                    sortPlayerInventory(client, playerInvStart);
                }
            }
        }

        client.player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".inventorysort.completed"), true);
    }

    public void performPlayerSort() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!canSort(client)) return;

        ScreenHandler handler = client.player.currentScreenHandler;
        int playerInvStart = findPlayerInventoryStart(handler);

        sortPlayerInventory(client, playerInvStart);
        client.player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".inventorysort.completed"), true);
    }

    public void performContainerSort() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!canSort(client)) return;

        ScreenHandler handler = client.player.currentScreenHandler;
        int playerInvStart = findPlayerInventoryStart(handler);
        int containerSlots = getContainerSlotCount(handler, playerInvStart);

        if (containerSlots <= 0) return;

        sortContainer(client, containerSlots);
        client.player.sendMessage(Text.translatable("message." + ASTTweaks.MOD_ID + ".inventorysort.completed"), true);
    }

    private boolean canSort(MinecraftClient client) {
        return config.isEnabled() &&
               client.player != null &&
               client.interactionManager != null &&
               client.currentScreen instanceof HandledScreen;
    }

    /**
     * Sort player inventory instantly.
     * @param playerInvStart the screen slot index where player inventory starts (main inventory, not hotbar)
     */
    private void sortPlayerInventory(MinecraftClient client, int playerInvStart) {
        PlayerInventory inv = client.player.getInventory();
        ScreenHandler handler = client.player.currentScreenHandler;
        SortRange range = config.getSortRange();

        // Collect sortable slots (excluding excluded slots)
        List<Integer> slots = new ArrayList<>();
        for (int i = range.getStartSlot(); i <= range.getEndSlot(); i++) {
            if (!config.isSlotExcluded(i)) {
                slots.add(i);
            }
        }

        if (slots.isEmpty()) return;

        // Step 1: Physically merge partial stacks within target slots
        mergePartialStacks(client, handler, slots, playerInvStart, false);

        // Step 2: Re-collect items after merge
        List<SortEntry> entries = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isEmpty()) {
                entries.add(new SortEntry(slot, stack.copy()));
            }
        }

        if (entries.isEmpty()) return;

        // Step 3: Sort entries
        sortEntries(entries);

        // Step 4: Execute sort (simple swap-based)
        executeSort(client, handler, entries, slots, playerInvStart, false);
    }

    /**
     * Sort container instantly.
     */
    private void sortContainer(MinecraftClient client, int containerSlots) {
        ScreenHandler handler = client.player.currentScreenHandler;

        // Collect all container slots
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < containerSlots; i++) {
            slots.add(i);
        }

        if (slots.isEmpty()) return;

        // Step 1: Physically merge partial stacks
        mergePartialStacks(client, handler, slots, 0, true);

        // Step 2: Re-collect items after merge
        List<SortEntry> entries = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = handler.getSlot(slot).getStack();
            if (!stack.isEmpty()) {
                entries.add(new SortEntry(slot, stack.copy()));
            }
        }

        if (entries.isEmpty()) return;

        // Step 3: Sort entries
        sortEntries(entries);

        // Step 4: Execute sort
        executeSort(client, handler, entries, slots, containerSlots, true);
    }

    /**
     * Sort entries by configured mode.
     */
    private void sortEntries(List<SortEntry> entries) {
        SortMode mode = config.getSortMode();

        Comparator<SortEntry> cmp = switch (mode) {
            case ITEM_ID -> Comparator.comparing(e -> Registries.ITEM.getId(e.stack.getItem()).toString());
            case ITEM_NAME -> Comparator.comparing(e -> e.stack.getName().getString());
            case CATEGORY -> Comparator.<SortEntry>comparingInt(e -> getCategory(e.stack.getItem()))
                    .thenComparing(e -> Registries.ITEM.getId(e.stack.getItem()).toString());
            case STACK_COUNT -> Comparator.<SortEntry>comparingInt(e -> -e.stack.getCount())
                    .thenComparing(e -> Registries.ITEM.getId(e.stack.getItem()).toString());
        };

        entries.sort(cmp);
    }

    private int getCategory(Item item) {
        if (item instanceof SwordItem || item instanceof AxeItem) return 0;
        if (item instanceof ToolItem) return 1;
        if (item instanceof ArmorItem) return 2;
        if (item.getFoodComponent() != null) return 3;
        if (item instanceof BlockItem) return 4;
        return 5;
    }

    /**
     * Execute the sort by swapping items to their target positions.
     * @param playerInvStart for player inventory: screen slot where main inventory starts; for container: unused
     */
    private void executeSort(MinecraftClient client, ScreenHandler handler,
                              List<SortEntry> sortedEntries, List<Integer> availableSlots,
                              int playerInvStart, boolean isContainer) {
        int syncId = handler.syncId;

        // Build current state: which item index is at which slot
        // itemLocations[i] = current slot of item i
        int[] itemLocations = new int[sortedEntries.size()];
        for (int i = 0; i < sortedEntries.size(); i++) {
            itemLocations[i] = sortedEntries.get(i).originalSlot;
        }

        // For each target position, move the correct item there
        for (int targetIdx = 0; targetIdx < sortedEntries.size() && targetIdx < availableSlots.size(); targetIdx++) {
            int targetSlot = availableSlots.get(targetIdx);
            int currentSlot = itemLocations[targetIdx];

            if (currentSlot == targetSlot) continue; // Already in place

            // Find if there's an item at the target slot
            int itemAtTarget = -1;
            for (int i = targetIdx + 1; i < itemLocations.length; i++) {
                if (itemLocations[i] == targetSlot) {
                    itemAtTarget = i;
                    break;
                }
            }

            // Convert to screen slots
            int screenCurrent = isContainer ? currentSlot : toScreenSlot(currentSlot, playerInvStart);
            int screenTarget = isContainer ? targetSlot : toScreenSlot(targetSlot, playerInvStart);

            // Swap using pickup operations
            click(client, syncId, screenCurrent); // Pick up item from current
            click(client, syncId, screenTarget);  // Swap with target (or place if empty)

            // If target had an item, we now have it on cursor, put it back
            if (itemAtTarget >= 0) {
                click(client, syncId, screenCurrent); // Put swapped item at original location
                itemLocations[itemAtTarget] = currentSlot;
            }

            itemLocations[targetIdx] = targetSlot;
        }
    }

    /**
     * Physically merge partial stacks of the same item type by clicking.
     * This performs actual inventory operations to combine items.
     */
    private void mergePartialStacks(MinecraftClient client, ScreenHandler handler,
                                     List<Integer> slots, int playerInvStart, boolean isContainer) {
        PlayerInventory inv = client.player.getInventory();
        int syncId = handler.syncId;

        // Keep merging until no more merges are possible
        boolean merged;
        int maxIterations = 100; // Safety limit
        int iterations = 0;

        do {
            merged = false;
            iterations++;

            // Group partial stacks by item identity
            Map<String, List<Integer>> partialStacks = new LinkedHashMap<>();
            for (int slot : slots) {
                ItemStack stack = isContainer ?
                        handler.getSlot(slot).getStack() :
                        inv.getStack(slot);

                if (!stack.isEmpty() && stack.getCount() < stack.getMaxCount()) {
                    String key = getItemIdentityKey(stack);
                    partialStacks.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
                }
            }

            // For each item type with multiple partial stacks, merge one pair
            for (List<Integer> slotList : partialStacks.values()) {
                if (slotList.size() < 2) continue;

                // Find the best target (most filled) and source (any other partial)
                int targetSlot = slotList.get(0);
                int sourceSlot = slotList.get(1);

                int screenSource = isContainer ? sourceSlot : toScreenSlot(sourceSlot, playerInvStart);
                int screenTarget = isContainer ? targetSlot : toScreenSlot(targetSlot, playerInvStart);

                // Pick up source
                click(client, syncId, screenSource);
                // Drop onto target (will auto-merge same items)
                click(client, syncId, screenTarget);
                // If there's overflow on cursor, put it back at source
                click(client, syncId, screenSource);

                merged = true;
                break; // Process one merge per iteration to keep inventory state consistent
            }
        } while (merged && iterations < maxIterations);
    }

    /**
     * Generate a unique identity key for an item stack.
     * Items with the same key can be merged.
     */
    private String getItemIdentityKey(ItemStack stack) {
        String key = Registries.ITEM.getId(stack.getItem()).toString();
        if (stack.hasNbt()) {
            key += "|" + stack.getNbt().toString();
        }
        return key;
    }

    /**
     * Find the screen slot index where the player's main inventory starts.
     * This correctly handles both container screens and player inventory screen.
     */
    private int findPlayerInventoryStart(ScreenHandler handler) {
        // Find the first slot that belongs to PlayerInventory and is a main inventory slot (index 9-35)
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.inventory instanceof PlayerInventory) {
                int invIndex = slot.getIndex();
                // Main inventory slots are 9-35 in PlayerInventory
                if (invIndex >= 9 && invIndex <= 35) {
                    // This screen slot corresponds to main inventory slot 9
                    // So playerInvStart = i - (invIndex - 9)
                    return i - (invIndex - 9);
                }
            }
        }
        // Fallback: shouldn't happen
        return handler.slots.size() - 36;
    }

    /**
     * Get the number of container slots (slots before player inventory).
     * Returns 0 if this is a player inventory screen with no external container.
     */
    private int getContainerSlotCount(ScreenHandler handler, int playerInvStart) {
        // Count slots before player inventory that are NOT part of PlayerInventory
        int count = 0;
        for (int i = 0; i < playerInvStart; i++) {
            Slot slot = handler.getSlot(i);
            if (!(slot.inventory instanceof PlayerInventory)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Convert player inventory slot to screen slot.
     * PlayerInventory: 0-8 = hotbar, 9-35 = main
     * Screen: playerInvStart to +26 = main, playerInvStart+27 to +35 = hotbar
     * @param playerInvStart the screen slot where main inventory starts
     */
    private int toScreenSlot(int invSlot, int playerInvStart) {
        if (invSlot < 9) {
            // Hotbar: comes after main inventory (27 slots)
            return playerInvStart + 27 + invSlot;
        } else {
            // Main inventory: starts at playerInvStart
            return playerInvStart + (invSlot - 9);
        }
    }

    /**
     * Perform a left click on a slot.
     */
    private void click(MinecraftClient client, int syncId, int slot) {
        client.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, client.player);
    }

    private static class SortEntry {
        final int originalSlot;
        final ItemStack stack;

        SortEntry(int slot, ItemStack stack) {
            this.originalSlot = slot;
            this.stack = stack;
        }
    }
}
