package com.astral.asttweaks.feature.massgrindstone;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.KeyBindings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GrindstoneScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.Map;

/**
 * Mass Grindstone feature - ItemScroller/MassCraft style fast enchantment removal.
 * Press the execute key once to start processing, close the grindstone screen to stop.
 * Results are dropped for maximum efficiency.
 */
public class MassGrindstoneFeature implements Feature {
    private final MassGrindstoneConfig config;
    private boolean isProcessing = false;
    private int processedCount = 0;
    private boolean keyWasPressed = false;  // Track key state for edge detection

    // Grindstone slot indices
    private static final int GRINDSTONE_INPUT_SLOT_1 = 0;  // Top input slot
    private static final int GRINDSTONE_INPUT_SLOT_2 = 1;  // Bottom input slot
    private static final int GRINDSTONE_OUTPUT_SLOT = 2;   // Result slot
    private static final int GRINDSTONE_INVENTORY_START = 3;  // Main inventory starts here
    private static final int GRINDSTONE_HOTBAR_START = 30;    // Hotbar starts here

    public MassGrindstoneFeature() {
        this.config = new MassGrindstoneConfig();
    }

    @Override
    public String getId() {
        return "massgrindstone";
    }

    @Override
    public String getName() {
        return "Mass Grindstone";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("MassGrindstone feature initialized");
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
            stopProcessing();
        }
    }

    public MassGrindstoneConfig getConfig() {
        return config;
    }

    /**
     * Start processing items in the grindstone.
     */
    public void startProcessing() {
        if (!isProcessing) {
            isProcessing = true;
            processedCount = 0;
            ASTTweaks.LOGGER.info("MassGrindstone: Started processing");
        }
    }

    /**
     * Stop processing and show completion message.
     */
    public void stopProcessing() {
        if (isProcessing) {
            isProcessing = false;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && processedCount > 0) {
                client.player.sendMessage(
                    Text.translatable("message." + ASTTweaks.MOD_ID + ".massgrindstone.completed", processedCount),
                    true
                );
            }
            ASTTweaks.LOGGER.info("MassGrindstone: Stopped processing, processed {} items", processedCount);
            processedCount = 0;
        }
    }

    /**
     * Check if the grindstone screen is currently open.
     */
    public static boolean isGrindstoneScreenOpen(MinecraftClient client) {
        return client.currentScreen instanceof GrindstoneScreen;
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            return;
        }

        if (client.player == null || client.world == null || client.isPaused()) {
            stopProcessing();
            return;
        }

        // Stop processing if grindstone screen is closed
        if (!isGrindstoneScreenOpen(client)) {
            if (isProcessing) {
                stopProcessing();
            }
            keyWasPressed = false;
            return;
        }

        // Check for key press while grindstone screen is open
        // We need to check the key directly since wasPressed() doesn't work in GUI screens
        boolean keyIsCurrentlyPressed = isExecuteKeyPressed(client);
        if (keyIsCurrentlyPressed && !keyWasPressed) {
            // Key was just pressed (rising edge)
            if (!isProcessing) {
                startProcessing();
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.translatable("message." + ASTTweaks.MOD_ID + ".massgrindstone.processing"),
                        true
                    );
                }
            }
        }
        keyWasPressed = keyIsCurrentlyPressed;

        if (!isProcessing) {
            return;
        }

        // Process multiple items per tick for speed
        int operationsPerTick = config.getOperationsPerTick();
        for (int i = 0; i < operationsPerTick; i++) {
            if (!processNextItem(client)) {
                // No more items to process
                stopProcessing();
                break;
            }
        }
    }

    /**
     * Check if the execute key is currently pressed.
     * Uses InputUtil to directly check key state, which works even in GUI screens.
     */
    private boolean isExecuteKeyPressed(MinecraftClient client) {
        if (client.getWindow() == null) {
            return false;
        }

        // Get the bound key from KeyBindings
        InputUtil.Key boundKey = KeyBindings.massGrindstoneExecute.boundKey;
        if (boundKey.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
            return false;  // Key not bound
        }

        long windowHandle = client.getWindow().getHandle();
        return InputUtil.isKeyPressed(windowHandle, boundKey.getCode());
    }

    /**
     * Process the next enchanted item.
     * @return true if an item was processed, false if no more items to process
     */
    private boolean processNextItem(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) {
            return false;
        }

        // Check if handler is correct type
        if (!(player.currentScreenHandler instanceof GrindstoneScreenHandler handler)) {
            return false;
        }

        // First, check if there's already a result waiting
        ItemStack outputStack = handler.getSlot(GRINDSTONE_OUTPUT_SLOT).getStack();
        if (!outputStack.isEmpty()) {
            // Drop the result (Ctrl+Q = drop stack)
            dropResultFromGrindstone(client, player, handler);
            processedCount++;
            return true;
        }

        // Check if input slot already has an item
        ItemStack inputStack = handler.getSlot(GRINDSTONE_INPUT_SLOT_1).getStack();
        if (!inputStack.isEmpty()) {
            // Wait for result to appear (next tick)
            return true;
        }

        // Find next enchanted item in inventory
        int slot = findNextEnchantedItem(player, handler);
        if (slot == -1) {
            return false;
        }

        // Move item to grindstone input slot (quick move / shift+click)
        quickMoveToGrindstone(client, player, handler, slot);

        return true;
    }

    /**
     * Find the next enchanted item in the player's inventory that should be processed.
     * @return The screen slot index, or -1 if no suitable item found
     */
    private int findNextEnchantedItem(PlayerEntity player, GrindstoneScreenHandler handler) {
        // Search main inventory (slots 3-29 in grindstone screen)
        for (int i = GRINDSTONE_INVENTORY_START; i < GRINDSTONE_HOTBAR_START; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (shouldProcessStack(stack)) {
                return i;
            }
        }

        // Search hotbar (slots 30-38 in grindstone screen)
        for (int i = GRINDSTONE_HOTBAR_START; i < GRINDSTONE_HOTBAR_START + 9; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (shouldProcessStack(stack)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Check if an item stack should be processed by the grindstone.
     */
    private boolean shouldProcessStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check if item is in whitelist/blacklist
        if (!config.shouldProcessItem(stack.getItem())) {
            return false;
        }

        // Check if item has removable enchantments
        return hasRemovableEnchantments(stack);
    }

    /**
     * Check if an item has enchantments that can be removed by a grindstone.
     * Items with only curses cannot be processed as grindstones don't remove curses.
     */
    private boolean hasRemovableEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);

        if (enchantments.isEmpty()) {
            return false;
        }

        // Check if there's at least one non-curse enchantment
        for (Enchantment enchantment : enchantments.keySet()) {
            if (!enchantment.isCursed()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Quick move (shift+click) an item to the grindstone input slot.
     */
    private void quickMoveToGrindstone(MinecraftClient client, PlayerEntity player,
                                        GrindstoneScreenHandler handler, int screenSlot) {
        if (client.interactionManager == null) return;

        client.interactionManager.clickSlot(
            handler.syncId,
            screenSlot,
            0,
            SlotActionType.QUICK_MOVE,
            player
        );
    }

    /**
     * Drop the result from the grindstone output slot.
     * Uses Ctrl+Q (throw stack) for efficiency.
     */
    private void dropResultFromGrindstone(MinecraftClient client, PlayerEntity player,
                                          GrindstoneScreenHandler handler) {
        if (client.interactionManager == null) return;

        if (config.shouldDropResults()) {
            // Ctrl+Q to drop the entire stack
            client.interactionManager.clickSlot(
                handler.syncId,
                GRINDSTONE_OUTPUT_SLOT,
                1,  // Button 1 with THROW = Ctrl+Q (drop stack)
                SlotActionType.THROW,
                player
            );
        } else {
            // Quick move to inventory
            client.interactionManager.clickSlot(
                handler.syncId,
                GRINDSTONE_OUTPUT_SLOT,
                0,
                SlotActionType.QUICK_MOVE,
                player
            );
        }
    }
}
