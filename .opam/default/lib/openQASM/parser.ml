
module MenhirBasics = struct
  
  exception Error
  
  let _eRR =
    fun _s ->
      raise Error
  
  type token = 
    | U
    | TIMES
    | TAN
    | STRING of (
# 8 "lib/parser.mly"
       (string)
# 18 "lib/parser.ml"
  )
    | SQRT
    | SIN
    | SEMICOLON
    | RPAREN
    | RESET
    | REAL of (
# 7 "lib/parser.mly"
       (float)
# 28 "lib/parser.ml"
  )
    | RBRACKET
    | RBRACE
    | QREG
    | POW
    | PLUS
    | PI
    | OPENQASM
    | OPAQUE
    | NINT of (
# 6 "lib/parser.mly"
       (int)
# 41 "lib/parser.ml"
  )
    | MINUS
    | MEASURE
    | LPAREN
    | LN
    | LBRACKET
    | LBRACE
    | INCLUDE
    | IF
    | ID of (
# 5 "lib/parser.mly"
       (string)
# 54 "lib/parser.ml"
  )
    | GATE
    | EXP
    | EQUALS
    | EOF
    | DIV
    | CX
    | CREG
    | COS
    | COMMA
    | BARRIER
    | ARROW
  
end

include MenhirBasics

# 1 "lib/parser.mly"
  
  open AST

# 76 "lib/parser.ml"

