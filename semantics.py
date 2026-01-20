import sympy
import sys
import json
import numpy

# Define symbolic variables for parameterized gates
theta1, theta2, theta3, phi, lam, gamma = sympy.symbols('theta1 theta2 theta3 phi lam gamma', real=True)


# Define the matrix semantics for common quantum gates using sympy
# This list is based on the gates found in EggGen.java, plus other standard gates.

param_symbol_map = {'theta1': theta1, 'theta2': theta2, 'theta3': theta3, 'phi': phi, 'lam': lam, 'gamma': gamma, 'pi': sympy.pi, 'pi/2': sympy.pi/2, 'pi/4': sympy.pi/4}
gate_semantics = {
    # === Standard 1-Qubit Gates ===
    'i': sympy.Matrix([[1, 0], [0, 1]]),
    'h': 1/sympy.sqrt(2) * sympy.Matrix([[1, 1], [1, -1]]),
    'x': sympy.Matrix([[0, 1], [1, 0]]),
    'y': sympy.Matrix([[0, -sympy.I], [sympy.I, 0]]),
    'z': sympy.Matrix([[1, 0], [0, -1]]),
    's': sympy.Matrix([[1, 0], [0, sympy.I]]),
    'sdg': sympy.Matrix([[1, 0], [0, -sympy.I]]),
    't': sympy.Matrix([[1, 0], [0, sympy.exp(sympy.I * sympy.pi / 4)]]),
    'tdg': sympy.Matrix([[1, 0], [0, sympy.exp(-sympy.I * sympy.pi / 4)]]),
    'sx': 1/2 * sympy.Matrix([[1+sympy.I, 1-sympy.I], [1-sympy.I, 1+sympy.I]]),

    # === Standard 2-Qubit Gates ===
    'cx': sympy.Matrix([
        [1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 0, 1], [0, 0, 1, 0]
    ]),
    'cz': sympy.Matrix([
        [1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 1, 0], [0, 0, 0, -1]
    ]),
    'swap': sympy.Matrix([
        [1, 0, 0, 0], [0, 0, 1, 0], [0, 1, 0, 0], [0, 0, 0, 1]
    ]),

    # === Parameterized 1-Qubit Gates from EggGen.java ===
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
    # VZ is equivalent to RZ. We use the same definition.
    'vz': sympy.Matrix([
        [sympy.exp(-sympy.I * theta1 / 2), 0],
        [0, sympy.exp(sympy.I * theta1 / 2)]
    ]),

    # === Parameterized 2-Qubit Gates from EggGen.java ===
    'rxx': sympy.Matrix([
        [sympy.cos(theta1/2), 0, 0, -sympy.I*sympy.sin(theta1/2)],
        [0, sympy.cos(theta1/2), -sympy.I*sympy.sin(theta1/2), 0],
        [0, -sympy.I*sympy.sin(theta1/2), sympy.cos(theta1/2), 0],
        [-sympy.I*sympy.sin(theta1/2), 0, 0, sympy.cos(theta1/2)]
    ]),
    # The MS gate has multiple definitions. The one in EggGen.java has two
    # parameters (phi1, phi2), which is ambiguous. A common MS gate is
    # equivalent to RXX(pi/2). We provide the RXX definition here as 'ms'
    # for a general Mølmer–Sørensen-like interaction.
    'ms': sympy.Matrix([
        [sympy.cos(theta1/2), 0, 0, -sympy.I*sympy.sin(theta1/2)],
        [0, sympy.cos(theta1/2), -sympy.I*sympy.sin(theta1/2), 0],
        [0, -sympy.I*sympy.sin(theta1/2), sympy.cos(theta1/2), 0],
        [-sympy.I*sympy.sin(theta1/2), 0, 0, sympy.cos(theta1/2)]
    ]),
}
# Alias for CNOT
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

    # 1. Create permutation mapping from target layout to initial qubits
    pi = list(targets) + sorted(list(set(range(n_qubits)) - set(targets)))

    # 2. Create the permutation matrix
    dim = 2**n_qubits
    perm_matrix = sympy.zeros(dim, dim)
    for i in range(dim):
        in_basis_str = f'{i:0{n_qubits}b}'
        out_basis_str = ''.join([in_basis_str[pi[j]] for j in range(n_qubits)])
        j = int(out_basis_str, 2)
        perm_matrix[j, i] = 1
        
    # 3. The core operator is U_k (tensor) I_{n-k}
    if n_qubits - k > 0:
        identity_part = sympy.eye(2**(n_qubits - k))
        tensor_res = sympy.kronecker_product(gate_matrix, identity_part)
        core_op = sympy.simplify(sympy.Matrix(tensor_res))
    else:
        core_op = gate_matrix

    # 4. The full operator is P_inv * (U_k (tensor) I) * P
    # For permutation matrices, P_inv = P.T
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

    # A map from the bit position in the k-qubit system to the qubit index in the n-qubit system.
    # The order is assumed from MSB to LSB for the k-qubit system.
    qubit_map = {i: q for i, q in enumerate(qubits_to_keep)}

    for i in range(projected_dim):  # Corresponds to the i-th basis state of the k-qubit system
        for j in range(projected_dim):  # Corresponds to the j-th basis state of the k-qubit system
            
            # Construct the n-qubit index for the row, assuming other qubits are |0>
            row_idx = 0
            for bit_pos in range(k):
                if (i >> (k - 1 - bit_pos)) & 1:
                    row_idx |= (1 << qubit_map[bit_pos])

            # Construct the n-qubit index for the column
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

    # Gates are applied in order, so we process the list from start to end
    for op in circuit:
        gate_name = op['gate']
        targets = op['targets']
        
        if gate_name not in gate_semantics:
            raise ValueError(f"Gate '{gate_name}' not found in gate_semantics.")
            
        gate_matrix = gate_semantics[gate_name]

        # Substitute parameters if they are provided
        if 'params' in op:
            gate_matrix = gate_matrix.subs(op['params'])

        # Embed the operator into the n-qubit space
        op_matrix = embed_operator(n_qubits, gate_matrix, targets)

        # The total transformation is U_final * ... * U_2 * U_1.
        # So we left-multiply the current circuit matrix by the new operator.
        circuit_matrix = op_matrix @ circuit_matrix

    return circuit_matrix


