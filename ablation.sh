#!/usr/bin/env bash
# ablation.sh -- run the §7.3 ablation variants (paper Fig 13 & 14).
#
# Reuses the FULL Qsymb run (concrete + anchored symbolic, produced by
# optimize.sh, e.g. /root/final_<gs>) -- it is NOT re-run here. This script
# only runs the ablation variants, into a separate output tree, then draws
# the two ablation figures.
#
# Variants (ONE concrete variant, no -lr; serves Fig 13 and Fig 14):
#   concrete    -r <concrete>               (no -sr, no -lr, -symb false) + ILP
#   canonical   -r <concrete> -sr <symb_nm> (un-anchored symbolic) + long rules + ILP
#   random      -r random.txt               (same #rules, random Queso subset; no -sr, no -lr)
# Figures land in /root/paper_results/figures/.
#
# Usage:
#   bash ablation.sh -g ibmnew -b benchmark.txt -t 3600 -n 16 \
#                    --full /root/final_ibmnew/summary.csv [-o ablation_ibmnew]
#
# Each variant writes <outdir>/<variant>/{<bench>/<bench>.log, summary.csv}.

set -u
cd /root

# Default parallelism: min(cores/2, TotalMemory/8GB), at least 1 -- each
# task can use ~8 GB (JVM + egglog), so this stays within memory.
CORES_HALF=$(( $(nproc) / 2 ))
MEM_SLOTS=$(( $(awk '/MemTotal/{print $2}' /proc/meminfo) / (8*1024*1024) ))
THREADS_DEFAULT=$(( CORES_HALF < MEM_SLOTS ? CORES_HALF : MEM_SLOTS ))
[ "$THREADS_DEFAULT" -lt 1 ] && THREADS_DEFAULT=1


GATESET="ibmnew"; BENCHMARK_FILE="benchmark.txt"; TIMEOUT_S="3600"; THREADS=""
PARENT_OUT=""; FULL_SUMMARY=""; MINSYMB="5"; MAXSYMB="20"; SUFFIX="fresh"
while [ "$#" -gt 0 ]; do
  case "$1" in
    -g) GATESET="$2"; shift 2 ;;
    -b) BENCHMARK_FILE="$2"; shift 2 ;;
    -t) TIMEOUT_S="$2"; shift 2 ;;
    -n) THREADS="$2"; shift 2 ;;
    -o) PARENT_OUT="$2"; shift 2 ;;
    --full) FULL_SUMMARY="$2"; shift 2 ;;
    --minsymb) MINSYMB="$2"; shift 2 ;;
    --maxsymb) MAXSYMB="$2"; shift 2 ;;
    --suffix) SUFFIX="$2"; shift 2 ;;
    *) echo "unknown arg: $1" >&2; exit 1 ;;
  esac
done

if [ -z "$THREADS" ]; then
  THREADS="$THREADS_DEFAULT"
  echo "-n not given: auto-computed n=$THREADS  (min(cores/2, TotalMemory/8GB) = min($CORES_HALF, $MEM_SLOTS))"
fi

JAR=/root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar
[ -f "$JAR" ] || { echo "FATAL: jar missing -- bash build_qsymb.sh" >&2; exit 1; }
[ -z "$PARENT_OUT" ] && PARENT_OUT="/root/ablation_${GATESET}"
[ -z "$FULL_SUMMARY" ] && FULL_SUMMARY="/root/final_${GATESET}/summary.csv"

declare -A BENCH_DIR=( [nam]=qsymb_benchmarks/nam_rz [ion]=qsymb_benchmarks/ion [ibmnew]=qsymb_benchmarks/ibmnew [rigetti]=qsymb_benchmarks/rigetti )
BDIR="${BENCH_DIR[$GATESET]:-qsymb_benchmarks/$GATESET}"

