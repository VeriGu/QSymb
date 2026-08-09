import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;

import org.apache.commons.math3.complex.Complex;

import ast.Expr;
import ast.Fun;
import ast.Real;
import ast.Symbol;
import ast.UnOp;
import ast.Var;
import ast.Expr.Op;
import ast.BinOp;
import ast.Bool;
import java.util.Set;

import java.util.TreeSet;

public class Verifier {
  private static final int TOLERANCE = -8;
  private static final int RANDOM_SAMPLES = 10;
  private Random rand;
  private Map<Integer, boolean[][]> termsMap;
  private Map<Integer, List<List<Integer>>> permsMap;
  private int maxQubits;
  private List<List<Complex>> randomQubitStates;
  private List<List<Complex>> randomQubitStates2;

  public Verifier(Random rand, int maxQubits) {
    this.rand = rand;
    this.termsMap = new HashMap<>();
    for (int i = 1; i <= maxQubits; i++) {
      termsMap.put(i, generateTerms(i));
    }

    this.randomQubitStates = new ArrayList<>();

    for(int k = 0; k < RANDOM_SAMPLES; k++) {
      List<Complex> randomQubitState = new ArrayList<>();
      double norm = 0.0;
      for(int i = 0; i < 1 << maxQubits; i++) {
        double theta1 = rand.nextGaussian();
        double theta2 = rand.nextGaussian();
        Complex phase = new Complex(theta1, theta2);
        norm += theta1 * theta1 + theta2 * theta2;
        randomQubitState.add(phase);
      }
      norm = Math.sqrt(norm);
      for(int i = 0; i < randomQubitState.size(); i++) {
        Complex v = randomQubitState.get(i).divide(new Complex(norm));
        randomQubitState.set(i, v);
      }
      this.randomQubitStates.add(randomQubitState);
    }

    this.randomQubitStates2 = new ArrayList<>();
    for (int k = 0; k < RANDOM_SAMPLES; k++) {
      List<Complex> randomQubitState2 = new ArrayList<>();
      double norm = 0.0;
      for(int i = 0; i < 1 << maxQubits; i++) {
        double theta = rand.nextGaussian();
        double theta2 = rand.nextGaussian();
        Complex phase = new Complex(theta, theta2);
        norm += theta * theta + theta2 * theta2;
        randomQubitState2.add(phase);
      }
      norm = Math.sqrt(norm);
      for(int i = 0; i < randomQubitState2.size(); i++) {
        Complex v = randomQubitState2.get(i).divide(new Complex(norm));
        randomQubitState2.set(i, v);
      }
      this.randomQubitStates2.add(randomQubitState2);
    }

    if (maxQubits < 4) {
      this.permsMap = new HashMap<>();
      for (int i = 1; i <= maxQubits; i++) {
        int numTerms = 1 << i;
        List<List<Integer>> perms = new ArrayList<>();
        perm(numTerms, new ArrayList<>(), numTerms, perms);
        permsMap.put(numTerms, perms);
      }
    }
    this.maxQubits = maxQubits;
  }

  public List<List<Integer>> verify(Circuit c1, Circuit c2) {
    Circuit cDiff = subtractCircuits(c1, c2);

    boolean[][] terms = termsMap.get(cDiff.getQubits().size());
    boolean[][] funTerms = cDiff.getUsedQubits().size() > 0 ? termsMap.get(cDiff.getUsedQubits().size()) : terms;
    List<Map<String, Integer>> qubitMaps = getQubitMaps(cDiff, terms);

    if (verifyHelper(cDiff, terms, qubitMaps, Collections.nCopies(terms.length, new HashMap<>()))) {
      return new ArrayList<>();
    }

    List<List<Integer>> perms = permsMap.get(funTerms.length);
    ArrayList<List<Integer>> constraints = new ArrayList<>();

    for (List<Integer> perm : perms) {
      List<Map<String, Expr>> funMaps = getFunMaps(cDiff, funTerms, perm);
      if (verifyHelper(cDiff, terms, qubitMaps, funMaps)) {
        constraints.add(perm);
      }
    }
    if (!constraints.isEmpty()) {
      return constraints;
    }

    return null;
  }

