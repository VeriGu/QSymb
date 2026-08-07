import os
import re
import csv
import warnings
warnings.filterwarnings("ignore")

from qiskit import QuantumCircuit

FIDELITIES = {
    # --- IBM-Eagle (Qiskit Washington calibration data) ---
    "rz": 1,
    "cx": 0.9885404797320638,
    "sx": 0.9997188818297166,
    "x":  0.9997188818297166,

    # --- Nam / general-purpose decomposition gates ---
    "u1": 1,
    "u2": 0.9997188818297166,
    "u3": 0.9997188818297166,

    # --- IonQ Forte (published spec: 1Q error 0.02%, 2Q error 0.4%) ---
    #     Source: https://ionq.com/quantum-systems/forte
    "rx":  0.9998,
    "ry":  0.9998,
    "rxx": 0.996,

    # --- Rigetti Aspen-M-3 (published median: 1Q 99.7%, CZ 93.3%) ---
    #     Source: Rigetti Aspen-M commercial-availability announcement (2022)
    "rx1": 0.997,
    "rx2": 0.997,
    "rx3": 0.997,
    "cz":  0.933,
}

# Gates that carry no error weight (structural / classical): fidelity 1.
_ZERO_WEIGHT = {"measure", "barrier", "reset"}


def _print_gateset_fidelities(name, gates):
    print(f"{name} gate fidelities (scientific notation):")
    for g in gates:
        fid = FIDELITIES[g]
        print(f"  {g:<4}: fidelity={fid:.6e}  error_rate={1 - fid:.6e}")
    print()


def get_fidelity_circuit(circuit):
    """Product of per-gate fidelities over the circuit's gate counts.

    Unknown structural gates (measure/barrier/reset) are treated as lossless;
    any other unrecognized gate raises so a mis-parsed gateset isn't silently
    scored as perfect.
    """
    fidelity = 1
    gates = circuit.count_ops()
    for gate, count in gates.items():
        if gate in _ZERO_WEIGHT:
            continue
        if gate not in FIDELITIES:
            raise KeyError(f"no fidelity for gate '{gate}' (ops: {dict(gates)})")
        fidelity *= FIDELITIES[gate] ** count
    return fidelity


def _circuit_from_qasm_text(text):
    """Parse OPENQASM text, synthesizing a header if the file is a bare gate
    list (the format Optimizer writes to <bench>_optimized.qasm).

    Registers are not always named `q` (benchmarks use node[...], psi[...],
    reg[...], eval[...], ...), so declare a qreg for EVERY register name
    referenced in the gate list, each sized to its max index + 1."""
    if "OPENQASM" in text:
        return QuantumCircuit.from_qasm_str(text)
    regs = {}
    for name, idx in re.findall(r'([A-Za-z_][A-Za-z0-9_]*)\[(\d+)\]', text):
        idx = int(idx)
        if idx >= regs.get(name, 0):
            regs[name] = idx + 1
    if not regs:
        regs = {'q': 1}
    header = 'OPENQASM 2.0;\ninclude "qelib1.inc";\n'
    header += ''.join(f'qreg {name}[{size}];\n' for name, size in sorted(regs.items()))
    return QuantumCircuit.from_qasm_str(header + text)


def fidelity_of_qasm_file(path):
    """Load an OPENQASM file (with or without a header) and return its
    circuit fidelity."""
    with open(path) as f:
        return get_fidelity_circuit(_circuit_from_qasm_text(f.read()))


