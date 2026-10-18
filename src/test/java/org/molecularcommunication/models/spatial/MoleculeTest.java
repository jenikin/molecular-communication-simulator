package org.molecularcommunication.models.spatial;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoleculeTest {
    @Test
    void reportsInsideBorderAndOutsideStatesExclusively() {
        Cell cell = new Cell(new Location(10, 10), 10);
        Molecule inside = new Molecule(new Location(10, 10));
        Molecule border = new Molecule(new Location(15, 10));
        Molecule outside = new Molecule(new Location(18, 10));

        inside.updateCellLocation(List.of(cell));
        border.updateCellLocation(List.of(cell));
        outside.updateCellLocation(List.of(cell));

        assertTrue(inside.isInCell());
        assertFalse(inside.isOnCellBorder());
        assertFalse(border.isInCell());
        assertTrue(border.isOnCellBorder());
        assertFalse(outside.isInCell());
        assertFalse(outside.isOnCellBorder());
    }
}
