package org.molecularcommunication.models.spatial;

public final class Cell {
    private final Location location;
    private final double diameter;

    public Cell(Location location, double diameter) {
        this.location = location;
        this.diameter = diameter;
    }

    public Location getLocation() {
        return location;
    }

    public double getDiameter() {
        return diameter;
    }

    public boolean contains(Location point) {
        return location.distanceTo(point) < diameter / 2;
    }

    public boolean isOnBorder(Location point) {
        return Math.abs(location.distanceTo(point) - diameter / 2) < 0.00001;
    }
}
