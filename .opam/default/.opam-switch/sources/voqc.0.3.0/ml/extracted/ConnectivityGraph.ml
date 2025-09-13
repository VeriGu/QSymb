open Layouts
open PeanoNat

(** val list_nats : int -> int option -> int list **)

let rec list_nats n o =
  (fun fO fS n -> if n=0 then fO () else fS (n-1))
    (fun _ -> [])
    (fun n' ->
    match o with
    | Some x -> if (=) x n' then list_nats n' o else n' :: (list_nats n' o)
    | None -> n' :: (list_nats n' o))
    n

(** val merge_path : int list -> int list -> int list **)

let rec merge_path p1 p2 =
  match p1 with
  | [] -> p2
  | h :: t -> (match t with
               | [] -> p2
               | _ :: _ -> h :: (merge_path t p2))

(** val interleave : int list -> int list -> int list **)

let rec interleave l1 l2 =
  match l1 with
  | [] -> l2
  | h1 :: t1 ->
    (match l2 with
     | [] -> l1
     | h2 :: t2 -> h1 :: (h2 :: (interleave t1 t2)))

module LNN =
 struct
  (** val is_in_graph : int -> int -> int -> bool **)

  let is_in_graph dim n1 n2 =
    (||)
      ((&&) (Nat.ltb ((+) n1 (Pervasives.succ 0)) dim)
        ((=) n2 ((+) n1 (Pervasives.succ 0))))
      ((&&) (Nat.ltb ((+) n2 (Pervasives.succ 0)) dim)
        ((=) n1 ((+) n2 (Pervasives.succ 0))))

  (** val move_left : int -> int -> int list **)

  let rec move_left n dist =
    (fun fO fS n -> if n=0 then fO () else fS (n-1))
      (fun _ -> n :: [])
      (fun n' -> n :: (move_left ((-) n (Pervasives.succ 0)) n'))
      dist

  (** val move_right : int -> int -> int list **)

  let rec move_right n dist =
    (fun fO fS n -> if n=0 then fO () else fS (n-1))
      (fun _ -> n :: [])
      (fun n' -> n :: (move_right ((+) n (Pervasives.succ 0)) n'))
      dist

  (** val get_path : int -> int -> int list **)

  let get_path n1 n2 =
    if Nat.ltb n1 n2
    then move_right n1 ((-) n2 n1)
    else if Nat.ltb n2 n1 then move_left n1 ((-) n1 n2) else n1 :: []

  (** val get_nearby : int -> int -> int list **)

  let get_nearby dim n =
    if not (Nat.ltb n dim)
    then list_nats dim None
    else if Nat.ltb dim (Pervasives.succ (Pervasives.succ 0))
         then []
         else if (=) n 0
              then get_path (Pervasives.succ 0) ((-) dim (Pervasives.succ 0))
              else if (=) n ((-) dim (Pervasives.succ 0))
                   then get_path
                          ((-) dim (Pervasives.succ (Pervasives.succ 0))) 0
                   else interleave (get_path ((-) n (Pervasives.succ 0)) 0)
                          (get_path ((+) n (Pervasives.succ 0))
                            ((-) dim (Pervasives.succ 0)))

  (** val q_ordering : int -> int option -> int list **)

  let q_ordering dim = function
  | Some x -> get_nearby dim x
  | None ->
    if (=) dim 0
    then []
    else (Nat.div dim (Pervasives.succ (Pervasives.succ 0))) :: (get_nearby
                                                                  dim
                                                                  (Nat.div
                                                                    dim
                                                                    (Pervasives.succ
                                                                    (Pervasives.succ
                                                                    0))))
 end

