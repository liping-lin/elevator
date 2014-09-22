package com.llin.interview.elevator;

import static com.llin.interview.elevator.Direction.DOWN;
import static com.llin.interview.elevator.Direction.UP;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

public class ElevatorTest {

    @Test
    public void testRequestsInTreeSet() {
        Set<Request> upRequests = new TreeSet<Request>();
        for (int i : new int[] { 3, 5, 1, 4, 2 }) {
            upRequests.add(new Request(Direction.UP, i));
        }
        // Make sure in ascending order
        int n = 1;
        for (Request r : upRequests) {
            assertEquals(n++, r.getFloor());
        }

        Set<Request> downRequests = new TreeSet<Request>();
        for (int i : new int[] { 3, 5, 1, 4, 2 }) {
            downRequests.add(new Request(Direction.DOWN, i));
        }
        // Make sure in descending order
        n = downRequests.size();
        for (Request r : downRequests) {
            assertEquals(n--, r.getFloor());
        }
    }

    @Test
    public void testFloorRequests() throws InterruptedException, IOException,
            ExecutionException {
        ElevatorSystem sys = new ElevatorSystem();
        sys.request(DOWN, 3);
        Thread.sleep(3000);
        sys.request(DOWN, 10);
        Thread.sleep(3000);
        sys.request(DOWN, 8);
        sys.waitUntilAllFinished();
    }

    @Test
    public void testDestinations() throws InterruptedException, IOException,
            ExecutionException {
        ElevatorSystem sys = new ElevatorSystem();
        sys.request(DOWN, 10, new int[] { 3, 8, 5, 1 });
        sys.waitUntilAllFinished();
    }

    @Test
    public void testUpAndDown() throws InterruptedException, IOException,
            ExecutionException {
        ElevatorSystem sys = new ElevatorSystem();
        sys.request(UP, 2);
        Thread.sleep(1500);
        sys.request(DOWN, 10);
        Thread.sleep(1500);
        sys.request(UP, 4);
        Thread.sleep(1500);
        sys.request(DOWN, 8);
        sys.waitUntilAllFinished();
    }

    @Test
    public void testFloorRequestsWithDestinations()
            throws InterruptedException, IOException, ExecutionException {
        ElevatorSystem sys = new ElevatorSystem();
        sys.request(DOWN, 3, new int[] { 1 });
        Thread.sleep(1500);
        sys.request(DOWN, 10, new int[] { 3, 5, 1 });
        Thread.sleep(1500);
        sys.request(DOWN, 8, new int[] { 1, 2, 4 });
        sys.waitUntilAllFinished();
    }

}
