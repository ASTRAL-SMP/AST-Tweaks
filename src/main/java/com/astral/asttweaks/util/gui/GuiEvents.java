package com.astral.asttweaks.util.gui;

import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.components.events.GuiEventListener;
//? if >=26.1 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
//?}

/**
 * 入力イベントの世代差を吸収する。26.1 からイベントが record（MouseButtonEvent 等）になった。
 * CompatScreen / CompatListEntry が処理中のイベントを保持し、子ウィジェットへの転送に使う。
 */
public final class GuiEvents {
    private GuiEvents() {}

    //? if >=26.1 {
    private static MouseButtonEvent mouse;
    private static boolean doubleClick;
    private static KeyEvent key;

    public static boolean withMouse(MouseButtonEvent event, boolean isDoubleClick, BooleanSupplier body) {
        MouseButtonEvent prev = mouse;
        boolean prevDouble = doubleClick;
        mouse = event;
        doubleClick = isDoubleClick;
        try {
            return body.getAsBoolean();
        } finally {
            mouse = prev;
            doubleClick = prevDouble;
        }
    }

    public static boolean withKey(KeyEvent event, BooleanSupplier body) {
        KeyEvent prev = key;
        key = event;
        try {
            return body.getAsBoolean();
        } finally {
            key = prev;
        }
    }

    public static MouseButtonEvent mouseEvent(double x, double y, int button) {
        int modifiers = mouse != null ? mouse.modifiers() : 0;
        return new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
    }

    public static boolean doubleClick() {
        return doubleClick;
    }

    public static KeyEvent keyEvent(int keyCode, int scancode, int modifiers) {
        return new KeyEvent(keyCode, scancode, modifiers);
    }

    /** 物理キーのコード（26.3 で GLFW のスキャンコードから SDL のキーコードになった） */
    public static int scancode(KeyEvent event) {
        //? if >=26.3 {
        /*return event.keycode();
        *///?} else {
        return event.scancode();
        //?}
    }
    //?}

    public static boolean click(GuiEventListener target, double x, double y, int button) {
        //? if >=26.1 {
        return target.mouseClicked(mouseEvent(x, y, button), doubleClick);
        //?} else {
        /*return target.mouseClicked(x, y, button);
        *///?}
    }

    public static boolean release(GuiEventListener target, double x, double y, int button) {
        //? if >=26.1 {
        return target.mouseReleased(mouseEvent(x, y, button));
        //?} else {
        /*return target.mouseReleased(x, y, button);
        *///?}
    }

    public static boolean drag(GuiEventListener target, double x, double y, int button, double deltaX, double deltaY) {
        //? if >=26.1 {
        return target.mouseDragged(mouseEvent(x, y, button), deltaX, deltaY);
        //?} else {
        /*return target.mouseDragged(x, y, button, deltaX, deltaY);
        *///?}
    }

    public static boolean scroll(GuiEventListener target, double x, double y, double amount) {
        //? if >=26.1 {
        return target.mouseScrolled(x, y, 0, amount);
        //?} else {
        /*return target.mouseScrolled(x, y, amount);
        *///?}
    }

    public static boolean keyPress(GuiEventListener target, int keyCode, int scancode, int modifiers) {
        //? if >=26.1 {
        return target.keyPressed(keyEvent(keyCode, scancode, modifiers));
        //?} else {
        /*return target.keyPressed(keyCode, scancode, modifiers);
        *///?}
    }

    public static boolean keyRelease(GuiEventListener target, int keyCode, int scancode, int modifiers) {
        //? if >=26.1 {
        return target.keyReleased(keyEvent(keyCode, scancode, modifiers));
        //?} else {
        /*return target.keyReleased(keyCode, scancode, modifiers);
        *///?}
    }

    public static boolean charType(GuiEventListener target, int codepoint, int modifiers) {
        //? if >=26.1 {
        return target.charTyped(new CharacterEvent(codepoint));
        //?} else {
        /*return target.charTyped((char) codepoint, modifiers);
        *///?}
    }
}
