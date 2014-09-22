package com.llin.interview.elevator;

public class Request implements Comparable<Request> {
    private final int floor; // Immutable
    private final Direction direction; // Immutable
    private final int[] destinations; // Optional, used for simulating

    public Request(Direction direction, int floor) {
        this(direction, floor, new int[0]);
    }

    public Request(Direction direction, int floor, int[] destinations) {
        this.floor = floor;
        this.direction = direction;
        this.destinations = destinations == null ? new int[0] : destinations;
    }

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public int[] getDestinations() {
        return destinations;
    }

    @Override
    public int compareTo(Request o) {
        if (!direction.equals(o.direction)) {
            throw new IllegalArgumentException("Expected only same direction in the collection.");
        }
        return (Direction.UP.equals(direction) ? 1 : -1) * (floor - o.floor);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Request [floor=").append(floor).append(", direction=")
                .append(direction).append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((direction == null) ? 0 : direction.hashCode());
        result = prime * result + floor;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Request other = (Request) obj;
        if (direction != other.direction)
            return false;
        if (floor != other.floor)
            return false;
        return true;
    }

}
