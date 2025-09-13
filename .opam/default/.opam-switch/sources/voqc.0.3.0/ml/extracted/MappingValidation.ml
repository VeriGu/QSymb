open Layouts
open MappingGateSet
open UnitaryListRepresentation

(** val remove_swaps' :
    'a1 map_ucom_l -> layout -> 'a1 coq_Map_Unitary gate_app list -> 'a1
    map_ucom_l * layout **)

let rec remove_swaps' l m acc =
  match l with
  | [] -> ((List.rev_append acc []), m)
  | g :: t ->
    (match g with
     | App1 (u, q) -> remove_swaps' t m ((App1 (u, (get_log m q))) :: acc)
     | App2 (m0, q1, q2) ->
       (match m0 with
        | UMap_U _ -> ([], m)
        | UMap_CNOT ->
          remove_swaps' t m ((coq_CNOT (get_log m q1) (get_log m q2)) :: acc)
        | UMap_SWAP -> remove_swaps' t (swap_log m q1 q2) acc)
     | App3 (_, _, _, _) -> ([], m))

(** val remove_swaps : 'a1 map_ucom_l -> layout -> 'a1 map_ucom_l * layout **)

let remove_swaps l m =
  remove_swaps' l m []

(** val check_swap_equivalence :
    'a1 map_ucom_l -> 'a1 map_ucom_l -> layout -> layout -> (int -> 'a1
    coq_Map_Unitary -> 'a1 coq_Map_Unitary -> bool) -> (layout * layout)
    option **)

let check_swap_equivalence l1 l2 m1 m2 match_gate =
  let (l1', m1') = remove_swaps l1 m1 in
  let (l2', m2') = remove_swaps l2 m2 in
  if equalb l1' l2' match_gate then Some (m1', m2') else None

(** val is_swap_equivalent :
    'a1 map_ucom_l -> 'a1 map_ucom_l -> layout -> layout -> (int -> 'a1
    coq_Map_Unitary -> 'a1 coq_Map_Unitary -> bool) -> bool **)

let is_swap_equivalent l1 l2 m1 m2 match_gate =
  match check_swap_equivalence l1 l2 m1 m2 match_gate with
  | Some _ -> true
  | None -> false

(** val check_gate_constraint :
    'a1 coq_Map_Unitary gate_app -> (int -> int -> bool) -> bool **)

let check_gate_constraint g is_in_graph =
  match g with
  | App1 (_, _) -> true
  | App2 (m, q1, q2) ->
    (match m with
     | UMap_CNOT -> is_in_graph q1 q2
     | _ -> false)
  | App3 (_, _, _, _) -> false

(** val check_constraints : 'a1 map_ucom_l -> (int -> int -> bool) -> bool **)

let check_constraints l is_in_graph =
  List.for_all (fun g -> check_gate_constraint g is_in_graph) l
