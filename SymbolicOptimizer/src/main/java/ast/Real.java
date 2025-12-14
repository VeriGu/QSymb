package ast;

public final class Real extends Expr {
  private double number;

  public Real(double number) {
    this.number = number;
  }

  public double getNumber() {
    return number;
  }

  public void setNumber(double number) {
    this.number = number;
  }

  @Override
  public String toString() {
    return "" + number;
  }

  @Override
  public String toEggString() {
    if(number == Math.floor(number) && !Double.isInfinite(number)) {
      return String.format("(Real %.1f)", number);
    }
    return String.format("(Real %.17g)", number);
  }
}
