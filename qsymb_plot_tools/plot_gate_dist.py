import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Benchmark list
with open("benchmark.txt") as f:
    wanted = [l.strip() for l in f if l.strip()]

# Gate sizes from nam_results, qubit counts parsed from qasm
df = pd.read_csv("nam_results_135.csv")
sub = df[df["benchmark"].isin(wanted)].copy()
sizes = sub["original_size"].values

qb = pd.read_csv("qubit_counts.csv")
qb = qb[qb["benchmark"].isin(wanted)]
qubits = qb["qubits"].values

# ---- Figure 1: qubit number distribution ----
fig1, ax1 = plt.subplots(figsize=(7, 5))
bins_q = np.arange(qubits.min(), qubits.max() + 2) - 0.5
ax1.hist(qubits, bins=bins_q, color="#4C72B0", edgecolor="white")
ax1.set_xlabel("Number of qubits")
ax1.set_ylabel("Number of benchmarks")
ax1.grid(axis="y", alpha=0.3)
fig1.tight_layout()
fig1.savefig("benchmark_qubit_distribution.png", dpi=150, bbox_inches="tight")
print("saved benchmark_qubit_distribution.png")

# ---- Figure 2: gate size distribution (log bins) ----
fig2, ax2 = plt.subplots(figsize=(7, 5))
logbins = np.logspace(np.log10(sizes.min()), np.log10(sizes.max()), 25)
ax2.hist(sizes, bins=logbins, color="#C44E52", edgecolor="white")
ax2.set_xscale("log")
ax2.set_xlabel("Gate count (original_size)")
ax2.set_ylabel("Number of benchmarks")
ax2.grid(axis="y", alpha=0.3)
fig2.tight_layout()
fig2.savefig("benchmark_gate_size_distribution.png", dpi=150, bbox_inches="tight")
print("saved benchmark_gate_size_distribution.png")
