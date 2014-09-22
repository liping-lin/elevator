package com.llin.interview.elevator;

import static com.llin.interview.elevator.Direction.DOWN;
import static com.llin.interview.elevator.Direction.UP;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ElevatorAssigner {
    private final List<Elevator> elevators;
    private final Map<Direction, Set<Request>> requests;

    public ElevatorAssigner(List<Elevator> elevators,
            Map<Direction, Set<Request>> requests) {
        this.elevators = elevators;
        this.requests = requests;
    }

    private Request getRequestStart(Direction direction) {
        Iterator<Request> ir = requests.get(direction).iterator();
        if (ir.hasNext()) {
            return ir.next();
        }
        return null;
    }

    private Request getRequestEnd(Direction direction) {
        Request[] reqs = requests.get(direction).toArray(new Request[0]);
        if (reqs.length > 0) {
            return reqs[reqs.length - 1];
        }
        return null;
    }

    private void addFloorStart(Set<Request> res, Direction dir) {
        addFloor(res, getRequestStart(dir));
    }

    private void addFloorEnd(Set<Request> res, Direction dir) {
        addFloor(res, getRequestEnd(dir));
    }

    private void addFloor(Set<Request> res, Request request) {
        if (request != null && res.size() < elevators.size()) {
            res.add(request);
        }
    }

    private Set<Request> getHighPriorityRequests() {
        Set<Request> res = new LinkedHashSet<Request>();
        addFloorStart(res, UP);
        addFloorStart(res, DOWN);
        addFloorEnd(res, UP);
        addFloorEnd(res, DOWN);
        return res;
    }

    private Elevator assignRequest(Set<Elevator> restElevators, Request req) {
        float minCost = Float.MAX_VALUE;
        Elevator selected = null;
        for (Elevator e : restElevators) {
            float cost = e.estimateCost(req);
            if (cost < minCost) {
                minCost = cost;
                selected = e;
            }
        }
        if (selected != null) {
            selected.goTo(req);
            restElevators.remove(selected);
        }
        return selected;
    }

    public void assignRequests() {
        Set<Elevator> restElevators = new LinkedHashSet<Elevator>(elevators);
        for (Request req : getHighPriorityRequests()) {
            assignRequest(restElevators, req);
        }
    }
}
