import sympy
import sys
import json
import numpy

theta1, theta2, theta3, phi, lam, gamma = sympy.symbols('theta1 theta2 theta3 phi lam gamma', real=True)

param_symbol_map = {'theta1': theta1, 'theta2': theta2, 'theta3': theta3, 'phi': phi, 'lam': lam, 'gamma': gamma, 'pi': sympy.pi, 'pi/2': sympy.pi/2, 'pi/4': sympy.pi/4}
gate_semantics = {
    'i': sympy.Matrix([[1, 0], [0, 1]]),
    'h': 1/sympy.sqrt(2) * sympy.Matrix([[1, 1], [1, -1]]),
    'x': sympy.Matrix([[0, 1], [1, 0]]),
    'y': sympy.Matrix([[0, -sympy.I], [sympy.I, 0]]),
    'z': sympy.Matrix([[1, 0], [0, -1]]),
    's': sympy.Matrix([[1, 0], [0, sympy.I]]),
    'sdg': sympy.Matrix([[1, 0], [0, -sympy.I]]),
    't': sympy.Matrix([[1, 0], [0, sympy.exp(sympy.I * sympy.pi / 4)]]),
    'tdg': sympy.Matrix([[1, 0], [0, sympy.exp(-sympy.I * sympy.pi / 4)]]),
    'sx': sympy.Rational(1, 2) * sympy.Matrix([[1+sympy.I, 1-sympy.I], [1-sympy.I, 1+sympy.I]]),

    'cx': sympy.Matrix([
        [1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 0, 1], [0, 0, 1, 0]
    ]),
    'cz': sympy.Matrix([
        [1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, -1]
    ]),
    'swap': sympy.Matrix([
        [1, 0, 0, 0], [0, 0, 1, 0], [0, 1, 0, 0], [0, 0, 0, 1]
    ]),

    'rz': sympy.Matrix([
        [sympy.exp(-sympy.I * gamma / 2), 0],
        [0, sympy.exp(sympy.I * gamma / 2)]
    ]),
    'rx': sympy.Matrix([
        [sympy.cos(theta1 / 2), -sympy.I * sympy.sin(theta1 / 2)],
        [-sympy.I * sympy.sin(theta1 / 2), sympy.cos(theta1 / 2)]
    ]),
    'ry': sympy.Matrix([
        [sympy.cos(theta1 / 2), -sympy.sin(theta1 / 2)],
        [sympy.sin(theta1/ 2), sympy.cos(theta1 / 2)]
    ]),
    'u1': sympy.Matrix([[1, 0], [0, sympy.exp(sympy.I * lam)]]),
    'u2': 1/sympy.sqrt(2) * sympy.Matrix([
        [1, -sympy.exp(sympy.I * lam)],
        [sympy.exp(sympy.I * phi), sympy.exp(sympy.I * (phi + lam))]
    ]),
    'u3': sympy.Matrix([
        [sympy.cos(theta1/2), -sympy.exp(sympy.I * lam) * sympy.sin(theta1/2)],
        [sympy.exp(sympy.I * phi) * sympy.sin(theta1/2), sympy.exp(sympy.I * (phi + lam)) * sympy.cos(theta1/2)]
    ]),
    'gpi': sympy.Matrix([[0, sympy.exp(-sympy.I * phi)], [sympy.exp(sympy.I * phi), 0]]),
    'gpi2': 1/sympy.sqrt(2) * sympy.Matrix([
        [1, -sympy.I * sympy.exp(-sympy.I * phi)],
        [-sympy.I * sympy.exp(sympy.I * phi), 1]
    ]),
    'vz': sympy.Matrix([
        [sympy.exp(-sympy.I * theta1 / 2), 0],
        [0, sympy.exp(sympy.I * theta1 / 2)]
    ]),

    'rxx': sympy.Matrix([
        [sympy.cos(theta1/2), 0, 0, -sympy.I*sympy.sin(theta1/2)],
        [0, sympy.cos(theta1/2), -sympy.I*sympy.sin(theta1/2), 0],
        [0, -sympy.I*sympy.sin(theta1/2), sympy.cos(theta1/2), 0],
        [-sympy.I*sympy.sin(theta1/2), 0, 0, sympy.cos(theta1/2)]
    ]),
    'ms': sympy.Matrix([
        [sympy.cos(theta1/2), 0, 0, -sympy.I*sympy.sin(theta1/2)],
        [0, sympy.cos(theta1/2), -sympy.I*sympy.sin(theta1/2), 0],
        [0, -sympy.I*sympy.sin(theta1/2), sympy.cos(theta1/2), 0],
        [-sympy.I*sympy.sin(theta1/2), 0, 0, sympy.cos(theta1/2)]
    ]),
}
gate_semantics['cnot'] = gate_semantics['cx']

def _tensor_product(matrices):
    """Helper function to compute the tensor product of a list of matrices."""
    result = matrices[0]
    for i in range(1, len(matrices)):
        result = sympy.tensorproduct(result, matrices[i])
    return result

def embed_operator(n_qubits, gate_matrix, targets):
    """
    Embeds a k-qubit operator into an n-qubit circuit.

    Args:
        n_qubits (int): The total number of qubits in the circuit.
        gate_matrix (sympy.Matrix): The matrix of the k-qubit gate.
        targets (list[int]): A list of the target qubit indices.

    Returns:
        sympy.Matrix: The n-qubit matrix representation of the gate.
    """
    k = len(targets)
    if gate_matrix.shape != (2**k, 2**k):
        raise ValueError(f"Gate matrix shape {gate_matrix.shape} does not match target qubit count {k}")

    if k == n_qubits and targets == list(range(n_qubits)):
        return gate_matrix

    pi = list(targets) + sorted(list(set(range(n_qubits)) - set(targets)))

    dim = 2**n_qubits
    perm_matrix = sympy.zeros(dim, dim)
    for i in range(dim):
        in_basis_str = f'{i:0{n_qubits}b}'
        out_basis_str = ''.join([in_basis_str[pi[j]] for j in range(n_qubits)])
        j = int(out_basis_str, 2)
        perm_matrix[j, i] = 1

    if n_qubits - k > 0:
        identity_part = sympy.eye(2**(n_qubits - k))
        tensor_res = sympy.kronecker_product(gate_matrix, identity_part)
        core_op = sympy.simplify(sympy.Matrix(tensor_res))
    else:
        core_op = gate_matrix

    perm_matrix_inv = perm_matrix.T

    full_op = perm_matrix_inv @ core_op @ perm_matrix
    return full_op

def project_to_subspace(n_qubits, circuit_matrix, qubits_to_keep):
    """
    Projects an n-qubit circuit matrix to a k-qubit subspace.

    This is done by assuming the qubits not in `qubits_to_keep` are in the |0> state
    for both the input and output states of the operation.

    The order of qubits in the resulting k-qubit matrix corresponds to the order
    of qubits in the `qubits_to_keep` list. For example, if `qubits_to_keep` is `[2, 0]`,
    the basis of the resulting 2-qubit matrix will be with respect to `|q2, q0>`.

    Args:
        n_qubits (int): The total number of qubits in the original circuit.
        circuit_matrix (sympy.Matrix): The n-qubit matrix of the circuit.
        qubits_to_keep (list[int]): A list of the qubit indices to keep. The order
                                   determines the basis of the projected matrix.

    Returns:
        sympy.Matrix: The k-qubit matrix for the specified subspace.
    """
    k = len(qubits_to_keep)
    if k > n_qubits:
        raise ValueError("Number of qubits to keep cannot be greater than total number of qubits.")

    projected_dim = 2**k
    projected_matrix = sympy.zeros(projected_dim, projected_dim)

    qubit_map = {i: q for i, q in enumerate(qubits_to_keep)}

    for i in range(projected_dim):
        for j in range(projected_dim):

            row_idx = 0
            for bit_pos in range(k):
                if (i >> (k - 1 - bit_pos)) & 1:
                    row_idx |= (1 << qubit_map[bit_pos])

            col_idx = 0
            for bit_pos in range(k):
                if (j >> (k - 1 - bit_pos)) & 1:
                    col_idx |= (1 << qubit_map[bit_pos])

            projected_matrix[i, j] = circuit_matrix[row_idx, col_idx]

    return projected_matrix

def calculate_circuit_matrix(n_qubits, circuit):
    """
    Calculates the matrix representation of a quantum circuit.

    Args:
        n_qubits (int): The total number of qubits in the circuit.
        circuit (list[dict]): A list of gate operations, where each operation
                              is a dictionary with 'gate' (str), 'targets' (list[int]),
                              and optionally 'params' (dict) for parameterized gates.
                              The keys in 'params' should be sympy.Symbol objects.

    Returns:
        sympy.Matrix: The n-qubit matrix representation of the entire circuit.
    """
    dim = 2**n_qubits
    circuit_matrix = sympy.eye(dim)

    for op in circuit:
        gate_name = op['gate']
        targets = op['targets']

        if gate_name not in gate_semantics:
            raise ValueError(f"Gate '{gate_name}' not found in gate_semantics.")

        gate_matrix = gate_semantics[gate_name]

        if 'params' in op:
            gate_matrix = gate_matrix.subs(op['params'])

        op_matrix = embed_operator(n_qubits, gate_matrix, targets)

        circuit_matrix = op_matrix @ circuit_matrix

    return circuit_matrix

def _realify_const_nullspace(K):
    import sympy
    from sympy.polys.matrices import DomainMatrix
    from sympy import QQ, I, sqrt
    n2 = K.shape[0]
    try:
        A = K.applyfunc(sympy.re)
        B = K.applyfunc(sympy.im)
        Mre = sympy.Matrix(sympy.BlockMatrix([[A, -B], [B, A]]))
        Fs2 = QQ.algebraic_field(sqrt(2))
        Nre = DomainMatrix.from_Matrix(Mre).convert_to(Fs2).nullspace().to_Matrix()
        cols = []
        for r in range(Nre.rows):
            vec = Nre.row(r).T
            cols.append(vec[0:n2, 0] + I * vec[n2:2 * n2, 0])
        if not cols:
            return []
        C = sympy.Matrix.hstack(*cols)
        d = C.cols // 2
        if d == 0:
            return []
        import numpy as _np
        Cf = _np.array([[complex(sympy.N(C[i, j])) for j in range(C.cols)]
                        for i in range(C.rows)])
        sel = []
        cur = None
        for j in range(C.cols):
            cand = Cf[:, j:j + 1] if cur is None else _np.column_stack([cur, Cf[:, j]])
            if _np.linalg.matrix_rank(cand, tol=1e-9) > (0 if cur is None else cur.shape[1]):
                cur = cand
                sel.append(j)
                if len(sel) == d:
                    break
        return [C.col(j) for j in sel]
    except Exception:
        return None

