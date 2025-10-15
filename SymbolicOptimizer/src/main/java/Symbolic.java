import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import ast.BinOp;
import ast.Bool;
import ast.Expr;
import ast.Expr.Op;
import ast.Fun;
import ast.Real;
import ast.Symbol;
import ast.UnOp;
import ast.Var;

public class Symbolic {
  private Expr phi;
  private TreeMap<String, Expr> f; // So that keys are sorted e.g. q0, q1, ...

  public static final String SYMB = "symb q";
  public static final String S_PI = "pi";
  public static final String S_PHI = "phi";
  public static final String S_I = "i";
  private static final Expr PI = new Symbol(S_PI);
  private static final Expr PHI = new Symbol(S_PHI);
  private static final Expr ISQ2 = new BinOp(Op.DIV, new Real(1), new UnOp(Op.SQRT, new Real(2)));
  private static final Expr I = new Symbol(S_I);

  public Symbolic(Expr phi, TreeMap<String, Expr> f) {
    this.phi = phi;
    this.f = f;
  }

  public Expr getPhi() {
    return phi;
  }

  public void setPhi(Expr phi) {
    this.phi = phi;
  }

  public TreeMap<String, Expr> getF() {
    return f;
  }

  public void setF(TreeMap<String, Expr> f) {
    this.f = f;
  }

  @Override
  public String toString() {
    return "Symbolic [phi=" + phi + ", f=" + f + "]";
  }

