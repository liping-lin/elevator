package com.llin.interview.elevator;

public enum Direction {
    UP, DOWN, NONE;

    public Direction getOpposite() {
        if (this.ordinal() == UP.ordinal()) {
            return DOWN;
        } else if (this.ordinal() == DOWN.ordinal()) {
            return UP;
        }
        return null;
    }
}
