package com.astral.asttweaks.feature.autorestock;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Restocks configured inventory slots from the player's inventory or opened storage containers.
 */
public class AutoRestockFeature implements Feature {
    private final AutoRestockConfig config;
    private TransferTask activeTask;
    private int hiddenContainerSyncId = -1;

    private enum TaskContext {
        PLAYER,
        CONTAINER
    }

    private enum TaskStage {
        PICKUP_SOURCE,
        PLACE_ALL,
        PLACE_ONE_BY_ONE,
        RETURN_REMAINDER
    }

    private static class TransferTask {
        final TaskContext context;
        final ItemStack configuredStack;
        final int expectedSyncId;
        final int sourceScreenSlot;
        final int targetScreenSlot;
        final int sourceCount;
        final int transferCount;
        TaskStage stage = TaskStage.PICKUP_SOURCE;
        int remainingPlacements;

        TransferTask(TaskContext context, ItemStack configuredStack, int expectedSyncId,
                     int sourceScreenSlot, int targetScreenSlot, int sourceCount, int transferCount) {
            this.context = context;
            this.configuredStack = configuredStack;
            this.expectedSyncId = expectedSyncId;
            this.sourceScreenSlot = sourceScreenSlot;
            this.targetScreenSlot = targetScreenSlot;
            this.sourceCount = sourceCount;
            this.transferCount = transferCount;
            this.remainingPlacements = transferCount;
        }

        boolean useWholeStack() {
            return transferCount == sourceCount;
        }
    }

    public AutoRestockFeature() {
        this.config = new AutoRestockConfig();
    }

    @Override
    public String getId() {
        return "autorestock";
    }

    @Override
    public String getName() {
        return "Auto Restock";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("Auto Restock feature initialized");
    }

    @Override
    public void tick() {
        // Tick processing handled in onClientTick
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
        if (!enabled) {
            closeHiddenContainerIfNeeded(MinecraftClient.getInstance());
            resetState();
        }
    }

    public AutoRestockConfig getConfig() {
        return config;
    }

