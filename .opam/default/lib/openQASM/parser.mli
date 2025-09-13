
(* The type of tokens. *)

type token = 
  | U
  | TIMES
  | TAN
  | STRING of (string)
  | SQRT
  | SIN
  | SEMICOLON
  | RPAREN
  | RESET
  | REAL of (float)
  | RBRACKET
  | RBRACE
  | QREG
  | POW
  | PLUS
  | PI
  | OPENQASM
  | OPAQUE
  | NINT of (int)
  | MINUS
  | MEASURE
  | LPAREN
  | LN
  | LBRACKET
  | LBRACE
  | INCLUDE
  | IF
  | ID of (string)
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

(* This exception is raised by the monolithic API functions. *)

exception Error

(* The monolithic API. *)

val mainprogram: (Lexing.lexbuf -> token) -> Lexing.lexbuf -> (AST.program)
