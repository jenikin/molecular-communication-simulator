package org.molecularcommunication.app;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationWorkbookTest {
    @TempDir
    Path tempDir;

    @Test
    void generatedTemplateReadsBackItsDefaultInputs() throws IOException {
        Path workbook = template("defaults.xlsx");

        SimulationWorkbook.SpatialInputs spatial = SimulationWorkbook.readSpatial(workbook);
        double[] concentrations = SimulationWorkbook.readConcentrations(workbook);

        assertEquals(1, spatial.cells().size());
        assertEquals(10, spatial.cells().get(0).getLocation().getX());
        assertEquals(10, spatial.cells().get(0).getLocation().getY());
        assertEquals(10, spatial.cells().get(0).getDiameter());
        assertEquals(1, spatial.molecules().size());
        assertEquals(15, spatial.molecules().get(0).getLocation().getX());
        assertEquals(10, spatial.molecules().get(0).getLocation().getY());
        assertArrayEquals(new double[]{0.3, 0.4, 0.3, 0.2}, concentrations, 1e-12);
    }

    @Test
    void rejectsMissingSheet() throws IOException {
        Path workbook = tempDir.resolve("missing-sheet.xlsx");
        try (XSSFWorkbook xssfWorkbook = new XSSFWorkbook()) {
            xssfWorkbook.createSheet("Spatial Molecules");
            write(xssfWorkbook, workbook);
        }

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> SimulationWorkbook.readSpatial(workbook));
        assertTrue(exception.getMessage().contains("Spatial Cells"));
    }

    @Test
    void rejectsIncorrectHeaders() throws IOException {
        Path workbook = template("wrong-header.xlsx");
        edit(workbook, xssfWorkbook -> xssfWorkbook.getSheet("Spatial Cells")
                .getRow(0).getCell(0).setCellValue("latitude"));

        assertThrows(IllegalArgumentException.class, () -> SimulationWorkbook.readSpatial(workbook));
    }

    @Test
    void rejectsMissingAndNonnumericSpatialValues() throws IOException {
        Path missingValue = template("missing-value.xlsx");
        edit(missingValue, xssfWorkbook -> xssfWorkbook.getSheet("Spatial Cells")
                .getRow(1).removeCell(xssfWorkbook.getSheet("Spatial Cells").getRow(1).getCell(1)));
        assertThrows(IllegalArgumentException.class, () -> SimulationWorkbook.readSpatial(missingValue));

        Path nonnumericValue = template("nonnumeric-value.xlsx");
        edit(nonnumericValue, xssfWorkbook -> xssfWorkbook.getSheet("Spatial Cells")
                .getRow(1).getCell(1).setCellValue("ten"));
        assertThrows(IllegalArgumentException.class, () -> SimulationWorkbook.readSpatial(nonnumericValue));
    }

    @Test
    void rejectsOutOfBoundsCoordinatesAndNonpositiveDiameter() throws IOException {
        Path invalidCoordinate = template("invalid-coordinate.xlsx");
        edit(invalidCoordinate, xssfWorkbook -> xssfWorkbook.getSheet("Spatial Molecules")
                .getRow(1).getCell(0).setCellValue(20));
        assertThrows(IllegalArgumentException.class, () -> SimulationWorkbook.readSpatial(invalidCoordinate));

        Path invalidDiameter = template("invalid-diameter.xlsx");
        edit(invalidDiameter, xssfWorkbook -> xssfWorkbook.getSheet("Spatial Cells")
                .getRow(1).getCell(2).setCellValue(0));
        assertThrows(IllegalArgumentException.class, () -> SimulationWorkbook.readSpatial(invalidDiameter));
    }

    @Test
    void rejectsNegativeConcentrations() throws IOException {
        Path workbook = template("negative-concentration.xlsx");
        edit(workbook, xssfWorkbook -> xssfWorkbook.getSheet("Concentrations")
                .getRow(1).getCell(1).setCellValue(-0.1));

        assertThrows(IllegalArgumentException.class,
                () -> SimulationWorkbook.readConcentrations(workbook));
    }

    @Test
    void ignoresBlankRowsInSpatialTables() throws IOException {
        Path workbook = template("blank-rows.xlsx");
        edit(workbook, xssfWorkbook -> {
            xssfWorkbook.getSheet("Spatial Cells").createRow(10);
            xssfWorkbook.getSheet("Spatial Molecules").createRow(10);
        });

        SimulationWorkbook.SpatialInputs spatial = SimulationWorkbook.readSpatial(workbook);
        assertEquals(1, spatial.cells().size());
        assertEquals(1, spatial.molecules().size());
    }

    private Path template(String fileName) throws IOException {
        Path path = tempDir.resolve(fileName);
        SimulationWorkbook.createTemplate(path);
        return path;
    }

    private void edit(Path path, Consumer<XSSFWorkbook> editor) throws IOException {
        XSSFWorkbook workbook;
        try (InputStream input = Files.newInputStream(path)) {
            workbook = new XSSFWorkbook(input);
        }
        try (workbook) {
            editor.accept(workbook);
            write(workbook, path);
        }
    }

    private void write(XSSFWorkbook workbook, Path path) throws IOException {
        try (OutputStream output = Files.newOutputStream(path)) {
            workbook.write(output);
        }
    }
}
