package com.astral.asttweaks.feature.inventorysort;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.*;

/**
 * Simple and fast inventory sort feature.
 */
public class InventorySortFeature implements Feature {
    private static final int LARGE_CONTAINER_SLOTS = 54;
    private static final int QUEUED_CLICKS_PER_TICK = 4;

    private final InventorySortConfig config;
    private final Queue<Integer> queuedClicks = new ArrayDeque<>();
    private boolean keyWasPressed = false;
    private boolean containerKeyWasPressed = false;
    private boolean sorting = false;
    private int queuedSyncId = -1;

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

    private void onClientTick(Minecraft client) {
        if (!config.isEnabled()) return;
        if (client.player == null || client.level == null) return;

        if (client.screen instanceof AbstractContainerScreen) {
            if (processQueuedSort(client)) {
                keyWasPressed = isKeyPressed(client);
                containerKeyWasPressed = isContainerKeyPressed(client);
                return;
            }

            boolean keyPressed = isKeyPressed(client);
            if (keyPressed && !keyWasPressed) {
                performSort();
            }
            keyWasPressed = keyPressed;

            boolean containerKeyPressed = isContainerKeyPressed(client);
            if (containerKeyPressed && !containerKeyWasPressed) {
                performContainerSort();
            }
            containerKeyWasPressed = containerKeyPressed;
        } else {
            clearQueuedSort();
            keyWasPressed = false;
            containerKeyWasPressed = false;
        }
    }

    private boolean isKeyPressed(Minecraft client) {
        if (client.getWindow() == null) return false;
        long windowHandle = client.getWindow().handle();
        return com.astral.asttweaks.config.ModConfig.getInstance().inventorySortExecuteKey.isPressed(windowHandle);
    }

    private boolean isContainerKeyPressed(Minecraft client) {
        if (client.getWindow() == null) return false;
        long windowHandle = client.getWindow().handle();
        return com.astral.asttweaks.config.ModConfig.getInstance().inventorySortContainerExecuteKey.isPressed(windowHandle);
    }

    @Override public void tick() {}
    @Override public boolean isEnabled() { return config.isEnabled(); }
    @Override public void setEnabled(boolean enabled) { config.setEnabled(enabled); }
    public InventorySortConfig getConfig() { return config; }
    public boolean isSorting() { return sorting; }

