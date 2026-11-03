package com.astral.asttweaks.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Minecraft クライアント API のバージョン差分を吸収する。
 * 26.2 で画面管理が Minecraft から Gui に、HUD の表示状態とチャットが Hud に移った。
 */
public final class McClient {
    private McClient() {}

    public static Screen screen() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen();
        *///?} else {
        return Minecraft.getInstance().screen;
        //?}
    }

    public static void setScreen(Screen screen) {
        //? if >=26.2 {
        /*Minecraft.getInstance().gui.setScreen(screen);
        *///?} else {
        Minecraft.getInstance().setScreen(screen);
        //?}
    }

    public static boolean isHudHidden() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.isHidden();
        *///?} else {
        return Minecraft.getInstance().options.hideGui;
        //?}
    }

    public static ChatComponent chat() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.getChat();
        *///?} else {
        return Minecraft.getInstance().gui.getChat();
        //?}
    }

    /** 全チャンクの再構築を要求する */
    public static void reloadLevelRenderer() {
        //? if >=26.2 {
        /*Minecraft.getInstance().levelExtractor.allChanged();
        *///?} else {
        Minecraft.getInstance().levelRenderer.allChanged();
        //?}
    }

    /** アクションバー（ホットバー上）にメッセージを出す */
    public static void overlay(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    public static int selectedSlot(Inventory inventory) {
        //? if >=26.1 {
        return inventory.getSelectedSlot();
        //?} else {
        /*return inventory.selected;
        *///?}
    }

    public static void setSelectedSlot(Inventory inventory, int slot) {
        //? if >=26.1 {
        inventory.setSelectedSlot(slot);
        //?} else {
        /*inventory.selected = slot;
        *///?}
    }
}
