#!/usr/bin/env python3
"""Export a QSymb suite-aggregated 2q-over-time CSV (time_s,total_2q) from an
optimize.sh output directory, reconstructed from the strict-improvement lines
    Progress 2q: <n> (total <m>) at <t>s
in each benchmark's log. The CSV is what plot_2q_reduction.py consumes.

Usage:
    python3 export_2q_over_time.py --gateset ibmnew --ours-dir results_ibmnew_trial1
Writes <paper-results>/qsymb_<gateset>_2q_over_time[_<suffix>].csv.
"""
import argparse
import os
import re

import numpy as np
import pandas as pd

PROGRESS_RE = re.compile(r'Progress 2q: (\d+) \(total \d+\) at ([0-9.]+)s')


def count_2q_in_qasm(path):
    n = 0
    pat = re.compile(r'^\s*(cx|cz|rxx|ms)\s', re.IGNORECASE)
    with open(path) as f:
        for line in f:
            if pat.match(line):
                n += 1
    return n


def load_ours(ours_dir, bench_dir, benchmarks):
    """Suite-aggregated (times, totals) step function from the per-benchmark
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
            o = count_2q_in_qasm(qasm) if os.path.exists(qasm) else 0
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
    times, totals, cur = [0.0], [float(total0)], float(total0)
    for t, d in events:
        cur += d
        times.append(t)
        totals.append(cur)
    return np.array(times), np.array(totals)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--gateset', required=True, choices=['nam', 'ibmnew'])
    ap.add_argument('--ours-dir', required=True,
                    help='optimize.sh output dir (summary.csv + per-bench logs)')
    ap.add_argument('--paper-results', default='/root/paper_results')
    ap.add_argument('--bench-dir', default=None)
    ap.add_argument('--benchmarks', default='/root/benchmark.txt')
    ap.add_argument('--suffix', default='fresh',
                    help="suffix for the output filename (default 'fresh'; "
                         "pass '' to overwrite the precomputed CSV)")
    args = ap.parse_args()

    bench_dir = args.bench_dir or os.path.join(
        '/root/guoq_benchmarks', 'nam_rz' if args.gateset == 'nam' else args.gateset)
    with open(args.benchmarks) as f:
        benchmarks = [l.strip() for l in f if l.strip()]

    t, v = load_ours(args.ours_dir, bench_dir, benchmarks)
    sfx = f'_{args.suffix.lstrip("_")}' if args.suffix else ''
    out = os.path.join(args.paper_results, f'qsymb_{args.gateset}_2q_over_time{sfx}.csv')
    pd.DataFrame({'time_s': t, 'total_2q': v.astype(int)}).to_csv(out, index=False)
    print(f"wrote {out} ({len(t)} rows, start={v[0]:.0f} end={v[-1]:.0f})")


if __name__ == '__main__':
    main()
