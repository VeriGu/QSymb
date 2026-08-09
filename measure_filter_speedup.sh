#!/usr/bin/env bash
# measure_filter_speedup.sh -- RQ2 ablation (paper Table 3): measure the
# speedup of the trace-grouping + eigenvalue filters in symbolic-rule
# synthesis versus directly solving every candidate pair.
#
# For each gateset it runs EnumeratorPrune's symbolic phase twice:
#   run A: filters ON  (default pipeline)
#   run B: filters OFF (-nofilter, direct solve)
# and reports wall time, solver-call counts, rule counts, and the speedup.
# The symbolic rule size is FIXED per gateset (paper setting):
#   nam=3, ibmnew=3, rigetti=3, ion=2.
#
# Usage:
#   bash measure_filter_speedup.sh              # all four gatesets + Table 3
#   bash measure_filter_speedup.sh ibmnew       # a single gateset
#
# WARNING: the direct-solve run considers ALL candidate pairs and can take
# far longer than the filtered run -- that is the point of the measurement.
# Per-candidate solves are bounded by Params.SYMB_SOLVE_TIMEOUT_SEC.

set -u
cd /root

declare -A SIZES=( [nam]=3 [ibmnew]=3 [rigetti]=3 [ion]=2 )
declare -A PAPER_NAME=( [ibmnew]=ibm-eagle [nam]=nam [rigetti]=rigetti [ion]=ion )
ALL_GS=(ibmnew nam rigetti ion)

if [ "$#" -gt 0 ]; then
  GATESETS=("$@")
else
  GATESETS=("${ALL_GS[@]}")
fi

JAR=/root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar
[ -f "$JAR" ] || { echo "FATAL: jar missing -- bash build_qsymb.sh" >&2; exit 1; }

for GS in "${GATESETS[@]}"; do
  [ -n "${SIZES[$GS]:-}" ] || { echo "FATAL: unknown gateset '$GS' (use: ${ALL_GS[*]})" >&2; exit 1; }
  [ -f "/root/SymbolicOptimizer/grammars/${GS}_symb.grammar" ] || {
    echo "FATAL: grammar for $GS missing" >&2; exit 1; }
done

run_one() {  # gs size mode [extra flags]
  local GS="$1" SIZE="$2" mode="$3"; shift 3
  local OUTDIR=/root/filter_speedup_${GS}
  local log="$OUTDIR/${GS}_s${SIZE}_${mode}.log"
  echo "[$(date +%H:%M:%S)] symbolic synthesis: $GS s=$SIZE, filters $mode"
  SECONDS=0
  java --enable-preview -Xss256m -Xmx8g -cp "$JAR" EnumeratorPrune \
    -g "$GS" -q 3 -s "$SIZE" -symb true \
    --grammar "/root/SymbolicOptimizer/grammars/${GS}_symb.grammar" "$@" \
    > "$log" 2>&1
  local rc=$?
  echo "wall time (s): $SECONDS" >> "$log"
  # snapshot the produced rule set for cross-mode comparison
  cp "rules_${GS}_q3_${SIZE}_symb_nm.txt" "$OUTDIR/rules_${mode}.txt" 2>/dev/null
  echo "[$(date +%H:%M:%S)] done (exit $rc)"
}