def _run_mac_batch():
    """Original hard-coded cross-tool batch (local Mac layout).

    Only runs when this file is executed as a script WITHOUT --qasm, and only
    where the hard-coded input paths exist. Left intact for reproducing the
    paper's cross-tool fidelity CSV.
    """
    _print_gateset_fidelities("IBM-Eagle",         ["cx", "rz", "x", "sx"])
    _print_gateset_fidelities("Rigetti Aspen-M-3", ["cz", "rz", "rx1", "rx2", "rx3"])
    _print_gateset_fidelities("IonQ Forte",        ["rxx", "rx", "ry", "rz"])

    # Load 135 circuit IDs
    with open("/Users/weiqiang/Downloads/complete_numeric_circuit_ids.txt") as f:
        target_circuits = set(line.strip() for line in f if line.strip())

    BASE = "/Users/weiqiang/Downloads/benchmark_qt"
    QUARTZ_DIR = f"{BASE}/quartz_depth_1hr"
    QUESO_DIR  = f"{BASE}/queso_1hr/queso"
    TKET_DIR   = "/Users/weiqiang/Downloads/tket"
    GUOQ_DIR   = "/Users/weiqiang/Downloads/guoq_results"
    QSYMB_DIR  = "/Users/weiqiang/Downloads/new_log"
    QISKIT_DIR = "/Users/weiqiang/Downloads/qiskit"

    results = []

    for cid in sorted(target_circuits):
        row = {"circuit_id": cid}

        # --- Quartz: pick last file by step number ---
        cid_dir = os.path.join(QUARTZ_DIR, cid)
        candidate_dirs = [
            os.path.join(cid_dir, "quartz_out", cid),  # layout 1
            os.path.join(cid_dir, "quartz_out"),         # layout 2
            cid_dir,                                      # layout 3
        ]
        quartz_fidelity = None
        found_dir = None
        for d in candidate_dirs:
            if os.path.isdir(d):
                qasm_files = [f for f in os.listdir(d) if f.endswith(".qasm")]
                if qasm_files:
                    found_dir = d
                    break
        if found_dir:
            try:
                qasm_files.sort(key=lambda f: int(f.split("_")[0]))
            except (ValueError, IndexError):
                pass
            best_file = os.path.join(found_dir, qasm_files[-1])
            try:
                qc = QuantumCircuit.from_qasm_file(best_file)
                quartz_fidelity = get_fidelity_circuit(qc)
            except Exception as e:
                print(f"  [quartz] {cid}: ERROR {e}")
        else:
            print(f"  [quartz] {cid}: no qasm files found in any expected location")
        row["quartz_fidelity"] = quartz_fidelity

        # --- Queso ---
        queso_fidelity = None
        queso_candidates = [
            os.path.join(QUESO_DIR, cid, f"results_{cid}", f"latest_sol_none_queso_{cid}.qasm"),
            os.path.join(QUESO_DIR, cid, f"latest_sol_none_none_{cid}.qasm"),
        ]
        queso_file = next((p for p in queso_candidates if os.path.exists(p)), None)
        if queso_file:
            try:
                qc = QuantumCircuit.from_qasm_file(queso_file)
                queso_fidelity = get_fidelity_circuit(qc)
            except Exception as e:
                print(f"  [queso] {cid}: ERROR {e}")
        else:
            print(f"  [queso] {cid}: file not found")
        row["queso_fidelity"] = queso_fidelity

        # --- TKET ---
        tket_file = os.path.join(TKET_DIR, f"results_{cid}", f"optimized_none_none_{cid}.qasm")
        tket_fidelity = None
        if os.path.exists(tket_file):
            try:
                qc = QuantumCircuit.from_qasm_file(tket_file)
                tket_fidelity = get_fidelity_circuit(qc)
            except Exception as e:
                print(f"  [tket] {cid}: ERROR {e}")
        else:
            print(f"  [tket] {cid}: file not found")
        row["tket_fidelity"] = tket_fidelity

        # --- GUOQ ---
        guoq_file = os.path.join(GUOQ_DIR, f"results_{cid}", f"latest_sol_none_1_{cid}.qasm")
        guoq_fidelity = None
        if os.path.exists(guoq_file):
            try:
                qc = QuantumCircuit.from_qasm_file(guoq_file)
                guoq_fidelity = get_fidelity_circuit(qc)
            except Exception as e:
                print(f"  [guoq] {cid}: ERROR {e}")
        else:
            print(f"  [guoq] {cid}: file not found")
        row["guoq_fidelity"] = guoq_fidelity

        # --- QSYMB: parse final circuit from log file ---
        qsymb_fidelity = None
        log_files = [f for f in os.listdir(QSYMB_DIR) if f.startswith(cid + ".qasm")]
        if log_files:
            log_path = os.path.join(QSYMB_DIR, log_files[0])
            try:
                log_text = open(log_path).read()
                idx = log_text.rfind("Final Circuit:")
                if idx != -1:
                    circuit_text = log_text[idx + len("Final Circuit:"):].strip()
                    qubits = set(int(m) for m in re.findall(r'q\[(\d+)\]', circuit_text))
                    n_qubits = max(qubits) + 1
                    qasm = f'OPENQASM 2.0;\ninclude "qelib1.inc";\nqreg q[{n_qubits}];\n' + circuit_text
                    qc = QuantumCircuit.from_qasm_str(qasm)
                    qsymb_fidelity = get_fidelity_circuit(qc)
                else:
                    print(f"  [qsymb] {cid}: 'Final Circuit:' not found in log")
            except Exception as e:
                print(f"  [qsymb] {cid}: ERROR {e}")
        else:
            print(f"  [qsymb] {cid}: log file not found")
        row["qsymb_fidelity"] = qsymb_fidelity

        # --- Qiskit ---
        qiskit_file = os.path.join(QISKIT_DIR, f"results_{cid}", f"optimized_none_none_{cid}.qasm")
        qiskit_fidelity = None
        if os.path.exists(qiskit_file):
            try:
                qc = QuantumCircuit.from_qasm_file(qiskit_file)
                qiskit_fidelity = get_fidelity_circuit(qc)
            except Exception as e:
                print(f"  [qiskit] {cid}: ERROR {e}")
        else:
            print(f"  [qiskit] {cid}: file not found")
        row["qiskit_fidelity"] = qiskit_fidelity

        results.append(row)
        q1 = f"{quartz_fidelity:.6f}" if quartz_fidelity is not None else "N/A"
        q2 = f"{queso_fidelity:.6f}" if queso_fidelity is not None else "N/A"
        q3 = f"{tket_fidelity:.6f}" if tket_fidelity is not None else "N/A"
        q4 = f"{guoq_fidelity:.6f}" if guoq_fidelity is not None else "N/A"
        q5 = f"{qsymb_fidelity:.6f}" if qsymb_fidelity is not None else "N/A"
        q6 = f"{qiskit_fidelity:.6f}" if qiskit_fidelity is not None else "N/A"
        print(f"{cid}: quartz={q1}, queso={q2}, tket={q3}, guoq={q4}, qsymb={q5}, qiskit={q6}")

    out_path = "/Users/weiqiang/Downloads/benchmark_fidelity.csv"
    with open(out_path, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["circuit_id", "quartz_fidelity", "queso_fidelity", "tket_fidelity", "guoq_fidelity", "qsymb_fidelity", "qiskit_fidelity"])
        writer.writeheader()
        writer.writerows(results)

    print(f"\nSaved {len(results)} rows to {out_path}")


if __name__ == "__main__":
    import sys
    import argparse

    ap = argparse.ArgumentParser(description="Circuit fidelity from per-gate error rates.")
    ap.add_argument("--qasm", help="compute fidelity of a single OPENQASM file and print it")
    args, _ = ap.parse_known_args()

    if args.qasm:
        # Single-file mode: emit one parseable line, nothing else on stdout.
        print(f"FIDELITY {fidelity_of_qasm_file(args.qasm):.10f}")
        sys.exit(0)

    # No --qasm: run the original hard-coded cross-tool batch.
    _run_mac_batch()
