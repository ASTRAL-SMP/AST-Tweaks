package com.astral.asttweaks.feature.autorestock;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Restocks configured inventory slots from the player's inventory or an opened shulker box.
 */
public class AutoRestockFeature implements Feature {
    private final AutoRestockConfig config;
    private TransferTask activeTask;

    private enum TaskContext {
        PLAYER,
        SHULKER
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
            resetState();
        }
    }

    public AutoRestockConfig getConfig() {
        return config;
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            resetState();
            return;
        }

        if (client.player == null || client.world == null || client.interactionManager == null || client.isPaused()) {
            resetState();
            return;
        }

        if (activeTask != null) {
            processActiveTask(client);
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        if (!handler.getCursorStack().isEmpty()) {
            return;
        }

        if (isShulkerScreenOpen(client)) {
            if (!config.isShulkerRestockEnabled()) {
                return;
            }

            activeTask = createNextTask(handler, TaskContext.SHULKER);
        } else if (client.currentScreen == null && config.isInventoryRestockEnabled()) {
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
            for (int targetSlot : config.getTargetSlots(entry)) {
                TransferTask task = createTaskForEntry(handler, entry, targetSlot, context);
                if (task != null) {
                    return task;
                }
            }
        }

        return null;
    }

    private TransferTask createTaskForEntry(ScreenHandler handler, AutoRestockEntry entry, int targetInventorySlot, TaskContext context) {
        ItemStack configuredStack = config.getConfiguredStack(entry);
        if (configuredStack.isEmpty()) {
            return null;
        }

        int targetInvIndex = sanitizeTargetSlot(targetInventorySlot);
        int targetScreenSlot = findPlayerInventoryScreenSlot(handler, targetInvIndex);
        if (targetScreenSlot == -1) {
            return null;
        }

        Slot destinationSlot = handler.getSlot(targetScreenSlot);
        ItemStack targetStack = destinationSlot.getStack();
        if (!targetStack.isEmpty() && !AutoRestockConfig.matches(configuredStack, targetStack)) {
            return null;
        }

        int desiredCount = Math.max(1, Math.min(entry.desiredCount, configuredStack.getMaxCount()));
        int currentCount = targetStack.isEmpty() ? 0 : targetStack.getCount();
        int deficit = desiredCount - currentCount;
        if (deficit <= 0) {
            return null;
        }

        int sourceScreenSlot = context == TaskContext.SHULKER
                ? findContainerSourceSlot(handler, configuredStack)
                : findPlayerSourceSlot(handler, configuredStack, targetInvIndex);
        if (sourceScreenSlot == -1) {
            return null;
        }

        ItemStack sourceStack = handler.getSlot(sourceScreenSlot).getStack();
        int availableSpace = configuredStack.getMaxCount() - currentCount;
        int transferCount = Math.min(Math.min(deficit, sourceStack.getCount()), availableSpace);
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

    private int findPlayerSourceSlot(ScreenHandler handler, ItemStack configuredStack, int targetInvIndex) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (!(slot.inventory instanceof PlayerInventory)) {
                continue;
            }

            int invIndex = slot.getIndex();
            if (invIndex == targetInvIndex || invIndex == AutoRestockConfig.OFFHAND_SLOT || invIndex < 9 || invIndex > 35) {
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
            if (invIndex == targetInvIndex || invIndex < 0 || invIndex > 8) {
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
            case PLAYER -> client.currentScreen == null;
            case SHULKER -> isShulkerScreenOpen(client);
        };
    }

    private boolean isShulkerScreenOpen(MinecraftClient client) {
        return client.currentScreen instanceof ShulkerBoxScreen;
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
