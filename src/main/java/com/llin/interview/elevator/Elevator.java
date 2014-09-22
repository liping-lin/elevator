package com.llin.interview.elevator;

import static com.llin.interview.elevator.Direction.DOWN;
import static com.llin.interview.elevator.Direction.NONE;
import static com.llin.interview.elevator.Direction.UP;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

public class Elevator implements Runnable {
    private static final Logger LOG = Logger
            .getLogger(Elevator.class.getName());
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2);
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final int MAX_WAITING_SECONDS = 5;
    private static final int SECONDS_PER_FLOOR = 2;
    private static final int POLLING_PERIOD = 1000;

    private static enum Status {
        NONE, STARTED, SHUTDOWN_REQUESTED
    };

    private static int getNextId() {
        return COUNTER.getAndIncrement();
    }

    private final int id = getNextId();
    private final ElevatorSystem system;
    private final Future<?> future;
    private int currentFloor = 1;
    private Direction currentDirection = Direction.NONE;
    private Set<Request> destinations = Collections
            .synchronizedSet(new TreeSet<Request>());
    private Request floorRequest;
    private Status status = Status.NONE;

    public Elevator(ElevatorSystem sys) {
        this.system = sys;
        future = EXEC.submit(this);
    }

    @Override
    public String toString() {
        return "E#" + id;
    }

    private void info(String message) {
        LOG.info(this + " " + currentDirection + " at " + currentFloor + ": "
                + message + ".");
    }

    public float estimateCost(Request req) {
        int dist = Math.abs(req.getFloor() - currentFloor);
        if (currentDirection.equals(Direction.NONE)) { // Stopped
            return dist;
        } else if (currentDirection.equals(req.getDirection())) {
            float cost = (currentDirection.equals(Direction.UP) ? -1 : 1)
                    * (currentFloor - req.getFloor());
            if (cost >= 0) {
                return cost - 0.1f; // Cost less if already on the go
            }
            info("Passed dir.");
            return calcFarEndCost(req.getFloor());
        } else { // The opposite direction
            return calcFarEndCost(req.getFloor());
        }
    }

    private float calcFarEndCost(int floor) {
        Integer farEnd = getFinalDestination();
        if (farEnd == null) {
            return Math.abs(currentFloor - floor);
        }
        return Math.abs(farEnd - currentFloor) + Math.abs(farEnd - floor);
    }

    private Integer getFinalDestination() {
        if (destinations.size() > 0) {
            Request[] reqs = destinations.toArray(new Request[0]);
            return reqs[reqs.length - 1].getFloor();
        }
        return null;
    }

    public boolean request(int floor) {
        if (floor == currentFloor || currentDirection.equals(UP)
                && floor < currentFloor || currentDirection.equals(DOWN)
                && floor > currentFloor) {
            // Already at the same floor => just open the door.
            // Already passed the smaller floor if going UP or vice versa
            return false; // Ignore invalid requests (buttons disabled)
        }
        goTo(floor);
        destinations.add(new Request(currentDirection, floor));
        return true;
    }

    public void goTo(Request request) {
        floorRequest = request;
        info("Assigned: " + floorRequest);
        goTo(request.getFloor());
    }

    private synchronized void goTo(int floor) {
        // Move only if not moving
        if (currentDirection.equals(NONE) || destinations.size() == 0) {
            if (floor != currentFloor) {
                changeDirection(floor > currentFloor ? UP : DOWN);
                notifyAll(); // Wake up, the direction may have been changed.
            }
        }
    }

    private synchronized void waitingForTask() throws InterruptedException {
        wait(POLLING_PERIOD);
    }

    private boolean readyToShutdown() {
        return Status.SHUTDOWN_REQUESTED.equals(status)
                && Direction.NONE.equals(currentDirection)
                && destinations.size() < 1;
    }

    public void run() {
        info("Thread started");
        status = Status.STARTED;
        try {
            while (!readyToShutdown()) {
                if (Direction.NONE.equals(currentDirection)) {
                    waitingForTask();
                    continue;
                }
                moveInOneDirection();
                if (checkFloorRequest(currentDirection.getOpposite())) {
                    // Honor the floor request with destinations requested.
                    continue;
                }
                currentDirection = Direction.NONE;
                info("Top or bottom most reached, no destination button clicked");
                system.assignRequests(); //
            }
        } catch (InterruptedException e1) {
            info("Interrupted, existing..");
        }
        info("Thread ended");
    }

    private void moveInOneDirection() throws InterruptedException {
        Integer nextFloor = getNextFloor();
        while (nextFloor != null && nextFloor != currentFloor) {
            Thread.sleep(SECONDS_PER_FLOOR * 1000);
            if (currentDirection.equals(UP)) {
                ++currentFloor;
            } else if (currentDirection.equals(DOWN)) {
                --currentFloor;
            }
            info(""); // Reached info
            checkDestination();
            checkFloorRequest(currentDirection);
            nextFloor = getNextFloor();
        }
    }

    private void checkDestination() throws InterruptedException {
        Request current = new Request(currentDirection, currentFloor);
        if (destinations.contains(current)) {
            destinations.remove(current);
            info("Open the door for the destination requested from inside");
            int waitingTime = (int) (MAX_WAITING_SECONDS * Math.random() + 1);
            Thread.sleep(waitingTime * 1000); // Wait to allow people in
            info("All people were OUT after " + waitingTime + " seconds");
        }
    }

    private boolean checkFloorRequest(Direction direction)
            throws InterruptedException {
        Request req = new Request(direction, currentFloor);
        if (system.hasRequest(req)) {
            system.clearRequest(req);
            info("Open the door for the system floor " + direction +" request");
            int waitingTime = (int) (MAX_WAITING_SECONDS * Math.random() + 1);
            Thread.sleep(waitingTime * 1000);
            info("All people were IN after " + waitingTime + " seconds");
        }
        boolean hasDestinations = false;
        if (req.equals(floorRequest)) {
            info("The floor request satisfied: " + floorRequest);
            if (floorRequest.getDestinations().length > 0) {
                hasDestinations = true;
                changeDirection(direction);
                for (int dest : floorRequest.getDestinations()) {
                    request(dest);
                }
            }
            floorRequest = null;
        }
        return hasDestinations;
    }

    private boolean changeDirection(Direction newDirection) {
        if (!currentDirection.equals(newDirection)) { // Changed?
            destinations.clear();
            currentDirection = newDirection;
            return true;
        }
        return false;
    }

    private Integer getNextDestination() {
        Request current = new Request(currentDirection, currentFloor);
        for (Iterator<Request> ir = destinations.iterator(); ir.hasNext();) {
            Request dest = ir.next();
            if (dest.compareTo(current) < 0) {
                // Clean up those already passed
                // (in case being accidentally added in any racing condition)
                ir.remove();
            } else {
                return dest.getFloor();
            }
        }
        return null;
    }

    private Integer getNextFloor() {
        Integer nextDest = getNextDestination();
        if (nextDest == null) {
            if (floorRequest == null) {
                return null;
            }
            return floorRequest.getFloor();
        }
        if (floorRequest == null) {
            return nextDest;
        }
        if (floorRequest.getDirection().equals(currentDirection)) {
            return Math.min(nextDest, floorRequest.getFloor());
        } else {
            return nextDest;
        }
    }

    public void waitUntilFinished() throws InterruptedException,
            ExecutionException {
        while (Status.NONE.equals(status)) { // The thread started?
            Thread.sleep(POLLING_PERIOD);
        }
        while (destinations.size() > 0) { // Any destinations to serve?
            Thread.sleep(POLLING_PERIOD);
        }
        status = Status.SHUTDOWN_REQUESTED;
        future.get();
    }

}
