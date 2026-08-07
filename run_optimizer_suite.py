#!/usr/bin/env python3

import os
import subprocess
import argparse
import concurrent.futures

# Default rule files per gateset, so the rule files don't have to be edited
# by hand for every run. Keys:
#   rule     -> concrete ruleset       (-r,  required)
#   longrule -> long ruleset           (-lr, optional; None to skip)
#   symbrule -> symbolic ruleset       (-sr, required)
#   monomial -> monomial ruleset       (-sm, optional; None to skip)
GATESET_RULE_FILES = {
    "nam": {
        "rule": "rules_nam_q3_5.txt",
        "longrule": "rules_q3_s6_nam.txt",
        "symbrule": "anchored_nam_q3.txt",
        "monomial": None,
    },
    "ibmnew": {
        "rule": "rules_ibmnew_q3_5.txt",
        "longrule": None,
        "symbrule": None,
        "monomial": None,
    },
    "ion": {
        "rule": "rules_ion_q3_4.txt",
        "longrule": "rules_q3_s3_ion.txt",
        "symbrule": "anchored_ion_q3_only.txt",
        "monomial": None,
    },
    "rigetti": {
        "rule": "rules_rigetti_q3_5.txt",
        "longrule": None,
        "symbrule": "anchored_rigetti_q3.txt",
        "monomial": None,
    },
}


def rule_files_for(gateset):
    """Return the default rule-file config for a gateset."""
    cfg = GATESET_RULE_FILES.get(gateset)
    if cfg is None:
        raise SystemExit(
            f"No default rule files configured for gateset '{gateset}'. "
            f"Add an entry to GATESET_RULE_FILES."
        )
    return cfg

SMALL_BENCHMARK_FILES = [
    "guoq_benchmarks/ibmnew/4gt11_84.qasm",
    "guoq_benchmarks/ibmnew/4gt11_83.qasm",
    "guoq_benchmarks/ibmnew/rd32-v0_66.qasm",
]

# The directory where the optimizer jar is located
OPTIMIZER_JAR_PATH = "SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar"

# The directory to store the logs. Absolute so it never resolves against a
# transient CWD (a relative path here once caused logs to be written into a
# sandbox temp dir that was later deleted, losing the whole run's output).
LOG_DIR = "/root/optimizer_logs"

# Each gateset reads its benchmarks from a different directory under
# guoq_benchmarks/. Gatesets not listed here fall back to a directory of
# the same name (e.g. "ibm" -> guoq_benchmarks/ibm/).
GATESET_BENCHMARK_DIRS = {
    "nam": "guoq_benchmarks/nam_rz/",
    "ibm": "guoq_benchmarks/ibm/",
    "ibmnew": "guoq_benchmarks/ibmnew/",
    "ion": "guoq_benchmarks/ion/",
}


def benchmark_dir_for(gateset):
    """Return the benchmark directory for a gateset, defaulting to a
    same-named directory under guoq_benchmarks/ when not explicitly mapped."""
    return GATESET_BENCHMARK_DIRS.get(gateset, f"guoq_benchmarks/{gateset}/")

def run_optimizer(benchmark_file, rule_file, symbolic_rule_file, log_file, mode, timeout, usesymb, symbolic_gate_size=None, gateset=None, monomial_rule_file=None, longrules_file=None, ilp=False):
    """Runs the optimizer with the given parameters and logs the output."""
    command = [
        "java",
        "--enable-preview",
        "-Xss256m",
        "-Xmx32g",
        "-cp",
        OPTIMIZER_JAR_PATH,
        "Optimizer",
        "-b",
        benchmark_file,
        "-r",
        rule_file,
        "-m",
        mode,
        "-t",
        str(timeout),
        "-symb",
        'true' if usesymb else 'false',
        "-g",
        gateset,
        "-ilp",
        'true' if ilp else 'false',
    ]
    if symbolic_rule_file:
        command.extend(["-sr", symbolic_rule_file])
    if longrules_file:
        command.extend(["-lr", longrules_file])
    if usesymb:
        command.extend(["-minsymb", str(symbolic_gate_size[0])])
        command.extend(["-maxsymb", str(symbolic_gate_size[1])])

    if monomial_rule_file:
        command.extend(["-sm", monomial_rule_file])

    print(f"Running command: {' '.join(command)}")
    print(f"Running optimizer for {benchmark_file} with {rule_file}...")
    with open(log_file, "w") as f:
        subprocess.run(command, stdout=f, stderr=subprocess.STDOUT)


