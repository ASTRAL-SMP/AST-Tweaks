package com.astral.asttweaks.feature.autoeat;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Auto-eat feature that automatically consumes food when hunger is low.
 */
public class AutoEatFeature implements Feature {
    private final AutoEatConfig config;

    // 自動食事システムの状態管理
    private enum AutoEatState {
        MONITORING,      // 満腹度監視中
        EATING,          // 食事実行中
        WAIT_FOR_SERVER, // サーバー反映待ち
        COMPLETING       // 食事完了確認中
    }

    private AutoEatState currentState = AutoEatState.MONITORING;
    private int tickCounter = 0;
    private int eatCooldown = 0;

    // 食事実行時の状態保存
    private int originalHotbarSlot = -1;
    private int eatingSlot = -1;
    private int hungerBeforeEating = -1;
    private int eatingTicks = 0;
    private int itemCountBeforeEating = -1;
    private boolean hasStartedEating = false;
    private static final int MAX_EATING_TICKS = 200; // 最大10秒間食事を待機
    private int waitForServerTicks = 0;
    private static final int MAX_WAIT_FOR_SERVER_TICKS = 3;
    private boolean isHoldingUseKey = false;

    public AutoEatFeature() {
        this.config = new AutoEatConfig();
    }

    @Override
    public String getId() {
        return "autoeat";
    }

    @Override
    public String getName() {
        return "Auto Eat";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("AutoEat feature initialized");
    }

    @Override
    public void tick() {
        // tick処理はClientTickEventsで行う
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        config.setEnabled(enabled);
    }

    public AutoEatConfig getConfig() {
        return config;
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.isPaused()) {
            if (currentState != AutoEatState.MONITORING) {
                resetToMonitoring();
            }
            return;
        }

        if (eatCooldown > 0) {
            eatCooldown--;
        }

        tickCounter++;
        if (tickCounter >= 1) {
            tickCounter = 0;
            if (config.isEnabled() && eatCooldown == 0) {
                processAutoEat(client);
            }
        }
    }

    private void processAutoEat(MinecraftClient client) {
        PlayerEntity player = client.player;

        switch (currentState) {
            case MONITORING:
                handleMonitoring(client, player);
                break;
            case EATING:
                handleEating(client, player);
                break;
            case WAIT_FOR_SERVER:
                handleWaitForServer(client, player);
                break;
            case COMPLETING:
                handleCompleting(client, player);
                break;
        }
    }

    private void handleMonitoring(MinecraftClient client, PlayerEntity player) {
        if (player.getHungerManager().getFoodLevel() <= config.getHungerThreshold()) {
            ASTTweaks.LOGGER.debug("Hunger below threshold: {} <= {}",
                    player.getHungerManager().getFoodLevel(), config.getHungerThreshold());

            if (!canEatNow(player)) {
                return;
            }

            int foodSlot = findEatableFoodInHotbar(player);
            if (foodSlot != -1) {
                startEating(client, foodSlot);
            }
        }
    }

    private boolean canEatNow(PlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }

        if (!config.isEatWhileAction()) {
            if (player.isSprinting() || player.isSneaking() || player.isUsingItem()) {
                return false;
            }
        } else {
            if (player.isUsingItem()) {
                return false;
            }
        }

        return true;
    }

    private void handleEating(MinecraftClient client, PlayerEntity player) {
        eatingTicks++;

        if (!isHoldingUseKey) {
            client.options.useKey.setPressed(true);
            isHoldingUseKey = true;
        }

        ItemStack currentStack = player.getInventory().getStack(eatingSlot);
        int currentHunger = player.getHungerManager().getFoodLevel();
        if ((itemCountBeforeEating != -1 && currentStack.getCount() < itemCountBeforeEating)
                || (currentHunger > hungerBeforeEating)) {
            client.options.useKey.setPressed(false);
            isHoldingUseKey = false;
            currentState = AutoEatState.COMPLETING;
            eatingTicks = 0;
            return;
        }

        if (eatingTicks >= MAX_EATING_TICKS) {
            ASTTweaks.LOGGER.warn("Eating timed out");
            client.options.useKey.setPressed(false);
            isHoldingUseKey = false;
            resetToMonitoring();
        }
    }

    private void handleWaitForServer(MinecraftClient client, PlayerEntity player) {
        waitForServerTicks++;
        ItemStack currentStack = player.getInventory().getStack(eatingSlot);
        int currentHunger = player.getHungerManager().getFoodLevel();

        if ((itemCountBeforeEating != -1 && currentStack.getCount() < itemCountBeforeEating)
                || (currentHunger > hungerBeforeEating)) {
            currentState = AutoEatState.COMPLETING;
            eatingTicks = 0;
            return;
        }
        if (waitForServerTicks > MAX_WAIT_FOR_SERVER_TICKS) {
            hasStartedEating = false;
            currentState = AutoEatState.EATING;
        }
    }

    private void handleCompleting(MinecraftClient client, PlayerEntity player) {
        eatingTicks++;

        int currentHunger = player.getHungerManager().getFoodLevel();
        if (currentHunger > config.getHungerThreshold()) {
            finishEating(client);
        } else if (eatingTicks >= 60) {
            resetToMonitoring();
        }
    }

    private int findEatableFoodInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && canEat(stack, player)) {
                return i;
            }
        }
        return -1;
    }

    private void startEating(MinecraftClient client, int slot) {
        PlayerEntity player = client.player;

        currentState = AutoEatState.EATING;
        originalHotbarSlot = player.getInventory().selectedSlot;
        eatingSlot = slot;
        hungerBeforeEating = player.getHungerManager().getFoodLevel();
        itemCountBeforeEating = player.getInventory().getStack(slot).getCount();
        eatingTicks = 0;
        hasStartedEating = false;

        player.getInventory().selectedSlot = slot;
    }

    private void finishEating(MinecraftClient client) {
        if (client.player != null && originalHotbarSlot != -1) {
            client.player.getInventory().selectedSlot = originalHotbarSlot;
        }
        client.options.useKey.setPressed(false);
        isHoldingUseKey = false;
        resetToMonitoring();
        eatCooldown = 60;
    }

    private void resetToMonitoring() {
        currentState = AutoEatState.MONITORING;
        originalHotbarSlot = -1;
        eatingSlot = -1;
        hungerBeforeEating = -1;
        itemCountBeforeEating = -1;
        eatingTicks = 0;
        hasStartedEating = false;
        isHoldingUseKey = false;
    }

    private boolean canEat(ItemStack stack, PlayerEntity player) {
        Item item = stack.getItem();
        if (config.isBlacklisted(item)) {
            return false;
        }
        FoodComponent food = item.getFoodComponent();
        if (food == null) {
            return false;
        }
        return player.canConsume(food.isAlwaysEdible());
    }
}
