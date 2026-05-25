"""Tests for Clifford-orbit priority-pass utilities in semantics.py.

Run with: python3 test_clifford_orbit.py

Each test prints a single PASS/FAIL line and the script exits non-zero on any
failure so it can be plugged into a CI runner. Tests are progressive:
  1. clifford_orbit_image — verify the math on known cases (RXX → RXX, RZZ, RYY).
  2. transform_basis — verify the transformed basis satisfies the new
     intertwiner equation S · L = R · S.
  3. Cross-check with intertwiner_basis — recompute the intertwiner basis for
     the orbit-image rule from scratch (via solve) and verify it spans the
     same subspace as the transformed basis.
"""
import sys
import sympy
import numpy as np

import semantics

PI = sympy.pi
I = sympy.I

# Helpers --------------------------------------------------------------------

I2 = sympy.eye(2)
X = sympy.Matrix([[0, 1], [1, 0]])
Y = sympy.Matrix([[0, -I], [I, 0]])
Z = sympy.Matrix([[1, 0], [0, -1]])
H = sympy.Rational(1, 1) / sympy.sqrt(2) * sympy.Matrix([[1, 1], [1, -1]])


def Rx(t):
    return sympy.Matrix([[sympy.cos(t/2), -I*sympy.sin(t/2)],
                         [-I*sympy.sin(t/2), sympy.cos(t/2)]])


def Ry(t):
    return sympy.Matrix([[sympy.cos(t/2), -sympy.sin(t/2)],
                         [sympy.sin(t/2), sympy.cos(t/2)]])


def Rz(t):
    return sympy.Matrix([[sympy.exp(-I*t/2), 0],
                         [0, sympy.exp(I*t/2)]])


def RXX(t):
    c, s = sympy.cos(t/2), sympy.sin(t/2)
    return sympy.Matrix([
        [c, 0, 0, -I*s],
        [0, c, -I*s, 0],
        [0, -I*s, c, 0],
        [-I*s, 0, 0, c],
    ])


def RYY(t):
    c, s = sympy.cos(t/2), sympy.sin(t/2)
    return sympy.Matrix([
        [c, 0, 0, I*s],
        [0, c, -I*s, 0],
        [0, -I*s, c, 0],
        [I*s, 0, 0, c],
    ])


def RZZ(t):
    return sympy.diag(sympy.exp(-I*t/2), sympy.exp(I*t/2),
                      sympy.exp(I*t/2), sympy.exp(-I*t/2))


def assert_matrices_equal(A, B, tol=1e-9, label=""):
    """Numeric equality check after sympy.evalf — robust to surface differences
    like cos(π/4) vs 1/√2."""
    diff = sympy.simplify(A - B)
    diff_num = np.array(diff.evalf().tolist(), dtype=np.complex128)
    err = np.linalg.norm(diff_num)
    if err > tol:
        print(f"FAIL: {label} (||diff|| = {err:.3e})")
        print(f"  A = {A}")
        print(f"  B = {B}")
        sys.exit(1)


# ---------------------------------------------------------------------------
# Test 1 — clifford_orbit_image returns expected matrices on known cases
# ---------------------------------------------------------------------------

def test_orbit_image_identity():
    """U_a = U_b = I should leave L unchanged."""
    L = RXX(PI/2)
    R = semantics.clifford_orbit_image(L, I2, I2)
    assert_matrices_equal(R, L, label="orbit_image identity")
    print("PASS: orbit_image_identity")


def test_orbit_image_RXX_to_RZZ_via_Ry_anti():
    """For U_a = Ry(π/2), U_b = Ry(-π/2): RXX(π/2) → RZZ(-π/2). This is the
    specific pair we constructed in /tmp/test_constructed_rule.py."""
    L = RXX(PI/2)
    U_a = Ry(PI/2) * Rx(-PI/2)
    U_b = Ry(-PI/2) * Rx(-PI/2)
    R = semantics.clifford_orbit_image(L, U_a, U_b)
    R_expected = RZZ(-PI/2)
    assert_matrices_equal(R, R_expected, label="orbit_image RXX→RZZ(-π/2)")
    print("PASS: orbit_image_RXX_to_RZZ_via_Ry_anti")