module LNNRing =
 struct
  (** val is_in_graph : int -> int -> int -> bool **)

  let is_in_graph dim n1 n2 =
    (&&) (Nat.ltb (Pervasives.succ 0) dim)
      ((||)
        ((&&) (Nat.ltb n1 dim)
          ((=) n2 (Nat.modulo ((+) n1 (Pervasives.succ 0)) dim)))
        ((&&) (Nat.ltb n2 dim)
          ((=) n1 (Nat.modulo ((+) n2 (Pervasives.succ 0)) dim))))

  (** val move_cw : int -> int -> int -> int list **)

  let rec move_cw dim n dist =
    (fun fO fS n -> if n=0 then fO () else fS (n-1))
      (fun _ -> n :: [])
      (fun dist' ->
      n :: (move_cw dim (Nat.modulo ((+) n (Pervasives.succ 0)) dim) dist'))
      dist

  (** val move_ccw : int -> int -> int -> int list **)

  let rec move_ccw dim n dist =
    (fun fO fS n -> if n=0 then fO () else fS (n-1))
      (fun _ -> n :: [])
      (fun dist' ->
      n :: (move_ccw dim
             (Nat.modulo ((-) ((+) dim n) (Pervasives.succ 0)) dim) dist'))
      dist

  (** val get_path : int -> int -> int -> int list **)

  let get_path dim n1 n2 =
    if Nat.ltb n1 n2
    then let dist_cw = (-) n2 n1 in
         let dist_ccw = (-) ((+) dim n1) n2 in
         if Nat.ltb dist_cw dist_ccw
         then move_cw dim n1 dist_cw
         else move_ccw dim n1 dist_ccw
    else if Nat.ltb n2 n1
         then let dist_cw = (-) ((+) dim n2) n1 in
              let dist_ccw = (-) n1 n2 in
              if Nat.ltb dist_cw dist_ccw
              then move_cw dim n1 dist_cw
              else move_ccw dim n1 dist_ccw
         else n1 :: []

  (** val get_nearby : int -> int -> int list **)

  let get_nearby dim n =
    if not (Nat.ltb n dim)
    then list_nats dim None
    else if Nat.ltb dim (Pervasives.succ (Pervasives.succ 0))
         then []
         else let mid = Nat.div dim (Pervasives.succ (Pervasives.succ 0)) in
              interleave
                (move_ccw dim
                  (Nat.modulo ((-) ((+) dim n) (Pervasives.succ 0)) dim)
                  ((-) mid (Pervasives.succ 0)))
                (move_cw dim (Nat.modulo ((+) n (Pervasives.succ 0)) dim)
                  ((-) ((-) dim mid) (Pervasives.succ (Pervasives.succ 0))))

  (** val q_ordering' : int -> int option -> int list **)

  let q_ordering' dim = function
  | Some x -> get_nearby dim x
  | None -> if (=) dim 0 then [] else 0 :: (get_nearby dim 0)

  (** val q_ordering : int -> int option -> int list **)

  let q_ordering dim o =
    let attempt = q_ordering' dim o in
    let backup = list_nats dim o in
    (match o with
     | Some x ->
       if Nat.ltb x dim
       then if (&&) ((=) (List.length attempt) ((-) dim (Pervasives.succ 0)))
                 (check_list (x :: attempt))
            then attempt
            else backup
       else backup
     | None ->
       if (&&) ((=) (List.length attempt) dim) (check_list attempt)
       then attempt
       else backup)
 end

module Grid =
 struct
  (** val is_in_graph : int -> int -> int -> int -> bool **)

  let is_in_graph numRows numCols i i' =
    (||)
      ((||)
        ((||)
          ((&&) (Nat.ltb ((+) i numCols) (( * ) numRows numCols))
            ((=) i' ((+) i numCols)))
          ((&&) (Nat.ltb ((+) i' numCols) (( * ) numRows numCols))
            ((=) i ((+) i' numCols))))
        ((&&) (Nat.ltb ((+) i (Pervasives.succ 0)) (( * ) numRows numCols))
          ((=) i' ((+) i (Pervasives.succ 0)))))
      ((&&) (Nat.ltb ((+) i' (Pervasives.succ 0)) (( * ) numRows numCols))
        ((=) i ((+) i' (Pervasives.succ 0))))

  (** val row : int -> int -> int **)

  let row numCols i =
    Nat.div i numCols

  (** val col : int -> int -> int **)

  let col numCols i =
    Nat.modulo i numCols

  (** val move_up : int -> int -> int **)

  let move_up numCols i =
    (-) i numCols

  (** val move_down : int -> int -> int **)

  let move_down numCols i =
    (+) i numCols

  (** val move_left : int -> int **)

  let move_left i =
    (-) i (Pervasives.succ 0)

  (** val move_right : int -> int **)

  let move_right i =
    (+) i (Pervasives.succ 0)

  (** val repeat_move : (int -> int) -> int -> int -> int list **)

  let rec repeat_move f i dist =
    (fun fO fS n -> if n=0 then fO () else fS (n-1))
      (fun _ -> i :: [])
      (fun dist' -> i :: (repeat_move f (f i) dist'))
      dist

  (** val get_path : int -> int -> int -> int list **)

  let get_path numCols i1 i2 =
    let r1 = row numCols i1 in
    let c1 = col numCols i1 in
    let r2 = row numCols i2 in
    let c2 = col numCols i2 in
    if (&&) (Nat.ltb r1 r2) (Nat.ltb c1 c2)
    then let p1 = repeat_move (move_down numCols) i1 ((-) r2 r1) in
         let p2 =
           repeat_move move_right ((+) i1 (( * ) ((-) r2 r1) numCols))
             ((-) c2 c1)
         in
         merge_path p1 p2
    else if (&&) (Nat.ltb r1 r2) ((=) c1 c2)
         then repeat_move (move_down numCols) i1 ((-) r2 r1)
         else if (&&) (Nat.ltb r1 r2) (Nat.ltb c2 c1)
              then let p1 = repeat_move move_left i1 ((-) c1 c2) in
                   let p2 =
                     repeat_move (move_down numCols) ((-) i1 ((-) c1 c2))
                       ((-) r2 r1)
                   in
                   merge_path p1 p2
              else if (&&) ((=) r1 r2) (Nat.ltb c1 c2)
                   then repeat_move move_right i1 ((-) c2 c1)
                   else if (&&) ((=) r1 r2) ((=) c1 c2)
                        then []
                        else if (&&) ((=) r1 r2) (Nat.ltb c2 c1)
                             then repeat_move move_left i1 ((-) c1 c2)
                             else if (&&) (Nat.ltb r2 r1) (Nat.ltb c1 c2)
                                  then let p1 =
                                         repeat_move (move_up numCols) i1
                                           ((-) r1 r2)
                                       in
                                       let p2 =
                                         repeat_move move_right
                                           ((-) i1
                                             (( * ) ((-) r1 r2) numCols))
                                           ((-) c2 c1)
                                       in
                                       merge_path p1 p2
                                  else if (&&) (Nat.ltb r2 r1) ((=) c1 c2)
                                       then repeat_move (move_up numCols) i1
                                              ((-) r1 r2)
                                       else if (&&) (Nat.ltb r2 r1)
                                                 (Nat.ltb c2 c1)
                                            then let p1 =
                                                   repeat_move
                                                     (move_up numCols) i1
                                                     ((-) r1 r2)
                                                 in
                                                 let p2 =
                                                   repeat_move move_left
                                                     ((-) i1
                                                       (( * ) ((-) r1 r2)
                                                         numCols)) ((-) c1 c2)
                                                 in
                                                 merge_path p1 p2
                                            else []
 end
