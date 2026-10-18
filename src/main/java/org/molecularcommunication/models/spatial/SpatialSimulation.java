package org.molecularcommunication.models.spatial;

import java.util.ArrayList;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Random;

public final class SpatialSimulation {
    private static final int WIDTH = 20;
    private static final int HEIGHT = 20;
    private static final long RANDOM_SEED = 42L;
    private static final double END_TIME = 20;
    private static final double TIME_STEP = 0.2;

    private final List<Cell> cells = new ArrayList<>();
    private final List<Molecule> molecules = new ArrayList<>();
    private final Random random = new Random(RANDOM_SEED);

    public SpatialSimulation(List<Cell> initialCells, List<Molecule> initialMolecules) {
        cells.addAll(initialCells);
        molecules.addAll(initialMolecules);
        updateMoleculeLocations();
    }

    public void addCell(Cell cell) {
        cells.add(cell);
        updateMoleculeLocations();
    }

    public void addMolecule(Molecule molecule) {
        molecules.add(molecule);
        molecule.updateCellLocation(cells);
    }

    public List<Cell> getCells() {
        return List.copyOf(cells);
    }

    public List<Molecule> getMolecules() {
        return List.copyOf(molecules);
    }

    public void run() {
        try {
            run(null);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export simulation results.", exception);
        }
    }

    public void run(Writer csvOutput) throws IOException {
        System.out.printf("Spatial model: %d x %d area, seed %d%n", WIDTH, HEIGHT, RANDOM_SEED);
        if (csvOutput != null) {
            csvOutput.write("time,molecule,x,y,state\n");
        }
        printState(0);
        writeState(csvOutput, 0);

        int steps = (int) Math.round(END_TIME / TIME_STEP);
        for (int step = 1; step <= steps; step++) {
            simulate(TIME_STEP);
            if (step % 10 == 0) {
                double time = step * TIME_STEP;
                printState(time);
                writeState(csvOutput, time);
            }
        }
    }

    private void writeState(Writer output, double time) throws IOException {
        if (output == null) {
            return;
        }
        for (int index = 0; index < molecules.size(); index++) {
            Molecule molecule = molecules.get(index);
            Location location = molecule.getLocation();
            String state = molecule.isOnCellBorder() ? "border"
                    : molecule.isInCell() ? "intracellular" : "extracellular";
            output.write(String.format(java.util.Locale.ROOT, "%.4f,molecule-%d,%.8f,%.8f,%s%n",
                    time, index, location.getX(), location.getY(), state));
        }
    }

    public void simulate(double timeStep) {
        for (Molecule molecule : molecules) {
            molecule.move(timeStep, random, WIDTH, HEIGHT, cells);
        }
    }

    private void updateMoleculeLocations() {
        for (Molecule molecule : molecules) {
            molecule.updateCellLocation(cells);
        }
    }

    private void printState(double time) {
        PopulationCounts counts = countStates();
        System.out.printf("t=%.1f, extracellular=%d, intracellular=%d, on-cell-border=%d%n",
                time, counts.extracellular(), counts.intracellular(), counts.border());
        for (int index = 0; index < molecules.size(); index++) {
            Location location = molecules.get(index).getLocation();
            System.out.printf("  molecule-%d: (%.3f, %.3f)%n", index, location.getX(), location.getY());
        }
    }

    PopulationCounts countStates() {
        long intracellular = molecules.stream().filter(Molecule::isInCell).count();
        long border = molecules.stream().filter(Molecule::isOnCellBorder).count();
        long extracellular = molecules.size() - intracellular - border;
        return new PopulationCounts(extracellular, intracellular, border, molecules.size());
    }

    record PopulationCounts(long extracellular, long intracellular, long border, long total) {
    }
}