def intertwiner_basis(L, R):
    """
    Return basis matrices {S_k} spanning the (parametric) intertwiner space
    L S = S R. S is allowed to depend on the rule's free angles -- at rule-apply
    time the matched θ gets substituted into the basis matrices, yielding the
    full at-θ nullspace. Restricting to constant S was sound but conservative;
    the parametric basis captures the full intertwiner space and accepts more
    valid matches.

    Rules whose L and R do not share the same set of free angle symbols are
    discarded (return empty basis): the two sides aren't even talking about the
    same parameters, so an intertwiner between them would be a coincidence,
    not a semantically meaningful rewrite.

    Method: replace exp(iθ/2) with a formal variable W (so exp(-iθ/2) becomes
    1/W and the matrix becomes Laurent-polynomial in the W's), take the full
    `K.nullspace()` over Q(W), then back-substitute W -> exp(iθ/2) on the
    result.
    """
    import sys, time
    def _stage(msg):
        print(f"[INT {time.strftime('%H:%M:%S')}] {msg}", file=sys.stderr, flush=True)
    _stage(f"entry: L.free={sorted(map(str,L.free_symbols))}, R.free={sorted(map(str,R.free_symbols))}")
    if L.free_symbols != R.free_symbols:
        _stage("DISCARD: free_symbols differ")
        return [], None
    n = L.shape[0]
    _stage(f"build K (size {n*n}x{n*n})")
    K = sympy.kronecker_product(L.T, sympy.eye(n)) - sympy.kronecker_product(sympy.eye(n), R)
    _stage("K built")

    angles = sorted(K.free_symbols, key=str)
    back = {}
    Ws = []
    if not angles:
        def _norm_const(entry):
            if not hasattr(entry, 'replace'):
                return entry
            e = entry.replace(
                lambda x: isinstance(x, sympy.exp) and not x.args[0].free_symbols,
                lambda x: sympy.nsimplify(x.rewrite(sympy.cos)))
            e = e.replace(
                lambda x: isinstance(x, (sympy.cos, sympy.sin)) and not x.args[0].free_symbols,
                lambda x: sympy.nsimplify(x))
            return e
        K = K.applyfunc(_norm_const).applyfunc(sympy.expand)
    if angles:
        _stage(f"rewrite(exp) + expand on K  (angles={list(map(str,angles))})")
        K_exp = sympy.expand(K.rewrite(sympy.exp))
        _stage("rewrite+expand DONE")
        fwd = {}
        for th in angles:
            W = sympy.Symbol('W_' + str(th))
            Ws.append(W)
            fwd[sympy.exp(sympy.I * th / 2)] = W
            back[W] = sympy.exp(sympy.I * th / 2)
        _stage("subs exp->W")
        K = K_exp.subs(fwd)
        _stage(f"subs DONE; K.free={sorted(map(str,K.free_symbols))}")

    if Ws:
        W_set = set(Ws)
        _stage("normalize concrete exp/cos/sin via nsimplify")
        def _norm(entry):
            if not hasattr(entry, 'replace'):
                return entry
            e = entry.replace(
                lambda x: isinstance(x, sympy.exp) and not (x.args[0].free_symbols & W_set),
                lambda x: sympy.nsimplify(x.rewrite(sympy.cos)))
            e = e.replace(
                lambda x: isinstance(x, (sympy.cos, sympy.sin)) and not x.args[0].free_symbols,
                lambda x: sympy.nsimplify(x))
            return e
        K = K.applyfunc(_norm).applyfunc(sympy.expand)
        _stage("normalize DONE")

    null = None
    from sympy.polys.matrices import DomainMatrix
    from sympy import QQ_I, QQ
    if Ws:
        domain_candidates = [
            ("QQ_I", lambda: QQ_I.frac_field(*Ws)),
            ("QQ(sqrt2)", lambda: QQ.algebraic_field(sympy.sqrt(2)).frac_field(*Ws)),
            ("QQ(ζ_8)", lambda: QQ.algebraic_field(sympy.exp(sympy.I*sympy.pi/4)).frac_field(*Ws)),
            ("QQ(ζ_16)", lambda: QQ.algebraic_field(sympy.exp(sympy.I*sympy.pi/8)).frac_field(*Ws)),
        ]
    else:
        domain_candidates = [
            ("QQ_I const", lambda: QQ_I),
            ("QQ(sqrt2) const", lambda: QQ.algebraic_field(sympy.sqrt(2))),
            ("realify const", None),
            ("QQ(ζ_8) const", lambda: QQ.algebraic_field(sympy.exp(sympy.I*sympy.pi/4))),
            ("QQ(ζ_16) const", lambda: QQ.algebraic_field(sympy.exp(sympy.I*sympy.pi/8))),
        ]
    for name, build in domain_candidates:
        try:
            _stage(f"trying DomainMatrix over {name}")
            if build is None:
                null = _realify_const_nullspace(K)
                if null is None:
                    raise ValueError("realify not applicable")
            else:
                F = build()
                dm = DomainMatrix.from_Matrix(K).convert_to(F)
                null_dm = dm.nullspace()
                null_mat = null_dm.to_Matrix()
                null = [null_mat.row(i).T for i in range(null_mat.rows)]
            _stage(f"DomainMatrix({name}).nullspace() DONE -> {len(null)} null vectors")
            break
        except Exception as e:
            _stage(f"{name} failed ({type(e).__name__}: {str(e)[:80]})")
    if null is None:
        _stage("calling K.nullspace() (fallback) ...")
        null = K.nullspace()
        _stage(f"K.nullspace() DONE -> {len(null)} null vectors")
    null = [v.applyfunc(sympy.cancel) for v in null]
    _stage("cancel DONE")
    basis = []
    for v in null:
        S = sympy.Matrix(n, n, v).T.subs(back)
        S = S.applyfunc(lambda e:
            sympy.cancel(e.rewrite(sympy.cos)) if getattr(e, 'free_symbols', None) else e)
        basis.append(S)
    return basis, K

def unitary_solutions_from_basis(basis):
    """
    If you want to enforce unitarity S†S=I in the found subspace:
    parametrize S = sum_j (a_j) * B_j, then impose polynomial equations.
    Returns the symbolic polynomial system. If solve=True, it also attempts to solve it.
    """
    if not basis:
        return None, [], [] if solve else None

    n = basis[0].rows
    xs = sympy.symbols(' '.join([f'x{j}' for j in range(len(basis))]), real=True)
    ys = sympy.symbols(' '.join([f'y{j}' for j in range(len(basis))]), real=True)
    coeffs = [xs[j] + sympy.I*ys[j] for j in range(len(basis))]
    S = sum((coeffs[j]*basis[j] for j in range(len(basis))), sympy.zeros(n))

    raw_eqs = list((S.H * S - sympy.eye(n)).reshape(n*n, 1))
    eqs = [sympy.Eq(e, 0) for e in raw_eqs]

    return sympy.simplify(S), eqs

def clifford_orbit_image(L, U_a, U_b):
    """Return R = (U_a ⊗ U_b) · L · (U_a ⊗ U_b)† for L a 2-qubit unitary.

    The intertwiner equation  S · L = R · S  is guaranteed to have an
    8-dimensional solution space when L is `RXX(θ)`-family (or more generally
    when L's commutant is 8-dimensional and U is single-qubit Clifford).

    Args:
        L (sympy.Matrix): 4×4 unitary.
        U_a, U_b (sympy.Matrix): 2×2 unitaries acting on qubits 0 and 1.

    Returns:
        sympy.Matrix: 4×4 unitary R.
    """
    if L.shape != (4, 4):
        raise ValueError(f"L must be 4×4, got {L.shape}")
    if U_a.shape != (2, 2) or U_b.shape != (2, 2):
        raise ValueError(f"U_a, U_b must be 2×2, got {U_a.shape}, {U_b.shape}")
    UU = sympy.kronecker_product(U_a, U_b)
    return UU * L * UU.H

def _clifford_orbit_set_ion():
    """Per-gateset orbit table for ion (native 2q = RXX).

    Returns a list of dicts: name, U_a, U_b, R_decomp(theta) where
        - name             : human-readable orbit label
        - U_a, U_b         : 2×2 single-qubit unitaries (sympy.Matrix)
        - R_decomp(theta)  : list-of-gate-dicts producing R = (U_a⊗U_b)·L·(U_a⊗U_b)†
                             in ion-native gates, parameterised by theta (sympy
                             expr or symbol). theta is a string in the gate dict
                             so that the same dict is reusable across symbolic
                             and concrete-angle calls.

    The "identity" pair (U_a = U_b = I) is intentionally omitted -- that orbit
    image equals L itself, which is the canonical commutant rule the
    completeness pass already finds.
    """
    PI = sympy.pi
    Ry_p = sympy.Matrix([[sympy.cos(PI/4), -sympy.sin(PI/4)],
                         [sympy.sin(PI/4),  sympy.cos(PI/4)]])
    Ry_n = sympy.Matrix([[sympy.cos(PI/4),  sympy.sin(PI/4)],
                         [-sympy.sin(PI/4), sympy.cos(PI/4)]])
    Rz_p = sympy.diag(sympy.exp(-sympy.I*PI/4), sympy.exp(sympy.I*PI/4))
    Rz_n = sympy.diag(sympy.exp(sympy.I*PI/4),  sympy.exp(-sympy.I*PI/4))

    def _ion_rzz_decomp(theta_str, theta_neg_str):
        """RZZ(θ) implemented as Ry(-π/2)q0; Ry(-π/2)q1; RXX(θ); Ry(π/2)q0; Ry(π/2)q1
        when both U_a = U_b = Ry(π/2). The dagger goes BEFORE L in the circuit
        (left-to-right gate order = right-to-left matrix multiplication)."""
        return [
            {'gate': 'ry', 'targets': [0], 'params': {'theta1': '-pi/2'}},
            {'gate': 'ry', 'targets': [1], 'params': {'theta1': '-pi/2'}},
            {'gate': 'rxx', 'targets': [0, 1], 'params': {'theta1': theta_str}},
            {'gate': 'ry', 'targets': [0], 'params': {'theta1': 'pi/2'}},
            {'gate': 'ry', 'targets': [1], 'params': {'theta1': 'pi/2'}},
        ]

    def _ion_rzz_neg_decomp(theta_str, theta_neg_str):
        """RZZ(-θ) — anti-pair (U_a, U_b) = (Ry(π/2), Ry(-π/2)). Dagger sandwich
        with mixed signs."""
        return [
            {'gate': 'ry', 'targets': [0], 'params': {'theta1': '-pi/2'}},
            {'gate': 'ry', 'targets': [1], 'params': {'theta1': 'pi/2'}},
            {'gate': 'rxx', 'targets': [0, 1], 'params': {'theta1': theta_str}},
            {'gate': 'ry', 'targets': [0], 'params': {'theta1': 'pi/2'}},
            {'gate': 'ry', 'targets': [1], 'params': {'theta1': '-pi/2'}},
        ]

    def _ion_ryy_decomp(theta_str, theta_neg_str):
        """RYY(θ) — same-pair (U_a, U_b) = (Rz(π/2), Rz(π/2))."""
        return [
            {'gate': 'rz', 'targets': [0], 'params': {'gamma': '-pi/2'}},
            {'gate': 'rz', 'targets': [1], 'params': {'gamma': '-pi/2'}},
            {'gate': 'rxx', 'targets': [0, 1], 'params': {'theta1': theta_str}},
            {'gate': 'rz', 'targets': [0], 'params': {'gamma': 'pi/2'}},
            {'gate': 'rz', 'targets': [1], 'params': {'gamma': 'pi/2'}},
        ]

    def _ion_ryy_neg_decomp(theta_str, theta_neg_str):
        return [
            {'gate': 'rz', 'targets': [0], 'params': {'gamma': '-pi/2'}},
            {'gate': 'rz', 'targets': [1], 'params': {'gamma': 'pi/2'}},
            {'gate': 'rxx', 'targets': [0, 1], 'params': {'theta1': theta_str}},
            {'gate': 'rz', 'targets': [0], 'params': {'gamma': 'pi/2'}},
            {'gate': 'rz', 'targets': [1], 'params': {'gamma': '-pi/2'}},
        ]

    return [
        {'name': 'ion_Ry_same', 'U_a': Ry_p, 'U_b': Ry_p, 'decomp': _ion_rzz_decomp},
        {'name': 'ion_Ry_anti', 'U_a': Ry_p, 'U_b': Ry_n, 'decomp': _ion_rzz_neg_decomp},
        {'name': 'ion_Rz_same', 'U_a': Rz_p, 'U_b': Rz_p, 'decomp': _ion_ryy_decomp},
        {'name': 'ion_Rz_anti', 'U_a': Rz_p, 'U_b': Rz_n, 'decomp': _ion_ryy_neg_decomp},
    ]

