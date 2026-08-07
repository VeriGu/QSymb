#!/usr/bin/env bash
# kick_start.sh -- regenerate concrete + symbolic + anchored rule sets for
# every gateset.
#
# Runs three sequential phases per gateset:
#
#   Phase 1 (concrete):  EnumeratorPrune -symb false, grammar <gs>.grammar
#                        -> rules_<gs>_q3_<size_c>.txt
#   Phase 2 (symbolic):  EnumeratorPrune -symb true,  grammar <gs>_symb.grammar
#                        -> rules_<gs>_q3_<size_s>_symb_nm.txt
#   Phase 3 (anchor):    Anchor -r <concrete_out> -sr <symb_out> -g <gs>
#                        -> anchored_<gs>_q3.txt
#
# Anchor composes each canonical symbolic rule with the size-reducing merge
# rules from rules_<gateset>.txt (auto-loaded from the -g flag) to produce
# a "shrink-ready" anchored ruleset that the SA runtime consumes via -sr.
#
# Usage:
#   bash kick_start.sh                 # all default gatesets
#   bash kick_start.sh nam ion         # only listed gatesets
#   MAXJOBS=1 bash kick_start.sh       # serial (default: 5)
#
# Logs land at kick_start_logs/<gateset>_{concrete,symb,anchor}.log.
# Exit 0 iff every gateset produced non-empty output in all three phases.

set -u

cd /root

JAR=/root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar
GRAMMAR_DIR=/root/SymbolicOptimizer/grammars
LOG_DIR=/root/kick_start_logs
MAXJOBS=${MAXJOBS:-5}
MAX_QUBITS=3

# Per-gateset max rule size, separate for concrete and symbolic phases.
# ion enumerates with larger 2-qubit gate spaces (rxx) so we cap it one size
# below the others in both phases. Concrete uses q3/q5 pattern (ion=3,
# others=5); symbolic uses smaller sizes because symbolic search is far more
# expensive per candidate.
declare -A SIZES_CONCRETE=(
  [nam]=5
  [ibmnew]=5
  [ion]=3
  [rigetti]=5
)
declare -A SIZES_SYMB=(
  [nam]=3
  [ibmnew]=3
  [ion]=2
  [rigetti]=3
)

DEFAULT_GATESETS=(nam ibmnew ion rigetti)

if [ "$#" -gt 0 ]; then
  GATESETS=("$@")
else
  GATESETS=("${DEFAULT_GATESETS[@]}")
fi

mkdir -p "$LOG_DIR"

if [ ! -f "$JAR" ]; then
  echo "FATAL: jar not found at $JAR -- run bash build_qsymb.sh first" >&2
  exit 1
fi

