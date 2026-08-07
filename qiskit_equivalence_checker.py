import sys
from qiskit import QuantumCircuit
from qiskit.quantum_info import Operator
import os
import tempfile

def check_equivalence(qasm_file1, qasm_file2):
    try:
        qc1 = QuantumCircuit.from_qasm_file(qasm_file1)
        qc2 = QuantumCircuit.from_qasm_file(qasm_file2)

        op1 = Operator(qc1)
        op2 = Operator(qc2)

        # Check if the unitary matrices are approximately equal
        # This handles floating point inaccuracies
        if op1.equiv(op2):
            print("true")
        else:
            print("false")
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        print("false") # Indicate failure
        sys.exit(1)

def run_single_test(qasm_content):
    full_qasm = """
OPENQASM 2.0;
include "qelib1.inc";
qreg q[5];
""" + "\nrz(pi) q[0];"

    full_qasm1 = """
OPENQASM 2.0;
include "qelib1.inc";
qreg q[5];
""" + "\nrz(3.1415926) q[0];"

    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f1:
        f1.write(full_qasm)
        temp_qasm_file1 = f1.name
    with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f2:
        f2.write(full_qasm1)
        temp_qasm_file2 = f2.name
    
    print(f"\n--- Testing QASM string against itself ---")
    sys.stdout.flush()
    check_equivalence(temp_qasm_file1, temp_qasm_file2)
    os.remove(temp_qasm_file1)
    os.remove(temp_qasm_file2)


if __name__ == "__main__":
    if len(sys.argv) == 1 or sys.argv[1] == "--test":
        # Original test cases
        # ... (omitted for brevity, assuming they are still there)
        # Test Case 1: Identical circuits
        qasm_content_identical = """
OPENQASM 2.0;
include "qelib1.inc";
qreg q[5];
h q[3];
h q[4];
cx q[1], q[4];
rz(-pi/4.0) q[4];
cx q[0], q[4];
rz(pi/4.0) q[4];
cx q[1], q[4];
rz(-pi/4.0) q[4];
cx q[0], q[4];
cx q[0], q[1];
rz(-pi/4.0) q[1];
cx q[0], q[1];
rz(pi/4.0) q[0];
rz(pi/4.0) q[1];
rz(pi/4.0) q[4];
h q[4];
cx q[4], q[3];
rz(-pi/4.0) q[3];
cx q[2], q[3];
rz(pi/4.0) q[3];
cx q[4], q[3];
rz(-pi/4.0) q[3];
cx q[2], q[3];
cx q[2], q[4];
rz(pi/4.0) q[3];
h q[3];
rz(-pi/4.0) q[4];
cx q[2], q[4];
rz(pi/4.0) q[2];
rz(pi/4.0) q[4];
h q[4];
cx q[1], q[4];
rz(-pi/4.0) q[4];
cx q[0], q[4];
rz(pi/4.0) q[4];
cx q[1], q[4];
rz(-pi/4.0) q[4];
cx q[0], q[4];
cx q[0], q[1];
rz(-pi/4.0) q[1];
cx q[0], q[1];
rz(pi/4.0) q[0];
rz(pi/4.0) q[1];
rz(pi/4.0) q[4];
h q[4];
"""
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f1:
            f1.write(qasm_content_identical)
            temp_qasm_file1 = f1.name
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f2:
            f2.write(qasm_content_identical)
            temp_qasm_file2 = f2.name
        
        print(f"\n--- Running Test Case 1: Identical Circuits ---")
        sys.stdout.flush()
        check_equivalence(temp_qasm_file1, temp_qasm_file2)
        os.remove(temp_qasm_file1)
        os.remove(temp_qasm_file2)

        # Test Case 2: Simple equivalent circuits (X X = I)
        qasm_content_x_x_1 = """
OPENQASM 2.0;
include "qelib1.inc";
qreg q[1];
x q[0];
x q[0];
"""
        qasm_content_x_x_2 = """
OPENQASM 2.0;
include "qelib1.inc";
qreg q[1];
"""
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f1:
            f1.write(qasm_content_x_x_1)
            temp_qasm_file1 = f1.name
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f2:
            f2.write(qasm_content_x_x_2)
            temp_qasm_file2 = f2.name
        
        print(f"\n--- Running Test Case 2: X X = I ---")
        sys.stdout.flush()
        check_equivalence(temp_qasm_file1, temp_qasm_file2)
        os.remove(temp_qasm_file1)
        os.remove(temp_qasm_file2)

        # Test Case 3: Simple non-equivalent circuits
        qasm_content_non_equiv_1 = """
OPENQASM 2.0;
include "qelib1.inc";
qreg q[1];
x q[0];
"""
        qasm_content_non_equiv_2 = """
OPENQASM 2.0;
include "qelib1.inc";
qreg q[1];
"""
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f1:
            f1.write(qasm_content_non_equiv_1)
            temp_qasm_file1 = f1.name
        with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.qasm') as f2:
            f2.write(qasm_content_non_equiv_2)
            temp_qasm_file2 = f2.name
        
        print(f"\n--- Running Test Case 3: Non-equivalent Circuits ---")
        sys.stdout.flush()
        check_equivalence(temp_qasm_file1, temp_qasm_file2)
        os.remove(temp_qasm_file1)
        os.remove(temp_qasm_file2)


        run_single_test(sys.argv[1])

    elif len(sys.argv) == 3:
        qasm_file1 = sys.argv[1]
        qasm_file2 = sys.argv[2]
        check_equivalence(qasm_file1, qasm_file2)
    elif len(sys.argv) == 2 and sys.argv[1] != "--test":
        # Assume the single argument is the QASM string to test against itself
        run_single_test(sys.argv[1])
    else:
        print("Usage: python qiskit_equivalence_checker.py [--test | <qasm_file1> <qasm_file2> | <qasm_string_to_test_against_itself>]", file=sys.stderr)
        sys.exit(1)