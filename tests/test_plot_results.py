"""Regression tests for the simulator results plotter."""

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import pandas as pd
import pytest

from scripts import plot_results

CONCENTRATION_COLUMNS = [
    "time",
    "intracellular_input",
    "extracellular_input",
    "intracellular_output",
    "extracellular_output",
]
SPATIAL_COLUMNS = ["time", "molecule", "x", "y", "state"]


@pytest.fixture(autouse=True)
def close_figures():
    """Keep plots from one test isolated from the next."""
    yield
    plt.close("all")


@pytest.fixture
def concentration_data():
    return pd.DataFrame(
        {
            "time": [0.0, 1.0, 2.0],
            "intracellular_input": [0.0, 0.2, 0.4],
            "extracellular_input": [0.0, 0.1, 0.2],
            "intracellular_output": [0.0, 0.3, 0.5],
            "extracellular_output": [0.0, 0.05, 0.15],
        }
    )


@pytest.fixture
def spatial_data():
    return pd.DataFrame(
        [
            (0.0, "molecule_1", 2.0, 3.0, "extracellular"),
            (1.0, "molecule_1", 4.0, 5.0, "intracellular"),
            (0.0, "molecule_2", 8.0, 9.0, "extracellular"),
            (1.0, "molecule_2", 7.0, 11.0, "border"),
        ],
        columns=SPATIAL_COLUMNS,
    )


@pytest.mark.parametrize(
    "columns",
    [CONCENTRATION_COLUMNS, SPATIAL_COLUMNS],
    ids=["concentration", "spatial"],
)
def test_has_known_columns_accepts_supported_schemas(columns):
    assert plot_results._has_known_columns(pd.DataFrame(columns=columns))


@pytest.mark.parametrize(
    "columns",
    [["time", "intracellular_input"], ["time", "molecule", "x", "y"]],
    ids=["incomplete-concentration", "incomplete-spatial"],
)
def test_has_known_columns_rejects_incomplete_schemas(columns):
    assert not plot_results._has_known_columns(pd.DataFrame(columns=columns))


def test_plot_concentrations_sets_labels_and_four_series(concentration_data):
    plot_results.plot_concentrations(concentration_data, "Concentration run")

    axes = plt.gcf().axes
    assert len(axes) == 1
    axis = axes[0]
    assert axis.get_title() == "Concentration run"
    assert axis.get_xlabel() == "Time"
    assert axis.get_ylabel() == "Concentration"
    assert len(axis.lines) == 4


def test_plot_concentrations_rejects_missing_required_column(concentration_data):
    with pytest.raises(ValueError, match="Concentration CSV is missing columns: time"):
        plot_results.plot_concentrations(
            concentration_data.drop(columns="time"), "Incomplete concentration run"
        )


def test_plot_spatial_sets_bounds_aspect_and_trajectories(spatial_data):
    plot_results.plot_spatial(spatial_data, "Spatial run")

    axis = plt.gcf().axes[0]
    assert axis.get_title() == "Spatial run"
    assert axis.get_xlabel() == "X location"
    assert axis.get_ylabel() == "Y location"
    assert axis.get_xlim() == (0.0, 20.0)
    assert axis.get_ylim() == (0.0, 20.0)
    assert axis.get_aspect() == 1.0
    assert len(axis.lines) == 2


@pytest.mark.parametrize(
    "data, message",
    [
        (
            pd.DataFrame(columns=["time", "molecule", "x", "y"]),
            "Spatial CSV is missing columns: state",
        ),
        (
            pd.DataFrame(columns=SPATIAL_COLUMNS),
            "Spatial CSV contains no molecule positions to plot",
        ),
    ],
    ids=["missing-column", "empty-data"],
)
def test_plot_spatial_rejects_invalid_data(data, message):
    with pytest.raises(ValueError, match=message):
        plot_results.plot_spatial(data, "Invalid spatial run")


@pytest.mark.parametrize(
    "data, default_output",
    [
        (
            pd.DataFrame(
                {
                    "time": [0.0, 1.0],
                    "intracellular_input": [0.0, 0.2],
                    "extracellular_input": [0.0, 0.1],
                    "intracellular_output": [0.0, 0.3],
                    "extracellular_output": [0.0, 0.05],
                }
            ),
            "concentration-results.png",
        ),
        (
            pd.DataFrame(
                [(0.0, "molecule_1", 2.0, 3.0, "extracellular")],
                columns=SPATIAL_COLUMNS,
            ),
            "spatial-results.png",
        ),
    ],
    ids=["concentration", "spatial"],
)
@pytest.mark.parametrize(
    "explicit_output", [False, True], ids=["default-output", "explicit-output"]
)
def test_main_saves_nonempty_png_for_each_model(
    tmp_path, monkeypatch, data, default_output, explicit_output
):
    csv_path = tmp_path / default_output.replace(".png", ".csv")
    data.to_csv(csv_path, index=False)
    if explicit_output:
        output_path = tmp_path / "nested" / "charts" / "custom.png"
        monkeypatch.setattr(
            "sys.argv", ["plot-results", str(csv_path), "--output", str(output_path)]
        )
    else:
        output_path = csv_path.with_suffix(".png")
        monkeypatch.setattr("sys.argv", ["plot-results", str(csv_path)])

    plot_results.main()

    assert output_path.is_file()
    assert output_path.stat().st_size > 0
    assert plt.get_fignums() == []


def test_main_recognizes_legacy_headerless_spatial_csv(tmp_path, monkeypatch):
    csv_path = tmp_path / "legacy-spatial.csv"
    csv_path.write_text(
        "0,molecule_1,2.0,3.0,extracellular\n"
        "1,molecule_1,4.0,5.0,intracellular\n",
        encoding="utf-8",
    )
    monkeypatch.setattr("sys.argv", ["plot-results", str(csv_path)])

    plot_results.main()

    output_path = csv_path.with_suffix(".png")
    assert output_path.is_file()
    assert output_path.stat().st_size > 0


def test_main_interactive_mode_saves_and_shows_png(tmp_path, monkeypatch):
    csv_path = tmp_path / "interactive.csv"
    pd.DataFrame(
        {
            "time": [0.0, 1.0],
            "intracellular_input": [0.0, 0.2],
            "extracellular_input": [0.0, 0.1],
            "intracellular_output": [0.0, 0.3],
            "extracellular_output": [0.0, 0.05],
        }
    ).to_csv(csv_path, index=False)
    shown = []
    monkeypatch.setattr(plt, "show", lambda: shown.append(True))
    monkeypatch.setattr("sys.argv", ["plot-results", str(csv_path), "--interactive"])

    plot_results.main()

    output_path = csv_path.with_suffix(".png")
    assert output_path.is_file()
    assert output_path.stat().st_size > 0
    assert shown == [True]


def test_main_rejects_unsupported_schema_and_closes_figure(tmp_path, monkeypatch):
    csv_path = tmp_path / "unsupported.csv"
    pd.DataFrame({"first": [1], "second": [2]}).to_csv(csv_path, index=False)
    monkeypatch.setattr("sys.argv", ["plot-results", str(csv_path)])

    with pytest.raises(ValueError, match="CSV columns do not match"):
        plot_results.main()

    assert plt.get_fignums() == []
    assert not csv_path.with_suffix(".png").exists()
