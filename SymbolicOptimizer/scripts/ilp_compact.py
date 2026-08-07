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

# dag.py (the MinLA ILP, originally Quasar's) is vendored alongside this
# script; override with QUASAR_SEQ_EG to point at an external copy instead.
QUASAR_SEQ_EG = os.environ.get("QUASAR_SEQ_EG", os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, QUASAR_SEQ_EG)

try:
    from qiskit import qasm2                   # noqa: E402  (terra >= 0.24)
except ImportError:
    qasm2 = None                               # terra <= 0.23: from_qasm_file
from qiskit import QuantumCircuit              # noqa: E402
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

    # Build the circuit by parsing the gate lines IN FILE ORDER and tag each
    # gate with its file-line index as a unique label.
    #
    # Why not qasm2.load / from_qasm_file: both qiskit loaders reorder gates
    # relative to the source text (circ.data order != file line order). The
    # Java caller (IlpCompactor) reorders its own FILE LINES by the permutation
    # we emit, so the permutation MUST be expressed in file-line index space.
    # Feeding a qiskit-loader ordering meant the permutation indexed a
    # different sequence than Java's lines, silently producing non-equivalent
    # circuits (worst on e-graph-folded circuits full of duplicate gates).
    import re as _re
    raw = open(infile).read()
    gate_lines = []
    nq = 0
    for ln in raw.splitlines():
        s = ln.strip()
        if not s or s.startswith(("OPENQASM", "include", "qreg", "creg", "//")):
            continue
        gate_lines.append(s)
        for qi_ in _re.findall(r"q\[(\d+)\]", s):
            nq = max(nq, int(qi_) + 1)

    tagged = QuantumCircuit(nq)
    for idx, s in enumerate(gate_lines):
        m = _re.match(r"([a-zA-Z_]\w*)\s*(?:\(([^)]*)\))?\s*(.+?);?$", s)
        if m is None:
            raise ValueError("unparseable gate line %d: %r" % (idx, s))
        name = m.group(1)
        angle = m.group(2)
        qubits = [int(x) for x in _re.findall(r"q\[(\d+)\]", m.group(3))]
        params = []
        if angle is not None and angle.strip() != "":
            # concrete numeric angles only (intermediate circuits are numeric)
            params = [float(eval(angle, {"__builtins__": {}}, {"pi": 3.141592653589793}))]
        method = getattr(tagged, name)
        inst = method(*params, *qubits)
        # tag the just-appended instruction with its file-line index
        tagged.data[-1].operation.label = "__idx%d__" % idx

    dag_in = circuit_to_dag(tagged)
    compact = linearized_circuit_from_dag(dag_in, ilp_time_limit_sec=time_limit)

    perm = []
    for instr in compact.data:
        lbl = instr.operation.label
        assert lbl is not None and lbl.startswith("__idx"), \
            "lost index label during linearization: %r" % lbl
        perm.append(int(lbl[5:-2]))

    assert sorted(perm) == list(range(len(gate_lines))), \
        "perm is not a bijection over input gates"

    with open(outfile, "w") as fh:
        fh.write("\n".join(str(i) for i in perm))
        fh.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
