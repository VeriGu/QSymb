#!/usr/bin/env bash
# queue_fixup.sh -- after the in-flight full rerun's optimize.sh exits:
#   1. re-run every OOM-killed (exit 137) benchmark solo at low parallelism
#   2. rebuild summary.csv (incl. fidelity column)
#   3. do the fresh-copy + figure steps the (old) in-flight script lacks:
#      qsymb_ibmnew_results_fresh.csv, over-time fresh CSV, fig11 _fresh figures
# queue_random.sh keeps waiting while our re-run JVMs are alive, so ordering
# is: full drain -> fixup re-runs -> fixup aggregation -> random variant.

set -u
cd /root
D=/root/ablation_ibmnew/full
JAR=/root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar

echo "[$(date +%H:%M:%S)] waiting for the full-rerun optimize.sh to exit..."
while ps -eo args | grep -E '[o]ptimize\.sh -b benchmark' >/dev/null; do sleep 120; done
echo "[$(date +%H:%M:%S)] full rerun done; checking for OOM victims"

mapfile -t VICTIMS < <(for f in "$D"/*/*.log.exit; do
  [ "$(cat "$f" 2>/dev/null)" = "137" ] && basename "$(dirname "$f")"; done)
echo "victims (${#VICTIMS[@]}): ${VICTIMS[*]:-none}"

# One pass: all victims in parallel (the batch has drained, so the whole
# machine is available; victim count is small).
for b in "${VICTIMS[@]:-}"; do
  [ -z "$b" ] && continue
  odir="$D/$b"; log="$odir/$b.log"
  echo "[$(date +%H:%M:%S)] re-running $b"
  ( java --enable-preview -Xss256m -Xmx8g -Dsemantics.pool.size=2 -cp "$JAR" Optimizer \
      -b "guoq_benchmarks/ibmnew/${b}.qasm" -r rules_ibmnew_q3_5.txt \
      -sr anchored_ibmnew_q3.txt -lr rules_q3_s6_ibmnew.txt -m SA -t 3600 -symb true \
      -g ibmnew -ilp true -minsymb 5 -maxsymb 20 -q -o "$odir" > "$log" 2>&1 ; \
    echo $? > "$log.exit" ) &
done
wait
echo "[$(date +%H:%M:%S)] re-runs done"

# Rebuild summary (same aggregation as optimize.sh, incl. fidelity).
summary="$D/summary.csv"
echo "benchmark,exit,original_size,original_2q,final_size,final_2q,symb_applied,final_fidelity" > "$summary"
while read -r b; do
  [ -z "$b" ] && continue
  log="$D/$b/$b.log"; [ -f "$log" ] || continue
  ec=$(cat "$log.exit" 2>/dev/null || echo "?")
  os=$(grep -oE "Original Gate Size: [0-9]+" "$log" | grep -oE "[0-9]+$")
  o2=$(grep -oE "Original 2q: [0-9]+"        "$log" | grep -oE "[0-9]+$")
  fs=$(grep -oE "Final Gate Size: [0-9]+"    "$log" | grep -oE "[0-9]+$")
  f2=$(grep -oE "Final 2q: [0-9]+"           "$log" | grep -oE "[0-9]+$")
  sa=$(grep -oE "Symbolic rules applied: [0-9]+" "$log" | grep -oE "[0-9]+$")
  fid=""
  optq="$D/$b/${b}_optimized.qasm"
  [ -f "$optq" ] && fid=$(python3 /root/qsymb_plot_tools/calc_fidelity.py --qasm "$optq" 2>/dev/null \
        | grep -oE "FIDELITY [0-9.]+" | awk '{print $2}')
  echo "${b},${ec},${os:-},${o2:-},${fs:-},${f2:-},${sa:-},${fid:-}" >> "$summary"
done < benchmark.txt
echo "[$(date +%H:%M:%S)] summary rebuilt ($(($(wc -l <"$summary")-1)) rows)"

# PROMOTE the fresh run to the precomputed ibmnew results: replace
# qsymb_ibmnew_results.csv + the over-time CSV, and regenerate the
# unsuffixed fig11 figures from the new data.
cp "$summary" paper_results/qsymb_ibmnew_results.csv
python3 qsymb_plot_tools/export_2q_over_time.py --gateset ibmnew --ours-dir "$D" --suffix ''
python3 qsymb_plot_tools/generate_s_curves_with_progress_bars.py \
    --gateset ibmnew --ours paper_results/qsymb_ibmnew_results.csv
python3 qsymb_plot_tools/plot_2q_reduction.py --gateset ibmnew
echo "[$(date +%H:%M:%S)] FIXUP DONE (fresh promoted to precomputed)"
