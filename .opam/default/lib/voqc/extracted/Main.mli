open FullGateSet
open UnitaryListRepresentation

(** {1 Overview} *)

(** VOQC contains utilities for optimizing quantum circuits and mapping them
   to a connectivity graph. VOQC was first presented at POPL 2021. The extended
   version of the POPLpaper is available {{:https://arxiv.org/abs/1912.02250}here}.
   In the remainder of this document we refer to this paper as "POPL VOQC". *)

(** This file lists the functions we provide in VOQC, as well as properties we 
   have verified about them. In general, our goal is to prove that VOQC circuit 
   transformations are {i semantics preserving }, meaning that the behavior of a 
   circuit (its "semantics") does not change after our transformations are applied. 
   The semantics of a unitary circuit over d qubits is a 2^d x 2^d 
   unitary matrix, so proving semantics-preservation requires showing that two 
   (symbolic) matrices are equal. Sometimes we also show that the output of a 
   transformation has a particular property, such as using a particular gate set. 
   
   In the remainder of this document, we will use the following shorthand for 
   properties proved:
- {i Preserves semantics } -- The semantics-preservation property is written in Coq as 
   [ forall (c : circ), [[opt c]] = [[c]]] for optimization [ opt]. In our development, 
   we support several variations on semantics-preservation:
{ul {- {i (WT) } -- We include the annotation (WT) to indicate that a transformation 
   preserves semantics provided that the input circuit is well-typed. A circuit is 
   "well-typed" for some dimension (i.e. number of qubits) d if every gate in the 
   circuit applies to qubit(s) less than d and no gate has duplicate arguments. 
   Some optimizations do not preserve semantics when the input is ill-typed because 
   they can produce a well-typed circuit from an ill-typed input. In Coq, this 
   requirement is written as [ forall c, well_typed c -> [[opt c]] = [[c]]].}
{- {i (phase) } -- We include the annotation (phase) to indicate that the semantics 
   are preserved up to a global phase. The unitary matrices X and e^(ix)X have the 
   same action on all input states, and can thus be considered equivalent. However, 
   this definition of equivalence is not compositional in the sense that the controlled 
   versions of these matrices are not equivalent (because the phase is no longer global). 
   In Coq, this restriction is written as [ forall c, exists x, [[opt c]] = e^(ix) [[c]]].}
{- {i (perm) } -- We include the annotation (perm) to indicate that the semantics are preserved 
   up to a permutation of qubits. This will be the case after applying circuit mapping. 
   In Coq, for permutation matrices P1 and P2, this variation is written as  
   [ forall c, exists P1 P2, [[c]] = P1 x [[opt c]] x P2].}}
- {i Preserves WT} -- This property says that if the input circuit is well-typed, 
   then the output circuit will also be well-typed. In our semantics, the denotation of 
   a well-typed circuit is a unitary matrix while the denotation of an ill-typed circuit 
   is the zero matrix, so all of the variations on semantics-preservation actually 
   imply WT-preservation.
- {i Preserves mapping} -- This property says that, for any connectivity graph G, if the 
   input circuit respects the constraints in G then the output 
   circuit also respects the constraints in G. This property allows us to apply 
   optimizations after mapping, preserving our guarantee that the output program 
   satisfies hardware constraints.
- {i Uses gates \{g1, ..., gn\}} -- This property says that every operation in the 
   circuit is an application of one of \{g1, ..., gn\}.
- {i Respects constraints G} -- This property says that every two-qubit interaction 
   in the circuit is allowed by the connectivity graph G.

*)

(** {1 Types} *)

(** The [circ] type is a list of gates from the full gate set. *)
type circ = FullGateSet.coq_Full_Unitary gate_list

(** A {i layout} describes a mapping from logical to physical qubits.
   For an architecture with n qubits, a layout should be a bijection on 
   integers \{0, ..., n-1\}. Under the hood, [layout] is a pair of functions 
   [(nat -> nat) * (nat -> nat)] that maps logical qubits to physical qubits 
   and physical qubits back to logical qubits. A layout is {i well-formed} if 
   these two functions accurately represent a bijection and its inverse.
   
   You can construct a layout using the functions [list_to_layout], [trivial_layout],
   or [greedy_layout]. *)
type layout

(** A {i connectivity graph} ([c_graph]) is a pair [ nat * (nat -> nat -> bool)], 
   that consists of the total number of qubits in the system and a an oracle that 
   indicates whether a directed edge exists between two qubits.
   
   You can construct a connectivity graph using [make_lnn], [make_lnn_ring],
   or [make_grid]. *)
type c_graph

(** A [path_finding_fun] is a function [ nat -> nat -> nat list ] that produces
   an undirected path between any two nodes in a graph. It is used for circuit
   mapping.
   
   You can construct an object of this type using [lnn_path_finding_fun], 
   [lnn_ring_path_finding_fun], or [grid_path_finding_fun]. *)
type path_finding_fun

(** A [qubit_ordering_fun] is a function [ nat option -> nat list ] that produces
   an ordering of all qubits in the system, excluding its optional argument. 
   It is used for ranking pysical qubits when generating layouts.
   
   You can construct an object of this type using [lnn_qubit_ordering_fun] or
   [lnn_ring_qubit_ordering_fun]. *)
type qubit_ordering_fun

(** {1 API} *)

(** {2 Utility Functions} *)

(** Check if a circuit is well-typed with the given number of qubits (i.e. every 
   gate is applied to qubits within range and never applied to duplicates). *)
val check_well_typed : circ -> int -> bool

(** Restrict a program to use gates in the IBM gate set (this may be done implicitly 
   when calling certain optimizations).

   {i Verified Properties }: preserves semantics, preserves WT, preserves mapping, 
   uses gates \{U1, U2, U3, CX\} *)
val convert_to_ibm : circ -> circ

(** Restrict a circuit to use gates in the RzQ gate set (this may be done implicitly 
   when calling certain optimizations).
    
   {i Verified properties:} preserves semantics (phase), preserves WT, 
   preserves mapping, uses gates \{H, X, Rzq, CX\} *)
val convert_to_rzq : circ -> circ

(** Replace the (non-standard) Rzq gate with its equivalent Rz, Z, T, S, Tdg, or Sdg gate.
    
   {i Verified Properties:} preserves semantics, preserves WT, preserves mapping, 
   uses any gate in the full set except Rzq *)
val replace_rzq : circ -> circ

(** Decompose CZ, SWAP, CCX, and CCZ gates so that the only multi-qubit gate is 
   CX (also called "CNOT").
    
   {i Verified Properties:} preserves semantics, preserves WT, uses any gate in 
   the full set except \{CZ, SWAP, CCX, CCZ\} *)
val decompose_to_cnot : circ -> circ

val count_I : circ -> int
val count_X : circ -> int
val count_Y : circ -> int
val count_Z : circ -> int
val count_H : circ -> int
val count_S : circ -> int
val count_T : circ -> int
val count_Sdg : circ -> int
val count_Tdg : circ -> int
val count_Rx : circ -> int
val count_Ry : circ -> int
val count_Rz : circ -> int
val count_Rzq : circ -> int
val count_U1 : circ -> int
val count_U2 : circ -> int
val count_U3 : circ -> int
val count_CX : circ -> int
val count_CZ : circ -> int
val count_SWAP : circ -> int
val count_CCX : circ -> int
val count_CCZ : circ -> int

(** Count the number of 1-qubit gates. *)
val count_1q : circ -> int

(** Count the number of 2-qubit gates. *)
val count_2q : circ -> int

(** Count the number of 3-qubit gates. *)
val count_3q : circ -> int

(** Count the total number of gates (i.e. length of the instruction list). *)
val count_total : circ -> int

(** Count the Rzq gates parameterized by a multiple of PI/2 (i.e. "Clifford" Rz gates). *)
val count_rzq_clifford : circ -> int

(** {2 Optimization Functions} *)

(** Implementation of Qiskit's {{:https://qiskit.org/documentation/stubs/qiskit.transpiler.passes.Optimize1qGates.html}Optimize1qGates} 
   routine, which merges adjacent 1-qubit gates. Internally uses the IBM gate set. 
   
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val optimize_ibm : circ -> circ

(** Implementation of Nam et al.'s "NOT propagation," which commutes X gates rightward 
    through the circuit, cancelling them when possible (see VOQC POPL Sec 4.3). 
    Internally uses the RzQ gate set.
  
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val not_propagation : circ -> circ 

(** Implementation of Nam et al.'s "Hadamard gate reduction," which applies a series 
    of identities to reduce the number of H gates (see VOQC POPL Sec 4.4). 
    Internally uses the RzQ gate set.
  
   {i Verified Properties:} Preserves semantics (phase), preserves WT, preserves mapping *)
val hadamard_reduction : circ -> circ

(** Implementation of Nam et al.'s "single-qubit gate cancellation," which commutes 
   single-qubit gates rightward through the circuit, cancelling them when possible, 
   and reverting them to their original positions if they fail to cancel (see VOQC 
   POPL Sec 4.3). Internally uses the RzQ gate set.
  
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val cancel_single_qubit_gates : circ -> circ

(** Implementation of Nam et al.'s "two-qubit gate cancellation," which commutes 
   CX gates rightward through the circuit, cancelling them when possible, 
   and reverting them to their original positions if they fail to cancel (see VOQC 
   POPL Sec 4.3). Internally uses the RzQ gate set.
  
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val cancel_two_qubit_gates : circ -> circ 

(** Implementation of Nam et al.'s "rotation merging using phase polynomials," 
   which combines Rz gates that act on the same logical state (see VOQC POPL
   Sec 4.4). Internally uses the RzQ gate set.
  
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val merge_rotations : circ -> circ

(** Run optimizations in the order 0, 1, 3, 2, 3, 1, 2, 4, 3, 2 where 0 is [not_propagation], 
   1 is [hadamard_reduction], 2 is [cancel_single_qubit_gates], 3 is [cancel_two_qubit_gates], 
   and 4 is [merge_rotations] (see VOQC POPL Sec 4.6).
  
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val optimize_nam : circ -> circ

(** Run optimizations in the order 0, 1, 3, 2, 3, 1, 2, using the same numbering 
   scheme as above. This will be faster than [optimize_nam] because it excludes 
   rotation merging, which is our slowest optimization.
    
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val optimize_nam_light : circ -> circ

(** Use [optimize_nam] to optimize across loop iterations. To count the gates 
   required for n iterations, use [count_gates_lcr].
    
    {i Verified Properties:} Say that this function returns [Some (l, c, r)] on 
    input [c0]. Then for any n >= 3, iterating [c0] n times is equivalent to 
    running [l] once, [c] n-2 times, and [r] once. If [c0] is well-typed then 
    [l, c, r] are also well-typed. If [c0] is properly mapped then [l, c, r] 
    are also properly mapped. *)
val optimize_nam_lcr : circ -> ((circ * circ) * circ) option

(** [optimize_nam] followed by [optimize_ibm].
   
   {i Verified Properties:} Preserves semantics (WT, phase), preserves WT, preserves mapping *)
val optimize : circ -> circ

(** {2 Mapping Functions} *)

(** Map a circuit to an architecture given an initial layout.
    
    {i Verified properties:} Provided that that the input circuit is well-typed 
    using the dimension stored in the input graph and the input layout and graph 
    are well-formed, this transformation preserves semantics (WT, perm) and preserves WT. 
    Furthermore, the output [c] respects the (undirected) constraints of the input graph. *)
val swap_route : circ -> layout -> c_graph -> path_finding_fun -> circ

(** Decompose swap gates and reorient cnot gates to satisfy connectivity constraints.
    
    {i Verified properties:} Provided that that the input circuit is well-typed 
    using the dimension stored in the input graph, this transformation preserves 
    semantics and preserves WT. Furthermore, if the input circuit restpects the 
    (undirected) constraints of the graph, then the output respects the (directed)
    constraints. *)
val decompose_swaps : circ -> c_graph -> circ

(** Create a trivial layout on n qubits (i.e. logical qubit i is mapped to physical qubit i).
    
    {i Verified Properties:} The output layout is well-formed. *)
val trivial_layout : int -> layout

(** Check if a list is a permutation over [[0...(n-1)]], meaning that it is valid input
   to [list_to_layout].
   
   {i Verified Properties:} If [check_list] returns true, the layout output by 
   [list_to_layout] is well-formed. *)
val check_list : int list -> bool

(** Make a layout from a list. Example: the list [[3; 4; 1; 2; 0]] is transformed 
   to a layout with physical to logical qubit mapping \{0->3, 1->4, 2->1, 3->2, 4->0\}
   (so physical qubit 0 stores logical qubit 3) and the appropriate inverse logical 
   to physical mapping. 
   
   {i Verified Properties:} If [check_list] returns true, the layout output by 
   [list_to_layout] is well-formed.*)
val list_to_layout : int list -> layout

(** Convert a layout to a list for easier printing. Example: the layout with 
   physical to logical qubit mapping \{0->3, 1->4, 2->1, 3->2, 4->0\} is
   transformed to the list [[3; 4; 1; 2; 0]]. *)    
val layout_to_list : layout -> int -> int list

(** Choose an initial layout for a program that puts qubits close together on 
   the architecture if they are used together in a two-qubit gate. 
   
   {i Verified Properties:} The output layout is well-formed. *)
val greedy_layout : circ -> c_graph -> qubit_ordering_fun -> layout

(** Create a 1D LNN graph with n qubits (see POPL VOQC Fig 8(b)). *)
val make_lnn : int -> c_graph

(** Create a 1D LNN ring graph with n qubits (see POPL VOQC Fig 8(c)). *)
val make_lnn_ring : int -> c_graph

(** Create a m x n 2D grid (see POPL VOQC Fig 8(d)). *)
val make_grid : int -> int -> c_graph

(** Create a connectivity graph from a list of pairs describing edges in the graph. *)
val c_graph_from_coupling_map : int -> (int * int) list -> c_graph

(** Get a function to find paths in a 1D LNN graph.
    
    {i Verified Properties:} The function returns valid paths. *)
val lnn_path_finding_fun : int -> path_finding_fun

(** Get a function to find paths in a 1D LNN ring graph.
    
    {i Verified Properties:} The function returns valid paths. *)
val lnn_ring_path_finding_fun : int -> path_finding_fun

(** Get a function to find paths in a 2D grid.
    
    {i Verified Properties:} The function returns valid paths. *)
val grid_path_finding_fun : int -> int -> path_finding_fun

(** Generate a preference-ordering function for qubits in a 1D LNN graph.
    
    {i Verified Properties:} The ordering function is valid. *)
val lnn_qubit_ordering_fun : int -> qubit_ordering_fun

(** Generate a preference-ordering function for qubits in a 1D LNN ring graph.
    
    {i Verified Properties:} The ordering function is valid. *)
val lnn_ring_qubit_ordering_fun : int -> qubit_ordering_fun

(** Remove all swap gates from a program, instead performing a logical relabeling 
   of qubits. 
   
   {i Verified Properties:} Provided that that the input circuit is well-typed 
   and the input layout is well-formed, this transformation preserves semantics 
   (WT, perm) and preserves WT.  *)
val remove_swaps : circ -> layout -> circ

(** Check if two programs (and their initial layouts) are equivalent up to inserted
   swaps gates. This function can be used to validate circuit routing routines. 
   
   {i Verified Properties:} Assuming the input circuits are well-typed and the input
   layouts are well formed, if this function returns true then the programs are
   equivalent up to a permutation of qubits.  *)
val check_swap_equivalence : circ -> circ -> layout -> layout -> bool

(** Check if a circuit satisfies the constraints of a connectivity graph.

   {i Verified Properties:} If this function returns true, the program satisfies
   the constraints of the connectivity graph. *)
val check_constraints : circ -> c_graph -> bool

(** Example composition of transformations. Applies [optimize_nam], followed by
   [swap_route] using the 16-qubit LNN ring architecture, followed by [optimize].

   {i Verified Properties:} If this function returns [Some c], then it preserves semantics
   (WT, phase) and [c] respects the (directed) constraints of the LNN ring graph. *)
val optimize_and_map_to_lnn_ring_16 : circ -> circ option