  public boolean verify(Circuit c1, Circuit c2, List<Integer> perm) {
    Circuit cDiff = subtractCircuits(c1, c2);

    boolean[][] terms = termsMap.get(cDiff.getQubits().size());
    boolean[][] funTerms = cDiff.getUsedQubits().size() > 0 ? termsMap.get(cDiff.getUsedQubits().size()) : terms;
    List<Map<String, Integer>> qubitMaps = getQubitMaps(cDiff, terms);
    List<Map<String, Expr>> funMaps = getFunMaps(cDiff, funTerms, perm);

    return verifyHelper(cDiff, terms, qubitMaps, funMaps);
  }

  public boolean verifyv2(Circuit c1, Circuit c2, Map<String, Double> symbolMap) {
    java.util.LinkedHashSet<String> qubitSet = new java.util.LinkedHashSet<>(c1.getQubits());
    qubitSet.addAll(c2.getQubits());
    List<String> unionQubits = new ArrayList<>(qubitSet);

    boolean[][] terms = termsMap.get(unionQubits.size());

    List<Map<String, Integer>> qubitMaps = new ArrayList<>();
    for (boolean[] qubitMapping : terms) {
      HashMap<String, Integer> qubitMap = new HashMap<>();
      int idx = 0;
      for (String qubit : unionQubits) {
        qubitMap.put(qubit, qubitMapping[idx] ? 1 : 0);
        idx++;
      }
      qubitMaps.add(qubitMap);
    }

    List<Complex[]> phiPairs = new ArrayList<>();
    for (Map<String, Integer> qubitMap : qubitMaps) {
      List<Concrete> evaluatedCircuit = evalCircuit(c1, qubitMap, symbolMap, new HashMap<>());
      List<Concrete> evaluatedCircuit2 = evalCircuit(c2, qubitMap, symbolMap, new HashMap<>());
      List<Concrete> groupedCircuit = groupTerms(evaluatedCircuit, terms);
      List<Concrete> groupedCircuit2 = groupTerms(evaluatedCircuit2, terms);
      if (groupedCircuit.size() != groupedCircuit2.size()) {
        return false;
      }
      for (int i = 0; i < groupedCircuit.size(); i++) {
        phiPairs.add(new Complex[]{groupedCircuit.get(i).getPhi(), groupedCircuit2.get(i).getPhi()});
      }
    }

    double tol = Math.pow(10, TOLERANCE);

    Complex candidate = null;
    double bestMag = -1;
    for (Complex[] pp : phiPairs) {
      double m1 = pp[0].abs();
      double m2 = pp[1].abs();
      boolean z1 = m1 < tol;
      boolean z2 = m2 < tol;
      if (z1 != z2) return false;
      if (z1) continue;
      if (m2 > bestMag) {
        bestMag = m2;
        candidate = pp[0].divide(pp[1]);
      }
    }

    if (candidate == null) return true;

    if (Math.abs(candidate.abs() - 1.0) >= tol) return false;

    for (Complex[] pp : phiPairs) {
      Complex phi1 = pp[0];
      Complex phi2 = pp[1];
      if (phi1.subtract(candidate.multiply(phi2)).abs() >= tol) {
        return false;
      }
    }
    return true;
  }