CLIFFORD_ORBIT_SETS = {
    'ion': _clifford_orbit_set_ion(),
}

def clifford_orbit_set(gateset):
    """Return the orbit set for a gateset. Raises if unknown."""
    if gateset not in CLIFFORD_ORBIT_SETS:
        raise ValueError(f"unknown gateset {gateset!r}; known: {sorted(CLIFFORD_ORBIT_SETS)}")
    return CLIFFORD_ORBIT_SETS[gateset]

def _commutant_basis_for_gateset(gateset):
    """The L-commutant basis used by the priority pass. For ion (L=RXX(θ))
    the commutant is the 8-dim X⊗X commutant {II, XI, IX, XX, YY, YZ, ZY, ZZ}
    -- θ-independent for any non-zero θ. Other gatesets get their own native
    commutant.
    """
    I2 = sympy.eye(2)
    X = sympy.Matrix([[0, 1], [1, 0]])
    Y = sympy.Matrix([[0, -sympy.I], [sympy.I, 0]])
    Z = sympy.Matrix([[1, 0], [0, -1]])
    if gateset == 'ion':
        return [
            sympy.kronecker_product(I2, I2),
            sympy.kronecker_product(X,  I2),
            sympy.kronecker_product(I2, X ),
            sympy.kronecker_product(X,  X ),
            sympy.kronecker_product(Y,  Y ),
            sympy.kronecker_product(Y,  Z ),
            sympy.kronecker_product(Z,  Y ),
            sympy.kronecker_product(Z,  Z ),
        ]
    raise ValueError(f"no commutant basis defined for gateset {gateset!r}")

def _l_circuit_for_gateset(gateset, theta_symbol_name='theta1'):
    """The size-1 L-circuit (the native 2q gate parameterised by a free
    angle) that the priority pass uses as its anchor."""
    if gateset == 'ion':
        return [{'gate': 'rxx', 'targets': [0, 1],
                 'params': {'theta1': theta_symbol_name}}]
    raise ValueError(f"no L-circuit defined for gateset {gateset!r}")

def priority_candidates(gateset, theta_symbol_name='theta1'):
    """Generate Clifford-orbit priority-pass candidates for a gateset.

    For each (U_a, U_b) in the gateset's orbit set, returns a tuple
        (L_gates, R_gates, basis)
    where
        L_gates : the L-circuit gates (the native 2q gate with free θ)
        R_gates : the R-circuit gates (decomp implementing U·L·U†)
        basis   : list of sympy.Matrix forming the intertwiner basis for (L, R)

    Each tuple corresponds to a canonical symbolic rule  L_gates; SYMB ≡ SYMB; R_gates
    discovered by the priority pass. The completeness pass (existing algorithm)
    runs independently and provides any rules whose L and R are both small.
    """
    orbit = clifford_orbit_set(gateset)
    commutant = _commutant_basis_for_gateset(gateset)
    L_gates = _l_circuit_for_gateset(gateset, theta_symbol_name)

    candidates = []
    for entry in orbit:
        U_a = entry['U_a']
        U_b = entry['U_b']
        R_gates = []
        for op in entry['decomp'](theta_symbol_name, '-(' + theta_symbol_name + ')'):
            new_op = dict(op)
            new_op['targets'] = list(new_op['targets'])
            if 'params' in new_op:
                new_op['params'] = dict(new_op['params'])
            R_gates.append(new_op)
        basis = transform_basis(commutant, U_a, U_b)
        candidates.append({
            'name': entry['name'],
            'L_gates': L_gates,
            'R_gates': R_gates,
            'basis': basis,
        })
    return candidates

def transform_basis(basis, U_a, U_b):
    """Transform a commutant basis {B_i} into the intertwiner basis for the
    orbit image R = (U_a ⊗ U_b) · L · (U_a ⊗ U_b)†.

    Runtime convention (matches `intertwiner_basis` after the transpose fix
    and what is_subspace_linear_combination accepts): basis matrices M
    satisfy M · L = R · M.

    Derivation for R = U·L·U† (where U = U_a ⊗ U_b):
        M · L = R · M = U·L·U†·M
        => U†·M·L = L·U†·M    (multiply left by U†)
        => Let T = U†·M, then T·L = L·T (T in commutant of L), and M = U·T.

    So basis = {U·C : C ∈ commutant(L)} — left-multiply by (U_a ⊗ U_b).

    Args:
        basis (list[sympy.Matrix]): 4×4 commutant basis matrices for L.
        U_a, U_b (sympy.Matrix): 2×2 unitaries used to build R.

    Returns:
        list[sympy.Matrix]: transformed basis.
    """
    UU = sympy.kronecker_product(U_a, U_b)
    return [UU * B for B in basis]

def have_same_eigenvalues(matrix1, matrix2):
    """
    Checks if two symbolic matrices have the same eigenvalues with the same multiplicities.

    Args:
        matrix1 (sympy.Matrix): The first matrix.
        matrix2 (sympy.Matrix): The second matrix.

    Returns:
        bool: True if they have the same eigenvalues, False otherwise.
    """
    from sympy import Symbol, expand, trigsimp
    lam = Symbol('lambda')
    char_poly1 = matrix1.charpoly(lam).as_expr()
    char_poly2 = matrix2.charpoly(lam).as_expr()

    expanded_poly1 = expand(char_poly1, complex=True)
    expanded_poly2 = expand(char_poly2, complex=True)

    trig_simplified1 = trigsimp(expanded_poly1)
    trig_simplified2 = trigsimp(expanded_poly2)

    return trig_simplified1 == trig_simplified2

def have_same_trace(matrix1, matrix2):
    """
    Checks if two symbolic matrices have the same trace.
    """
    tr1 = matrix1.trace()
    tr2 = matrix2.trace()

    if sympy.simplify(tr1 - tr2) == 0:
        return True
    else:
        return False

def have_same_determinant(matrix1, matrix2):
    """
    Checks if two symbolic matrices have the same determinant.
    """
    det1 = matrix1.det()
    det2 = matrix2.det()

    if sympy.simplify(det1 - det2) == 0:
        return True
    else:
        return False

_circuit_parts_cache = {}

def _circuit_parts(circuit_json):
    """Returns (n_qubits, pre-SYMB matrix, post-SYMB matrix) for a circuit,
    memoized by its JSON string. The matrices are a function of this circuit
    alone (unlike L/R, which couple a pair), so they cache cleanly."""
    cached = _circuit_parts_cache.get(circuit_json)
    if cached is not None:
        return cached
    circuit = json.loads(circuit_json)
    n_qubits = circuit.get("n_qubits")
    if n_qubits is None:
        raise ValueError("Circuit must specify 'n_qubits'.")
    gates = circuit.get("gates", [])
    for op in gates:
        if 'params' in op:
            op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}
    try:
        symb_index = [op['gate'] for op in gates].index('symb')
    except ValueError:
        raise ValueError("Circuit does not contain a 'symb' gate.")
    pre = calculate_circuit_matrix(n_qubits, gates[:symb_index])
    post = calculate_circuit_matrix(n_qubits, gates[symb_index + 1:])
    parts = (n_qubits, pre, post)
    if len(_circuit_parts_cache) > 1024:
        _circuit_parts_cache.clear()
    _circuit_parts_cache[circuit_json] = parts
    return parts

def compute_L_R(circuit1_json, circuit2_json):
    n_qubits1, A, B = _circuit_parts(circuit1_json)
    n_qubits2, C, D = _circuit_parts(circuit2_json)
    if n_qubits1 != n_qubits2:
        raise ValueError("The two circuits must have the same number of qubits.")

    L = A @ C.H
    R = B.H @ D
    return (L, R)

def solve_trace(circuit_json1, circuit_json2):
    L,R = compute_L_R(circuit_json1, circuit_json2)
    return have_same_trace(L, R)

