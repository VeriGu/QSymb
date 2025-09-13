#!/bin/bash

cd /root
source run_queso.sh $1 $2
source run_quartz.sh $1 $2

echo "Generating plots..."
python3 gen_plots.py $1 $2
echo "Plots generated!"
