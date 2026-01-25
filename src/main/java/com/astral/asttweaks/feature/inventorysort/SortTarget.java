package com.astral.asttweaks.feature.inventorysort;

/**
 * Enum for inventory sort target selection.
 */
public enum SortTarget {
    PLAYER_ONLY("player"),      // Player inventory only
    CONTAINER_ONLY("container"), // Container only
    BOTH("both");                // Both player and container

    private final String id;

    SortTarget(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
