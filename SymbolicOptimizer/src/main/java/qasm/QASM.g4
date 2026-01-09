grammar QASM;

// Parser Rules
program : header? declaration* statement* EOF | ';' EOF;

header : OPENQASM_KW REAL_LITERAL ';' INCLUDE_KW STRING_LITERAL ';';

declaration : qreg_decl | creg_decl;

qreg_decl : QREG_KW ID '[' INT_LITERAL ']' ';';
creg_decl : CREG_KW ID '[' INT_LITERAL ']' ';';


rewrites : rewrite*;

rewrite_body : (statement+) | ';';

rewrite : (lhs=rewrite_body) '|' (rhs=rewrite_body) ('when' equality (','(equality))*)?;

equality : (QUBIT op='=' QUBIT) | (QUBIT op='!=' QUBIT);

statement : gate_statement;

gate_statement : ID ( '(' expression (',' expression)* ')' )? qubits ';';

qubits : qubit (',' qubit)*;

qubit : (ID '[' num=INT_LITERAL ']') | QUBIT;

measure_statement : MEASURE_KW ( ID | ID '[' INT_LITERAL ']' ) TO ( ID | ID '[' INT_LITERAL ']' ) ';';

reset_statement : RESET_KW ( ID | ID '[' INT_LITERAL ']' ) ';';

barrier_statement : BARRIER_KW ( ID | ID '[' INT_LITERAL ']' ) (',' ( ID | ID '[' INT_LITERAL ']' ))* ';';

expression : multExpr ((PLUS | MINUS) multExpr)*;
multExpr : atomExpr ( (MULT | DIV) atomExpr )*;
atomExpr : MINUS? ( REAL_LITERAL | INT_LITERAL | ID | PI_KW | '(' expression ')' );

// Lexer Rules
OPENQASM_KW : 'OPENQASM';
INCLUDE_KW : 'include';
QREG_KW : 'qreg';
CREG_KW : 'creg';
MEASURE_KW : 'measure';
TO : '->';
RESET_KW : 'reset';
BARRIER_KW : 'barrier';
PI_KW : 'pi';
QUBIT : 'q' INT_LITERAL;
ID : [a-z_][a-zA-Z0-9_]*;
REAL_LITERAL : INT_LITERAL '.' INT_LITERAL? (('e' | 'E') (MINUS)? INT_LITERAL)? | INT_LITERAL ('e' | 'E') (PLUS | MINUS)? INT_LITERAL;
INT_LITERAL : [0-9]+;
STRING_LITERAL : '"' (WORD)* '"';


WORD          : (LETTER | '_' | '.') (LETTER | DIGIT | '_' )*;


fragment DIGIT : '0'..'9';
fragment LETTER : 'a'..'z' | 'A'..'Z';

PLUS : '+';
MINUS : '-';
MULT : '*';
DIV : '/';

WHITESPACE    : [ \t\r\n]+ -> skip;