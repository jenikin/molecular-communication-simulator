package org.molecularcommunication.models.spatial;

import java.util.List;
import java.util.Random;

public final class Molecule {
    private Location location;
    private boolean inCell;
    private boolean onCellBorder;

    public Molecule(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isInCell() {
        return inCell;
    }

    public boolean isOnCellBorder() {
        return onCellBorder;
    }

    void updateCellLocation(List<Cell> cells) {
        inCell = cells.stream().anyMatch(cell -> cell.contains(location));
        onCellBorder = !inCell && cells.stream().anyMatch(cell -> cell.isOnBorder(location));
    }

    void move(double timeStep, Random random, int width, int height, List<Cell> cells) {
        location = location.randomAdjacentLocation(timeStep, random, width, height);
        updateCellLocation(cells);
    }
}
