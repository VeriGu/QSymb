###
# This scripts runs an optimiser of choice (or all 
# optimisers for which runners are implemented) 
# and allows the user to specify custom arguments
###

import re
import os
import argparse
# qiskit
from qiskit import QuantumCircuit
from qiskit.compiler import transpile
from qiskit.transpiler import PassManager

# tket
from pytket.qasm import circuit_from_qasm, circuit_from_qasm_str, circuit_to_qasm, circuit_to_qasm_str
from pytket.passes import SequencePass
from pytket.passes import FullPeepholeOptimise, RemoveRedundancies, RebaseCustom
from pytket.transform import Transform
#PyZX
# import pyzx as zx

from pyvoqc.qiskit.voqc_pass import voqc_pass_manager

###
# Runners for optimisers found in 
# Hietala et al. (2021), a.k.a. the VOQC paper 
###

def run_VOQC_nam(args):
    circ = QuantumCircuit.from_qasm_file(args.prog)
    print(args.prog + " original 1q: " + str(sum([circ.count_ops()[k] for k in circ.count_ops()])))
    print(args.prog + " original 2q: " + str(circ.num_nonlocal_gates()))

    pm = voqc_pass_manager(post_opts=["optimize_nam"])
    circ = pm.run(circ)

    results = {}
    results['output_qasm'] = circ.qasm()
    print(args.prog + " voqc nam 1q: " + str(sum([circ.count_ops()[k] for k in circ.count_ops()])))
    print(args.prog + " voqc nam 2q: " + str(circ.num_nonlocal_gates()))
    return results

def run_VOQC_ibm(args):
    circ = QuantumCircuit.from_qasm_file(args.prog)

    pm = voqc_pass_manager(post_opts=["optimize"])
    circ = pm.run(circ)

    results = {}
    results['output_qasm'] = circ.qasm()
    print(args.prog + " voqc ibm 1q: " + str(sum([circ.count_ops()[k] for k in circ.count_ops()])))
    print(args.prog + " voqc ibm 2q: " + str(circ.num_nonlocal_gates()))
    return results

def run_Qiskit_nam(args):
    circ = QuantumCircuit.from_qasm_file(args.prog)
    results = {}
    for i in range(4):
        circ = transpile(circ, basis_gates=['h', 'x', 'rz', 'cx'], coupling_map=None, optimization_level=i, approximation_degree=1)
        if 'output_qasm' in results:
            if sum([circ.count_ops()[k] for k in circ.count_ops()]) < sum([results['output_qasm'].count_ops()[k] for k in results['output_qasm'].count_ops()]):
                results['output_qasm'] = circ
        else:
            results['output_qasm'] = circ

    print(args.prog + " qiskit nam 1q: " + str(sum([results['output_qasm'].count_ops()[k] for k in results['output_qasm'].count_ops()])))
    print(args.prog + " qiskit nam 2q: " + str(results['output_qasm'].num_nonlocal_gates()))
    results['output_qasm'] = results['output_qasm'].qasm()
    return results

def run_Qiskit_ibm(args):
    circ = QuantumCircuit.from_qasm_file(args.prog)
    results = {}
    for i in range(4):
        circ = transpile(circ, basis_gates=['u1', 'u2', 'u3', 'cx'], coupling_map=None, optimization_level=i, approximation_degree=1)
        if 'output_qasm' in results:
            if sum([circ.count_ops()[k] for k in circ.count_ops()]) < sum([results['output_qasm'].count_ops()[k] for k in results['output_qasm'].count_ops()]):
                results['output_qasm'] = circ
        else:
            results['output_qasm'] = circ

    print(args.prog + " qiskit ibm 1q: " + str(sum([results['output_qasm'].count_ops()[k] for k in results['output_qasm'].count_ops()])))
    print(args.prog + " qiskit ibm 2q: " + str(results['output_qasm'].num_nonlocal_gates()))
    results['output_qasm'] = results['output_qasm'].qasm()
    return results