    public void tryHideVisibleContainerScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!canHideVisibleContainerScreen(client)) {
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        TransferTask nextTask = activeTask;
        if (nextTask != null) {
            if (nextTask.context != TaskContext.CONTAINER || nextTask.expectedSyncId != handler.syncId) {
                return;
            }
        } else {
            if (!handler.getCursorStack().isEmpty()) {
                return;
            }

            nextTask = createNextTask(handler, TaskContext.CONTAINER);
            if (nextTask == null) {
                return;
            }
        }

        hiddenContainerSyncId = handler.syncId;
        activeTask = nextTask;
        client.setScreen(null);

        if (!isHiddenContainerSessionReady(client)) {
            clearHiddenContainerSession();
            if (!isTaskContextValid(client, nextTask)) {
                resetState();
            }
            return;
        }
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            closeHiddenContainerIfNeeded(client);
            resetState();
            return;
        }

        if (client.player == null || client.world == null || client.interactionManager == null || client.isPaused()) {
            clearHiddenContainerSession();
            resetState();
            return;
        }

        if (!hasHiddenContainerSession(client)) {
            clearHiddenContainerSession();
        }

        tryHideVisibleContainerScreen();

        if (activeTask != null) {
            processActiveTask(client);
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        if (!handler.getCursorStack().isEmpty()) {
            return;
        }

        if (shouldProcessContainerRestock(client)) {
            if (!hasHiddenContainerSession(client) && shouldDeferToExternalAutoCollect()) {
                return;
            }

            activeTask = createNextTask(handler, TaskContext.CONTAINER);
            if (activeTask == null && isHiddenContainerSessionReady(client)) {
                closeHiddenContainer(client);
                return;
            }
        } else if (client.currentScreen == null
                && config.isInventoryRestockEnabled()
                && handler instanceof PlayerScreenHandler) {
            activeTask = createNextTask(handler, TaskContext.PLAYER);
        }

        if (activeTask != null) {
            processActiveTask(client);
        }
    }

    private void processActiveTask(MinecraftClient client) {
        if (activeTask == null || !isTaskContextValid(client, activeTask)) {
            resetState();
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        int limit = config.getOperationsPerTick();

        for (int i = 0; i < limit && activeTask != null; i++) {
            switch (activeTask.stage) {
                case PICKUP_SOURCE -> {
                    ItemStack sourceStack = handler.getSlot(activeTask.sourceScreenSlot).getStack();
                    if (!AutoRestockConfig.matches(activeTask.configuredStack, sourceStack)) {
                        resetState();
                        return;
                    }

                    click(client, activeTask.sourceScreenSlot, 0);
                    activeTask.stage = activeTask.useWholeStack() ? TaskStage.PLACE_ALL : TaskStage.PLACE_ONE_BY_ONE;
                }

                case PLACE_ALL -> {
                    ItemStack targetStack = handler.getSlot(activeTask.targetScreenSlot).getStack();
                    if (!targetStack.isEmpty() && !AutoRestockConfig.matches(activeTask.configuredStack, targetStack)) {
                        resetState();
                        return;
                    }

                    click(client, activeTask.targetScreenSlot, 0);
                    activeTask = null;
                }

                case PLACE_ONE_BY_ONE -> {
                    ItemStack cursorStack = handler.getCursorStack();
                    ItemStack targetStack = handler.getSlot(activeTask.targetScreenSlot).getStack();
                    if (!AutoRestockConfig.matches(activeTask.configuredStack, cursorStack) ||
                            (!targetStack.isEmpty() && !AutoRestockConfig.matches(activeTask.configuredStack, targetStack))) {
                        resetState();
                        return;
                    }

                    click(client, activeTask.targetScreenSlot, 1);
                    activeTask.remainingPlacements--;
                    if (activeTask.remainingPlacements <= 0) {
                        activeTask.stage = TaskStage.RETURN_REMAINDER;
                    }
                }

                case RETURN_REMAINDER -> {
                    if (!handler.getCursorStack().isEmpty()) {
                        click(client, activeTask.sourceScreenSlot, 0);
                    }
                    activeTask = null;
                }
            }
        }
    }

    private TransferTask createNextTask(ScreenHandler handler, TaskContext context) {
        for (AutoRestockEntry entry : config.getEntries()) {
            TransferTask task = createTaskForEntry(handler, entry, context);
            if (task != null) {
                return task;
            }
        }

        return null;
    }

    private TransferTask createTaskForEntry(ScreenHandler handler, AutoRestockEntry entry, TaskContext context) {
        ItemStack configuredStack = config.getConfiguredStack(entry);
        if (configuredStack.isEmpty()) {
            return null;
        }

        List<Integer> targetSlots = config.getTargetSlots(entry);
        if (targetSlots.isEmpty()) {
            return null;
        }

        int desiredCount = Math.max(1, entry.desiredCount);
        int totalCurrentCount = 0;
        int targetScreenSlot = -1;
        int targetAvailableSpace = 0;

        for (int targetInventorySlot : targetSlots) {
            int targetInvIndex = sanitizeTargetSlot(targetInventorySlot);
            int candidateTargetScreenSlot = findPlayerInventoryScreenSlot(handler, targetInvIndex);
            if (candidateTargetScreenSlot == -1) {
                continue;
            }

            ItemStack targetStack = handler.getSlot(candidateTargetScreenSlot).getStack();
            if (targetStack.isEmpty()) {
                if (targetScreenSlot == -1) {
                    targetScreenSlot = candidateTargetScreenSlot;
                    targetAvailableSpace = configuredStack.getMaxCount();
                }
                continue;
            }

            if (!AutoRestockConfig.matches(configuredStack, targetStack)) {
                continue;
            }

            totalCurrentCount += targetStack.getCount();
            int availableSpace = configuredStack.getMaxCount() - targetStack.getCount();
            if (availableSpace > 0 && targetScreenSlot == -1) {
                targetScreenSlot = candidateTargetScreenSlot;
                targetAvailableSpace = availableSpace;
            }
        }

        int deficit = desiredCount - totalCurrentCount;
        if (deficit <= 0 || targetScreenSlot == -1 || targetAvailableSpace <= 0) {
            return null;
        }

        Set<Integer> reservedTargetSlots = new HashSet<>();
        for (int targetSlot : targetSlots) {
            reservedTargetSlots.add(sanitizeTargetSlot(targetSlot));
        }

        int sourceScreenSlot = context == TaskContext.CONTAINER
                ? findContainerSourceSlot(handler, configuredStack)
                : findPlayerSourceSlot(handler, configuredStack, reservedTargetSlots);
        if (sourceScreenSlot == -1) {
            return null;
        }

        ItemStack sourceStack = handler.getSlot(sourceScreenSlot).getStack();
        int transferCount = Math.min(Math.min(deficit, sourceStack.getCount()), targetAvailableSpace);
        if (transferCount <= 0) {
            return null;
        }

        return new TransferTask(
                context,
                configuredStack.copyWithCount(1),
                handler.syncId,
                sourceScreenSlot,
                targetScreenSlot,
                sourceStack.getCount(),
                transferCount);
    }

    private int findContainerSourceSlot(ScreenHandler handler, ItemStack configuredStack) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.inventory instanceof PlayerInventory) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (AutoRestockConfig.matches(configuredStack, stack)) {
                return i;
            }
        }

        return -1;
    }

    private int findPlayerSourceSlot(ScreenHandler handler, ItemStack configuredStack, Set<Integer> reservedTargetSlots) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (!(slot.inventory instanceof PlayerInventory)) {
                continue;
            }

            int invIndex = slot.getIndex();
            if (reservedTargetSlots.contains(invIndex)
                    || invIndex == AutoRestockConfig.OFFHAND_SLOT
                    || invIndex < 9
                    || invIndex > 35) {
                continue;
            }

            if (AutoRestockConfig.matches(configuredStack, slot.getStack())) {
                return i;
            }
        }

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (!(slot.inventory instanceof PlayerInventory)) {
                continue;
            }

            int invIndex = slot.getIndex();
            if (reservedTargetSlots.contains(invIndex) || invIndex < 0 || invIndex > 8) {
                continue;
            }

            if (AutoRestockConfig.matches(configuredStack, slot.getStack())) {
                return i;
            }
        }

        return -1;
    }

    private int findPlayerInventoryScreenSlot(ScreenHandler handler, int targetInvIndex) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.inventory instanceof PlayerInventory && slot.getIndex() == targetInvIndex) {
                return i;
            }
        }
        return -1;
    }

    private boolean isTaskContextValid(MinecraftClient client, TransferTask task) {
        if (client.player == null || client.player.currentScreenHandler.syncId != task.expectedSyncId) {
            return false;
        }

        return switch (task.context) {
            case PLAYER -> client.currentScreen == null && client.player.currentScreenHandler instanceof PlayerScreenHandler;
            case CONTAINER -> shouldProcessContainerRestock(client);
        };
    }

    private boolean shouldProcessContainerRestock(MinecraftClient client) {
        return config.isContainerRestockEnabled() && isSupportedOpenContainer(client);
    }

    private boolean isSupportedOpenContainer(MinecraftClient client) {
        if (client.player == null || client.player.currentScreenHandler instanceof PlayerScreenHandler) {
            return false;
        }

        return isHiddenContainerSessionReady(client);
    }

    private boolean hasHiddenContainerSession(MinecraftClient client) {
        return hiddenContainerSyncId != -1
                && client.player != null
                && !(client.player.currentScreenHandler instanceof PlayerScreenHandler)
                && client.player.currentScreenHandler.syncId == hiddenContainerSyncId;
    }

    private boolean isHiddenContainerSessionReady(MinecraftClient client) {
        return hasHiddenContainerSession(client) && client.currentScreen == null;
    }

    private boolean canHideVisibleContainerScreen(MinecraftClient client) {
        return config.isEnabled()
                && config.isContainerRestockEnabled()
                && client.player != null
                && client.world != null
                && client.interactionManager != null
                && !client.isPaused()
                && client.currentScreen != null
                && !shouldDeferToExternalAutoCollect()
                && isSupportedContainerScreen(client.currentScreen)
                && !(client.player.currentScreenHandler instanceof PlayerScreenHandler);
    }

    private void closeHiddenContainerIfNeeded(MinecraftClient client) {
        if (client != null && isHiddenContainerSessionReady(client)) {
            closeHiddenContainer(client);
        } else {
            clearHiddenContainerSession();
        }
    }

    private void closeHiddenContainer(MinecraftClient client) {
        clearHiddenContainerSession();
        activeTask = null;
        if (client.player != null && !(client.player.currentScreenHandler instanceof PlayerScreenHandler)) {
            client.player.closeHandledScreen();
        }
    }

    private void clearHiddenContainerSession() {
        hiddenContainerSyncId = -1;
    }

    private boolean shouldDeferToExternalAutoCollect() {
        return !config.shouldPrioritizeOverExternalAutoCollect()
                && TweakerooCompat.isAutoCollectMaterialListItemEnabled();
    }

    private boolean isSupportedContainerScreen(Screen screen) {
        return screen instanceof GenericContainerScreen
                || screen instanceof Generic3x3ContainerScreen
                || screen instanceof HopperScreen
                || screen instanceof ShulkerBoxScreen;
    }

    private int sanitizeTargetSlot(int slot) {
        if ((slot >= 0 && slot <= 35) || slot == AutoRestockConfig.OFFHAND_SLOT) {
            return slot;
        }
        return 0;
    }

    private void click(MinecraftClient client, int slot, int button) {
        client.interactionManager.clickSlot(
                client.player.currentScreenHandler.syncId,
                slot,
                button,
                SlotActionType.PICKUP,
                client.player);
    }

    private void resetState() {
        activeTask = null;
    }
}
