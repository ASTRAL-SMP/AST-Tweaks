package com.astral.asttweaks.feature.autodrop;

/**
 * Auto Drop の発動方式。
 */
public enum AutoDropMode {
    /** 任意コンテナ画面で実行キー押下時にドロップ (v1.1.2 既定動作) */
    EXECUTE_KEY("execute_key"),
    /** プレイヤーインベントリを開いた瞬間に自動ドロップ (v1.1.0 動作) */
    ON_INVENTORY_OPEN("on_inventory_open");

    private final String id;

    AutoDropMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