# Per-gateset rule files (must exist from kick_start.sh).
declare -A R_CONCRETE=( [nam]=rules_nam_q3_5.txt [ibmnew]=rules_ibmnew_q3_5.txt [ion]=rules_ion_q3_3.txt [rigetti]=rules_rigetti_q3_5.txt )
declare -A R_SYMBNM=(   [nam]=rules_nam_q3_3_symb_nm.txt [ibmnew]=rules_ibmnew_q3_3_symb_nm.txt [ion]=rules_ion_q3_2_symb_nm.txt [rigetti]=rules_rigetti_q3_3_symb_nm.txt )
declare -A R_LONG=(     [nam]=rules_q3_s6_nam.txt [ibmnew]=rules_q3_s6_ibmnew.txt [ion]=rules_q3_s3_ion.txt [rigetti]="" )
CONCRETE="${R_CONCRETE[$GATESET]}"; SYMBNM="${R_SYMBNM[$GATESET]}"; LONG="${R_LONG[$GATESET]:-}"
for f in "$CONCRETE" "$SYMBNM"; do
  [ -s "$f" ] || { echo "FATAL: rule file $f missing/empty -- run kick_start.sh $GATESET" >&2; exit 1; }
done

# --- Fig 14 random rule set: same #rules as concrete, drawn from Queso's set.
RANDOM_RULES="/root/random_${GATESET}.txt"
if [ -n "$LONG" ] && [ -s "$LONG" ]; then
  NCONC=$(grep -vc '^$' "$CONCRETE")
  awk 'BEGIN{srand(42)} NF{print rand()"\t"$0}' "$LONG" | sort | head -n "$NCONC" | cut -f2- > "$RANDOM_RULES"
  echo "random rule set: $RANDOM_RULES ($(wc -l < "$RANDOM_RULES") rules, from $LONG)"
else
  echo "WARN: no long-rule file for $GATESET; random variant will be skipped" >&2
fi

mkdir -p "$PARENT_OUT"
mapfile -t BENCHES < <(grep -v '^$' "$BENCHMARK_FILE")
echo "[$(date +%H:%M:%S)] ablation: $GATESET, ${#BENCHES[@]} benchmarks, t=${TIMEOUT_S}s, n=$THREADS"