def test_orbit_image_RXX_to_RZZ_via_H():
    """H · X · H = Z, so (H⊗H) · RXX(θ) · (H⊗H) = RZZ(θ) (Hadamards are
    self-inverse so the dagger doesn't change sign)."""
    L = RXX(PI/2)
    R = semantics.clifford_orbit_image(L, H, H)
    R_expected = RZZ(PI/2)
    assert_matrices_equal(R, R_expected, label="orbit_image RXX→RZZ via HH")
    print("PASS: orbit_image_RXX_to_RZZ_via_H")


def test_orbit_image_RXX_to_RYY_via_Rz():
    """Rz(π/2) · X · Rz(-π/2) = Y (rotates X axis to Y axis around Z).
    So (Rz(π/2)⊗Rz(π/2)) · RXX(θ) · (Rz(π/2)⊗Rz(π/2))† = RYY(θ)."""
    L = RXX(PI/2)
    U = Rz(PI/2)
    R = semantics.clifford_orbit_image(L, U, U)
    R_expected = RYY(PI/2)
    assert_matrices_equal(R, R_expected, label="orbit_image RXX→RYY via Rz⊗Rz")
    print("PASS: orbit_image_RXX_to_RYY_via_Rz")


def test_orbit_image_shape_validation():
    """Wrong-size inputs should raise ValueError."""
    L_bad = sympy.eye(2)  # only 2×2, not 4×4
    try:
        semantics.clifford_orbit_image(L_bad, I2, I2)
        print("FAIL: orbit_image_shape_validation (no error raised for 2×2 L)")
        sys.exit(1)
    except ValueError:
        pass
    L = RXX(PI/2)
    try:
        semantics.clifford_orbit_image(L, sympy.eye(4), I2)
        print("FAIL: orbit_image_shape_validation (no error for 4×4 U_a)")
        sys.exit(1)
    except ValueError:
        pass
    print("PASS: orbit_image_shape_validation")


# ---------------------------------------------------------------------------
# Test 2 — transform_basis preserves intertwiner equation S·L = R·S
# ---------------------------------------------------------------------------

X_op = X
Y_op = Y
Z_op = Z


def xx_commutant_basis():
    """The 8-element commutant of X⊗X (which equals the commutant of RXX(θ)
    for any θ): {II, XI, IX, XX, YY, YZ, ZY, ZZ}."""
    II = sympy.kronecker_product(I2, I2)
    XI = sympy.kronecker_product(X_op, I2)
    IX = sympy.kronecker_product(I2, X_op)
    XX = sympy.kronecker_product(X_op, X_op)
    YY = sympy.kronecker_product(Y_op, Y_op)
    YZ = sympy.kronecker_product(Y_op, Z_op)
    ZY = sympy.kronecker_product(Z_op, Y_op)
    ZZ = sympy.kronecker_product(Z_op, Z_op)
    return [II, XI, IX, XX, YY, YZ, ZY, ZZ]


def test_transform_basis_preserves_intertwiner_eq():
    """For each commutant basis element B, the transformed S should satisfy
    L · S = S · R where R is the orbit image. Convention: `intertwiner_basis`
    emits matrices satisfying L·S = S·R (verified empirically on asymmetric
    test pair; we mirror it here for consistency with downstream consumers)."""
    L = RXX(PI/2)
    U_a = Ry(PI/2) * Rx(-PI/2)
    U_b = Ry(-PI/2) * Rx(-PI/2)
    R = semantics.clifford_orbit_image(L, U_a, U_b)
    basis = xx_commutant_basis()
    new_basis = semantics.transform_basis(basis, U_a, U_b)
    assert len(new_basis) == len(basis), "basis size changed"
    for i, S in enumerate(new_basis):
        lhs = L * S
        rhs = S * R
        assert_matrices_equal(lhs, rhs, label=f"transform_basis intertwiner B[{i}]")
    print("PASS: transform_basis_preserves_intertwiner_eq")


