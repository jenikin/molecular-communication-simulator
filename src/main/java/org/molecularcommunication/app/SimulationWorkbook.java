package org.molecularcommunication.app;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.molecularcommunication.models.spatial.Cell;
import org.molecularcommunication.models.spatial.Location;
import org.molecularcommunication.models.spatial.Molecule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads simulation inputs from, and creates, the Excel workbook used by the CLI. */
public final class SimulationWorkbook {
    private static final double AREA_WIDTH = 20;
    private static final double AREA_HEIGHT = 20;
    private static final String[] CONCENTRATION_NAMES = {
            "intracellular_input", "extracellular_input", "intracellular_output", "extracellular_output"
    };

    private SimulationWorkbook() {
    }

    public static SpatialInputs readSpatial(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            List<Cell> cells = readCells(requiredSheet(workbook, "Spatial Cells"));
            List<Molecule> molecules = readMolecules(requiredSheet(workbook, "Spatial Molecules"));
            if (cells.isEmpty()) {
                throw new IllegalArgumentException("Spatial Cells must contain at least one cell.");
            }
            return new SpatialInputs(cells, molecules);
        }
    }

    public static double[] readConcentrations(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path); XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = requiredSheet(workbook, "Concentrations");
            requireHeader(sheet, 0, "concentration", "initial_value");
            double[] values = new double[CONCENTRATION_NAMES.length];
            for (int index = 0; index < CONCENTRATION_NAMES.length; index++) {
                Row row = sheet.getRow(index + 1);
                if (row == null || !CONCENTRATION_NAMES[index].equals(text(row.getCell(0)))) {
                    throw new IllegalArgumentException("Concentrations row " + (index + 2)
                            + " must be named " + CONCENTRATION_NAMES[index] + ".");
                }
                values[index] = number(row.getCell(1), "Concentrations!B" + (index + 2));
                if (values[index] < 0) {
                    throw new IllegalArgumentException("Concentrations!B" + (index + 2)
                            + " must be zero or greater.");
                }
            }
            return values;
        }
    }

    public static void createTemplate(Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath();
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet instructions = workbook.createSheet("Instructions");
            instructions.createRow(0).createCell(0).setCellValue("Molecular Communication Simulator Inputs");
            instructions.createRow(1).createCell(0).setCellValue(
                    "Edit the yellow input cells, save the workbook, then run the simulator with --config.");
            instructions.createRow(2).createCell(0).setCellValue(
                    "Spatial coordinates use a fixed 20 x 20 area. X and Y must be from 0 (inclusive) to 20 (exclusive).");
            instructions.createRow(3).createCell(0).setCellValue(
                    "Add rows to Spatial Cells or Spatial Molecules to add more objects. Do not change column headers.");
            instructions.createRow(4).createCell(0).setCellValue(
                    "For the concentrations model, edit only the four initial values. Rates, random input, and timing stay fixed.");
            instructions.setColumnWidth(0, 115 * 256);

            Sheet cells = workbook.createSheet("Spatial Cells");
            header(cells, "x", "y", "diameter");
            numericRow(cells, 1, 10, 10, 10);
            styleInputs(workbook, cells, 1, 1, 3);
            cells.setColumnWidth(0, 14 * 256);
            cells.setColumnWidth(1, 14 * 256);
            cells.setColumnWidth(2, 18 * 256);

            Sheet molecules = workbook.createSheet("Spatial Molecules");
            header(molecules, "x", "y");
            numericRow(molecules, 1, 15, 10);
            styleInputs(workbook, molecules, 1, 1, 2);
            molecules.setColumnWidth(0, 14 * 256);
            molecules.setColumnWidth(1, 14 * 256);

            Sheet concentrations = workbook.createSheet("Concentrations");
            header(concentrations, "concentration", "initial_value");
            double[] defaults = {0.3, 0.4, 0.3, 0.2};
            for (int i = 0; i < CONCENTRATION_NAMES.length; i++) {
                Row row = concentrations.createRow(i + 1);
                row.createCell(0).setCellValue(CONCENTRATION_NAMES[i]);
                row.createCell(1).setCellValue(defaults[i]);
            }
            styleInputs(workbook, concentrations, 1, 1, 1);
            concentrations.setColumnWidth(0, 32 * 256);
            concentrations.setColumnWidth(1, 18 * 256);

            try (OutputStream output = Files.newOutputStream(absolutePath)) {
                workbook.write(output);
            }
        }
    }

    private static List<Cell> readCells(Sheet sheet) {
        requireHeader(sheet, 0, "x", "y", "diameter");
        List<Cell> cells = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlank(row, 3)) {
                continue;
            }
            double x = number(row.getCell(0), "Spatial Cells!A" + (rowIndex + 1));
            double y = number(row.getCell(1), "Spatial Cells!B" + (rowIndex + 1));
            double diameter = number(row.getCell(2), "Spatial Cells!C" + (rowIndex + 1));
            requireCoordinate(x, y, "Spatial Cells row " + (rowIndex + 1));
            if (diameter <= 0 || x - diameter / 2 < 0 || x + diameter / 2 > AREA_WIDTH
                    || y - diameter / 2 < 0 || y + diameter / 2 > AREA_HEIGHT) {
                throw new IllegalArgumentException("Spatial Cells row " + (rowIndex + 1)
                        + " must have a positive diameter and fit within the 20 x 20 area.");
            }
            cells.add(new Cell(new Location(x, y), diameter));
        }
        return cells;
    }

    private static List<Molecule> readMolecules(Sheet sheet) {
        requireHeader(sheet, 0, "x", "y");
        List<Molecule> molecules = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (isBlank(row, 2)) {
                continue;
            }
            double x = number(row.getCell(0), "Spatial Molecules!A" + (rowIndex + 1));
            double y = number(row.getCell(1), "Spatial Molecules!B" + (rowIndex + 1));
            requireCoordinate(x, y, "Spatial Molecules row " + (rowIndex + 1));
            molecules.add(new Molecule(new Location(x, y)));
        }
        return molecules;
    }

    private static void requireCoordinate(double x, double y, String label) {
        if (x < 0 || x >= AREA_WIDTH || y < 0 || y >= AREA_HEIGHT) {
            throw new IllegalArgumentException(label + " coordinates must be within [0, 20) on both axes.");
        }
    }

    private static Sheet requiredSheet(XSSFWorkbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new IllegalArgumentException("Workbook is missing the '" + name + "' sheet.");
        }
        return sheet;
    }

    private static void requireHeader(Sheet sheet, int rowIndex, String... expected) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            throw new IllegalArgumentException("Sheet '" + sheet.getSheetName() + "' is missing its header row.");
        }
        for (int index = 0; index < expected.length; index++) {
            if (!expected[index].equals(text(row.getCell(index)))) {
                throw new IllegalArgumentException("Sheet '" + sheet.getSheetName()
                        + "' must use these column headers in order: " + String.join(", ", expected) + ".");
            }
        }
    }

    private static double number(org.apache.poi.ss.usermodel.Cell cell, String label) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            throw new IllegalArgumentException(label + " must contain a number.");
        }
        double value = cell.getNumericCellValue();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be a finite number.");
        }
        return value;
    }

    private static String text(org.apache.poi.ss.usermodel.Cell cell) {
        return cell != null && cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "";
    }

    private static boolean isBlank(Row row, int columnCount) {
        if (row == null) {
            return true;
        }
        for (int column = 0; column < columnCount; column++) {
            org.apache.poi.ss.usermodel.Cell cell = row.getCell(column);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !(cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank())) {
                return false;
            }
        }
        return true;
    }

    private static void header(Sheet sheet, String... values) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < values.length; index++) {
            row.createCell(index).setCellValue(values[index]);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void numericRow(Sheet sheet, int rowIndex, double... values) {
        Row row = sheet.createRow(rowIndex);
        for (int index = 0; index < values.length; index++) {
            row.createCell(index).setCellValue(values[index]);
        }
    }

    private static void styleInputs(XSSFWorkbook workbook, Sheet sheet, int firstRow, int lastRow,
                                    int lastColumn) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            for (int column = 0; column < lastColumn; column++) {
                row.getCell(column).setCellStyle(style);
            }
        }
    }

    public record SpatialInputs(List<Cell> cells, List<Molecule> molecules) {
        public SpatialInputs {
            cells = List.copyOf(cells);
            molecules = List.copyOf(molecules);
        }
    }
}
