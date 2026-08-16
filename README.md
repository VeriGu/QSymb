# QSymb

**Synthesis of Compact and Expressive Quantum-Circuit Optimizations.**

A state-of-the-art rule synthesizer and optimizer for quantum circuit optimization. QSymb automatically synthesizes both concrete rules and symbolic rules and QSymb-Optimizer use them as input to perform optimization.

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

**Recommended — prebuilt Docker image** (everything preinstalled):
```bash
docker pull xiaoqianghweye/qsymb:oopsla26
```
See **[Artifact_README.md](Artifact_README.md)** for running the image.

**Native toolchain** (for building outside the image):

- JDK 17 (built/run with `--enable-preview`)
- Maven >= 3.6 (Java libraries — antlr4, jgrapht, guava, gson, commons-*, opencsv, lombok — are fetched automatically)
- `egglog-experimental` 1.0.0 (binary on `PATH`)

## Reproducing the paper
See **[Artifact_README.md](Artifact_README.md)** for full build and experiment instructions
(rule synthesis & cost — Tables 1–3; optimization performance — Figs. 11–12; ablations — Figs. 13–14).
