package ast;

import java.util.Set;

public sealed class Expr
  permits Bool, Real, Symbol, Var, Fun, UnOp, BinOp {
  public enum Op {
    EXP,
    SQRT,
    MINUS,
    COS,
    SIN,
    NOT,
    PLUS,
    SUBTRACT,
    MULT,
    DIV,
    POWER,
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
