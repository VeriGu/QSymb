#!/bin/bash

mkdir -p /root/logs/queso_logs

numBenchmarks=`cat /root/$1 | wc -l`
totalTasks=$((numBenchmarks * 13))
tasksCompleted=0

# Run QUESO normal
while read c; do
  echo "Running QUESO on ${c}.qasm using Nam gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g nam -r rules_q3_s6_nam.txt -sr rules_q3_s3_nam_symb.txt -t $2 -o optimized_benchmarks -j "nam"
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_normal_logs_"${1/.txt/}"_${2}_nam.txt

while read c; do
  echo "Running QUESO on ${c}.qasm using IBM gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ibm -r rules_q3_s4_ibm.txt -sr rules_q3_s3_ibm_symb.txt -t $2 -o optimized_benchmarks -j "ibm"
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_normal_logs_"${1/.txt/}"_${2}_ibm.txt

while read c; do
  echo "Running QUESO on ${c}.qasm using Rigetti gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g rigetti -r rules_q3_s5_rigetti.txt -sr rules_q3_s3_rigetti_symb.txt -t $2 -o optimized_benchmarks -j "rigetti"
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_normal_logs_"${1/.txt/}"_${2}_rigetti.txt

while read c; do
  echo "Running QUESO with RZ objective function on ${c}.qasm using Ion gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ion -r rules_q3_s3_ion.txt -sr rules_q3_s3_ion_symb.txt -t $2 -o optimized_benchmarks -opt rz -j "ion"
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_normal_logs_"${1/.txt/}"_${2}_ion.txt

while read c; do
  echo "Running QUESO on ${c}.qasm using Ion gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ion -r rules_q3_s3_ion.txt -sr rules_q3_s3_ion_symb.txt -t $2 -o optimized_benchmarks -j "ionNormal"
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_normal_logs_"${1/.txt/}"_${2}_ion_normal.txt

# Run QUESO on result of Quartz preprocessing (QUESO-PP)
while read c; do
  echo "Running QUESO with Quartz preprocessing on ${c}.qasm using Nam gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/nam_afterQuartzPP_${c}.qasm -g nam -r rules_q3_s6_nam.txt -sr rules_q3_s3_nam_symb.txt -t $2 -o optimized_benchmarks
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_pp_logs_"${1/.txt/}"_${2}_nam.txt

while read c; do
  echo "Running QUESO with Quartz preprocessing on ${c}.qasm using IBM gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/ibm_afterQuartzPP_${c}.qasm -g ibm -r rules_q3_s4_ibm.txt -sr rules_q3_s3_ibm_symb.txt -t $2 -o optimized_benchmarks
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_pp_logs_"${1/.txt/}"_${2}_ibm.txt

while read c; do  
  echo "Running QUESO with Quartz preprocessing on ${c}.qasm using Rigetti gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/rigetti_afterQuartzPP_${c}.qasm -g rigetti -r rules_q3_s5_rigetti.txt -sr rules_q3_s3_rigetti_symb.txt -t $2 -o optimized_benchmarks
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_pp_logs_"${1/.txt/}"_${2}_rigetti.txt

# Run QUESO with different toggles (Fig 13)
while read c; do
  echo "Running QUESO without symbolic rules on ${c}.qasm using IBM gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ibm -r rules_q3_s4_ibm.txt -sr empty.txt -t $2 -o optimized_benchmarks -j "removeSymbolicRules" # remove symbolic rules
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_toggles_logs_"${1/.txt/}"_${2}_ibm_removeSymbolicRules.txt

while read c; do
  echo "Running QUESO without size-preserving rules on ${c}.qasm using IBM gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ibm -r rules_q3_s4_ibm.txt -sr rules_q3_s3_ibm_symb.txt -t $2 -o optimized_benchmarks -removeSizePreservingRules -j "removeSizePreserveRules" # remove size-preserving rules
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_toggles_logs_"${1/.txt/}"_${2}_ibm_removeSizePreserveRules.txt

while read c; do
  echo "Running QUESO without 3-qubit rules on ${c}.qasm using IBM gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ibm -r rules_q3_s4_ibm.txt -sr rules_q3_s3_ibm_symb.txt -t $2 -o optimized_benchmarks -mrq 2 -j "remove3QRules" # remove rules with 3 qubits
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_toggles_logs_"${1/.txt/}"_${2}_ibm_remove3QRules.txt

while read c; do
  echo "Running QUESO with size-preserving symbolic rules on ${c}.qasm using IBM gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ibm -r rules_q3_s4_ibm.txt -sr rules_q3_s3_ibm_symb.txt -t $2 -o optimized_benchmarks -useSizePreservingSymbRules -j "addSizePreserveSymbRules" # add size-preserving symbolic rules (Fig 21 appendix)
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_toggles_logs_"${1/.txt/}"_${2}_ibm_addSizePreserveSymbRules.txt


# Run QUESO with pruned rules (Fig 12)
# Get rules used
grep -i "applied to best circuit" /root/logs/queso_logs/queso_normal_logs_"${1/.txt/}"_${2}_ibm.txt > rules_applied_to_output.txt
python3 parse_rules_applied.py rules_applied_to_output.txt rules_q3_s4_ibm.txt > rules_q3_s4_ibm_pruned.txt
python3 parse_rules_applied.py rules_applied_to_output.txt rules_q3_s3_ibm_symb.txt > rules_q3_s3_ibm_symb_pruned.txt

while read c; do
  echo "Running QUESO with pruned rules on ${c}.qasm using IBM gate set..." >/dev/tty
  java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar Applier -c benchmarks/decomp0_${c}.qasm -g ibm -r rules_q3_s4_ibm_pruned.txt -sr rules_q3_s3_ibm_symb_pruned.txt -t $2 -o optimized_benchmarks -j "prunedRules"
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks QUESO tasks completed" >/dev/tty
done < /root/$1 &> /root/logs/queso_logs/queso_pruned_logs_"${1/.txt/}"_${2}_ibm.txt

grep -i "Final gate count" /root/logs/queso_logs/queso*logs*.txt > /root/logs/queso_gate_counts.txt
grep -i "time to best final" /root/logs/queso_logs/queso*logs*ibm.txt > /root/logs/queso_ibm_time_to_best.txt

mkdir -p /root/optimized_benchmarks/optimized_queso_ibm
mkdir -p /root/optimized_benchmarks/optimized_queso_rigetti
mkdir -p /root/optimized_benchmarks/optimized_queso_ion

mv /root/optimized_benchmarks/*ibm*decomp0* /root/optimized_benchmarks/optimized_queso_ibm
mv /root/optimized_benchmarks/*rigetti*decomp0* /root/optimized_benchmarks/optimized_queso_rigetti
mv /root/optimized_benchmarks/*ion*decomp0* /root/optimized_benchmarks/optimized_queso_ion

echo "Done running QUESO!"
