import warnings
warnings.filterwarnings("ignore")

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
from qiskit import QuantumCircuit

IBMNEW_DIR = "/Users/weiqiang/Downloads/qsymb_benchmarks/ibmnew"
BENCHMARK_FILE = "/Users/weiqiang/Downloads/qsymb_benchmark.txt"

# Load benchmark names
with open(BENCHMARK_FILE) as f:
    benchmarks = [line.strip() for line in f if line.strip()]

# Collect gate counts
total_gates = []
two_q_gates = []
names = []
missing = []

for cid in benchmarks:
    qasm_path = f"{IBMNEW_DIR}/{cid}.qasm"
    try:
        qc = QuantumCircuit.from_qasm_file(qasm_path)
        total_gates.append(qc.size())
        two_q_gates.append(qc.num_nonlocal_gates())
        names.append(cid)
    except Exception as e:
        missing.append((cid, str(e)))

if missing:
    print(f"Missing/failed ({len(missing)}):")
    for cid, err in missing:
        print(f"  {cid}: {err}")

print(f"Loaded {len(names)} benchmarks")
print(f"Total gates  — min:{min(total_gates)}, max:{max(total_gates)}, median:{int(np.median(total_gates))}")
print(f"2Q gates     — min:{min(two_q_gates)}, max:{max(two_q_gates)}, median:{int(np.median(two_q_gates))}")

# ── Shared style ──────────────────────────────────────────────────────────────
FONTSIZE_LABEL  = 22
FONTSIZE_TICK   = 16
FONTSIZE_TITLE  = 20
BAR_COLOR       = "#4C72B0"
EDGE_COLOR      = "white"

def plot_distribution(values, xlabel, filename, title):
    fig, ax = plt.subplots(figsize=(8, 5))

    # Use log-spaced bins to handle the wide spread of values
    log_min = np.floor(np.log10(max(min(values), 1)))
    log_max = np.ceil(np.log10(max(values)))
    bins = np.logspace(log_min, log_max, 25)

    ax.hist(values, bins=bins, color=BAR_COLOR, edgecolor=EDGE_COLOR, linewidth=0.6)
    ax.set_xscale("log")

    ax.set_xlabel(xlabel, fontsize=FONTSIZE_LABEL, fontweight="bold")
    ax.set_ylabel("Number of Benchmarks", fontsize=FONTSIZE_LABEL, fontweight="bold")
    ax.set_title(title, fontsize=FONTSIZE_TITLE, fontweight="bold")

    ax.tick_params(axis="both", labelsize=FONTSIZE_TICK)
    for label in ax.get_xticklabels() + ax.get_yticklabels():
        label.set_fontweight("bold")

    ax.grid(True, linestyle="--", alpha=0.5, axis="y")
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)

    # Annotate median
    med = np.median(values)
    ax.axvline(med, color="red", linestyle="--", linewidth=1.5)
    ax.text(med * 1.08, ax.get_ylim()[1] * 0.92,
            f"median={int(med)}", color="red",
            fontsize=14, fontweight="bold", va="top")

    plt.tight_layout()
    plt.savefig(filename, dpi=400, bbox_inches="tight")
    plt.close()
    print(f"Saved {filename}")


plot_distribution(
    total_gates,
    xlabel="Total Gate Count",
    filename="/Users/weiqiang/Downloads/benchmark_dist_total_gates.pdf",
    title="Benchmark Distribution — Total Gates"
)

plot_distribution(
    two_q_gates,
    xlabel="2-Qubit Gate Count",
    filename="/Users/weiqiang/Downloads/benchmark_dist_2q_gates.pdf",
    title="Benchmark Distribution — 2-Qubit Gates"
)
