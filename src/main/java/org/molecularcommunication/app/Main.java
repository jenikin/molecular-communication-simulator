package org.molecularcommunication.app;

import org.molecularcommunication.models.concentration.ConcentrationSimulation;
import org.molecularcommunication.models.spatial.SpatialSimulation;

import java.nio.file.Path;
import java.io.Writer;
import java.nio.file.Files;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        if (args.length == 2 && "--create-template".equals(args[0])) {
            try {
                SimulationWorkbook.createTemplate(Path.of(args[1]));
                System.out.println("Created workbook template: " + args[1]);
                return 0;
            } catch (Exception exception) {
                return reportError(exception);
            }
        }

        if ((args.length != 4 && args.length != 6) || !"--model".equals(args[0])
                || !"--config".equals(args[2])
                || (args.length == 6 && !"--output".equals(args[4]))) {
            printUsage();
            return 2;
        }

        String model = args[1].toLowerCase();
        if (!"spatial".equals(model) && !"concentrations".equals(model)) {
            printUsage();
            return 2;
        }
        Path workbook = Path.of(args[3]);
        Path csvPath = args.length == 6 ? Path.of(args[5]) : null;
        SimulationWorkbook.SpatialInputs spatialInputs = null;
        double[] concentrations = null;
        try {
            if ("spatial".equals(model)) {
                spatialInputs = SimulationWorkbook.readSpatial(workbook);
            } else {
                concentrations = SimulationWorkbook.readConcentrations(workbook);
            }
        } catch (Exception exception) {
            return reportError(exception);
        }

        try (Writer csvOutput = csvPath == null ? null : Files.newBufferedWriter(csvPath)) {
            switch (model) {
                case "spatial" -> new SpatialSimulation(spatialInputs.cells(), spatialInputs.molecules())
                        .run(csvOutput);
                case "concentrations" -> new ConcentrationSimulation(concentrations).run(csvOutput);
                default -> throw new IllegalStateException("Unexpected model: " + model);
            }
            if (csvPath != null) {
                System.out.println("Results saved to " + csvPath);
            }
            return 0;
        } catch (Exception exception) {
            return reportError(exception);
        }
    }

    private static int reportError(Exception exception) {
        System.err.println("Error: " + exception.getMessage());
        return 2;
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar molecular-communication-simulator.jar --create-template <path.xlsx>");
        System.out.println("  java -jar molecular-communication-simulator.jar --model <spatial|concentrations> --config <path.xlsx>");
        System.out.println("      [--output <results.csv>]");
    }
}
