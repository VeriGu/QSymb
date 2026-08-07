#!/usr/bin/env python3
"""2q-reduction S-curve comparison figure (Qsymb vs baseline tools).

Merges our optimizer output (optimize.sh summary.csv) with the baseline tool
results in paper_results/ for one gateset, then renders one S-curve panel per
baseline with ours-better / ours-worse / equal progress bars underneath.

Usage (typically invoked by optimize.sh after a nam/ibmnew batch):
    python3 generate_s_curves_with_progress_bars.py \
        --gateset ibmnew --ours <out_dir>/summary.csv --outdir <out_dir>

Inputs per gateset <gs>:
    --ours summary.csv                      benchmark,...,original_2q,...,final_2q,...
    paper_results/guoq_<gs>_results.csv     guoq schema: benchmark(.qasm), total_2q
    paper_results/{qiskit,quartz,tket,queso}_<gs>_results.csv
                                            uniform: benchmark, original_2q, optimized_2q

Outputs into --outdir:
    all_comparison_data_<gs>.csv
    s_curve_<gs>.png / .pdf
"""

import argparse
import os

import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.font_manager import FontProperties

BASELINES = ['qiskit', 'guoq', 'quartz', 'tket', 'queso']
LABELS = {
    'qiskit_2q': 'Qiskit',
    'guoq_2q': 'Guoq-Rewrite',
    'quartz_2q': 'Quartz',
    'tket_2q': 'TKET',
    'queso_2q': 'Queso',
}


def _strip_qasm(series):
    return series.astype(str).str.replace(r'\.qasm$', '', regex=True)


def preprocess(gateset, ours_csv, paper_results_dir, output_csv, tool_csvs=None):
    """Merge ours + the baselines into one comparison CSV for `gateset`.

    Accepts either the optimize.sh summary schema (final_2q) or the
    nam_results_135.csv schema (optimized_2q) for our results.

    tool_csvs: optional {tool: csv_path} overrides. A tool with an explicit
    path uses that CSV (any file, any location); tools without one fall back
    to the conventional <paper_results_dir>/<tool>_<gateset>_results.csv.
    """
    ours = pd.read_csv(ours_csv)
    ours_col = 'final_2q' if 'final_2q' in ours.columns else 'optimized_2q'
    keep = ['benchmark', 'original_2q', ours_col]
    # Our fidelity (optimize.sh writes final_fidelity for ibmnew).
    if 'final_fidelity' in ours.columns:
        keep.append('final_fidelity')
    ours = ours[keep]
    ours = ours.rename(columns={ours_col: 'ours_2q', 'final_fidelity': 'ours_fid'})
    ours['benchmark'] = _strip_qasm(ours['benchmark'])

    merged = ours
    for tool in BASELINES:
        if tool_csvs and tool_csvs.get(tool):
            path = tool_csvs[tool]
            print(f"baseline {tool}: {path} (explicit)")
        else:
            path = os.path.join(paper_results_dir, f'{tool}_{gateset}_results.csv')
        if not os.path.exists(path):
            print(f"WARN: {path} missing; skipping {tool}")
            continue
        df = pd.read_csv(path)
        has_fid = 'fidelity' in df.columns
        if 'total_2q' in df.columns:          # guoq schema
            cols = ['benchmark', 'total_2q'] + (['fidelity'] if has_fid else [])
            df = df[cols].rename(columns={'total_2q': f'{tool}_2q', 'fidelity': f'{tool}_fid'})
        else:                                  # uniform schema
            cols = ['benchmark', 'optimized_2q'] + (['fidelity'] if has_fid else [])
            df = df[cols].rename(columns={'optimized_2q': f'{tool}_2q', 'fidelity': f'{tool}_fid'})
        df['benchmark'] = _strip_qasm(df['benchmark'])
        # Multiple rows per circuit can exist; keep the best (minimum) 2q and,
        # when present, the best (maximum) fidelity.
        agg = {f'{tool}_2q': 'min'}
        if has_fid:
            agg[f'{tool}_fid'] = 'max'
        df = df.groupby('benchmark', as_index=False).agg(agg)
        merged = merged.merge(df, on='benchmark', how='left')

    merged.to_csv(output_csv, index=False)
    print(f"Wrote {output_csv} with {len(merged)} rows.")
    return merged