def is_linear_combination(matrix_m, list_of_matrices_a):
    """
    Checks if a matrix M is a linear combination of a list of matrices A.

    Args:
        matrix_m (sympy.Matrix): The matrix to check.
        list_of_matrices_a (list[sympy.Matrix]): The list of matrices to form the basis.

    Returns:
        tuple[bool, dict or None]: A tuple containing:
            - A boolean which is True if M is a linear combination, False otherwise.
            - A dictionary of the coefficients if a solution is found, otherwise None.
    """
    if not list_of_matrices_a:
        return matrix_m.is_zero_matrix, None

    rows, cols = matrix_m.shape
    for A in list_of_matrices_a:
        if A.shape != (rows, cols):
            raise ValueError("All matrices must have the same dimensions.")

    num_matrices = len(list_of_matrices_a)
    coeffs = sympy.symbols(f'c_:{num_matrices}')

    linear_combination = sympy.zeros(rows, cols)
    for i in range(num_matrices):
        linear_combination += coeffs[i] * list_of_matrices_a[i]
    print("linear_combination: " + str(linear_combination))
    equations = []
    for r in range(rows):
        for c in range(cols):
            equations.append(sympy.Eq(matrix_m[r, c], linear_combination[r, c], evaluate=False))

    solution = sympy.linsolve(equations, coeffs)
    if solution:
        if isinstance(solution, list):
            solution = solution[0]
        return True, solution

    try:
        M_num = np.array(matrix_m.evalf().tolist(), dtype=np.complex128)
        B_flat = np.stack([
            np.array(B.evalf().tolist(), dtype=np.complex128).reshape(-1)
            for B in list_of_matrices_a
        ], axis=1)
        m_flat = M_num.reshape(-1)
        x, *_ = np.linalg.lstsq(B_flat, m_flat, rcond=None)
        residual = np.linalg.norm(B_flat @ x - m_flat)
        if residual < 1e-6:
            return True, None
    except Exception as e:
        print(f"numerical fallback failed: {e}")

    return False, None

def solve_intertwiner_equation(circuit1_json, circuit2_json, output_file=None):
    """
    Solves the intertwiner equation A;S;B = C;S;D for S.
    LHS is circuit1, RHS is circuit2.
    Each circuit must contain exactly one 'symb' gate.
    The equation is transformed to L*S = S*R and solved for S.
    """
    L,R = compute_L_R(circuit1_json, circuit2_json)

    basis, _ = intertwiner_basis(L, R)

    if not basis:
        print("No non-trivial solutions found.")
    else:
        for i, b in enumerate(basis):
            print(b)

def read_basis_file(file_path):
    """
    Reads a basis file containing a list of matrices in JSON format,
    and returns a list of sympy.MutableDenseMatrix objects.
    This function is designed to read files created by the current version of
    solve_intertwiner_equation, which may contain multiple JSON objects.
    It will parse the last valid JSON object in the file.
    """
    with open(file_path, 'r') as f:
        content = f.read().strip()

    if not content:
        return []

    all_bases = json.loads(content)
    all_bases_syp = []
    for basis_list in all_bases:
        basis_list_syp = []
        for matrix_as_lol in basis_list:
            matrix_as_sympy_expr = [[sympy.sympify(e) for e in row] for row in matrix_as_lol]
            basis_list_syp.append(sympy.Matrix(matrix_as_sympy_expr))
        all_bases_syp.append(basis_list_syp)

    return all_bases_syp

def sparse_to_basis(sparse_matrices, symbol_map=None):
    ms = json.loads(sparse_matrices)
    if symbol_map:
        symbol_map = json.loads(symbol_map)

    if symbol_map:
        symbol_map = {param_symbol_map[k]: param_symbol_map[v] if isinstance(v, str) else v for k, v in symbol_map.items()}
    matrices = []
    for m in ms:
        num_row = m["rows"]
        num_col = m["cols"]
        entries = m["entries"]

        M = sympy.zeros(rows=num_row, cols=num_col)
        for entry in entries:
            M[entry["row"], entry["col"]] = sympy.sympify(entry["value"], locals=param_symbol_map)
        if symbol_map:
            M = M.subs(symbol_map)
        matrices.append(M)
    return matrices

_np_const_gate_cache = {}
_np_gate_lambda_cache = {}
_np_embed_idx_cache = {}

def _np_gate_matrix(name, params):
    """Numpy matrix for gate `name` under concrete params {Symbol: value}.
    Returns None if any required parameter is missing or non-numeric."""
    gm = gate_semantics[name]
    syms = sorted(gm.free_symbols, key=str)
    if not syms:
        M = _np_const_gate_cache.get(name)
        if M is None:
            M = numpy.array(gm.evalf(), dtype=numpy.complex128)
            _np_const_gate_cache[name] = M
        return M
    cached = _np_gate_lambda_cache.get(name)
    if cached is None:
        fn = sympy.lambdify(syms, gm, 'numpy')
        cached = (syms, fn)
        _np_gate_lambda_cache[name] = cached
    syms, fn = cached
    vals = []
    for s in syms:
        v = None if params is None else params.get(s)
        if v is None:
            return None
        try:
            vals.append(complex(v))
        except (TypeError, ValueError):
            return None
    return numpy.asarray(fn(*vals), dtype=numpy.complex128)

def _np_embed(n_qubits, gate_np, targets):
    """Numeric embed_operator: P^T (G (x) I) P via fancy indexing."""
    k = len(targets)
    if k == n_qubits and list(targets) == list(range(n_qubits)):
        return gate_np
    key = (n_qubits, tuple(targets))
    idx = _np_embed_idx_cache.get(key)
    if idx is None:
        pi = list(targets) + sorted(set(range(n_qubits)) - set(targets))
        dim = 2 ** n_qubits
        idx = numpy.empty(dim, dtype=numpy.int64)
        for i in range(dim):
            b = f'{i:0{n_qubits}b}'
            idx[i] = int(''.join(b[pi[j]] for j in range(n_qubits)), 2)
        _np_embed_idx_cache[key] = idx
    if n_qubits > k:
        core = numpy.kron(gate_np, numpy.eye(2 ** (n_qubits - k), dtype=numpy.complex128))
    else:
        core = gate_np
    return core[numpy.ix_(idx, idx)]

def _np_circuit_matrix(n_qubits, gates):
    """Numeric window unitary, or None if any gate parameter is symbolic."""
    U = numpy.eye(2 ** n_qubits, dtype=numpy.complex128)
    for op in gates:
        g = _np_gate_matrix(op['gate'], op.get('params'))
        if g is None:
            return None
        U = _np_embed(n_qubits, g, op['targets']) @ U
    return U

_basis_template_cache = {}
_basis_numeric_cache = {}
_BASIS_CACHE_MAX = 4096

def _basis_and_A(sparse_basis, symbol_map):
    """Returns (basis_matrices, A) where A is the numpy column-stack of the
    basis (or None if the basis is symbolic under this symbol_map)."""
    templates = _basis_template_cache.get(sparse_basis)
    if templates is None:
        if len(_basis_template_cache) > _BASIS_CACHE_MAX:
            _basis_template_cache.clear()
            _basis_numeric_cache.clear()
        templates = sparse_to_basis(sparse_basis, None)
        _basis_template_cache[sparse_basis] = templates
    if not templates:
        return [], None

    A = _basis_numeric_cache.get(sparse_basis)
    if A is None:
        if any(M.free_symbols for M in templates):
            A = False
        else:
            A = numpy.column_stack([
                numpy.array(M.evalf(), dtype=numpy.complex128).reshape(-1)
                for M in templates])
        _basis_numeric_cache[sparse_basis] = A

    if A is not False:
        return templates, A

    basis_matrices = sparse_to_basis(sparse_basis, symbol_map)
    try:
        A_sub = numpy.column_stack([
            numpy.array(M.evalf(), dtype=numpy.complex128).reshape(-1)
            for M in basis_matrices])
    except (TypeError, ValueError):
        A_sub = None
    return basis_matrices, A_sub

def check_rule_not_affect_other_qubits(circuit_json, L_json, qubits_to_check):
    """
    Checks that the circuit's transformation on the qubits not in qubits_to_check is identity.
    """
    circuit = json.loads(circuit_json)
    n_qubits = circuit["n_qubits"]
    qubits_to_check = json.loads(qubits_to_check)
    L = json.loads(L_json)

    calculate_circuit_matrix(n_qubits, circuit["gates"])
    circuit_matrix = calculate_circuit_matrix(n_qubits, circuit["gates"])

    new_gates = []
    new_gates.append(L["gates"])
    new_gates.append(circuit['gates'])
    combined_circuit = calculate_circuit_matrix(n_qubits, new_gates)

    all_qubits = set(range(n_qubits))
    permutations = []
    for i in 0 in range(2 ** n_qubits):
        perm = []
        for j in range(n_qubits):
            perm.append((i >> j) & 1)
        permutations.append(perm)

    for perm in permutations:
        vec1 = circuit_matrix @ sympy.Matrix(perm)
        vec2 = combined_circuit @ sympy.Matrix(perm)

        for q in all_qubits - set(qubits_to_check):
            if vec1[q] != vec2[q]:
                return False

    return True

def is_subspace_linear_combination(circuit_json, sparse_basis, qubits_to_check, symbol_map=None, eps=None):
    """
    Exact lifted-basis check for the symbolic-rule constraint S |= M.

    The matched window acts on the rule qubits (canonicalized by the Java side
    to indices 0..r-1; qubit 0 = MSB) plus possibly external qubits (r..n-1).
    The rule stays valid for a window U that couples rule and external qubits
    iff (L (x) I_ext) U = U (R (x) I_ext); the solution space of that lifted
    intertwiner equation is exactly span{B_i} (x) L(H_ext). Equivalently:
    slice U into rule-space operators M_(ie,je)[ir,jr] = U[(ir,ie),(jr,je)]
    and accept iff every slice lies in span{B_i}. Sound and complete, unlike
    the old per-gate projection which dropped boundary-crossing CX branches.
    """
    circuit = json.loads(circuit_json)
    n_qubits = circuit["n_qubits"]
    qubits_to_check = json.loads(qubits_to_check)
    for op in circuit['gates']:
        if 'params' in op:
            op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}

    r = len(qubits_to_check)
    if sorted(qubits_to_check) != list(range(r)):
        raise ValueError(f"rule qubits must be 0..{r-1}, got {qubits_to_check}")

    basis_matrices, A = _basis_and_A(sparse_basis, symbol_map)
    if not basis_matrices:
        return False

    n_eff = max(n_qubits, r)
    d_r = 2 ** r
    d_e = 2 ** (n_eff - r)
    tol = eps if eps is not None else 1e-6

    if A is not None:
        Un = _np_circuit_matrix(n_eff, circuit["gates"])
        if Un is not None:
            T = Un.reshape(d_r, d_e, d_r, d_e).transpose(1, 3, 0, 2) \
                  .reshape(d_e * d_e, d_r * d_r).T
            X, _, _, _ = numpy.linalg.lstsq(A, T, rcond=None)
            residual = float(numpy.max(numpy.abs(A @ X - T)))
            print("residual: %.3e (tol %.1e)" % (residual, tol))
            return bool(residual < tol)

    U = calculate_circuit_matrix(n_eff, circuit["gates"])

    slices = []
    for i_e in range(d_e):
        for j_e in range(d_e):
            M = sympy.zeros(d_r, d_r)
            for i_r in range(d_r):
                for j_r in range(d_r):
                    M[i_r, j_r] = U[i_r * d_e + i_e, j_r * d_e + j_e]
            slices.append(M)

    try:
        A2 = numpy.column_stack([
            numpy.array(B.evalf(), dtype=numpy.complex128).reshape(d_r * d_r)
            for B in basis_matrices])
        T = numpy.column_stack([
            numpy.array(M.evalf(), dtype=numpy.complex128).reshape(d_r * d_r)
            for M in slices])
        X, _, _, _ = numpy.linalg.lstsq(A2, T, rcond=None)
        residual = float(numpy.max(numpy.abs(A2 @ X - T)))
        print("residual: %.3e (tol %.1e)" % (residual, tol))
        return bool(residual < tol)
    except (TypeError, ValueError):
        pass

    for M in slices:
        is_combo, _ = is_linear_combination(M, basis_matrices)
        if not is_combo:
            return False
    return True