  /************************************* GATES *************************************/
  public static void x(Circuit circuit, String qubit) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    for (Symbolic s : circuit.getPathSum()) {
      s.getF().put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));
    }

    String qasm = String.format("x q[%s]", qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.X(qubit));
  }

  public static void cx(Circuit circuit, String control, String target) {
    if (!circuit.hasQubit(control)) { circuit.addQubit(control); }
    if (!circuit.hasQubit(target)) { circuit.addQubit(target); }

    for (Symbolic s : circuit.getPathSum()) {
      s.getF().put(target, new BinOp(Op.XOR, s.getF().get(control), s.getF().get(target)));
    }

    String qasm = String.format("cx q[%s], q[%s]", control.replace("q", ""), target.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.CX(control, target));
  }

  public static void rz(Circuit circuit, String qubit, Expr angle) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    for (Symbolic s : circuit.getPathSum()) {
      s.setPhi(new BinOp(Op.MULT, s.phi, new UnOp(Op.EXP, new BinOp(Op.MULT, new BinOp(Op.DIV, angle, new Real(2)), new BinOp(Op.SUBTRACT, new BinOp(Op.MULT, new Real(2), s.getF().get(qubit)), new Real(1))))));
    }

    String qasm = String.format("rz(%s) q[%s]", angle, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.RZ(qubit, angle));
  }

  public static void h(Circuit circuit, String qubit) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phi0 = new BinOp(Op.MULT, s.phi, ISQ2);
      Expr phi1 = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, ISQ2, new UnOp(Op.EXP, new BinOp(Op.MULT, PI, s.getF().get(qubit)))));
      TreeMap<String, Expr> f0 = new TreeMap<>();
      f0.putAll(s.f);
      f0.put(qubit, new Bool(false));
      TreeMap<String, Expr> f1 = new TreeMap<>();
      f1.putAll(s.f);
      f1.put(qubit, new Bool(true));

      newPathSum.add(new Symbolic(phi0, f0));
      newPathSum.add(new Symbolic(phi1, f1));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("h q[%s]", qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.H(qubit));
  }

  public static void symb(Circuit circuit, int maxQubits) {
    for (int i = 0; i < maxQubits; i++) {
      String name = String.format("q%s", i);
      if (!circuit.hasQubit(name)) { circuit.addQubit(name); }
    }

    for (Symbolic s : circuit.getPathSum()) {
      s.setPhi(new BinOp(Op.MULT, s.phi, PHI));
      TreeMap<String, Expr> newF = new TreeMap<>();
      for (Map.Entry<String, Expr> e : s.f.entrySet()) {
        newF.put(e.getKey(), new Fun(e.getKey(), e.getValue()));
      }
      s.setF(newF);
    }

    circuit.getQasm().add(SYMB);
    circuit.getGates().add(new EggGen.SYMB(maxQubits));
  }

  public static void u1(Circuit circuit, String qubit, Expr lambda) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    for (Symbolic s : circuit.getPathSum()) {
      s.setPhi(new BinOp(Op.MULT, s.phi, new UnOp(Op.EXP, new BinOp(Op.MULT, lambda, s.getF().get(qubit)))));
    }

    String qasm = String.format("u1(%s) q[%s]", lambda, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.U1(qubit, lambda));
  }

  public static void u2(Circuit circuit, String qubit, Expr phi, Expr lambda) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phix = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, ISQ2, new UnOp(Op.EXP, new BinOp(Op.MULT, new BinOp(Op.PLUS, phi, lambda), s.getF().get(qubit)))));
      Expr phiNegx = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, ISQ2, new BinOp(Op.MULT, new UnOp(Op.EXP, new BinOp(Op.MULT, phi, new UnOp(Op.NOT, s.getF().get(qubit)))), new BinOp(Op.MULT, new UnOp(Op.EXP, new BinOp(Op.MULT, lambda, s.getF().get(qubit))), new UnOp(Op.EXP, new BinOp(Op.MULT, PI, new BinOp(Op.SUBTRACT, new Real(2), s.getF().get(qubit))))))));

      TreeMap<String, Expr> fx = new TreeMap<>();
      fx.putAll(s.f);
      TreeMap<String, Expr> fNegx = new TreeMap<>();
      fNegx.putAll(s.f);
      fNegx.put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));

      newPathSum.add(new Symbolic(phix, fx));
      newPathSum.add(new Symbolic(phiNegx, fNegx));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("u2(%s,%s) q[%s]", phi, lambda, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.U2(qubit, phi, lambda));
  }

  public static void u3(Circuit circuit, String qubit, Expr theta, Expr phi, Expr lambda) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phix = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, new UnOp(Op.COS, new BinOp(Op.DIV, theta, new Real(2))), new UnOp(Op.EXP, new BinOp(Op.MULT, new BinOp(Op.PLUS, phi, lambda), s.getF().get(qubit)))));
      Expr phiNegx = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, new UnOp(Op.SIN, new BinOp(Op.DIV, theta, new Real(2))), new BinOp(Op.MULT, new UnOp(Op.EXP, new BinOp(Op.MULT, phi, new UnOp(Op.NOT, s.getF().get(qubit)))), new BinOp(Op.MULT, new UnOp(Op.EXP, new BinOp(Op.MULT, lambda, s.getF().get(qubit))), new UnOp(Op.EXP, new BinOp(Op.MULT, PI, new BinOp(Op.SUBTRACT, new Real(2), s.getF().get(qubit))))))));

      TreeMap<String, Expr> fx = new TreeMap<>();
      fx.putAll(s.f);
      TreeMap<String, Expr> fNegx = new TreeMap<>();
      fNegx.putAll(s.f);
      fNegx.put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));

      newPathSum.add(new Symbolic(phix, fx));
      newPathSum.add(new Symbolic(phiNegx, fNegx));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("u3(%s,%s,%s) q[%s]", theta, phi, lambda, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.U3(qubit, theta, phi, lambda));
  }

  public static void rx(Circuit circuit, String qubit, Expr angle) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phix = new BinOp(Op.MULT, s.getPhi(), new UnOp(Op.COS, new BinOp(Op.DIV, angle, new Real(2))));
      Expr phiNegx = new BinOp(Op.MULT, s.getPhi(), new BinOp(Op.MULT, new UnOp(Op.EXP, new BinOp(Op.DIV, new BinOp(Op.MULT, new Real(3), PI), new Real(2))), new UnOp(Op.SIN, new BinOp(Op.DIV, angle, new Real(2)))));

      TreeMap<String, Expr> fx = new TreeMap<>();
      fx.putAll(s.f);
      TreeMap<String, Expr> fNegx = new TreeMap<>();
      fNegx.putAll(s.f);
      fNegx.put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));

      newPathSum.add(new Symbolic(phix, fx));
      newPathSum.add(new Symbolic(phiNegx, fNegx));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("rx(%s) q[%s]", angle, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.RX(qubit, angle));
  }

  public static void cz(Circuit circuit, String control, String target) {
    if (!circuit.hasQubit(control)) { circuit.addQubit(control); }
    if (!circuit.hasQubit(target)) { circuit.addQubit(target); }

    for (Symbolic s : circuit.getPathSum()) {
      s.setPhi(new BinOp(Op.MULT, s.phi, new UnOp(Op.EXP, new BinOp(Op.MULT, PI, new BinOp(Op.MULT, s.getF().get(control), s.getF().get(target))))));
    }

    String qasm = String.format("cz q[%s], q[%s]", control.replace("q", ""), target.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.CZ(control, target));
  }

  public static void ry(Circuit circuit, String qubit, Expr angle) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phix = new BinOp(Op.MULT, s.getPhi(), new UnOp(Op.COS, new BinOp(Op.DIV, angle, new Real(2))));
      Expr phiNegx = new BinOp(Op.MULT, s.getPhi(), new BinOp(Op.MULT, new UnOp(Op.EXP, new BinOp(Op.MULT, PI, s.getF().get(qubit))), new UnOp(Op.SIN, new BinOp(Op.DIV, angle, new Real(2)))));

      TreeMap<String, Expr> fx = new TreeMap<>();
      fx.putAll(s.f);
      TreeMap<String, Expr> fNegx = new TreeMap<>();
      fNegx.putAll(s.f);
      fNegx.put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));

      newPathSum.add(new Symbolic(phix, fx));
      newPathSum.add(new Symbolic(phiNegx, fNegx));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("ry(%s) q[%s]", angle, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.RY(qubit, angle));
  }

  public static void rxx(Circuit circuit, String qubit1, String qubit2, Expr angle) {
    if (!circuit.hasQubit(qubit1)) { circuit.addQubit(qubit1); }
    if (!circuit.hasQubit(qubit2)) { circuit.addQubit(qubit2); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phix = new BinOp(Op.MULT, s.getPhi(), new UnOp(Op.COS, new BinOp(Op.DIV, angle, new Real(2))));
      Expr phiNegx = new BinOp(Op.MULT, s.getPhi(), new BinOp(Op.MULT, new UnOp(Op.EXP, new BinOp(Op.DIV, new BinOp(Op.MULT, new Real(3), PI), new Real(2))), new UnOp(Op.SIN, new BinOp(Op.DIV, angle, new Real(2)))));

      TreeMap<String, Expr> fx = new TreeMap<>();
      fx.putAll(s.f);
      TreeMap<String, Expr> fNegx = new TreeMap<>();
      fNegx.putAll(s.f);
      fNegx.put(qubit1, new UnOp(Op.NOT, s.getF().get(qubit1)));
      fNegx.put(qubit2, new UnOp(Op.NOT, s.getF().get(qubit2)));

      newPathSum.add(new Symbolic(phix, fx));
      newPathSum.add(new Symbolic(phiNegx, fNegx));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("rxx(%s) q[%s], q[%s]", angle, qubit1.replace("q", ""), qubit2.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.RXX(qubit1, qubit2, angle));
  }

  public static void gpi(Circuit circuit, String qubit, Expr phi) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    for (Symbolic s : circuit.getPathSum()) {
      s.setPhi(new BinOp(Op.MULT, s.phi, new UnOp(Op.EXP, new BinOp(Op.MULT, phi, new BinOp(Op.PLUS, new BinOp(Op.MULT, new Real(-2), s.getF().get(qubit)), new Real(1))))));
      s.getF().put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));
    }

    String qasm = String.format("gpi(%s) q[%s]", phi, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.GPI(qubit, phi));
  }

  public static void gpi2(Circuit circuit, String qubit, Expr phi) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phi1 = new BinOp(Op.MULT, s.phi, ISQ2);
      Expr phi2 = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, new UnOp(Op.MINUS, ISQ2), new BinOp(Op.MULT, I, new UnOp(Op.EXP, new BinOp(Op.MULT, phi, new BinOp(Op.PLUS, new BinOp(Op.MULT, new Real(-2), s.getF().get(qubit)), new Real(1)))))));

      TreeMap<String, Expr> f1 = new TreeMap<>();
      f1.putAll(s.f);
      TreeMap<String, Expr> f2 = new TreeMap<>();
      f2.putAll(s.f);
      f2.put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));

      newPathSum.add(new Symbolic(phi1, f1));
      newPathSum.add(new Symbolic(phi2, f2));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("gpi2(%s) q[%s]", phi, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.GPI2(qubit, phi));
  }

  public static void vz(Circuit circuit, String qubit, Expr theta) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    for (Symbolic s : circuit.getPathSum()) {
      s.setPhi(new BinOp(Op.MULT, s.phi, new UnOp(Op.EXP, new BinOp(Op.MULT, new BinOp(Op.DIV, theta, new Real(2)), new BinOp(Op.SUBTRACT, new BinOp(Op.MULT, new Real(2), s.getF().get(qubit)), new Real(1))))));
    }

    String qasm = String.format("rz(%s) q[%s]", theta, qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.VZ(qubit, theta));
  }

  public static void ms(Circuit circuit, String qubit1, String qubit2, Expr phi1, Expr phi2) {
    if (!circuit.hasQubit(qubit1)) { circuit.addQubit(qubit1); }
    if (!circuit.hasQubit(qubit2)) { circuit.addQubit(qubit2); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phi11 = new BinOp(Op.MULT, s.phi, ISQ2);
      Expr phi22 = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, new UnOp(Op.MINUS, ISQ2), new BinOp(Op.MULT, I, new UnOp(Op.EXP, new BinOp(Op.MULT, new BinOp(Op.PLUS, phi1, new BinOp(Op.MULT, new BinOp(Op.PLUS, new BinOp(Op.MULT, new Real(-2), new BinOp(Op.XOR, s.getF().get(qubit1), s.getF().get(qubit2))), new Real(1)), phi2)), new BinOp(Op.PLUS, new BinOp(Op.MULT, new Real(-2), s.getF().get(qubit1)), new Real(1)))))));

      TreeMap<String, Expr> f1 = new TreeMap<>();
      f1.putAll(s.f);
      TreeMap<String, Expr> f2 = new TreeMap<>();
      f2.putAll(s.f);
      f2.put(qubit1, new UnOp(Op.NOT, s.getF().get(qubit1)));
      f2.put(qubit2, new UnOp(Op.NOT, s.getF().get(qubit2)));

      newPathSum.add(new Symbolic(phi11, f1));
      newPathSum.add(new Symbolic(phi22, f2));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("ms (%s,%s) q[%s], q[%s]", phi1, phi2, qubit1.replace("q", ""), qubit2.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.MS(qubit1, qubit2, phi1, phi2));
  }

  public static void sx(Circuit circuit, String qubit) {
    if (!circuit.hasQubit(qubit)) { circuit.addQubit(qubit); }

    ArrayList<Symbolic> newPathSum = new ArrayList<>();
    for (Symbolic s : circuit.getPathSum()) {
      Expr phi1 = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, new Real(0.5), new BinOp(Op.PLUS, new Real(1), I)));
      Expr phi2 = new BinOp(Op.MULT, s.phi, new BinOp(Op.MULT, new Real(0.5), new BinOp(Op.SUBTRACT, new Real(1), I)));

      TreeMap<String, Expr> f1 = new TreeMap<>();
      f1.putAll(s.f);
      TreeMap<String, Expr> f2 = new TreeMap<>();
      f2.putAll(s.f);
      f2.put(qubit, new UnOp(Op.NOT, s.getF().get(qubit)));

      newPathSum.add(new Symbolic(phi1, f1));
      newPathSum.add(new Symbolic(phi2, f2));
    }
    circuit.setPathSum(newPathSum);

    String qasm = String.format("sx q[%s]", qubit.replace("q", ""));
    circuit.getQasm().add(qasm);
    circuit.getGates().add(new EggGen.SX(qubit));
  }

  // for testing
  public static void main(String[] args) {
    ArrayList<String> qubits = new ArrayList<>(Arrays.asList("q0", "q1"));
    Expr phi = new Real(1);
    TreeMap<String, Expr> f = new TreeMap<>(Map.of(
      "q0", new Var("q0"),
      "q1", new Var("q1")
    ));

    Symbolic s = new Symbolic(phi, f);
    ArrayList<Symbolic> pathSum = new ArrayList<>(Arrays.asList(s));
    Circuit c = new Circuit(qubits, pathSum, new ArrayList<>(), new ArrayList<>());

    Symbolic.symb(c, 2);

    System.out.println(c);
  }
}
