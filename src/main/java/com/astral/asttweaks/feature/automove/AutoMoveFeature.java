package com.astral.asttweaks.feature.automove;

import com.astral.asttweaks.ASTTweaks;
import com.astral.asttweaks.feature.Feature;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

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
                    KeyBinding key = getKeyForDirection(client, config.getDirection());
                    if (key != null) {
                        key.setPressed(false);
                    }
                }
                wasFreecamActive = true;
                return;
            }
            wasFreecamActive = false;
            // 選択された方向のキーを押す
            KeyBinding key = getKeyForDirection(client, config.getDirection());
            if (key != null) {
                key.setPressed(true);
            }
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

    private KeyBinding getKeyForDirection(MinecraftClient client, MoveDirection dir) {
        return switch (dir) {
            case FORWARD -> client.options.forwardKey;
            case BACKWARD -> client.options.backKey;
            case LEFT -> client.options.leftKey;
            case RIGHT -> client.options.rightKey;
        };
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
                KeyBinding key = getKeyForDirection(client, config.getDirection());
                if (key != null) {
                    key.setPressed(false);
                }
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
                KeyBinding key = getKeyForDirection(client, direction);
                if (key != null) {
                    key.setPressed(false);
                }
            }
            isMoving = false;
        } else {
            // 別の方向 or 停止中 → 現在の方向をリリースして新しい方向で開始
            if (isMoving && client != null) {
                KeyBinding key = getKeyForDirection(client, config.getDirection());
                if (key != null) {
                    key.setPressed(false);
                }
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
