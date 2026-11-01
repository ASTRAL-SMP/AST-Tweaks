package com.astral.asttweaks.feature.updatechecker;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.translatable("message.asttweaks.updatechecker.chat.current", currentVersion)
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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        // Schedule on the main thread
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.translatable("message.asttweaks.updatechecker.chat.available", newVersion)
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
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.translatable("message.asttweaks.updatechecker.chat.available_named", modName, newVersion)
                );
            }
        });
    }

    /**
     * Show a notification that no update is available (for manual checks).
     */
    public static void showUpToDate() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.translatable("message.asttweaks.updatechecker.chat.uptodate")
                );
            }
        });
    }

    /**
     * Show an error notification.
     */
    public static void showError() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.translatable("message.asttweaks.updatechecker.chat.error")
                );
            }
        });
    }
}
