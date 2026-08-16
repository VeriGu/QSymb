# Artifact: QSymb

Artifact for **QSymb**: rewrite-rule synthesis and synthesis cost (§7.1,
Tables 1–3), optimization performance vs. state-of-the-art tools (§7.2,
Figs. 11–12), and ablations (§7.3, Figs. 13–14).

All commands are run from `/root` inside the container. Precomputed results
ship in `paper_results/`; every fresh run writes its results alongside them
with a `_fresh` suffix (configurable via `--suffix`). All figures land in
`paper_results/figures/`, named by paper figure number.

---
## 0. Load the image and start a container

**Pull the image from Docker Hub (~10 GB compressed):**
```bash
docker pull xiaoqianghweye/qsymb:oopsla26
docker tag  xiaoqianghweye/qsymb:oopsla26 oopsla26:qsymb-slim
```
The image contains all dependencies (Java 17, Python 3.10, Z3 4.12,
PuLP + CBC, full QSymb codebase).

**Start an interactive container:**
```bash
docker run -it --name qsymb -w /root oopsla26:qsymb-slim /bin/bash
```
You are now inside the container at `/root`. Detach without stopping:
`Ctrl-P` then `Ctrl-Q`. Reattach: `docker exec -it qsymb bash`.
Stop/remove: `docker stop qsymb && docker rm qsymb`.

## 1. Build (~1 min)

```bash
bash build_qsymb.sh
```
Builds `SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## 2. Kick-the-tires (~15 min)

```bash
bash build_qsymb.sh
bash kick_start.sh ibmnew
printf "barenco_tof_3\n4gt13_90\nqaoa_10\n" > /tmp/smoke.txt
bash optimize.sh -b /tmp/smoke.txt -t 120 -g ibmnew -n 3 -o /tmp/smoke_out
cat /tmp/smoke_out/summary.csv
```
Expected: `kick_start.sh` prints a non-empty rule-count table; the smoke
`summary.csv` has 3 rows with `exit=0` and `final_2q ≤ original_2q`.

---

## 3. Experiment 1 — rule synthesis & synthesis cost (§7.1, Tables 1–3)

```bash
bash kick_start.sh                 # all four gate sets
```
Runs concrete, symbolic, and anchoring rule synthesis per gate set
(concrete size 5 for ibmnew/nam/rigetti, 3 for ion; symbolic size 3/3/3/2)
and prints two tables in the paper's format:

- `=== Table 1. Concrete rule-set size ===` → compare to paper **Table 1**.
- `=== Table 2. Anchoring cost of symbolic rules ===` → compare to paper
  **Table 2** (anchoring takes seconds).

The rule count may not be exact as the rule choosing order can impact the rule count.
Inspect that the rule set is still much smaller compared to prior rule set of other tools.

Logs: `kick_start_logs/<gs>_{concrete,symb,anchor}.log`. Rule counts vary
slightly between runs (non-deterministic ordering) but stay small.

### 3.1 Property-grouping speedup (Table 3)

```bash
bash measure_filter_speedup.sh              # all four gate sets
bash measure_filter_speedup.sh ibmnew       # or one gate set
```
Runs symbolic synthesis twice per gate set (grouping ON, then `-nofilter`
direct solve; sizes fixed to the paper setting) and prints
`=== Table 3 ... ===` in the paper's format → compare to paper **Table 3**
(speedups ~6–28×; rule sets identical in both modes). Logs:
`filter_speedup_<gs>/`.

---

## 4. Experiment 2 — optimization performance (§7.2, Figs. 11–12)

### 4.1 Generate figures from precomputed data (seconds)

```bash
python3 qsymb_plot_tools/generate_s_curves_with_progress_bars.py \
    --gateset ibmnew --ours paper_results/qsymb_ibmnew_results.csv
python3 qsymb_plot_tools/generate_s_curves_with_progress_bars.py \
    --gateset nam    --ours paper_results/qsymb_nam_results.csv
