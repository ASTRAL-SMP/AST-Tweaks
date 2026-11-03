package com.astral.asttweaks.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
//? if >=26.3 {
/*import net.minecraft.client.MouseHandler;
*///?} else {
import org.lwjgl.glfw.GLFW;
//?}

/**
 * キー・マウスボタンの押下状態の取得。
 * 26.3 で GLFW が SDL に置き換わり、キーコードの体系（InputConstants.KEY_*）も変わった。
 * 設定に保存されるコードはそのバージョンのネイティブ値。
 */
public final class KeyInput {
    private KeyInput() {}

    public static boolean isKeyDown(int code) {
        if (code < 0) return false;
        //? if >=26.3 {
        /*return InputConstants.isKeyDown(code);
        *///?} else if >=26.1 {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), code);
        //?} else {
        /*return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), code);
        *///?}
    }

    public static boolean isMouseDown(int button) {
        if (button < 0) return false;
        //? if >=26.3 {
        /*MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        if (button == InputConstants.MOUSE_BUTTON_LEFT) return mouse.isLeftPressed();
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) return mouse.isRightPressed();
        if (button == InputConstants.MOUSE_BUTTON_MIDDLE) return mouse.isMiddlePressed();
        return false;
        *///?} else if >=26.1 {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), button) == GLFW.GLFW_PRESS;
        //?} else {
        /*return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), button) == GLFW.GLFW_PRESS;
        *///?}
    }

    public static String keyName(int code) {
        //? if >=26.3 {
        /*return InputConstants.Type.KEYBOARD.getOrCreate(code).getDisplayName().getString();
        *///?} else {
        return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
        //?}
    }

    public static String mouseName(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button).getDisplayName().getString();
    }
}
