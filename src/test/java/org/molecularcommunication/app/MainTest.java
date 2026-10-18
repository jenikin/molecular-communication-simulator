package org.molecularcommunication.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @TempDir
    Path tempDir;

    @Test
    void invalidArgumentsAndModelNamesReturnStatusTwo() {
        assertEquals(2, Main.run(new String[0]));
        assertEquals(2, Main.run(new String[]{"--wrong", "spatial", "--config", "inputs.xlsx"}));
        assertEquals(2, Main.run(new String[]{"--model", "unknown", "--config", "inputs.xlsx"}));
        assertEquals(2, Main.run(new String[]{"--model", "spatial", "--config"}));
    }

    @Test
    void createTemplateCommandCreatesAReadableWorkbook() throws IOException {
        Path workbook = tempDir.resolve("cli-template.xlsx");

        assertEquals(0, Main.run(new String[]{"--create-template", workbook.toString()}));
        assertTrue(Files.exists(workbook));
        assertEquals(1, SimulationWorkbook.readSpatial(workbook).cells().size());
    }

    @Test
    void eachModelRunsFromWorkbookAndWritesReadableCsv() throws IOException {
        Path workbook = tempDir.resolve("inputs.xlsx");
        Path spatialCsv = tempDir.resolve("spatial.csv");
        Path concentrationCsv = tempDir.resolve("concentrations.csv");
        SimulationWorkbook.createTemplate(workbook);

        assertEquals(0, Main.run(new String[]{"--model", "spatial", "--config", workbook.toString(),
                "--output", spatialCsv.toString()}));
        assertEquals(0, Main.run(new String[]{"--model", "concentrations", "--config", workbook.toString(),
                "--output", concentrationCsv.toString()}));

        List<String> spatialLines = Files.readAllLines(spatialCsv);
        assertEquals("time,molecule,x,y,state", spatialLines.get(0));
        assertEquals(12, spatialLines.size());
        assertEquals(5, spatialLines.get(1).split(",").length);

        List<String> concentrationLines = Files.readAllLines(concentrationCsv);
        assertEquals("time,intracellular_input,extracellular_input,intracellular_output,extracellular_output",
                concentrationLines.get(0));
        assertEquals(12, concentrationLines.size());
        assertEquals(5, concentrationLines.get(1).split(",").length);
    }
}
