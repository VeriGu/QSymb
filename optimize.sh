#!/usr/bin/env bash
# optimize.sh -- run the SymbolicOptimizer on a list of benchmarks with the
# quiet-mode flags, one output directory per benchmark.
#
# Usage:
#   bash optimize.sh -b <benchmark.txt> -t <timeout_s> -g <gateset> -n <threads> \
#                    [-o <parent_out_dir>] [-p <baseline_csv_dir>]
#
# -p/--baselines: directory holding the baseline-tool CSVs used for the
#   S-curve figure (<tool>_<gateset>_results.csv for qiskit/guoq/quartz/tket/
#   queso). Default: /root/paper_results.
#
# Layout produced under <parent_out_dir> (default: /root/optimize_out_<gateset>_<timestamp>/):
#   parent_dir/
#     <bench>/
#       <bench>.log              # optimizer stdout+stderr (5 quiet-mode lines)
#       <bench>_optimized.qasm   # final circuit, written by Optimizer -o
#     ...
#
# Per-gateset rule file selection matches the kick_start artifacts. Long rules
# (-lr) are intentionally NOT passed for any gateset here.
#   ibmnew : -r rules_ibmnew_q3_5.txt   -sr anchored_ibmnew_q3.txt
#   nam    : -r rules_nam_q3_5.txt      -sr anchored_nam_q3.txt
#   ion    : -r rules_ion_q3_3.txt      -sr anchored_ion_q3_only.txt
#   rigetti: -r rules_rigetti_q3_5.txt  -sr anchored_rigetti_q3.txt

set -u

# Default parallelism: min(cores/2, TotalMemory/8GB), at least 1 -- each
# task can use ~8 GB (JVM + egglog), so this stays within memory.
CORES_HALF=$(( $(nproc) / 2 ))
MEM_SLOTS=$(( $(awk '/MemTotal/{print $2}' /proc/meminfo) / (8*1024*1024) ))
THREADS_DEFAULT=$(( CORES_HALF < MEM_SLOTS ? CORES_HALF : MEM_SLOTS ))
[ "$THREADS_DEFAULT" -lt 1 ] && THREADS_DEFAULT=1


BENCHMARK_FILE=""
TIMEOUT_S=""
GATESET=""
THREADS=""
PARENT_OUT=""
APPROX_EPS=""
MINSYMB="5"
MAXSYMB="20"
# Baseline-results directory for the S-curve figure. The figure script reads
# <dir>/<tool>_<gateset>_results.csv for tool in qiskit/guoq/quartz/tket/queso.
BASELINE_DIR="/root/paper_results"
# Suffix for the copies of freshly generated summaries/figures placed in
# paper_results (so they sit alongside, not overwrite, the precomputed ones).
SUFFIX="fresh"

while [ "$#" -gt 0 ]; do
  case "$1" in
    -b) BENCHMARK_FILE="$2"; shift 2 ;;
    -t) TIMEOUT_S="$2";      shift 2 ;;
    -g) GATESET="$2";        shift 2 ;;
    -n) THREADS="$2";        shift 2 ;;
    -o) PARENT_OUT="$2";     shift 2 ;;
    -p|--baselines) BASELINE_DIR="$2"; shift 2 ;;
    --suffix) SUFFIX="$2"; shift 2 ;;
    -approx) APPROX_EPS="$2"; shift 2 ;;
    -minsymb) MINSYMB="$2";  shift 2 ;;
    -maxsymb) MAXSYMB="$2";  shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

if [ -z "$THREADS" ]; then
  THREADS="$THREADS_DEFAULT"
  echo "-n not given: auto-computed n=$THREADS  (min(cores/2, TotalMemory/8GB) = min($CORES_HALF, $MEM_SLOTS))"
fi

if [ -z "$BENCHMARK_FILE" ] || [ -z "$TIMEOUT_S" ] || [ -z "$GATESET" ]; then
  echo "usage: bash optimize.sh -b <benchmark.txt> -t <timeout_s> -g <gateset> [-n <threads>] [-o <out_dir>] [-p <baseline_csv_dir>]" >&2
  exit 1
fi

cd /root
JAR=/root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar
if [ ! -f "$JAR" ]; then
  echo "FATAL: jar not found at $JAR -- run bash build_qsymb.sh first" >&2
  exit 1
fi

# Per-gateset benchmark directory (matches run_optimizer_suite.py).
declare -A BENCH_DIR=(
  [nam]=qsymb_benchmarks/nam_rz
  [ion]=qsymb_benchmarks/ion
  [ibmnew]=qsymb_benchmarks/ibmnew
  [rigetti]=qsymb_benchmarks/rigetti
)