  public boolean verify(Circuit c, Map<String, Integer> qubitMap, Map<String, Boolean> expectedMap) {
    if (c.getSize() == 0) {
      for (String q : qubitMap.keySet()) {
        if (!(qubitMap.get(q) == (expectedMap.get(q) ? 1 : 0))) {
          return false;
        }
      }
      return true;
    }

    for (String qubit : expectedMap.keySet()) {
      if (!c.getUsedQubits().contains(qubit)) {
        return false;
      }
    }

    List<String> qubitsNotInMap = new ArrayList<>();
    for (String qubit : c.getUsedQubits()) {
      if (!qubitMap.containsKey(qubit)) {
        qubitsNotInMap.add(qubit);
      }
    }

    List<Map<String, Integer>> qubitMaps = new ArrayList<>();

    if (!qubitsNotInMap.isEmpty()) {
      boolean[][] terms = termsMap.get(qubitsNotInMap.size());
      for (boolean[] qubitMapping : terms) {
        Map<String, Integer> map = new HashMap<>();
        map.putAll(qubitMap);
        int i = 0;
        for (String qubit : qubitsNotInMap) {
          map.put(qubit, qubitMapping[i] ? 1 : 0);
          i++;
        }
        qubitMaps.add(map);
      }
    } else {
      qubitMaps.add(qubitMap);
    }

    for (int i = 0; i < qubitMaps.size(); i++) {
      List<Concrete> evaluatedCircuit = evalCircuit(c, qubitMaps.get(i), new HashMap<>(), new HashMap<>());
      List<Concrete> groupedCircuit = groupTerms(evaluatedCircuit, termsMap.get(c.getUsedQubits().size()));

      for (Concrete concrete : groupedCircuit) {
        if (!(concrete.getPhi().abs() - 0 < Math.pow(10, TOLERANCE))) {
          for (String qubit : expectedMap.keySet()) {
            if (c.getUsedQubits().contains(qubit)) {
              if (!(concrete.getF()[c.getUsedQubits().stream().toList().indexOf(qubit)] == expectedMap.get(qubit))) {
                return false;
              }
            }
          }
        }
      }
    }

    return true;
  }

  private static List<Double> quantize(List<Double> values, double precision) {
        List<Double> out = new ArrayList<>(values.size());
        for (double v : values) {
            double q = Math.rint(v / precision) * precision;
            if (q == -0.0) q = 0.0;
            out.add(q);
        }
        return out;
  }

  public List<SimpleEntry<Integer, List<Integer>>> hashCode(Circuit c, Map<String, Double> symbolMap) {
    ArrayList<SimpleEntry<Integer, List<Integer>>> result = new ArrayList<>();
    List<Map<String, Integer>> qubitMaps = getQubitMaps(c, termsMap.get(c.getQubits().size()));
    List<Double> fingerprints = new ArrayList<>();
    for(int k = 0; k < RANDOM_SAMPLES; k++) {
      List<Complex> randomQubitState = randomQubitStates.get(k);
      List<Complex> randomQubitState2 = randomQubitStates2.get(k);
      List<Concrete> newcircuit = new ArrayList<>();
      for(int i = 0; i < qubitMaps.size(); i++) {
        Complex v = randomQubitState2.get(i);
        Map<String, Integer> qubitMap = qubitMaps.get(i);
        List<Concrete> evaluatedCircuit = evalCircuit(c, qubitMap, symbolMap, new HashMap<>());
        List<Concrete> groupedCircuit = groupTerms(evaluatedCircuit, termsMap.get(c.getQubits().size()));
        for(int j = 0; j < groupedCircuit.size(); j++) {
          Complex phi = groupedCircuit.get(j).getPhi();
          Complex finger = v.conjugate().multiply(phi);
          newcircuit.add(new Concrete(finger, groupedCircuit.get(j).getF()));
        }
      }
      List<Concrete> groupedCircuit = groupTerms(newcircuit, termsMap.get(c.getQubits().size()));
      Complex squaredmod = new Complex(0.0);
      for (int j = 0; j < groupedCircuit.size(); j++) {
        Complex phi = groupedCircuit.get(j).getPhi();
        Complex finger = randomQubitState.get(j).conjugate().multiply(phi);
        squaredmod = squaredmod.add(finger);
      }
      double finger = Math.rint(squaredmod.abs() / 1e-6) * 1e-6;
      fingerprints.add(finger);
    }

    result.add(new SimpleEntry<>(fingerprints.toString().hashCode(), new ArrayList<>()));
    return result;
  }

  public Map<Integer, boolean[][]> getTermsMap() {
    return termsMap;
  }

  private boolean verifyHelper(Circuit c, boolean[][] terms, List<Map<String, Integer>> qubitMaps, List<Map<String, Expr>> funMap) {
    for (int i = 0; i < qubitMaps.size(); i++) {
      List<Concrete> evaluatedCircuit = evalCircuit(c, qubitMaps.get(i), new HashMap<>(), funMap.get(i % funMap.size()));
      List<Concrete> groupedCircuit = groupTerms(evaluatedCircuit, terms);
      if (!isZero(groupedCircuit)) {
        return false;
      }
    }
    return true;
  }