```
Renders **Fig. 11** (`fig11_s_curve_ibmnew.png`, plus
`fig11_s_curve_fidelity_ibmnew.png` when fidelity data is present) and
**Fig. 12** (`fig12_s_curve_nam.png`) into `paper_results/figures/` from the
precomputed QSymb summaries and baseline CSVs.

### 4.2 Run QSymb (fresh~135 CPU hours)

```bash
bash optimize.sh -b benchmark.txt -t 3600 -g ibmnew -o results_ibmnew_trial1
bash optimize.sh -b benchmark.txt -t 3600 -g nam    -o results_nam_trial1
```
Runs the full pipeline,
60-min timeout per circuit on the 135-circuit suite. Wall time ≈ `135/n`
hours. Parallelism `-n` is auto-set to `min(cores/2, TotalMemory/8GB)`
(~8 GB per task); pass `-n <threads>` only to override.

Flags: `-b <benchmarks> -t <timeout_s> -g <gateset> [-n <threads>]
[-o <out_dir>] [-p <baseline_csv_dir>] [--suffix <sfx>] [-minsymb N]
[-maxsymb N] [-approx EPS]`. `-n` defaults to
`min(cores/2, TotalMemory/8GB)`.

Outputs:
| path | description |
|---|---|
| `<out_dir>/<bench>/{<bench>.log, <bench>.log.exit, <bench>_optimized.qasm}` | per-circuit log, exit code, optimized circuit |
| `<out_dir>/summary.csv` | `benchmark,exit,original_size,original_2q,final_size,final_2q,symb_applied,final_fidelity` (fidelity: ibmnew only) |
| `paper_results/qsymb_<gs>_results_fresh.csv` | copy of the fresh summary |
| `paper_results/figures/fig11_*_fresh.png` / `fig12_*_fresh.png` | fresh figures (generated automatically at the end of the run) |

### 4.3 Reproduce figures from fresh data

Generated automatically by 4.2; to re-render manually:

```bash
python3 qsymb_plot_tools/generate_s_curves_with_progress_bars.py \
    --gateset ibmnew --ours paper_results/qsymb_ibmnew_results_fresh.csv --suffix fresh
```
`--suffix fresh` appends `_fresh` to the output filenames so fresh figures sit
alongside the precomputed ones.

**Compare to the paper**: `fig11_*` → **Fig. 11** (IBM-Eagle 2q reduction +
fidelity), `fig12_*` → **Fig. 12** (Nam 2q reduction). QSymb's curve should
sit at or above the baselines; per-circuit numbers vary (non-deterministic SA).

### 4.4 Runtime figure — 2Q reduction rate vs time

From precomputed data:
```bash
python3 qsymb_plot_tools/plot_2q_reduction.py --gateset ibmnew
```
From a fresh run (export the over-time CSV first, then plot it):
```bash
python3 qsymb_plot_tools/export_2q_over_time.py --gateset ibmnew --ours-dir results_ibmnew_trial1
python3 qsymb_plot_tools/plot_2q_reduction.py --gateset ibmnew \
    --ours paper_results/qsymb_ibmnew_2q_over_time_fresh.csv
```
All curves are read as `time_s,total_2q` CSVs from `paper_results/`
(`qsymb_<gs>_2q_over_time.csv` for ours, `<tool>_<gs>_2q_over_time.csv` per
baseline) and plotted as suite-wide 2Q reduction rate over the 60-min budget
(`--linear` for a linear time axis). Outputs
`2q_reduction_vs_time_<gs>_log.png/.pdf` + `2q_reduction_legend.png/.pdf`.

### 4.5 (Optional) Re-run all baselines from scratch (≈ 405 CPU hours)

Runs 135 benchmarks × 5 tools (Qiskit, Guoq, TKET, Quartz, Queso). Queso,
Guoq, Quartz are search-based with a 1-hour timeout per circuit; Qiskit and
TKET terminate fast.

```bash
docker pull xiaoqianghweye/other_tools:oopsla26
docker tag  xiaoqianghweye/other_tools:oopsla26 other_tools
docker run -d --name other_tools other_tools sleep infinity
docker exec other_tools run_all_tools_bench.sh -o /root/paper_results benchmark.txt <N>
```
`<N>` = parallel tasks (~8 GB each; keep `N ≤ TotalMemory/8GB`). Results go to
`/root/paper_results` in that container (CSVs at top level with a `_fresh`
suffix, per-tool logs under `logs/`). Stream them into the QSymb container:

```bash
docker exec other_tools tar cf - -C /root/paper_results . \
  | docker exec -i qsymb tar xf - -C /root/paper_results
```

Re-render the figures against the fresh baselines (each `--<tool>` flag
accepts any CSV path; omitted tools use the precomputed CSVs):

```bash
python3 qsymb_plot_tools/generate_s_curves_with_progress_bars.py \
    --gateset ibmnew --ours paper_results/qsymb_ibmnew_results_fresh.csv \
    --qiskit paper_results/qiskit_ibmnew_results_fresh.csv \
    --guoq   paper_results/guoq_ibmnew_results_fresh.csv \
    --quartz paper_results/quartz_ibmnew_results_fresh.csv \
    --tket   paper_results/tket_ibmnew_results_fresh.csv \
    --queso  paper_results/queso_ibmnew_results_fresh.csv \
    --suffix fresh
```

---

## 5. Experiment 3 — ablations (§7.3, Figs. 13–14)

### 5.1 Generate figures from precomputed data (seconds)

```bash
python3 qsymb_plot_tools/generate_ablation_figures.py --gateset ibmnew \
    --full      paper_results/qsymb_ibmnew_results.csv \
    --concrete  paper_results/qsymb_ibmnew_concrete_results.csv \
    --canonical paper_results/qsymb_ibmnew_canonical_results.csv \
    --random    paper_results/qsymb_ibmnew_random_results.csv
