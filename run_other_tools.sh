#!/bin/bash

cd optimizer_benchmarking
cd experiment.benchmark

qvm -S &> /dev/null &
QVM_PID=$!

quilc -S &> /dev/null &
QUILC_PID=$!

echo "Running manual optimizers on IBM gate set..."
python3 experiment_run.py ibm.args &> /root/logs/other_tools_ibm.txt
echo "Running manual optimizers on Nam gate set..."
python3 experiment_run.py nam.args &> /root/logs/other_tools_nam.txt
echo "Running manual optimizers on Rigetti gate set..."
python3 experiment_run.py rigetti.args &> /root/logs/other_tools_rigetti.txt
echo "Running manual optimizers on Ion gate set..."
python3 experiment_run.py ion.args &> /root/logs/other_tools_ion.txt

kill -9 $QVM_PID $QUILC_PID

mkdir -p /root/optimized_benchmarks/optimized_tket_ibm
mkdir -p /root/optimized_benchmarks/optimized_qiskit_ibm
mkdir -p /root/optimized_benchmarks/optimized_voqc_ibm
mkdir -p /root/optimized_benchmarks/optimized_tket_rigetti
mkdir -p /root/optimized_benchmarks/optimized_quilc_rigetti
mkdir -p /root/optimized_benchmarks/optimized_qiskit_ion

for dir in results.ibm*; do
  mv $dir/*tket* /root/optimized_benchmarks/optimized_tket_ibm
  mv $dir/*VOQC* /root/optimized_benchmarks/optimized_voqc_ibm
  mv $dir/*Qiskit* /root/optimized_benchmarks/optimized_qiskit_ibm
done

for dir in results.rigetti*; do
  mv $dir/*tket* /root/optimized_benchmarks/optimized_tket_rigetti
  mv $dir/*quilc* /root/optimized_benchmarks/optimized_quilc_rigetti
done

for dir in results.ion*; do
  mv $dir/*Qiskit* /root/optimized_benchmarks/optimized_qiskit_ion
done

cd /root

echo "Done running manual optimizers!"
