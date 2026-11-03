package com.astral.asttweaks.feature.autoeat;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import com.astral.asttweaks.util.Foods;
import com.astral.asttweaks.util.McClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Auto-eat feature that automatically consumes food when hunger is low.
 */
public class AutoEatFeature implements Feature {
    private final AutoEatConfig config;

    // 自動食事システムの状態管理
    private enum AutoEatState {
        MONITORING,      // 満腹度監視中
        EATING,          // 食事実行中
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

    // リトライ管理
    private int eatRetryCount = 0;
    private static final int MAX_EAT_RETRIES = 5;

    // COMPLETING遷移のレースコンディション防止
    private int ticksSinceStartedEating = 0;

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

    /**
     * AutoEat中かどうかを返す。MixinからstopUsingItemをキャンセルするために使用。
     */
    public boolean isAutoEating() {
        return currentState == AutoEatState.EATING && hasStartedEating;
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null || client.isPaused()) {
            if (currentState != AutoEatState.MONITORING) {
                resetToMonitoring();
            }
            return;
        }

        // GUI画面が開いている場合は処理しない（AutoRepairFeature・AutoMoveFeatureと同パターン）
        if (McClient.screen() != null) {
            if (currentState != AutoEatState.MONITORING) {
                ASTTweaks.LOGGER.debug("AutoEat: GUI screen open, resetting to MONITORING");
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

    private void processAutoEat(Minecraft client) {
        Player player = client.player;

        switch (currentState) {
            case MONITORING:
                handleMonitoring(client, player);
                break;
            case EATING:
                handleEating(client, player);
                break;
            case COMPLETING:
                handleCompleting(client, player);
                break;
        }
    }

    private void handleMonitoring(Minecraft client, Player player) {
        if (player.getFoodData().getFoodLevel() <= config.getHungerThreshold()) {
            ASTTweaks.LOGGER.debug("Hunger below threshold: {} <= {}",
                    player.getFoodData().getFoodLevel(), config.getHungerThreshold());

            if (!canEatNow(player)) {
                return;
            }

            int foodSlot = findEatableFoodInHotbar(player);
            if (foodSlot != -1) {
                startEating(client, foodSlot);
            }
        }
    }

    private boolean canEatNow(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            ASTTweaks.LOGGER.debug("AutoEat: canEatNow=false (creative or spectator)");
            return false;
        }

        if (!config.isEatWhileAction()) {
            if (player.isSprinting() || player.isShiftKeyDown() || player.isUsingItem()) {
                ASTTweaks.LOGGER.debug("AutoEat: canEatNow=false (action in progress: sprinting={}, sneaking={}, usingItem={})",
                        player.isSprinting(), player.isShiftKeyDown(), player.isUsingItem());
                return false;
            }
        } else {
            if (player.isUsingItem()) {
                ASTTweaks.LOGGER.debug("AutoEat: canEatNow=false (already using item)");
                return false;
            }
        }

        return true;
    }

    private void handleEating(Minecraft client, Player player) {
        eatingTicks++;

        McClient.setSelectedSlot(player.getInventory(), eatingSlot);

        ItemStack currentStack = player.getInventory().getItem(eatingSlot);
        if (!canEat(currentStack, player)) {
            ASTTweaks.LOGGER.debug("AutoEat: canEat() returned false during EATING, resetting");
            resetToMonitoring();
            return;
        }

        if (!hasStartedEating) {
            // 食事開始を試行
            if (!tryStartEating(client, player)) {
                eatRetryCount++;
                ASTTweaks.LOGGER.debug("AutoEat: tryStartEating failed (retry {}/{})",
                        eatRetryCount, MAX_EAT_RETRIES);
                if (eatRetryCount >= MAX_EAT_RETRIES) {
                    ASTTweaks.LOGGER.warn("AutoEat: max retries reached, backing off");
                    resetToMonitoring();
                    eatCooldown = 60; // 3秒バックオフ
                }
            }
            return;
        }

        // hasStartedEating == true: Mixinが releaseUsingItem() をキャンセルして食事を維持
        ticksSinceStartedEating++;

        if (player.isUsingItem()) {
            // 食事進行中: アイテム消費 or 満腹度上昇で完了を検出
            int currentHunger = player.getFoodData().getFoodLevel();
            if ((itemCountBeforeEating != -1 && currentStack.getCount() < itemCountBeforeEating)
                    || (currentHunger > hungerBeforeEating)) {
                ASTTweaks.LOGGER.debug("AutoEat: eating completed (item consumed or hunger restored), transitioning to COMPLETING");
                currentState = AutoEatState.COMPLETING;
                eatingTicks = 0;
                return;
            }
        } else {
            // isUsingItem() == false: 食事がまだ反映されていないか、キャンセルされた
            if (ticksSinceStartedEating <= 3) {
                ASTTweaks.LOGGER.debug("AutoEat: grace period tick {}/3, waiting for isUsingItem()",
                        ticksSinceStartedEating);
                return;
            }

            // 猶予期間後: 食事の証拠を確認
            int currentHunger = player.getFoodData().getFoodLevel();
            boolean itemConsumed = itemCountBeforeEating != -1
                    && currentStack.getCount() < itemCountBeforeEating;
            boolean hungerRestored = currentHunger > hungerBeforeEating;

            if (itemConsumed || hungerRestored) {
                ASTTweaks.LOGGER.debug("AutoEat: eating evidence found (itemConsumed={}, hungerRestored={}), transitioning to COMPLETING",
                        itemConsumed, hungerRestored);
                currentState = AutoEatState.COMPLETING;
                eatingTicks = 0;
            } else {
                // 食事が実際には完了していない → リトライ
                eatRetryCount++;
                ASTTweaks.LOGGER.debug("AutoEat: no eating evidence after grace period, retrying (retry {}/{})",
                        eatRetryCount, MAX_EAT_RETRIES);
                if (eatRetryCount >= MAX_EAT_RETRIES) {
                    ASTTweaks.LOGGER.warn("AutoEat: max retries reached after grace period, backing off");
                    resetToMonitoring();
                    eatCooldown = 60;
                } else {
                    hasStartedEating = false;
                    ticksSinceStartedEating = 0;
                }
            }
            return;
        }

        if (eatingTicks >= MAX_EATING_TICKS) {
            ASTTweaks.LOGGER.warn("AutoEat: eating timed out after {} ticks", MAX_EATING_TICKS);
            resetToMonitoring();
        }
    }

    private void handleCompleting(Minecraft client, Player player) {
        eatingTicks++;

        int currentHunger = player.getFoodData().getFoodLevel();
        if (currentHunger > config.getHungerThreshold()) {
            finishEating(client);
        } else if (eatingTicks >= 60) {
            resetToMonitoring();
        }
    }

    private int findEatableFoodInHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && canEat(stack, player)) {
                ASTTweaks.LOGGER.debug("AutoEat: found eatable food in hotbar slot {}: {}",
                        i, stack.getItem());
                return i;
            }
        }
        ASTTweaks.LOGGER.debug("AutoEat: no eatable food found in hotbar");
        return -1;
    }

    private void startEating(Minecraft client, int slot) {
        Player player = client.player;

        ASTTweaks.LOGGER.debug("AutoEat: transitioning MONITORING -> EATING (slot={}, hunger={})",
                slot, player.getFoodData().getFoodLevel());

        currentState = AutoEatState.EATING;
        originalHotbarSlot = McClient.selectedSlot(player.getInventory());
        eatingSlot = slot;
        hungerBeforeEating = player.getFoodData().getFoodLevel();
        itemCountBeforeEating = player.getInventory().getItem(slot).getCount();
        eatingTicks = 0;
        hasStartedEating = false;
        eatRetryCount = 0;
        ticksSinceStartedEating = 0;

        McClient.setSelectedSlot(player.getInventory(), slot);
    }


    private boolean tryStartEating(Minecraft client, Player player) {
        if (client.gameMode == null) {
            ASTTweaks.LOGGER.debug("AutoEat: tryStartEating failed (interactionManager is null)");
            return false;
        }

        InteractionResult result = client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        ASTTweaks.LOGGER.debug("AutoEat: interactItem result={}", result);
        if (result.consumesAction()) {
            hasStartedEating = true;
            ticksSinceStartedEating = 0;
            return true;
        }

        return false;
    }

    private void finishEating(Minecraft client) {
        resetToMonitoring();
        eatCooldown = 60;
    }

    private void resetToMonitoring() {
        // スロット復元
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && originalHotbarSlot != -1) {
            McClient.setSelectedSlot(client.player.getInventory(), originalHotbarSlot);
        }
        currentState = AutoEatState.MONITORING;
        originalHotbarSlot = -1;
        eatingSlot = -1;
        hungerBeforeEating = -1;
        itemCountBeforeEating = -1;
        eatingTicks = 0;
        hasStartedEating = false;
        eatRetryCount = 0;
        ticksSinceStartedEating = 0;
    }

    private boolean canEat(ItemStack stack, Player player) {
        Item item = stack.getItem();
        if (config.isBlacklisted(item)) {
            ASTTweaks.LOGGER.debug("AutoEat: canEat=false (item {} is blacklisted)", item);
            return false;
        }
        FoodProperties food = Foods.of(stack);
        if (food == null) {
            ASTTweaks.LOGGER.debug("AutoEat: canEat=false (item {} has no food component)", item);
            return false;
        }
        boolean canConsume = player.canEat(food.canAlwaysEat());
        if (!canConsume) {
            ASTTweaks.LOGGER.debug("AutoEat: canEat=false (player cannot consume, alwaysEdible={})", food.canAlwaysEat());
        }
        return canConsume;
    }
}
