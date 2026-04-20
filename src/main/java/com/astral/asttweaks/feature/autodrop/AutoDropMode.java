package com.astral.asttweaks.feature.autodrop;

/**
 * Auto Drop の発動方式。
 */
public enum AutoDropMode {
    /** 任意コンテナ画面で実行キー押下時にドロップ */
    EXECUTE_KEY("execute_key"),
    /** 任意コンテナ画面（プレイヤーインベントリ含む）を開いた瞬間に自動ドロップ */
    ON_INVENTORY_OPEN("on_inventory_open");

    private final String id;

    AutoDropMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
