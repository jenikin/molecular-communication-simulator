#!/usr/bin/env python3
"""Plot a simulator CSV export using pandas and matplotlib."""

import argparse
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd
from matplotlib.colors import Normalize


def plot_concentrations(data: pd.DataFrame, title: str) -> None:
    required = {
        "time",
        "intracellular_input",
        "extracellular_input",
        "intracellular_output",
        "extracellular_output",
    }
    missing = required.difference(data.columns)
    if missing:
        raise ValueError(f"Concentration CSV is missing columns: {', '.join(sorted(missing))}")

    ax = plt.gca()
    data.plot(x="time", y=sorted(required - {"time"}), marker="o", ax=ax)
    ax.set_title(title)
    ax.set_xlabel("Time")
    ax.set_ylabel("Concentration")
    ax.grid(True, alpha=0.3)


def plot_spatial(data: pd.DataFrame, title: str) -> None:
    required = {"time", "molecule", "x", "y", "state"}
    missing = required.difference(data.columns)
    if missing:
        raise ValueError(f"Spatial CSV is missing columns: {', '.join(sorted(missing))}")
    if data.empty:
        raise ValueError("Spatial CSV contains no molecule positions to plot.")

    ax = plt.gca()
    time_values = data["time"].astype(float)
    time_norm = Normalize(vmin=time_values.min(), vmax=time_values.max() or 1.0)
    scatter = None
    for name, track in data.groupby("molecule", sort=True):
        track = track.sort_values("time")
        ax.plot(track["x"], track["y"], linewidth=1, alpha=0.65, label=name)
        scatter = ax.scatter(
            track["x"],
            track["y"],
            c=track["time"],
            cmap="viridis",
            norm=time_norm,
            s=24,
        )
        ax.scatter(track["x"].iloc[0], track["y"].iloc[0], marker="o", s=65, edgecolors="black")
        ax.scatter(track["x"].iloc[-1], track["y"].iloc[-1], marker="X", s=65, edgecolors="black")

    ax.set_title(title)
    ax.set_xlabel("X location")
    ax.set_ylabel("Y location")
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 20)
    ax.set_aspect("equal", adjustable="box")
    ax.grid(True, alpha=0.3)
    if data["molecule"].nunique() <= 12:
        ax.legend(title="Molecule", loc="best")
    if scatter is not None:
        plt.colorbar(scatter, ax=ax, label="Time")
    ax.text(
        0.01,
        0.01,
        "Circle = start   X = end",
        transform=ax.transAxes,
        fontsize=8,
        va="bottom",
    )


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("csv", type=Path, help="CSV file exported by the simulator")
    parser.add_argument(
        "--output",
        type=Path,
        help="PNG output path (default: alongside the CSV with a .png extension)",
    )
    parser.add_argument(
        "--interactive",
        action="store_true",
        help="also open the chart in a matplotlib window for pan and zoom",
    )
    args = parser.parse_args()

    data = pd.read_csv(args.csv)
    if not _has_known_columns(data):
        # Earlier spatial exports omitted a header. Keep those files usable.
        headerless = pd.read_csv(args.csv, header=None)
        if headerless.shape[1] == 5:
            headerless.columns = ["time", "molecule", "x", "y", "state"]
            try:
                headerless["time"] = pd.to_numeric(headerless["time"], errors="raise")
                headerless["x"] = pd.to_numeric(headerless["x"], errors="raise")
                headerless["y"] = pd.to_numeric(headerless["y"], errors="raise")
            except (TypeError, ValueError):
                pass
            else:
                data = headerless

    output = args.output or args.csv.with_suffix(".png")
    title = f"Simulation results — {args.csv.stem}"

    figure = plt.figure(figsize=(10, 6))
    concentration_columns = {
        "intracellular_input",
        "extracellular_input",
        "intracellular_output",
        "extracellular_output",
    }
    try:
        if concentration_columns.issubset(data.columns):
            plot_concentrations(data, title)
        elif {"molecule", "x", "y", "state"}.issubset(data.columns):
            plot_spatial(data, title)
        else:
            raise ValueError(
                "CSV columns do not match a concentration or spatial simulation export."
            )

        plt.tight_layout()
        output.parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(output, dpi=160, bbox_inches="tight")
        print(f"Saved graph to {output}")
        if args.interactive:
            plt.show()
    except Exception:
        plt.close(figure)
        raise
    else:
        if not args.interactive:
            plt.close(figure)


def _has_known_columns(data: pd.DataFrame) -> bool:
    concentration_columns = {
        "time",
        "intracellular_input",
        "extracellular_input",
        "intracellular_output",
        "extracellular_output",
    }
    spatial_columns = {"time", "molecule", "x", "y", "state"}
    return concentration_columns.issubset(data.columns) or spatial_columns.issubset(data.columns)


if __name__ == "__main__":
    main()