# ---------------------------------------------------------------------------
#  run_variant <name> <symb:true|false> <sr-file|""> <lr-file|""> <r-file>
# ---------------------------------------------------------------------------
run_variant() {
  local name="$1" symb="$2" sr="$3" lr="$4" rfile="$5"
  local vout="$PARENT_OUT/$name"
  mkdir -p "$vout"
  echo "[$(date +%H:%M:%S)] === variant $name  (r=$rfile symb=$symb sr=${sr:-none} lr=${lr:-none}) ==="
  for b in "${BENCHES[@]}"; do
    while [ "$(jobs -rp | wc -l)" -ge "$THREADS" ]; do wait -n; done
    local qasm="$BDIR/${b}.qasm"; local odir="$vout/$b"; local log="$odir/$b.log"
    mkdir -p "$odir"
    [ -f "$qasm" ] || { echo "SKIP $b: $qasm not found" > "$log"; continue; }
    local cmd=(java --enable-preview -Xss256m -Xmx8g -Dsemantics.pool.size=2 -cp "$JAR" Optimizer
      -b "$qasm" -r "$rfile" -m SA -t "$TIMEOUT_S" -symb "$symb" -g "$GATESET"
      -ilp true -minsymb "$MINSYMB" -maxsymb "$MAXSYMB" -q -o "$odir")
    [ -n "$sr" ] && cmd+=(-sr "$sr")
    [ -n "$lr" ] && cmd+=(-lr "$lr")
    ( "${cmd[@]}" > "$log" 2>&1 ; ec=$?; echo "$ec" > "$log.exit"
      f2=$(grep -aoE "Final 2q: [0-9]+" "$log" | grep -oE "[0-9]+$" | tail -1)
      echo "[$(date +%H:%M:%S)] done $b (exit=$ec final_2q=${f2:-?})" ) &
  done
  wait
  # per-variant summary -- aggregated over every benchmark dir in the variant
  # tree (not just this invocation's -b list) so a targeted re-run of failed
  # tasks into the same -o dir rebuilds a complete summary.
  local summ="$vout/summary.csv"
  echo "benchmark,exit,original_size,original_2q,final_size,final_2q,symb_applied" > "$summ"
  local d
  for d in "$vout"/*/; do
    local b; b=$(basename "$d")
    local log="$vout/$b/$b.log"; [ -f "$log" ] || continue
    local ec os o2 fs f2 sa
    ec=$(cat "$log.exit" 2>/dev/null || echo "?")
    os=$(grep -oE "Original Gate Size: [0-9]+" "$log" | grep -oE "[0-9]+$")
    o2=$(grep -oE "Original 2q: [0-9]+"        "$log" | grep -oE "[0-9]+$")
    fs=$(grep -oE "Final Gate Size: [0-9]+"    "$log" | grep -oE "[0-9]+$")
    f2=$(grep -oE "Final 2q: [0-9]+"           "$log" | grep -oE "[0-9]+$")
    sa=$(grep -oE "Symbolic rules applied: [0-9]+" "$log" | grep -oE "[0-9]+$")
    echo "${b},${ec},${os:-},${o2:-},${fs:-},${f2:-},${sa:-}" >> "$summ"
  done
  echo "[$(date +%H:%M:%S)] variant $name done -> $summ"
  # Explicit failure listing (137 = SIGKILL, usually the OOM killer).
  # Failed names accumulate in <outdir>/failed_<variant>.txt for re-runs.
  local failed_txt="$PARENT_OUT/failed_${name}.txt"
  rm -f "$failed_txt"
  for d in "$vout"/*/; do
    local b; b=$(basename "$d")
    local flog="$vout/$b/$b.log"; [ -f "$flog" ] || continue
    local fec; fec=$(cat "$flog.exit" 2>/dev/null || echo "?")
    if [ "$fec" != "0" ]; then
      local why=""
      [ "$fec" = "137" ] && why=" (OOM-killed)"
      grep -q "OutOfMemoryError" "$flog" && why=" (JVM OutOfMemoryError)"
      echo "  FAILED $b: exit=$fec$why"
      echo "$b" >> "$failed_txt"
    fi
  done
  [ -f "$failed_txt" ] && echo "failed benchmarks written to: $failed_txt"
  # Store the fresh variant summary alongside the precomputed one.
  cp "$summ" "/root/paper_results/qsymb_${GATESET}_${name}_results_${SUFFIX}.csv"
}

# Variants. ONE concrete variant only: concrete rules, NO long rules -- used
# both as the Fig-13 concrete series and the Fig-14 reference.
run_variant concrete   false ""        ""      "$CONCRETE"
run_variant canonical  true  "$SYMBNM" "$LONG" "$CONCRETE"
[ -s "$RANDOM_RULES" ] && run_variant random false "" "" "$RANDOM_RULES"

# ---------------------------------------------------------------------------
#  Figures
# ---------------------------------------------------------------------------
echo "[$(date +%H:%M:%S)] generating ablation figures"
python3 /root/qsymb_plot_tools/generate_ablation_figures.py \
  --gateset "$GATESET" \
  --full "$FULL_SUMMARY" \
  --concrete "$PARENT_OUT/concrete/summary.csv" \
  --canonical "$PARENT_OUT/canonical/summary.csv" \
  --random "$PARENT_OUT/random/summary.csv" \
  --benchmarks "$BENCHMARK_FILE" \
  --outdir /root/paper_results/figures \
  --suffix "$SUFFIX" \
  || echo "WARN: figure generation failed" >&2

echo "[$(date +%H:%M:%S)] ABLATION DONE -> $PARENT_OUT"