def test_transform_basis_under_identity_is_noop():
    """U_a = U_b = I means R = L and the basis is unchanged."""
    basis = xx_commutant_basis()
    new_basis = semantics.transform_basis(basis, I2, I2)
    assert len(new_basis) == len(basis)
    for B_old, B_new in zip(basis, new_basis):
        assert_matrices_equal(B_old, B_new, label="transform_basis identity no-op")
    print("PASS: transform_basis_under_identity_is_noop")


def test_transform_basis_span_dimension_preserved():
    """Transformed basis spans the same dimension (rank) as original."""
    L = RXX(PI/2)
    U_a = Ry(PI/2) * Rx(-PI/2)
    U_b = Ry(-PI/2) * Rx(-PI/2)
    basis = xx_commutant_basis()
    new_basis = semantics.transform_basis(basis, U_a, U_b)
    # Build matrix where each column is vec(B) for B in basis; rank = span dim.
    def stack(matrices):
        cols = [np.array(B.evalf().tolist(), dtype=np.complex128).reshape(-1, 1)
                for B in matrices]
        return np.hstack(cols)
    old_rank = np.linalg.matrix_rank(stack(basis), tol=1e-9)
    new_rank = np.linalg.matrix_rank(stack(new_basis), tol=1e-9)
    if old_rank != new_rank or old_rank != 8:
        print(f"FAIL: transform_basis_span_dimension_preserved (old={old_rank}, new={new_rank}, expected 8)")
        sys.exit(1)
    print("PASS: transform_basis_span_dimension_preserved (rank=8)")


# ---------------------------------------------------------------------------
# Test 3 — cross-check: transformed basis matches solver's basis for the
# orbit-image rule from scratch
# ---------------------------------------------------------------------------

def stack_basis_matrix(basis):
    """Build matrix with each column = vec(B) for B in basis."""
    cols = [np.array(B.evalf().tolist(), dtype=np.complex128).reshape(-1, 1)
            for B in basis]
    return np.hstack(cols)


def spans_match(basis_a, basis_b, tol=1e-7):
    """Two bases span the same subspace iff each one's vectors lie in the
    other's span. Test via least-squares projection."""
    A = stack_basis_matrix(basis_a)
    B = stack_basis_matrix(basis_b)
    if A.shape[1] != B.shape[1]:
        return False
    for j in range(B.shape[1]):
        x, *_ = np.linalg.lstsq(A, B[:, j], rcond=None)
        if np.linalg.norm(A @ x - B[:, j]) > tol:
            return False
    for j in range(A.shape[1]):
        x, *_ = np.linalg.lstsq(B, A[:, j], rcond=None)
        if np.linalg.norm(B @ x - A[:, j]) > tol:
            return False
    return True


def test_transformed_basis_matches_intertwiner_solve():
    """Compute intertwiner basis of (L, R) directly via the solver, then
    compute it via transform_basis(commutant(L), U_a, U_b), and verify the
    two span the same subspace."""
    L = RXX(PI/2)
    U_a = Ry(PI/2) * Rx(-PI/2)
    U_b = Ry(-PI/2) * Rx(-PI/2)
    R = semantics.clifford_orbit_image(L, U_a, U_b)

    # Path A: direct intertwiner solve
    direct_basis, _ = semantics.intertwiner_basis(L, R)

    # Path B: transform commutant basis
    transformed = semantics.transform_basis(xx_commutant_basis(), U_a, U_b)

    if not direct_basis:
        print("FAIL: transformed_basis_matches_intertwiner_solve (direct basis empty)")
        sys.exit(1)
    if not spans_match(direct_basis, transformed):
        print("FAIL: transformed_basis_matches_intertwiner_solve (spans differ)")
        print(f"  direct basis size: {len(direct_basis)}")
        print(f"  transformed basis size: {len(transformed)}")
        sys.exit(1)
    print(f"PASS: transformed_basis_matches_intertwiner_solve (both size {len(direct_basis)})")


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# Test 4 — gateset orbit table: each entry's decomposition circuit's matrix
# equals the expected orbit image R = (U_a⊗U_b)·L·(U_a⊗U_b)†.
# ---------------------------------------------------------------------------

