package org.marvin.dealership;

/**
 * Created by Marvin on 28.05.2014.
 */
public enum MoveMode {
    MOVE_FORWARD(1),
    MOVE_BACK(2),
    MOVE_LEFT(3),
    MOVE_RIGHT(4),
    MOVE_FORWARD_LEFT(5),
    MOVE_FORWARD_RIGHT(6),
    MOVE_BACK_LEFT(7),
    MOVE_BACK_RIGHT(8),
    NONE(0);

    private final int id;
    MoveMode(int id) { this.id = id; }
    public int getValue() { return id; }
}
