package org.molecularcommunication.models.concentration;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcentrationSimulationTest {
    @Test
    void hillResponseHasExpectedShapeAndValues() {
        double atPointTwo = ConcentrationSimulation.hillFunction(0.2);
        double atPointFour = ConcentrationSimulation.hillFunction(0.4);

        assertEquals(0.0, ConcentrationSimulation.hillFunction(0.0));
        assertEquals(1.0 / 6.0, atPointTwo, 1e-12);
        assertEquals(2.0 / 7.0, atPointFour, 1e-12);
        assertTrue(atPointFour > atPointTwo);
        assertTrue(atPointTwo < 1.0);
        assertTrue(atPointFour < 1.0);
        assertFalse(Math.abs(atPointTwo - 0.5) < 1e-6);
        assertFalse(Math.abs(atPointFour - 0.5) < 1e-6);
    }

    @Test
    void extracellularOutputReceivesTheOneStepReleaseFlux() {
        double initialIntracellularOutput = 0.6;
        double timeStep = 1e-5;
        ConcentrationSimulation simulation = new ConcentrationSimulation(
                new double[]{0.2, 0.0, initialIntracellularOutput, 0.2});

        simulation.simulate(timeStep);

        double[] result = simulation.getConcentrations();
        double expectedRelease = timeStep * 1e4 * initialIntracellularOutput;
        assertEquals(expectedRelease, result[3] - 0.2, 1e-14);
    }

    @Test
    void releaseTransfersOutputAndDegradationAccountsForRemainingLoss() {
        double initialIntracellularOutput = 0.6;
        double initialExtracellularOutput = 0.2;
        double timeStep = 1e-5;
        ConcentrationSimulation simulation = new ConcentrationSimulation(
                new double[]{0.2, 0.0, initialIntracellularOutput, initialExtracellularOutput});

        simulation.simulate(timeStep);

        double[] result = simulation.getConcentrations();
        double initialTotalOutput = initialIntracellularOutput + initialExtracellularOutput;
        double finalTotalOutput = result[2] + result[3];
        double degradationLoss = timeStep * 1e-5 * initialIntracellularOutput;
        assertEquals(-degradationLoss, finalTotalOutput - initialTotalOutput, 1e-14);
    }

    @Test
    void runsWithTheSameInitialValuesProduceIdenticalCsv() throws Exception {
        String firstRun = runToCsv(new double[]{0.3, 0.4, 0.3, 0.2});
        String secondRun = runToCsv(new double[]{0.3, 0.4, 0.3, 0.2});

        assertEquals(firstRun, secondRun);
    }

    @Test
    void concentrationCsvHasExpectedColumnsAndSampleTimes() throws Exception {
        String csv = runToCsv(new double[]{0.3, 0.4, 0.3, 0.2});
        String[] lines = csv.strip().split("\\R");

        assertEquals("time,intracellular_input,extracellular_input,intracellular_output,extracellular_output",
                lines[0]);
        assertEquals(12, lines.length, "one header plus samples from t=0 through t=20");
        for (int sampleIndex = 0; sampleIndex < 11; sampleIndex++) {
            String[] columns = lines[sampleIndex + 1].split(",");
            assertEquals(5, columns.length);
            assertEquals(sampleIndex * 2.0, Double.parseDouble(columns[0]), 1e-9);
        }
    }

    private String runToCsv(double[] initialConcentrations) throws Exception {
        StringWriter output = new StringWriter();
        new ConcentrationSimulation(initialConcentrations).run(output);
        return output.toString();
    }
}