# ---------------------------------------------------------------------------
#  launch_one <gs> <phase>
#    phase = "concrete" | "symb" | "anchor"
# ---------------------------------------------------------------------------
launch_one() {
  local gs="$1"
  local phase="$2"
  local log="$LOG_DIR/${gs}_${phase}.log"
  local size_c="${SIZES_CONCRETE[$gs]:-}"
  local size_s="${SIZES_SYMB[$gs]:-}"

  # Compute per-phase inputs/outputs.
  local grammar symb_flag out size
  case "$phase" in
    concrete)
      size="$size_c"
      symb_flag=false
      grammar="$GRAMMAR_DIR/${gs}.grammar"
      out="/root/rules_${gs}_q${MAX_QUBITS}_${size}.txt"
      ;;
    symb)
      size="$size_s"
      symb_flag=true
      grammar="$GRAMMAR_DIR/${gs}_symb.grammar"
      out="/root/rules_${gs}_q${MAX_QUBITS}_${size}_symb_nm.txt"
      ;;
    anchor)
      # Anchor consumes the outputs of the other two phases; no grammar.
      out="/root/anchored_${gs}_q${MAX_QUBITS}.txt"
      ;;
    *)
      echo "[SKIP] $gs: unknown phase '$phase'" >&2
      echo 2 > "$log.exit"
      return
      ;;
  esac

  # Precondition checks per phase.
  if [ "$phase" != "anchor" ]; then
    if [ -z "$size" ]; then
      echo "[SKIP] $gs: no size configured for phase $phase" >&2
      echo 2 > "$log.exit"
      return
    fi
    if [ ! -f "$grammar" ]; then
      echo "[SKIP] $gs: grammar file $grammar not found" >&2
      echo 2 > "$log.exit"
      return
    fi
  else
    # Anchor needs both prior outputs on disk.
    local concrete_in="/root/rules_${gs}_q${MAX_QUBITS}_${size_c}.txt"
    local symb_in="/root/rules_${gs}_q${MAX_QUBITS}_${size_s}_symb_nm.txt"
    if [ ! -s "$concrete_in" ] || [ ! -s "$symb_in" ]; then
      echo "[SKIP] $gs anchor: missing input ($concrete_in or $symb_in)" >&2
      echo 2 > "$log.exit"
      return
    fi
  fi

  # Backup any prior output so a bad run doesn't silently overwrite.
  if [ -f "$out" ]; then
    cp "$out" "${out}.bak_kick_start"
  fi

  # Launch. Anchor doesn't emit its own wall-time log, so wrap the invocation
  # with SECONDS and append it in the same format the report parses.
  if [ "$phase" = "anchor" ]; then
    echo "[$(date +%H:%M:%S)] launching $gs (anchor)"
    (
      SECONDS=0
      java --enable-preview -Xss256m -Xmx8g \
        -cp "$JAR" Anchor \
        -r "$concrete_in" \
        -sr "$symb_in" \
        -g "$gs" \
        -o "$out" \
        > "$log" 2>&1
      rc=$?
      echo "Anchor wall time (s): $SECONDS" >> "$log"
      echo "$rc" > "$log.exit"
    ) &
  else
    echo "[$(date +%H:%M:%S)] launching $gs ($phase, q=$MAX_QUBITS, size=$size)"
    (
      java --enable-preview -Xss256m -Xmx8g \
        -cp "$JAR" EnumeratorPrune \
        -g "$gs" \
        -q "$MAX_QUBITS" \
        -s "$size" \
        -symb "$symb_flag" \
        --grammar "$grammar" \
        > "$log" 2>&1
      echo $? > "$log.exit"
    ) &
  fi
}

run_phase() {
  local phase="$1"
  echo
  echo "=========================================================="
  echo "  Phase: $phase"
  echo "=========================================================="
  for gs in "${GATESETS[@]}"; do
    while [ "$(jobs -rp | wc -l)" -ge "$MAXJOBS" ]; do wait -n; done
    launch_one "$gs" "$phase"
  done
  wait
}

run_phase concrete
run_phase symb
run_phase anchor

# ---------------------------------------------------------------------------
#  Report -- one row per gateset covering both phases.
# ---------------------------------------------------------------------------
echo
# ---------------------------------------------------------------------------
#  Paper Table 1 (Concrete rule-set size), exactly the paper's columns:
#  Gateset | Gates | #Rules (Qsymb) | #Rules (Prior) | n | Time.
#  Prior-tool counts are the fixed reference values from the paper.
# ---------------------------------------------------------------------------
declare -A PAPER_NAME=(  [ibmnew]=ibm-eagle [nam]=nam [ion]=ion [rigetti]=rigetti )
declare -A PAPER_GATES=( [ibmnew]="cx rz x sx" [nam]="x h rz cx" [ion]="rx ry rz rxx" [rigetti]="rx1 rx2 rx3 rz cz" )
declare -A PRIOR_RULES=( [ibmnew]="6291 (Queso)" [nam]="8002 (Quartz,Queso)" [rigetti]="7904 (Quartz,Queso)" [ion]="11776 (Queso)" )

echo "=== Table 1. Concrete rule-set size ==="
printf "%-10s %-18s %-14s %-20s %-3s %-8s\n" \
  "Gateset" "Gates" "#Rules (Qsymb)" "#Rules (Prior)" "n" "Time (s)"
