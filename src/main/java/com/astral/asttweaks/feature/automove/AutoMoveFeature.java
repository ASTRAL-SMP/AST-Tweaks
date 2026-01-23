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
            // 選択された方向のキーを押す
            KeyBinding key = getKeyForDirection(client, config.getDirection());
            if (key != null) {
                key.setPressed(true);
            }
        }
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
     * Check if currently auto-moving.
     */
    public boolean isMoving() {
        return isMoving;
    }
}
