open FMapAVL
open OrderedTypeEx
open PeanoNat

type __ = Obj.t

module FMap = Make(Nat_as_OT)

type layout = int FMap.t * int FMap.t

(** val empty : layout **)

let empty =
  (FMap.empty, FMap.empty)

(** val find_phys : layout -> FMap.key -> int option **)

let find_phys lay l =
  FMap.find l (fst lay)

(** val get_phys : layout -> FMap.key -> int **)

let get_phys lay l =
  match find_phys lay l with
  | Some p -> p
  | None -> 0

(** val find_log : layout -> FMap.key -> int option **)

let find_log lay p =
  FMap.find p (snd lay)

(** val get_log : layout -> FMap.key -> int **)

let get_log lay p =
  match find_log lay p with
  | Some l -> l
  | None -> 0

(** val add : layout -> FMap.key -> int -> layout **)

let add lay l p =
  ((FMap.add l p (fst lay)), (FMap.add p l (snd lay)))

(** val swap_log : layout -> FMap.key -> FMap.key -> layout **)

let swap_log lay p1 p2 =
  match find_log lay p1 with
  | Some l1 ->
    (match find_log lay p2 with
     | Some l2 -> add (add lay l1 p2) l2 p1
     | None -> lay)
  | None -> lay

(** val trivial_layout : int -> layout **)

let rec trivial_layout n =
  (fun fO fS n -> if n=0 then fO () else fS (n-1))
    (fun _ -> empty)
    (fun n' -> add (trivial_layout n') n' n')
    n

(** val layout_to_list' : int -> layout -> int option list **)

let rec layout_to_list' x m =
  (fun fO fS n -> if n=0 then fO () else fS (n-1))
    (fun _ -> [])
    (fun x' -> List.append (layout_to_list' x' m) ((find_log m x') :: []))
    x

(** val layout_to_list : int -> layout -> int option list **)

let layout_to_list =
  layout_to_list'

(** val list_to_layout' : FMap.key list -> int -> layout **)

let rec list_to_layout' l acc =
  match l with
  | [] -> empty
  | h :: t0 ->
    let m' = list_to_layout' t0 (Pervasives.succ acc) in add m' h acc

(** val list_to_layout : FMap.key list -> layout **)

let list_to_layout l =
  list_to_layout' l 0

(** val nodup : int list -> bool **)

let rec nodup = function
| [] -> true
| x :: xs -> if List.exists (fun y -> (=) y x) xs then false else nodup xs

(** val check_list : int list -> bool **)

let check_list l =
  (&&) (List.for_all (fun x -> Nat.ltb x (List.length l)) l) (nodup l)