def run_Qiskit_ion(args):
    circ = QuantumCircuit.from_qasm_file(args.prog)
    results = {}
    for i in range(4):
        circ = transpile(circ, basis_gates=['rx', 'rz', 'ry', 'rxx'], coupling_map=None, optimization_level=i, approximation_degree=1)
        if 'output_qasm' in results:
            if sum([circ.count_ops()[k] for k in circ.count_ops()]) < sum([results['output_qasm'].count_ops()[k] for k in results['output_qasm'].count_ops()]):
                results['output_qasm'] = circ
        else:
            results['output_qasm'] = circ

    print(args.prog + " qiskit ion 1q: " + str(sum([results['output_qasm'].count_ops()[k] for k in results['output_qasm'].count_ops()])))
    print(args.prog + " qiskit ion 2q: " + str(results['output_qasm'].num_nonlocal_gates()))
    results['output_qasm'] = results['output_qasm'].qasm()
    return results

from pytket.circuit import OpType, Circuit

def run_tket_nam(args):
    # tket
    circ = circuit_from_qasm(args.prog)
    # Hietala et al. use the following gate count optimizations for tket: FullPeepholeOptimise(), RemoveRedundancies()
    seq_pass = SequencePass([FullPeepholeOptimise(), RemoveRedundancies()])
    seq_pass.apply(circ) # <-- decomposes to atomic gates, neat side effect
    cx_r = Circuit(2)
    cx_r.CX(0,1)
    def tk1_r(a, b, c):
        circ = Circuit(1)
        circ.Rz(c, 0).H(0).Rz(b, 0).H(0).Rz(a, 0)
        return circ
    custom = RebaseCustom({OpType.CX, OpType.Rz, OpType.X, OpType.H}, cx_r, tk1_r)
    custom.apply(circ)

    results = {}
    # results['time'] = time_final - time_init
    results['output_qasm'] = circuit_to_qasm_str(circ)
    print(args.prog + " tket nam 1q: " + str(circ.n_gates))
    print(args.prog + " tket nam 2q: " + str(circ.n_gates_of_type(OpType.CX)))
    return results

def run_tket_ibm(args):
    # tket
    circ = circuit_from_qasm(args.prog)
    # Hietala et al. use the following gate count optimizations for tket: FullPeepholeOptimise(), RemoveRedundancies()
    seq_pass = SequencePass([FullPeepholeOptimise(), RemoveRedundancies()])
    seq_pass.apply(circ) # <-- decomposes to atomic gates, neat side effect

    results = {}
    # results['time'] = time_final - time_init
    results['output_qasm'] = circuit_to_qasm_str(circ)
    print(args.prog + " tket ibm 1q: " + str(circ.n_gates))
    print(args.prog + " tket ibm 2q: " + str(circ.n_gates_of_type(OpType.CX)))
    return results

def run_tket_rigetti(args):
    # tket
    circ = circuit_from_qasm(args.prog)
    # Hietala et al. use the following gate count optimizations for tket: FullPeepholeOptimise(), RemoveRedundancies()
    seq_pass = SequencePass([FullPeepholeOptimise(), RemoveRedundancies()])
    seq_pass.apply(circ) # <-- decomposes to atomic gates, neat side effect
    Transform.RebaseToQuil().apply(circ)

    results = {}
    # results['time'] = time_final - time_init
    results['output_qasm'] = circuit_to_qasm_str(circ)
    print(args.prog + " tket rigetti 1q: " + str(circ.n_gates))
    print(args.prog + " tket rigetti 2q: " + str(circ.n_gates_of_type(OpType.CZ)))
    return results

from pyquil.api import get_qc
from pyquil.gates import CNOT, H, RZ, X, CCNOT, T, RX, CZ
from pyquil.quil import Program
from math import pi