```
Renders **Fig. 13** (`fig13_ablation_ibmnew.png`) and **Fig. 14**
(`fig14_concrete_vs_random_ibmnew.png`) into `paper_results/figures/`.
Fig. 14 is skipped if the random-variant CSV is absent.

### 5.2 Run the ablation variants (fresh ~405 CPU hours)

```bash
bash ablation.sh -g ibmnew -b benchmark.txt -t 3600 \
                 --full results_ibmnew_trial1/summary.csv -o /root/ablation_ibmnew
```
Runs three variants (135 circuits each; `--full` = a completed §4.2 summary,
not re-run):
| variant | configuration | figure |
|---|---|---|
| `concrete` | concrete rules only  | 13 & 14 |
| `canonical` | concrete + un-anchored symbolic | 13 |
| `random` | equal-count random Queso rules | 14 |

Outputs:
| path | description |
|---|---|
| `<out_dir>/<variant>/{<bench>/, summary.csv}` | per-variant logs and summary |
| `paper_results/qsymb_ibmnew_<variant>_results_fresh.csv` | copy of each fresh variant summary |
| `paper_results/figures/fig13_*_fresh.png`, `fig14_*_fresh.png` | fresh figures (generated automatically at the end) |

### 5.3 Generate figures from fresh data

Generated automatically by 5.2; to re-render manually:

```bash
python3 qsymb_plot_tools/generate_ablation_figures.py --gateset ibmnew \
    --full      paper_results/qsymb_ibmnew_results_fresh.csv \
    --concrete  paper_results/qsymb_ibmnew_concrete_results_fresh.csv \
    --canonical paper_results/qsymb_ibmnew_canonical_results_fresh.csv \
    --random    paper_results/qsymb_ibmnew_random_results_fresh.csv \
    --suffix fresh
```

**Compare to the paper**: `fig13_*` → **Fig. 13** (full vs. concrete vs.
canonical), `fig14_*` → **Fig. 14** (concrete vs. random).

---

## 6. Artifact layout and Reusesability

```
/root/
  build_qsymb.sh                 # build -> the fat jar
  kick_start.sh                  # EXPERIMENT 1: rule synthesis, Tables 1-2 (§7.1)
  measure_filter_speedup.sh      # EXPERIMENT 1: property-grouping speedup, Table 3 (§7.1)
  optimize.sh                    # EXPERIMENT 2: optimization, Figs. 11-12 (§7.2)
  ablation.sh                    # EXPERIMENT 3: ablations, Figs. 13-14   (§7.3)
  benchmark.txt                 # the paper's 135-circuit suite (names)
  qsymb_benchmarks/<gs>/          # benchmark QASM per gate set (nam_rz/ ibmnew/ ion/ rigetti/)
  SymbolicOptimizer/             # Java sources (Optimizer, EnumeratorPrune, Anchor)
    grammars/<gs>.grammar        #   enumeration grammar (concrete phase)
    grammars/<gs>_symb.grammar   #   enumeration grammar (symbolic phase)
  semantics.py                   # symbolic-matrix constraint solver (server pool)
  rules_<gs>.txt                 # hand-written egglog merge/commute rules
  qsymb_plot_tools/
    generate_s_curves_with_progress_bars.py   # Figs. 11-12
    generate_ablation_figures.py              # Figs. 13-14
    plot_2q_reduction.py                      # 2Q-reduction-vs-time figure
    calc_fidelity.py                          # per-circuit fidelity
  paper_results/                 # precomputed results: baseline CSVs, qsymb_* summaries
    figures/                     #   all generated figures (fig11..fig14, runtime)
    logs/                        #   baseline re-run logs (§4.5)
  qsymb.pdf                      # the paper
```

Benchmark suite (§7.2): 135 circuits; ibm-eagle total gate count avg 1371.8 /
max 17438 / min 15; qubits avg 13.3 / max 36 / min 4.

## 7. Troubleshooting: memory failures

Each task needs ~8 GB (JVM + egglog). If tasks die, the run prints
`FAILED <bench>: exit=137 (OOM-killed)` (or `JVM OutOfMemoryError`) at the
end and writes the failed benchmark names to a text file:
`<out_dir>/failed.txt` (optimize.sh) or `<out_dir>/failed_<variant>.txt`
(ablation.sh).

Re-run ONLY the failed tasks at lower parallelism into the SAME output
directory; the summary CSV is rebuilt over all benchmark directories, so
earlier successes are kept:

```bash
bash optimize.sh -b results_ibmnew_trial1/failed.txt -t 3600 -g ibmnew \
                 -n 4 -o results_ibmnew_trial1
```

Halve `-n` each time failures persist (memory spikes are transient; fewer
concurrent tasks means fewer coinciding spikes). `-n 1` is guaranteed to fit
on a 16 GB machine.
