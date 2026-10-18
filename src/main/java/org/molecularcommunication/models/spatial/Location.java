package org.molecularcommunication.models.spatial;

import java.util.Random;

public final class Location {
    private final double x;
    private final double y;

    public Location(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Location randomAdjacentLocation(double timeStep, Random random, int width, int height) {
        double standardDeviation = 2 * timeStep;
        double newX;
        double newY;
        do {
            newX = x + random.nextGaussian() * standardDeviation;
            newY = y + random.nextGaussian() * standardDeviation;
        } while (newX < 0 || newX >= width || newY < 0 || newY >= height);
        return new Location(newX, newY);
    }

    public double distanceTo(Location location) {
        double xDifference = x - location.x;
        double yDifference = y - location.y;
        return Math.sqrt(xDifference * xDifference + yDifference * yDifference);
    }
}