    public void performSort() {
        Minecraft client = Minecraft.getInstance();
        if (!canSort(client)) return;
        if (sorting) return;

        AbstractContainerMenu handler = client.player.containerMenu;
        int playerInvStart = findPlayerInventoryStart(handler);
        int containerSlots = getContainerSlotCount(handler, playerInvStart);

        SortTarget target = config.getSortTarget();
        boolean hasContainer = containerSlots > 0;
        boolean queued = false;

        if (!hasContainer) {
            sortPlayerInventory(client, playerInvStart);
        } else {
            switch (target) {
                case PLAYER_ONLY -> sortPlayerInventory(client, playerInvStart);
                case CONTAINER_ONLY -> queued = sortContainer(client, containerSlots);
                case BOTH -> {
                    sortPlayerInventory(client, playerInvStart);
                    queued = sortContainer(client, containerSlots);
                }
            }
        }

        if (!queued) {
            client.player.sendOverlayMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".inventorysort.completed"));
        }
    }

    public void performPlayerSort() {
        Minecraft client = Minecraft.getInstance();
        if (!canSort(client)) return;
        if (sorting) return;

        AbstractContainerMenu handler = client.player.containerMenu;
        int playerInvStart = findPlayerInventoryStart(handler);

        sortPlayerInventory(client, playerInvStart);
        client.player.sendOverlayMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".inventorysort.completed"));
    }

    public void performContainerSort() {
        Minecraft client = Minecraft.getInstance();
        if (!canSort(client)) return;
        if (sorting) return;

        AbstractContainerMenu handler = client.player.containerMenu;
        int playerInvStart = findPlayerInventoryStart(handler);
        int containerSlots = getContainerSlotCount(handler, playerInvStart);

        if (containerSlots <= 0) return;

        boolean queued = sortContainer(client, containerSlots);
        if (!queued) {
            client.player.sendOverlayMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".inventorysort.completed"));
        }
    }

    private boolean canSort(Minecraft client) {
        return config.isEnabled() &&
               client.player != null &&
               client.gameMode != null &&
               client.screen instanceof AbstractContainerScreen;
    }

    /**
     * Sort player inventory instantly.
     * @param playerInvStart the screen slot index where player inventory starts (main inventory, not hotbar)
     */
    private void sortPlayerInventory(Minecraft client, int playerInvStart) {
        Inventory inv = client.player.getInventory();
        AbstractContainerMenu handler = client.player.containerMenu;
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
            ItemStack stack = inv.getItem(slot);
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
    private boolean sortContainer(Minecraft client, int containerSlots) {
        AbstractContainerMenu handler = client.player.containerMenu;
        boolean queueClicks = shouldUseQueuedShulkerSort(handler, containerSlots);

        // Collect all container slots
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < containerSlots; i++) {
            slots.add(i);
        }

        if (slots.isEmpty()) return false;

        // Step 1: Physically merge partial stacks
        mergePartialStacks(client, handler, slots, 0, true);

        // Step 2: Re-collect items after merge
        List<SortEntry> entries = new ArrayList<>();
        for (int slot : slots) {
            ItemStack stack = handler.getSlot(slot).getItem();
            if (!stack.isEmpty()) {
                entries.add(new SortEntry(slot, stack.copy()));
            }
        }

        if (entries.isEmpty()) return false;

        // Step 3: Sort entries
        sortEntries(entries);

        // Step 4: Execute sort
        executeSort(client, handler, entries, slots, containerSlots, true, queueClicks);
        return queueClicks && sorting;
    }

    /**
     * Sort entries by configured mode.
     */
    private void sortEntries(List<SortEntry> entries) {
        SortMode mode = config.getSortMode();

        Comparator<SortEntry> cmp = switch (mode) {
            case ITEM_ID -> Comparator.<SortEntry, String>comparing(e -> getItemIdSortKey(e.stack))
                    .thenComparingInt(e -> -e.stack.getCount());
            case ITEM_NAME -> Comparator.<SortEntry, String>comparing(e -> getItemNameSortKey(e.stack))
                    .thenComparingInt(e -> -e.stack.getCount());
            case CATEGORY -> Comparator.<SortEntry>comparingInt(e -> getStackCategory(e.stack))
                    .thenComparing(e -> getItemIdSortKey(e.stack))
                    .thenComparingInt(e -> -e.stack.getCount());
            case STACK_COUNT -> Comparator.<SortEntry>comparingInt(e -> -e.stack.getCount())
                    .thenComparing(e -> getItemIdSortKey(e.stack));
            case TOTAL_COUNT -> {
                Map<String, Integer> totalCounts = new HashMap<>();
                for (SortEntry e : entries) {
                    String key = getItemIdSortKey(e.stack);
                    totalCounts.merge(key, e.stack.getCount(), Integer::sum);
                }
                yield Comparator.<SortEntry>comparingInt(
                            e -> -totalCounts.getOrDefault(
                                getItemIdSortKey(e.stack), 0))
                        .thenComparing(e -> getItemIdSortKey(e.stack))
                        .thenComparingInt(e -> -e.stack.getCount());
            }
        };

        entries.sort(cmp);
    }

    private String getItemIdSortKey(ItemStack stack) {
        if (isShulkerBox(stack)) {
            String contentKey = getShulkerContentSortKey(stack);
            if (!contentKey.isEmpty()) {
                return contentKey;
            }
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private String getItemNameSortKey(ItemStack stack) {
        // Sort by the stack's own display name. Shulker boxes are intentionally
        // keyed by their own name (not their contents) so they cluster together
        // instead of scattering across unrelated items.
        return stack.getHoverName().getString();
    }

    private int getStackCategory(ItemStack stack) {
        ItemStack primaryContent = getPrimaryShulkerContent(stack);
        return getCategory(primaryContent.isEmpty() ? stack : primaryContent);
    }

    private int getCategory(ItemStack stack) {
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) return 0;
        if (stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES)) return 1;
        if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) return 2;
        if (stack.has(DataComponents.FOOD)) return 3;
        if (stack.getItem() instanceof BlockItem) return 4;
        return 5;
    }

    /**
     * Execute the sort by swapping items to their target positions.
     * @param playerInvStart for player inventory: screen slot where main inventory starts; for container: unused
     */
    private void executeSort(Minecraft client, AbstractContainerMenu handler,
                              List<SortEntry> sortedEntries, List<Integer> availableSlots,
                              int playerInvStart, boolean isContainer) {
        executeSort(client, handler, sortedEntries, availableSlots, playerInvStart, isContainer, false);
    }

    /**
     * Execute the sort by swapping items to their target positions.
     * @param playerInvStart for player inventory: screen slot where main inventory starts; for container: unused
     */
    private void executeSort(Minecraft client, AbstractContainerMenu handler,
                              List<SortEntry> sortedEntries, List<Integer> availableSlots,
                              int playerInvStart, boolean isContainer, boolean queueClicks) {
        int syncId = handler.containerId;
        List<Integer> clickSlots = new ArrayList<>();

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
            clickSlots.add(screenCurrent); // Pick up item from current
            clickSlots.add(screenTarget);  // Swap with target (or place if empty)

            // If target had an item, we now have it on cursor, put it back
            if (itemAtTarget >= 0) {
                clickSlots.add(screenCurrent); // Put swapped item at original location
                itemLocations[itemAtTarget] = currentSlot;
            }

            itemLocations[targetIdx] = targetSlot;
        }

        if (queueClicks) {
            enqueueSort(syncId, clickSlots);
        } else {
            for (int slot : clickSlots) {
                click(client, syncId, slot);
            }
        }
    }

    /**
     * Physically merge partial stacks of the same item type by clicking.
     * This performs actual inventory operations to combine items.
     */
    private void mergePartialStacks(Minecraft client, AbstractContainerMenu handler,
                                     List<Integer> slots, int playerInvStart, boolean isContainer) {
        Inventory inv = client.player.getInventory();
        int syncId = handler.containerId;

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
                        handler.getSlot(slot).getItem() :
                        inv.getItem(slot);

                if (!stack.isEmpty() && stack.getCount() < stack.getMaxStackSize()) {
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
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String components = componentsKey(stack);
        if (!components.isEmpty()) {
            key += "|" + components;
        }
        return key;
    }

    private static String componentsKey(ItemStack stack) {
        DataComponentPatch patch = stack.getComponentsPatch();
        return patch.isEmpty() ? "" : patch.toString();
    }

    private boolean shouldUseQueuedShulkerSort(AbstractContainerMenu handler, int containerSlots) {
        if (containerSlots != LARGE_CONTAINER_SLOTS) {
            return false;
        }

        for (int i = 0; i < containerSlots; i++) {
            if (isShulkerBox(handler.getSlot(i).getItem())) {
                return true;
            }
        }
        return false;
    }

    private boolean isShulkerBox(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();
        return block instanceof ShulkerBoxBlock;
    }

    private String getShulkerContentSortKey(ItemStack stack) {
        List<ContainedItemSummary> summaries = getShulkerContentSummaries(stack);
        if (summaries.isEmpty()) {
            return "";
        }

        summaries.sort(Comparator
                .<ContainedItemSummary>comparingInt(summary -> -summary.totalCount)
                .thenComparing(summary -> summary.id)
                .thenComparing(summary -> summary.nbt));

        StringBuilder key = new StringBuilder();
        ContainedItemSummary primary = summaries.get(0);
        key.append(primary.id);

        for (ContainedItemSummary summary : summaries) {
            key.append('|')
                    .append(summary.id)
                    .append('#')
                    .append(summary.totalCount);
            if (!summary.nbt.isEmpty()) {
                key.append('@').append(summary.nbt);
            }
        }
        return key.toString();
    }

    private ItemStack getPrimaryShulkerContent(ItemStack stack) {
        if (!isShulkerBox(stack)) {
            return ItemStack.EMPTY;
        }

        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        ItemStack bestStack = ItemStack.EMPTY;
        String bestId = "";
        int bestCount = -1;

        for (ItemStack contentStack : (Iterable<ItemStack>) contents.nonEmptyItemCopyStream()::iterator) {
            String id = BuiltInRegistries.ITEM.getKey(contentStack.getItem()).toString();
            int count = contentStack.getCount();
            if (count > bestCount || (count == bestCount && id.compareTo(bestId) < 0)) {
                bestStack = contentStack;
                bestId = id;
                bestCount = count;
            }
        }

        return bestStack;
    }

    private List<ContainedItemSummary> getShulkerContentSummaries(ItemStack stack) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return Collections.emptyList();
        }

        Map<String, ContainedItemSummary> summaries = new HashMap<>();

        for (ItemStack contentStack : (Iterable<ItemStack>) contents.nonEmptyItemCopyStream()::iterator) {
            String id = BuiltInRegistries.ITEM.getKey(contentStack.getItem()).toString();
            String nbt = componentsKey(contentStack);

            String key = id + "|" + nbt;
            String summaryId = id;
            String summaryNbt = nbt;
            ContainedItemSummary summary = summaries.computeIfAbsent(
                    key,
                    ignored -> new ContainedItemSummary(summaryId, summaryNbt)
            );
            summary.totalCount += contentStack.getCount();
        }

        return new ArrayList<>(summaries.values());
    }

    private boolean processQueuedSort(Minecraft client) {
        if (!sorting) {
            return false;
        }

        if (client.player == null
                || client.gameMode == null
                || client.player.containerMenu.containerId != queuedSyncId) {
            clearQueuedSort();
            return false;
        }

        int clicksThisTick = Math.min(QUEUED_CLICKS_PER_TICK, queuedClicks.size());
        for (int i = 0; i < clicksThisTick; i++) {
            click(client, queuedSyncId, queuedClicks.remove());
        }

        if (queuedClicks.isEmpty()) {
            clearQueuedSort();
            client.player.sendOverlayMessage(Component.translatable("message." + ASTTweaks.MOD_ID + ".inventorysort.completed"));
        }

        return true;
    }

    private void enqueueSort(int syncId, List<Integer> clickSlots) {
        if (clickSlots.isEmpty()) {
            return;
        }

        queuedClicks.clear();
        queuedClicks.addAll(clickSlots);
        queuedSyncId = syncId;
        sorting = true;
    }

    private void clearQueuedSort() {
        queuedClicks.clear();
        queuedSyncId = -1;
        sorting = false;
    }

    /**
     * Find the screen slot index where the player's main inventory starts.
     * This correctly handles both container screens and player inventory screen.
     */
    private int findPlayerInventoryStart(AbstractContainerMenu handler) {
        // Find the first slot that belongs to PlayerInventory and is a main inventory slot (index 9-35)
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.container instanceof Inventory) {
                int invIndex = slot.getContainerSlot();
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
    private int getContainerSlotCount(AbstractContainerMenu handler, int playerInvStart) {
        // Count slots before player inventory that are NOT part of PlayerInventory
        int count = 0;
        for (int i = 0; i < playerInvStart; i++) {
            Slot slot = handler.getSlot(i);
            if (!(slot.container instanceof Inventory)) {
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
    private void click(Minecraft client, int syncId, int slot) {
        client.gameMode.handleContainerInput(syncId, slot, 0, ContainerInput.PICKUP, client.player);
    }

    private static class SortEntry {
        final int originalSlot;
        final ItemStack stack;

        SortEntry(int slot, ItemStack stack) {
            this.originalSlot = slot;
            this.stack = stack;
        }
    }

    private static class ContainedItemSummary {
        final String id;
        final String nbt;
        int totalCount;

        ContainedItemSummary(String id, String nbt) {
            this.id = id;
            this.nbt = nbt;
        }
    }
}
