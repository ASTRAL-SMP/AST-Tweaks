package com.astral.asttweaks.feature.autodrop;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.config.ModConfig;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Auto Drop feature.
 * モードに応じて2通りの発動方式をサポート:
 *  - EXECUTE_KEY: 任意のコンテナ画面で実行キー押下時にドロップ
 *  - ON_INVENTORY_OPEN: 任意のコンテナ画面（プレイヤーインベントリ含む）を開いた瞬間に自動ドロップ
 * - アーマー4スロット（PlayerInventory 36-39）は常に保護
 * - 保護対象スロット（Main/Hotbar/Offhand の個別スロット）はドロップしない
 * - 除外アイテムリストに含まれるアイテムはドロップしない
 * - operationsPerTick でレート制限
 */
public class AutoDropFeature implements Feature {
    private final AutoDropConfig config;
    private final Deque<Integer> dropQueue = new ArrayDeque<>();
    private boolean wasExecuteKeyDown = false;
    private boolean wasContainerOpen = false;
    private boolean triggeredThisSession = false;

    public AutoDropFeature() {
        this.config = new AutoDropConfig();
    }

    @Override
    public String getId() { return "autodrop"; }

    @Override
    public String getName() { return "Auto Drop"; }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("Auto Drop feature initialized");
    }

    private void onClientTick(MinecraftClient client) {
        if (!config.isEnabled()) {
            reset();
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) {
            reset();
            return;
        }

        AutoDropMode mode = config.getMode();
        boolean isContainerOpen = client.currentScreen instanceof HandledScreen<?>;

        if (mode == AutoDropMode.EXECUTE_KEY) {
            if (isContainerOpen && client.getWindow() != null) {
                long window = client.getWindow().getHandle();
                boolean executeDown = ModConfig.getInstance().autoDropExecuteKey.isPressed(window);
                if (executeDown && !wasExecuteKeyDown) {
                    enqueueDrops(client);
                }
                wasExecuteKeyDown = executeDown;
            } else {
                wasExecuteKeyDown = false;
            }
        } else { // ON_INVENTORY_OPEN
            if (isContainerOpen && !wasContainerOpen && !triggeredThisSession) {
                enqueueDrops(client);
                triggeredThisSession = true;
            }
            if (!isContainerOpen) {
                triggeredThisSession = false;
            }
            wasContainerOpen = isContainerOpen;
            wasExecuteKeyDown = false;
        }

        if (isContainerOpen && !dropQueue.isEmpty()) {
            processQueue(client);
        }

        if (!isContainerOpen) {
            dropQueue.clear();
        }
    }

    private void reset() {
        dropQueue.clear();
        wasExecuteKeyDown = false;
        wasContainerOpen = false;
        triggeredThisSession = false;
    }

    private void enqueueDrops(MinecraftClient client) {
        ScreenHandler handler = client.player.currentScreenHandler;
        dropQueue.clear();

        // ハンドラレイアウトに依存せず、PlayerInventory に属するスロットのみを対象にする。
        // PlayerInventory index の意味は固定: 0-8=Hotbar, 9-35=Main, 36-39=Armor, 40=Offhand。
        // コンテナ側スロット（Chest, Crafting grid 等）は slot.inventory が PlayerInventory ではないため除外される。
        for (int screenSlot = 0; screenSlot < handler.slots.size(); screenSlot++) {
            Slot slot = handler.getSlot(screenSlot);
            if (!(slot.inventory instanceof PlayerInventory)) continue;

            int invIndex = slot.getIndex();
            if (invIndex >= 36 && invIndex <= 39) continue; // armor: always protected

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            if (config.isSlotProtected(invIndex)) continue;

            String itemId = Registries.ITEM.getId(stack.getItem()).toString();
            if (config.isItemExcluded(itemId)) continue;

            dropQueue.add(screenSlot);
        }

        if (!dropQueue.isEmpty()) {
            client.player.sendMessage(
                    Text.translatable("message." + ASTTweaks.MOD_ID + ".autodrop.started", dropQueue.size()),
                    true);
        }
    }

    private void processQueue(MinecraftClient client) {
        int limit = config.getOperationsPerTick();
        int syncId = client.player.currentScreenHandler.syncId;

        for (int i = 0; i < limit && !dropQueue.isEmpty(); i++) {
            int screenSlot = dropQueue.poll();
            // button=1 in THROW drops the full stack (vanilla Ctrl+Q equivalent)
            client.interactionManager.clickSlot(syncId, screenSlot, 1, SlotActionType.THROW, client.player);
        }
    }

    @Override public void tick() {}
    @Override public boolean isEnabled() { return config.isEnabled(); }
    @Override public void setEnabled(boolean enabled) { config.setEnabled(enabled); }
    public AutoDropConfig getConfig() { return config; }
}
