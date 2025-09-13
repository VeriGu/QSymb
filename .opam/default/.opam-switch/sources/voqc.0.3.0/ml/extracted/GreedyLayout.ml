open Datatypes
open FSetAVL
open Layouts
open MappingGateSet
open OrderedTypeEx
open UnitaryListRepresentation

module FSet = Make(Nat_as_OT)

(** val get_fresh_qubit : int list -> FSet.t -> int **)

let rec get_fresh_qubit prefs alloc =
  match prefs with
  | [] -> 0
  | p :: prefs0 ->
    if FSet.mem p alloc then get_fresh_qubit prefs0 alloc else p

(** val build_layout :
    'a1 coq_Map_Unitary gate_list -> FSet.t -> layout -> (int option -> int
    list) -> FSet.t * layout **)

let rec build_layout l alloc lay q_ordering =
  match l with
  | [] -> (alloc, lay)
  | g :: t0 ->
    (match g with
     | App2 (m0, m, n) ->
       (match m0 with
        | UMap_CNOT ->
          (match find_phys lay m with
           | Some m_phys ->
             (match find_phys lay n with
              | Some _ -> build_layout t0 alloc lay q_ordering
              | None ->
                let n_phys = get_fresh_qubit (q_ordering (Some m_phys)) alloc
                in
                let alloc' = FSet.add n_phys alloc in
                let lay' = add lay n n_phys in
                build_layout t0 alloc' lay' q_ordering)
           | None ->
             (match find_phys lay n with
              | Some n_phys ->
                let m_phys = get_fresh_qubit (q_ordering (Some n_phys)) alloc
                in
                let alloc' = FSet.add m_phys alloc in
                let lay' = add lay m m_phys in
                build_layout t0 alloc' lay' q_ordering
              | None ->
                let m_phys = get_fresh_qubit (q_ordering None) alloc in
                let n_phys =
                  get_fresh_qubit (q_ordering (Some m_phys))
                    (FSet.add m_phys alloc)
                in
                let alloc' = FSet.add n_phys (FSet.add m_phys alloc) in
                let lay' = add (add lay m m_phys) n n_phys in
                build_layout t0 alloc' lay' q_ordering))
        | _ -> build_layout t0 alloc lay q_ordering)
     | _ -> build_layout t0 alloc lay q_ordering)

(** val extend_layout : int -> FSet.t -> layout -> int list -> layout **)

let rec extend_layout n alloc lay prefs =
  (fun fO fS n -> if n=0 then fO () else fS (n-1))
    (fun _ -> lay)
    (fun n' ->
    match find_phys lay n' with
    | Some _ -> extend_layout n' alloc lay prefs
    | None ->
      let m = get_fresh_qubit prefs alloc in
      extend_layout n' (FSet.add m alloc) (add lay n' m) prefs)
    n

(** val greedy_layout :
    'a1 coq_Map_Unitary gate_list -> int -> (int option -> int list) -> layout **)

let greedy_layout l n q_ordering =
  let (alloc, lay) = build_layout l FSet.empty empty q_ordering in
  extend_layout n alloc lay (q_ordering None)
