# Molecular Communication Simulator

This Java project brings together two related but distinct molecular communication models, as part of work done for a mentorship program. Select one simulation model for each run; they run independently and use different assumptions.

## Models

### **Spatial**

Tracks individual molecules moving by Gaussian random steps in a 2D bounded area and reports whether they are extracellular, intracellular, or on a cell boundary.

The model reads cell centers, diameters, and molecule starting locations from the workbook. Molecules move by Gaussian random steps within the fixed 20 × 20 area, and the output reports their locations and cell status.

### **Concentrations**

Evolves four well-mixed concentration values (intracellular input, extracellular input, intracellular output, and extracellular output) using the rate equations in the original concentration prototype.

The model reads the four initial concentration values from the workbook. It evolves those values using the rate equations below; its random input process, rates, and timing remain fixed in code.

Differential equations are used to calculate the changes in molecule concentrations intracellularly and extracellularly, based on three variables:

1. the exchange of input molecules from the extracellular to the intracellular environment,
2. the signal processing of output molecules through regulated gene expressions, and
3. the release of the output molecules from the intracellular to the extracellular environment.

The spatial model is an agent-based prototype. The concentration model is an aggregate ordinary-differential-equation prototype. The spatial model currently does not drive the concentration model, and molecule exchange/release behavior has not yet been implemented.

## Requirements

- JDK 17 or later
- No global Maven installation is required; the Maven Wrapper is included.
- Internet access is needed the first time the wrapper downloads Maven and project dependencies.

## Build

From this directory, build the executable JAR:

```bash
./mvnw -version
./mvnw clean package
```

On Windows, use `mvnw.cmd` in place of `./mvnw`.

## Tests

Run the Java unit and CLI smoke tests with:

```bash
./mvnw test
```

The JUnit suite covers concentration equations and deterministic CSV output, spatial classifications and movement, workbook defaults and validation, and command-line behavior for both models. Failsafe integration tests are not configured; use them if testing the packaged JAR becomes necessary.

Run the Python plotting tests with:

```bash
uv sync
uv run pytest
```

The pytest suite checks supported CSV schemas, plot structure and validation, legacy spatial exports, and PNG output for both simulation modes. Tests use Matplotlib's noninteractive `Agg` backend and do not compare rendered pixels.

## Workbook inputs

Create an Excel workbook template, then edit it in Excel, LibreOffice, or another `.xlsx` editor:

```bash
java -jar target/molecular-communication-simulator-1.0.0-SNAPSHOT.jar --create-template simulator-input.xlsx
```

The workbook contains these sheets:

- **Spatial Cells** — one cell per row with its `x`, `y`, and `diameter`.
- **Spatial Molecules** — one molecule per row with its starting `x` and `y`. Add or remove rows to change the molecule count.
- **Concentrations** — the four initial concentration values. The random input process and all model rates and timing remain fixed in code.

Spatial coordinates use the existing 20 × 20 area. Coordinates must be from 0 (inclusive) to 20 (exclusive); each cell must fit within the area. The workbook includes the current sample setup. Keep the sheet names and column headers unchanged. Invalid or missing inputs are reported when the workbook is loaded.

Run either model using the same workbook:

```bash
java -jar target/molecular-communication-simulator-1.0.0-SNAPSHOT.jar --model spatial --config simulator-input.xlsx --output spatial-results.csv
java -jar target/molecular-communication-simulator-1.0.0-SNAPSHOT.jar --model concentrations --config simulator-input.xlsx --output concentration-results.csv
```

The optional `--output` argument exports the sampled simulation history as a CSV with column headers that pandas can read directly. Concentration exports contain one row per recorded time and one column per concentration. Spatial exports contain one row per molecule per recorded time, including coordinates and cell status. The plotter also accepts headerless spatial CSVs from earlier builds. The output directory must already exist.

## Python environment and plotting

The project supports Python 3.12 and newer, and uses Python 3.15 as its local default through `.python-version`. Python 3.15 is the current stable release; pandas 3.0.6 is compatible with it. See the [Python 3.15 release](https://www.python.org/downloads/release/python-3150/) and [pandas 3.0.6 notes](https://pandas.pydata.org/pandas-docs/stable/whatsnew/v3.0.6.html). Install uv, then sync the project to create `.venv`, install dependencies, and write/update `uv.lock`:

```bash
uv sync
```

Create a static PNG graph with the project command:

```bash
uv run plot-results concentration-results.csv
uv run plot-results spatial-results.csv
```

The graph is saved next to its CSV by default. Add `--interactive` to open the graph in a matplotlib window with pan and zoom controls, while still saving the PNG:

```bash
uv run plot-results concentration-results.csv --interactive
```

Run the plotting script from the project environment with `uv run scripts/plot_results.py <results.csv>` when you need its other command-line options. Runtime dependencies are declared in `pyproject.toml`; `uv.lock` records the resolved versions for repeatable installs.

Each invocation runs only the selected model. The spatial model uses a fixed random seed so its trajectory is repeatable. The concentration model uses an explicit Euler time step of `0.0001`; it prints periodic samples rather than every integration step.

## Project layout

```text
src/main/java/org/molecularcommunication/
  app/                         command-line entry point
  models/concentration/        well-mixed concentration model
  models/spatial/              spatial molecule movement model
```

## Limitations, model assumptions and next work

The models are based on PhD research that was done by my mentor. In real world scenarios, both of the aforementioned models apply, but in this project they are treated separately due to coupling concerns.

As such, the main objective of this exercise is to practice OOP concepts in Java with a link to biological algorithms. Future work will include coupling the models for a more realistic simulation.
