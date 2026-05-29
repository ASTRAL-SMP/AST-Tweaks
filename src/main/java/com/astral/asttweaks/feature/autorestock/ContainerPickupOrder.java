package com.astral.asttweaks.feature.autorestock;

/**
 * コンテナ（シュルカーボックス等）から補充元アイテムを取得する際のスロット走査順。
 */
public enum ContainerPickupOrder {
    /** スロットの先頭から探し、最初に見つかったマッチを取得する（従来の挙動）。 */
    FIRST_SLOT("first_slot"),
    /** スロットの末尾から探し、最後尾のマッチを取得する。 */
    LAST_SLOT("last_slot");

    private final String id;

    ContainerPickupOrder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
