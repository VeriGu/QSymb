#!/usr/bin/env bash
# queue_random.sh -- after the current batch drains, run the missing ablation
# `random` variant (equal-count random Queso rules, no -lr) on the 135
# benchmarks, store its summary as the PRECOMPUTED
# paper_results/qsymb_ibmnew_random_results.csv, and render Fig 14.

set -u
cd /root
T=3600; N=20; GS=ibmnew
JAR=/root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar
BDIR=guoq_benchmarks/ibmnew

echo "[$(date +%H:%M:%S)] waiting for running batches to drain..."
while ps -eo args | grep -E '[o]ptimize\.sh -b|[a]blation\.sh' >/dev/null \
   || [ "$(ps -eo args | grep 'jar-with-dependencies.jar Optimizer' | grep -vc grep)" -gt 0 ]; do
  sleep 120
done
echo "[$(date +%H:%M:%S)] drained; running random variant"

# same-size random Queso subset (seed 42), matching ablation.sh
NCONC=$(grep -c ' | ' rules_ibmnew_q3_5.txt)
awk 'BEGIN{srand(42)} NF{print rand()"\t"$0}' rules_q3_s6_ibmnew.txt | sort | head -n "$NCONC" | cut -f2- > random_ibmnew.txt
echo "random set: $(wc -l < random_ibmnew.txt) rules"

vout=/root/ablation_ibmnew/random
rm -rf "$vout"; mkdir -p "$vout"
mapfile -t BENCHES < <(grep -v '^$' benchmark.txt)
for b in "${BENCHES[@]}"; do
  while [ "$(jobs -rp | wc -l)" -ge "$N" ]; do wait -n; done
  qasm="$BDIR/${b}.qasm"; odir="$vout/$b"; log="$odir/$b.log"; mkdir -p "$odir"
  [ -f "$qasm" ] || { echo "SKIP $b" > "$log"; continue; }
  ( java --enable-preview -Xss256m -Xmx8g -Dsemantics.pool.size=2 -cp "$JAR" Optimizer \
      -b "$qasm" -r random_ibmnew.txt -m SA -t "$T" -symb false -g "$GS" \
      -ilp true -minsymb 5 -maxsymb 20 -q -o "$odir" > "$log" 2>&1 ; echo $? > "$log.exit" ) &
done
wait

summ="$vout/summary.csv"
echo "benchmark,exit,original_size,original_2q,final_size,final_2q,symb_applied" > "$summ"
for b in "${BENCHES[@]}"; do
  log="$vout/$b/$b.log"; [ -f "$log" ] || continue
  ec=$(cat "$log.exit" 2>/dev/null || echo "?")
  os=$(grep -oE "Original Gate Size: [0-9]+" "$log" | grep -oE "[0-9]+$")
  o2=$(grep -oE "Original 2q: [0-9]+"        "$log" | grep -oE "[0-9]+$")
  fs=$(grep -oE "Final Gate Size: [0-9]+"    "$log" | grep -oE "[0-9]+$")
  f2=$(grep -oE "Final 2q: [0-9]+"           "$log" | grep -oE "[0-9]+$")
  sa=$(grep -oE "Symbolic rules applied: [0-9]+" "$log" | grep -oE "[0-9]+$")
  echo "${b},${ec},${os:-},${o2:-},${fs:-},${f2:-},${sa:-}" >> "$summ"
done
cp "$summ" paper_results/qsymb_ibmnew_random_results.csv
echo "[$(date +%H:%M:%S)] random done -> paper_results/qsymb_ibmnew_random_results.csv ($(($(wc -l <$summ)-1)) rows)"

# Fig 14 (and refreshed Fig 13) from the precomputed CSVs.
python3 /root/qsymb_plot_tools/generate_ablation_figures.py --gateset ibmnew \
    --full      paper_results/qsymb_ibmnew_results.csv \
    --concrete  paper_results/qsymb_ibmnew_concrete_results.csv \
    --canonical paper_results/qsymb_ibmnew_canonical_results.csv \
    --random    paper_results/qsymb_ibmnew_random_results.csv
echo "[$(date +%H:%M:%S)] QUEUE_RANDOM DONE"
