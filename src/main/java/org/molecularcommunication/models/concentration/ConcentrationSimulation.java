package org.molecularcommunication.models.concentration;

import java.io.IOException;
import java.io.Writer;
import java.util.Random;

/** A well-mixed model of four intracellular and extracellular concentrations. */
public final class ConcentrationSimulation {
    private static final double EXCHANGE_RATE = 1e-10;
    private static final double DEGRADATION_RATE = 1e-5;
    private static final double PROTEIN_PRODUCTION_RATE = 10;
    private static final double DATA_FITTING_PARAMETER = 1;
    private static final double HILL_COEFFICIENT = 1;
    private static final double RELEASE_RATE = 1e4;
    private static final double END_TIME = 20;
    // The release term is fast; this small Euler step avoids the instability of the original 0.5 step.
    private static final double TIME_STEP = 1e-4;
    private static final int PRINT_EVERY_STEPS = 20_000;

    private final Random random = new Random(42L);
    private double[] concentrations;

    public ConcentrationSimulation(double[] initialConcentrations) {
        if (initialConcentrations.length != 4) {
            throw new IllegalArgumentException("Exactly four initial concentrations are required.");
        }
        concentrations = initialConcentrations.clone();
    }

    public void run() {
        try {
            run(null);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not export simulation results.", exception);
        }
    }

    double[] getConcentrations() {
        return concentrations.clone();
    }

    public void run(Writer csvOutput) throws IOException {
        System.out.println("Concentration model (explicit Euler)");
        if (csvOutput != null) {
            csvOutput.write("time,intracellular_input,extracellular_input,intracellular_output,extracellular_output\n");
        }
        printConcentrations(0);
        writeConcentrations(csvOutput, 0);

        int steps = (int) Math.round(END_TIME / TIME_STEP);
        for (int step = 1; step <= steps; step++) {
            simulate(TIME_STEP);
            if (step % PRINT_EVERY_STEPS == 0) {
                double time = step * TIME_STEP;
                printConcentrations(time);
                writeConcentrations(csvOutput, time);
            }
        }
    }

    private void writeConcentrations(Writer output, double time) throws IOException {
        if (output == null) {
            return;
        }
        output.write(String.format(java.util.Locale.ROOT, "%.4f,%.12g,%.12g,%.12g,%.12g%n",
                time, concentrations[0], concentrations[1], concentrations[2], concentrations[3]));
    }

    void simulate(double timeStep) {
        double[] current = concentrations;
        double[] next = new double[4];
        next[0] = random.nextDouble();
        next[1] = current[1] + timeStep * (EXCHANGE_RATE * current[0] - DEGRADATION_RATE * current[1]);
        next[2] = current[2] + timeStep * (PROTEIN_PRODUCTION_RATE * hillFunction(current[1])
                - (DEGRADATION_RATE + RELEASE_RATE) * current[2]);
        next[3] = current[3] + timeStep * RELEASE_RATE * current[2];
        concentrations = next;
    }

    static double hillFunction(double inputConcentration) {
        double poweredInput = Math.pow(inputConcentration, HILL_COEFFICIENT);
        return poweredInput / (1 + Math.pow(DATA_FITTING_PARAMETER * inputConcentration, HILL_COEFFICIENT));
    }

    private void printConcentrations(double time) {
        System.out.printf("t=%.1f, intracellular-input=%.6f, extracellular-input=%.6f, "
                        + "intracellular-output=%.6f, extracellular-output=%.6f%n",
                time, concentrations[0], concentrations[1], concentrations[2], concentrations[3]);
    }
}
