#!/usr/bin/env python3
"""ILP circuit-compaction wrapper for SymbolicOptimizer.

Reuses Quasar's ``dag.linearized_circuit_from_dag`` -- the MinLA ILP that
re-linearizes a circuit DAG into a topological order minimizing dependency
stretch (this is the core of Quasar's ``_run_ilp``).

It reads an OpenQASM 2.0 file and writes a *permutation*: one input-gate
index per line, listed in compacted order. Emitting a permutation instead
of QASM keeps the Java caller's exact angle representation intact across
the round trip (no float re-serialization of gate parameters).

Usage: ilp_compact.py <in.qasm> <out.perm> [time_limit_sec]
"""
import os
import sys

# Quasar's seq-eg dir holds dag.py; override with QUASAR_SEQ_EG if relocated.
QUASAR_SEQ_EG = os.environ.get("QUASAR_SEQ_EG", "/root/Quasar/seq-eg")
sys.path.insert(0, QUASAR_SEQ_EG)

from qiskit import qasm2                       # noqa: E402
from qiskit.converters import circuit_to_dag   # noqa: E402
from dag import linearized_circuit_from_dag    # noqa: E402


def _signature(circuit, instruction):
    """Order-independent identity of a gate: (name, params, qubit indices)."""
    op = instruction.operation
    params = tuple(repr(p) for p in op.params)
    qubits = tuple(circuit.find_bit(q).index for q in instruction.qubits)
    return (op.name, params, qubits)


def main():
    if len(sys.argv) < 3:
        sys.stderr.write("usage: ilp_compact.py <in.qasm> <out.perm> [tl]\n")
        return 2
    infile, outfile = sys.argv[1], sys.argv[2]
    time_limit = int(sys.argv[3]) if len(sys.argv) > 3 else 10

    circ_in = qasm2.load(
        infile, custom_instructions=qasm2.LEGACY_CUSTOM_INSTRUCTIONS)
    dag_in = circuit_to_dag(circ_in)
    compact = linearized_circuit_from_dag(dag_in, ilp_time_limit_sec=time_limit)

    # Bucket input gate indices by signature, then pull them out in the
    # order the ILP produced. The ILP only reorders gates, so the two
    # multisets of signatures are identical; identical gates are
    # interchangeable, so first-come assignment is correct.
    buckets = {}
    for idx, instr in enumerate(circ_in.data):
        buckets.setdefault(_signature(circ_in, instr), []).append(idx)

    perm = []
    for instr in compact.data:
        perm.append(buckets[_signature(compact, instr)].pop(0))

    with open(outfile, "w") as fh:
        fh.write("\n".join(str(i) for i in perm))
        fh.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
