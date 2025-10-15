package ast;

import java.util.Set;

public sealed class Expr 
  permits Bool, Real, Symbol, Var, Fun, UnOp, BinOp {
  public enum Op {
    /* UnOps */
    EXP,   // e^ix
    SQRT,
    MINUS, // -
    COS,
    SIN,
    // bool
    NOT,   // !
    /* BinOps */
    PLUS,
    SUBTRACT,
    MULT,
    DIV,
    POWER,
    // bool
    XOR,
    AND,
    OR;


    public String toEggString() {
        return "("+name()+")";
    }
  }

  @Override
  public String toString() {
    return "Expr []";
  }


  public String toEggString() {
    return "(Expr )";
  }


  public void getAllSymbols(Set<String> vars) {
    
  }
}
