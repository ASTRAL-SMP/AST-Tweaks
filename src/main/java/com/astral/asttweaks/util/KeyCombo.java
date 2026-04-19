package com.astral.asttweaks.util;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/**
 * 任意の2キーコンボを表現するクラス。
 * キーボードキーに加えてマウスボタン (Mouse L/R/M/4/5/...) もバインド可能。
 * GSONで自動シリアライズ可能（intフィールド + String タイプ）。
 */
public class KeyCombo {
    public static final String TYPE_KEY = "key";
    public static final String TYPE_MOUSE = "mouse";

    public int mainKey;              // GLFWキーコード or マウスボタンコード (-1 = 未設定)
    public int modifierKey;          // 同上 (-1 = 修飾なし)
    public String mainKeyType;       // "key" or "mouse"
    public String modifierKeyType;   // "key" or "mouse"

    public KeyCombo() {
        this(-1, -1);
    }

    public KeyCombo(int mainKey, int modifierKey) {
        this(mainKey, TYPE_KEY, modifierKey, TYPE_KEY);
    }

    public KeyCombo(int mainKey, String mainKeyType, int modifierKey, String modifierKeyType) {
        this.mainKey = mainKey;
        this.modifierKey = modifierKey;
        this.mainKeyType = mainKeyType != null ? mainKeyType : TYPE_KEY;
        this.modifierKeyType = modifierKeyType != null ? modifierKeyType : TYPE_KEY;
    }

    /**
     * 両キー同時押し判定。
     */
    public boolean isPressed(long windowHandle) {
        if (mainKey == -1) return false;
        if (!isInputDown(windowHandle, mainKey, mainKeyType)) return false;
        if (modifierKey == -1) return true;
        return isInputDown(windowHandle, modifierKey, modifierKeyType);
    }

    private static boolean isInputDown(long windowHandle, int code, String type) {
        if (TYPE_MOUSE.equals(type)) {
            return GLFW.glfwGetMouseButton(windowHandle, code) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(windowHandle, code) == GLFW.GLFW_PRESS;
    }

    /**
     * "K + L" のようなローカライズ表示名を返す。
     */
    public String getDisplayName() {
        if (mainKey == -1) {
            return "---";
        }
        String mainName = getInputName(mainKey, mainKeyType);
        if (modifierKey == -1) {
            return mainName;
        }
        String modName = getInputName(modifierKey, modifierKeyType);
        return modName + " + " + mainName;
    }

    public static String getInputName(int code, String type) {
        if (code == -1) return "---";
        if (TYPE_MOUSE.equals(type)) {
            return InputUtil.Type.MOUSE.createFromCode(code).getLocalizedText().getString();
        }
        return InputUtil.fromKeyCode(code, 0).getLocalizedText().getString();
    }

    /**
     * @deprecated {@link #getInputName(int, String)} を使用してください。
     */
    @Deprecated
    public static String getKeyName(int keyCode) {
        return getInputName(keyCode, TYPE_KEY);
    }

    /**
     * コピーを作成する。
     */
    public KeyCombo copy() {
        return new KeyCombo(this.mainKey, this.mainKeyType, this.modifierKey, this.modifierKeyType);
    }

    /**
     * 他のKeyComboと同値か判定する。
     */
    public boolean equals(KeyCombo other) {
        if (other == null) return false;
        return this.mainKey == other.mainKey
                && this.modifierKey == other.modifierKey
                && Objects.equals(normalizeType(this.mainKeyType), normalizeType(other.mainKeyType))
                && Objects.equals(normalizeType(this.modifierKeyType), normalizeType(other.modifierKeyType));
    }

    private static String normalizeType(String type) {
        return type != null ? type : TYPE_KEY;
    }

    /**
     * 他のKeyComboの値をコピーする。
     */
    public void copyFrom(KeyCombo other) {
        this.mainKey = other.mainKey;
        this.modifierKey = other.modifierKey;
        this.mainKeyType = normalizeType(other.mainKeyType);
        this.modifierKeyType = normalizeType(other.modifierKeyType);
    }
}
