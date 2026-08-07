#!/usr/bin/env python3
"""Unit tests for the exact lifted-basis check in semantics.is_subspace_linear_combination.

Cases:
  Synthetic basis = commutant of CX(q0 ctrl, q1 tgt), computed numerically.
    1. S = rz(0.3) q0 (control, diagonal)            -> True
    2. S = rz(0.3) q1 (target)                        -> False
    3. S = cx q0,q2 (couples external, lifted-valid)  -> True   <- key user requirement
    4. S = cx q1,q2 (couples external, INVALID; the old projection
       turned this into identity => false positive)   -> False
  Real anchored-rule basis (anchored_nam_q3.txt line 8, cx-sandwich rule):
    5. S = cx q0,q1; cx q1,q0 (known-good window)     -> True
    6. S = cx q0,q1 alone                             -> False
    7. S = cx q0,q1; cx q1,q0; cx q0,q2 (ext-coupled, valid) -> True
  CLI path (same invocation Java uses):
    8. case 3 via subprocess                          -> output contains True
    9. case 4 via subprocess                          -> output does not contain True
"""
import json
import re
import subprocess
import sys

import numpy as np

sys.path.insert(0, '/root')
import semantics


def commutant_basis_sparse(L, tol=1e-9):
    """Sparse-JSON basis of {S : L S = S L} (row-major vec)."""
    d = L.shape[0]
    K = np.kron(L, np.eye(d)) - np.kron(np.eye(d), L.T)
    _, s, Vh = np.linalg.svd(K)
    null_rows = Vh[s < tol].conj() if len(s) == d * d else None
    assert null_rows is not None and len(null_rows) > 0
    mats = []
    for v in null_rows:
        B = v.reshape(d, d)
        # sanity: really commutes
        assert np.max(np.abs(L @ B - B @ L)) < 1e-8
        entries = []
        for i in range(d):
            for j in range(d):
                z = B[i, j]
                if abs(z) > 1e-12:
                    entries.append({"row": i, "col": j,
                                    "value": f"({z.real})+({z.imag})*I"})
        mats.append({"rows": d, "cols": d, "entries": entries})
    return json.dumps(mats), len(mats)


def circ(n, gates):
    return json.dumps({"n_qubits": n, "gates": gates})


CX01 = np.array([[1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 0, 1], [0, 0, 1, 0]], dtype=complex)


def load_rule_basis(path, lhs_prefix):
    """Parse the Matrix(...) basis of the first rule whose LHS starts with lhs_prefix."""
    for line in open(path):
        p = line.rstrip("\n").split("|")
        if len(p) < 4 or not p[0].startswith(lhs_prefix):
            continue
        body = p[3].strip()[1:-1]  # strip [ ]
        mats = []
        for mm in body.split("::"):
            inner = mm[len("Matrix("):-1]
            parts = inner.split(";")
            rows, cols = int(parts[0]), int(parts[1])
            entries = []
            for e in parts[2:]:
                r, c, val = e[1:-1].split(",", 2)
                val = val.strip()
                if val not in ("0", "0.0"):
                    entries.append({"row": int(r), "col": int(c), "value": val})
            mats.append({"rows": rows, "cols": cols, "entries": entries})
        return json.dumps(mats)
    raise SystemExit(f"rule not found: {lhs_prefix}")


def run(name, expect, circuit_json, basis_json, symbol_map=None):
    import io, contextlib
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        got = semantics.is_subspace_linear_combination(circuit_json, basis_json, "[0,1]", symbol_map)
    ok = got == expect
    print(f"{'PASS' if ok else 'FAIL'}  {name}: expected {expect}, got {got}")
    return ok


def run_cli(name, expect_true, circuit_json, basis_json):
    out = subprocess.run(
        ["python3", "semantics.py", "-is_subspace_linear", circuit_json, basis_json, "[0,1]", "{}"],
        capture_output=True, text=True, cwd="/root")
    got = "True" in out.stdout
    ok = got == expect_true and out.returncode == 0
    print(f"{'PASS' if ok else 'FAIL'}  {name} (CLI): expected {expect_true}, got {got}, rc={out.returncode}")
    if not ok:
        print(out.stdout[-2000:], out.stderr[-2000:])
    return ok


def main():
    results = []
    basis_json, k = commutant_basis_sparse(CX01)
    print(f"CX(0,1) commutant basis dim = {k} (expect 10: eigmult 3,1 -> 9+1)")

    results.append(run("1 rz@control in commutant", True,
                       circ(2, [{"gate": "rz", "targets": [0], "params": {"gamma": 0.3}}]),
                       basis_json))
    results.append(run("2 rz@target not in commutant", False,
                       circ(2, [{"gate": "rz", "targets": [1], "params": {"gamma": 0.3}}]),
                       basis_json))
    results.append(run("3 cx q0,qext coupled-but-valid", True,
                       circ(3, [{"gate": "cx", "targets": [0, 2]}]),
                       basis_json))
    results.append(run("4 cx q1,qext invalid (old false positive)", False,
                       circ(3, [{"gate": "cx", "targets": [1, 2]}]),
                       basis_json))

    rule_basis = load_rule_basis("/root/anchored_nam_q3.txt",
                                 "(Cons (CX q1 q0) (Cons (SYMB 2) (Cons (CX q0 q1)")
    smap = json.dumps({"theta1": 0.5})
    results.append(run("5 real rule: cx01;cx10 window", True,
                       circ(2, [{"gate": "cx", "targets": [0, 1]}, {"gate": "cx", "targets": [1, 0]}]),
                       rule_basis, smap))
    results.append(run("6 real rule: lone cx01", False,
                       circ(2, [{"gate": "cx", "targets": [0, 1]}]),
                       rule_basis, smap))
    results.append(run("7 real rule: cx01;cx10;cx q0,qext valid", True,
                       circ(3, [{"gate": "cx", "targets": [0, 1]}, {"gate": "cx", "targets": [1, 0]},
                                {"gate": "cx", "targets": [0, 2]}]),
                       rule_basis, smap))

    results.append(run_cli("8 case-3", True,
                           circ(3, [{"gate": "cx", "targets": [0, 2]}]), basis_json))
    results.append(run_cli("9 case-4", False,
                           circ(3, [{"gate": "cx", "targets": [1, 2]}]), basis_json))

    print(f"\n{sum(results)}/{len(results)} passed")
    sys.exit(0 if all(results) else 1)


if __name__ == "__main__":
    main()
