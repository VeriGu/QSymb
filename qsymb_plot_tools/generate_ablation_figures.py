#!/usr/bin/env python3
"""Ablation figures for §7.3 (paper Fig 13 & 14).

Fig 13 (symbolic-rule ablation): 2 S-curve panels comparing FULL Qsymb
(concrete + anchored symbolic) against
  - concrete-only
  - concrete + canonical (un-anchored) symbolic
with ours-better / ours-worse / equal progress bars.

Fig 14 (concrete-rule quality): 1 S-curve panel comparing the Alg-1 concrete
rules against an equal-count random subset of Queso's rules.

Each --<variant> argument is an optimize.sh/ablation.sh summary.csv
(benchmark, exit, original_size, original_2q, final_size, final_2q, ...).
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


def _reduction(path, benchmarks):
    df = pd.read_csv(path)
    col = 'final_2q' if 'final_2q' in df.columns else 'optimized_2q'
    df['benchmark'] = df['benchmark'].astype(str).str.replace(r'\.qasm$', '', regex=True)
    for c in ('original_2q', col):
        df[c] = pd.to_numeric(df[c], errors='coerce')
    df = df.dropna(subset=['original_2q', col])
    df = df[df['original_2q'] > 0]
    if benchmarks is not None:
        df = df[df['benchmark'].isin(benchmarks)]
    df = df.copy()
    df['reduction'] = (df['original_2q'] - df[col]) / df['original_2q']
    return df.set_index('benchmark')['reduction']


def _panel(ax, fig, ref, other, ref_label, other_label, show_ylabel):
    """One S-curve panel: ref (blue) vs other (orange), sorted by ref, with
    ref-better / other-better / equal progress bars underneath."""
    joined = pd.concat([ref.rename('ref'), other.rename('other')], axis=1).dropna()
    joined = joined.sort_values('ref').reset_index(drop=True)
    x = np.arange(len(joined))
    ax.scatter(x, joined['ref'], s=15, label=ref_label)
    ax.scatter(x, joined['other'], s=15, color='orange', label=other_label)
    ax.grid(True, linestyle='--', alpha=0.6)
    ax.set_xticks([])
    ax.tick_params(axis='y', labelsize=16)
    ax.legend(prop=FontProperties(weight='bold', size=18))
    ax.set_ylim(0, 1)
    if show_ylabel:
        ax.set_yticks(np.arange(0.2, 1.0, 0.2))
        ax.set_ylabel('2q Reduction', fontsize=22, fontweight='bold')
        for lbl in ax.get_yticklabels():
            lbl.set_fontweight('bold'); lbl.set_fontsize(19)
    else:
        ax.set_yticklabels([])

    total = len(joined)
    ref_better = int((joined['ref'] > joined['other']).sum())
    other_better = int((joined['other'] > joined['ref']).sum())
    equal = total - ref_better - other_better

    ap = ax.get_position()
    bh = 0.06; by = ap.y0 - 0.1; bw = ap.width * 0.88; bx = ap.x0 + ap.width * 0.1
    for row, (count, color) in enumerate([(ref_better, 'skyblue'),
                                          (other_better, 'orange'),
                                          (equal, 'green')]):
        axb = fig.add_axes([bx, by - row * bh, bw, bh])
        axb.set_frame_on(False)
        axb.add_patch(patches.Rectangle((0, 0), 1, 1, facecolor='lightgray', edgecolor='none'))
        axb.add_patch(patches.Rectangle((0, 0), count / total if total else 0, 1,
                                        facecolor=color, edgecolor='none'))
        axb.set_xticks([]); axb.set_yticks([])
        axb.text(0.5, 0.5, f'{count}/{total}', ha='center', va='center',
                 fontsize=18, color='black', fontweight='bold')
    if show_ylabel:
        lx = bx - 0.01
        fig.text(lx, by + bh / 2, f'{ref_label} Better', ha='right', va='center', fontsize=16, fontweight='bold')
        fig.text(lx, by - bh / 2, f'{ref_label} Worse', ha='right', va='center', fontsize=16, fontweight='bold')
        fig.text(lx, by - 1.5 * bh, 'Equal', ha='right', va='center', fontsize=16, fontweight='bold')
    return ref_better, other_better, equal, total


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--gateset', required=True)
    ap.add_argument('--full', required=True, help='full Qsymb summary.csv')
    ap.add_argument('--concrete', required=True,
                    help='concrete-only (no lr) summary.csv; used for both Fig 13 and Fig 14')
    ap.add_argument('--canonical', required=True, help='concrete+canonical-symbolic (+lr) summary.csv')
    ap.add_argument('--random', required=True, help='random-Queso (no lr) summary.csv')
    ap.add_argument('--benchmarks', default='/root/benchmark.txt')
    ap.add_argument('--outdir', default='/root/paper_results/figures',
                    help='output directory (default: paper_results/figures)')
    ap.add_argument('--suffix', default='',
                    help='suffix appended to output filenames (e.g. fresh)')
    args = ap.parse_args()
    sfx = f'_{args.suffix.lstrip("_")}' if args.suffix else ''

    wanted = None
    if args.benchmarks:
        with open(args.benchmarks) as f:
            wanted = {ln.strip() for ln in f if ln.strip()}

    os.makedirs(args.outdir, exist_ok=True)

    # ---------- Fig 13 ----------
    full = _reduction(args.full, wanted)
    concrete = _reduction(args.concrete, wanted)
    canonical = _reduction(args.canonical, wanted)
    fig, axes = plt.subplots(1, 2, figsize=(12, 5))
    plt.subplots_adjust(left=0.08, right=0.98, top=1, bottom=0.25, wspace=0.12)
    s1 = _panel(axes[0], fig, full, concrete, 'Full', 'Concrete', True)
    s2 = _panel(axes[1], fig, full, canonical, 'Full', 'Canonical', False)
    out13 = os.path.join(args.outdir, f'fig13_ablation_{args.gateset}{sfx}')
    plt.savefig(out13 + '.png', dpi=400, bbox_inches='tight', pad_inches=0.05)
    plt.savefig(out13 + '.pdf', bbox_inches='tight', pad_inches=0.05)
    plt.close()
    print(f"Fig13 -> {out13}.png  | Full vs Concrete {s1[0]}/{s1[3]} better; "
          f"Full vs Canonical {s2[0]}/{s2[3]} better")

    # ---------- Fig 14 ---------- (same concrete variant as Fig 13)
    if os.path.exists(args.random):
        rand = _reduction(args.random, wanted)
        fig, ax = plt.subplots(1, 1, figsize=(6, 5))
        plt.subplots_adjust(left=0.22, right=0.96, top=1, bottom=0.25)
        s = _panel(ax, fig, concrete, rand, 'Concrete', 'Random', True)
        out14 = os.path.join(args.outdir, f'fig14_concrete_vs_random_{args.gateset}{sfx}')
        plt.savefig(out14 + '.png', dpi=400, bbox_inches='tight', pad_inches=0.05)
        plt.savefig(out14 + '.pdf', bbox_inches='tight', pad_inches=0.05)
        plt.close()
        print(f"Fig14 -> {out14}.png  | Concrete better {s[0]}/{s[3]}, Random better {s[1]}/{s[3]}")
    else:
        print(f"skip Fig14: {args.random} not found")


if __name__ == '__main__':
    main()