def _convert_params_for_circuit_matrix(gates):
    """Mimic is_subspace_linear_combination's param conversion: string values
    like 'pi/2' get sympify'd, keys become sympy.Symbol via param_symbol_map."""
    converted = []
    for op in gates:
        new_op = dict(op)
        if 'params' in new_op:
            new_op['params'] = {
                semantics.param_symbol_map.get(k, sympy.Symbol(k)):
                    sympy.sympify(v, locals=semantics.param_symbol_map)
                for k, v in new_op['params'].items()
            }
        converted.append(new_op)
    return converted


def test_ion_orbit_decomp_matches_conjugation():
    """For each ion-orbit entry, verify that the decomposition circuit's
    matrix (at a concrete θ = π/2) equals the conjugation R = (U_a⊗U_b)·L·(U_a⊗U_b)†."""
    orbit_set = semantics.clifford_orbit_set('ion')
    if len(orbit_set) == 0:
        print("FAIL: ion orbit set is empty")
        sys.exit(1)

    theta_val = PI/2
    L = RXX(theta_val)

    for entry in orbit_set:
        name = entry['name']
        U_a = entry['U_a']
        U_b = entry['U_b']

        # 1) expected R via conjugation
        R_expected = semantics.clifford_orbit_image(L, U_a, U_b)

        # 2) actual R via decomposition + calculate_circuit_matrix
        # decomp(theta_str, theta_neg_str) returns gate dicts.
        decomp = entry['decomp']('pi/2', '-pi/2')
        converted = _convert_params_for_circuit_matrix(decomp)
        R_actual = semantics.calculate_circuit_matrix(2, converted)

        try:
            assert_matrices_equal(R_actual, R_expected,
                                  label=f"ion orbit decomp {name}")
        except SystemExit:
            print(f"  diff:")
            diff = sympy.simplify(R_actual - R_expected)
            print(f"    R_expected (diag): {[R_expected[i,i] for i in range(4)]}")
            print(f"    R_actual (diag):   {[R_actual[i,i] for i in range(4)]}")
            raise
        print(f"PASS: ion_orbit_decomp_matches_conjugation [{name}]")


def test_ion_orbit_set_nonempty():
    """ion orbit set should have at least one (non-identity) entry."""
    orbit_set = semantics.clifford_orbit_set('ion')
    if len(orbit_set) < 1:
        print(f"FAIL: ion orbit set too small ({len(orbit_set)})")
        sys.exit(1)
    print(f"PASS: ion_orbit_set_nonempty (size {len(orbit_set)})")


def test_unknown_gateset_raises():
    """clifford_orbit_set must raise on unknown gateset."""
    try:
        semantics.clifford_orbit_set('does_not_exist')
        print("FAIL: unknown_gateset_raises (no error raised)")
        sys.exit(1)
    except ValueError:
        pass
    print("PASS: unknown_gateset_raises")


# ---------------------------------------------------------------------------
# Test 5 — priority_candidates end-to-end: returns valid (L, R, basis) tuples
# whose basis satisfies the intertwiner equation L·B = B·R.
# ---------------------------------------------------------------------------

def _gates_to_matrix(gates, n_qubits=2):
    converted = _convert_params_for_circuit_matrix(gates)
    return semantics.calculate_circuit_matrix(n_qubits, converted)


def test_priority_candidates_ion_count():
    cands = semantics.priority_candidates('ion')
    expected_min = len(semantics.clifford_orbit_set('ion'))
    if len(cands) != expected_min:
        print(f"FAIL: priority_candidates_ion_count (got {len(cands)}, expected {expected_min})")
        sys.exit(1)
    print(f"PASS: priority_candidates_ion_count (got {len(cands)})")


