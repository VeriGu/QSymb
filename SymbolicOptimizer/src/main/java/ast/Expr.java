package ast;

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
    OR
  }

  @Override
  public String toString() {
    return "Expr []";
  }


  public String toEggString() {
    return "(Expr )";
  }
}
