#!/bin/bash

echo "Running QUESO rule generation..."
./gen_small_rules.sh
./gen_rules.sh

echo "Running optimizers using manually-derived optimizations..."

./run_other_tools.sh

echo "Running all configurations of Quartz and QUESO on one benchmark with a 3 second timeout..."

./run_queso_quartz.sh one_benchmark.txt 3

echo "Done with getting started!"