type ('s, 'r) _menhir_state = 
  | MenhirState003 : ('s _menhir_cell0_REAL, _menhir_box_mainprogram) _menhir_state
    (** State 003.
        Stack shape : REAL.
        Start symbol: mainprogram. *)

  | MenhirState005 : (('s, _menhir_box_mainprogram) _menhir_cell1_U, _menhir_box_mainprogram) _menhir_state
    (** State 005.
        Stack shape : U.
        Start symbol: mainprogram. *)

  | MenhirState012 : (('s, _menhir_box_mainprogram) _menhir_cell1_MINUS, _menhir_box_mainprogram) _menhir_state
    (** State 012.
        Stack shape : MINUS.
        Start symbol: mainprogram. *)

  | MenhirState013 : (('s, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_state
    (** State 013.
        Stack shape : LPAREN.
        Start symbol: mainprogram. *)

  | MenhirState019 : (('s, _menhir_box_mainprogram) _menhir_cell1_unaryop, _menhir_box_mainprogram) _menhir_state
    (** State 019.
        Stack shape : unaryop.
        Start symbol: mainprogram. *)

  | MenhirState021 : (('s, _menhir_box_mainprogram) _menhir_cell1_exp, _menhir_box_mainprogram) _menhir_state
    (** State 021.
        Stack shape : exp.
        Start symbol: mainprogram. *)

  | MenhirState023 : (('s, _menhir_box_mainprogram) _menhir_cell1_exp, _menhir_box_mainprogram) _menhir_state
    (** State 023.
        Stack shape : exp.
        Start symbol: mainprogram. *)

  | MenhirState026 : (('s, _menhir_box_mainprogram) _menhir_cell1_exp, _menhir_box_mainprogram) _menhir_state
    (** State 026.
        Stack shape : exp.
        Start symbol: mainprogram. *)

  | MenhirState028 : (('s, _menhir_box_mainprogram) _menhir_cell1_exp, _menhir_box_mainprogram) _menhir_state
    (** State 028.
        Stack shape : exp.
        Start symbol: mainprogram. *)

  | MenhirState030 : (('s, _menhir_box_mainprogram) _menhir_cell1_exp, _menhir_box_mainprogram) _menhir_state
    (** State 030.
        Stack shape : exp.
        Start symbol: mainprogram. *)

  | MenhirState038 : ((('s, _menhir_box_mainprogram) _menhir_cell1_U, _menhir_box_mainprogram) _menhir_cell1_explist, _menhir_box_mainprogram) _menhir_state
    (** State 038.
        Stack shape : U explist.
        Start symbol: mainprogram. *)

  | MenhirState046 : (('s, _menhir_box_mainprogram) _menhir_cell1_exp, _menhir_box_mainprogram) _menhir_state
    (** State 046.
        Stack shape : exp.
        Start symbol: mainprogram. *)

  | MenhirState048 : (('s, _menhir_box_mainprogram) _menhir_cell1_RESET, _menhir_box_mainprogram) _menhir_state
    (** State 048.
        Stack shape : RESET.
        Start symbol: mainprogram. *)

  | MenhirState058 : (('s, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_state
    (** State 058.
        Stack shape : OPAQUE ID.
        Start symbol: mainprogram. *)

  | MenhirState059 : ((('s, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_state
    (** State 059.
        Stack shape : OPAQUE ID LPAREN.
        Start symbol: mainprogram. *)

  | MenhirState061 : (('s, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_state
    (** State 061.
        Stack shape : ID.
        Start symbol: mainprogram. *)

  | MenhirState066 : (((('s, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_idlist, _menhir_box_mainprogram) _menhir_state
    (** State 066.
        Stack shape : OPAQUE ID LPAREN idlist.
        Start symbol: mainprogram. *)

  | MenhirState071 : (('s, _menhir_box_mainprogram) _menhir_cell1_MEASURE, _menhir_box_mainprogram) _menhir_state
    (** State 071.
        Stack shape : MEASURE.
        Start symbol: mainprogram. *)

  | MenhirState073 : ((('s, _menhir_box_mainprogram) _menhir_cell1_MEASURE, _menhir_box_mainprogram) _menhir_cell1_argument, _menhir_box_mainprogram) _menhir_state
    (** State 073.
        Stack shape : MEASURE argument.
        Start symbol: mainprogram. *)

  | MenhirState084 : (('s, _menhir_box_mainprogram) _menhir_cell1_IF _menhir_cell0_ID _menhir_cell0_NINT, _menhir_box_mainprogram) _menhir_state
    (** State 084.
        Stack shape : IF ID NINT.
        Start symbol: mainprogram. *)

  | MenhirState085 : (('s, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_state
    (** State 085.
        Stack shape : ID.
        Start symbol: mainprogram. *)

  | MenhirState086 : ((('s, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_state
    (** State 086.
        Stack shape : ID LPAREN.
        Start symbol: mainprogram. *)

  | MenhirState088 : (((('s, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_explist, _menhir_box_mainprogram) _menhir_state
    (** State 088.
        Stack shape : ID LPAREN explist.
        Start symbol: mainprogram. *)

  | MenhirState092 : (('s, _menhir_box_mainprogram) _menhir_cell1_argument, _menhir_box_mainprogram) _menhir_state
    (** State 092.
        Stack shape : argument.
        Start symbol: mainprogram. *)

  | MenhirState098 : (('s, _menhir_box_mainprogram) _menhir_cell1_CX, _menhir_box_mainprogram) _menhir_state
    (** State 098.
        Stack shape : CX.
        Start symbol: mainprogram. *)

  | MenhirState100 : ((('s, _menhir_box_mainprogram) _menhir_cell1_CX, _menhir_box_mainprogram) _menhir_cell1_argument, _menhir_box_mainprogram) _menhir_state
    (** State 100.
        Stack shape : CX argument.
        Start symbol: mainprogram. *)

  | MenhirState106 : (('s, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_state
    (** State 106.
        Stack shape : GATE ID.
        Start symbol: mainprogram. *)

  | MenhirState107 : ((('s, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_state
    (** State 107.
        Stack shape : GATE ID LPAREN.
        Start symbol: mainprogram. *)

  | MenhirState109 : (((('s, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_idlist, _menhir_box_mainprogram) _menhir_state
    (** State 109.
        Stack shape : GATE ID LPAREN idlist.
        Start symbol: mainprogram. *)

  | MenhirState118 : (('s, _menhir_box_mainprogram) _menhir_cell1_BARRIER, _menhir_box_mainprogram) _menhir_state
    (** State 118.
        Stack shape : BARRIER.
        Start symbol: mainprogram. *)

  | MenhirState121 : (('s, _menhir_box_mainprogram) _menhir_cell1_statement, _menhir_box_mainprogram) _menhir_state
    (** State 121.
        Stack shape : statement.
        Start symbol: mainprogram. *)

  | MenhirState125 : (('s, _menhir_box_mainprogram) _menhir_cell1_gatedecl, _menhir_box_mainprogram) _menhir_state
    (** State 125.
        Stack shape : gatedecl.
        Start symbol: mainprogram. *)

  | MenhirState126 : (('s, _menhir_box_mainprogram) _menhir_cell1_BARRIER, _menhir_box_mainprogram) _menhir_state
    (** State 126.
        Stack shape : BARRIER.
        Start symbol: mainprogram. *)

  | MenhirState129 : (('s, _menhir_box_mainprogram) _menhir_cell1_uop_or_barrier, _menhir_box_mainprogram) _menhir_state
    (** State 129.
        Stack shape : uop_or_barrier.
        Start symbol: mainprogram. *)


and ('s, 'r) _menhir_cell1_argument = 
  | MenhirCell1_argument of 's * ('s, 'r) _menhir_state * (AST.argument)

and ('s, 'r) _menhir_cell1_exp = 
  | MenhirCell1_exp of 's * ('s, 'r) _menhir_state * (AST.exp)

and ('s, 'r) _menhir_cell1_explist = 
  | MenhirCell1_explist of 's * ('s, 'r) _menhir_state * (AST.exp list)

and ('s, 'r) _menhir_cell1_gatedecl = 
  | MenhirCell1_gatedecl of 's * ('s, 'r) _menhir_state * (AST.gatedecl)

and ('s, 'r) _menhir_cell1_idlist = 
  | MenhirCell1_idlist of 's * ('s, 'r) _menhir_state * (string list)

and ('s, 'r) _menhir_cell1_statement = 
  | MenhirCell1_statement of 's * ('s, 'r) _menhir_state * (AST.statement)

and ('s, 'r) _menhir_cell1_unaryop = 
  | MenhirCell1_unaryop of 's * ('s, 'r) _menhir_state * (AST.unaryop)

and ('s, 'r) _menhir_cell1_uop_or_barrier = 
  | MenhirCell1_uop_or_barrier of 's * ('s, 'r) _menhir_state * (AST.gop)

and ('s, 'r) _menhir_cell1_BARRIER = 
  | MenhirCell1_BARRIER of 's * ('s, 'r) _menhir_state

and ('s, 'r) _menhir_cell1_CX = 
  | MenhirCell1_CX of 's * ('s, 'r) _menhir_state

and ('s, 'r) _menhir_cell1_GATE = 
  | MenhirCell1_GATE of 's * ('s, 'r) _menhir_state

and ('s, 'r) _menhir_cell1_ID = 
  | MenhirCell1_ID of 's * ('s, 'r) _menhir_state * (
# 5 "lib/parser.mly"
       (string)
# 287 "lib/parser.ml"
)

and 's _menhir_cell0_ID = 
  | MenhirCell0_ID of 's * (
# 5 "lib/parser.mly"
       (string)
# 294 "lib/parser.ml"
)

and ('s, 'r) _menhir_cell1_IF = 
  | MenhirCell1_IF of 's * ('s, 'r) _menhir_state

and ('s, 'r) _menhir_cell1_LPAREN = 
  | MenhirCell1_LPAREN of 's * ('s, 'r) _menhir_state

and ('s, 'r) _menhir_cell1_MEASURE = 
  | MenhirCell1_MEASURE of 's * ('s, 'r) _menhir_state

and ('s, 'r) _menhir_cell1_MINUS = 
  | MenhirCell1_MINUS of 's * ('s, 'r) _menhir_state

and 's _menhir_cell0_NINT = 
  | MenhirCell0_NINT of 's * (
# 6 "lib/parser.mly"
       (int)
# 313 "lib/parser.ml"
)

and ('s, 'r) _menhir_cell1_OPAQUE = 
  | MenhirCell1_OPAQUE of 's * ('s, 'r) _menhir_state

and 's _menhir_cell0_REAL = 
  | MenhirCell0_REAL of 's * (
# 7 "lib/parser.mly"
       (float)
# 323 "lib/parser.ml"
)

and ('s, 'r) _menhir_cell1_RESET = 
  | MenhirCell1_RESET of 's * ('s, 'r) _menhir_state

and ('s, 'r) _menhir_cell1_U = 
  | MenhirCell1_U of 's * ('s, 'r) _menhir_state

and _menhir_box_mainprogram = 
  | MenhirBox_mainprogram of (AST.program) [@@unboxed]

let _menhir_action_01 =
  fun xs ->
    let al = 
# 229 "<standard.mly>"
    ( xs )
# 340 "lib/parser.ml"
     in
    (
# 77 "lib/parser.mly"
                                            ( al )
# 345 "lib/parser.ml"
     : (AST.argument list))

let _menhir_action_02 =
  fun name ->
    (
# 82 "lib/parser.mly"
                                  ( (name, None) )
# 353 "lib/parser.ml"
     : (AST.argument))

let _menhir_action_03 =
  fun idx name ->
    (
# 83 "lib/parser.mly"
                                  ( (name, Some idx) )
# 361 "lib/parser.ml"
     : (AST.argument))

let _menhir_action_04 =
  fun name size ->
    (
# 53 "lib/parser.mly"
                                           ( QReg(name, size) )
# 369 "lib/parser.ml"
     : (AST.decl))

let _menhir_action_05 =
  fun name size ->
    (
# 54 "lib/parser.mly"
                                           ( CReg(name, size) )
# 377 "lib/parser.ml"
     : (AST.decl))

let _menhir_action_06 =
  fun r ->
    (
# 88 "lib/parser.mly"
                                  ( Real(r) )
# 385 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_07 =
  fun n ->
    (
# 89 "lib/parser.mly"
                                  ( Nninteger(n) )
# 393 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_08 =
  fun () ->
    (
# 90 "lib/parser.mly"
                                  ( Pi )
# 401 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_09 =
  fun id ->
    (
# 91 "lib/parser.mly"
                                  ( Id(id) )
# 409 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_10 =
  fun e1 e2 ->
    (
# 92 "lib/parser.mly"
                                  ( BinaryOp(Plus, e1, e2) )
# 417 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_11 =
  fun e1 e2 ->
    (
# 93 "lib/parser.mly"
                                  ( BinaryOp(Minus, e1, e2) )
# 425 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_12 =
  fun e1 e2 ->
    (
# 94 "lib/parser.mly"
                                  ( BinaryOp(Times, e1, e2) )
# 433 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_13 =
  fun e1 e2 ->
    (
# 95 "lib/parser.mly"
                                  ( BinaryOp(Div, e1, e2) )
# 441 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_14 =
  fun e ->
    (
# 96 "lib/parser.mly"
                                  ( UnaryOp(UMinus, e) )
# 449 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_15 =
  fun e1 e2 ->
    (
# 97 "lib/parser.mly"
                                  ( BinaryOp(Pow, e1, e2) )
# 457 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_16 =
  fun e ->
    (
# 98 "lib/parser.mly"
                                  ( e )
# 465 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_17 =
  fun e uo ->
    (
# 99 "lib/parser.mly"
                                  ( UnaryOp(uo, e) )
# 473 "lib/parser.ml"
     : (AST.exp))

let _menhir_action_18 =
  fun xs ->
    let el = 
# 229 "<standard.mly>"
    ( xs )
# 481 "lib/parser.ml"
     in
    (
# 85 "lib/parser.mly"
                                       ( el )
# 486 "lib/parser.ml"
     : (AST.exp list))

let _menhir_action_19 =
  fun name qargs ->
    (
# 57 "lib/parser.mly"
                                                          ( (name, [], qargs) )
# 494 "lib/parser.ml"
     : (AST.gatedecl))

let _menhir_action_20 =
  fun name params qargs ->
    (
# 58 "lib/parser.mly"
                                                          ( (name, params, qargs) )
# 502 "lib/parser.ml"
     : (AST.gatedecl))

let _menhir_action_21 =
  fun body ->
    (
# 60 "lib/parser.mly"
                                ( body )
# 510 "lib/parser.ml"
     : (AST.gop list))

let _menhir_action_22 =
  fun xs ->
    let il = 
# 229 "<standard.mly>"
    ( xs )
# 518 "lib/parser.ml"
     in
    (
# 79 "lib/parser.mly"
                                     ( il )
# 523 "lib/parser.ml"
     : (string list))

let _menhir_action_23 =
  fun () ->
    (
# 208 "<standard.mly>"
    ( [] )
# 531 "lib/parser.ml"
     : (AST.program))

let _menhir_action_24 =
  fun x xs ->
    (
# 210 "<standard.mly>"
    ( x :: xs )
# 539 "lib/parser.ml"
     : (AST.program))

let _menhir_action_25 =
  fun () ->
    (
# 208 "<standard.mly>"
    ( [] )
# 547 "lib/parser.ml"
     : (AST.gop list))

let _menhir_action_26 =
  fun x xs ->
    (
# 210 "<standard.mly>"
    ( x :: xs )
# 555 "lib/parser.ml"
     : (AST.gop list))

let _menhir_action_27 =
  fun () ->
    (
# 139 "<standard.mly>"
    ( [] )
# 563 "lib/parser.ml"
     : (string list))

let _menhir_action_28 =
  fun x ->
    (
# 141 "<standard.mly>"
    ( x )
# 571 "lib/parser.ml"
     : (string list))

let _menhir_action_29 =
  fun () ->
    (
# 139 "<standard.mly>"
    ( [] )
# 579 "lib/parser.ml"
     : (AST.argument list))

let _menhir_action_30 =
  fun x ->
    (
# 141 "<standard.mly>"
    ( x )
# 587 "lib/parser.ml"
     : (AST.argument list))

let _menhir_action_31 =
  fun () ->
    (
# 139 "<standard.mly>"
    ( [] )
# 595 "lib/parser.ml"
     : (AST.exp list))

let _menhir_action_32 =
  fun x ->
    (
# 141 "<standard.mly>"
    ( x )
# 603 "lib/parser.ml"
     : (AST.exp list))

let _menhir_action_33 =
  fun p ->
    (
# 38 "lib/parser.mly"
                                               ( p )
# 611 "lib/parser.ml"
     : (AST.program))

let _menhir_action_34 =
  fun sl ->
    (
# 40 "lib/parser.mly"
                         ( sl )
# 619 "lib/parser.ml"
     : (AST.program))

let _menhir_action_35 =
  fun u ->
    (
# 67 "lib/parser.mly"
                                                      ( Uop(u) )
# 627 "lib/parser.ml"
     : (AST.qop))

let _menhir_action_36 =
  fun carg qarg ->
    (
# 68 "lib/parser.mly"
                                                      ( Meas(qarg, carg) )
# 635 "lib/parser.ml"
     : (AST.qop))

let _menhir_action_37 =
  fun qarg ->
    (
# 69 "lib/parser.mly"
                                                      ( Reset(qarg) )
# 643 "lib/parser.ml"
     : (AST.qop))

let _menhir_action_38 =
  fun x ->
    (
# 238 "<standard.mly>"
    ( [ x ] )
# 651 "lib/parser.ml"
     : (string list))

let _menhir_action_39 =
  fun x xs ->
    (
# 240 "<standard.mly>"
    ( x :: xs )
# 659 "lib/parser.ml"
     : (string list))

let _menhir_action_40 =
  fun x ->
    (
# 238 "<standard.mly>"
    ( [ x ] )
# 667 "lib/parser.ml"
     : (AST.argument list))

let _menhir_action_41 =
  fun x xs ->
    (
# 240 "<standard.mly>"
    ( x :: xs )
# 675 "lib/parser.ml"
     : (AST.argument list))

let _menhir_action_42 =
  fun x ->
    (
# 238 "<standard.mly>"
    ( [ x ] )
# 683 "lib/parser.ml"
     : (AST.exp list))

let _menhir_action_43 =
  fun x xs ->
    (
# 240 "<standard.mly>"
    ( x :: xs )
# 691 "lib/parser.ml"
     : (AST.exp list))

let _menhir_action_44 =
  fun inc ->
    (
# 43 "lib/parser.mly"
                                                                ( Include(inc) )
# 699 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_45 =
  fun d ->
    (
# 44 "lib/parser.mly"
                                                                ( Decl(d) )
# 707 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_46 =
  fun gd gl ->
    (
# 45 "lib/parser.mly"
                                                                ( GateDecl(gd, gl) )
# 715 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_47 =
  fun name qargs ->
    (
# 46 "lib/parser.mly"
                                                                ( OpaqueDecl(name, [], qargs) )
# 723 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_48 =
  fun name params qargs ->
    (
# 47 "lib/parser.mly"
                                                                ( OpaqueDecl(name, params, qargs) )
# 731 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_49 =
  fun q ->
    (
# 48 "lib/parser.mly"
                                                                ( Qop(q) )
# 739 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_50 =
  fun creg n q ->
    (
# 49 "lib/parser.mly"
                                                                ( If(creg, n, q) )
# 747 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_51 =
  fun qargs ->
    (
# 50 "lib/parser.mly"
                                                                ( Barrier(qargs) )
# 755 "lib/parser.ml"
     : (AST.statement))

let _menhir_action_52 =
  fun () ->
    (
# 102 "lib/parser.mly"
          ( Sin )
# 763 "lib/parser.ml"
     : (AST.unaryop))

let _menhir_action_53 =
  fun () ->
    (
# 103 "lib/parser.mly"
          ( Cos )
# 771 "lib/parser.ml"
     : (AST.unaryop))

let _menhir_action_54 =
  fun () ->
    (
# 104 "lib/parser.mly"
          ( Tan )
# 779 "lib/parser.ml"
     : (AST.unaryop))

let _menhir_action_55 =
  fun () ->
    (
# 105 "lib/parser.mly"
          ( Exp )
# 787 "lib/parser.ml"
     : (AST.unaryop))

let _menhir_action_56 =
  fun () ->
    (
# 106 "lib/parser.mly"
          ( Ln )
# 795 "lib/parser.ml"
     : (AST.unaryop))

let _menhir_action_57 =
  fun () ->
    (
# 107 "lib/parser.mly"
          ( Sqrt )
# 803 "lib/parser.ml"
     : (AST.unaryop))

let _menhir_action_58 =
  fun q1 q2 ->
    (
# 72 "lib/parser.mly"
                                                            ( CX(q1, q2) )
# 811 "lib/parser.ml"
     : (AST.uop))

let _menhir_action_59 =
  fun params q ->
    (
# 73 "lib/parser.mly"
                                                            ( U(params, q) )
# 819 "lib/parser.ml"
     : (AST.uop))

let _menhir_action_60 =
  fun gname qargs ->
    (
# 74 "lib/parser.mly"
                                                            ( Gate(gname, [], qargs) )
# 827 "lib/parser.ml"
     : (AST.uop))

let _menhir_action_61 =
  fun gname params qargs ->
    (
# 75 "lib/parser.mly"
                                                            ( Gate(gname, params, qargs) )
# 835 "lib/parser.ml"
     : (AST.uop))

let _menhir_action_62 =
  fun u ->
    (
# 63 "lib/parser.mly"
                              ( GUop(u) )
# 843 "lib/parser.ml"
     : (AST.gop))

let _menhir_action_63 =
  fun qargs ->
    (
# 64 "lib/parser.mly"
                              ( GBarrier(qargs) )
# 851 "lib/parser.ml"
     : (AST.gop))

let _menhir_print_token : token -> string =
  fun _tok ->
    match _tok with
    | ARROW ->
        "ARROW"
    | BARRIER ->
        "BARRIER"
    | COMMA ->
        "COMMA"
    | COS ->
        "COS"
    | CREG ->
        "CREG"
    | CX ->
        "CX"
    | DIV ->
        "DIV"
    | EOF ->
        "EOF"
    | EQUALS ->
        "EQUALS"
    | EXP ->
        "EXP"
    | GATE ->
        "GATE"
    | ID _ ->
        "ID"
    | IF ->
        "IF"
    | INCLUDE ->
        "INCLUDE"
    | LBRACE ->
        "LBRACE"
    | LBRACKET ->
        "LBRACKET"
    | LN ->
        "LN"
    | LPAREN ->
        "LPAREN"
    | MEASURE ->
        "MEASURE"
    | MINUS ->
        "MINUS"
    | NINT _ ->
        "NINT"
    | OPAQUE ->
        "OPAQUE"
    | OPENQASM ->
        "OPENQASM"
    | PI ->
        "PI"
    | PLUS ->
        "PLUS"
    | POW ->
        "POW"
    | QREG ->
        "QREG"
    | RBRACE ->
        "RBRACE"
    | RBRACKET ->
        "RBRACKET"
    | REAL _ ->
        "REAL"
    | RESET ->
        "RESET"
    | RPAREN ->
        "RPAREN"
    | SEMICOLON ->
        "SEMICOLON"
    | SIN ->
        "SIN"
    | SQRT ->
        "SQRT"
    | STRING _ ->
        "STRING"
    | TAN ->
        "TAN"
    | TIMES ->
        "TIMES"
    | U ->
        "U"

let _menhir_fail : unit -> 'a =
  fun () ->
    Printf.eprintf "Internal failure -- please contact the parser generator's developers.\n%!";
    assert false

include struct
  
  [@@@ocaml.warning "-4-37-39"]
  
  let rec _menhir_run_138_spec_003 : type  ttv_stack. ttv_stack _menhir_cell0_REAL -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _v ->
      let _v =
        let sl = _v in
        _menhir_action_34 sl
      in
      let MenhirCell0_REAL (_menhir_stack, _) = _menhir_stack in
      let p = _v in
      let _v = _menhir_action_33 p in
      MenhirBox_mainprogram _v
  
  let rec _menhir_run_123 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_statement -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _v ->
      let MenhirCell1_statement (_menhir_stack, _menhir_s, x) = _menhir_stack in
      let xs = _v in
      let _v = _menhir_action_24 x xs in
      _menhir_goto_list_statement_ _menhir_stack _v _menhir_s
  
  and _menhir_goto_list_statement_ : type  ttv_stack. ttv_stack -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _v _menhir_s ->
      match _menhir_s with
      | MenhirState003 ->
          _menhir_run_138_spec_003 _menhir_stack _v
      | MenhirState121 ->
          _menhir_run_123 _menhir_stack _v
      | _ ->
          _menhir_fail ()
  
  let rec _menhir_run_004 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_U (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | LPAREN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | TAN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_54 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | SQRT ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_57 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | SIN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_52 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | REAL _v ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let r = _v in
              let _v = _menhir_action_06 r in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | PI ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_08 () in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | NINT _v ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let n = _v in
              let _v = _menhir_action_07 n in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | MINUS ->
              _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState005
          | LPAREN ->
              _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState005
          | LN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_56 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | ID _v ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let id = _v in
              let _v = _menhir_action_09 id in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | EXP ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_55 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | COS ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_53 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState005 _tok
          | RPAREN ->
              let _v = _menhir_action_31 () in
              _menhir_run_036_spec_005 _menhir_stack _menhir_lexbuf _menhir_lexer _v
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_018 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      let _menhir_stack = MenhirCell1_unaryop (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | LPAREN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | TAN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_54 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | SQRT ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_57 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | SIN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_52 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | REAL _v_3 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let r = _v_3 in
              let _v = _menhir_action_06 r in
              _menhir_run_020 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | PI ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_08 () in
              _menhir_run_020 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | NINT _v_6 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let n = _v_6 in
              let _v = _menhir_action_07 n in
              _menhir_run_020 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | MINUS ->
              _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState019
          | LPAREN ->
              _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState019
          | LN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_56 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | ID _v_9 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let id = _v_9 in
              let _v = _menhir_action_09 id in
              _menhir_run_020 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | EXP ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_55 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | COS ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_53 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState019 _tok
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_020 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_unaryop as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | TIMES ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_021 _menhir_stack _menhir_lexbuf _menhir_lexer
      | RPAREN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_unaryop (_menhir_stack, _menhir_s, uo) = _menhir_stack in
          let e = _v in
          let _v = _menhir_action_17 e uo in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | PLUS ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_026 _menhir_stack _menhir_lexbuf _menhir_lexer
      | MINUS ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_030 _menhir_stack _menhir_lexbuf _menhir_lexer
      | DIV ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_028 _menhir_stack _menhir_lexbuf _menhir_lexer
      | _ ->
          _eRR ()
  
  and _menhir_run_021 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | TAN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_54 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | SQRT ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_57 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | SIN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_52 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | REAL _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let r = _v in
          let _v = _menhir_action_06 r in
          _menhir_run_022 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | PI ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_08 () in
          _menhir_run_022 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | NINT _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let n = _v in
          let _v = _menhir_action_07 n in
          _menhir_run_022 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | MINUS ->
          _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState021
      | LPAREN ->
          _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState021
      | LN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_56 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let id = _v in
          let _v = _menhir_action_09 id in
          _menhir_run_022 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | EXP ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_55 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | COS ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_53 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState021 _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_022 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | COMMA | DIV | MINUS | PLUS | RPAREN | TIMES ->
          let MenhirCell1_exp (_menhir_stack, _menhir_s, e1) = _menhir_stack in
          let e2 = _v in
          let _v = _menhir_action_12 e1 e2 in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_023 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | TAN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_54 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | SQRT ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_57 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | SIN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_52 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | REAL _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let r = _v in
          let _v = _menhir_action_06 r in
          _menhir_run_024 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | PI ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_08 () in
          _menhir_run_024 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | NINT _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let n = _v in
          let _v = _menhir_action_07 n in
          _menhir_run_024 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | MINUS ->
          _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState023
      | LPAREN ->
          _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState023
      | LN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_56 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let id = _v in
          let _v = _menhir_action_09 id in
          _menhir_run_024 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | EXP ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_55 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | COS ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_53 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState023 _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_024 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | COMMA | DIV | MINUS | PLUS | RPAREN | TIMES ->
          let MenhirCell1_exp (_menhir_stack, _menhir_s, e1) = _menhir_stack in
          let e2 = _v in
          let _v = _menhir_action_15 e1 e2 in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_goto_exp : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match _menhir_s with
      | MenhirState086 ->
          _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState046 ->
          _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState005 ->
          _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState012 ->
          _menhir_run_034 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState013 ->
          _menhir_run_032 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState030 ->
          _menhir_run_031 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState028 ->
          _menhir_run_029 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState026 ->
          _menhir_run_027 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState023 ->
          _menhir_run_024 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState021 ->
          _menhir_run_022 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState019 ->
          _menhir_run_020 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_045 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | TIMES ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_021 _menhir_stack _menhir_lexbuf _menhir_lexer
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | PLUS ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_026 _menhir_stack _menhir_lexbuf _menhir_lexer
      | MINUS ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_030 _menhir_stack _menhir_lexbuf _menhir_lexer
      | DIV ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_028 _menhir_stack _menhir_lexbuf _menhir_lexer
      | COMMA ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | TAN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_54 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | SQRT ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_57 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | SIN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_52 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | REAL _v_3 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let r = _v_3 in
              let _v = _menhir_action_06 r in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | PI ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_08 () in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | NINT _v_6 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let n = _v_6 in
              let _v = _menhir_action_07 n in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | MINUS ->
              _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState046
          | LPAREN ->
              _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState046
          | LN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_56 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | ID _v_9 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let id = _v_9 in
              let _v = _menhir_action_09 id in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | EXP ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_55 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | COS ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_53 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState046 _tok
          | _ ->
              _eRR ())
      | RPAREN ->
          let x = _v in
          let _v = _menhir_action_42 x in
          _menhir_goto_separated_nonempty_list_COMMA_exp_ _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s
      | _ ->
          _eRR ()
  
  and _menhir_run_026 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | TAN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_54 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | SQRT ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_57 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | SIN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_52 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | REAL _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let r = _v in
          let _v = _menhir_action_06 r in
          _menhir_run_027 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | PI ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_08 () in
          _menhir_run_027 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | NINT _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let n = _v in
          let _v = _menhir_action_07 n in
          _menhir_run_027 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | MINUS ->
          _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState026
      | LPAREN ->
          _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState026
      | LN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_56 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let id = _v in
          let _v = _menhir_action_09 id in
          _menhir_run_027 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | EXP ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_55 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | COS ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_53 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState026 _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_027 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | TIMES ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_021 _menhir_stack _menhir_lexbuf _menhir_lexer
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | DIV ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_028 _menhir_stack _menhir_lexbuf _menhir_lexer
      | COMMA | MINUS | PLUS | RPAREN ->
          let MenhirCell1_exp (_menhir_stack, _menhir_s, e1) = _menhir_stack in
          let e2 = _v in
          let _v = _menhir_action_10 e1 e2 in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_028 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | TAN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_54 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | SQRT ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_57 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | SIN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_52 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | REAL _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let r = _v in
          let _v = _menhir_action_06 r in
          _menhir_run_029 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | PI ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_08 () in
          _menhir_run_029 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | NINT _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let n = _v in
          let _v = _menhir_action_07 n in
          _menhir_run_029 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | MINUS ->
          _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState028
      | LPAREN ->
          _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState028
      | LN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_56 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let id = _v in
          let _v = _menhir_action_09 id in
          _menhir_run_029 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | EXP ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_55 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | COS ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_53 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState028 _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_029 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | COMMA | DIV | MINUS | PLUS | RPAREN | TIMES ->
          let MenhirCell1_exp (_menhir_stack, _menhir_s, e1) = _menhir_stack in
          let e2 = _v in
          let _v = _menhir_action_13 e1 e2 in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_012 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_MINUS (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | TAN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_54 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | SQRT ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_57 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | SIN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_52 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | REAL _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let r = _v in
          let _v = _menhir_action_06 r in
          _menhir_run_034 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | PI ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_08 () in
          _menhir_run_034 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | NINT _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let n = _v in
          let _v = _menhir_action_07 n in
          _menhir_run_034 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | MINUS ->
          _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState012
      | LPAREN ->
          _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState012
      | LN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_56 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let id = _v in
          let _v = _menhir_action_09 id in
          _menhir_run_034 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | EXP ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_55 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | COS ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_53 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState012 _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_034 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_MINUS as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | COMMA | DIV | MINUS | PLUS | RPAREN | TIMES ->
          let MenhirCell1_MINUS (_menhir_stack, _menhir_s) = _menhir_stack in
          let e = _v in
          let _v = _menhir_action_14 e in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_013 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_LPAREN (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | TAN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_54 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | SQRT ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_57 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | SIN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_52 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | REAL _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let r = _v in
          let _v = _menhir_action_06 r in
          _menhir_run_032 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | PI ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_08 () in
          _menhir_run_032 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | NINT _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let n = _v in
          let _v = _menhir_action_07 n in
          _menhir_run_032 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | MINUS ->
          _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState013
      | LPAREN ->
          _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState013
      | LN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_56 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let id = _v in
          let _v = _menhir_action_09 id in
          _menhir_run_032 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | EXP ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_55 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | COS ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_53 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState013 _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_032 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_LPAREN as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | TIMES ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_021 _menhir_stack _menhir_lexbuf _menhir_lexer
      | RPAREN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_LPAREN (_menhir_stack, _menhir_s) = _menhir_stack in
          let e = _v in
          let _v = _menhir_action_16 e in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | PLUS ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_026 _menhir_stack _menhir_lexbuf _menhir_lexer
      | MINUS ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_030 _menhir_stack _menhir_lexbuf _menhir_lexer
      | DIV ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_028 _menhir_stack _menhir_lexbuf _menhir_lexer
      | _ ->
          _eRR ()
  
  and _menhir_run_030 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | TAN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_54 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | SQRT ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_57 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | SIN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_52 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | REAL _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let r = _v in
          let _v = _menhir_action_06 r in
          _menhir_run_031 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | PI ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_08 () in
          _menhir_run_031 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | NINT _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let n = _v in
          let _v = _menhir_action_07 n in
          _menhir_run_031 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | MINUS ->
          _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState030
      | LPAREN ->
          _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState030
      | LN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_56 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let id = _v in
          let _v = _menhir_action_09 id in
          _menhir_run_031 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | EXP ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_55 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | COS ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let _v = _menhir_action_53 () in
          _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState030 _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_031 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | TIMES ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_021 _menhir_stack _menhir_lexbuf _menhir_lexer
      | POW ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_023 _menhir_stack _menhir_lexbuf _menhir_lexer
      | DIV ->
          let _menhir_stack = MenhirCell1_exp (_menhir_stack, _menhir_s, _v) in
          _menhir_run_028 _menhir_stack _menhir_lexbuf _menhir_lexer
      | COMMA | MINUS | PLUS | RPAREN ->
          let MenhirCell1_exp (_menhir_stack, _menhir_s, e1) = _menhir_stack in
          let e2 = _v in
          let _v = _menhir_action_11 e1 e2 in
          _menhir_goto_exp _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_goto_separated_nonempty_list_COMMA_exp_ : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s ->
      match _menhir_s with
      | MenhirState046 ->
          _menhir_run_047 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | MenhirState086 ->
          _menhir_run_035_spec_086 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | MenhirState005 ->
          _menhir_run_035_spec_005 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_047 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_exp -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let MenhirCell1_exp (_menhir_stack, _menhir_s, x) = _menhir_stack in
      let xs = _v in
      let _v = _menhir_action_43 x xs in
      _menhir_goto_separated_nonempty_list_COMMA_exp_ _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s
  
  and _menhir_run_035_spec_086 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let x = _v in
      let _v = _menhir_action_32 x in
      _menhir_run_036_spec_086 _menhir_stack _menhir_lexbuf _menhir_lexer _v
  
  and _menhir_run_036_spec_086 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let xs = _v in
      let _v = _menhir_action_18 xs in
      let _menhir_s = MenhirState086 in
      let _menhir_stack = MenhirCell1_explist (_menhir_stack, _menhir_s, _v) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState088
      | SEMICOLON ->
          let _v = _menhir_action_29 () in
          _menhir_run_090_spec_088 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | _ ->
          _eRR ()
  
  and _menhir_run_039 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | LBRACKET ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | NINT _v_0 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              (match (_tok : MenhirBasics.token) with
              | RBRACKET ->
                  let _tok = _menhir_lexer _menhir_lexbuf in
                  let (name, idx) = (_v, _v_0) in
                  let _v = _menhir_action_03 idx name in
                  _menhir_goto_argument _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
              | _ ->
                  _eRR ())
          | _ ->
              _eRR ())
      | ARROW | COMMA | SEMICOLON ->
          let name = _v in
          let _v = _menhir_action_02 name in
          _menhir_goto_argument _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_goto_argument : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match _menhir_s with
      | MenhirState100 ->
          _menhir_run_101 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState098 ->
          _menhir_run_099 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState118 ->
          _menhir_run_091 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState085 ->
          _menhir_run_091 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState092 ->
          _menhir_run_091 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState088 ->
          _menhir_run_091 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState073 ->
          _menhir_run_074 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState071 ->
          _menhir_run_072 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | MenhirState048 ->
          _menhir_run_049 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState038 ->
          _menhir_run_043 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_101 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_CX, _menhir_box_mainprogram) _menhir_cell1_argument -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      match (_tok : MenhirBasics.token) with
      | SEMICOLON ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_argument (_menhir_stack, _, q1) = _menhir_stack in
          let MenhirCell1_CX (_menhir_stack, _menhir_s) = _menhir_stack in
          let q2 = _v in
          let _v = _menhir_action_58 q1 q2 in
          _menhir_goto_uop _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_goto_uop : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match _menhir_s with
      | MenhirState125 ->
          _menhir_run_130_spec_125 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState129 ->
          _menhir_run_130_spec_129 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState121 ->
          _menhir_run_103_spec_121 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState003 ->
          _menhir_run_103_spec_003 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState084 ->
          _menhir_run_103_spec_084 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_130_spec_125 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_gatedecl -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let u = _v in
      let _v = _menhir_action_62 u in
      _menhir_run_129 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState125 _tok
  
  and _menhir_run_129 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      let _menhir_stack = MenhirCell1_uop_or_barrier (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | U ->
          _menhir_run_004 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState129
      | ID _v_0 ->
          _menhir_run_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v_0 MenhirState129
      | CX ->
          _menhir_run_098 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState129
      | BARRIER ->
          _menhir_run_126 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState129
      | RBRACE ->
          let _v = _menhir_action_25 () in
          _menhir_run_131 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | _ ->
          _eRR ()
  
  and _menhir_run_085 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s ->
      let _menhir_stack = MenhirCell1_ID (_menhir_stack, _menhir_s, _v) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | LPAREN ->
          let _menhir_stack = MenhirCell1_LPAREN (_menhir_stack, MenhirState085) in
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | TAN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_54 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | SQRT ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_57 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | SIN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_52 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | REAL _v_3 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let r = _v_3 in
              let _v = _menhir_action_06 r in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | PI ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_08 () in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | NINT _v_6 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let n = _v_6 in
              let _v = _menhir_action_07 n in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | MINUS ->
              _menhir_run_012 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState086
          | LPAREN ->
              _menhir_run_013 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState086
          | LN ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_56 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | ID _v_9 ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let id = _v_9 in
              let _v = _menhir_action_09 id in
              _menhir_run_045 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | EXP ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_55 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | COS ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let _v = _menhir_action_53 () in
              _menhir_run_018 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState086 _tok
          | RPAREN ->
              let _v = _menhir_action_31 () in
              _menhir_run_036_spec_086 _menhir_stack _menhir_lexbuf _menhir_lexer _v
          | _ ->
              _eRR ())
      | ID _v_14 ->
          _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v_14 MenhirState085
      | SEMICOLON ->
          let _v = _menhir_action_29 () in
          _menhir_run_090_spec_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | _ ->
          _eRR ()
  
  and _menhir_run_090_spec_085 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_ID -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let _v =
        let xs = _v in
        _menhir_action_01 xs
      in
      let _tok = _menhir_lexer _menhir_lexbuf in
      let MenhirCell1_ID (_menhir_stack, _menhir_s, gname) = _menhir_stack in
      let qargs = _v in
      let _v = _menhir_action_60 gname qargs in
      _menhir_goto_uop _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_098 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_CX (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState098
      | _ ->
          _eRR ()
  
  and _menhir_run_126 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_BARRIER (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState126
      | SEMICOLON ->
          let _v = _menhir_action_27 () in
          _menhir_run_064_spec_126 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_060 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | COMMA ->
          let _menhir_stack = MenhirCell1_ID (_menhir_stack, _menhir_s, _v) in
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | ID _v ->
              _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState061
          | _ ->
              _eRR ())
      | LBRACE | RPAREN | SEMICOLON ->
          let x = _v in
          let _v = _menhir_action_38 x in
          _menhir_goto_separated_nonempty_list_COMMA_ID_ _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_goto_separated_nonempty_list_COMMA_ID_ : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match _menhir_s with
      | MenhirState126 ->
          _menhir_run_063_spec_126 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState106 ->
          _menhir_run_063_spec_106 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState109 ->
          _menhir_run_063_spec_109 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState107 ->
          _menhir_run_063_spec_107 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState058 ->
          _menhir_run_063_spec_058 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState066 ->
          _menhir_run_063_spec_066 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState059 ->
          _menhir_run_063_spec_059 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState061 ->
          _menhir_run_062 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_063_spec_126 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_BARRIER -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let x = _v in
      let _v = _menhir_action_28 x in
      _menhir_run_064_spec_126 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_064_spec_126 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_BARRIER -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let _v =
        let xs = _v in
        _menhir_action_22 xs
      in
      match (_tok : MenhirBasics.token) with
      | SEMICOLON ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_BARRIER (_menhir_stack, _menhir_s) = _menhir_stack in
          let qargs = _v in
          let _v = _menhir_action_63 qargs in
          _menhir_run_129 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_063_spec_106 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let x = _v in
      let _v = _menhir_action_28 x in
      _menhir_run_064_spec_106 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_064_spec_106 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let _v =
        let xs = _v in
        _menhir_action_22 xs
      in
      let MenhirCell0_ID (_menhir_stack, name) = _menhir_stack in
      let MenhirCell1_GATE (_menhir_stack, _menhir_s) = _menhir_stack in
      let qargs = _v in
      let _v = _menhir_action_19 name qargs in
      _menhir_goto_gatedecl _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_goto_gatedecl : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      let _menhir_stack = MenhirCell1_gatedecl (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | LBRACE ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | U ->
              _menhir_run_004 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState125
          | ID _v ->
              _menhir_run_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState125
          | CX ->
              _menhir_run_098 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState125
          | BARRIER ->
              _menhir_run_126 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState125
          | RBRACE ->
              let _v = _menhir_action_25 () in
              _menhir_run_132_spec_125 _menhir_stack _menhir_lexbuf _menhir_lexer _v
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_132_spec_125 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_gatedecl -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let _v =
        let body = _v in
        _menhir_action_21 body
      in
      let _tok = _menhir_lexer _menhir_lexbuf in
      let MenhirCell1_gatedecl (_menhir_stack, _menhir_s, gd) = _menhir_stack in
      let gl = _v in
      let _v = _menhir_action_46 gd gl in
      _menhir_goto_statement _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_goto_statement : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      _menhir_run_121 _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_121 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      let _menhir_stack = MenhirCell1_statement (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | U ->
          _menhir_run_004 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | RESET ->
          _menhir_run_048 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | QREG ->
          _menhir_run_051 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | OPAQUE ->
          _menhir_run_057 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | MEASURE ->
          _menhir_run_071 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | INCLUDE ->
          _menhir_run_076 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | IF ->
          _menhir_run_079 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | ID _v_0 ->
          _menhir_run_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v_0 MenhirState121
      | GATE ->
          _menhir_run_105 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | CX ->
          _menhir_run_098 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | CREG ->
          _menhir_run_112 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | BARRIER ->
          _menhir_run_118 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState121
      | EOF ->
          let _v = _menhir_action_23 () in
          _menhir_run_123 _menhir_stack _v
      | _ ->
          _eRR ()
  
  and _menhir_run_048 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_RESET (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState048
      | _ ->
          _eRR ()
  
  and _menhir_run_051 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | LBRACKET ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              (match (_tok : MenhirBasics.token) with
              | NINT _v_0 ->
                  let _tok = _menhir_lexer _menhir_lexbuf in
                  (match (_tok : MenhirBasics.token) with
                  | RBRACKET ->
                      let _tok = _menhir_lexer _menhir_lexbuf in
                      (match (_tok : MenhirBasics.token) with
                      | SEMICOLON ->
                          let _tok = _menhir_lexer _menhir_lexbuf in
                          let (name, size) = (_v, _v_0) in
                          let _v = _menhir_action_04 name size in
                          _menhir_goto_decl _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
                      | _ ->
                          _eRR ())
                  | _ ->
                      _eRR ())
              | _ ->
                  _eRR ())
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_goto_decl : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      let d = _v in
      let _v = _menhir_action_45 d in
      _menhir_goto_statement _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_057 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_OPAQUE (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          let _menhir_stack = MenhirCell0_ID (_menhir_stack, _v) in
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | LPAREN ->
              let _menhir_s = MenhirState058 in
              let _menhir_stack = MenhirCell1_LPAREN (_menhir_stack, _menhir_s) in
              let _tok = _menhir_lexer _menhir_lexbuf in
              (match (_tok : MenhirBasics.token) with
              | ID _v ->
                  _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState059
              | RPAREN ->
                  let _v = _menhir_action_27 () in
                  _menhir_run_064_spec_059 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
              | _ ->
                  _eRR ())
          | ID _v ->
              _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState058
          | SEMICOLON ->
              let _v = _menhir_action_27 () in
              _menhir_run_064_spec_058 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_064_spec_059 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let xs = _v in
      let _v = _menhir_action_22 xs in
      let _menhir_s = MenhirState059 in
      let _menhir_stack = MenhirCell1_idlist (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | RPAREN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | ID _v ->
              _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState066
          | SEMICOLON ->
              let _v = _menhir_action_27 () in
              _menhir_run_064_spec_066 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_064_spec_066 : type  ttv_stack. (((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_idlist -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let _v =
        let xs = _v in
        _menhir_action_22 xs
      in
      match (_tok : MenhirBasics.token) with
      | SEMICOLON ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_idlist (_menhir_stack, _, params) = _menhir_stack in
          let MenhirCell1_LPAREN (_menhir_stack, _) = _menhir_stack in
          let MenhirCell0_ID (_menhir_stack, name) = _menhir_stack in
          let MenhirCell1_OPAQUE (_menhir_stack, _menhir_s) = _menhir_stack in
          let qargs = _v in
          let _v = _menhir_action_48 name params qargs in
          _menhir_goto_statement _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_064_spec_058 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let _v =
        let xs = _v in
        _menhir_action_22 xs
      in
      match (_tok : MenhirBasics.token) with
      | SEMICOLON ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell0_ID (_menhir_stack, name) = _menhir_stack in
          let MenhirCell1_OPAQUE (_menhir_stack, _menhir_s) = _menhir_stack in
          let qargs = _v in
          let _v = _menhir_action_47 name qargs in
          _menhir_goto_statement _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_071 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_MEASURE (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState071
      | _ ->
          _eRR ()
  
  and _menhir_run_076 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | STRING _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | SEMICOLON ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              let inc = _v in
              let _v = _menhir_action_44 inc in
              _menhir_goto_statement _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_079 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_IF (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | LPAREN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | ID _v ->
              let _menhir_stack = MenhirCell0_ID (_menhir_stack, _v) in
              let _tok = _menhir_lexer _menhir_lexbuf in
              (match (_tok : MenhirBasics.token) with
              | EQUALS ->
                  let _tok = _menhir_lexer _menhir_lexbuf in
                  (match (_tok : MenhirBasics.token) with
                  | NINT _v ->
                      let _menhir_stack = MenhirCell0_NINT (_menhir_stack, _v) in
                      let _tok = _menhir_lexer _menhir_lexbuf in
                      (match (_tok : MenhirBasics.token) with
                      | RPAREN ->
                          let _tok = _menhir_lexer _menhir_lexbuf in
                          (match (_tok : MenhirBasics.token) with
                          | U ->
                              _menhir_run_004 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState084
                          | RESET ->
                              _menhir_run_048 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState084
                          | MEASURE ->
                              _menhir_run_071 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState084
                          | ID _v ->
                              _menhir_run_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState084
                          | CX ->
                              _menhir_run_098 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState084
                          | _ ->
                              _eRR ())
                      | _ ->
                          _eRR ())
                  | _ ->
                      _eRR ())
              | _ ->
                  _eRR ())
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_105 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_GATE (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          let _menhir_stack = MenhirCell0_ID (_menhir_stack, _v) in
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | LPAREN ->
              let _menhir_s = MenhirState106 in
              let _menhir_stack = MenhirCell1_LPAREN (_menhir_stack, _menhir_s) in
              let _tok = _menhir_lexer _menhir_lexbuf in
              (match (_tok : MenhirBasics.token) with
              | ID _v ->
                  _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState107
              | RPAREN ->
                  let _v = _menhir_action_27 () in
                  _menhir_run_064_spec_107 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
              | _ ->
                  _eRR ())
          | ID _v ->
              _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState106
          | LBRACE ->
              let _v = _menhir_action_27 () in
              _menhir_run_064_spec_106 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_064_spec_107 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let xs = _v in
      let _v = _menhir_action_22 xs in
      let _menhir_s = MenhirState107 in
      let _menhir_stack = MenhirCell1_idlist (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | RPAREN ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | ID _v ->
              _menhir_run_060 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState109
          | LBRACE ->
              let _v = _menhir_action_27 () in
              _menhir_run_064_spec_109 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_064_spec_109 : type  ttv_stack. (((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_idlist -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let _v =
        let xs = _v in
        _menhir_action_22 xs
      in
      let MenhirCell1_idlist (_menhir_stack, _, params) = _menhir_stack in
      let MenhirCell1_LPAREN (_menhir_stack, _) = _menhir_stack in
      let MenhirCell0_ID (_menhir_stack, name) = _menhir_stack in
      let MenhirCell1_GATE (_menhir_stack, _menhir_s) = _menhir_stack in
      let qargs = _v in
      let _v = _menhir_action_20 name params qargs in
      _menhir_goto_gatedecl _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_112 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | LBRACKET ->
              let _tok = _menhir_lexer _menhir_lexbuf in
              (match (_tok : MenhirBasics.token) with
              | NINT _v_0 ->
                  let _tok = _menhir_lexer _menhir_lexbuf in
                  (match (_tok : MenhirBasics.token) with
                  | RBRACKET ->
                      let _tok = _menhir_lexer _menhir_lexbuf in
                      (match (_tok : MenhirBasics.token) with
                      | SEMICOLON ->
                          let _tok = _menhir_lexer _menhir_lexbuf in
                          let (name, size) = (_v, _v_0) in
                          let _v = _menhir_action_05 name size in
                          _menhir_goto_decl _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
                      | _ ->
                          _eRR ())
                  | _ ->
                      _eRR ())
              | _ ->
                  _eRR ())
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_118 : type  ttv_stack. ttv_stack -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _menhir_s ->
      let _menhir_stack = MenhirCell1_BARRIER (_menhir_stack, _menhir_s) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState118
      | SEMICOLON ->
          let _v = _menhir_action_29 () in
          _menhir_run_090_spec_118 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | _ ->
          _eRR ()
  
  and _menhir_run_090_spec_118 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_BARRIER -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let _v =
        let xs = _v in
        _menhir_action_01 xs
      in
      let _tok = _menhir_lexer _menhir_lexbuf in
      let MenhirCell1_BARRIER (_menhir_stack, _menhir_s) = _menhir_stack in
      let qargs = _v in
      let _v = _menhir_action_51 qargs in
      _menhir_goto_statement _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_063_spec_109 : type  ttv_stack. (((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_idlist -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let x = _v in
      let _v = _menhir_action_28 x in
      _menhir_run_064_spec_109 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_063_spec_107 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_GATE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let x = _v in
      let _v = _menhir_action_28 x in
      _menhir_run_064_spec_107 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_063_spec_058 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let x = _v in
      let _v = _menhir_action_28 x in
      _menhir_run_064_spec_058 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_063_spec_066 : type  ttv_stack. (((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_idlist -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let x = _v in
      let _v = _menhir_action_28 x in
      _menhir_run_064_spec_066 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_063_spec_059 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_OPAQUE _menhir_cell0_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let x = _v in
      let _v = _menhir_action_28 x in
      _menhir_run_064_spec_059 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_062 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_ID -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let MenhirCell1_ID (_menhir_stack, _menhir_s, x) = _menhir_stack in
      let xs = _v in
      let _v = _menhir_action_39 x xs in
      _menhir_goto_separated_nonempty_list_COMMA_ID_ _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_131 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_uop_or_barrier -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let MenhirCell1_uop_or_barrier (_menhir_stack, _menhir_s, x) = _menhir_stack in
      let xs = _v in
      let _v = _menhir_action_26 x xs in
      _menhir_goto_list_uop_or_barrier_ _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s
  
  and _menhir_goto_list_uop_or_barrier_ : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s ->
      match _menhir_s with
      | MenhirState125 ->
          _menhir_run_132_spec_125 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | MenhirState129 ->
          _menhir_run_131 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_130_spec_129 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_uop_or_barrier -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let u = _v in
      let _v = _menhir_action_62 u in
      _menhir_run_129 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState129 _tok
  
  and _menhir_run_103_spec_121 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_statement -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let u = _v in
      let _v = _menhir_action_35 u in
      _menhir_run_122_spec_121 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_122_spec_121 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_statement -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let q = _v in
      let _v = _menhir_action_49 q in
      _menhir_run_121 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState121 _tok
  
  and _menhir_run_103_spec_003 : type  ttv_stack. ttv_stack _menhir_cell0_REAL -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let u = _v in
      let _v = _menhir_action_35 u in
      _menhir_run_122_spec_003 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_122_spec_003 : type  ttv_stack. ttv_stack _menhir_cell0_REAL -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let q = _v in
      let _v = _menhir_action_49 q in
      _menhir_run_121 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState003 _tok
  
  and _menhir_run_103_spec_084 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_IF _menhir_cell0_ID _menhir_cell0_NINT -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let u = _v in
      let _v = _menhir_action_35 u in
      _menhir_run_104 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
  
  and _menhir_run_104 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_IF _menhir_cell0_ID _menhir_cell0_NINT -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      let MenhirCell0_NINT (_menhir_stack, n) = _menhir_stack in
      let MenhirCell0_ID (_menhir_stack, creg) = _menhir_stack in
      let MenhirCell1_IF (_menhir_stack, _menhir_s) = _menhir_stack in
      let q = _v in
      let _v = _menhir_action_50 creg n q in
      _menhir_goto_statement _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_099 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_CX as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      let _menhir_stack = MenhirCell1_argument (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | COMMA ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | ID _v ->
              _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState100
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_091 : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match (_tok : MenhirBasics.token) with
      | COMMA ->
          let _menhir_stack = MenhirCell1_argument (_menhir_stack, _menhir_s, _v) in
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | ID _v ->
              _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState092
          | _ ->
              _eRR ())
      | SEMICOLON ->
          let x = _v in
          let _v = _menhir_action_40 x in
          _menhir_goto_separated_nonempty_list_COMMA_argument_ _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s
      | _ ->
          _eRR ()
  
  and _menhir_goto_separated_nonempty_list_COMMA_argument_ : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s ->
      match _menhir_s with
      | MenhirState092 ->
          _menhir_run_093 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | MenhirState118 ->
          _menhir_run_089_spec_118 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | MenhirState085 ->
          _menhir_run_089_spec_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | MenhirState088 ->
          _menhir_run_089_spec_088 _menhir_stack _menhir_lexbuf _menhir_lexer _v
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_093 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_argument -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let MenhirCell1_argument (_menhir_stack, _menhir_s, x) = _menhir_stack in
      let xs = _v in
      let _v = _menhir_action_41 x xs in
      _menhir_goto_separated_nonempty_list_COMMA_argument_ _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s
  
  and _menhir_run_089_spec_118 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_BARRIER -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let x = _v in
      let _v = _menhir_action_30 x in
      _menhir_run_090_spec_118 _menhir_stack _menhir_lexbuf _menhir_lexer _v
  
  and _menhir_run_089_spec_085 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_ID -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let x = _v in
      let _v = _menhir_action_30 x in
      _menhir_run_090_spec_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v
  
  and _menhir_run_089_spec_088 : type  ttv_stack. (((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_explist -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let x = _v in
      let _v = _menhir_action_30 x in
      _menhir_run_090_spec_088 _menhir_stack _menhir_lexbuf _menhir_lexer _v
  
  and _menhir_run_090_spec_088 : type  ttv_stack. (((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_ID, _menhir_box_mainprogram) _menhir_cell1_LPAREN, _menhir_box_mainprogram) _menhir_cell1_explist -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let _v =
        let xs = _v in
        _menhir_action_01 xs
      in
      let _tok = _menhir_lexer _menhir_lexbuf in
      let MenhirCell1_explist (_menhir_stack, _, params) = _menhir_stack in
      let MenhirCell1_LPAREN (_menhir_stack, _) = _menhir_stack in
      let MenhirCell1_ID (_menhir_stack, _menhir_s, gname) = _menhir_stack in
      let qargs = _v in
      let _v = _menhir_action_61 gname params qargs in
      _menhir_goto_uop _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
  
  and _menhir_run_074 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_MEASURE, _menhir_box_mainprogram) _menhir_cell1_argument -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      match (_tok : MenhirBasics.token) with
      | SEMICOLON ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_argument (_menhir_stack, _, qarg) = _menhir_stack in
          let MenhirCell1_MEASURE (_menhir_stack, _menhir_s) = _menhir_stack in
          let carg = _v in
          let _v = _menhir_action_36 carg qarg in
          _menhir_goto_qop _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_goto_qop : type  ttv_stack. ttv_stack -> _ -> _ -> _ -> (ttv_stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      match _menhir_s with
      | MenhirState003 ->
          _menhir_run_122_spec_003 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState121 ->
          _menhir_run_122_spec_121 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | MenhirState084 ->
          _menhir_run_104 _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok
      | _ ->
          _menhir_fail ()
  
  and _menhir_run_072 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_MEASURE as 'stack) -> _ -> _ -> _ -> ('stack, _menhir_box_mainprogram) _menhir_state -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok ->
      let _menhir_stack = MenhirCell1_argument (_menhir_stack, _menhir_s, _v) in
      match (_tok : MenhirBasics.token) with
      | ARROW ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | ID _v ->
              _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState073
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
  and _menhir_run_049 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_RESET -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      match (_tok : MenhirBasics.token) with
      | SEMICOLON ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_RESET (_menhir_stack, _menhir_s) = _menhir_stack in
          let qarg = _v in
          let _v = _menhir_action_37 qarg in
          _menhir_goto_qop _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_043 : type  ttv_stack. ((ttv_stack, _menhir_box_mainprogram) _menhir_cell1_U, _menhir_box_mainprogram) _menhir_cell1_explist -> _ -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v _tok ->
      match (_tok : MenhirBasics.token) with
      | SEMICOLON ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          let MenhirCell1_explist (_menhir_stack, _, params) = _menhir_stack in
          let MenhirCell1_U (_menhir_stack, _menhir_s) = _menhir_stack in
          let q = _v in
          let _v = _menhir_action_59 params q in
          _menhir_goto_uop _menhir_stack _menhir_lexbuf _menhir_lexer _v _menhir_s _tok
      | _ ->
          _eRR ()
  
  and _menhir_run_035_spec_005 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_U -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let x = _v in
      let _v = _menhir_action_32 x in
      _menhir_run_036_spec_005 _menhir_stack _menhir_lexbuf _menhir_lexer _v
  
  and _menhir_run_036_spec_005 : type  ttv_stack. (ttv_stack, _menhir_box_mainprogram) _menhir_cell1_U -> _ -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer _v ->
      let xs = _v in
      let _v = _menhir_action_18 xs in
      let _menhir_s = MenhirState005 in
      let _menhir_stack = MenhirCell1_explist (_menhir_stack, _menhir_s, _v) in
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | ID _v ->
          _menhir_run_039 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState038
      | _ ->
          _eRR ()
  
  let rec _menhir_run_000 : type  ttv_stack. ttv_stack -> _ -> _ -> _menhir_box_mainprogram =
    fun _menhir_stack _menhir_lexbuf _menhir_lexer ->
      let _tok = _menhir_lexer _menhir_lexbuf in
      match (_tok : MenhirBasics.token) with
      | OPENQASM ->
          let _tok = _menhir_lexer _menhir_lexbuf in
          (match (_tok : MenhirBasics.token) with
          | REAL _v ->
              let _menhir_stack = MenhirCell0_REAL (_menhir_stack, _v) in
              let _tok = _menhir_lexer _menhir_lexbuf in
              (match (_tok : MenhirBasics.token) with
              | SEMICOLON ->
                  let _tok = _menhir_lexer _menhir_lexbuf in
                  (match (_tok : MenhirBasics.token) with
                  | U ->
                      _menhir_run_004 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | RESET ->
                      _menhir_run_048 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | QREG ->
                      _menhir_run_051 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | OPAQUE ->
                      _menhir_run_057 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | MEASURE ->
                      _menhir_run_071 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | INCLUDE ->
                      _menhir_run_076 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | IF ->
                      _menhir_run_079 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | ID _v ->
                      _menhir_run_085 _menhir_stack _menhir_lexbuf _menhir_lexer _v MenhirState003
                  | GATE ->
                      _menhir_run_105 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | CX ->
                      _menhir_run_098 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | CREG ->
                      _menhir_run_112 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | BARRIER ->
                      _menhir_run_118 _menhir_stack _menhir_lexbuf _menhir_lexer MenhirState003
                  | EOF ->
                      let _v = _menhir_action_23 () in
                      _menhir_run_138_spec_003 _menhir_stack _v
                  | _ ->
                      _eRR ())
              | _ ->
                  _eRR ())
          | _ ->
              _eRR ())
      | _ ->
          _eRR ()
  
end

let mainprogram =
  fun _menhir_lexer _menhir_lexbuf ->
    let _menhir_stack = () in
    let MenhirBox_mainprogram v = _menhir_run_000 _menhir_stack _menhir_lexbuf _menhir_lexer in
    v
