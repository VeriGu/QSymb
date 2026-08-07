import unittest
import json
from semantics import solve_intertwiner_equation, is_subspace_linear_combination, verify_subspace_linear_combination
import io
from contextlib import redirect_stdout

class TestSolveIntertwinerEquation(unittest.TestCase):

    def test_identity(self):
        """ S = S -> L=I, R=I. Solution is any matrix. """
        circuit_json = """
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]}
            ]
        }
        """
        f = io.StringIO()
        with redirect_stdout(f):
            solve_intertwiner_equation(circuit_json, circuit_json)
        output = f.getvalue()
        self.assertIn("Matrix([[1, 0], [0, 0]])", output)
        self.assertIn("Matrix([[0, 1], [0, 0]])", output)
        self.assertIn("Matrix([[0, 0], [1, 0]])", output)
        self.assertIn("Matrix([[0, 0], [0, 1]])", output)

    def test_commutation(self):
        """ ZS = SZ -> L=Z, R=Z. Solution is any matrix that commutes with Z (diagonal matrices). """
        circuit1_json = """
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "z", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        """
        circuit2_json = """
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "z", "targets": [0]}
            ]
        }
        """
        f = io.StringIO()
        with redirect_stdout(f):
            solve_intertwiner_equation(circuit1_json, circuit2_json)
        output = f.getvalue()
        self.assertIn("Matrix([[1, 0], [0, 0]])", output)
        self.assertIn("Matrix([[0, 0], [0, 1]])", output)
        self.assertNotIn("Matrix([[0, 1], [0, 0]])", output)
        self.assertNotIn("Matrix([[0, 0], [1, 0]])", output)

    def test_x_s_equals_s_z(self):
        """ X S = S Z. Solution is S = [[a,b],[a,-b]]. """
        circuit1_json = """
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "x", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        """
        circuit2_json = """
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "z", "targets": [0]}
            ]
        }
        """
        f = io.StringIO()
        with redirect_stdout(f):
            solve_intertwiner_equation(circuit1_json, circuit2_json)
        output = f.getvalue()
        # Basis should be [[1,0],[1,0]] and [[0,1],[0,-1]]
        self.assertIn("Matrix([[1, 0], [1, 0]])", output)
        self.assertIn("Matrix([[0, -1], [0, 1]])", output)

    def test_no_solution(self):
        """ X S = S Y. No non-trivial solution. """
        circuit1_json = """
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "x", "targets": [0]},
                {"gate": "symb", "targets": [0]}
            ]
        }
        """
        circuit2_json = """
        {
            "n_qubits": 1,
            "gates": [
                {"gate": "symb", "targets": [0]},
                {"gate": "y", "targets": [0]}
            ]
        }
        """
        f = io.StringIO()
        with redirect_stdout(f):
            solve_intertwiner_equation(circuit1_json, circuit2_json)
        output = f.getvalue()
        self.assertNotIn("No non-trivial solutions found.", output)


import sympy
from semantics import calculate_circuit_matrix, project_to_subspace, gate_semantics


