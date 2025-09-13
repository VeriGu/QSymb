open UnitaryListRepresentation

module FullGateSet :
 sig
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

  val match_gate : int -> coq_U -> coq_U -> bool
 end

type full_ucom_l = FullGateSet.coq_Full_Unitary gate_list

val change_gate_set :
  ('a1 gate_app -> 'a2 gate_list) -> 'a1 gate_list -> 'a2 gate_list

val full_to_IBM : full_ucom_l -> IBMGateSet.coq_IBM_ucom_l

val coq_IBM_to_full : IBMGateSet.coq_IBM_ucom_l -> full_ucom_l

val full_to_RzQ : full_ucom_l -> RzQGateSet.coq_RzQ_ucom_l

val coq_RzQ_to_full : RzQGateSet.coq_RzQ_ucom_l -> full_ucom_l

val decompose_to_cnot : full_ucom_l -> full_ucom_l

val convert_to_ibm : full_ucom_l -> full_ucom_l

val convert_to_rzq : full_ucom_l -> full_ucom_l

val replace_rzq : full_ucom_l -> full_ucom_l

val full_to_map : full_ucom_l -> FullGateSet.coq_Full_Unitary MappingGateSet.map_ucom_l

val map_to_full : FullGateSet.coq_Full_Unitary MappingGateSet.map_ucom_l -> full_ucom_l