  private List<Map<String, Integer>> getQubitMaps(Circuit c, boolean[][] terms) {
    ArrayList<Map<String, Integer>> qubitMaps = new ArrayList<>();
    for (boolean[] qubitMapping : terms) {
      HashMap<String, Integer> qubitMap = new HashMap<>();
      int i = 0;
      for (String qubit : c.getQubits()) {
        qubitMap.put(qubit, qubitMapping[i] ? 1 : 0);
        i++;
      }
      qubitMaps.add(qubitMap);
    }

    return qubitMaps;
  }

  private List<Map<String, Expr>> getFunMaps(Circuit c, boolean[][] terms, List<Integer> perm) {
    if (perm.isEmpty()) {
      return Collections.nCopies(terms.length, new HashMap<>());
    }

    ArrayList<Map<String, Expr>> funMaps = new ArrayList<>();
    for (int i = 0; i < terms.length; i++) {
      boolean[] funVals = terms[perm.get(i)];
      HashMap<String, Expr> funMap = new HashMap<>();

      int j = 0;
      for (String qubit : c.getPathSum().get(0).getF().keySet()) {
        if (c.getUsedQubits().contains(qubit)) {
          funMap.put("F" + qubit, new Real(funVals[j] ? 1 : 0));
          j++;
        }
      }
      funMaps.add(funMap);
    }

    return funMaps;
  }

  private Circuit subtractCircuits(Circuit c1, Circuit c2) {
    for (String qubit : c1.getUsedQubits()) {
      if (!c2.hasQubit(qubit)) { c2.addQubit(qubit); }
    }
    for (String qubit : c2.getUsedQubits()) {
      if (!c1.hasQubit(qubit)) { c1.addQubit(qubit); }
    }

    Set<String> qubits = new TreeSet<>(c1.getUsedQubits());

    ArrayList<Symbolic> pathSum = new ArrayList<>();
    pathSum.addAll(c1.getPathSum());
    for (Symbolic s : c2.getPathSum()) {
      Symbolic negated = new Symbolic(new UnOp(Op.MINUS, s.getPhi()), s.getF());
      pathSum.add(negated);
    }

    ArrayList<String> qasm = new ArrayList<>();
    qasm.addAll(c1.getQasm());
    qasm.addAll(c2.getQasm());

    Circuit c1Minusc2 = new Circuit(new ArrayList<>(c1.getQubits()), pathSum, qasm, new ArrayList<>());
    c1Minusc2.setUsedQubits(qubits);

    return c1Minusc2;
  }

  private List<Concrete> evalCircuit(Circuit c,
                                     Map<String, Integer> qubitMap,
                                     Map<String, Double> symbolMap,
                                     Map<String, Expr> funMap) {
    ArrayList<Concrete> eval_circuit = new ArrayList<>();
    for (Symbolic s : c.getPathSum()) {
      Complex eval_phi = evalExpr(s.getPhi(), qubitMap, symbolMap, funMap);
      boolean[] eval_f = new boolean[s.getF().size()];
      int i = 0;
      for (Expr e : s.getF().values()) {
        eval_f[i] = assertZeroOrOne(evalExpr(e, qubitMap, symbolMap, funMap)) == 1;
        i++;
      }
      eval_circuit.add(new Concrete(eval_phi, eval_f));
    }

    return eval_circuit;
  }

  public Complex evalExpr(Expr e,
                           Map<String, Integer> qubitMap,
                           Map<String, Double> symbolMap,
                           Map<String, Expr> funMap) {
    switch (e) {
      case Real r: return new Complex(r.getNumber());
      case Bool b: return b.isBool() ? Complex.ONE : Complex.ZERO;
      case Var v: return new Complex(qubitMap.get(v.getId()));
      case Symbol s: return evalSymbol(s, qubitMap, symbolMap, funMap);
      case Fun f: return evalFun(f, qubitMap, symbolMap, funMap);
      case UnOp uo: return evalUnOp(uo, qubitMap, symbolMap, funMap);
      case BinOp bo: return evalBinOp(bo, qubitMap, symbolMap, funMap);
      default: assert false; return null;
    }
  }

