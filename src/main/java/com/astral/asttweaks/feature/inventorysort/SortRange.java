package com.astral.asttweaks.feature.inventorysort;

/**
 * Enum for inventory sort range.
 */
public enum SortRange {
    MAIN_ONLY("main", 9, 35);  // メインインベントリのみ

    private final String id;
    private final int startSlot;
    private final int endSlot;

    SortRange(String id, int start, int end) {
        this.id = id;
        this.startSlot = start;
        this.endSlot = end;
    }

    public String getId() {
        return id;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }
}