def intertwiner_basis(L, R):
    """
    Return a list of basis matrices {S_k} spanning all solutions to L S = S R
    (no unitarity enforced here; purely the linear intertwiner space).
    Works with numeric or symbolic entries.
    """
    n = L.shape[0]
    K = sympy.kronecker_product(L.T, sympy.eye(n)) - sympy.kronecker_product(sympy.eye(n), R)
    null = K.nullspace()
    
    basis = [sympy.Matrix(n, n, v) for v in null]  # reshape vec -> n×n
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
    
    # Simplify the difference to see if it is zero
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
    
    # Simplify the difference to see if it is zero
    if sympy.simplify(det1 - det2) == 0:
        return True
    else:
        return False

def compute_L_R(circuit1_json, circuit2_json):
    circuit1 = json.loads(circuit1_json)
    circuit2 = json.loads(circuit2_json)
    
    n_qubits1 = circuit1.get("n_qubits")
    n_qubits2 = circuit2.get("n_qubits")

    if n_qubits1 is None or n_qubits2 is None:
        raise ValueError("Both circuits must specify 'n_qubits'.")

    if n_qubits1 != n_qubits2:
        raise ValueError("The two circuits must have the same number of qubits.")
    
    n_qubits = n_qubits1
    
    gates1 = circuit1.get("gates", [])
    for op in gates1:
        if 'params' in op:
            op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}
            
    gates2 = circuit2.get("gates", [])
    for op in gates2:
        if 'params' in op:
            op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}
    

    # Find the symbolic gate in each circuit
    try:
        symb_index1 = [op['gate'] for op in gates1].index('symb')
    except ValueError:
        raise ValueError("Circuit 1 (LHS) does not contain a 'symb' gate.")

    try:
        symb_index2 = [op['gate'] for op in gates2].index('symb')
    except ValueError:
        raise ValueError("Circuit 2 (RHS) does not contain a 'symb' gate.")

    # Split circuits into parts before and after the symbolic gate
    pre_symb1_gates = gates1[:symb_index1]
    post_symb1_gates = gates1[symb_index1+1:]

    pre_symb2_gates = gates2[:symb_index2]
    post_symb2_gates = gates2[symb_index2+1:]

    # A;S;B form for circuit 1
    A = calculate_circuit_matrix(n_qubits, pre_symb1_gates)
    B = calculate_circuit_matrix(n_qubits, post_symb1_gates)

    # C;S;D form for circuit 2
    C = calculate_circuit_matrix(n_qubits, pre_symb2_gates)
    D = calculate_circuit_matrix(n_qubits, post_symb2_gates)

    # We are solving A; S; B = C; S; D
    # THe applying order is [B] [S] [A] = [D] [S] [C]
    # This is equivalent to [S] [A][C]^-1 =  [B]^-1 [D] [S]
    # Let L = [A][C]^-1  R = [B]^-1 [D]
    # Since C and B are unitary, their inverse is their conjugate transpose (dagger)
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

    # Construct the symbolic linear combination
    linear_combination = sympy.zeros(rows, cols)
    for i in range(num_matrices):
        linear_combination += coeffs[i] * list_of_matrices_a[i]
    print("linear_combination: " + str(linear_combination))
    # Set up the system of equations M = linear_combination
    equations = []
    for r in range(rows):
        for c in range(cols):
            equations.append(sympy.Eq(matrix_m[r, c], linear_combination[r, c], evaluate=False))

    # Solve for the coefficients

    solution = sympy.linsolve(equations, coeffs)
    if solution:
        # If solution is a list of solutions, take the first one.
        if isinstance(solution, list):
            solution = solution[0]
        return True, solution
    else:
        return False, None
    

