open Printf
open Voqc.Qasm
open Voqc.Main

(** Example application of the VOQC library. Runs optimization and maps to
  a 5-qubit ring architecture supporting the gate set {U1, U2, U3, CX}.
  Expects an input OpenQASM 2.0 file (-f). *)

(* Argument parsing *)
let f = ref ""
let usage = "usage: " ^ Sys.argv.(0) ^ " -f string"
let speclist = [
    ("-f", Arg.Set_string f, ": input program");
  ]
let () =
  Arg.parse
    speclist
    (fun x -> raise (Arg.Bad ("Bad argument : " ^ x)))
    usage;
if !f = "" then printf "ERROR: Input file (-f) required.\n" else 

(* Read input file *)
let (c0, n) = read_qasm !f in
let _ = printf "Input circuit has %d gates and uses %d qubits.\n" (count_total c0) n in

(* Convert to the RzQ gate set and print more statistics *)
let c1 = convert_to_rzq c0 in
let _ = printf "After decomposition to the RzQ gate set, the circuit uses %d gates : { H : %d, X : %d, Rzq : %d, CX : %d }.\n" 
          (count_total c1) (count_H c1) (count_X c1) (count_Rzq c1) (count_CX c1) in

(* Map to the 5 qubit LNN ring architecture *)
let cg = make_lnn_ring 5 in
let la = trivial_layout 5 in
let c2 = decompose_swaps (swap_route c1 la cg (lnn_ring_path_finding_fun 5)) cg in
let _ = printf "After mapping, the circuit uses %d gates : { H : %d, X : %d, Rzq : %d, CX : %d }.\n" 
          (count_total c2) (count_H c2) (count_X c2) (count_Rzq c2) (count_CX c2) in

(* Optimize again *)
let c3 = optimize c2 in
printf "After optimization, the circuit uses %d gates : { U1 : %d, U2 : %d, U3 : %d, CX : %d }.\n" 
  (count_total c3) (count_U1 c3) (count_U2 c3) (count_U3 c3) (count_CX c3)