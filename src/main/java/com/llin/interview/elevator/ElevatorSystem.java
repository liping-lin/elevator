package com.llin.interview.elevator;

import static com.llin.interview.elevator.Direction.DOWN;
import static com.llin.interview.elevator.Direction.UP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

public class ElevatorSystem {
    private static final Logger LOG = Logger.getLogger(ElevatorSystem.class
            .getName());
    private final List<Elevator> elevators = Collections
            .synchronizedList(new ArrayList<Elevator>(2));
    private final Map<Direction, Set<Request>> requests = new ConcurrentHashMap<Direction, Set<Request>>(
            30);
    private final ElevatorAssigner assigner;

    public ElevatorSystem() {
        // Consolidate UP and DOWN requests respectively.
        requests.put(UP, Collections.synchronizedSet(new TreeSet<Request>()));
        requests.put(DOWN, Collections.synchronizedSet(new TreeSet<Request>()));
        addNewElevator(); // Add 2 elevators by default
        addNewElevator();
        assigner = new ElevatorAssigner(elevators, requests);
    }

    public ElevatorSystem addNewElevator() {
        elevators.add(new Elevator(this));
        return this;
    }

    public boolean hasRequest(Request req) {
        return requests.get(req.getDirection()).contains(req);
    }

    public boolean clearRequest(Request req) {
        return requests.get(req.getDirection()).remove(req);
    }

    public void request(Direction direction, int fromFloor) {
        request(direction, fromFloor, new int[0]);
    }

    public void assignRequests() {
        assigner.assignRequests();
    }

    public void request(Direction direction, int fromFloor, int[] destinations) {
        requests.get(direction).add(
                new Request(direction, fromFloor, destinations));
        LOG.info("New floor request added from " + fromFloor + " floor asking "
                + direction + " planning to get to "
                + Arrays.toString(destinations) + " floors.");
        assignRequests();
    }

    private boolean anyFloorRequests() {
        for (Set<Request> reqs : requests.values()) {
            if (reqs.size() > 0) {
                return true;
            }
        }
        return false;
    }

    public void waitUntilAllFinished() throws InterruptedException,
            ExecutionException {
        while (anyFloorRequests()) { // Any floor requests?
            Thread.sleep(1000);
        }
        for (Elevator e : elevators) {
            e.waitUntilFinished();
        }
    }

}