# Per-gateset rule files. An empty -lr entry means: don't pass that flag.
declare -A RULE_R=(   [nam]=rules_nam_q3_5.txt      [ibmnew]=rules_ibmnew_q3_5.txt      [ion]=rules_ion_q3_3.txt     [rigetti]=rules_rigetti_q3_5.txt )
declare -A RULE_SR=(  [nam]=anchored_nam_q3.txt     [ibmnew]=anchored_ibmnew_q3.txt     [ion]=anchored_ion_q3_only.txt [rigetti]=anchored_rigetti_q3.txt )
declare -A RULE_LR=(  [nam]=rules_q3_s6_nam.txt     [ibmnew]=rules_q3_s6_ibmnew.txt     [ion]=rules_q3_s3_ion.txt    [rigetti]="" )

if [ -z "${RULE_R[$GATESET]:-}" ]; then
  echo "FATAL: no rule config for gateset '$GATESET'" >&2
  exit 1
fi

BDIR="${BENCH_DIR[$GATESET]:-qsymb_benchmarks/$GATESET}"
if [ ! -d "$BDIR" ]; then
  echo "FATAL: benchmark dir $BDIR not found" >&2
  exit 1
fi

# Parent output dir.
if [ -z "$PARENT_OUT" ]; then
  PARENT_OUT="/root/optimize_out_${GATESET}_$(date +%Y%m%d_%H%M%S)"
fi
mkdir -p "$PARENT_OUT"
echo "[$(date +%H:%M:%S)] parent output dir: $PARENT_OUT"

# Read the benchmark list (one basename per line, no .qasm extension).
mapfile -t BENCHES < <(grep -v '^$' "$BENCHMARK_FILE")
echo "[$(date +%H:%M:%S)] $((${#BENCHES[@]})) benchmarks, gateset=$GATESET, timeout=${TIMEOUT_S}s, threads=$THREADS"

# ---------------------------------------------------------------------------
#  Launcher: one benchmark -> its own subdir.
# ---------------------------------------------------------------------------
launch_bench() {
  local b="$1"
  local qasm="$BDIR/${b}.qasm"
  local out_dir="$PARENT_OUT/${b}"
  local log="$out_dir/${b}.log"
  mkdir -p "$out_dir"

  if [ ! -f "$qasm" ]; then
    echo "[$(date +%H:%M:%S)] SKIP $b: $qasm not found" | tee "$log"
    return
  fi

  local cmd=(java --enable-preview -Xss256m -Xmx8g -Dsemantics.pool.size=2 -cp "$JAR" Optimizer
    -b "$qasm"
    -r "${RULE_R[$GATESET]}"
    -m SA -t "$TIMEOUT_S" -symb true -g "$GATESET" -ilp true
    -minsymb "$MINSYMB" -maxsymb "$MAXSYMB"
    -q -o "$out_dir")
  if [ -n "${RULE_SR[$GATESET]:-}" ]; then
    cmd+=(-sr "${RULE_SR[$GATESET]}")
  fi
  if [ -n "${RULE_LR[$GATESET]:-}" ]; then
    cmd+=(-lr "${RULE_LR[$GATESET]}")
  fi
  if [ -n "$APPROX_EPS" ]; then
    cmd+=(-approx "$APPROX_EPS")
  fi

  echo "[$(date +%H:%M:%S)] launching $b"
  ( "${cmd[@]}" > "$log" 2>&1 ; ec=$?; echo "$ec" > "$log.exit"
    f2=$(grep -aoE "Final 2q: [0-9]+" "$log" | grep -oE "[0-9]+$" | tail -1)
    echo "[$(date +%H:%M:%S)] done $b (exit=$ec final_2q=${f2:-?})" ) &
}

for b in "${BENCHES[@]}"; do
  while [ "$(jobs -rp | wc -l)" -ge "$THREADS" ]; do wait -n; done
  launch_bench "$b"
done
wait