def calculate_subspace_circuit_matrix(n_qubits, circuit, qubits_to_check):
    """
    Calculates the matrix representation of a quantum circuit on a specified subspace.

    Args:
        n_qubits (int): The total number of qubits in the circuit.
        circuit (list[dict]): A list of gate operations.
        qubits_to_check (list[int]): A list of the qubit indices for the subspace.

    Returns:
        sympy.Matrix: The matrix representation of the circuit on the subspace.
    """
    k = len(qubits_to_check)
    subspace_matrices = []
    subspace_matrix = sympy.eye(2**k)
    subspace_matrices.append(subspace_matrix)
    qubit_map = {qubit: i for i, qubit in enumerate(qubits_to_check)}

    for op in circuit:
        gate_name = op['gate']
        targets = op['targets']

        if all(t in qubits_to_check for t in targets):
            subspace_targets = [qubit_map[t] for t in targets]

            if gate_name not in gate_semantics:
                raise ValueError(f"Gate '{gate_name}' not found in gate_semantics.")

            gate_matrix = gate_semantics[gate_name]

            if 'params' in op:
                gate_matrix = gate_matrix.subs(op['params'])

            op_matrix = embed_operator(k, gate_matrix, subspace_targets)

            temp_subspace_matrices = []
            for sm in subspace_matrices:
                new_sub = op_matrix @ sm
                try:
                    if not new_sub.free_symbols:
                        new_sub = sympy.Matrix(np.array(new_sub.evalf().tolist(), dtype=np.complex128))
                except Exception:
                    pass
                temp_subspace_matrices.append(new_sub)
            subspace_matrices = temp_subspace_matrices

        elif gate_name in ['cx', 'cnot'] and targets[0] in qubits_to_check and targets[1] not in qubits_to_check:
            pass
        elif gate_name in ['cx', 'cnot'] and targets[1] in qubits_to_check and targets[0] not in qubits_to_check:
            temp_subspace_matrices = []
            for sm in subspace_matrices:
                x = gate_semantics['x']
                op_matrix = embed_operator(k, x, [qubit_map[targets[1]]])
                new_sub = op_matrix @ sm
                temp_subspace_matrices.append(new_sub)
            subspace_matrices = temp_subspace_matrices

        elif all(t not in qubits_to_check for t in targets):
            pass
    print("subspace_matrices: " + str(subspace_matrices))
    return subspace_matrices

import numpy as np
import sympy

def linear_span_test(basis):
    if not basis:
        return False

    n = basis[0].rows
    cols = []
    for b1 in basis:
        for b2 in basis:
            C = (b1.H * b2).evalf()
            cols.append(np.array(C.tolist(), dtype=np.complex128).reshape(-1, 1))
    M = np.hstack(cols)
    b = np.eye(n).reshape(-1)

    x, residuals, rank, s = np.linalg.lstsq(M, b, rcond=None)
    return np.linalg.norm(M @ x - b) < tol

def solve_eigen(circuit1_json, circuit2_json):
    L,R = compute_L_R(circuit1_json, circuit2_json)
    L_eigen = L.eigenvals()
    R_eigen = R.eigenvals()
    for (k,v) in L_eigen.items():
        if k in R_eigen:
            if R_eigen[k] != v:
                return False
        else:
            return False
    for (k,v) in R_eigen.items():
        if k in L_eigen:
            if L_eigen[k] != v:
                return False
        else:
            return False
    return True

def solve_big_check(circuit1_json, circuit2_json):
    L,R = compute_L_R(circuit1_json, circuit2_json)

    if have_same_trace(L, R):
        return True
    return False

_BIGCHECK_EIGEN_TOL = 1e-7

def _sorted_concrete_eigvals(mat, sub):
    """Substitute ``sub`` into the sympy matrix ``mat``, convert to a numpy
    complex array, return its eigenvalues sorted lexicographically by
    (real, imag) and rounded to suppress floating-point jitter."""
    numeric = mat.subs(sub)
    arr = numpy.array(numeric.tolist(), dtype=numpy.complex128)
    vals = numpy.linalg.eigvals(arr)
    sortable = sorted(((round(v.real, 8) + 0.0, round(v.imag, 8) + 0.0) for v in vals))
    return [complex(r, i) for r, i in sortable]

def solve_eigen_symbolic_check(circuit1_json, circuit2_json):
    """Exact symbolic check that L and R share the same multiset of
    eigenvalues, by comparing their characteristic polynomials coefficient
    by coefficient under sympy simplification.

    Returns True iff every coefficient of charpoly(L) - charpoly(R)
    simplifies (with trig simplification) to zero. Intended as a final
    gate after the probabilistic concrete-eigen check has already passed.
    """
    L, R = compute_L_R(circuit1_json, circuit2_json)
    lam_var = sympy.Dummy('chrlambda')
    poly_L = L.charpoly(lam_var)
    poly_R = R.charpoly(lam_var)
    coeffs_L = poly_L.all_coeffs()
    coeffs_R = poly_R.all_coeffs()
    if len(coeffs_L) != len(coeffs_R):
        return False
    for a, b in zip(coeffs_L, coeffs_R):
        diff = sympy.trigsimp(sympy.expand(sympy.simplify(a - b)))
        if diff != 0:
            return False
    return True

def single_circuit_eigen_fingerprint(circuit_json, seed=None):
    """Per-circuit eigenvalue fingerprint at a concrete random sample. The
    SYMB placeholder (if present) is treated as identity. Two circuits with
    the same fingerprint are unitarily similar at that sample, so they're
    plausible intertwiner partners -- a cheap bucket key for the filter
    phase that avoids O(N^2) checkBig calls."""
    import json as _json
    circuit = _json.loads(circuit_json)
    n_qubits = circuit.get("n_qubits", 0)
    gates = [op for op in circuit.get("gates", []) if op.get("gate") != "symb"]
    for op in gates:
        if 'params' in op:
            op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map)
                            for k, v in op['params'].items()}
    M = calculate_circuit_matrix(n_qubits, gates)
    rng = numpy.random.default_rng(int(seed) if seed is not None else 0)
    sub = {
        theta1: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        theta2: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        theta3: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        phi:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
        lam:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
        gamma:  float(rng.uniform(0.0, 2.0 * numpy.pi)),
    }
    vals = _sorted_concrete_eigvals(M, sub)
    return ";".join(f"{v.real:.8f}+{v.imag:.8f}i" for v in vals)

def multi_circuit_eigen_fingerprint(circuits_json, seed=None, ntraces=1):
    """Batched per-circuit eigenvalue fingerprints, one line per circuit.

    Takes a JSON object {"circuits": [circuit, ...]} and returns each
    circuit's fingerprint at `ntraces` seeded angle samples (samples drawn
    once, shared by all circuits, so equal-fingerprint <=> eigen-equal at
    every sample -- the same predicate the pairwise checkBig verified).
    Batching turns O(N) IPC round-trips into one.
    """
    import json as _json
    payload = _json.loads(circuits_json)
    rng = numpy.random.default_rng(int(seed) if seed is not None else 0)
    subs = []
    for _ in range(max(1, int(ntraces))):
        subs.append({
            theta1: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            theta2: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            theta3: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            phi:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
            lam:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
            gamma:  float(rng.uniform(0.0, 2.0 * numpy.pi)),
        })
    lines = []
    for circuit in payload.get("circuits", []):
        n_qubits = circuit.get("n_qubits", 0)
        gates = [op for op in circuit.get("gates", []) if op.get("gate") != "symb"]
        for op in gates:
            if 'params' in op:
                op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map)
                                for k, v in op['params'].items()}

        parts = _np_multi_fingerprint_parts(n_qubits, gates, subs)
        if parts is None:
            M = calculate_circuit_matrix(n_qubits, gates)
            parts = []
            for sub in subs:
                vals = _sorted_concrete_eigvals(M, sub)
                parts.append(";".join(f"{v.real:.8f}+{v.imag:.8f}i" for v in vals))
        lines.append("|".join(parts))
    return "\n".join(lines)

def _np_multi_fingerprint_parts(n_qubits, gates, subs):
    """Numeric fingerprint strings for one circuit across all samples, or
    None if any gate parameter cannot be resolved to a number under the
    sample substitution (then the caller falls back to sympy)."""
    parts = []
    for sub in subs:
        U = numpy.eye(2 ** n_qubits, dtype=numpy.complex128)
        for op in gates:
            params_num = None
            if 'params' in op:
                params_num = {}
                for sym, expr in op['params'].items():
                    try:
                        params_num[sym] = complex(expr.subs(sub)) if hasattr(expr, 'subs') else complex(expr)
                    except (TypeError, ValueError):
                        return None
            g = _np_gate_matrix(op['gate'], params_num)
            if g is None:
                return None
            U = _np_embed(n_qubits, g, op['targets']) @ U
        vals = numpy.linalg.eigvals(U)
        sortable = sorted(((round(v.real, 8) + 0.0, round(v.imag, 8) + 0.0) for v in vals))
        parts.append(";".join(f"{r:.8f}+{i:.8f}i" for r, i in sortable))
    return parts

def _np_multi_trace_parts(n_qubits, gates, subs):
    parts = []
    for sub in subs:
        U = numpy.eye(2 ** n_qubits, dtype=numpy.complex128)
        for op in gates:
            params_num = None
            if 'params' in op:
                params_num = {}
                for sym, expr in op['params'].items():
                    try:
                        params_num[sym] = complex(expr.subs(sub)) if hasattr(expr, 'subs') else complex(expr)
                    except (TypeError, ValueError):
                        return None
            g = _np_gate_matrix(op['gate'], params_num)
            if g is None:
                return None
            U = _np_embed(n_qubits, g, op['targets']) @ U
        tr = numpy.trace(U)
        parts.append(f"{round(float(tr.real), 8) + 0.0:.8f}+{round(float(tr.imag), 8) + 0.0:.8f}i")
    return parts