def solve_intertwiner_equation(circuit1_json, circuit2_json, output_file=None):
    """
    Solves the intertwiner equation A;S;B = C;S;D for S.
    LHS is circuit1, RHS is circuit2.
    Each circuit must contain exactly one 'symb' gate.
    The equation is transformed to L*S = S*R and solved for S.
    """
    #with open(output_file, 'w') as f:
        #basis_list = []
        # for circuit1_json, circuit2_json in zip(circuit1_jsons, circuit2_jsons):
    L,R = compute_L_R(circuit1_json, circuit2_json)

    basis, _ = intertwiner_basis(L, R)
    
    if not basis:
        print("No non-trivial solutions found.")
    else:
        # if output_file:
        #     serializable_basis = []
        #     for b in basis:
        #         matrix_as_lol = [[str(e) for e in row] for row in b.tolist()]
        #         serializable_basis.append(matrix_as_lol)
        #     basis_list.append(serializable_basis)
        # else:
        for i, b in enumerate(basis):
            print(b)
        #json.dump(basis_list, f)
        

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
    print("sparse_matrices: ", sparse_matrices)
    ms = json.loads(sparse_matrices)
    if symbol_map:
        symbol_map = json.loads(symbol_map)

    if symbol_map:
        symbol_map = {param_symbol_map[k]: param_symbol_map[v] if isinstance(v, str) else v for k, v in symbol_map.items()}
    print(symbol_map)
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
            print("substituted matrix" + str(M))
        matrices.append(M)
    return matrices



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
    # create a list of vectors for all the qubits that is premutation of all the qubits = 0 or 1
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
                                           

def is_subspace_linear_combination(circuit_json, sparse_basis, qubits_to_check, symbol_map=None):
    """
    Checks if the circuit's transformation on a subset of qubits is a linear combination of a given basis.
    """
    circuit = json.loads(circuit_json)
    n_qubits = circuit["n_qubits"]
    qubits_to_check = json.loads(qubits_to_check)
    for op in circuit['gates']:
        if 'params' in op:
            print("params: " + str(op['params']))
            op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): sympy.sympify(v, locals=param_symbol_map) for k, v in op['params'].items()}
            
    
    # Calculate the matrix for the subspace
    subspace_matrices = calculate_subspace_circuit_matrix(n_qubits, circuit["gates"], qubits_to_check)
    #print("subspace_matrix: " + str(subspace_matrix))
    if not subspace_matrices:
        return False
    # The basis is already defined on the subspace, so no projection is needed.
    basis_matrices = sparse_to_basis(sparse_basis, symbol_map)
    print("basis_matrices: " + str(basis_matrices))
    for sm in subspace_matrices:
        is_combo, _ = is_linear_combination(sm, basis_matrices)
        if(not is_combo):
            return False
    return True



