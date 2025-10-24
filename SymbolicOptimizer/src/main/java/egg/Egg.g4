grammar Egg;

parse         : constrainedCircuit EOF;

constrainedCircuit
              : LPAREN 'CCircuit' circuit permutation RPAREN
              ;

circuit       : LPAREN Nil RPAREN
              | LPAREN 'Cons' gate circuit RPAREN
              ;

permutation   : LPAREN cons='PermNil' RPAREN
              | LPAREN cons='PermCons' INTEGER permutation RPAREN
              ;

gate          : LPAREN gateN='X' qubit RPAREN
              | LPAREN gateN='CX' qubit qubit RPAREN
              | LPAREN gateN='RZ' qubit expr RPAREN
              | LPAREN gateN='H' qubit RPAREN
              | LPAREN gateN='SYMB' INTEGER RPAREN
              | LPAREN gateN='U1' qubit expr RPAREN
              | LPAREN gateN='U2' qubit expr expr RPAREN
              | LPAREN gateN='U3' qubit expr expr expr RPAREN
              | LPAREN gateN='RX' qubit expr RPAREN
              | LPAREN gateN='CZ' qubit qubit RPAREN
              | LPAREN gateN='RY' qubit expr RPAREN
              | LPAREN gateN='RXX' qubit qubit expr RPAREN
              | LPAREN gateN='GPI' qubit expr RPAREN
              | LPAREN gateN='GPI2' qubit expr RPAREN
              | LPAREN gateN='VZ' qubit expr RPAREN
              | LPAREN gateN='MS' qubit qubit expr expr RPAREN
              | LPAREN gateN='SX' qubit RPAREN
              ;

qubit         : LPAREN 'Q' STRING RPAREN
              ;

expr          : LPAREN cons='Bool' BOOLEAN RPAREN
              | LPAREN cons='Real' F64 RPAREN
              | LPAREN cons='Symbol' STRING RPAREN
              | LPAREN cons='Var' STRING RPAREN
              | LPAREN cons='Fun' STRING expr RPAREN
              | LPAREN cons='UnOp' LPAREN op RPAREN expr RPAREN
              | LPAREN cons='BinOp' LPAREN op RPAREN expr expr RPAREN
              ;

op            : 'EXP' | 'SQRT' | 'MINUS' | 'COS' | 'SIN' | 'NOT'
              | 'PLUS' | 'SUBTRACT' | 'MULT' | 'DIV' | 'POWER'
              | 'XOR' | 'AND' | 'OR'
              ;

BOOLEAN       : 'true' | 'false';
F64           : NUMBER; // f64 is represented as a number
Nil           : 'Nil';
LPAREN        : '(';
RPAREN        : ')';
STRING        : '"' (WORD)* '"';
INTEGER       : [0-9]+;
NUMBER        : '-'? [0-9]+ ('.' [0-9]+)?;
WORD          : (LETTER | '_') (LETTER | DIGIT | '_')*;
WHITESPACE    : [ \t\r\n]+ -> skip;

fragment DIGIT : '0'..'9';
fragment LETTER : 'a'..'z' | 'A'..'Z';