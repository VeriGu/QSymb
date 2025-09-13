#!/bin/bash

cp /H_CZ_2_2_complete_ECC_set_modified.json /quartz-artifact/H_CZ_2_2_complete_ECC_set_modified.json
cd /quartz-artifact/build

rm -rf /optimized_quartz_${2}
mkdir /optimized_quartz_${2}

mkdir -p /root/logs/optimized_quartz_${2}

echo "Building Quartz..."
make test_nam
make test_ibmq
make test_rigetti

numBenchmarks=`cat /root/$1 | wc -l`
totalTasks=$((numBenchmarks * 6))
tasksCompleted=0

# Run Quartz with preprocessing
while read c; do
  echo "Running Quartz with preprocessing on ${c}.qasm using Nam gate set..." 
  ./test_nam ../circuit/nam-benchmarks/${c}.qasm --output /optimized_quartz_${2}/${c}.qasm.output.nam ../Nam_6_3_complete_ECC_set.json --timeout $2
  cp ../Nam_6_3_${c}.log /root/logs/optimized_quartz_${2}/Nam_6_3_${c}.log
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks Quartz tasks completed"

  echo "Running Quartz with preprocessing on ${c}.qasm using Rigetti gate set..." 
  ./test_rigetti ../circuit/nam-benchmarks/${c}.qasm --output /optimized_quartz_${2}/${c}.qasm.output.rigetti ../Rigetti_3_3_complete_ECC_set.json --timeout $2
  cp ../Rigetti_3_3_${c}.log /root/logs/optimized_quartz_${2}/Rigetti_3_3_${c}.log
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks Quartz tasks completed"

  echo "Running Quartz with preprocessing on ${c}.qasm using IBM gate set..." 
  ./test_ibmq ../circuit/nam-benchmarks/${c}.qasm --output /optimized_quartz_${2}/${c}.qasm.output.ibmq ../IBM_4_3_complete_ECC_set.json --timeout $2
  cp ../IBM_4_3_${c}.log /root/logs/optimized_quartz_${2}/IBM_4_3_${c}.log
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks Quartz tasks completed"
done < /root/$1

# Run Quartz without preprocessing (Quartz-NoPP)
cp /H_CZ_2_2_complete_ECC_set_modified_empty.json /quartz-artifact/H_CZ_2_2_complete_ECC_set_modified.json
while read c; do
  echo "Running Quartz without preprocessing on ${c}.qasm using Nam gate set..." 
  ./test_nam ../circuit/nam-benchmarks/decomp0_${c}.qasm --output /optimized_quartz_${2}/decomp0_${c}.qasm.output.nam ../Nam_6_3_complete_ECC_set.json --timeout $2
  cp ../Nam_6_3_decomp0_${c}.log /root/logs/optimized_quartz_${2}/Nam_6_3_decomp0_${c}.log
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks Quartz tasks completed"

  echo "Running Quartz without preprocessing on ${c}.qasm using Rigetti gate set..."
  ./test_rigetti ../circuit/nam-benchmarks/decomp0_${c}.qasm --output /optimized_quartz_${2}/decomp0_${c}.qasm.output.rigetti ../Rigetti_3_3_complete_ECC_set.json --timeout $2
  cp ../Rigetti_3_3_decomp0_${c}.log /root/logs/optimized_quartz_${2}/Rigetti_3_3_decomp0_${c}.log
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks Quartz tasks completed"

  echo "Running Quartz without preprocessing on ${c}.qasm using IBM gate set..."
  ./test_ibmq ../circuit/nam-benchmarks/decomp0_${c}.qasm --output /optimized_quartz_${2}/decomp0_${c}.qasm.output.ibmq ../IBM_4_3_complete_ECC_set.json --timeout $2
  cp ../IBM_4_3_decomp0_${c}.log /root/logs/optimized_quartz_${2}/IBM_4_3_decomp0_${c}.log
  tasksCompleted=$((tasksCompleted+1))
  echo "$tasksCompleted out of $totalTasks Quartz tasks completed"
done < /root/$1

mv /optimized_quartz_${2} /root/optimized_benchmarks/optimized_quartz_${2}

cd /root/logs/optimized_quartz_${2}
grep -i "Timeout" *.log > /root/logs/quartz_total_gate_counts.txt

for file in /root/optimized_benchmarks/optimized_quartz_${2}/*; do
  echo -n "$file " && grep -iE "cx|cz" $file | wc -l
done > /root/logs/quartz_2q_gate_counts.txt

cd /root

echo "Done running Quartz!"
