package com.astral.asttweaks.feature.autorepair;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.compat.TweakerooCompat;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Set;

/**
 * Fast auto repair feature (Tweakeroo style).
 * Uses experience bottles from offhand to repair items in mainhand at high speed.
 */
public class AutoRepairFeature implements Feature {
    private final AutoRepairConfig config;

    // State machine states
    private enum State {
        IDLE,               // Waiting for activation / checking conditions
        SETUP_OFFHAND,      // Moving experience bottle to offhand
        SETUP_MAINHAND,     // Moving repair item to mainhand
        REPAIRING,          // Fast using experience bottles
        SWAP_NEXT_ITEM      // Swapping to next repair item
    }

    private State currentState = State.IDLE;
    private int delayTicks = 0;
    private int currentRepairSlot = -1;      // Original slot of item being repaired
    private int previousMainhandSlot = -1;   // Player's original selected slot before repair
    private Set<Integer> repairedSlots = new HashSet<>();  // Slots that have been repaired this session
    private static final int SYNC_DELAY_TICKS = 1;  // Minimal delay for fast swapping

    public AutoRepairFeature() {
        this.config = new AutoRepairConfig();
    }

    @Override
    public String getId() {
        return "autorepair";
    }

    @Override
    public String getName() {
        return "Auto Repair";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("AutoRepair feature initialized (Fast mode)");
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
            TweakerooCompat.ensureRestored();
            resetState();
        }
    }

    public AutoRepairConfig getConfig() {
        return config;
    }

    /**
     * Get the target hotbar slot from config.
     */
    private int getTargetSlot() {
        return config.getTargetSlot();
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            return;
        }

        if (client.player == null || client.world == null || client.isPaused()) {
            resetState();
            return;
        }

        // Don't process if in a GUI screen (except chat)
        if (client.currentScreen != null) {
            return;
        }

        processStateMachine(client);
    }

    private void processStateMachine(MinecraftClient client) {
        PlayerEntity player = client.player;

        switch (currentState) {
            case IDLE:
                // Check if we have experience bottles and items needing repair
                if (hasExperienceBottle(player) && findNextRepairItem(player) != -1) {
                    previousMainhandSlot = player.getInventory().selectedSlot;
                    // Disable Tweakeroo's AlmostBrokenTools to prevent interference
                    TweakerooCompat.disableAlmostBrokenTools();
                    currentState = State.SETUP_OFFHAND;
                    ASTTweaks.LOGGER.info("Starting fast repair sequence, previousSlot: {}", previousMainhandSlot);
                }
                break;

            case SETUP_OFFHAND:
                // Ensure experience bottle is in offhand
                if (!hasExperienceBottleInOffhand(player)) {
                    if (!moveExperienceBottleToOffhand(client, player)) {
                        // No bottle available, stop
                        resetState();
                        return;
                    }
                    delayTicks = SYNC_DELAY_TICKS;
                } else {
                    delayTicks = 0;
                }
                currentState = State.SETUP_MAINHAND;
                break;

            case SETUP_MAINHAND:
                if (delayTicks > 0) {
                    delayTicks--;
                    return;
                }

                // Find next item to repair and move to target slot
                int repairSlot = findNextRepairItem(player);
                if (repairSlot == -1) {
                    // All items repaired
                    finishRepair(player);
                    return;
                }

                currentRepairSlot = repairSlot;
                int targetSlot = getTargetSlot();
                ASTTweaks.LOGGER.info("State: SETUP_MAINHAND, repairSlot: {}, targetSlot: {}", repairSlot, targetSlot);

                // Move item to target slot if not already there
                if (repairSlot != targetSlot) {
                    moveItemToMainhand(client, player, repairSlot);
                    delayTicks = SYNC_DELAY_TICKS;
                }

                // Select target slot
                player.getInventory().selectedSlot = targetSlot;
                currentState = State.REPAIRING;
                break;

            case REPAIRING:
                if (delayTicks > 0) {
                    delayTicks--;
                    return;
                }

                // Check if current item is fully repaired
                if (isCurrentItemFullyRepaired(player)) {
                    currentState = State.SWAP_NEXT_ITEM;
                    return;
                }

                // Check if we still have experience bottles
                if (!hasExperienceBottleInOffhand(player)) {
                    // Try to refill offhand
                    if (!moveExperienceBottleToOffhand(client, player)) {
                        // No more bottles, finish
                        finishRepair(player);
                        return;
                    }
                    delayTicks = SYNC_DELAY_TICKS;
                    return;
                }

                // Fast use experience bottles from offhand
                fastUseOffhand(client, player);
                break;

            case SWAP_NEXT_ITEM:
                int targetSlotSwap = getTargetSlot();

                // Mark current slot as repaired
                if (currentRepairSlot != -1) {
                    repairedSlots.add(currentRepairSlot);
                    ASTTweaks.LOGGER.info("Marked slot {} as repaired", currentRepairSlot);
                }

                // Return current item to its original slot if it was moved
                if (currentRepairSlot != targetSlotSwap && currentRepairSlot != -1) {
                    // Swap back
                    swapSlots(client, player, targetSlotSwap, currentRepairSlot);
                    delayTicks = SYNC_DELAY_TICKS;
                }

                currentRepairSlot = -1;
                currentState = State.SETUP_MAINHAND;
                break;
        }
    }

    /**
     * Fast use experience bottles from offhand (multiple times per tick).
     */
    private void fastUseOffhand(MinecraftClient client, PlayerEntity player) {
        if (client.interactionManager == null) return;

        int clicks = config.getClicksPerTick();
        for (int i = 0; i < clicks; i++) {
            // Check if we still have bottles before each use
            if (!hasExperienceBottleInOffhand(player)) {
                break;
            }
            client.interactionManager.interactItem(player, Hand.OFF_HAND);
        }
    }

    /**
     * Check if player has experience bottle anywhere in inventory.
     */
    private boolean hasExperienceBottle(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();

        // Check offhand first
        if (inv.offHand.get(0).getItem() == Items.EXPERIENCE_BOTTLE) {
            return true;
        }

        // Check main inventory
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if offhand contains experience bottle.
     */
    private boolean hasExperienceBottleInOffhand(PlayerEntity player) {
        return player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
    }

    /**
     * Move experience bottle to offhand.
     */
    private boolean moveExperienceBottleToOffhand(MinecraftClient client, PlayerEntity player) {
        if (client.interactionManager == null) return false;

        PlayerInventory inv = player.getInventory();

        // Find experience bottle in inventory
        int bottleSlot = -1;
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).getItem() == Items.EXPERIENCE_BOTTLE) {
                bottleSlot = i;
                break;
            }
        }

        if (bottleSlot == -1) {
            return false;
        }

        // Convert inventory slot to screen slot
        // Hotbar slots 0-8 -> screen slots 36-44
        // Main inventory 9-35 -> screen slots 9-35
        int screenSlot = bottleSlot < 9 ? bottleSlot + 36 : bottleSlot;

        // Offhand slot is 45 in player inventory screen
        int offhandScreenSlot = 45;

        // Swap bottle with offhand using SWAP action
        // First pick up the bottle
        client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                screenSlot,
                0,
                SlotActionType.PICKUP,
                player
        );

        // Then put it in offhand
        client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                offhandScreenSlot,
                0,
                SlotActionType.PICKUP,
                player
        );

        // If there was something in offhand, put it back
        client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId,
                screenSlot,
                0,
                SlotActionType.PICKUP,
                player
        );

        ASTTweaks.LOGGER.info("Moved experience bottle to offhand from slot {}", bottleSlot);
        return true;
    }

    /**
     * Find next item that needs repair in inventory.
     * Returns the inventory slot, or -1 if none found.
     * Excludes slots that have already been repaired this session.
     */
    private int findNextRepairItem(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();

        // Check hotbar first (slots 0-8)
        for (int i = 0; i < 9; i++) {
            if (!repairedSlots.contains(i) && needsRepair(inv.getStack(i))) {
                return i;
            }
        }

        // Then check main inventory (slots 9-35)
        for (int i = 9; i < 36; i++) {
            if (!repairedSlots.contains(i) && needsRepair(inv.getStack(i))) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Check if an item needs repair.
     */
    private boolean needsRepair(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Check if item is damageable
        if (!stack.isDamageable()) {
            return false;
        }

        // Check if item has Mending
        if (!hasMending(stack)) {
            return false;
        }

        // Check if item is in the repair list (whitelist/blacklist)
        if (!config.shouldRepairItem(stack.getItem())) {
            return false;
        }

        // Check if item has any damage
        return stack.getDamage() > 0;
    }

    /**
     * Check if an item has the Mending enchantment.
     */
    private boolean hasMending(ItemStack stack) {
        return EnchantmentHelper.getLevel(Enchantments.MENDING, stack) > 0;
    }

    /**
     * Check if current mainhand item is fully repaired (damage == 0).
     */
    private boolean isCurrentItemFullyRepaired(PlayerEntity player) {
        ItemStack mainhand = player.getMainHandStack();
        if (mainhand.isEmpty() || !mainhand.isDamageable()) {
            return true;
        }
        return mainhand.getDamage() == 0;
    }

    /**
     * Move item from given slot to target slot.
     */
    private void moveItemToMainhand(MinecraftClient client, PlayerEntity player, int fromSlot) {
        int targetSlot = getTargetSlot();
        swapSlots(client, player, fromSlot, targetSlot);
        ASTTweaks.LOGGER.info("Moving item from slot {} to target slot {}", fromSlot, targetSlot);
    }

    /**
     * Swap two inventory slots.
     */
    private void swapSlots(MinecraftClient client, PlayerEntity player, int slot1, int slot2) {
        if (client.interactionManager == null) return;

        // Use number key swap for hotbar slots
        if (slot2 < 9) {
            // Convert slot1 to screen slot
            int screenSlot1 = slot1 < 9 ? slot1 + 36 : slot1;

            // SWAP action with hotbar key
            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    screenSlot1,
                    slot2,
                    SlotActionType.SWAP,
                    player
            );
        } else if (slot1 < 9) {
            // Swap other way
            int screenSlot2 = slot2 < 9 ? slot2 + 36 : slot2;

            client.interactionManager.clickSlot(
                    player.currentScreenHandler.syncId,
                    screenSlot2,
                    slot1,
                    SlotActionType.SWAP,
                    player
            );
        }
    }

    /**
     * Finish repair and restore player's original state.
     */
    private void finishRepair(PlayerEntity player) {
        // Restore original selected slot
        if (previousMainhandSlot != -1 && previousMainhandSlot != player.getInventory().selectedSlot) {
            player.getInventory().selectedSlot = previousMainhandSlot;
        }
        // Restore Tweakeroo AlmostBrokenTools if it was disabled
        TweakerooCompat.restoreAlmostBrokenTools();
        ASTTweaks.LOGGER.info("Fast repair completed, repaired {} slots", repairedSlots.size());
        resetState();
    }

    private void resetState() {
        // Ensure Tweakeroo setting is restored if we're resetting due to interruption
        TweakerooCompat.ensureRestored();
        currentState = State.IDLE;
        delayTicks = 0;
        currentRepairSlot = -1;
        previousMainhandSlot = -1;
        repairedSlots.clear();
    }
}