def qasm_to_quilc(filename):
    with open(filename) as f:
        lines = f.readlines()
        p = Program()

        for line in lines:
            if ";" not in line: continue
            if line.startswith("ccx"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                line = line[line.index(",")+1:]
                qubit2 = line[line.index("[")+1:line.index("]")]
                line = line[line.index(",")+1:]
                qubit3 = line[line.index("[")+1:line.index("]")]
                p += CCNOT(int(qubit1), int(qubit2), int(qubit3))
            elif line.startswith("h"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                p += H(int(qubit1))
            elif line.startswith("x"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                p += X(int(qubit1))
            elif line.startswith("tdg"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                p += T(int(qubit1)).dagger()
            elif line.startswith("t"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                p += T(int(qubit1))
            elif line.startswith("rz"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                angle = eval(line[line.index("(")+1:line.index(")")])
                p += RZ(angle, int(qubit1))
            elif line.startswith("rx"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                angle = eval(line[line.index("(")+1:line.index(")")])
                p += RX(angle, int(qubit1))
            elif line.startswith("cx"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                line = line[line.index(",")+1:]
                qubit2 = line[line.index("[")+1:line.index("]")]
                p += CNOT(int(qubit1), int(qubit2))
            elif line.startswith("cz"):
                qubit1 = line[line.index("[")+1:line.index("]")]
                line = line[line.index(",")+1:]
                qubit2 = line[line.index("[")+1:line.index("]")]
                p += CZ(int(qubit1), int(qubit2))
            elif line.startswith("OPEN") or line.startswith("include") or line.startswith("qreg"):
                continue
            else:
                raise RuntimeError("unimplemented gate")
        return p

def run_quilc(args):
    # quilc
    qc = get_qc("40q-qvm", compiler_timeout=10000)
    program = qasm_to_quilc(args.prog)

    ep = qc.compile(program)
    
    print(args.prog + " quilc rigetti 1q: " + str(len(str(ep).split('\n')) - 2))
    print(args.prog + " quilc rigetti 2q: " + str(str(ep).count("CZ")))

    results = {}
    # # results['time'] = time_final - time_init
    results['output_qasm'] = str(ep)
    return results

# def run_PyZX(args):
#     # PyZX

#     #Load in circuit, transform to ZX graph
#     circuit = zx.Circuit.load(args.prog)
#     g = circuit.to_graph()

#     # Hietala et al. use the following T count optimizations for PyZX:
#     zx.full_reduce(g)
    
#     # Transform back from ZX graph, then to QASM, then return
#     c = zx.extract_circuit(g.copy())
#     output_qasm = c.to_qasm()

#     results = {}
#     results['output_qasm'] = output_qasm
#     return results

def run_optimiser(args, prog_name):
    # Optimises circuit using specified optimiser
    optimisers = args.optimiser.split(",")
    for optimiser in optimisers:
        results = runners[optimiser](args)

        # Write output QASM to output directory
        if results['output_qasm']:
            output_qasm = results['output_qasm']
            with open(f'{args.output_dir}/{prog_name}.{optimiser}.qasm', mode="w") as f:
                f.write(output_qasm)

def run_optimiser_all(args, prog_name):
    # Optimises circuit using all optimisers 
    # that have working runners
    for optimiser in optimisers_available:
        args.optimiser = optimiser
        run_optimiser(args, prog_name)

runners = {
'VOQC_nam':run_VOQC_nam, 
'VOQC_ibm':run_VOQC_ibm, 
'Qiskit_nam':run_Qiskit_nam, 
'Qiskit_ibm':run_Qiskit_ibm, 
'Qiskit_ion': run_Qiskit_ion,
'tket_ibm':run_tket_ibm, 
'tket_rigetti':run_tket_rigetti, 
'tket_nam': run_tket_nam,
'quilc':run_quilc, 
# 'PyZX':run_PyZX, 
'Nam_et_al':None, 
'Amy_et_al':None}

# List of working implemented runners
optimisers_available = [
    'VOQC_nam', 
    # 'VOQC_ibm', 
    'Qiskit_nam',
    # 'Qiskit_ibm',
    # 'Qiskit_ion',
    # 'tket_nam', # shouldn't use
    # 'tket_ibm', 
    # 'tket_rigetti', 
    # 'quilc',
    # 'PyZX', 
    ]

if __name__ == '__main__':
    #Parse user-provided args
    parser = argparse.ArgumentParser()
    parser.add_argument("prog", help="path to input program file")
    parser.add_argument("--optimiser", help="name of optimiser used to optimise input program")
    parser.add_argument("-o_d", "--output_dir", help="directory for output files. Use 'default' for output_dir='results.{prog_name}'", default = ".")
    args = parser.parse_args()

    # Extract name of circuit
    prog_name = (args.prog).split("/")[-1]
    prog_name = prog_name.split(".")[0]
    print(f'Benchmarking {prog_name}...')

    # Create output dir if appropriate
    if args.output_dir == "default":
        output_dir = f"results.{prog_name}"
        try:
            os.mkdir(output_dir)
        except: #directory already exists
            pass
        args.output_dir = output_dir

    # Run appropriate optimiser(s)
    if args.optimiser:
        run_optimiser(args, prog_name)
    else:
        run_optimiser_all(args, prog_name)