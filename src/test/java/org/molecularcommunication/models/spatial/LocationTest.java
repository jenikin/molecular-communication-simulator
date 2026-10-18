package org.molecularcommunication.models.spatial;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationTest {
    @Test
    void movementKeepsLocationsWithinTheBoundedArea() {
        Random random = new Random(17L);
        Location location = new Location(0.01, 19.99);

        for (int step = 0; step < 500; step++) {
            location = location.randomAdjacentLocation(1.0, random, 20, 20);
            assertTrue(location.getX() >= 0 && location.getX() < 20);
            assertTrue(location.getY() >= 0 && location.getY() < 20);
        }
    }
}