def multi_circuit_trace_fingerprint(circuits_json, seed=None, ntraces=1):
    import json as _json
    payload = _json.loads(circuits_json)
    rng = numpy.random.default_rng(int(seed) if seed is not None else 0)
    subs = []
    for _ in range(max(1, int(ntraces))):
        subs.append({
            theta1: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            theta2: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            theta3: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            phi:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
            lam:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
            gamma:  float(rng.uniform(0.0, 2.0 * numpy.pi)),
        })
    lines = []
    for circuit in payload.get("circuits", []):
        n_qubits = circuit.get("n_qubits", 0)
        gates = [op for op in circuit.get("gates", []) if op.get("gate") != "symb"]
        for op in gates:
            if 'params' in op:
                op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map)
                                for k, v in op['params'].items()}
        parts = _np_multi_trace_parts(n_qubits, gates, subs)
        if parts is None:
            M = calculate_circuit_matrix(n_qubits, gates)
            parts = []
            for sub in subs:
                tr = complex((M.subs(sub)).trace())
                parts.append(f"{round(tr.real, 8) + 0.0:.8f}+{round(tr.imag, 8) + 0.0:.8f}i")
        lines.append("|".join(parts))
    return "\n".join(lines)

def eigenvalue_fingerprint(circuit1_json, circuit2_json, seed=None):
    """Return the L-matrix eigenvalues (sorted, rounded) at a concrete random
    sample of the symbolic angles -- intended as a bucket key for the
    pre-filter phase. Two circuits with the same fingerprint are eigen-
    equivalent at this sample; ``solve_big_check_eigen`` with the same seed
    then verifies across ntraces draws.

    The interface mirrors solve_distinct_eigen / solve_big_check_eigen (takes
    two circuits, computes L from compute_L_R). Use circuit2_json = the empty
    circuit JSON if you want raw eigenvalues of c1's matrix; otherwise it's
    eigen(c1) at concrete sample.
    """
    L, _R = compute_L_R(circuit1_json, circuit2_json)
    rng = numpy.random.default_rng(int(seed) if seed is not None else 0)
    sub = {
        theta1: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        theta2: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        theta3: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        phi:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
        lam:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
        gamma:  float(rng.uniform(0.0, 2.0 * numpy.pi)),
    }
    vals = _sorted_concrete_eigvals(L, sub)
    return ";".join(f"{v.real:.8f}+{v.imag:.8f}i" for v in vals)

def all_distinct_eigen(circuit1_json, circuit2_json, seed=None):
    """Returns True iff at a random concrete substitution the L matrix has
    all distinct eigenvalues. Used as an early skip for the symbolic
    intertwiner solve: dim(intertwiner) = n in this case, producing a
    relatively small / "diagonal" basis (3-torus of unitary intertwiners
    modulo global phase) — a less expressive symbolic rule.
    """
    L, _R = compute_L_R(circuit1_json, circuit2_json)
    rng = numpy.random.default_rng(int(seed) if seed is not None else 0)
    sub = {
        theta1: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        theta2: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        theta3: float(rng.uniform(0.0, 2.0 * numpy.pi)),
        phi:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
        lam:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
        gamma:  float(rng.uniform(0.0, 2.0 * numpy.pi)),
    }
    vals = _sorted_concrete_eigvals(L, sub)
    for i in range(len(vals) - 1):
        if abs(vals[i+1] - vals[i]) < _BIGCHECK_EIGEN_TOL:
            return False
    return True

def solve_big_check_eigen(circuit1_json, circuit2_json, seed, ntraces):
    """Concrete-eigenvalue version of ``solve_big_check``: draws ``ntraces``
    independent random substitutions for the symbolic params from a
    seeded RNG and returns True only if L and R share their sorted
    eigenvalue tuple on every draw. Stronger than a single-trace check;
    intended to be called after circuits have already been grouped by
    trace upstream.
    """
    L, R = compute_L_R(circuit1_json, circuit2_json)
    rng = numpy.random.default_rng(int(seed))
    n = max(1, int(ntraces))
    for _ in range(n):
        sub = {
            theta1: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            theta2: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            theta3: float(rng.uniform(0.0, 2.0 * numpy.pi)),
            phi:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
            lam:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
            gamma:  float(rng.uniform(0.0, 2.0 * numpy.pi)),
        }
        lvals = _sorted_concrete_eigvals(L, sub)
        rvals = _sorted_concrete_eigvals(R, sub)
        if len(lvals) != len(rvals):
            return False
        for a, b in zip(lvals, rvals):
            if abs(a - b) > _BIGCHECK_EIGEN_TOL:
                return False
    return True

