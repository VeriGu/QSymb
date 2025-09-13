open List
open MappingGateSet
open UnitaryListRepresentation

module FullGateSet =
 struct
  type coq_Full_Unitary =
  | U_I
  | U_X
  | U_Y
  | U_Z
  | U_H
  | U_S
  | U_T
  | U_Sdg
  | U_Tdg
  | U_Rx of float
  | U_Ry of float
  | U_Rz of float
  | U_Rzq of Q.t
  | U_U1 of float
  | U_U2 of float * float
  | U_U3 of float * float * float
  | U_CX
  | U_CZ
  | U_SWAP
  | U_CCX
  | U_CCZ

  type coq_U = coq_Full_Unitary

  (** val match_gate : int -> coq_U -> coq_U -> bool **)

  let match_gate _ u u' =
    match u with
    | U_I -> (match u' with
              | U_I -> true
              | _ -> false)
    | U_X -> (match u' with
              | U_X -> true
              | _ -> false)
    | U_Y -> (match u' with
              | U_Y -> true
              | _ -> false)
    | U_Z -> (match u' with
              | U_Z -> true
              | _ -> false)
    | U_H -> (match u' with
              | U_H -> true
              | _ -> false)
    | U_S -> (match u' with
              | U_S -> true
              | _ -> false)
    | U_T -> (match u' with
              | U_T -> true
              | _ -> false)
    | U_Sdg -> (match u' with
                | U_Sdg -> true
                | _ -> false)
    | U_Tdg -> (match u' with
                | U_Tdg -> true
                | _ -> false)
    | U_Rx r -> (match u' with
                 | U_Rx r' -> ( = ) r r'
                 | _ -> false)
    | U_Ry r -> (match u' with
                 | U_Ry r' -> ( = ) r r'
                 | _ -> false)
    | U_Rz r -> (match u' with
                 | U_Rz r' -> ( = ) r r'
                 | _ -> false)
    | U_Rzq q -> (match u' with
                  | U_Rzq q' -> Q.equal q q'
                  | _ -> false)
    | U_U1 r -> (match u' with
                 | U_U1 r' -> ( = ) r r'
                 | _ -> false)
    | U_U2 (r1, r2) ->
      (match u' with
       | U_U2 (r1', r2') -> (&&) (( = ) r1 r1') (( = ) r2 r2')
       | _ -> false)
    | U_U3 (r1, r2, r3) ->
      (match u' with
       | U_U3 (r1', r2', r3') ->
         (&&) ((&&) (( = ) r1 r1') (( = ) r2 r2')) (( = ) r3 r3')
       | _ -> false)
    | U_CX -> (match u' with
               | U_CX -> true
               | _ -> false)
    | U_CZ -> (match u' with
               | U_CZ -> true
               | _ -> false)
    | U_SWAP -> (match u' with
                 | U_SWAP -> true
                 | _ -> false)
    | U_CCX -> (match u' with
                | U_CCX -> true
                | _ -> false)
    | U_CCZ -> (match u' with
                | U_CCZ -> true
                | _ -> false)
 end

type full_ucom_l = FullGateSet.coq_Full_Unitary gate_list

(** val change_gate_set' :
    ('a1 gate_app -> 'a2 gate_list) -> 'a1 gate_list -> 'a2 gate_list -> 'a2
    gate_list **)

let rec change_gate_set' f l acc =
  match l with
  | [] -> List.rev acc
  | g :: t -> change_gate_set' f t (List.append (List.rev (f g)) acc)

(** val change_gate_set :
    ('a1 gate_app -> 'a2 gate_list) -> 'a1 gate_list -> 'a2 gate_list **)

let change_gate_set f l =
  change_gate_set' f l []

(** val full_to_IBM_u :
    FullGateSet.coq_Full_Unitary gate_app -> IBMGateSet.coq_IBM_ucom_l **)

let full_to_IBM_u = function
| App1 (f, m) ->
  (match f with
   | FullGateSet.U_I -> (IBMGateSet.coq_Rz (Float.of_int 0) m) :: []
   | FullGateSet.U_X -> (IBMGateSet.coq_X m) :: []
   | FullGateSet.U_Y -> (IBMGateSet.coq_Y m) :: []
   | FullGateSet.U_Z -> (IBMGateSet.coq_Z m) :: []
   | FullGateSet.U_H -> (IBMGateSet.coq_H m) :: []
   | FullGateSet.U_S -> (IBMGateSet.coq_P m) :: []
   | FullGateSet.U_T -> (IBMGateSet.coq_T m) :: []
   | FullGateSet.U_Sdg -> (IBMGateSet.coq_PDAG m) :: []
   | FullGateSet.U_Tdg -> (IBMGateSet.coq_TDAG m) :: []
   | FullGateSet.U_Rx r -> (IBMGateSet.coq_Rx r m) :: []
   | FullGateSet.U_Ry r -> (IBMGateSet.coq_Ry r m) :: []
   | FullGateSet.U_Rz r -> (IBMGateSet.coq_Rz r m) :: []
   | FullGateSet.U_Rzq q ->
     (IBMGateSet.coq_Rz (( *. ) (Q.to_float q) Float.pi) m) :: []
   | FullGateSet.U_U1 r -> (IBMGateSet.coq_U1 r m) :: []
   | FullGateSet.U_U2 (r1, r2) -> (IBMGateSet.coq_U2 r1 r2 m) :: []
   | FullGateSet.U_U3 (r1, r2, r3) -> (IBMGateSet.coq_U3 r1 r2 r3 m) :: []
   | _ -> [])
| App2 (f, m, n) ->
  (match f with
   | FullGateSet.U_CX -> (IBMGateSet.coq_CNOT m n) :: []
   | FullGateSet.U_CZ -> IBMGateSet.coq_CZ m n
   | FullGateSet.U_SWAP -> IBMGateSet.coq_SWAP m n
   | _ -> [])
| App3 (f, m, n, p) ->
  (match f with
   | FullGateSet.U_CCX -> IBMGateSet.coq_CCX m n p
   | FullGateSet.U_CCZ -> IBMGateSet.coq_CCZ m n p
   | _ -> [])

(** val coq_IBM_to_full_u :
    IBMGateSet.IBMGateSet.coq_IBM_Unitary gate_app -> full_ucom_l **)

let coq_IBM_to_full_u = function
| App1 (i, m) ->
  (match i with
   | IBMGateSet.IBMGateSet.UIBM_U1 a -> (App1 ((FullGateSet.U_U1 a), m)) :: []
   | IBMGateSet.IBMGateSet.UIBM_U2 (a, b) ->
     (App1 ((FullGateSet.U_U2 (a, b)), m)) :: []
   | IBMGateSet.IBMGateSet.UIBM_U3 (a, b, c) ->
     (App1 ((FullGateSet.U_U3 (a, b, c)), m)) :: []
   | IBMGateSet.IBMGateSet.UIBM_CNOT -> [])
| App2 (i, m, n) ->
  (match i with
   | IBMGateSet.IBMGateSet.UIBM_CNOT -> (App2 (FullGateSet.U_CX, m, n)) :: []
   | _ -> [])
| App3 (_, _, _, _) -> []

(** val full_to_IBM : full_ucom_l -> IBMGateSet.coq_IBM_ucom_l **)

let full_to_IBM l =
  change_gate_set full_to_IBM_u l

(** val coq_IBM_to_full : IBMGateSet.coq_IBM_ucom_l -> full_ucom_l **)

let coq_IBM_to_full l =
  change_gate_set coq_IBM_to_full_u l

(** val coq_R2Q_PI : float -> Q.t **)

let coq_R2Q_PI x =
  Q.of_float (( /. ) x Float.pi)

(** val coq_Rx : float -> int -> RzQGateSet.coq_RzQ_ucom_l **)

let coq_Rx a q =
  (RzQGateSet.coq_H q) :: ((RzQGateSet.coq_Rzq (coq_R2Q_PI a) q) :: (
    (RzQGateSet.coq_H q) :: []))

(** val coq_Rz :
    float -> int -> RzQGateSet.RzQGateSet.coq_RzQ_Unitary gate_app **)

let coq_Rz a q =
  App1 ((RzQGateSet.RzQGateSet.URzQ_Rz (coq_R2Q_PI a)), q)

(** val coq_Ry : float -> int -> RzQGateSet.coq_RzQ_ucom_l **)

let coq_Ry a q =
  (RzQGateSet.coq_PDAG q) :: ((RzQGateSet.coq_H q) :: ((RzQGateSet.coq_Rzq
                                                         (coq_R2Q_PI a) q) :: (
    (RzQGateSet.coq_H q) :: ((RzQGateSet.coq_P q) :: []))))

(** val coq_U1 :
    float -> int -> RzQGateSet.RzQGateSet.coq_RzQ_Unitary gate_app **)

let coq_U1 =
  coq_Rz

(** val coq_U2 : float -> float -> int -> RzQGateSet.coq_RzQ_ucom_l **)

let coq_U2 a b q =
  (RzQGateSet.coq_Rzq (coq_R2Q_PI (( -. ) b Float.pi)) q) :: ((RzQGateSet.coq_H
                                                                q) :: (
    (RzQGateSet.coq_Rzq (coq_R2Q_PI a) q) :: []))

(** val coq_U3 :
    float -> float -> float -> int -> RzQGateSet.coq_RzQ_ucom_l **)

let coq_U3 a b c q =
  (RzQGateSet.coq_Rzq
    (coq_R2Q_PI (( -. ) c (( /. ) Float.pi (Float.of_int ((fun p->2*p) 1)))))
    q) :: ((RzQGateSet.coq_H q) :: ((RzQGateSet.coq_Rzq (coq_R2Q_PI a) q) :: (
    (RzQGateSet.coq_H q) :: ((RzQGateSet.coq_Rzq
                               (coq_R2Q_PI
                                 (( +. ) b
                                   (( /. ) Float.pi
                                     (Float.of_int ((fun p->2*p) 1))))) q) :: []))))

(** val full_to_RzQ_u :
    FullGateSet.coq_Full_Unitary gate_app -> RzQGateSet.coq_RzQ_ucom_l **)

let full_to_RzQ_u = function
| App1 (f, m) ->
  (match f with
   | FullGateSet.U_I -> (RzQGateSet.coq_Rzq (Q.of_int 0) m) :: []
   | FullGateSet.U_X -> (RzQGateSet.coq_X m) :: []
   | FullGateSet.U_Y -> RzQGateSet.coq_Y m
   | FullGateSet.U_Z -> (RzQGateSet.coq_Z m) :: []
   | FullGateSet.U_H -> (RzQGateSet.coq_H m) :: []
   | FullGateSet.U_S -> (RzQGateSet.coq_P m) :: []
   | FullGateSet.U_T -> (RzQGateSet.coq_T m) :: []
   | FullGateSet.U_Sdg -> (RzQGateSet.coq_PDAG m) :: []
   | FullGateSet.U_Tdg -> (RzQGateSet.coq_TDAG m) :: []
   | FullGateSet.U_Rx r -> coq_Rx r m
   | FullGateSet.U_Ry r -> coq_Ry r m
   | FullGateSet.U_Rz r -> (coq_Rz r m) :: []
   | FullGateSet.U_Rzq q -> (RzQGateSet.coq_Rzq q m) :: []
   | FullGateSet.U_U1 r -> (coq_U1 r m) :: []
   | FullGateSet.U_U2 (r1, r2) -> coq_U2 r1 r2 m
   | FullGateSet.U_U3 (r1, r2, r3) -> coq_U3 r1 r2 r3 m
   | _ -> [])
| App2 (f, m, n) ->
  (match f with
   | FullGateSet.U_CX -> (RzQGateSet.coq_CNOT m n) :: []
   | FullGateSet.U_CZ -> RzQGateSet.coq_CZ m n
   | FullGateSet.U_SWAP -> RzQGateSet.coq_SWAP m n
   | _ -> [])
| App3 (f, m, n, p) ->
  (match f with
   | FullGateSet.U_CCX -> RzQGateSet.coq_CCX m n p
   | FullGateSet.U_CCZ -> RzQGateSet.coq_CCZ m n p
   | _ -> [])

(** val coq_RzQ_to_full_u :
    RzQGateSet.RzQGateSet.coq_RzQ_Unitary gate_app -> full_ucom_l **)

let coq_RzQ_to_full_u = function
| App1 (r, m) ->
  (match r with
   | RzQGateSet.RzQGateSet.URzQ_H -> (App1 (FullGateSet.U_H, m)) :: []
   | RzQGateSet.RzQGateSet.URzQ_X -> (App1 (FullGateSet.U_X, m)) :: []
   | RzQGateSet.RzQGateSet.URzQ_Rz q ->
     (App1 ((FullGateSet.U_Rzq q), m)) :: []
   | RzQGateSet.RzQGateSet.URzQ_CNOT -> [])
| App2 (r, m, n) ->
  (match r with
   | RzQGateSet.RzQGateSet.URzQ_CNOT -> (App2 (FullGateSet.U_CX, m, n)) :: []
   | _ -> [])
| App3 (_, _, _, _) -> []

(** val full_to_RzQ : full_ucom_l -> RzQGateSet.coq_RzQ_ucom_l **)

let full_to_RzQ l =
  change_gate_set full_to_RzQ_u l

(** val coq_RzQ_to_full : RzQGateSet.coq_RzQ_ucom_l -> full_ucom_l **)

let coq_RzQ_to_full l =
  change_gate_set coq_RzQ_to_full_u l

(** val decompose_to_cnot_and_swap_u :
    FullGateSet.coq_Full_Unitary gate_app -> full_ucom_l **)

let decompose_to_cnot_and_swap_u g = match g with
| App1 (_, _) -> g :: []
| App2 (f, m, n) ->
  (match f with
   | FullGateSet.U_CZ ->
     (App1 (FullGateSet.U_H, n)) :: ((App2 (FullGateSet.U_CX, m,
       n)) :: ((App1 (FullGateSet.U_H, n)) :: []))
   | _ -> g :: [])
| App3 (f, m, n, p) ->
  (match f with
   | FullGateSet.U_CCX ->
     (App1 (FullGateSet.U_H, p)) :: ((App2 (FullGateSet.U_CX, n,
       p)) :: ((App1 (FullGateSet.U_Tdg, p)) :: ((App2 (FullGateSet.U_CX, m,
       p)) :: ((App1 (FullGateSet.U_T, p)) :: ((App2 (FullGateSet.U_CX, n,
       p)) :: ((App1 (FullGateSet.U_Tdg, p)) :: ((App2 (FullGateSet.U_CX, m,
       p)) :: ((App2 (FullGateSet.U_CX, m, n)) :: ((App1 (FullGateSet.U_Tdg,
       n)) :: ((App2 (FullGateSet.U_CX, m, n)) :: ((App1 (FullGateSet.U_T,
       m)) :: ((App1 (FullGateSet.U_T, n)) :: ((App1 (FullGateSet.U_T,
       p)) :: ((App1 (FullGateSet.U_H, p)) :: []))))))))))))))
   | FullGateSet.U_CCZ ->
     (App2 (FullGateSet.U_CX, n, p)) :: ((App1 (FullGateSet.U_Tdg,
       p)) :: ((App2 (FullGateSet.U_CX, m, p)) :: ((App1 (FullGateSet.U_T,
       p)) :: ((App2 (FullGateSet.U_CX, n, p)) :: ((App1 (FullGateSet.U_Tdg,
       p)) :: ((App2 (FullGateSet.U_CX, m, p)) :: ((App2 (FullGateSet.U_CX,
       m, n)) :: ((App1 (FullGateSet.U_Tdg, n)) :: ((App2 (FullGateSet.U_CX,
       m, n)) :: ((App1 (FullGateSet.U_T, m)) :: ((App1 (FullGateSet.U_T,
       n)) :: ((App1 (FullGateSet.U_T, p)) :: []))))))))))))
   | _ -> g :: [])

(** val full_to_map_u :
    FullGateSet.coq_Full_Unitary gate_app -> FullGateSet.coq_Full_Unitary
    coq_Map_Unitary gate_app **)

let full_to_map_u = function
| App1 (u, m) -> App1 ((UMap_U u), m)
| App2 (f, m, n) ->
  (match f with
   | FullGateSet.U_CX -> App2 (UMap_CNOT, m, n)
   | FullGateSet.U_SWAP -> App2 (UMap_SWAP, m, n)
   | _ -> App1 ((UMap_U FullGateSet.U_I), 0))
| App3 (_, _, _, _) -> App1 ((UMap_U FullGateSet.U_I), 0)

(** val full_to_map :
    full_ucom_l -> FullGateSet.coq_Full_Unitary map_ucom_l **)

let full_to_map l =
  change_gate_set (fun g ->
    map full_to_map_u (decompose_to_cnot_and_swap_u g)) l

(** val map_to_full_u :
    FullGateSet.coq_Full_Unitary coq_Map_Unitary gate_app -> full_ucom_l **)

let map_to_full_u = function
| App1 (m0, m) -> (match m0 with
                   | UMap_U u -> (App1 (u, m)) :: []
                   | _ -> [])
| App2 (m0, m, n) ->
  (match m0 with
   | UMap_U _ -> []
   | UMap_CNOT -> (App2 (FullGateSet.U_CX, m, n)) :: []
   | UMap_SWAP -> (App2 (FullGateSet.U_SWAP, m, n)) :: [])
| App3 (_, _, _, _) -> []

(** val map_to_full :
    FullGateSet.coq_Full_Unitary map_ucom_l -> full_ucom_l **)

let map_to_full l =
  change_gate_set map_to_full_u l

(** val decompose_to_cnot_u :
    FullGateSet.coq_Full_Unitary gate_app -> full_ucom_l **)

let decompose_to_cnot_u g = match g with
| App1 (_, _) -> g :: []
| App2 (f, m, n) ->
  (match f with
   | FullGateSet.U_CZ ->
     (App1 (FullGateSet.U_H, n)) :: ((App2 (FullGateSet.U_CX, m,
       n)) :: ((App1 (FullGateSet.U_H, n)) :: []))
   | FullGateSet.U_SWAP ->
     (App2 (FullGateSet.U_CX, m, n)) :: ((App2 (FullGateSet.U_CX, n,
       m)) :: ((App2 (FullGateSet.U_CX, m, n)) :: []))
   | _ -> g :: [])
| App3 (f, m, n, p) ->
  (match f with
   | FullGateSet.U_CCX ->
     (App1 (FullGateSet.U_H, p)) :: ((App2 (FullGateSet.U_CX, n,
       p)) :: ((App1 (FullGateSet.U_Tdg, p)) :: ((App2 (FullGateSet.U_CX, m,
       p)) :: ((App1 (FullGateSet.U_T, p)) :: ((App2 (FullGateSet.U_CX, n,
       p)) :: ((App1 (FullGateSet.U_Tdg, p)) :: ((App2 (FullGateSet.U_CX, m,
       p)) :: ((App2 (FullGateSet.U_CX, m, n)) :: ((App1 (FullGateSet.U_Tdg,
       n)) :: ((App2 (FullGateSet.U_CX, m, n)) :: ((App1 (FullGateSet.U_T,
       m)) :: ((App1 (FullGateSet.U_T, n)) :: ((App1 (FullGateSet.U_T,
       p)) :: ((App1 (FullGateSet.U_H, p)) :: []))))))))))))))
   | FullGateSet.U_CCZ ->
     (App2 (FullGateSet.U_CX, n, p)) :: ((App1 (FullGateSet.U_Tdg,
       p)) :: ((App2 (FullGateSet.U_CX, m, p)) :: ((App1 (FullGateSet.U_T,
       p)) :: ((App2 (FullGateSet.U_CX, n, p)) :: ((App1 (FullGateSet.U_Tdg,
       p)) :: ((App2 (FullGateSet.U_CX, m, p)) :: ((App2 (FullGateSet.U_CX,
       m, n)) :: ((App1 (FullGateSet.U_Tdg, n)) :: ((App2 (FullGateSet.U_CX,
       m, n)) :: ((App1 (FullGateSet.U_T, m)) :: ((App1 (FullGateSet.U_T,
       n)) :: ((App1 (FullGateSet.U_T, p)) :: []))))))))))))
   | _ -> g :: [])

(** val decompose_to_cnot :
    full_ucom_l -> FullGateSet.coq_Full_Unitary gate_list **)

let decompose_to_cnot l =
  change_gate_set decompose_to_cnot_u l

(** val convert_to_ibm : full_ucom_l -> full_ucom_l **)

let convert_to_ibm l =
  coq_IBM_to_full (full_to_IBM l)

(** val convert_to_rzq : full_ucom_l -> full_ucom_l **)

let convert_to_rzq l =
  coq_RzQ_to_full (full_to_RzQ l)

(** val replace_rzq_u :
    FullGateSet.coq_Full_Unitary gate_app -> full_ucom_l **)

let replace_rzq_u g = match g with
| App1 (f, m) ->
  (match f with
   | FullGateSet.U_Rzq q ->
     if Q.equal q (Q.of_int 0)
     then (App1 (FullGateSet.U_I, m)) :: []
     else if Q.equal q (Q.of_int 1)
          then (App1 (FullGateSet.U_Z, m)) :: []
          else if Q.equal q (Q.of_ints 1 2)
               then (App1 (FullGateSet.U_S, m)) :: []
               else if Q.equal q (Q.of_ints 3 2)
                    then (App1 (FullGateSet.U_Sdg, m)) :: []
                    else if Q.equal q (Q.of_ints 1 4)
                         then (App1 (FullGateSet.U_T, m)) :: []
                         else if Q.equal q (Q.of_ints 7 4)
                              then (App1 (FullGateSet.U_Tdg, m)) :: []
                              else (App1 ((FullGateSet.U_Rz
                                     (( *. ) (Q.to_float q) Float.pi)),
                                     m)) :: []
   | _ -> g :: [])
| _ -> g :: []

(** val replace_rzq :
    full_ucom_l -> FullGateSet.coq_Full_Unitary gate_list **)

let replace_rzq l =
  change_gate_set replace_rzq_u l
