open Layouts
open MappingGateSet
open UnitaryListRepresentation

(** val path_to_swaps :
    FMap.key list -> layout -> 'a1 map_ucom_l * layout **)

let rec path_to_swaps p m =
  match p with
  | [] -> ([], m)
  | n1 :: t ->
    (match t with
     | [] -> ([], m)
     | n2 :: l ->
       (match l with
        | [] -> ([], m)
        | _ :: _ ->
          let (l0, m') = path_to_swaps t (swap_log m n1 n2) in
          (((coq_SWAP n1 n2) :: l0), m')))

(** val swap_route :
    'a1 map_ucom_l -> layout -> (int -> int -> int list) -> 'a1
    map_ucom_l * layout **)

let rec swap_route l m get_path =
  match l with
  | [] -> ([], m)
  | g :: t ->
    (match g with
     | App1 (u, n) ->
       let (t', m') = swap_route t m get_path in
       (((App1 (u, (get_phys m n))) :: t'), m')
     | App2 (u, n1, n2) ->
       let p = get_path (get_phys m n1) (get_phys m n2) in
       let (swaps, m') = path_to_swaps p m in
       let mapped_cnot =
         List.append swaps ((App2 (u, (get_phys m' n1),
           (get_phys m' n2))) :: [])
       in
       let (t', m'') = swap_route t m' get_path in
       ((List.append mapped_cnot t'), m'')
     | App3 (_, _, _, _) -> ([], m))

(** val coq_H :
    int -> FullGateSet.FullGateSet.coq_Full_Unitary coq_Map_Unitary gate_app **)

let coq_H a =
  App1 ((UMap_U FullGateSet.FullGateSet.U_H), a)

(** val cancel_cnots_with_swaps' :
    'a1 map_ucom_l -> int -> 'a1 coq_Map_Unitary gate_app list -> 'a1
    coq_Map_Unitary gate_app list **)

let rec cancel_cnots_with_swaps' l fuel acc =
  (fun fO fS n -> if n=0 then fO () else fS (n-1))
    (fun _ -> List.rev_append acc l)
    (fun fuel0 ->
    match l with
    | [] -> List.rev_append acc []
    | u :: t ->
      (match u with
       | App2 (m0, m, n) ->
         (match m0 with
          | UMap_U _ -> cancel_cnots_with_swaps' t fuel0 (u :: acc)
          | UMap_CNOT ->
            (match next_two_qubit_gate t m with
             | Some p ->
               let (p0, t2) = p in
               let (p1, n') = p0 in
               let (p2, m') = p1 in
               let (t1, m1) = p2 in
               (match m1 with
                | UMap_SWAP ->
                  if (&&) ((&&) ((=) m m') ((=) n n'))
                       (does_not_reference t1 n)
                  then cancel_cnots_with_swaps'
                         (List.append t1
                           (List.append ((coq_CNOT n m) :: [])
                             (List.append ((coq_CNOT m n) :: []) t2))) fuel0
                         acc
                  else if (&&) ((&&) ((=) m n') ((=) n m'))
                            (does_not_reference t1 n)
                       then cancel_cnots_with_swaps'
                              (List.append t1
                                (List.append ((coq_CNOT m' n') :: [])
                                  (List.append ((coq_CNOT n' m') :: []) t2)))
                              fuel0 acc
                       else cancel_cnots_with_swaps' t fuel0
                              ((coq_CNOT m n) :: acc)
                | _ ->
                  cancel_cnots_with_swaps' t fuel0 ((coq_CNOT m n) :: acc))
             | None ->
               cancel_cnots_with_swaps' t fuel0 ((coq_CNOT m n) :: acc))
          | UMap_SWAP ->
            (match next_two_qubit_gate t m with
             | Some p ->
               let (p0, t2) = p in
               let (p1, n') = p0 in
               let (p2, m') = p1 in
               let (t1, m1) = p2 in
               (match m1 with
                | UMap_CNOT ->
                  if (&&) ((&&) ((=) m m') ((=) n n'))
                       (does_not_reference t1 n)
                  then cancel_cnots_with_swaps'
                         (List.append ((coq_CNOT m n) :: [])
                           (List.append ((coq_CNOT n m) :: [])
                             (List.append t1 t2))) fuel0 acc
                  else if (&&) ((&&) ((=) m n') ((=) n m'))
                            (does_not_reference t1 n)
                       then cancel_cnots_with_swaps'
                              (List.append ((coq_CNOT n m) :: [])
                                (List.append ((coq_CNOT m n) :: [])
                                  (List.append t1 t2))) fuel0 acc
                       else cancel_cnots_with_swaps' t fuel0
                              ((coq_SWAP m n) :: acc)
                | _ ->
                  cancel_cnots_with_swaps' t fuel0 ((coq_SWAP m n) :: acc))
             | None ->
               cancel_cnots_with_swaps' t fuel0 ((coq_SWAP m n) :: acc)))
       | _ -> cancel_cnots_with_swaps' t fuel0 (u :: acc)))
    fuel

(** val fuel_aux : 'a1 map_ucom_l -> int -> int **)

let rec fuel_aux l acc =
  match l with
  | [] -> acc
  | g :: t ->
    (match g with
     | App2 (m0, _, _) ->
       (match m0 with
        | UMap_SWAP ->
          fuel_aux t
            ((+) acc (Pervasives.succ (Pervasives.succ (Pervasives.succ 0))))
        | _ -> fuel_aux t (Pervasives.succ acc))
     | _ -> fuel_aux t (Pervasives.succ acc))

(** val cancel_cnots_with_swaps :
    'a1 map_ucom_l -> 'a1 coq_Map_Unitary gate_app list **)

let cancel_cnots_with_swaps l =
  cancel_cnots_with_swaps' l (fuel_aux l 0) []

(** val decompose_swaps_and_cnots_aux :
    (int -> int -> bool) -> FullGateSet.FullGateSet.coq_Full_Unitary
    coq_Map_Unitary gate_app -> FullGateSet.FullGateSet.coq_Full_Unitary
    map_ucom_l **)

let decompose_swaps_and_cnots_aux is_in_graph g = match g with
| App2 (m0, m, n) ->
  (match m0 with
   | UMap_U _ -> g :: []
   | UMap_CNOT ->
     if is_in_graph m n
     then (coq_CNOT m n) :: []
     else (coq_H m) :: ((coq_H n) :: ((coq_CNOT n m) :: ((coq_H m) :: (
            (coq_H n) :: []))))
   | UMap_SWAP ->
     if is_in_graph m n
     then if is_in_graph n m
          then (coq_CNOT m n) :: ((coq_CNOT n m) :: ((coq_CNOT m n) :: []))
          else (coq_CNOT m n) :: ((coq_H n) :: ((coq_H m) :: ((coq_CNOT m n) :: (
                 (coq_H n) :: ((coq_H m) :: ((coq_CNOT m n) :: []))))))
     else (coq_CNOT n m) :: ((coq_H m) :: ((coq_H n) :: ((coq_CNOT n m) :: (
            (coq_H m) :: ((coq_H n) :: ((coq_CNOT n m) :: [])))))))
| _ -> g :: []

(** val decompose_swaps_and_cnots :
    FullGateSet.FullGateSet.coq_Full_Unitary map_ucom_l -> (int -> int ->
    bool) -> FullGateSet.FullGateSet.coq_Full_Unitary coq_Map_Unitary
    gate_list **)

let decompose_swaps_and_cnots l is_in_graph =
  FullGateSet.change_gate_set (decompose_swaps_and_cnots_aux is_in_graph)
    (cancel_cnots_with_swaps l)
