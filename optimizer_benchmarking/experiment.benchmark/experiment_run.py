import subprocess
import sys

argsfilename = sys.argv[1]

# read .args
with open(argsfilename) as args_file:
    # for each line of read input, run a subprocess
    for line in args_file:
        args = ["python3", "../run_optimiser.py"]
        args.extend(line.strip().split(" "))
        # print(f'args: {args}')
        subprocess.run(args)
