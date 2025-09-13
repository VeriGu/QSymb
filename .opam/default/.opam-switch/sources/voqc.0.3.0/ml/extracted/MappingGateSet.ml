open UnitaryListRepresentation

type 'u coq_Map_Unitary =
| UMap_U of 'u
| UMap_CNOT
| UMap_SWAP

(** val coq_CNOT : int -> int -> 'a1 coq_Map_Unitary gate_app **)

let coq_CNOT a b =
  App2 (UMap_CNOT, a, b)

(** val coq_SWAP : int -> int -> 'a1 coq_Map_Unitary gate_app **)

let coq_SWAP a b =
  App2 (UMap_SWAP, a, b)

type 'u map_ucom_l = 'u coq_Map_Unitary gate_list

(** val match_gate :
    int -> ('a1 -> 'a1 -> bool) -> 'a1 coq_Map_Unitary -> 'a1 coq_Map_Unitary
    -> bool **)

let match_gate _ match_gate0 u u' =
  match u with
  | UMap_U u1 -> (match u' with
                  | UMap_U u2 -> match_gate0 u1 u2
                  | _ -> false)
  | UMap_CNOT -> (match u' with
                  | UMap_CNOT -> true
                  | _ -> false)
  | UMap_SWAP -> (match u' with
                  | UMap_SWAP -> true
                  | _ -> false)