def test_priority_candidates_basis_intertwines_concrete():
    """For each candidate, substitute θ=π/2 in L and R, then verify every
    basis matrix B satisfies L·B = B·R (the codebase convention).

    This is the strongest correctness check: it bypasses the orbit math and
    just checks that the emitted (L_circuit, R_circuit, basis) triple is a
    valid intertwiner rule under the codebase's actual matrix conventions."""
    cands = semantics.priority_candidates('ion', theta_symbol_name='theta1')
    theta1 = semantics.theta1  # use the same Symbol the gate library uses
    for cand in cands:
        # Build L and R matrices at θ=π/2.
        L = _gates_to_matrix(cand['L_gates']).subs(theta1, sympy.pi/2)
        # R_gates has 'theta1' substituted as a string already; calculate
        # _circuit_matrix's param conversion will sympify it.
        R = _gates_to_matrix(cand['R_gates']).subs(theta1, sympy.pi/2)
        # Substitute θ in the basis too (basis is θ-independent for ion's
        # X⊗X commutant, but be safe).
        for i, B in enumerate(cand['basis']):
            B_at = B.subs(theta1, sympy.pi/2)
            lhs = L * B_at
            rhs = B_at * R
            assert_matrices_equal(lhs, rhs,
                label=f"priority intertwiner [{cand['name']}] basis[{i}]")
        print(f"PASS: priority_candidates_basis_intertwines_concrete [{cand['name']}]")


def test_priority_candidates_includes_RZZ_neg():
    """Specifically check that 'ion_Ry_anti' is in the candidate list and
    produces the same R_matrix as our hand-built RXX(π/2)→RZZ(-π/2) rule."""
    cands = semantics.priority_candidates('ion')
    name_set = {c['name'] for c in cands}
    if 'ion_Ry_anti' not in name_set:
        print(f"FAIL: priority_candidates_includes_RZZ_neg (got {name_set})")
        sys.exit(1)
    cand = next(c for c in cands if c['name'] == 'ion_Ry_anti')
    theta1 = semantics.theta1  # use the same Symbol the gate library uses
    R = _gates_to_matrix(cand['R_gates']).subs(theta1, PI/2)
    R_expected = RZZ(-PI/2)
    assert_matrices_equal(R, R_expected, label="ion_Ry_anti R = RZZ(-π/2)")
    print("PASS: priority_candidates_includes_RZZ_neg")


def test_priority_candidates_basis_size_consistent():
    """Each candidate's basis has the same size as the L-commutant (8 for ion)."""
    cands = semantics.priority_candidates('ion')
    for cand in cands:
        if len(cand['basis']) != 8:
            print(f"FAIL: candidate {cand['name']} has basis size {len(cand['basis'])}, expected 8")
            sys.exit(1)
    print(f"PASS: priority_candidates_basis_size_consistent (all 8)")


def main():
    print("=== Component 1: clifford_orbit_image ===")
    test_orbit_image_identity()
    test_orbit_image_RXX_to_RZZ_via_Ry_anti()
    test_orbit_image_RXX_to_RZZ_via_H()
    test_orbit_image_RXX_to_RYY_via_Rz()
    test_orbit_image_shape_validation()

    print("\n=== Component 2: transform_basis ===")
    test_transform_basis_preserves_intertwiner_eq()
    test_transform_basis_under_identity_is_noop()
    test_transform_basis_span_dimension_preserved()

    print("\n=== Cross-check: transformed basis ↔ direct solve ===")
    test_transformed_basis_matches_intertwiner_solve()

    print("\n=== Component 3: gateset orbit tables ===")
    test_ion_orbit_set_nonempty()
    test_unknown_gateset_raises()
    test_ion_orbit_decomp_matches_conjugation()

    print("\n=== Component 4: priority_candidates ===")
    test_priority_candidates_ion_count()
    test_priority_candidates_includes_RZZ_neg()
    test_priority_candidates_basis_size_consistent()
    test_priority_candidates_basis_intertwines_concrete()

    print("\nAll tests passed.")


if __name__ == "__main__":
    main()
