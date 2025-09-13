# mlvoqc

This repository contains OCaml code for running the VOQC quantum circuit compiler, [presented at POPL 2021](https://dl.acm.org/doi/10.1145/3434318). The `.ml` files in `ml/extracted` are *extracted* from the verified Coq definitions in the `VOQC` directory of [inQWIRE/SQIR](https://github.com/inQWIRE/SQIR). For instructions on how to re-generate the extracted OCaml code from our Coq definitions see [Extraction](#extraction) below. 

`example.ml` is an example OCaml program that uses the VOQC library; we include instructions for compiling and running it below. However, we recommend using our Python wrapper available in [inQWIRE/pyvoqc](https://github.com/inQWIRE/pyvoqc) instead. The pyvoqc repository also includes a tutorial.

## Table of Contents

- [mlvoqc](#mlvoqc)
  - [Table of Contents](#table-of-contents)
  - [Setup](#setup)
  - [Installation](#installation)
  - [Usage](#usage)
  - [Documentation](#documentation)
  - [Extraction](#extraction)
    - [Notes on Extraction](#notes-on-extraction)
  - [Contributing](#contributing)
  - [Acknowledgements](#acknowledgements)

## Setup

VOQC requires OCaml (version >= 4.08.1), [opam](https://opam.ocaml.org/doc/Install.html), and dune (version >= 2.7). Once you have opam installed, follow the instructions below to set up your environment.
```
# environment setup
opam init
eval $(opam env)

# install some version of the OCaml compiler in a switch named "voqc"
opam switch create voqc 4.13.1
eval $(opam env)

# install dune (needed to build VOQC)
opam install dune
```

*Note*: opam error messages and warnings are typically informative, so if you run into trouble then make sure you read the console output.

## Installation

You can now install the VOQC library using the opam package manager.
```
opam install voqc
```

Once you have the VOQC library installed, you can build the example program with `make example`.

*Notes*: 
* When building the VOQC executable on a Mac, you will likely see the warning `ld: warning: directory not found for option '-L/opt/local/lib'`. This is due to zarith (see [ocaml/opam-repository#3000](https://github.com/ocaml/opam-repository/issues/3000)) and seems to be fine to ignore.
* If you decide to build mlvoqc locally (using `make install`) instead of through opam, you will need to install the OCaml OpenQASM parser (`opam install openQASM`).

## Usage

Since the example program is built using dune, you need to run it with `dune exec`. Here is an example run:
```
$ dune exec -- ./example.exe -f example.qasm 

Input circuit has 3 gates and uses 5 qubits.
After decomposition to the RzQ gate set, the circuit uses 45 gates : { H : 6, X : 0, Rzq : 21, CX : 18 }.
After mapping, the circuit uses 70 gates : { H : 6, X : 0, Rzq : 21, CX : 43 }.
After optimization, the circuit uses 61 gates : { U1 : 14, U2 : 6, U3 : 0, CX : 41 }.
```

VOQC supports OpenQASM programs that use the following gates:
* I, X, Y, Z, H, S, T, Sdg, Tdg
* Rx(f), Ry(f), Rz(f)
* Rzq(n,n)
* U1(f), U2(f,f), U3(f,f,f)
* CX, CZ, SWAP
* CCX, CCZ

where n is an integer expression and f is a float expression. rzq is a non-standard gate that we have defined specifically for VOQC. rzq(num,den) performs a rotation about the z-axis by ((num /den) * pi) for integers num and den. VOQC currently does not support OpenQASM programs that use measurement.

## Documentation

Documentation for the VOQC library (generated with [odoc](https://github.com/ocaml/odoc)) is available at [https://inQWIRE.github.io/mlvoqc/voqc/index.html](https://inQWIRE.github.io/mlvoqc/voqc/index.html).

## Extraction

To re-generate the extracted OCaml code (e.g. when you want to update to include new features from [inQWIRE/SQIR](https://github.com/inQWIRE/SQIR)), change into the `extraction` directory and run `./extract.sh`. This will run Coq on our `Extraction.v` file and move the generated OCaml code to the correct directory. Depending on updates made to the Coq code, you may need to modify `extract.sh` or `Extraction.v`.

In order to perform extraction, you will need to have Coq installed (`opam install coq`). Extraction has been tested with Coq versions 8.12-8.15.

### Notes on Extraction

For performance, we:
* Extract Coq nat to OCaml int.
* Extract Coq Q to OCaml Zarith Q.
* Replace Coq's FMapAVL and FSetAVL implementations with OCaml's built-in Set and Map.

This makes the assumption that these OCaml data structures satisfy the properties proved about their corresponding Coq implementations. Note that nats are only used to identify qubit indices and we do not perform arithmetic over qubit indices, so an overflow is unlikely.

Perhaps more problematic, we have decided to extract Coq's axiomatized Reals (used for continuous gate parameters) to OCaml floats. This invites the possibility of floating point rounding error, which is not accounted for in our proofs. We have not observed errors cause by this during testing, but it's something to keep in mind. We are working to come up with a better solution.

## Contributing

Pull requests are welcome! Note that the code in `ml/extracted` should *only* be updated following the instructions in [Extraction](#extraction) above. If you are interested in developing verified optimizations for quantum circuits, then consider working from our Coq development at [inQWIRE/SQIR](https://github.com/inQWIRE/SQIR) instead.

## Acknowledgements

This project is supported by the U.S. Department of Energy, Office of Science, Office of Advanced Scientific Computing Research, Quantum Testbed Pathfinder Program under Award Number DE-SC0019040 and the Air Force Office of Scientific Research under Grant Number FA95502110051.
