package com.astral.asttweaks.feature.automove;

/**
 * Enum representing the direction for auto-move feature.
 */
public enum MoveDirection {
    FORWARD("forward"),
    BACKWARD("backward"),
    LEFT("left"),
    RIGHT("right");

    private final String id;

    MoveDirection(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
