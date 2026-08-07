#!/usr/bin/env python3
"""Time-normalized 2Q gate reduction rate vs time figure (paper style).

Artifact version: instead of the original per-tool log scrapers (author's
local layout), the baseline curves come from the suite-aggregated CSVs
    paper_results/<tool>_<gateset>_2q_over_time.csv   (time_s,total_2q)
and our curve is reconstructed from the strict-improvement lines
    Progress 2q: <n> (total <m>) at <t>s
in an optimize.sh output directory (--ours-dir).

reduction(t) = 100 * (total_2q(0) - total_2q(t)) / total_2q(0)

Usage:
    python3 plot_2q_reduction.py --gateset ibmnew \
        --ours-dir /root/results_ibmnew_trial1 --outdir <dir> [--linear]

Outputs into --outdir:
    2q_reduction_vs_time_<gateset>_{log|linear}.pdf/.png
    2q_reduction_legend.pdf/.png
"""
import os
import re
import argparse
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import pandas as pd
from pathlib import Path

# Geometric time axis: 0.01s to 3600s (60 min), log-spaced so curves start near 0%
TIME_POINTS = np.geomspace(0.01, 3600, 721)

CSV_TOOLS = {          # csv tool key -> display name
    'guoq': 'GUOQ',
    'queso': 'QUESO',
    'quartz': 'Quartz',
    'qiskit': 'Qiskit',
    'tket': 'tket',
}



# ── helpers ──────────────────────────────────────────────────────────────────

def step_interpolate(times, values, time_points):
    """
    Step-function interpolation: for each query point, take the last known value.
    times and values must be co-sorted by time.
    """
    t_arr = np.array(times, dtype=float)
    v_arr = np.array(values, dtype=float)
    result = np.empty(len(time_points))
    for i, tp in enumerate(time_points):
        idx = np.searchsorted(t_arr, tp, side='right') - 1
        if idx < 0:
            result[i] = v_arr[0]
        else:
            result[i] = v_arr[min(idx, len(v_arr) - 1)]
    return result



# ── data loaders (artifact layout) ──────────────────────────────────────────

def load_tool_csv(paper_results, tool, gateset):
    """Suite-aggregated (times, totals) from <tool>_<gateset>_2q_over_time.csv."""
    path = os.path.join(paper_results, f'{tool}_{gateset}_2q_over_time.csv')
    if not os.path.exists(path):
        return None
    df = pd.read_csv(path).sort_values('time_s')
    return df['time_s'].values.astype(float), df['total_2q'].values.astype(float)