def generate_s_curves_with_progress_bars(df, out_png, out_pdf):
    """Render the per-baseline S-curve panels with progress bars."""
    methods = ['ours_2q'] + [f'{t}_2q' for t in BASELINES if f'{t}_2q' in df.columns]
    for col in ['original_2q'] + methods:
        df[col] = pd.to_numeric(df[col], errors='coerce')
    df = df.dropna(subset=['original_2q'])
    df = df[df['original_2q'] > 0]

    for method in methods:
        df[f'{method}_reduction'] = (df['original_2q'] - df[method]) / df['original_2q']

    df_sorted = df.sort_values(by='ours_2q_reduction').reset_index(drop=True)

    plot_methods = [m for m in methods if m != 'ours_2q']
    n = len(plot_methods)
    fig, axes = plt.subplots(1, n, figsize=(5 * n, 5))
    if n == 1:
        axes = [axes]
    plt.subplots_adjust(left=0.05, right=0.98, top=1, bottom=0.25, wspace=0.1)
    font_props = FontProperties(weight='bold', size=20)

    for i, method in enumerate(plot_methods):
        ax = axes[i]
        ours_col = 'ours_2q_reduction'
        theirs_col = f'{method}_reduction'
        # Paper counting: the denominator is every circuit WE have a result
        # for (the full suite); a baseline with no result for a circuit is a
        # tool failure and counts as zero reduction, not a dropped row.
        comparison_data = df_sorted.dropna(subset=[ours_col]).copy()
        comparison_data[theirs_col] = comparison_data[theirs_col].fillna(0.0)

        ax.scatter(np.arange(len(comparison_data)), comparison_data[ours_col],
                   s=15, label='Ours')
        ax.scatter(np.arange(len(comparison_data)), comparison_data[theirs_col],
                   s=15, color='orange', label=LABELS.get(method, method))

        ax.grid(True, linestyle='--', alpha=0.6)
        ax.set_xticks([])
        ax.tick_params(axis='y', labelsize=16)
        ax.legend(prop=font_props)
        ax.set_aspect('auto')
        ax.set_ylim(0, 1)

        if i == 0:
            ax.set_yticks(np.arange(0.2, 1.0, 0.2))
            ax.set_ylabel('2q Reduction', fontsize=22, fontweight='bold')
            for label in ax.get_yticklabels():
                label.set_fontweight('bold')
                label.set_fontsize(19)
        else:
            ax.set_yticklabels([])

        ours_better = (comparison_data[ours_col] > comparison_data[theirs_col]).sum()
        theirs_better = (comparison_data[theirs_col] > comparison_data[ours_col]).sum()
        equal = len(comparison_data) - ours_better - theirs_better
        total = len(comparison_data)

        ax_pos = ax.get_position()
        bar_height = 0.06
        bar_y_start = ax_pos.y0 - 0.1
        bar_width = ax_pos.width * 0.88
        bar_x_start = ax_pos.x0 + (ax_pos.width * 0.1)

        for row, (count, color) in enumerate([
                (ours_better, 'skyblue'), (theirs_better, 'orange'), (equal, 'green')]):
            axb = fig.add_axes([bar_x_start, bar_y_start - row * bar_height, bar_width, bar_height])
            axb.set_frame_on(False)
            axb.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray', edgecolor='none'))
            axb.add_patch(patches.Rectangle((0, 0), count / total, 1, facecolor=color, edgecolor='none'))
            axb.set_xticks([])
            axb.set_yticks([])
            axb.text(0.5, 0.5, f'{count}/{total}', ha='center', va='center',
                     fontsize=18, color='black', fontweight='bold')

        if i == 0:
            legend_x = bar_x_start - 0.01
            fig.text(legend_x, bar_y_start + bar_height / 2, 'Ours Better',
                     ha='right', va='center', fontsize=18, fontweight='bold')
            fig.text(legend_x, bar_y_start - bar_height / 2, 'Ours Worse',
                     ha='right', va='center', fontsize=18, fontweight='bold')
            fig.text(legend_x, bar_y_start - 1.5 * bar_height, 'Equal',
                     ha='right', va='center', fontsize=18, fontweight='bold')

    plt.savefig(out_png, dpi=400, bbox_inches='tight', pad_inches=0)
    plt.savefig(out_pdf, bbox_inches='tight', pad_inches=0)
    plt.close()
    print(f"Saved {out_png} and {out_pdf}")