report_one() {  # gs size -> per-gateset report
  local GS="$1" SIZE="$2"
  local OUTDIR=/root/filter_speedup_${GS}
  get() { grep -oE "$2" "$OUTDIR/${GS}_s${SIZE}_$1.log" 2>/dev/null | grep -oE "[0-9]+" | tail -1; }
  local t_on t_off cand_on cand_off rules_on rules_off
  t_on=$(get on  "Symbolic Rule generation time \(s\): [0-9]+")
  t_off=$(get off "Symbolic Rule generation time \(s\): [0-9]+")
  cand_on=$(get on  "final accepted candidates = [0-9]+")
  cand_off=$(get off "final accepted candidates = [0-9]+")
  rules_on=$(wc -l < "$OUTDIR/rules_on.txt" 2>/dev/null || echo 0)
  rules_off=$(wc -l < "$OUTDIR/rules_off.txt" 2>/dev/null || echo 0)

  echo
  echo "=== filter speedup: $GS, symb size $SIZE ==="
  printf "%-28s %12s %12s\n" "" "filters ON" "filters OFF"
  printf "%-28s %12s %12s\n" "symb generation time (s)" "${t_on:-?}" "${t_off:-?}"
  printf "%-28s %12s %12s\n" "solver candidates"        "${cand_on:-?}" "${cand_off:-?}"
  printf "%-28s %12s %12s\n" "rules generated"          "$rules_on" "$rules_off"
  if [ -n "${t_on:-}" ] && [ -n "${t_off:-}" ] && [ "$t_on" -gt 0 ]; then
    awk -v a="$t_off" -v b="$t_on" 'BEGIN{printf "SPEEDUP (generation time, off/on): %.1fx\n", a/b}'
  fi
  if cmp -s "$OUTDIR/rules_on.txt" "$OUTDIR/rules_off.txt"; then
    echo "rule sets: IDENTICAL (filters are lossless on this input)"
  else
    echo "rule sets: differ ($(diff "$OUTDIR/rules_on.txt" "$OUTDIR/rules_off.txt" 2>/dev/null | grep -c '^[<>]') diff lines) -- see $OUTDIR"
  fi
}

for GS in "${GATESETS[@]}"; do
  SIZE="${SIZES[$GS]}"
  mkdir -p "/root/filter_speedup_${GS}"
  run_one "$GS" "$SIZE" on
  run_one "$GS" "$SIZE" off -nofilter
  report_one "$GS" "$SIZE"
done

# ---------------------------------------------------------------------------
#  Paper Table 3 (Synthesis of canonical symbolic rules with and without
#  property grouping), same columns as the paper:
#  Gateset | #Rules | Cost w/ Grouping (s) | Cost w/o Grouping (s) | Speedup | size
# ---------------------------------------------------------------------------
echo
echo "=== Table 3. Synthesis of canonical symbolic rules with and without property grouping ==="
printf "%-10s %-8s %-22s %-24s %-9s %-5s\n" \
  "Gateset" "#Rules" "Cost w/ Grouping (s)" "Cost w/o Grouping (s)" "Speedup" "size"
for GS in "${GATESETS[@]}"; do
  SIZE="${SIZES[$GS]}"
  OUTDIR=/root/filter_speedup_${GS}
  t_on=$(grep -oE "Symbolic Rule generation time \(s\): [0-9]+" "$OUTDIR/${GS}_s${SIZE}_on.log" 2>/dev/null | grep -oE "[0-9]+" | tail -1)
  t_off=$(grep -oE "Symbolic Rule generation time \(s\): [0-9]+" "$OUTDIR/${GS}_s${SIZE}_off.log" 2>/dev/null | grep -oE "[0-9]+" | tail -1)
  # Sub-second generation times round to 0 whole seconds; fall back to the
  # process wall time so the speedup ratio stays defined.
  if [ -z "${t_on:-}" ] || [ "${t_on:-0}" -eq 0 ]; then
    t_on=$(grep -oE "wall time \(s\): [0-9]+" "$OUTDIR/${GS}_s${SIZE}_on.log" 2>/dev/null | grep -oE "[0-9]+" | tail -1)
    t_off=$(grep -oE "wall time \(s\): [0-9]+" "$OUTDIR/${GS}_s${SIZE}_off.log" 2>/dev/null | grep -oE "[0-9]+" | tail -1)
  fi
  rules=$(wc -l < "$OUTDIR/rules_on.txt" 2>/dev/null || echo "?")
  m_on=$(awk -v s="${t_on:-}" 'BEGIN{if(s=="")print"?";else printf "%d", s}')
  m_off=$(awk -v s="${t_off:-}" 'BEGIN{if(s=="")print"?";else printf "%d", s}')
  spd=$(awk -v a="${t_off:-0}" -v b="${t_on:-0}" 'BEGIN{if(b>0)printf "%.0fx", a/b; else print"?"}')
  printf "%-10s %-8s %-22s %-24s %-9s %-5s\n" \
    "${PAPER_NAME[$GS]:-$GS}" "$rules" "$m_on" "$m_off" "$spd" "$SIZE"
done
