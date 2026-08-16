# QSymb

**Synthesis of Compact and Expressive Quantum-Circuit Optimizations.**

A state-of-the-art rule synthesizer and optimizer for quantum circuit optimization. Given a gate set, QSymb automatically synthesizes both concrete rules and symbolic rules, and QSymb-Optimizer uses them as input to perform optimization. On the IBM-Eagle gate set, Qsymb strictly outperforms state-of-the-art rewrite-based optimizers (Qiskit, Guoq-Rewrite, Quartz, TKET, and Queso) in two-qubit-gate reduction on 90%, 67%, 82%, 85%, and 83% of standard quantum algorithm benchmarks, respectively; on Nam gate set, the corresponding rates are 88%, 74%, 81%, 86%, and 82.9%. It
achieves final average two-qubit-gate reductions of 27.44% and 29.95%.

## Key ideas
- **Symbolic rewrite rules** — a symbolic gate stands for infinitely many subcircuits; a
  symbolic-matrix constraint characterizes exactly when two fragments are equivalent.
- **Canonical symbolic rules** (`L; S = S; R`) — a compact generative core from which
  general symbolic rules are derived.
- **Rule anchoring** — turns the canonical core into optimization-effective rules.
- **Guarantees** — soundness (validation), non-derivability, and bounded completeness.

QSymb synthesizes (1) a small, non-derivable **concrete** rule set complete up to chosen
size/qubit bounds, and (2) a small, expressive **canonical symbolic** rule set.

## Layout
| Path | Contents |
|------|----------|
| `SymbolicOptimizer/` | rule synthesis + optimizer (Java) |
| `qsymb_benchmarks/`, `benchmark.txt` | benchmark circuits and the 135-circuit suite |
| `*.sh` | build, synthesis, optimization, ablation scripts |
| `qsymb_plot_tools/`, `paper_results/` | figure generation and precomputed results |

## Prerequisites
**Native toolchain** (for building outside the image):

- JDK 17 (built/run with `--enable-preview`)
- Maven >= 3.6 (Java libraries — antlr4, jgrapht, guava, gson, commons-*, opencsv, lombok — are fetched automatically)
- `egglog-experimental` 1.0.0 (binary on `PATH`)
- Python 3.10 with the packages in `requirements.txt`:

```bash
pip install -r requirements.txt
```

## Reproducing the paper
See **[Artifact_README.md](Artifact_README.md)** for full build and experiment instructions
(rule synthesis & cost — Tables 1–3; optimization performance — Figs. 11–12; ablations — Figs. 13–14).