# ---------------------------------------------------------------------------
#  Aggregate: one CSV summary at the parent level.
# ---------------------------------------------------------------------------
summary="$PARENT_OUT/summary.csv"
echo "benchmark,exit,original_size,original_2q,final_size,final_2q,symb_applied,final_fidelity" > "$summary"
ok=0; fail=0
# Aggregate over every benchmark dir under PARENT_OUT (not just this
# invocation's -b list) so a targeted re-run of failed tasks into the same
# -o dir rebuilds a COMPLETE summary covering earlier successes too.
for d in "$PARENT_OUT"/*/; do
  b=$(basename "$d")
  log="$PARENT_OUT/${b}/${b}.log"
  [ ! -f "$log" ] && continue
  ec=$(cat "$log.exit" 2>/dev/null || echo "?")
  os=$(grep -oE "Original Gate Size: [0-9]+" "$log" | grep -oE "[0-9]+$")
  o2=$(grep -oE "Original 2q: [0-9]+"        "$log" | grep -oE "[0-9]+$")
  fs=$(grep -oE "Final Gate Size: [0-9]+"    "$log" | grep -oE "[0-9]+$")
  f2=$(grep -oE "Final 2q: [0-9]+"           "$log" | grep -oE "[0-9]+$")
  sa=$(grep -oE "Symbolic rules applied: [0-9]+" "$log" | grep -oE "[0-9]+$")
  # Final-circuit fidelity (product of per-gate fidelities) for ibmnew, via the
  # modular calc_fidelity.py --qasm CLI applied to the written optimized circuit.
  fid=""
  optq="$PARENT_OUT/${b}/${b}_optimized.qasm"
  if [ "$GATESET" = "ibmnew" ] && [ -f "$optq" ]; then
    fid=$(python3 /root/qsymb_plot_tools/calc_fidelity.py --qasm "$optq" 2>/dev/null \
          | grep -oE "FIDELITY [0-9.]+" | awk '{print $2}')
  fi
  echo "${b},${ec},${os:-},${o2:-},${fs:-},${f2:-},${sa:-},${fid:-}" >> "$summary"
  if [ "$ec" = "0" ] && [ -n "$f2" ]; then ok=$((ok+1)); else fail=$((fail+1)); fi
done
echo
echo "[$(date +%H:%M:%S)] done: $ok ok / $fail fail"
# Explicit failure listing: decode OOM kills (137 = SIGKILL, usually the OOM
# killer; also flag JVM OutOfMemoryError found in the log). Failed benchmark
# names go to failed.txt, which can be fed back via -b for a targeted re-run.
failed_txt="$PARENT_OUT/failed.txt"
rm -f "$failed_txt"
for d in "$PARENT_OUT"/*/; do
  b=$(basename "$d")
  log="$PARENT_OUT/${b}/${b}.log"; [ -f "$log" ] || continue
  ec=$(cat "$log.exit" 2>/dev/null || echo "?")
  if [ "$ec" != "0" ]; then
    why=""
    [ "$ec" = "137" ] && why=" (OOM-killed)"
    grep -q "OutOfMemoryError" "$log" && why=" (JVM OutOfMemoryError)"
    echo "  FAILED $b: exit=$ec$why"
    echo "$b" >> "$failed_txt"
  fi
done
[ -f "$failed_txt" ] && echo "failed benchmarks written to: $failed_txt (re-run: bash optimize.sh -b $failed_txt -t $TIMEOUT_S -g $GATESET -n <lower_n> -o $PARENT_OUT)"
echo "summary CSV: $summary"

# Store the fresh summary alongside the precomputed one in paper_results.
cp "$summary" "$BASELINE_DIR/qsymb_${GATESET}_results_${SUFFIX}.csv"
echo "fresh summary copy: $BASELINE_DIR/qsymb_${GATESET}_results_${SUFFIX}.csv"

# ---------------------------------------------------------------------------
#  Figure: 2q-reduction S-curves vs baseline tools (nam/ibmnew only -- the
#  gatesets with baseline data). Baseline CSVs are read from $BASELINE_DIR
#  (override with -p/--baselines), as <tool>_<gateset>_results.csv.
# ---------------------------------------------------------------------------
if [ "$GATESET" = "nam" ] || [ "$GATESET" = "ibmnew" ]; then
  FIG_DIR="$BASELINE_DIR/figures"
  mkdir -p "$FIG_DIR"
  echo "[$(date +%H:%M:%S)] generating 2q-reduction figure (baselines: $BASELINE_DIR, figures: $FIG_DIR)"
  if python3 /root/qsymb_plot_tools/generate_s_curves_with_progress_bars.py \
       --gateset "$GATESET" --ours "$summary" --outdir "$FIG_DIR" \
       --paper-results "$BASELINE_DIR" \
       --benchmarks "$BENCHMARK_FILE" --suffix "$SUFFIX"; then
    FP=$([ "$GATESET" = "ibmnew" ] && echo fig11 || echo fig12); echo "figure: $FIG_DIR/${FP}_s_curve_${GATESET}.png"
  else
    echo "WARN: figure generation failed (results unaffected)" >&2
  fi
fi
