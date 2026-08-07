#!/usr/bin/env bash
# queue_table3.sh -- waits for the machine to drain (no Optimizer JVMs), then
# reproduces paper Table 3 by running measure_filter_speedup.sh for all four
# gate sets. The per-gateset symbolic sizes are fixed inside
# measure_filter_speedup.sh (nam/ibmnew/rigetti=3, ion=2), and it prints the
# final Table 3 block itself.
#
# The shared rules_<gs>_q3_<size>_symb_nm.txt files are backed up and restored
# around the measurement (measure_filter_speedup overwrites them).

set -u
cd /root
declare -A SZ=( [ibmnew]=3 [nam]=3 [rigetti]=3 [ion]=2 )
ORDER=(ibmnew nam rigetti ion)

# 1. Wait for any running batch to drain so memory is free. Require BOTH the
#    batch orchestrator script(s) and all Optimizer JVMs to be gone -- checking
#    JVMs alone can race the JVM-free window between two variants of a batch.
echo "[$(date +%H:%M:%S)] waiting for batch scripts + Optimizer jobs to drain..."
while ps -eo args | grep -E '[r]un_cnr_random\.sh|[r]un_full_queso\.sh|[a]blation\.sh' >/dev/null \
   || [ "$(ps -eo args | grep 'jar-with-dependencies.jar Optimizer' | grep -vc grep)" -gt 0 ]; do
  sleep 60
done
echo "[$(date +%H:%M:%S)] machine free ($(free -g | awk '/Mem/{print $7}') GB avail); starting Table 3"

# 2. Back up the shared symbolic rule files.
for gs in "${ORDER[@]}"; do
  f="rules_${gs}_q3_${SZ[$gs]}_symb_nm.txt"
  [ -f "$f" ] && cp "$f" "/tmp/${f}.table3bak"
done

# 3. Run the measurement for all gate sets (prints Table 3 at the end).
bash measure_filter_speedup.sh || echo "WARN: measurement failed" >&2

# 4. Restore the shared symbolic rule files.
for gs in "${ORDER[@]}"; do
  f="rules_${gs}_q3_${SZ[$gs]}_symb_nm.txt"
  [ -f "/tmp/${f}.table3bak" ] && cp "/tmp/${f}.table3bak" "$f"
done

echo "[$(date +%H:%M:%S)] TABLE3 DONE -> logs in /root/filter_speedup_<gs>/"
