package ast;

import java.util.Set;

public final class Symbol extends Expr {
  private String symbol;

  public Symbol(String symbol) {
    this.symbol = symbol;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public String toString() {
    return symbol;
  }

  @Override
  public String toEggString() {
    return String.format("(Symbol \"%s\")", symbol);
  }

  @Override
  public void getAllSymbols(Set<String> vars) {
    vars.add(symbol);
  }
}
