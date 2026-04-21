package com.astral.asttweaks.feature.automove;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-move feature that automatically holds movement keys.
 */
public class AutoMoveFeature implements Feature {
    private final AutoMoveConfig config;
    private boolean isMoving = false;
    private boolean wasFreecamActive = false;

    public AutoMoveFeature() {
        this.config = new AutoMoveConfig();
    }

    @Override
    public String getId() {
        return "automove";
    }

    @Override
    public String getName() {
        return "Auto Move";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ASTTweaks.LOGGER.info("AutoMove feature initialized");
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

    public AutoMoveConfig getConfig() {
        return config;
    }

    private void onClientTick(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        if (isEnabled() && isMoving) {
            boolean freecam = isFreecamActive();
            if (freecam) {
                // freecam開始時のみKeyBindをリリースして残留状態をクリアする
                // 以降は物理キー操作がそのまま反映される
                // プレイヤー本体の移動はMixinで直接入力を注入する
                if (!wasFreecamActive) {
                    releaseKeys(client, config.getDirection());
                }
                wasFreecamActive = true;
                return;
            }
            wasFreecamActive = false;
            // 選択された方向のキーを押す（斜めは2キー同時）
            pressKeys(client, config.getDirection());
        } else {
            wasFreecamActive = false;
        }
    }

    /**
     * freecamが有効かどうかを判定する。
     * カメラエンティティがプレイヤーと異なる場合にfreecamとみなす。
     */
    public boolean isFreecamActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && client.getCameraEntity() != client.player;
    }

    private List<KeyBinding> getKeysForDirection(MinecraftClient client, MoveDirection dir) {
        List<KeyBinding> keys = new ArrayList<>(2);
        int fwd = dir.getForwardAxis();
        int side = dir.getSidewaysAxis();
        if (fwd > 0) keys.add(client.options.forwardKey);
        else if (fwd < 0) keys.add(client.options.backKey);
        if (side > 0) keys.add(client.options.leftKey);
        else if (side < 0) keys.add(client.options.rightKey);
        return keys;
    }

    private void pressKeys(MinecraftClient client, MoveDirection dir) {
        for (KeyBinding key : getKeysForDirection(client, dir)) {
            key.setPressed(true);
        }
    }

    private void releaseKeys(MinecraftClient client, MoveDirection dir) {
        for (KeyBinding key : getKeysForDirection(client, dir)) {
            key.setPressed(false);
        }
    }

    /**
     * Toggle auto-move state.
     */
    public void toggle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isMoving) {
            // オンにする
            isMoving = true;
        } else {
            // オフにする前に現在の方向のキーをリリース
            if (client != null) {
                releaseKeys(client, config.getDirection());
            }
            isMoving = false;
        }
    }

    /**
     * 方向を指定してトグル。
     * 同じ方向なら停止、別の方向なら切り替え、停止中なら開始。
     */
    public void toggleWithDirection(MoveDirection direction) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (isMoving && config.getDirection() == direction) {
            // 同じ方向 → 停止
            if (client != null) {
                releaseKeys(client, direction);
            }
            isMoving = false;
        } else {
            // 別の方向 or 停止中 → 現在の方向をリリースして新しい方向で開始
            if (isMoving && client != null) {
                releaseKeys(client, config.getDirection());
            }
            config.setDirection(direction);
            isMoving = true;
        }
    }

    /**
     * Check if currently auto-moving.
     */
    public boolean isMoving() {
        return isMoving;
    }
}