def generate_fidelity_s_curves(df, out_png, out_pdf):
    """Fidelity S-curve panels (paper Fig. 11 right side): raw circuit
    fidelity (product of per-gate fidelities, 0..1) per baseline, sorted by
    our fidelity, with ours-better / ours-worse / equal progress bars."""
    fid_tools = [t for t in BASELINES
                 if f'{t}_fid' in df.columns and df[f'{t}_fid'].notna().any()]
    if 'ours_fid' not in df.columns or not df['ours_fid'].notna().any() or not fid_tools:
        print("skip fidelity S-curve: no fidelity data (ours_fid / <tool>_fid)")
        return
    cols = ['ours_fid'] + [f'{t}_fid' for t in fid_tools]
    for col in cols:
        df[col] = pd.to_numeric(df[col], errors='coerce')

    df_sorted = df.sort_values(by='ours_fid').reset_index(drop=True)

    n = len(fid_tools)
    fig, axes = plt.subplots(1, n, figsize=(5 * n, 5))
    if n == 1:
        axes = [axes]
    plt.subplots_adjust(left=0.05, right=0.98, top=1, bottom=0.25, wspace=0.1)
    font_props = FontProperties(weight='bold', size=20)

    for i, tool in enumerate(fid_tools):
        ax = axes[i]
        theirs_col = f'{tool}_fid'
        # Same counting policy as the 2q panels: tool failure -> fidelity 0.
        comparison_data = df_sorted.dropna(subset=['ours_fid']).copy()
        comparison_data[theirs_col] = comparison_data[theirs_col].fillna(0.0)

        ax.scatter(np.arange(len(comparison_data)), comparison_data['ours_fid'],
                   s=15, label='Ours')
        ax.scatter(np.arange(len(comparison_data)), comparison_data[theirs_col],
                   s=15, color='orange', label=LABELS.get(f'{tool}_2q', tool))

        ax.grid(True, linestyle='--', alpha=0.6)
        ax.set_xticks([])
        ax.tick_params(axis='y', labelsize=16)
        ax.legend(prop=font_props)
        ax.set_aspect('auto')
        ax.set_ylim(0, 1)

        if i == 0:
            ax.set_yticks(np.arange(0.2, 1.0, 0.2))
            ax.set_ylabel('Fidelity', fontsize=22, fontweight='bold')
            for label in ax.get_yticklabels():
                label.set_fontweight('bold')
                label.set_fontsize(19)
        else:
            ax.set_yticklabels([])

        # Compare at a rounding tolerance so float noise doesn't split ties.
        ours_v = comparison_data['ours_fid'].round(8)
        theirs_v = comparison_data[theirs_col].round(8)
        ours_better = (ours_v > theirs_v).sum()
        theirs_better = (theirs_v > ours_v).sum()
        equal = len(comparison_data) - ours_better - theirs_better
        total = len(comparison_data)

        ax_pos = ax.get_position()
        bar_height = 0.06
        bar_y_start = ax_pos.y0 - 0.1
        bar_width = ax_pos.width * 0.88
        bar_x_start = ax_pos.x0 + (ax_pos.width * 0.1)

        for row, (count, color) in enumerate([
                (ours_better, 'skyblue'), (theirs_better, 'orange'), (equal, 'green')]):
            axb = fig.add_axes([bar_x_start, bar_y_start - row * bar_height, bar_width, bar_height])
            axb.set_frame_on(False)
            axb.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray', edgecolor='none'))
            axb.add_patch(patches.Rectangle((0, 0), count / total if total else 0, 1,
                                            facecolor=color, edgecolor='none'))
            axb.set_xticks([])
            axb.set_yticks([])
            axb.text(0.5, 0.5, f'{count}/{total}', ha='center', va='center',
                     fontsize=18, color='black', fontweight='bold')

        if i == 0:
            legend_x = bar_x_start - 0.01
            fig.text(legend_x, bar_y_start + bar_height / 2, 'Ours Better',
                     ha='right', va='center', fontsize=18, fontweight='bold')
            fig.text(legend_x, bar_y_start - bar_height / 2, 'Ours Worse',
                     ha='right', va='center', fontsize=18, fontweight='bold')
            fig.text(legend_x, bar_y_start - 1.5 * bar_height, 'Equal',
                     ha='right', va='center', fontsize=18, fontweight='bold')

    plt.savefig(out_png, dpi=400, bbox_inches='tight', pad_inches=0)
    plt.savefig(out_pdf, bbox_inches='tight', pad_inches=0)
    plt.close()
    print(f"Saved {out_png} and {out_pdf}")


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--gateset', required=True, choices=['nam', 'ibmnew'],
                    help='which gateset baselines to compare against')
    ap.add_argument('--ours', required=True,
                    help='optimize.sh summary.csv with our results')
    ap.add_argument('--paper-results', default='/root/paper_results',
                    help='directory holding <tool>_<gateset>_results.csv baselines')
    ap.add_argument('--outdir', default='/root/paper_results/figures',
                    help='output directory for the merged CSV and figures '
                         '(default: paper_results/figures)')
    ap.add_argument('--benchmarks', default='/root/benchmark.txt',
                    help='benchmark-name list restricting which circuits are plotted '
                         '(the paper suite); pass an empty string to disable filtering')
    for tool in BASELINES:
        ap.add_argument(f'--{tool}', metavar='CSV', default=None,
                        help=f'explicit CSV for the {tool} baseline (any path); '
                             f'overrides <paper-results>/{tool}_<gateset>_results.csv')
    ap.add_argument('--suffix', default='',
                    help='suffix appended to every output filename (e.g. '
                         '--suffix fresh -> s_curve_<gs>_fresh.png), so figures '
                         'built from fresh baseline CSVs are distinguishable')
    args = ap.parse_args()
    tool_csvs = {tool: getattr(args, tool) for tool in BASELINES}
    sfx = f'_{args.suffix.lstrip("_")}' if args.suffix else ''

    os.makedirs(args.outdir, exist_ok=True)
    merged_csv = os.path.join(args.outdir, f'all_comparison_data_{args.gateset}{sfx}.csv')
    df = preprocess(args.gateset, args.ours, args.paper_results, merged_csv, tool_csvs=tool_csvs)
    if args.benchmarks:
        with open(args.benchmarks) as f:
            wanted = {line.strip() for line in f if line.strip()}
        before = len(df)
        df = df[df['benchmark'].isin(wanted)].copy()
        print(f"Filtered to {args.benchmarks}: {before} -> {len(df)} circuits")
        df.to_csv(merged_csv, index=False)
    # Paper figure numbers: ibmnew -> Fig 11, nam -> Fig 12 (filenames carry
    # the number so paper_results/figures is self-describing).
    fig_no = {'ibmnew': 'fig11', 'nam': 'fig12'}[args.gateset]
    generate_s_curves_with_progress_bars(
        df,
        os.path.join(args.outdir, f'{fig_no}_s_curve_{args.gateset}{sfx}.png'),
        os.path.join(args.outdir, f'{fig_no}_s_curve_{args.gateset}{sfx}.pdf'))
    # Fidelity S-curve (Fig. 11 companion): rendered only when both our
    # summary carries final_fidelity (ibmnew) and at least one baseline CSV
    # carries a fidelity column (e.g. the _fresh re-run CSVs).
    generate_fidelity_s_curves(
        df,
        os.path.join(args.outdir, f'{fig_no}_s_curve_fidelity_{args.gateset}{sfx}.png'),
        os.path.join(args.outdir, f'{fig_no}_s_curve_fidelity_{args.gateset}{sfx}.pdf'))


if __name__ == '__main__':
    main()
