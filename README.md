# QSymb

**Synthesis of Compact and Expressive Quantum-Circuit Optimizations.**

Quantum devices are noisy, so reducing circuit size is critical. Existing rule-based
optimizers rely on large, hard-to-manage rule sets yet still miss useful transformations.
Given a quantum architecture, **QSymb automatically synthesizes a small but expressive set
of rewrite rules with formal guarantees** — and a simple optimizer using them outperforms
prior tools that depend on larger, less structured rule sets.

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

## Reproducing the paper
See **[Artifact_README.md](Artifact_README.md)** for full build and experiment instructions
(rule synthesis & cost — Tables 1–3; optimization performance — Figs. 11–12; ablations — Figs. 13–14).
