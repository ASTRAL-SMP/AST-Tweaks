package com.astral.asttweaks.feature.automove;

/**
 * Enum representing the direction for auto-move feature.
 * forwardAxis: +1=forward, -1=backward, 0=none.
 * sidewaysAxis: +1=left, -1=right, 0=none（Minecraftの movementSideways の符号に合わせる）。
 */
public enum MoveDirection {
    FORWARD("forward", 1, 0),
    BACKWARD("backward", -1, 0),
    LEFT("left", 0, 1),
    RIGHT("right", 0, -1),
    FORWARD_LEFT("forward_left", 1, 1),
    FORWARD_RIGHT("forward_right", 1, -1),
    BACKWARD_LEFT("backward_left", -1, 1),
    BACKWARD_RIGHT("backward_right", -1, -1);

    private final String id;
    private final int forwardAxis;
    private final int sidewaysAxis;

    MoveDirection(String id, int forwardAxis, int sidewaysAxis) {
        this.id = id;
        this.forwardAxis = forwardAxis;
        this.sidewaysAxis = sidewaysAxis;
    }

    public String getId() {
        return id;
    }

    public int getForwardAxis() {
        return forwardAxis;
    }

    public int getSidewaysAxis() {
        return sidewaysAxis;
    }
}