overall=0
declare -A FAILS=()
for gs in "${GATESETS[@]}"; do
  size_c="${SIZES_CONCRETE[$gs]:-?}"
  size_s="${SIZES_SYMB[$gs]:-?}"
  out_c="/root/rules_${gs}_q${MAX_QUBITS}_${size_c}.txt"
  out_s="/root/rules_${gs}_q${MAX_QUBITS}_${size_s}_symb_nm.txt"
  out_a="/root/anchored_${gs}_q${MAX_QUBITS}.txt"
  log_c="$LOG_DIR/${gs}_concrete.log"
  log_s="$LOG_DIR/${gs}_symb.log"
  log_a="$LOG_DIR/${gs}_anchor.log"

  exit_c=$(cat "$log_c.exit" 2>/dev/null || echo "?")
  exit_s=$(cat "$log_s.exit" 2>/dev/null || echo "?")
  exit_a=$(cat "$log_a.exit" 2>/dev/null || echo "?")
  rules_c=$(wc -l < "$out_c" 2>/dev/null || echo 0)
  rules_s=$(wc -l < "$out_s" 2>/dev/null || echo 0)
  rules_a=$(wc -l < "$out_a" 2>/dev/null || echo 0)
  # Concrete-pass wall time.
  concrete_s=$(grep -oE "Concrete Rule generation time \(s\): [0-9]+" "$log_c" 2>/dev/null | grep -oE "[0-9]+" | tail -1)
  [ -z "$concrete_s" ] && \
    concrete_s=$(grep -oE "total time \(s\): [0-9]+" "$log_c" 2>/dev/null | grep -oE "[0-9]+" | tail -1)

  fail=""
  if [ "$exit_c" != "0" ] || [ "$rules_c" -eq 0 ]; then fail="${fail}concrete "; overall=1; fi
  if [ "$exit_s" != "0" ] || [ "$rules_s" -eq 0 ]; then fail="${fail}symb "; overall=1; fi
  if [ "$exit_a" != "0" ] || [ "$rules_a" -eq 0 ]; then fail="${fail}anchor "; overall=1; fi
  [ -n "$fail" ] && FAILS[$gs]="${fail% }"

  printf "%-10s %-18s %-14s %-20s %-3s %-8s\n" \
    "${PAPER_NAME[$gs]:-$gs}" "${PAPER_GATES[$gs]:-?}" \
    "$rules_c" "${PRIOR_RULES[$gs]:--}" "$size_c" "${concrete_s:-?}"
done

# ---------------------------------------------------------------------------
#  Paper Table 2 (Anchoring cost of symbolic rules), same columns/format:
#  Gateset | #Rules (anchored) | Time (s) | size (concrete-rule length used).
# ---------------------------------------------------------------------------
echo
echo "=== Table 2. Anchoring cost of symbolic rules ==="
printf "%-10s %-8s %-9s %-5s\n" "Gateset" "#Rules" "Time (s)" "size"
for gs in "${GATESETS[@]}"; do
  size_c="${SIZES_CONCRETE[$gs]:-?}"
  out_a="/root/anchored_${gs}_q${MAX_QUBITS}.txt"
  log_a="$LOG_DIR/${gs}_anchor.log"
  rules_a=$(wc -l < "$out_a" 2>/dev/null || echo 0)
  anchor_s=$(grep -oE "Anchor wall time \(s\): [0-9]+" "$log_a" 2>/dev/null | grep -oE "[0-9]+" | tail -1)
  printf "%-10s %-8s %-9s %-5s\n" "$gs" "$rules_a" "${anchor_s:-?}" "$size_c"
done

if [ "$overall" -eq 0 ]; then
  echo
  echo "[$(date +%H:%M:%S)] ALL RULE SETS REGENERATED"
else
  echo
  echo "[$(date +%H:%M:%S)] one or more phases failed -- see logs in $LOG_DIR" >&2
  for gs in "${!FAILS[@]}"; do
    echo "  $gs: failed phase(s) -> ${FAILS[$gs]}" >&2
  done
fi
exit "$overall"