# [10, 40], [10, 50], [20, 40], [30,50], [10, 500]
symbolic_gate_size_list = [[1, 20]]

def main():
    """Main function to run the optimizer test suite."""
    parser = argparse.ArgumentParser(description="Run the optimizer test suite.")
    parser.add_argument("-b", "--benchmark", help="Path to the file containing the list of benchmarks.", required=False)
    parser.add_argument("-m", "--mode", help="Mode to run the optimizer in.")
    parser.add_argument("-t", "--timeout", help="Timeout for the optimizer.")
    parser.add_argument("-g", "--gateset", help="Gateset to use.")
    parser.add_argument("-symb", "--usesymb", help="Use symbolic rules.")
    parser.add_argument("-ilp", "--useilp", help="Compact the circuit with ILP each iteration ('true'/'false').")
    parser.add_argument("-only_s", "--onlysymb", help="Only run concrete + symbolic rules.")
    parser.add_argument("-n", "--threads", help="Number of threads to use.", default=1, type=int)
    args = parser.parse_args()

    # Create the log directory if it doesn't exist
    if not os.path.exists(LOG_DIR):
        os.makedirs(LOG_DIR)

    # Read the list of benchmarks. The directory is chosen by gateset, so
    # e.g. -g nam reads from guoq_benchmarks/nam_rz/.
    if args.benchmark:
        bench_dir = benchmark_dir_for(args.gateset)
        with open(args.benchmark, "r") as f:
            benchmarks = [bench_dir + line.strip() + ".qasm" for line in f if line.strip()]
    else:
        benchmarks = SMALL_BENCHMARK_FILES

    # Rule files are chosen by gateset (see GATESET_RULE_FILES) so they don't
    # need to be edited by hand between runs.
    rule_cfg = rule_files_for(args.gateset)
    rule_file = rule_cfg["rule"]
    longrules_file = rule_cfg.get("longrule")
    symbolic_rule_file = rule_cfg["symbrule"]
    monomial_rule_file = rule_cfg.get("monomial")

    tasks = []
    # Run the optimizer for each benchmark
    for benchmark in benchmarks:
        use_ilp = str(args.useilp).lower() == "true"
        ilp_tag = "ilpon" if use_ilp else "ilpoff"
        if args.usesymb:
            for gate_size in symbolic_gate_size_list:
                log_file_name_usesymb = f"{os.path.basename(benchmark)}_{os.path.basename(rule_file)}_{args.mode}_{os.path.basename(symbolic_rule_file)}_usesymbnm_{gate_size}_{ilp_tag}.log"
                log_file_path_usesymb = os.path.join(LOG_DIR, log_file_name_usesymb)
                tasks.append((benchmark, rule_file, symbolic_rule_file, log_file_path_usesymb, args.mode, int(args.timeout), True, gate_size, args.gateset, monomial_rule_file, longrules_file, use_ilp))
        elif not args.onlysymb:
            log_file_name_nosymb = f"{os.path.basename(benchmark)}_{os.path.basename(rule_file)}_{args.mode}_nosymb_{ilp_tag}.log"
            log_file_path_nosymb = os.path.join(LOG_DIR, log_file_name_nosymb)
            tasks.append((benchmark, rule_file, symbolic_rule_file, log_file_path_nosymb, args.mode, int(args.timeout), False, None, args.gateset, monomial_rule_file, longrules_file, use_ilp))

    with concurrent.futures.ThreadPoolExecutor(max_workers=args.threads) as executor:
        futures = [executor.submit(run_optimizer, *task) for task in tasks]
        for future in concurrent.futures.as_completed(futures):
            try:
                future.result()
            except Exception as e:
                print(f"Task failed: {e}")

if __name__ == "__main__":
    main()