# ── main ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--gateset', required=True, choices=['nam', 'ibmnew'])
    parser.add_argument('--ours', default=None, metavar='CSV',
                        help='QSymb suite-aggregated over-time CSV (time_s,total_2q); '
                             'default: <paper-results>/qsymb_<gateset>_2q_over_time.csv. '
                             'Produce one from a run dir with export_2q_over_time.py.')
    parser.add_argument('--paper-results', default='/root/paper_results')
    parser.add_argument('--outdir', default='/root/paper_results/figures',
                        help='output directory (default: paper_results/figures)')
    parser.add_argument('--linear', action='store_true',
                        help='Use linear x-axis instead of log scale')
    args = parser.parse_args()
    log_scale = not args.linear

    os.makedirs(args.outdir, exist_ok=True)

    # Build per-tool reduction-rate series on the common time axis.
    tool_stats = {}

    ours_csv = args.ours or os.path.join(args.paper_results,
                                         f'qsymb_{args.gateset}_2q_over_time.csv')
    df_o = pd.read_csv(ours_csv).sort_values('time_s')
    t, v = df_o['time_s'].values.astype(float), df_o['total_2q'].values.astype(float)
    interp = step_interpolate(t, v, TIME_POINTS)
    tool_stats['QSyMB'] = (v[0] - interp) / v[0]
    print(f"QSyMB: start={v[0]:.0f} end={v[-1]:.0f} final={(100*tool_stats['QSyMB'][-1]):.2f}%")

    for tool, name in CSV_TOOLS.items():
        r = load_tool_csv(args.paper_results, tool, args.gateset)
        if r is None:
            print(f"WARN: no over-time CSV for {tool}; skipping")
            continue
        tt, vv = r
        interp = step_interpolate(tt, vv, TIME_POINTS)
        tool_stats[name] = (vv[0] - interp) / vv[0]
        print(f"{name}: start={vv[0]:.0f} end={vv[-1]:.0f} final={(100*tool_stats[name][-1]):.2f}%")

    # ── Plot (unchanged paper styling) ────────────────────────────────────────
    styles = {
        "GUOQ":   dict(color="#1f77b4", linestyle="-",          marker="o", markersize=6),
        "QUESO":  dict(color="#ff7f0e", linestyle="--",         marker="s", markersize=6),
        "QSyMB":  dict(color="#2ca02c", linestyle="-.",         marker="^", markersize=7),
        "Quartz": dict(color="#d62728", linestyle=":",          marker="D", markersize=6),
        "Qiskit": dict(color="#9467bd", linestyle=(0,(5,2,1,2)),marker="v", markersize=6),
        "tket":   dict(color="#8c564b", linestyle=(0,(4,1,1,1)),marker="P", markersize=7),
    }
    order = ["QSyMB", "GUOQ", "QUESO", "Quartz", "Qiskit", "tket"]

    plt.rcParams.update({
        "font.size": 16,
        "font.weight": "bold",
        "axes.labelweight": "bold",
        "axes.titleweight": "bold",
        "xtick.labelsize": 15,
        "ytick.labelsize": 15,
        "axes.linewidth": 1.4,
    })
    fig, ax = plt.subplots(figsize=(5, 5))
    fig.subplots_adjust(left=0.18, right=0.97, top=0.93, bottom=0.14)
    x = TIME_POINTS / 60  # seconds → minutes for display

    lines = []
    for name in order:
        if name not in tool_stats:
            continue
        geo_mean = tool_stats[name]
        s = styles[name]
        markevery = max(1, len(x) // 8)
        ln, = ax.plot(x, geo_mean * 100, label=name, linewidth=4.5,
                      markevery=markevery, markersize=s.pop("markersize", 7) + 3,
                      **s)
        s["markersize"] = ln.get_markersize() - 3  # restore for next run
        lines.append(ln)

    import matplotlib.ticker as ticker
    if log_scale:
        ax.set_xscale("log")
        ax.set_xlim(left=0.1)
        ax.xaxis.set_major_formatter(ticker.LogFormatterMathtext())
        suffix = "log"
    else:
        ax.set_xlim(left=0, right=60)
        ax.xaxis.set_major_formatter(ticker.ScalarFormatter())
        suffix = "linear"
    for lbl in ax.get_xticklabels() + ax.get_yticklabels():
        lbl.set_fontweight("bold")

    ax.set_xlabel("Time (minutes)", fontsize=25)
    ax.set_ylabel("2Q Gate Reduction (%)", fontsize=25)
    ax.grid(True, alpha=0.2, which="major", linestyle="--")
    ax.set_ylim(bottom=0)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)

    outdir = Path(args.outdir)
    out_pdf = outdir / f"2q_reduction_vs_time_{args.gateset}_{suffix}.pdf"
    out_png = outdir / f"2q_reduction_vs_time_{args.gateset}_{suffix}.png"
    fig.savefig(out_pdf, dpi=200, bbox_inches="tight")
    fig.savefig(out_png, dpi=200, bbox_inches="tight")
    print(f"Saved: {out_pdf}")
    print(f"Saved: {out_png}")

    # ── Separate legend figure ─────────────────────────────────────────────────
    fig_leg = plt.figure(figsize=(4, 2.2))
    fig_leg.legend(handles=lines,
                   loc="center",
                   ncol=2,
                   fontsize=18,
                   framealpha=0.95,
                   edgecolor="#aaaaaa",
                   handlelength=3,
                   handleheight=1.2,
                   handletextpad=0.8,
                   columnspacing=1.2,
                   prop={"size": 18, "weight": "bold"})
    fig_leg.tight_layout()
    leg_pdf = outdir / "2q_reduction_legend.pdf"
    leg_png = outdir / "2q_reduction_legend.png"
    fig_leg.savefig(leg_pdf, dpi=200, bbox_inches="tight")
    fig_leg.savefig(leg_png, dpi=200, bbox_inches="tight")
    print(f"Saved: {leg_pdf}")
    print(f"Saved: {leg_png}")


if __name__ == "__main__":
    main()