  private Complex evalSymbol(Symbol s,
                             Map<String, Integer> qubitMap,
                             Map<String, Double> symbolMap,
                             Map<String, Expr> funMap) {
    String symbol = s.getSymbol();
    switch (symbol) {
      case Symbolic.S_I: return Complex.I;
      case Symbolic.S_PI: return new Complex(Math.PI);
      case Symbolic.S_PHI: {
        if (symbolMap.containsKey(symbol)) {
          return Complex.I.multiply(Math.PI).multiply(symbolMap.get(symbol)).exp();
        } else {
          double randDouble = rand.nextDouble();
          Complex v = Complex.I.multiply(Math.PI).multiply(randDouble).exp();
          symbolMap.put(symbol, randDouble);
          return v;
        }
      }
      default: {
        assert symbol.contains("theta");
        if (symbolMap.containsKey(symbol)) {
          return new Complex(symbolMap.get(symbol));
        } else {
          double randDouble = rand.nextDouble();
          symbolMap.put(symbol, randDouble);
          return new Complex(randDouble);
        }
      }
    }
  }

  private Complex evalFun(Fun f,
                          Map<String, Integer> qubitMap,
                          Map<String, Double> symbolMap,
                          Map<String, Expr> funMap) {
    Complex arg = evalExpr(f.getArg(), qubitMap, symbolMap, funMap);
    int v = assertZeroOrOne(arg);
    if (funMap.containsKey(f.getName())) {
      qubitMap.put(f.getQubit(), v);
      return evalExpr(funMap.get(f.getName()), qubitMap, symbolMap, funMap);
    } else {
      return arg;
    }
  }

  private Complex evalUnOp(UnOp uo,
                           Map<String, Integer> qubitMap,
                           Map<String, Double> symbolMap,
                           Map<String, Expr> funMap) {
    Complex v = evalExpr(uo.getE(), qubitMap, symbolMap, funMap);
    switch (uo.getOp()) {
      case EXP: return Complex.I.multiply(v).exp();
      case SQRT: return v.sqrt();
      case MINUS: return v.negate();
      case NOT: {
        int vint = assertZeroOrOne(v);
        return vint == 0 ? Complex.ONE : Complex.ZERO;
      }
      case COS: return v.cos();
      case SIN: return v.sin();
      default: throw new RuntimeException(String.format("unimplemented UnOp: %s", uo.getOp()));
    }
  }

  private Complex evalBinOp(BinOp bo,
                            Map<String, Integer> qubitMap,
                            Map<String, Double> symbolMap,
                            Map<String, Expr> funMap) {
    Complex v1 = evalExpr(bo.getE1(), qubitMap, symbolMap, funMap);
    Complex v2 = evalExpr(bo.getE2(), qubitMap, symbolMap, funMap);
    switch (bo.getOp()) {
      case PLUS: return v1.add(v2);
      case SUBTRACT: return v1.subtract(v2);
      case MULT: return v1.multiply(v2);
      case DIV: return v1.divide(v2);
      case POWER: return v1.pow(v2);
      case XOR: {
        int v1int = assertZeroOrOne(v1);
        int v2int = assertZeroOrOne(v2);
        return new Complex(v1int ^ v2int);
      }
      case AND: {
        int v1int = assertZeroOrOne(v1);
        int v2int = assertZeroOrOne(v2);
        return new Complex(v1int & v2int);
      }
      case OR: {
        int v1int = assertZeroOrOne(v1);
        int v2int = assertZeroOrOne(v2);
        return new Complex(v1int | v2int);
      }
      default: throw new RuntimeException(String.format("unimplemented BinOp: %s", bo.getOp()));
    }
  }

  private int assertZeroOrOne(Complex c) {
    if (Math.round(c.getImaginary()) != 0) {
      throw new RuntimeException("unreachable. complex val in f");
    }

    long v = Math.round(c.getReal());
    if (v != 0 && v != 1) {
      throw new RuntimeException("unreachable. non-bool val in f");
    }

    return (int) v;
  }

