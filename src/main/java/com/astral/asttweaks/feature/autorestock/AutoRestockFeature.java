package com.astral.asttweaks.feature.autorestock;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
        RETURN_REMAINDER,
        SWAP_TO_OFFHAND
    }

    /** Hotbar button index used by SlotActionType.SWAP to swap a slot with the offhand. */
    private static final int OFFHAND_SWAP_BUTTON = 40;

    private static class TransferTask {
        final TaskContext context;
        final ItemStack configuredStack;
        final int expectedSyncId;
        final int sourceScreenSlot;
        final int targetScreenSlot;
        final int sourceCount;
        final int transferCount;
        TaskStage stage;
        int remainingPlacements;

        TransferTask(TaskContext context, ItemStack configuredStack, int expectedSyncId,
                     int sourceScreenSlot, int targetScreenSlot, int sourceCount, int transferCount,
                     boolean offhandSwap) {
            this.context = context;
            this.configuredStack = configuredStack;
            this.expectedSyncId = expectedSyncId;
            this.sourceScreenSlot = sourceScreenSlot;
            this.targetScreenSlot = targetScreenSlot;
            this.sourceCount = sourceCount;
            this.transferCount = transferCount;
            this.stage = offhandSwap ? TaskStage.SWAP_TO_OFFHAND : TaskStage.PICKUP_SOURCE;
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
            closeHiddenContainerIfNeeded(Minecraft.getInstance());
            resetState();
        }
    }

    public AutoRestockConfig getConfig() {
        return config;
    }

    public void tryHideVisibleContainerScreen() {
        Minecraft client = Minecraft.getInstance();
        if (!canHideVisibleContainerScreen(client)) {
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        TransferTask nextTask = activeTask;
        if (nextTask != null) {
            if (nextTask.context != TaskContext.CONTAINER || nextTask.expectedSyncId != handler.containerId) {
                return;
            }
        } else {
            if (!handler.getCarried().isEmpty()) {
                return;
            }

            nextTask = createNextTask(handler, TaskContext.CONTAINER);
            if (nextTask == null) {
                return;
            }
        }

        hiddenContainerSyncId = handler.containerId;
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

    private void onClientTick(Minecraft client) {
        if (!config.isEnabled()) {
            closeHiddenContainerIfNeeded(client);
            resetState();
            return;
        }

        if (client.player == null || client.level == null || client.gameMode == null || client.isPaused()) {
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

        AbstractContainerMenu handler = client.player.containerMenu;
        if (!handler.getCarried().isEmpty()) {
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
        } else if (client.screen == null
                && config.isInventoryRestockEnabled()
                && handler instanceof InventoryMenu) {
            activeTask = createNextTask(handler, TaskContext.PLAYER);
        }

        if (activeTask != null) {
            processActiveTask(client);
        }
    }

    private void processActiveTask(Minecraft client) {
        if (activeTask == null || !isTaskContextValid(client, activeTask)) {
            resetState();
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        int limit = config.getOperationsPerTick();

        for (int i = 0; i < limit && activeTask != null; i++) {
            switch (activeTask.stage) {
                case PICKUP_SOURCE -> {
                    ItemStack sourceStack = handler.getSlot(activeTask.sourceScreenSlot).getItem();
                    if (!AutoRestockConfig.matches(activeTask.configuredStack, sourceStack)) {
                        resetState();
                        return;
                    }

                    click(client, activeTask.sourceScreenSlot, 0);
                    activeTask.stage = activeTask.useWholeStack() ? TaskStage.PLACE_ALL : TaskStage.PLACE_ONE_BY_ONE;
                }

                case PLACE_ALL -> {
                    ItemStack targetStack = handler.getSlot(activeTask.targetScreenSlot).getItem();
                    if (!targetStack.isEmpty() && !AutoRestockConfig.matches(activeTask.configuredStack, targetStack)) {
                        resetState();
                        return;
                    }

                    click(client, activeTask.targetScreenSlot, 0);
                    activeTask = null;
                }

                case PLACE_ONE_BY_ONE -> {
                    ItemStack cursorStack = handler.getCarried();
                    ItemStack targetStack = handler.getSlot(activeTask.targetScreenSlot).getItem();
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
                    if (!handler.getCarried().isEmpty()) {
                        click(client, activeTask.sourceScreenSlot, 0);
                    }
                    activeTask = null;
                }

                case SWAP_TO_OFFHAND -> {
                    ItemStack sourceStack = handler.getSlot(activeTask.sourceScreenSlot).getItem();
                    if (!getOffhandStack().isEmpty()
                            || !AutoRestockConfig.matches(activeTask.configuredStack, sourceStack)) {
                        resetState();
                        return;
                    }

                    swapToOffhand(client, activeTask.sourceScreenSlot);
                    activeTask = null;
                }
            }
        }
    }

    private TransferTask createNextTask(AbstractContainerMenu handler, TaskContext context) {
        for (AutoRestockEntry entry : config.getEntries()) {
            TransferTask task = createTaskForEntry(handler, entry, context);
            if (task != null) {
                return task;
            }
        }

        return null;
    }

    private TransferTask createTaskForEntry(AbstractContainerMenu handler, AutoRestockEntry entry, TaskContext context) {
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
        boolean offhandContainerTarget = false;
        boolean offhandEmpty = false;

        for (int targetInventorySlot : targetSlots) {
            int targetInvIndex = sanitizeTargetSlot(targetInventorySlot);

            // Container screen handlers do not expose the offhand slot, so it cannot be
            // clicked directly. Track it here and, if no other target needs filling,
            // restock it through an offhand SWAP action below.
            if (targetInvIndex == AutoRestockConfig.OFFHAND_SLOT && context == TaskContext.CONTAINER) {
                ItemStack offhandStack = getOffhandStack();
                offhandContainerTarget = true;
                offhandEmpty = offhandStack.isEmpty();
                if (!offhandStack.isEmpty() && AutoRestockConfig.matches(configuredStack, offhandStack)) {
                    totalCurrentCount += offhandStack.getCount();
                }
                continue;
            }

            int candidateTargetScreenSlot = findPlayerInventoryScreenSlot(handler, targetInvIndex);
            if (candidateTargetScreenSlot == -1) {
                continue;
            }

            ItemStack targetStack = handler.getSlot(candidateTargetScreenSlot).getItem();
            if (targetStack.isEmpty()) {
                if (targetScreenSlot == -1) {
                    targetScreenSlot = candidateTargetScreenSlot;
                    targetAvailableSpace = configuredStack.getMaxStackSize();
                }
                continue;
            }

            if (!AutoRestockConfig.matches(configuredStack, targetStack)) {
                continue;
            }

            totalCurrentCount += targetStack.getCount();
            int availableSpace = configuredStack.getMaxStackSize() - targetStack.getCount();
            if (availableSpace > 0 && targetScreenSlot == -1) {
                targetScreenSlot = candidateTargetScreenSlot;
                targetAvailableSpace = availableSpace;
            }
        }

        // The offhand can only be reached from a container via a SWAP action, and only
        // safely when it is empty. Use it as a fallback once no directly clickable
        // target slot needs filling.
        boolean offhandSwapTarget = false;
        if (targetScreenSlot == -1 && offhandContainerTarget && offhandEmpty) {
            offhandSwapTarget = true;
            targetAvailableSpace = configuredStack.getMaxStackSize();
        }

        int deficit = desiredCount - totalCurrentCount;
        if (deficit <= 0 || targetAvailableSpace <= 0 || (targetScreenSlot == -1 && !offhandSwapTarget)) {
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

        ItemStack sourceStack = handler.getSlot(sourceScreenSlot).getItem();
        int transferCount = Math.min(Math.min(deficit, sourceStack.getCount()), targetAvailableSpace);
        if (transferCount <= 0) {
            return null;
        }

        if (offhandSwapTarget) {
            // A direct SWAP moves the whole source stack into the offhand. Only use it
            // when that exactly matches the amount we want (e.g. a single totem);
            // partial offhand restock from a container is not supported.
            if (transferCount != sourceStack.getCount()) {
                return null;
            }

            return new TransferTask(
                    context,
                    configuredStack.copyWithCount(1),
                    handler.containerId,
                    sourceScreenSlot,
                    -1,
                    sourceStack.getCount(),
                    transferCount,
                    true);
        }

        return new TransferTask(
                context,
                configuredStack.copyWithCount(1),
                handler.containerId,
                sourceScreenSlot,
                targetScreenSlot,
                sourceStack.getCount(),
                transferCount,
                false);
    }

    private int findContainerSourceSlot(AbstractContainerMenu handler, ItemStack configuredStack) {
        boolean fromEnd = config.getContainerPickupOrder() == ContainerPickupOrder.LAST_SLOT;
        int size = handler.slots.size();

        for (int n = 0; n < size; n++) {
            int i = fromEnd ? size - 1 - n : n;
            Slot slot = handler.getSlot(i);
            if (slot.container instanceof Inventory) {
                continue;
            }

            ItemStack stack = slot.getItem();
            if (AutoRestockConfig.matches(configuredStack, stack)) {
                return i;
            }
        }

        return -1;
    }

    private int findPlayerSourceSlot(AbstractContainerMenu handler, ItemStack configuredStack, Set<Integer> reservedTargetSlots) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (!(slot.container instanceof Inventory)) {
                continue;
            }

            int invIndex = slot.getContainerSlot();
            if (reservedTargetSlots.contains(invIndex)
                    || config.isSlotProtected(invIndex)
                    || invIndex == AutoRestockConfig.OFFHAND_SLOT
                    || invIndex < 9
                    || invIndex > 35) {
                continue;
            }

            if (AutoRestockConfig.matches(configuredStack, slot.getItem())) {
                return i;
            }
        }

        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (!(slot.container instanceof Inventory)) {
                continue;
            }

            int invIndex = slot.getContainerSlot();
            if (reservedTargetSlots.contains(invIndex)
                    || config.isSlotProtected(invIndex)
                    || invIndex < 0
                    || invIndex > 8) {
                continue;
            }

            if (AutoRestockConfig.matches(configuredStack, slot.getItem())) {
                return i;
            }
        }

        return -1;
    }

    private int findPlayerInventoryScreenSlot(AbstractContainerMenu handler, int targetInvIndex) {
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.getSlot(i);
            if (slot.container instanceof Inventory && slot.getContainerSlot() == targetInvIndex) {
                return i;
            }
        }
        return -1;
    }

    private boolean isTaskContextValid(Minecraft client, TransferTask task) {
        if (client.player == null || client.player.containerMenu.containerId != task.expectedSyncId) {
            return false;
        }

        return switch (task.context) {
            case PLAYER -> client.screen == null && client.player.containerMenu instanceof InventoryMenu;
            case CONTAINER -> shouldProcessContainerRestock(client);
        };
    }

    private boolean shouldProcessContainerRestock(Minecraft client) {
        return config.isContainerRestockEnabled() && isSupportedOpenContainer(client);
    }

    private boolean isSupportedOpenContainer(Minecraft client) {
        if (client.player == null || client.player.containerMenu instanceof InventoryMenu) {
            return false;
        }

        return isHiddenContainerSessionReady(client);
    }

    private boolean hasHiddenContainerSession(Minecraft client) {
        return hiddenContainerSyncId != -1
                && client.player != null
                && !(client.player.containerMenu instanceof InventoryMenu)
                && client.player.containerMenu.containerId == hiddenContainerSyncId;
    }

    private boolean isHiddenContainerSessionReady(Minecraft client) {
        return hasHiddenContainerSession(client) && client.screen == null;
    }

    private boolean canHideVisibleContainerScreen(Minecraft client) {
        return config.isEnabled()
                && config.isContainerRestockEnabled()
                && client.player != null
                && client.level != null
                && client.gameMode != null
                && !client.isPaused()
                && client.screen != null
                && !shouldDeferToExternalAutoCollect()
                && isSupportedContainerScreen(client.screen)
                && !(client.player.containerMenu instanceof InventoryMenu);
    }

    private void closeHiddenContainerIfNeeded(Minecraft client) {
        if (client != null && isHiddenContainerSessionReady(client)) {
            closeHiddenContainer(client);
        } else {
            clearHiddenContainerSession();
        }
    }

    private void closeHiddenContainer(Minecraft client) {
        clearHiddenContainerSession();
        activeTask = null;
        if (client.player != null && !(client.player.containerMenu instanceof InventoryMenu)) {
            client.player.closeContainer();
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
        return screen instanceof ContainerScreen
                || screen instanceof DispenserScreen
                || screen instanceof HopperScreen
                || screen instanceof ShulkerBoxScreen;
    }

    private int sanitizeTargetSlot(int slot) {
        if ((slot >= 0 && slot <= 35) || slot == AutoRestockConfig.OFFHAND_SLOT) {
            return slot;
        }
        return 0;
    }

    private void click(Minecraft client, int slot, int button) {
        client.gameMode.handleContainerInput(
                client.player.containerMenu.containerId,
                slot,
                button,
                ContainerInput.PICKUP,
                client.player);
    }

    private void swapToOffhand(Minecraft client, int slot) {
        client.gameMode.handleContainerInput(
                client.player.containerMenu.containerId,
                slot,
                OFFHAND_SWAP_BUTTON,
                ContainerInput.SWAP,
                client.player);
    }

    private ItemStack getOffhandStack() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return ItemStack.EMPTY;
        }
        return client.player.getOffhandItem();
    }

    private void resetState() {
        activeTask = null;
    }
}
