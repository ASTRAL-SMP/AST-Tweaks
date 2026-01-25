package com.astral.asttweaks.feature.updatechecker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Handles displaying update notifications using chat messages.
 */
public class UpdateNotification {

    /**
     * Show the current version notification.
     *
     * @param currentVersion The current version string
     */
    public static void showCurrentVersion(String currentVersion) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.asttweaks.updatechecker.chat.current", currentVersion),
                        false
                );
            }
        });
    }

    /**
     * Show an update available notification.
     *
     * @param newVersion The new version string
     * @param currentVersion The current version string
     */
    public static void show(String newVersion, String currentVersion) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        // Schedule on the main thread
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.asttweaks.updatechecker.chat.available", newVersion),
                        false  // false = チャット欄に表示
                );
            }
        });
    }

    /**
     * Show an update available notification with mod name.
     *
     * @param modName The mod name
     * @param newVersion The new version string
     */
    public static void showWithModName(String modName, String newVersion) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.asttweaks.updatechecker.chat.available_named", modName, newVersion),
                        false
                );
            }
        });
    }

    /**
     * Show a notification that no update is available (for manual checks).
     */
    public static void showUpToDate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.asttweaks.updatechecker.chat.uptodate"),
                        false
                );
            }
        });
    }

    /**
     * Show an error notification.
     */
    public static void showError() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.asttweaks.updatechecker.chat.error"),
                        false
                );
            }
        });
    }
}