  private List<Concrete> groupTerms(List<Concrete> evaluatedCircuit, boolean[][] terms) {
    ArrayList<Concrete> groupedCircuit = new ArrayList<>();
    for (boolean[] term : terms) {
      Complex coefficient = Complex.ZERO;
      for (Concrete c : evaluatedCircuit) {
        if (Arrays.equals(term, c.getF())) {
          coefficient = coefficient.add(c.getPhi());
        }
      }
      groupedCircuit.add(new Concrete(coefficient, term));
    }
    return groupedCircuit;
  }

  private boolean isZero(List<Concrete> groupedCircuit) {
    for (Concrete c : groupedCircuit) {
      if (!(c.getPhi().abs() - 0 < Math.pow(10, TOLERANCE))) {
        return false;
      }
    }
    return true;
  }

  private boolean[][] generateTerms(int numQubits) {
    int numTerms = 1 << numQubits;
    boolean[][] terms = new boolean[numTerms][numQubits];
    for (int i = 0; i < numTerms; i++) {
      for (int j = 0; j < numQubits; j++) {
        terms[i][j] = (1 & ((numTerms * j + i) >> j)) == 1;
      }
    }

    return terms;
  }

  private void perm(int numItems, List<Integer> perm, int length,  List<List<Integer>> acc) {
    if (perm.size() == length) {
      acc.add(perm);
      return;
    }

    for (int i = 0; i < numItems; i++) {
      ArrayList<Integer> copy = new ArrayList<>(perm);
      if (!copy.contains(i)) {
        copy.add(i);
        perm(numItems, copy, length, acc);
      }
    }
  }

  public static void main(String[] args) {
    ArrayList<String> qubits = new ArrayList<>(Arrays.asList("q0", "q1"));
    Expr phi = new Real(1);
    TreeMap<String, Expr> f = new TreeMap<>(Map.of(
            "q0", new Var("q0"),
            "q1", new Var("q1")
    ));

    Symbolic s = new Symbolic(phi, f);
    ArrayList<Symbolic> pathSum = new ArrayList<>(Arrays.asList(s));
    Circuit c1 = new Circuit(qubits, pathSum, new ArrayList<>(), new ArrayList());

    Symbolic.rz(c1, "q0", new Symbol("theta2"));
    Symbolic.cz(c1, "q0", "q1");
    Symbolic.rx(c1, "q0", new Symbol("pi"));
    Symbolic.rz(c1, "q0", new Symbol("theta1"));

    ArrayList<String> qubits2 = new ArrayList<>(Arrays.asList("q0", "q1"));
    Expr phi2 = new Real(1);
    TreeMap<String, Expr> f2 = new TreeMap<>(Map.of(
            "q0", new Var("q0"),
            "q1", new Var("q1")
    ));

    Symbolic s2 = new Symbolic(phi2, f2);
    ArrayList<Symbolic> pathSum2 = new ArrayList<>(Arrays.asList(s2));
    Circuit c2 = new Circuit(qubits2, pathSum2, new ArrayList<>(), new ArrayList<>());

    Symbolic.cz(c2, "q0", "q1");
    Symbolic.rx(c2, "q0", new Symbol("pi"));
    Symbolic.rz(c2, "q0", new BinOp(Op.SUBTRACT, new Symbol("theta1"), new Symbol("theta2")));

    Random rand = new Random();
    rand.setSeed(54);
    Verifier verifier = new Verifier(rand, 2);
    List<List<Integer>> result = verifier.verify(c1, c2);

    if (result == null) {
      System.out.println("not eq");
    } else {
      if (result.size() == 0) {
        System.out.println("eq, no constraints");
      } else {
        System.out.println("eq, constraints: " + result.toString());
      }
    }

     HashMap<String, Double> symbolMap = new HashMap<>();
     symbolMap.put(Symbolic.S_PHI, rand.nextDouble());
     symbolMap.put("theta1", rand.nextDouble());
     symbolMap.put("theta2", rand.nextDouble());
     System.out.println(verifier.hashCode(c1, symbolMap));
     System.out.println(verifier.hashCode(c2, symbolMap));
  }
}
