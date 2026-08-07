#!/usr/bin/env python3
"""Quality-over-time figure: suite-wide 2q REDUCTION RATE (%) vs runtime.

One marker-line per tool (paper style). Baselines come from
paper_results/<tool>_<gateset>_2q_over_time.csv with schema
    time_s,total_2q
(total_2q = suite-wide 2q count at per-circuit elapsed time). Our curve is
reconstructed from the strict-improvement lines
    Progress 2q: <n> (total <m>) at <t>s
in an optimize.sh output directory. Every curve is sampled at regular time
points and converted to reduction rate:
    reduction(t) = 100 * (total_2q(0) - total_2q(t)) / total_2q(0)

Usage:
    python3 plot_quality_time.py --gateset ibmnew \
        --ours-dir /root/results_ibmnew_trial1 --outdir <dir>

Outputs: <outdir>/quality_vs_time_<gateset>.png / .pdf
"""

import argparse
import os
import re

import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

BASELINES = ['qiskit', 'guoq', 'quartz', 'tket', 'queso']
LABELS = {'qiskit': 'Qiskit', 'guoq': 'Guoq-Rewrite', 'quartz': 'Quartz',
          'tket': 'TKET', 'queso': 'Queso'}

PROGRESS_RE = re.compile(r'Progress 2q: (\d+) \(total \d+\) at ([0-9.]+)s')


def _count_2q_in_qasm(path):
    n = 0
    with open(path) as f:
        for line in f:
            if line.strip().startswith(('cx', 'cz', 'rxx', 'ms')):
                n += 1
    return n


def ours_step(ours_dir, bench_dir, benchmarks):
    """(times, totals) suite-wide step function from the per-benchmark
    Progress lines; starts at the suite's total original 2q."""
    summary = pd.read_csv(os.path.join(ours_dir, 'summary.csv'))
    summary['benchmark'] = summary['benchmark'].astype(str)
    orig = dict(zip(summary['benchmark'],
                    pd.to_numeric(summary['original_2q'], errors='coerce')))
    events, total0 = [], 0
    for b in benchmarks:
        o = orig.get(b)
        if o is None or pd.isna(o):
            qasm = os.path.join(bench_dir, b + '.qasm')
            o = _count_2q_in_qasm(qasm) if os.path.exists(qasm) else 0
            print(f"note: original_2q for {b} from qasm -> {o}")
        total0 += o
        log = os.path.join(ours_dir, b, b + '.log')
        if not os.path.exists(log):
            continue
        prev = o
        with open(log) as f:
            for line in f:
                m = PROGRESS_RE.search(line)
                if m:
                    val, t = int(m.group(1)), float(m.group(2))
                    if val < prev:
                        events.append((t, val - prev))
                        prev = val
    events.sort()
    times, totals, cur = [0.0], [total0], total0
    for t, d in events:
        cur += d
        times.append(t)
        totals.append(cur)
    return np.array(times), np.array(totals, dtype=float)


def sample_reduction(times, totals, sample_ts):
    """Reduction rate (%) at each sample time for a step function."""
    base = totals[0]
    idx = np.searchsorted(times, sample_ts, side='right') - 1
    idx = np.clip(idx, 0, len(totals) - 1)
    return 100.0 * (base - totals[idx]) / base


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--gateset', required=True, choices=['nam', 'ibmnew'])
    ap.add_argument('--ours-dir', required=True,
                    help='optimize.sh output dir (summary.csv + per-bench logs)')
    ap.add_argument('--paper-results', default='/root/paper_results')
    ap.add_argument('--bench-dir', default=None)
    ap.add_argument('--benchmarks', default='/root/benchmark.txt')
    ap.add_argument('--outdir', default='/root/paper_results/figures',
                    help='output directory (default: paper_results/figures)')
    ap.add_argument('--horizon', type=float, default=3600.0,
                    help='time-axis end in seconds (default 3600 = 60 min)')
    ap.add_argument('--samples', type=int, default=10,
                    help='number of equally-spaced sample points (default 10)')
    args = ap.parse_args()

    bench_dir = args.bench_dir or os.path.join(
        '/root/qsymb_benchmarks', 'nam_rz' if args.gateset == 'nam' else args.gateset)
    with open(args.benchmarks) as f:
        benchmarks = [l.strip() for l in f if l.strip()]
    os.makedirs(args.outdir, exist_ok=True)

    sample_ts = np.linspace(args.horizon / args.samples, args.horizon, args.samples)
    sample_min = sample_ts / 60.0

    plt.figure(figsize=(10, 10))

    t, v = ours_step(args.ours_dir, bench_dir, benchmarks)
    red = sample_reduction(t, v, sample_ts)
    plt.plot(sample_min, red, marker='o', linestyle='-', linewidth=2.5,
             label='QSymb', zorder=5)
    print(f"QSymb: {[f'{r:.2f}' for r in red]}")

    for tool in BASELINES:
        path = os.path.join(args.paper_results,
                            f'{tool}_{args.gateset}_2q_over_time.csv')
        if not os.path.exists(path):
            print(f"WARN: {path} missing; skipping {tool}")
            continue
        df = pd.read_csv(path).sort_values('time_s')
        red = sample_reduction(df['time_s'].values,
                               df['total_2q'].values.astype(float), sample_ts)
        plt.plot(sample_min, red, marker='o', linestyle='-',
                 label=LABELS[tool])
        print(f"{LABELS[tool]}: {[f'{r:.2f}' for r in red]}")

    plt.xlabel('Time (minutes)', fontsize=26, fontweight='bold')
    plt.ylabel('2q reduction rate (%)', fontsize=26, fontweight='bold')
    plt.legend(fontsize=24)
    plt.grid(True, linestyle='--', alpha=0.7)
    plt.xticks(fontsize=22)
    plt.yticks(fontsize=22)
    plt.tight_layout()

    out = os.path.join(args.outdir, f'quality_vs_time_{args.gateset}')
    plt.savefig(out + '.png', dpi=600)
    plt.savefig(out + '.pdf')
    plt.close()
    print(f"Saved {out}.png and {out}.pdf")


if __name__ == '__main__':
    main()
