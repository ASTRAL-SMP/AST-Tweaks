package com.astral.asttweaks.feature.inventorysort;

/**
 * Enum for inventory sort modes.
 */
public enum SortMode {
    ITEM_ID("item_id"),
    ITEM_NAME("item_name"),
    CATEGORY("category"),
    STACK_COUNT("stack_count"),
    TOTAL_COUNT("total_count");

    private final String id;

    SortMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
