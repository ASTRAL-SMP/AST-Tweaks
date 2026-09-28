package com.astral.asttweaks.feature.voidtrade;

/**
 * Void Trade ワークフローのステップ種別。
 */
public enum VoidTradeStepType {
    MOVE_TO("move_to", true, true),
    NUDGE("nudge", false, false),
    INTERACT_ENTITY("interact_entity", true, true),
    INTERACT_BLOCK("interact_block", true, false),
    WAIT_UNLOAD("wait_unload", false, true),
    TRADE("trade", false, true),
    CLOSE_SCREEN("close_screen", false, false),
    WAIT("wait", false, false),
    COMMAND("command", false, false);

    private final String id;
    private final boolean usesPosition;
    private final boolean hasTimeout;

    VoidTradeStepType(String id, boolean usesPosition, boolean hasTimeout) {
        this.id = id;
        this.usesPosition = usesPosition;
        this.hasTimeout = hasTimeout;
    }

    public String getId() {
        return id;
    }

    /**
     * 座標 (x, y, z) を使うステップか。
     */
    public boolean usesPosition() {
        return usesPosition;
    }

    /**
     * 完了条件を待つステップか（設定のタイムアウトで打ち切る対象）。
     */
    public boolean hasTimeout() {
        return hasTimeout;
    }
}
