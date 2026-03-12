package com.astral.asttweaks.util;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * 任意の2キーコンボを表現するクラス。
 * GSONで自動シリアライズ可能（intフィールドのみ）。
 */
public class KeyCombo {
    public int mainKey;      // GLFWキーコード (-1 = 未設定)
    public int modifierKey;  // GLFWキーコード (-1 = 修飾なし)

    public KeyCombo() {
        this.mainKey = -1;
        this.modifierKey = -1;
    }

    public KeyCombo(int mainKey, int modifierKey) {
        this.mainKey = mainKey;
        this.modifierKey = modifierKey;
    }

    /**
     * 両キー同時押し判定。
     */
    public boolean isPressed(long windowHandle) {
        if (mainKey == -1) return false;
        boolean mainDown = GLFW.glfwGetKey(windowHandle, mainKey) == GLFW.GLFW_PRESS;
        if (!mainDown) return false;
        if (modifierKey == -1) return true;
        return GLFW.glfwGetKey(windowHandle, modifierKey) == GLFW.GLFW_PRESS;
    }

    /**
     * "K + L" のようなローカライズ表示名を返す。
     */
    public String getDisplayName() {
        if (mainKey == -1 && modifierKey == -1) {
            return "---";
        }
        if (mainKey == -1) {
            return "---";
        }
        String mainName = getKeyName(mainKey);
        if (modifierKey == -1) {
            return mainName;
        }
        String modName = getKeyName(modifierKey);
        return modName + " + " + mainName;
    }

    public static String getKeyName(int keyCode) {
        if (keyCode == -1) return "---";
        return InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
    }

    /**
     * コピーを作成する。
     */
    public KeyCombo copy() {
        return new KeyCombo(this.mainKey, this.modifierKey);
    }

    /**
     * 他のKeyComboと同値か判定する。
     */
    public boolean equals(KeyCombo other) {
        if (other == null) return false;
        return this.mainKey == other.mainKey && this.modifierKey == other.modifierKey;
    }

    /**
     * 他のKeyComboの値をコピーする。
     */
    public void copyFrom(KeyCombo other) {
        this.mainKey = other.mainKey;
        this.modifierKey = other.modifierKey;
    }
}