class TestSubspaceLinearCombination(unittest.TestCase):

    def test_cnot_control_in_subspace(self):
        """ Test with CNOT where only the control is in the subspace. """
        circuit_json = '''
        {
            "n_qubits": 2,
            "gates": [
                {"gate": "cx", "targets": [0, 1]}
            ]
        }
        '''
        # Basis is Identity for the 1-qubit subspace
        basis_json = '''
        [
            {
                "rows": 2, "cols": 2,
                "entries": [
                    {"row": 0, "col": 0, "value": "1"},
                    {"row": 1, "col": 1, "value": "1"}
                ]
            }
        ]
        '''
        qubits_to_check = '[0]'
        is_combo = is_subspace_linear_combination(circuit_json, basis_json, qubits_to_check)
        self.assertTrue(is_combo)

    def test_cnot_target_in_subspace(self):
        """ Test with CNOT where only the target is in the subspace. """
        circuit_json = '''
        {
            "n_qubits": 2,
            "gates": [
                {"gate": "cx", "targets": [0, 1]}
            ]
        }
        '''
        basis_json = '''
        [
            {
                "rows": 2, "cols": 2,
                "entries": [
                    {"row": 0, "col": 0, "value": "1"},
                    {"row": 1, "col": 1, "value": "1"}
                ]
            }
        ]
        '''
        qubits_to_check = '[0]'
        is_combo = is_subspace_linear_combination(circuit_json, basis_json, qubits_to_check)
        self.assertTrue(is_combo)

    def test_circuit_with_mixed_gates(self):
        """ Test a circuit with gates inside and outside the subspace. """
        circuit_json = '''
        {
            "n_qubits": 3,
            "gates": [
                {"gate": "h", "targets": [0]},
                {"gate": "cx", "targets": [0, 1]},
                {"gate": "x", "targets": [2]}
            ]
        }
        '''
        # On subspace {0}, the circuit is just H gate.
        # We check if H is a linear combination of H.
        basis_json = '''
        [
            {
                "rows": 2, "cols": 2,
                "entries": [
                    {"row": 0, "col": 0, "value": "1/sqrt(2)"},
                    {"row": 0, "col": 1, "value": "1/sqrt(2)"},
                    {"row": 1, "col": 0, "value": "1/sqrt(2)"},
                    {"row": 1, "col": 1, "value": "-1/sqrt(2)"}
                ]
            }
        ]
        '''
        qubits_to_check = '[0]'
        is_combo = is_subspace_linear_combination(circuit_json, basis_json, qubits_to_check)
        self.assertTrue(is_combo)

class TestVerifySubspaceLinearCombination(unittest.TestCase):

    def test_complex_circuit_projection(self):
        """ Project a 3-qubit circuit to a 2-qubit subspace. """
        circuit_json = '''
        {
            "n_qubits": 3,
            "gates": [
                {"gate": "h", "targets": [0]},
                {"gate": "cx", "targets": [0, 1]},
                {"gate": "x", "targets": [2]}
            ]
        }
        '''
        # The projection of the circuit onto qubits [0, 1] is the Bell state circuit matrix.
        # We check if that matrix is in the span of itself.
        bell_state_matrix = 1/sympy.sqrt(2) * sympy.Matrix([[1, 0, 1, 0], [0, 1, 0, 1], [0, 1, 0, -1], [1, 0,  -1, 0]])
        
        basis_json = f'''
        [
            {{
                "rows": 4, "cols": 4,
                "entries": [
                    {{"row": 0, "col": 0, "value": "{bell_state_matrix[0,0]}"}},
                    {{"row": 0, "col": 1, "value": "{bell_state_matrix[0,1]}"}},
                    {{"row": 0, "col": 2, "value": "{bell_state_matrix[0,2]}"}},
                    {{"row": 0, "col": 3, "value": "{bell_state_matrix[0,3]}"}},
                    {{"row": 1, "col": 0, "value": "{bell_state_matrix[1,0]}"}},
                    {{"row": 1, "col": 1, "value": "{bell_state_matrix[1,1]}"}},
                    {{"row": 1, "col": 2, "value": "{bell_state_matrix[1,2]}"}},
                    {{"row": 1, "col": 3, "value": "{bell_state_matrix[1,3]}"}},
                    {{"row": 2, "col": 0, "value": "{bell_state_matrix[2,0]}"}},
                    {{"row": 2, "col": 1, "value": "{bell_state_matrix[2,1]}"}},
                    {{"row": 2, "col": 2, "value": "{bell_state_matrix[2,2]}"}},
                    {{"row": 2, "col": 3, "value": "{bell_state_matrix[2,3]}"}},
                    {{"row": 3, "col": 0, "value": "{bell_state_matrix[3,0]}"}},
                    {{"row": 3, "col": 1, "value": "{bell_state_matrix[3,1]}"}},
                    {{"row": 3, "col": 2, "value": "{bell_state_matrix[3,2]}"}},
                    {{"row": 3, "col": 3, "value": "{bell_state_matrix[3,3]}"}}
                ]
            }}
        ]
        '''
        qubits_to_check = '[0, 1]'
        is_combo = verify_subspace_linear_combination(circuit_json, basis_json, qubits_to_check)
        self.assertTrue(is_combo)

if __name__ == '__main__':
    unittest.main()