# def verify_subspace_linear_combination(circuit_json, sparse_basis, qubits_to_check, symbol_map=None):
#     """
#     Verifies that the circuit's transformation on a subset of qubits is a linear combination of a given basis.
#     """
#     circuit = json.loads(circuit_json)
#     n_qubits = circuit["n_qubits"]
#     qubits_to_check = json.loads(qubits_to_check)
#     basis_matrices = sparse_to_basis(sparse_basis, symbol_map)
#     for op in circuit['gates']:
#         if 'params' in op:
#             print("params: " + str(op['params']))
#             op['params'] = {param_symbol_map.get(k, sympy.Symbol(k)): (param_symbol_map.get(v, sympy.Symbol(v)) if isinstance(v, str) else v) for k, v in op['params'].items()}
    
    
#     # for all qubits values in qubits to check, first for each combination of qubit value, generate a qubit map
#     # then calculate the circuit matrix for the subspace
#     #term map for big endian encoding
#     circuit_matrix = calculate_circuit_matrix(n_qubits, circuit["gates"])
#     number_of_terms = 2 ** n_qubits;
#     term_map = [[0 for _ in range(n_qubits)] for _ in range(number_of_terms)]
#     for i in range(number_of_terms):
#         for j in range(n_qubits):
#             term_map[i][n_qubits - j - 1] = ((j * number_of_terms + i) >> j) & 1
#     print("term_map: " + str(term_map))
    
    
#     rows, cols = basis_matrices[0].shape
#     for A in basis_matrices:
#         if A.shape != (rows, cols):
#             raise ValueError("All matrices must have the same dimensions.")

#     num_matrices = len(basis_matrices)
#     coeffs = sympy.symbols(f'c_:{num_matrices}')

#     # Construct the symbolic linear combination
#     symbolic_matrix = sympy.zeros(rows, cols)
#     for i in range(num_matrices):
#         symbolic_matrix += coeffs[i] * basis_matrices[i]

#     print("symbolic_matrix: " + str(symbolic_matrix))
#     for i, term_mapping in enumerate(term_map):
#         vector_representation = [0 for _ in range(2 ** n_qubits)]
#         vector_representation[i] = 1
#         original_vector_two_qubit = [0 for _ in range(4)]
#         if term_mapping[0] == 0 and term_mapping[1] == 0:
#             original_vector_two_qubit[0] = 1
#         elif term_mapping[0] == 0 and term_mapping[1] == 1:
#             original_vector_two_qubit[1] = 1
#         elif term_mapping[0] == 1 and term_mapping[1] == 0:
#             original_vector_two_qubit[2] = 1
#         elif term_mapping[0] == 1 and term_mapping[1] == 1:
#             original_vector_two_qubit[3] = 1
#         print("vector_representation: " + str(vector_representation))
#         original_vector_two_qubit_matrix = sympy.Matrix(original_vector_two_qubit)
#         print("circuit_matrix: " + str(circuit_matrix))
#         res = circuit_matrix @ sympy.Matrix(vector_representation)
#         print("res: " + str(res))
#         path_sum_list = []
#         for j in range(res.rows):
#             phi = res[j]
#             term_mapping_res = term_map[j]
#             path_sum_list.append((phi, term_mapping_res))
#         print("path_sum_list: " + str(path_sum_list))
#         phi00 = 0
#         phi01 = 0
#         phi10 = 0
#         phi11 = 0
#         for phi, term_mapping_res in path_sum_list:
#             if term_mapping_res[0] == 0 and term_mapping_res[1] == 0:
#                 phi00 += phi**2
#             elif term_mapping_res[0] == 0 and term_mapping_res[1] == 1:
#                 phi01 += phi**2
#             elif term_mapping_res[0] == 1 and term_mapping_res[1] == 0:
#                 phi10 += phi**2
#             elif term_mapping_res[0] == 1 and term_mapping_res[1] == 1:
#                 phi11 += phi**2
#         res_two_qubit_vector = sympy.Matrix([sympy.sqrt(phi00), sympy.sqrt(phi01), sympy.sqrt(phi10), sympy.sqrt(phi11)])
#         print(res_two_qubit_vector)

