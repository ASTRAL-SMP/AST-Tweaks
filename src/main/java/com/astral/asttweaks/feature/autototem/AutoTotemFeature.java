package com.astral.asttweaks.feature.autototem;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.McClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Auto totem restock feature that automatically moves a totem to the offhand
 * after the current one is consumed.
 */
public class AutoTotemFeature implements Feature {
    private final AutoTotemConfig config;

    // State machine states
    private enum State {
        IDLE,           // Waiting for totem use
        TOTEM_USED,     // Totem just used, start restock
        WAIT_DELAY,     // Waiting for server sync
        RESTOCKING      // Moving totem to offhand
    }

    private State currentState = State.IDLE;
    private int delayTicks = 0;
    private static final int WAIT_DELAY_TICKS = 3;

    public AutoTotemFeature() {
        this.config = new AutoTotemConfig();
    }

    @Override
    public String getId() {
        return "autototem";
    }

    @Override
    public String getName() {
        return "Auto Totem";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("AutoTotem feature initialized");
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
    }

    public AutoTotemConfig getConfig() {
        return config;
    }

    /**
     * Called when a totem of undying is used (from mixin).
     */
    public void onTotemUsed() {
        if (config.isEnabled() && currentState == State.IDLE) {
            currentState = State.TOTEM_USED;
            ASTTweaks.LOGGER.debug("Totem used, starting restock sequence");
        }
    }

    private void onClientTick(Minecraft client) {
        if (!config.isEnabled()) {
            return;
        }

        if (client.player == null || client.level == null || client.isPaused()) {
            resetState();
            return;
        }

        processStateMachine(client);
    }

    private void processStateMachine(Minecraft client) {
        Player player = client.player;

        switch (currentState) {
            case IDLE:
                // Do nothing, waiting for totem use event
                break;

            case TOTEM_USED:
                currentState = State.WAIT_DELAY;
                delayTicks = 0;
                break;

            case WAIT_DELAY:
                delayTicks++;
                if (delayTicks >= WAIT_DELAY_TICKS) {
                    currentState = State.RESTOCKING;
                }
                break;

            case RESTOCKING:
                restockTotem(client, player);
                resetState();
                break;
        }
    }

    private void restockTotem(Minecraft client, Player player) {
        Inventory inventory = player.getInventory();

        // Check if offhand already has a totem
        ItemStack offhandStack = inventory.getItem(Inventory.SLOT_OFFHAND);
        if (offhandStack.getItem() == Items.TOTEM_OF_UNDYING) {
            ASTTweaks.LOGGER.debug("Offhand already has a totem");
            return;
        }

        // First check hotbar (slots 0-8)
        int totemSlot = findTotemInHotbar(inventory);
        if (totemSlot != -1) {
            swapToOffhand(client, player, totemSlot);
            return;
        }

        // Then check main inventory (slots 9-35)
        totemSlot = findTotemInMainInventory(inventory);
        if (totemSlot != -1) {
            moveTotemFromInventory(client, player, totemSlot);
            return;
        }

        ASTTweaks.LOGGER.debug("No totem found in inventory");
    }

    private int findTotemInHotbar(Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }

    private int findTotemInMainInventory(Inventory inventory) {
        for (int i = 9; i < 36; i++) {
            if (inventory.getItem(i).getItem() == Items.TOTEM_OF_UNDYING) {
                return i;
            }
        }
        return -1;
    }

    private void swapToOffhand(Minecraft client, Player player, int hotbarSlot) {
        // Save current slot
        int originalSlot = McClient.selectedSlot(player.getInventory());

        // Select the totem slot
        McClient.setSelectedSlot(player.getInventory(), hotbarSlot);

        // Simulate pressing the swap key (F key)
        if (client.gameMode != null && client.player != null) {
            // Use clickSlot to swap main hand with offhand
            // Slot 45 is the offhand slot in player inventory
            // We'll use a pickup + offhand swap approach
            client.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    36 + hotbarSlot, // Hotbar slots in container start at 36
                    40, // Offhand slot
                    ContainerInput.SWAP,
                    player
            );
        }

        // Restore original slot
        McClient.setSelectedSlot(player.getInventory(), originalSlot);

        ASTTweaks.LOGGER.debug("Swapped totem from hotbar slot {} to offhand", hotbarSlot);
    }

    private void moveTotemFromInventory(Minecraft client, Player player, int inventorySlot) {
        if (client.gameMode != null) {
            // Swap the inventory slot with offhand directly
            client.gameMode.handleContainerInput(
                    player.containerMenu.containerId,
                    inventorySlot, // Inventory slot
                    40, // Offhand slot
                    ContainerInput.SWAP,
                    player
            );
            ASTTweaks.LOGGER.debug("Moved totem from inventory slot {} to offhand", inventorySlot);
        }
    }

    private void resetState() {
        currentState = State.IDLE;
        delayTicks = 0;
    }
}
