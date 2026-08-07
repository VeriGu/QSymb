#!/usr/bin/env bash
# 17 guoq-losing benchmarks, compact -lr (rules_ibmnew_q3_5.txt) with the
# reverser stack ON (-Dlongrule.reverse=true), max 15 concurrent.
set -u
cd /root
D=/root/ablation_ibmnew/rev17
JAR=/root/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar
while read -r b; do
  [ -z "$b" ] && continue
  while [ "$(jobs -rp | wc -l)" -ge 15 ]; do sleep 30; done
  odir="$D/$b"; mkdir -p "$odir"
  echo "[$(date +%H:%M:%S)] launching $b"
  ( java --enable-preview -Xss256m -Xmx8g -Dsemantics.pool.size=2 -Dlongrule.reverse=true \
      -cp "$JAR" Optimizer -b "guoq_benchmarks/ibmnew/${b}.qasm" \
      -r rules_ibmnew_q3_5.txt -sr anchored_ibmnew_q3.txt -lr rules_ibmnew_q3_5.txt \
      -m SA -t 3600 -symb true -g ibmnew -ilp true -minsymb 5 -maxsymb 20 -q \
      -o "$odir" > "$odir/$b.log" 2>&1 ; echo $? > "$odir/$b.log.exit" ) &
done < "$D/bench.txt"
wait
echo "[$(date +%H:%M:%S)] ALL DONE"