#         res_symbolic = symbolic_matrix @ original_vector_two_qubit_matrix
#         equations = []
#         for i in range(res_symbolic.rows):
#             equations.append(sympy.Eq(res_symbolic[i], res_two_qubit_vector[i], evaluate=False))
#         solution = sympy.linsolve(equations, coeffs)
#         if solution:
#             pass
#         else:
#             return False
#     return True


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
    # Create a mapping from original qubit index to subspace index
    qubit_map = {qubit: i for i, qubit in enumerate(qubits_to_check)}

    for op in circuit:
        gate_name = op['gate']
        targets = op['targets']
        
        # Check if the gate acts entirely within the subspace
        if all(t in qubits_to_check for t in targets):
            subspace_targets = [qubit_map[t] for t in targets]
            
            if gate_name not in gate_semantics:
                raise ValueError(f"Gate '{gate_name}' not found in gate_semantics.")
            
            gate_matrix = gate_semantics[gate_name]

            if 'params' in op:
                gate_matrix = gate_matrix.subs(op['params'])

            # Embed the operator in the k-qubit subspace
            op_matrix = embed_operator(k, gate_matrix, subspace_targets)
            
            temp_subspace_matrices = []
            for sm in subspace_matrices:
                new_sub = op_matrix @ sm
                temp_subspace_matrices.append(new_sub)
            subspace_matrices = temp_subspace_matrices

        # Handle CNOT with only control in subspace
        elif gate_name in ['cx', 'cnot'] and targets[0] in qubits_to_check and targets[1] not in qubits_to_check:
            # Control is in the subspace, target is not. This is an identity on the control qubit.
            pass
        # Handle CNOT with only target in subspace
        elif gate_name in ['cx', 'cnot'] and targets[1] in qubits_to_check and targets[0] not in qubits_to_check:
            # Target is in the subspace, control is not. Assume control is |0>, so Identity.
            # This is equivalent to checking both appending X gate and appending nothing to the subspace
            temp_subspace_matrices = []
            for sm in subspace_matrices:
                new_sub = gate_matrix['x'] @ sm
                temp_subspace_matrices.append(new_sub)
            subspace_matrices = temp_subspace_matrices

        elif all(t not in qubits_to_check for t in targets):
            pass # No operation
    print("subspace_matrices: " + str(subspace_matrices))
    return subspace_matrices



import numpy as np
import sympy

def linear_span_test(basis):
    if not basis:
        return False

    n = basis[0].rows
    # Convert each basis matrix to numeric array
    cols = []
    for b1 in basis:
        for b2 in basis:
            # evaluate and cast to float+complex
            C = (b1.H * b2).evalf()
            cols.append(np.array(C.tolist(), dtype=np.complex128).reshape(-1, 1))
    M = np.hstack(cols)                 # (n^2) × (m^2)
    b = np.eye(n).reshape(-1)           # vec(I)

    # Solve least-squares to see if consistent
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

