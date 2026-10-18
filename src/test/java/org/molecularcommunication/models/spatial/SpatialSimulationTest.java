package org.molecularcommunication.models.spatial;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialSimulationTest {
    @Test
    void stateCountsAreExclusiveAndSumToTheNumberOfMolecules() {
        Cell cell = new Cell(new Location(10, 10), 10);
        SpatialSimulation simulation = new SpatialSimulation(
                List.of(cell),
                List.of(new Molecule(new Location(10, 10)),
                        new Molecule(new Location(15, 10)),
                        new Molecule(new Location(18, 10))));

        SpatialSimulation.PopulationCounts counts = simulation.countStates();

        assertEquals(1, counts.intracellular());
        assertEquals(1, counts.border());
        assertEquals(1, counts.extracellular());
        assertEquals(3, counts.total());
        assertEquals(counts.total(), counts.intracellular() + counts.border() + counts.extracellular());
    }

    @Test
    void identicalInputsProduceIdenticalMovementTrajectories() {
        SpatialSimulation first = newSimulation();
        SpatialSimulation second = newSimulation();

        for (int step = 0; step < 50; step++) {
            first.simulate(0.2);
            second.simulate(0.2);
        }

        Location firstLocation = first.getMolecules().get(0).getLocation();
        Location secondLocation = second.getMolecules().get(0).getLocation();
        assertEquals(firstLocation.getX(), secondLocation.getX());
        assertEquals(firstLocation.getY(), secondLocation.getY());
    }

    @Test
    void spatialCsvContainsHeaderEveryMoleculeSampleAndValidStates() throws Exception {
        SpatialSimulation simulation = newSimulation();
        StringWriter output = new StringWriter();

        simulation.run(output);

        String[] lines = output.toString().strip().split("\\R");
        assertEquals("time,molecule,x,y,state", lines[0]);
        assertEquals(12, lines.length);
        assertEquals("border", lines[1].split(",")[4]);
        for (int rowIndex = 1; rowIndex < lines.length; rowIndex++) {
            String[] columns = lines[rowIndex].split(",");
            assertEquals(5, columns.length);
            assertTrue(List.of("intracellular", "border", "extracellular").contains(columns[4]));
        }
    }

    private SpatialSimulation newSimulation() {
        return new SpatialSimulation(
                List.of(new Cell(new Location(10, 10), 10)),
                List.of(new Molecule(new Location(15, 10))));
    }
}