import argparse
def main(argv=None):
    parser = argparse.ArgumentParser(description='Quantum circuit semantics analysis.')
    parser.add_argument('-eigenvals', nargs=1, metavar=('C_JSON'),
                        help='Compute and print the eigenvalues of the input circuit matrix.')
    parser.add_argument('-tracecheck', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Solve A;S;B = C;S;D -> L;S = S;R, check trace(L) = trace(R)')
    parser.add_argument('-trace', nargs=1, metavar=('C1_JSON'), help='Compute and print the trace of the input circuit matrix.')
    parser.add_argument('-seed', nargs=1, metavar=('SEED'), help='If set, draw concrete random values for symbolic params (theta1, theta2, theta3, phi, lam, gamma) using this integer seed, and print a numeric trace instead of a symbolic one. Same seed across invocations yields the same substitution, so circuits with the same matrix get the same numeric trace.')
    parser.add_argument('-ntraces', nargs=1, metavar=('N'), help='Number of independent random substitutions to perform when -seed is set. Each draws fresh random values from the same seeded RNG stream, so the printed output is a tuple of N numeric traces. Larger N lowers the chance of two unrelated circuits sharing a fingerprint. Default 1.')
    parser.add_argument('-symbeigencheck', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Exact symbolic check that L and R from A;S;B = C;S;D have equal characteristic polynomials (i.e. matching eigenvalues with multiplicity). Use after the probabilistic -bigcheck with -seed.')
    parser.add_argument('-solve', nargs=2, metavar=('C1_JSON', 'C2_JSON'),
                        help='Solve A;S;B = C;S;D for S. Takes two circuit JSON strings as input.')
    parser.add_argument('circuit_json', nargs='?', default=None,
                        help='The input circuit as a JSON string.')
    parser.add_argument('-eigencheck', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Solve A;S;B = C;S;D -> L;S = S;R, check eigen(L) = eigen(R)')
    parser.add_argument('-bigcheck', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Solve A;S;B = C;S;D -> L;S = S;R, check trace(L) = trace(R), Det(T) = Det(R), trace(L^2) = trace(R^2)')
    parser.add_argument('-distincteigen', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Returns True iff L has all distinct eigenvalues at a random concrete sample. Use to filter out structurally simple intertwiner cases.')
    parser.add_argument('-eigenfp', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Return a string fingerprint of L\'s eigenvalues at a random concrete sample. Use for cheap bucket-grouping before solving full intertwiner.')
    parser.add_argument('-singleeigenfp', nargs=1, metavar=('C_JSON'), help='Per-circuit eigenvalue fingerprint at a random concrete sample (SYMB treated as identity). For pre-bucket grouping inside trace buckets.')
    parser.add_argument('-multieigenfp', nargs=1, metavar=('CS_JSON'), help='Batched eigen fingerprints: {"circuits":[...]}; one fingerprint line per circuit at -ntraces seeded samples. Replaces per-circuit trace/eigen IPC round-trips.')
    parser.add_argument('-multitracefp', nargs=1, metavar=('CS_JSON'), help='Batched TRACE fingerprints (cheap first-stage grouping before the eigen fingerprint).')
    parser.add_argument('-islinear', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='check that a circuit is in linear combination of basis')
    parser.add_argument('-is_subspace_linear', nargs=4, metavar=('C1_JSON', 'C2_JSON', 'SUBSPACE_JSON', 'SYMBOL_MAP_JSON'), help='check that a circuit is in linear combination of basis')
    parser.add_argument('-approx_eps', type=float, default=None, help='approximate-match tolerance for -is_subspace_linear (least-squares residual threshold; default: exact 1e-6)')
    parser.add_argument('-check_rule_not_affect_other', nargs=3, metavar=('CIRCUIT_JSON', 'L_JSON', 'QUBIT_TO_CHECK'), help='Check that a rule does not affect other parts of the circuit')
    args = parser.parse_args(argv)

    if args.eigenvals:
        circuit_json = args.eigenvals[0]
        circuit = json.loads(circuit_json)
        for op in circuit["gates"]:
            if 'params' in op:
                op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}
        circuit_matrix = calculate_circuit_matrix(circuit["n_qubits"], circuit["gates"])
        try:
            res = sorted(list(circuit_matrix.eigenvals().items()), key=lambda x: str(x[0]))
            print(res)
        except Exception as e:
            print(e)
        return

    if args.trace:
        circuit_json = args.trace[0]
        circuit = json.loads(circuit_json)
        for op in circuit["gates"]:
            if 'params' in op:
                op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}
        circuit_matrix = calculate_circuit_matrix(circuit["n_qubits"], circuit["gates"])
        try:
            res = circuit_matrix.trace()
            if args.seed is not None:
                seed = int(args.seed[0])
                ntraces = int(args.ntraces[0]) if args.ntraces is not None else 1
                rng = numpy.random.default_rng(seed)
                parts = []
                for _ in range(ntraces):
                    sub = {
                        theta1: float(rng.uniform(0.0, 2.0 * numpy.pi)),
                        theta2: float(rng.uniform(0.0, 2.0 * numpy.pi)),
                        theta3: float(rng.uniform(0.0, 2.0 * numpy.pi)),
                        phi:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
                        lam:    float(rng.uniform(0.0, 2.0 * numpy.pi)),
                        gamma:  float(rng.uniform(0.0, 2.0 * numpy.pi)),
                    }
                    numeric = complex(sympy.N(res.subs(sub)))
                    parts.append(f"({round(numeric.real, 10) + 0.0}{'+' if numeric.imag >= 0 else '-'}{round(abs(numeric.imag), 10) + 0.0}j)")
                print(",".join(parts))
            else:
                print(res)
        except Exception as e:
            print(e)
        return

    if args.is_subspace_linear:
        res = is_subspace_linear_combination(args.is_subspace_linear[0], args.is_subspace_linear[1], args.is_subspace_linear[2], args.is_subspace_linear[3], eps=args.approx_eps)
        print(res)
        return

    if args.islinear:
        circuit_json = args.islinear[0]
        sparse_basis = args.islinear[1]
        circuit = json.loads(circuit_json)
        circuit_matrix = calculate_circuit_matrix(circuit["n_qubits"], circuit["gates"])
        print(circuit_matrix)
        try:
            res = is_linear_combination(circuit_matrix, sparse_to_basis(sparse_basis))
            print(res)
            return
        except Exception as e:
            print(e)
            print(sparse_basis)
            return

    if args.tracecheck:
        res = solve_trace(args.tracecheck[0], args.tracecheck[1])
        print(res)
        return

    if args.eigencheck:
        res = solve_eigen(args.eigencheck[0], args.eigencheck[1])
        print(res)
        return

    if args.symbeigencheck:
        try:
            res = solve_eigen_symbolic_check(args.symbeigencheck[0], args.symbeigencheck[1])
            print(res)
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        return

    if args.bigcheck:
        try:
            if args.seed is not None:
                seed = int(args.seed[0])
                ntraces = int(args.ntraces[0]) if args.ntraces is not None else 1
                res = solve_big_check_eigen(args.bigcheck[0], args.bigcheck[1], seed, ntraces)
            else:
                res = solve_big_check(args.bigcheck[0], args.bigcheck[1])
            print(res)
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        return

    if args.distincteigen:
        try:
            seed = int(args.seed[0]) if args.seed is not None else 0
            res = all_distinct_eigen(args.distincteigen[0], args.distincteigen[1], seed)
            print(res)
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        return

    if args.eigenfp:
        try:
            seed = int(args.seed[0]) if args.seed is not None else 0
            res = eigenvalue_fingerprint(args.eigenfp[0], args.eigenfp[1], seed)
            print(res)
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        return

    if args.singleeigenfp:
        try:
            seed = int(args.seed[0]) if args.seed is not None else 0
            res = single_circuit_eigen_fingerprint(args.singleeigenfp[0], seed)
            print(res)
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        return

    if args.multieigenfp:
        try:
            seed = int(args.seed[0]) if args.seed is not None else 0
            ntraces = int(args.ntraces[0]) if args.ntraces is not None else 1
            print(multi_circuit_eigen_fingerprint(args.multieigenfp[0], seed, ntraces))
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        return

    if args.multitracefp:
        try:
            seed = int(args.seed[0]) if args.seed is not None else 0
            ntraces = int(args.ntraces[0]) if args.ntraces is not None else 1
            print(multi_circuit_trace_fingerprint(args.multitracefp[0], seed, ntraces))
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)
        return

    if args.solve:
        solve_intertwiner_equation(args.solve[0], args.solve[1])
        return

    if args.check_rule_not_affect_other:
        res = check_rule_not_affect_other_qubits(args.check_rule_not_affect_other[0], args.check_rule_not_affect_other[1], args.check_rule_not_affect_other[2])
        print(res)
        return

    if args.circuit_json:
        circuit = json.loads(args.circuit_json)

        if not circuit:
            print("")
            return

        max_qubit = circuit["n_qubits"]
        for op in circuit["gates"]:
            if 'params' in op:
                op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}

        matrix = calculate_circuit_matrix(max_qubit, circuit["gates"])
    else:
        print("--- Quantum Gate Semantics and Embedding Test ---")

        print("\n--- Test 1: SX Gate (sqrt(X)) ---")
        sx_gate = gate_semantics['sx']
        sympy.pprint(sx_gate)

        print("\n--- Test 2: U3(theta, phi, lam) Gate ---")
        u3_gate = gate_semantics['u3']
        sympy.pprint(u3_gate)

        print("\n--- Test 3: U3(pi/2, pi/2, pi) ---")
        u3_subbed = u3_gate.subs({theta1: sympy.pi/2, phi: sympy.pi/2, lam: sympy.pi})
        sympy.pprint(u3_subbed.evalf())

        print("\n--- Test 4: H gate on qubit 2 in a 3-qubit system ---")
        h_gate = gate_semantics['h']
        h_on_q2_of_3 = embed_operator(n_qubits=3, gate_matrix=h_gate, targets=[2])
        print("Shape:", h_on_q2_of_3.shape)
        sympy.pprint(h_on_q2_of_3)

        print("\n--- Test 4a: Rx(theta) on qubit 0 ---")
        rz_gate = gate_semantics['rz']
        rz_2_0 = embed_operator(n_qubits=2, gate_matrix=rz_gate, targets=[0])
        print("Shape:", rz_2_0.shape)
        sympy.pprint(rz_2_0)

        print("\n--- Test 5: CX on qubits [0, 2] in a 3-qubit system (control=0, target=2) ---")
        cx_gate = gate_semantics['cx']
        cx_02_of_3 = embed_operator(n_qubits=3, gate_matrix=cx_gate, targets=[0, 2])
        print("Shape:", cx_02_of_3.shape)
        sympy.pprint(cx_02_of_3)

        print("\n--- Test 6: RXX(pi/2) on qubits [1, 2] in a 3-qubit system ---")
        rxx_gate = gate_semantics['rxx'].subs({theta1: sympy.pi/2})
        rxx_12_of_3 = embed_operator(n_qubits=3, gate_matrix=rxx_gate, targets=[1, 2])
        print("Shape:", rxx_12_of_3.shape)
        sympy.pprint(rxx_12_of_3.evalf(chop=True))

        print("\n--- Test 7: Bell state preparation circuit H(0)CX(0,1) ---")
        bell_circuit = [
            {'gate': 'h', 'targets': [0]},
            {'gate': 'cx', 'targets': [0, 1]}
        ]
        bell_matrix = calculate_circuit_matrix(n_qubits=2, circuit=bell_circuit)
        print("Shape:", bell_matrix.shape)
        sympy.pprint(bell_matrix)

        print("\n--- Test 8: Eigenvalue collision test for X and Z gates ---")
        x_gate = gate_semantics['x']
        z_gate = gate_semantics['z']
        are_colliding = have_same_eigenvalues(x_gate, z_gate)
        print(f"Do X and Z gates have the same eigenvalues? {are_colliding}")

        h_gate = gate_semantics['h']
        print("\n--- Test 8b: Eigenvalue collision test for X and H gates ---")
        are_colliding_xh = have_same_eigenvalues(x_gate, h_gate)
        print(f"Do X and H gates have the same eigenvalues? {are_colliding_xh}")

        print("\n--- Test 8c: Eigenvalue collision test for RX(theta) and RZ(gamma) ---")
        rx_gate = gate_semantics['rx']
        rz_gate = gate_semantics['rz']
        are_colliding_rx_rz = have_same_eigenvalues(rx_gate, rz_gate)
        print(f"Do RX(theta) and RZ(gamma) have the same eigenvalues? {are_colliding_rx_rz}")

        print("\n--- Test 8d: Eigenvalue collision test for RX(theta) and RZ(theta) ---")
        rz_gate_theta = gate_semantics['rz'].subs({gamma: theta1})
        are_colliding_rx_rz_theta = have_same_eigenvalues(rx_gate, rz_gate_theta)
        print(f"Do RX(theta) and RZ(theta) have the same eigenvalues? {are_colliding_rx_rz_theta}")

        print("\n--- Test 9: Linear combination test ---")
        h_gate = gate_semantics['h']
        x_gate = gate_semantics['x']
        z_gate = gate_semantics['z']

        is_combo, coeffs = is_linear_combination(h_gate, [x_gate, z_gate])

        if is_combo:
            print("H is a linear combination of X and Z.")
            print("Coefficients:")
            sympy.pprint(coeffs)
        else:
            print("H is NOT a linear combination of X and Z.")

        i_gate = gate_semantics['i']
        is_combo_no, _ = is_linear_combination(z_gate, [i_gate, x_gate])

        if is_combo_no:
            print("\nZ is a linear combination of I and X.")
        else:
            print("\nZ is NOT a linear combination of I and X.")

        circuit = [
            {'gate': 'rz', 'targets': [0], 'params':{gamma: theta1}},
            {'gate': 'rz', 'targets': [0], 'params':{gamma: theta2}}
            ]
        L = calculate_circuit_matrix(2, circuit)
        circuit2 = [
            {'gate':'rz', 'targets': [0], 'params':{gamma: theta1 + theta2}}
        ]

        R = calculate_circuit_matrix(2, circuit2)
        print(L.eigenvals())
        print(R.eigenvals())
        print(L.eigenvects())
        print(R.eigenvects())

        basis, _ = intertwiner_basis(L, R)
        for base in basis:
            sympy.pprint(base)

        S_param, eqs = unitary_solutions_from_basis(basis)
        sympy.pprint(S_param)

        print("\nUnitary constraint equations:")
        for eq in eqs:
            sympy.pprint(eq)

        circuit = [
            {'gate': 'x', 'targets': [0]},
            ]
        L = calculate_circuit_matrix(1, circuit)
        circuit2 = [
            {'gate':'z', 'targets': [0]}
        ]
        R = calculate_circuit_matrix(1, circuit2)
        print(L.eigenvals())
        print(R.eigenvals())
        print(L.eigenvects())
        print(R.eigenvects())
        basis, _ = intertwiner_basis(L, R)
        for base in basis:
            sympy.pprint(base)

        print("\n--- Test 12: Solve A;S;B = C;S;D ---")
        circuit1_json = '''
        {
            "n_qubits": 2,
            "gates": [
                {"gate": "rz", "targets": [0], "params":{"gamma":"theta1"}},
                {"gate": "symb", "targets": [0, 1]},
                {"gate": "rz", "targets": [1],"params":{"gamma":"theta2"}}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 2,
            "gates": [
                {"gate": "symb", "targets": [0, 1]},
                {"gate": "rz", "targets": [1], "params":{"gamma":"theta1+theta2"}}
            ]
        }
        '''

        solve_intertwiner_equation(circuit1_json, circuit2_json)

        print("\n--- Test 13: Endianness Check ---")
        s_gate = gate_semantics['s']

        print("\nCalling embed_operator(n_qubits=2, gate_matrix=S, targets=[0])")
        s_on_q0 = embed_operator(n_qubits=2, gate_matrix=s_gate, targets=[0])
        sympy.pprint(s_on_q0)

        print("\nCalling embed_operator(n_qubits=2, gate_matrix=S, targets=[1])")
        s_on_q1 = embed_operator(n_qubits=2, gate_matrix=s_gate, targets=[1])
        sympy.pprint(s_on_q1)

        i_gate = gate_semantics['i']
        i_tensor_s = sympy.kronecker_product(i_gate, s_gate)
        s_tensor_i = sympy.kronecker_product(s_gate, i_gate)

        print("\nExpected matrix for I ⊗ S (Little-Endian for target 0):")
        sympy.pprint(i_tensor_s)

        print("\nExpected matrix for S ⊗ I (Little-Endian for target 1):")
        sympy.pprint(s_tensor_i)

        print("\n--- Test 14: intertwiner_basis with L=R=I ---")
        i_gate = gate_semantics['i']
        basis_I, _ = intertwiner_basis(i_gate, i_gate)
        print(f"Basis found for S in I*S = S*I. Number of basis elements: {len(basis_I)}")
        for b in basis_I:
            sympy.pprint(b)

        print("\n--- Test 15: intertwiner_basis with L=R=Z ---")
        z_gate = gate_semantics['z']
        basis_Z, _ = intertwiner_basis(z_gate, z_gate)
        print(f"Basis found for S in Z*S = S*Z. Number of basis elements: {len(basis_Z)}")
        for b in basis_Z:
            sympy.pprint(b)

        print("\n--- Test 16: solve_intertwiner_equation for X*S*X = S ---")
        circuit1_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "x", "targets": [0]},
                {"gate": "symb", "targets": [0]},
                {"gate": "x", "targets": [0]}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        try:
            solve_intertwiner_equation(circuit1_json, circuit2_json)
        except Exception as e:
            print(f"Error during Test 16: {e}", file=sys.stderr)

        print("\n--- Test 17: intertwiner_basis with complex result ---")
        y_gate = gate_semantics['y']
        z_gate = gate_semantics['z']

        L = y_gate
        R = -z_gate

        print("Solving Y*S = S*(-Z)")
        basis_complex, _ = intertwiner_basis(L, R)

        print(f"Basis found. Number of basis elements: {len(basis_complex)}")
        for b in basis_complex:
            print(b)

        print("\n--- Test 18: intertwiner_basis with more complex numbers ---")
        x_gate = gate_semantics['x']
        h_gate = gate_semantics['h']

        L = x_gate @ h_gate
        R = h_gate @ x_gate

        print("Solving (X*H)*S = S*(H*X)")
        basis_complex_2, _ = intertwiner_basis(L, R)

        print(f"Basis found. Number of basis elements: {len(basis_complex_2)}")
        for b in basis_complex_2:
            print(b)

        print("\n--- Test 19: intertwiner_basis with irrational numbers ---")

        L = h_gate
        R = x_gate

        print("Solving H*S = S*X")
        basis_irrational, _ = intertwiner_basis(L, R)

        print(f"Basis found. Number of basis elements: {len(basis_irrational)}")
        for b in basis_irrational:
            print(b)

        print("\n--- Test 20: Check for monomial solution for X*S = S*Z ---")

        L = x_gate
        R = z_gate

        print("Solving X*S = S*Z")
        basis, _ = intertwiner_basis(L, R)

        print(f"Basis found. Number of basis elements: {len(basis)}")
        print("Basis matrices are:")
        for b in basis:
            print(b)

        print("\n--- Test 21: Find multiple unitary solutions from a basis ---")
        print("Solving for unitary S in Z*S = S*Z")
        L = z_gate
        R = z_gate
        basis, _ = intertwiner_basis(L, R)

        print("\nBasis found for S:")
        for b in basis:
            print(b)

        S_param, eqs = unitary_solutions_from_basis(basis)

        print("\nParameterized solution S based on basis:")
        sympy.pprint(S_param)

        print("\nEquations for coefficients to enforce S*S_dagger = I:")
        for eq in eqs:
            sympy.pprint(eq)

        print("\nSolving the equations for the coefficients gives:")
        print("|c0|^2 = 1  and  |c1|^2 = 1")
        print("This confirms that multiple (in fact, infinite) unitary solutions can be formed from the basis.")

        print("\n--- Test 23: Trace equality check ---")

        z_gate = gate_semantics['z']
        x_gate = gate_semantics['x']
        i_gate = gate_semantics['i']
        rz_gate = gate_semantics['rz']
        rx_gate = gate_semantics['rx']
        rz_gate_sub = rz_gate.subs({gamma: theta1})

        print(f"Do Z and X have the same trace? {have_same_trace(z_gate, x_gate)}")

        print(f"Do Z and I have the same trace? {have_same_trace(z_gate, i_gate)}")

        print(f"Do RZ(theta1) and RX(theta1) have the same trace? {have_same_trace(rz_gate_sub, rx_gate)}")

        print("\n--- Test 24: solve_trace and solve_eigen tests ---")

        circuit1_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "h", "targets": [0]},
                {"gate": "symb", "targets": [0]},
                {"gate": "h", "targets": [0]}
            ]
        }
        '''
        print(f"solve_trace test (pass expected): {solve_trace(circuit1_json, circuit2_json)}")

        circuit1_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "x", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "z", "targets": [0]}
            ]
        }
        '''
        print(f"solve_trace test (fail expected): {not solve_trace(circuit1_json, circuit2_json)}")

        circuit1_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "x", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "z", "targets": [0]}
            ]
        }
        '''
        print(f"solve_eigen test (pass expected): {solve_eigen(circuit1_json, circuit2_json)}")

        circuit1_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "h", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "x", "targets": [0]}
            ]
        }
        '''
        print(f"solve_eigen test (fail expected): {not solve_eigen(circuit1_json, circuit2_json)}")

        print("\n--- Test 25: Determinant equality check ---")

        z_gate = gate_semantics['z']
        x_gate = gate_semantics['x']
        h_gate = gate_semantics['h']
        s_gate = gate_semantics['s']

        print(f"Do Z and X have the same determinant? {have_same_determinant(z_gate, x_gate)}")

        print(f"Do H and X have the same determinant? {not have_same_determinant(h_gate, x_gate)}")

        print(f"Do S and Z have the same determinant? {not have_same_determinant(s_gate, z_gate)}")

        print("\n--- Test 26: Big check test ---")

        circuit1_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "x", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "z", "targets": [0]}
            ]
        }
        '''
        print(f"solve_big_check test (pass expected): {solve_big_check(circuit1_json, circuit2_json)}")

        circuit1_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "s", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        circuit2_json = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "z", "targets": [0]}
            ]
        }
        '''
        print(f"solve_big_check test (fail expected): {not solve_big_check(circuit1_json, circuit2_json)}")

        print("\n--- Test 24b: solve_big_check_eigen tests ---")
        eq_circuit = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "x", "targets": [0]},
                {"gate": "symb", "targets": [0]},
                {"gate": "x", "targets": [0]}
            ]
        }
        '''
        eq_pass = solve_big_check_eigen(eq_circuit, eq_circuit, seed=42, ntraces=5)
        print(f"solve_big_check_eigen identical (pass expected): {eq_pass}")
        assert eq_pass is True
        x_circuit = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "x", "targets": [0]}
            ]
        }
        '''
        s_circuit = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "s", "targets": [0]}
            ]
        }
        '''
        neq_pass = solve_big_check_eigen(x_circuit, s_circuit, seed=42, ntraces=5)
        print(f"solve_big_check_eigen X vs S (fail expected): {not neq_pass}")
        assert neq_pass is False
        rep1 = solve_big_check_eigen(eq_circuit, eq_circuit, seed=123, ntraces=3)
        rep2 = solve_big_check_eigen(eq_circuit, eq_circuit, seed=123, ntraces=3)
        print(f"solve_big_check_eigen seed reproducibility: {rep1 == rep2 == True}")
        assert rep1 == rep2 == True

        print("\n--- Test 24c: solve_eigen_symbolic_check tests ---")
        sc_pass = solve_eigen_symbolic_check(eq_circuit, eq_circuit)
        print(f"solve_eigen_symbolic_check identical (pass expected): {sc_pass}")
        assert sc_pass is True
        sc_fail = solve_eigen_symbolic_check(x_circuit, s_circuit)
        print(f"solve_eigen_symbolic_check X vs S (fail expected): {not sc_fail}")
        assert sc_fail is False
        rz_left = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "rz", "targets": [0], "params": {"theta1": "theta1"}},
                {"gate": "symb", "targets": [0]}
            ]
        }
        '''
        rz_right = '''
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "rz", "targets": [0], "params": {"theta1": "theta1"}}
            ]
        }
        '''
        sc_param = solve_eigen_symbolic_check(rz_left, rz_right)
        print(f"solve_eigen_symbolic_check RZ(theta1) before/after SYMB (pass expected): {sc_param}")
        assert sc_param is True

        print("\n--- Testing linear_span_test ---")

        print("Test 1: Empty basis")
        result = linear_span_test([])
        print(f"Expected: False, Got: {result}")
        assert result == False

        print("\nAll linear_span_test assertions passed!")

        print("\n--- Test 27: Eigenvalue of rx(theta2) q[0]; rxx(theta1+theta2) q[0], q[1] ---")
        circuit = [
            {'gate': 'rz', 'targets': [0], 'params': {gamma: sympy.pi}},
            {'gate': 'rx', 'targets': [0], 'params': {theta1: sympy.pi/2}}
        ]
        circuit_matrix = calculate_circuit_matrix(n_qubits=2, circuit=circuit)
        print("Shape:", circuit_matrix.shape)
        print("Eigenvalues:")
        try:
            print(circuit_matrix.trace())
        except Exception as e:
            print(f"Could not compute eigenvalues: {e}")

def serve():
    """Persistent-server mode (`semantics.py --server`).

    Reads one request per line from stdin, runs the same dispatch as main(),
    and writes one response line. Both request args and response are base64
    so the protocol is newline-safe. Started once by SymbolicSolve so the
    enumerator avoids a process spawn + sympy import on every call; results
    are memoized in-process, keyed by the request.
    """
    import base64, io, contextlib
    real_stdout = sys.stdout
    cache = {}
    for raw in sys.stdin:
        raw = raw.strip()
        if not raw:
            continue
        if raw == "SHUTDOWN":
            break
        if raw in cache:
            result = cache[raw]
        else:
            argv = [base64.b64decode(f).decode("utf-8") for f in raw.split("\t")]
            buf = io.StringIO()
            try:
                with contextlib.redirect_stdout(buf), contextlib.redirect_stderr(buf):
                    main(argv)
            except SystemExit:
                pass
            except Exception as e:
                buf.write(f"Error: {e}")
            result = buf.getvalue()
            if len(cache) > 2048:
                cache.clear()
            cache[raw] = result
        real_stdout.write(base64.b64encode(result.encode("utf-8")).decode("ascii"))
        real_stdout.write("\n")
        real_stdout.flush()

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--server":
        serve()
    else:
        main()
