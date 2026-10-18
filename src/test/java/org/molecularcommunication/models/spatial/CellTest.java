package org.molecularcommunication.models.spatial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellTest {
    private final Cell cell = new Cell(new Location(10, 10), 10);

    @Test
    void classifiesInsideBoundaryAndOutsidePoints() {
        Location inside = new Location(10, 10);
        Location boundary = new Location(15, 10);
        Location outside = new Location(15.01, 10);

        assertTrue(cell.contains(inside));
        assertFalse(cell.isOnBorder(inside));
        assertFalse(cell.contains(boundary));
        assertTrue(cell.isOnBorder(boundary));
        assertFalse(cell.contains(outside));
        assertFalse(cell.isOnBorder(outside));
    }
}