import argparse
def main():
    parser = argparse.ArgumentParser(description='Quantum circuit semantics analysis.')
    parser.add_argument('-eigenvals', nargs=1, metavar=('C_JSON'),
                        help='Compute and print the eigenvalues of the input circuit matrix.')
    parser.add_argument('-tracecheck', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Solve A;S;B = C;S;D -> L;S = S;R, check trace(L) = trace(R)')
    parser.add_argument('-trace', nargs=1, metavar=('C1_JSON'), help='Compute and print the trace of the input circuit matrix.')
    parser.add_argument('-solve', nargs=2, metavar=('C1_JSON', 'C2_JSON'),
                        help='Solve A;S;B = C;S;D for S. Takes two circuit JSON strings as input.')
    parser.add_argument('circuit_json', nargs='?', default=None,
                        help='The input circuit as a JSON string.')
    parser.add_argument('-eigencheck', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Solve A;S;B = C;S;D -> L;S = S;R, check eigen(L) = eigen(R)')
    parser.add_argument('-bigcheck', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='Solve A;S;B = C;S;D -> L;S = S;R, check trace(L) = trace(R), Det(T) = Det(R), trace(L^2) = trace(R^2)')
    parser.add_argument('-islinear', nargs=2, metavar=('C1_JSON', 'C2_JSON'), help='check that a circuit is in linear combination of basis')
    parser.add_argument('-is_subspace_linear', nargs=4, metavar=('C1_JSON', 'C2_JSON', 'SUBSPACE_JSON', 'SYMBOL_MAP_JSON'), help='check that a circuit is in linear combination of basis')
    parser.add_argument('-check_rule_not_affect_other', nargs=3, metavar=('CIRCUIT_JSON', 'L_JSON', 'QUBIT_TO_CHECK'), help='Check that a rule does not affect other parts of the circuit')
    args = parser.parse_args()

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
            print(res)
        except Exception as e:
            print(e)
        return
    
    if args.is_subspace_linear:
        res = is_subspace_linear_combination(args.is_subspace_linear[0], args.is_subspace_linear[1], args.is_subspace_linear[2], args.is_subspace_linear[3])
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

    if args.bigcheck:
        try:
            res = solve_big_check(args.bigcheck[0], args.bigcheck[1])
            print(res)
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
        # Original main function for testing
        print("--- Quantum Gate Semantics and Embedding Test ---")

        # --- Test 1: Display a simple gate matrix ---
        print("\n--- Test 1: SX Gate (sqrt(X)) ---")
        sx_gate = gate_semantics['sx']
        sympy.pprint(sx_gate)

        # --- Test 2: Display a parameterized gate matrix ---
        print("\n--- Test 2: U3(theta, phi, lam) Gate ---")
        u3_gate = gate_semantics['u3']
        sympy.pprint(u3_gate)

        # --- Test 3: Substitute values into a parameterized gate ---
        print("\n--- Test 3: U3(pi/2, pi/2, pi) ---")
        u3_subbed = u3_gate.subs({theta1: sympy.pi/2, phi: sympy.pi/2, lam: sympy.pi})
        sympy.pprint(u3_subbed.evalf())

        # --- Test 4: Embed a 1-qubit gate in a 3-qubit circuit ---
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

        # --- Test 5: Embed a 2-qubit gate (CX) on non-adjacent qubits ---
        print("\n--- Test 5: CX on qubits [0, 2] in a 3-qubit system (control=0, target=2) ---")
        cx_gate = gate_semantics['cx']
        cx_02_of_3 = embed_operator(n_qubits=3, gate_matrix=cx_gate, targets=[0, 2])
        print("Shape:", cx_02_of_3.shape)
        sympy.pprint(cx_02_of_3)
        
        # --- Test 6: Embed a parameterized 2-qubit gate (RXX) ---
        print("\n--- Test 6: RXX(pi/2) on qubits [1, 2] in a 3-qubit system ---")
        rxx_gate = gate_semantics['rxx'].subs({theta1: sympy.pi/2})
        rxx_12_of_3 = embed_operator(n_qubits=3, gate_matrix=rxx_gate, targets=[1, 2])
        print("Shape:", rxx_12_of_3.shape)
        sympy.pprint(rxx_12_of_3.evalf(chop=True))


        # --- Test 7: Calculate matrix for a full circuit (Bell state) ---
        print("\n--- Test 7: Bell state preparation circuit H(0)CX(0,1) ---")
        bell_circuit = [
            {'gate': 'h', 'targets': [0]},
            {'gate': 'cx', 'targets': [0, 1]}
        ]
        bell_matrix = calculate_circuit_matrix(n_qubits=2, circuit=bell_circuit)
        print("Shape:", bell_matrix.shape)
        sympy.pprint(bell_matrix)

        # --- Test 8: Check for eigenvalue collision ---
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

        # --- Test 9: Linear combination test ---
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


        ## Test 10
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

        # Test 11:
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

        # --- Test 12: Solve for intertwiner ---
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

        # --- Test 13: Endianness check for embed_operator ---
        print("\n--- Test 13: Endianness Check ---")
        s_gate = gate_semantics['s']
        
        print("\nCalling embed_operator(n_qubits=2, gate_matrix=S, targets=[0])")
        s_on_q0 = embed_operator(n_qubits=2, gate_matrix=s_gate, targets=[0])
        sympy.pprint(s_on_q0)

        print("\nCalling embed_operator(n_qubits=2, gate_matrix=S, targets=[1])")
        s_on_q1 = embed_operator(n_qubits=2, gate_matrix=s_gate, targets=[1])
        sympy.pprint(s_on_q1)

        i_gate = gate_semantics['i']
        # Note: sympy.kronecker_product(A, B) corresponds to A ⊗ B
        # In a little-endian system (q0, q1, ...), A ⊗ B applies A to q0 and B to q1
        # In a big-endian system (..., q1, q0), A ⊗ B applies A to q_{n-1} and B to q_{n-2}
        i_tensor_s = sympy.kronecker_product(i_gate, s_gate)
        s_tensor_i = sympy.kronecker_product(s_gate, i_gate)

        print("\nExpected matrix for I ⊗ S (Little-Endian for target 0):")
        sympy.pprint(i_tensor_s)

        print("\nExpected matrix for S ⊗ I (Little-Endian for target 1):")
        sympy.pprint(s_tensor_i)

        # --- Test 14: intertwiner_basis with L=R=I ---
        print("\n--- Test 14: intertwiner_basis with L=R=I ---")
        i_gate = gate_semantics['i']
        basis_I, _ = intertwiner_basis(i_gate, i_gate)
        print(f"Basis found for S in I*S = S*I. Number of basis elements: {len(basis_I)}")
        # Expected: 4 basis elements for a 2x2 matrix space
        for b in basis_I:
            sympy.pprint(b)

        # --- Test 15: intertwiner_basis with L=R=Z ---
        print("\n--- Test 15: intertwiner_basis with L=R=Z ---")
        z_gate = gate_semantics['z']
        basis_Z, _ = intertwiner_basis(z_gate, z_gate)
        print(f"Basis found for S in Z*S = S*Z. Number of basis elements: {len(basis_Z)}")
        # Expected: 2 basis elements (diagonal matrices)
        for b in basis_Z:
            sympy.pprint(b)

        # --- Test 16: solve_intertwiner_equation with X*S*X = S ---
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

        # --- Test 17: intertwiner_basis with complex result ---
        print("\n--- Test 17: intertwiner_basis with complex result ---")
        y_gate = gate_semantics['y']
        z_gate = gate_semantics['z']
        
        # We solve Y*S = S*(-Z)
        L = y_gate
        R = -z_gate
        
        print("Solving Y*S = S*(-Z)")
        basis_complex, _ = intertwiner_basis(L, R)
        
        print(f"Basis found. Number of basis elements: {len(basis_complex)}")
        for b in basis_complex:
            print(b) # Use print() for machine-readable output

        # --- Test 18: intertwiner_basis with more complex numbers ---
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

        # --- Test 19: intertwiner_basis with irrational numbers ---
        print("\n--- Test 19: intertwiner_basis with irrational numbers ---")
        
        L = h_gate
        R = x_gate
        
        print("Solving H*S = S*X")
        basis_irrational, _ = intertwiner_basis(L, R)
        
        print(f"Basis found. Number of basis elements: {len(basis_irrational)}")
        for b in basis_irrational:
            print(b)

        # --- Test 20: Check for monomial solution for X*S = S*Z ---
        print("\n--- Test 20: Check for monomial solution for X*S = S*Z ---")
        
        L = x_gate
        R = z_gate
        
        print("Solving X*S = S*Z")
        basis, _ = intertwiner_basis(L, R)
        
        print(f"Basis found. Number of basis elements: {len(basis)}")
        print("Basis matrices are:")
        for b in basis:
            print(b)

        # --- Test 21: Find multiple unitary solutions from a basis ---
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
        # For this case, the equations are x0**2 + y0**2 - 1 = 0 and x1**2 + y1**2 - 1 = 0
        # This means the complex coefficients c0 and c1 must lie on the unit circle.
        # There are infinite solutions, e.g., c0=1,c1=1 (Identity) or c0=1,c1=-1 (Z gate).
        print("|c0|^2 = 1  and  |c1|^2 = 1")
        print("This confirms that multiple (in fact, infinite) unitary solutions can be formed from the basis.")

        # --- Test 23: Trace equality check ---
        print("\n--- Test 23: Trace equality check ---")
        
        z_gate = gate_semantics['z']
        x_gate = gate_semantics['x']
        i_gate = gate_semantics['i']
        rz_gate = gate_semantics['rz']
        # In semantics.py, rx has symbol theta1, and rz has symbol gamma.
        # We need to substitute one to make them the same for a valid comparison.
        rx_gate = gate_semantics['rx']
        rz_gate_sub = rz_gate.subs({gamma: theta1})

        # Case 1: Z and X (Traces are equal: 0 == 0)
        print(f"Do Z and X have the same trace? {have_same_trace(z_gate, x_gate)}")

        # Case 2: Z and I (Traces are not equal: 0 != 2)
        print(f"Do Z and I have the same trace? {have_same_trace(z_gate, i_gate)}")

        # Case 3: RZ(theta1) and RX(theta1) (Traces are equal)
        print(f"Do RZ(theta1) and RX(theta1) have the same trace? {have_same_trace(rz_gate_sub, rx_gate)}")

        # --- Test 24: solve_trace and solve_eigen tests ---
        print("\n--- Test 24: solve_trace and solve_eigen tests ---")

        # Test solve_trace: Pass
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

        # Test solve_trace: Fail
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

        # Test solve_eigen: Pass
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

        # Test solve_eigen: Fail
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


        # --- Test 25: Determinant equality check ---
        print("\n--- Test 25: Determinant equality check ---")
        
        z_gate = gate_semantics['z']
        x_gate = gate_semantics['x']
        h_gate = gate_semantics['h']
        s_gate = gate_semantics['s']

        # Case 1: Z and X (Determinants are equal: -1 == -1)
        print(f"Do Z and X have the same determinant? {have_same_determinant(z_gate, x_gate)}")

        # Case 2: H and X (Determinants are not equal: -1 != -1/sqrt(2))
        print(f"Do H and X have the same determinant? {not have_same_determinant(h_gate, x_gate)}")

        # Case 3: S and Z (Determinants are not equal: i != -1)
        print(f"Do S and Z have the same determinant? {not have_same_determinant(s_gate, z_gate)}")


        # --- Test 26: Big check test ---
        print("\n--- Test 26: Big check test ---")

        # Test solve_big_check: Pass
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

        # Test solve_big_check: Fail
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

        # Test cases for linear_span_test
        print("\n--- Testing linear_span_test ---")

        # Test 1: Empty basis
        print("Test 1: Empty basis")
        result = linear_span_test([])
        print(f"Expected: False, Got: {result}")
        assert result == False

        # # Test 2: Basis with products that form I (Pauli X)
        # print("\nTest 2: Basis with products that form I (Pauli X)")
        # X = sympy.Matrix([[0, 1], [1, 0]])
        # basis_X = [X]
        # result = linear_span_test(basis_X)
        # print(f"Expected: True, Got: {result}")
        # assert result == True

        # Test 3: Identity in span of products (projectors)
        # print("\nTest 3: Identity in span of products (projectors)")
        # B1 = sympy.Matrix([[1, 0], [0, 0]])
        # B2 = sympy.Matrix([[0, 0], [0, 1]])
        # basis_projectors = [B1, B2]
        # result = linear_span_test(basis_projectors)
        # print(f"Expected: True, Got: {result}")
        # assert result == True

        # Test 4: Identity not in span of products (nilpotent)
        # print("\nTest 4: Identity not in span of products (nilpotent)")
        # B_nilpotent = sympy.Matrix([[0, 1], [0, 0]], dtype=complex)
        # basis_nilpotent = [B_nilpotent]
        # result = linear_span_test(basis_nilpotent)
        # print(f"Expected: False, Got: {result}")
        # assert result == False

        # # Test 5: Basis with multiple elements, I not in span of products
        # print("\nTest 5: Basis with multiple elements, I in span of products")
        # B1_complex = sympy.Matrix([[1, 0], [0, 0]], dtype=complex)
        # B2_complex = sympy.Matrix([[0, 1j], [0, 0]], dtype=complex)
        # basis_complex = [B1_complex, B2_complex]
        # result = linear_span_test(basis_complex)
        # print(f"Expected: True, Got: {result}")
        # assert result == True

        print("\nAll linear_span_test assertions passed!")

        # --- Test 27: Eigenvalue of rx(theta2) q[0]; rxx(theta1+theta2) q[0], q[1] ---
        print("\n--- Test 27: Eigenvalue of rx(theta2) q[0]; rxx(theta1+theta2) q[0], q[1] ---")
        circuit = [
            {'gate': 'rz', 'targets': [0], 'params': {gamma: sympy.pi}},
            {'gate': 'rx', 'targets': [0], 'params': {theta1: sympy.pi/2}}
        ]
        circuit_matrix = calculate_circuit_matrix(n_qubits=2, circuit=circuit)
        print("Shape:", circuit_matrix.shape)
        print("Eigenvalues:")
        try:
            #eigenvals = circuit_matrix.eigenvals()
            #print(eigenvals)
            print(circuit_matrix.trace())
            # Convert to a list of tuples and sort by the string representation of the eigenvalue
            #sorted_eigenvals = sorted(eigenvals.items(), key=lambda x: str(x[0]))
            # for val, mult in sorted_eigenvals:
            #     print(f"{val}: {mult}")
        except Exception as e:
            print(f"Could not compute eigenvalues: {e}")

if __name__ == "__main__":
    main()

    