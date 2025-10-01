import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.math3.complex.Complex;

import ast.BinOp;
import ast.Bool;
import ast.Expr;
import ast.Expr.Op;
import ast.Fun;
import ast.Real;
import ast.Symbol;
import ast.UnOp;
import ast.Var;

public class Verifier {
  private static final int TOLERANCE = -8;

  private Random rand;
  private Map<Integer, boolean[][]> termsMap; // not worth using bitset unless > 8 qubits
  private Map<Integer, List<List<Integer>>> permsMap;
  private int maxQubits;

  public Verifier(Random rand, int maxQubits) {
    this.rand = rand;
    this.termsMap = new HashMap<>();
    for (int i = 1; i <= maxQubits; i++) {
      boolean[][] terms = generateTerms(i);
      termsMap.put(i, terms);
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
    // c1 - c2
    Circuit cDiff = subtractCircuits(c1, c2);

    // enumerate qubits
    boolean[][] terms = termsMap.get(cDiff.getQubits().size());
    boolean[][] funTerms = cDiff.getUsedQubits().size() > 0 ? termsMap.get(cDiff.getUsedQubits().size()) : terms;
    List<Map<String, Integer>> qubitMaps = getQubitMaps(cDiff, terms);


    if (verifyHelper(cDiff, terms, qubitMaps, new HashMap<>(), funTerms, new ArrayList<>())) {
      return new ArrayList<>();
    }

    // enumerate functions
    List<List<Integer>> perms = permsMap.get(funTerms.length);
    ArrayList<List<Integer>> constraints = new ArrayList<>();

    for (List<Integer> perm : perms) {
      //List<Map<String, Expr>> funMaps = getFunMaps(cDiff, funTerms, perm);
      Map<String, Integer> reverseMap = new HashMap<>();
      int j = 0;
      for (boolean[] term : terms) {
        String sterm = Arrays.toString(term);
        reverseMap.put(sterm, j);
        j++;
      }
      if (verifyHelper(cDiff, terms, qubitMaps, reverseMap, funTerms, perm)) {
        constraints.add(perm);
      }
    }
    if (!constraints.isEmpty()) {
      return constraints;
    }

    return null;
  }

  public boolean verifyv2(Circuit c1, Circuit c2, List<Integer> perm, Map<String, Double> symbolMap) {
    System.out.println("Verifying equivalence of circuits:" + c1.getQasm() + "|" + c2.getQasm());
    System.out.println("With permutation:" + perm.toString());
    assert (c1.getQubits().size() == c2.getQubits().size());

    boolean[][] terms = termsMap.get(c1.getQubits().size());
    boolean[][] funterms = termsMap.get(Integer.max(c1.getUsedQubits().size(), c2.getUsedQubits().size()));
    //they should have same number of qubits

    List<Map<String, Integer>> qubitMaps = getQubitMaps(c1, terms);



    Map<String, Integer> reverseMap = new HashMap<>();
    int j = 0;
    for (boolean[] term : funterms) {
      String sterm = Arrays.toString(funterms[j]);
      reverseMap.put(sterm, j);
      j++;
    }

    for (int i = 0; i < qubitMaps.size(); i++) {
      List<Concrete> evaluatedCircuit1 = evalCircuit(c1, new HashMap<>(qubitMaps.get(i)), symbolMap, reverseMap, funterms, perm);
      List<Concrete> evaluatedCircuit2 = evalCircuit(c2, new HashMap<>(qubitMaps.get(i)), symbolMap, reverseMap, funterms, perm);
      List<Concrete> groupedCircuit1 = groupTerms(evaluatedCircuit1, terms);
      List<Concrete> groupedCircuit2 = groupTerms(evaluatedCircuit2, terms);

      if(groupedCircuit1.size() != groupedCircuit2.size()) {
        System.out.println("Grouped circuits have different sizes: " + groupedCircuit1.size() + " vs " + groupedCircuit2.size());
        return false;
      }

      for(int k = 0; k < groupedCircuit1.size(); k++) {
        Concrete con1 = groupedCircuit1.get(k);
        Concrete con2 = groupedCircuit2.get(k);
        if(!(con1.getPhi().subtract(con2.getPhi()).abs() < Math.pow(10, TOLERANCE))) {
          System.out.println("Grouped circuits differ in phi: " + con1.getPhi() + " vs " + con2.getPhi());
          return false;
        }
        if(!Arrays.equals(con1.getF(), con2.getF())) {
          System.out.println("Grouped circuits differ in f: " + Arrays.toString(con1.getF()) + " vs " + Arrays.toString(con2.getF()));
          return false;
        }
      }
    }
    return true;
  }

  public boolean verify(Circuit c1, Circuit c2, List<Integer> perm) {
    // c1 - c2
    //System.out.println("Verifying equivalence of circuits:" + c1.getQasm() + "|" + c2.getQasm());
    //System.out.println("With permutation:" + perm.toString());
    Circuit cDiff = subtractCircuits(c1, c2);

    // enumerate qubits
    boolean[][] terms = termsMap.get(cDiff.getQubits().size());
    boolean[][] funTerms = cDiff.getUsedQubits().size() > 0 ? termsMap.get(cDiff.getUsedQubits().size()) : terms;
    List<Map<String, Integer>> qubitMaps = getQubitMaps(cDiff, terms);

    Map<String, Integer> reverseMap = new HashMap<>();
    int j = 0;
    for (boolean[] term : funTerms) {
      String sterm = Arrays.toString(funTerms[j]);
      reverseMap.put(sterm, j);
      j++;
    }
    //List<Map<String, Expr>> funMaps = getFunMaps(cDiff, funTerms, perm);

    return verifyHelper(cDiff, terms, qubitMaps, reverseMap, funTerms, perm);
  }

  public boolean verify(Circuit c, Map<String, Integer> qubitMap, Map<String, Boolean> expectedMap) {
    System.out.println("Verifying Circuit: " + c.getQasmString());
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
    //java.util.Collections.sort(c.getUsedQubits());
    for (String qubit : c.getUsedQubits()) {
      if (!qubitMap.containsKey(qubit)) {
        qubitsNotInMap.add(qubit);
      }
    }

    List<Map<String, Integer>> qubitMaps = new ArrayList<>();
    boolean[][] terms = termsMap.get(qubitsNotInMap.size());
    if (!qubitsNotInMap.isEmpty()) {
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
      List<Concrete> evaluatedCircuit = evalCircuit(c, qubitMaps.get(i), new HashMap<>(), new HashMap<>(), terms, new ArrayList<>());
      List<Concrete> groupedCircuit = groupTerms(evaluatedCircuit, termsMap.get(c.getUsedQubits().size()));

      for (Concrete concrete : groupedCircuit) {
        if (!(concrete.getPhi().abs() - 0 < Math.pow(10, TOLERANCE))) {
          for (String qubit : expectedMap.keySet()) {
            if (c.getUsedQubits().contains(qubit)) {
              if (!(concrete.getF()[Integer.parseInt(qubit.replace("q", ""))] == expectedMap.get(qubit))) {
                return false;
              }
            }
          }
        }
      }
    }

    return true;
  }

  public List<SimpleEntry<Integer, List<Integer>>> hashCode(Circuit c, Map<String, Double> symbolMap) {
    ArrayList<SimpleEntry<Integer, List<Integer>>> result = new ArrayList<>();
    List<Map<String, Integer>> qubitMaps = getQubitMaps(c, termsMap.get(c.getQubits().size()));
    boolean[][] terms = termsMap.get(c.getUsedQubits().size());
    if (c.hasSymb()) {
      Map<String, Integer> reverseMap = new HashMap<>();
      int j = 0;
      for (boolean[] term : terms) {
        String sterm = Arrays.toString(term);
        // System.out.println("reverse mapping " + sterm + " to " + j);
        reverseMap.put(sterm, j);
        j++;
      }
      List<List<Integer>> perms = permsMap.get(terms.length);

      for (List<Integer> perm : perms) {
        ArrayList<List<Concrete>> evaluatedCircuits = new ArrayList<>();
        //List<Map<String, Expr>> funMaps = getFunMaps(c, terms, perm);
        for (int i = 0; i < qubitMaps.size(); i++) { //for every term's qubit mapping
          List<Concrete> evaluatedCircuit = evalCircuit(c, new HashMap<>(qubitMaps.get(i)), symbolMap, reverseMap, terms, perm);
          List<Concrete> groupedCircuit = groupTerms(evaluatedCircuit, termsMap.get(c.getQubits().size()));
          evaluatedCircuits.add(groupedCircuit);
        }
        // System.out.println("Evaluated Circuits: " + evaluatedCircuits.toString() + "with hashcode:" + evaluatedCircuits.toString().hashCode());
        // the first entry is the evaluated value of the circuit, the second entry is the current symbolic function permulation
        result.add(new SimpleEntry<>(evaluatedCircuits.toString().hashCode(), perm));
      }
    } else {
      ArrayList<List<Concrete>> evaluatedCircuits = new ArrayList<>();
      for (Map<String, Integer> qubitMap : qubitMaps) {
        List<Concrete> evaluatedCircuit = evalCircuit(c, qubitMap, symbolMap, new HashMap<>(), terms, new ArrayList<>());
        List<Concrete> groupedCircuit = groupTerms(evaluatedCircuit, termsMap.get(c.getQubits().size()));
        evaluatedCircuits.add(groupedCircuit);
      }
      result.add(new SimpleEntry<>(evaluatedCircuits.toString().hashCode(), new ArrayList<>()));
    }

    return result;
  }

  public Map<Integer, boolean[][]> getTermsMap() {
    return termsMap;
  }

  private boolean verifyHelper(Circuit c, boolean[][] terms, List<Map<String, Integer>> qubitMaps, Map<String, Integer> reversemap, boolean[][] funTerms, List<Integer> perm) {
    for (int i = 0; i < qubitMaps.size(); i++) {
      List<Concrete> evaluatedCircuit = evalCircuit(c, new HashMap<>(qubitMaps.get(i)), new HashMap<>(), reversemap, funTerms, perm);
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
      if (c1.hasQubit(qubit)) {} else {
          c1.addQubit(qubit);
        }
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

  /****************** Helpers for evaluating path sum ******************/
  private List<Concrete> evalCircuit(Circuit c,
                                     Map<String, Integer> qubitMap,
                                     Map<String, Double> symbolMap,
                                     Map<String, Integer> reversemap,
                                     boolean[][] terms,
                                     List<Integer> perm) {
    ArrayList<Concrete> eval_circuit = new ArrayList<>();
    //System.out.println("Evaluating Circuit: " + c.getQasmString());
    //System.out.println("With qubit map: " + qubitMap.toString());
    //System.out.println("With perm: " + perm.toString());
    //System.out.println("With terms: " + Arrays.deepToString(terms));
    if(!c.hasSymb()) {
      for (Symbolic s : c.getPathSum()) {
        Complex eval_phi = evalExpr(s.getPhi(), qubitMap, symbolMap);
        boolean[] eval_f = new boolean[s.getF().size()];
        int i = 0;
        for (Expr e : s.getF().values()) {
          eval_f[i] = assertZeroOrOne(evalExpr(e, qubitMap, symbolMap)) == 1;
          i++;
        }
        eval_circuit.add(new Concrete(eval_phi, eval_f));
      }
    } else {
      for(Symbolic s : c.getPathSum()) {
        Complex eval_phi = evalExpr(s.getPhi(), qubitMap, symbolMap);
        boolean[] eval_f = new boolean[s.getF().size()];
        Map<String, Expr> exprs = new TreeMap<>();
        for (String qubit: s.getF().keySet()) {
          //System.out.println("before evaluating expr: " + s.getF().get(qubit).toString());
          SimpleEntry<Expr, Boolean> entry = evalExprBeforeFun(s.getF().get(qubit), qubitMap, symbolMap);
          //System.out.println("evaluated beforefun: " + entry.getKey().toString() + ", has fun? " + entry.getValue());
          if(c.getUsedQubits().contains(qubit)) {
            exprs.put(qubit, entry.getKey());
          }
        }
        //now calculate the function and update qubitMap
        boolean[] intermediate = new boolean[exprs.size()];
        int i = 0;
        for(String qubit : exprs.keySet()) {
          intermediate[i] = qubitMap.get(qubit) == 1;
          i++;
        }
        //System.out.println("qubitmap after evaluating before fun: " + qubitMap.toString());
        //TODO:: now need to permute intermediate based on permutation
        boolean[] after_map_term = terms[perm.get(reversemap.get(Arrays.toString(intermediate)))];
        //System.out.println("term after permutation: " + Arrays.toString(after_map_term));
        i = 0;
        for(String qubit: exprs.keySet()) {
          int val = after_map_term[i] ? 1 : 0;
          qubitMap.put(qubit, val);
          i++;
        }
        //System.out.println("qubitmap after permutation: " + qubitMap.toString());

        i = 0;
        for (Expr e : exprs.values()) {
          // System.out.println("evaluating after fun: " + e.toString());
          eval_f[i] = assertZeroOrOne(evalExpr(e, qubitMap, symbolMap)) == 1;
          i++;
        }
        //System.out.println("eval_f: " + Arrays.toString(eval_f));
        eval_circuit.add(new Concrete(eval_phi, eval_f));
      }
    }
    // System.out.println("Evaluated Circuit: " + eval_circuit.toString());
    return eval_circuit;
  }

  private SimpleEntry<Expr, Boolean> evalExprBeforeFun(Expr e,  Map<String, Integer> qubitMap, Map<String, Double> symbolMap) {
    switch (e) {
      case Real r: return new SimpleEntry<>(e, false);
      case Bool b: return new SimpleEntry<>(e, false);
      case Var v: return new SimpleEntry<>(new Real(qubitMap.get(v.getId())), false);
      case Fun f: return evalBeforeFun(f, qubitMap, symbolMap);
      case UnOp uo: return evalUnOpBeforeFun(uo, qubitMap, symbolMap);
      case BinOp bo: return evalBinOpBeforeFun(bo, qubitMap, symbolMap);
      default: assert false; return null; // stupid hack to make the compiler happy ugh
    }
  }

  private SimpleEntry<Expr, Boolean> evalBeforeFun(Fun f,
                          Map<String, Integer> qubitMap,
                          Map<String, Double> symbolMap
                          ) {
      
      Complex arg = evalExpr(f.getArg(), qubitMap, symbolMap);
      int v = assertZeroOrOne(arg);
      qubitMap.put(f.getQubit(), v);
      
      return new SimpleEntry<>(new Fun(f.getQubit(), new Var(f.getQubit())), true);
  }

  // assume only the transformer function uses it
  private SimpleEntry<Expr, Boolean> evalUnOpBeforeFun(UnOp uo,
                           Map<String, Integer> qubitMap,
                           Map<String, Double> symbolMap
                           ) {
    Expr e = uo.getE();
    SimpleEntry<Expr, Boolean> entry = evalExprBeforeFun(e, qubitMap, symbolMap);
    Expr val = entry.getKey();
    if(entry.getValue()) {
      return new SimpleEntry<>(new UnOp(uo.getOp(), val), true);
    }

    Complex v = evalExpr(val, qubitMap, symbolMap);
    int rv = assertZeroOrOne(v);
    switch (uo.getOp()) {
      case MINUS: return new SimpleEntry<>(new Real(-rv), false);
      case NOT: {
        int vint = assertZeroOrOne(v);
        return new SimpleEntry<>(vint == 0 ? new Real(1) : new Real(0), false);
      }
      default: throw new RuntimeException(String.format("unimplemented UnOp: %s", uo.getOp()));
    }
  }

  private SimpleEntry<Expr, Boolean> evalBinOpBeforeFun(BinOp bo,
                            Map<String, Integer> qubitMap,
                            Map<String, Double> symbolMap
                            ) {
    SimpleEntry<Expr, Boolean> entry1 = evalExprBeforeFun(bo.getE1(), qubitMap, symbolMap);
    SimpleEntry<Expr, Boolean> entry2 = evalExprBeforeFun(bo.getE2(), qubitMap, symbolMap);
    Expr v1 = entry1.getKey();
    Expr v2 = entry2.getKey();
    if (entry1.getValue() || entry2.getValue()) {
      return new SimpleEntry<>(new BinOp(bo.getOp(), v1, v2), true);
    }

    int i1 = assertZeroOrOne(evalExpr(v1, qubitMap, symbolMap));
    int i2 = assertZeroOrOne(evalExpr(v2, qubitMap, symbolMap));
    
    switch (bo.getOp()) {
      case XOR: {
        return new SimpleEntry<>(new Real(i1 ^ i2), false);
      }
      case AND: {
        return new SimpleEntry<>(new Real(i1 & i2), false);
      }
      case OR: {
        return new SimpleEntry<>(new Real(i1 | i2), false);
      }
      default: throw new RuntimeException(String.format("unimplemented BinOp: %s", bo.getOp()));
    }
  }

  public Complex evalExpr(Expr e,
                           Map<String, Integer> qubitMap,
                           Map<String, Double> symbolMap
                         ) {
    switch (e) {
      case Real r: return new Complex(r.getNumber());
      case Bool b: return b.isBool() ? Complex.ONE : Complex.ZERO;
      case Var v: return new Complex(qubitMap.get(v.getId()));
      case Fun f: return evalFun(f, qubitMap, symbolMap);
      case Symbol s: return evalSymbol(s, qubitMap, symbolMap);
      case UnOp uo: return evalUnOp(uo, qubitMap, symbolMap);
      case BinOp bo: return evalBinOp(bo, qubitMap, symbolMap);
      default: assert false; return null; // stupid hack to make the compiler happy ugh
    }
  }

  private Complex evalSymbol(Symbol s,
                             Map<String, Integer> qubitMap,
                             Map<String, Double> symbolMap
                             ) {
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
                          Map<String, Double> symbolMap
                          ) {
    Complex arg = evalExpr(f.getArg(), qubitMap, symbolMap);
    return arg;
  }

  private Complex evalUnOp(UnOp uo,
                           Map<String, Integer> qubitMap,
                           Map<String, Double> symbolMap
                           ) {
    Complex v = evalExpr(uo.getE(), qubitMap, symbolMap);
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
                            Map<String, Double> symbolMap
                            ) {
    Complex v1 = evalExpr(bo.getE1(), qubitMap, symbolMap);
    Complex v2 = evalExpr(bo.getE2(), qubitMap, symbolMap);
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
    int numTerms = 1 << numQubits; // n qubit has 2^n number of basis
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
    Circuit c1 = new Circuit(qubits, pathSum, new ArrayList<>(), new ArrayList<>());

    Symbolic.rz(c1, "q0", new Symbol("theta2"));
    Symbolic.cz(c1, "q0", "q1");
    Symbolic.rx(c1, "q0", new Symbol("pi"));
    Symbolic.rz(c1, "q0", new Symbol("theta1"));
//    Symbolic.symb(c1, 2);
//    Symbolic.rz(c1, "q1", new Symbol("theta2"));
//     Symbolic.cx(c1, "q1", "q0");
//     Symbolic.h(c1, "q1");
//     Symbolic.h(c1, "q0");
//    Symbolic.x(c1, "q0");
//    Symbolic.h(c1, "q0");
//    Symbolic.rz(c1, "q0", new Symbol("theta1"));
//    Symbolic.h(c1, "q0");
//    Symbolic.x(c1, "q0");


    ArrayList<String> qubits2 = new ArrayList<>(Arrays.asList("q0", "q1"));
    Expr phi2 = new Real(1);
    TreeMap<String, Expr> f2 = new TreeMap<>(Map.of(
            "q0", new Var("q0"),
            "q1", new Var("q1")
    ));

    Symbolic s2 = new Symbolic(phi2, f2);
    ArrayList<Symbolic> pathSum2 = new ArrayList<>(Arrays.asList(s2));
    Circuit c2 = new Circuit(qubits2, pathSum2, new ArrayList<>(), new ArrayList<>());

//    Symbolic.rz(c2, "q0", new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2")));
//    Symbolic.symb(c2, 2);
    Symbolic.cz(c2, "q0", "q1");
    Symbolic.rx(c2, "q0", new Symbol("pi"));
    Symbolic.rz(c2, "q0", new BinOp(Op.SUBTRACT, new Symbol("theta1"), new Symbol("theta2")));
//     Symbolic.h(c2, "q1");
//     Symbolic.h(c2, "q0");
//     Symbolic.cx(c2, "q0", "q1");
//    Symbolic.x(c2, "q0");
//    Symbolic.x(c2, "q0");
//    Symbolic.cx(c2, "q0", "q1");
//    Symbolic.rz(c2, "q0", new Symbol("theta1"));
//      Symbolic.h(c2, "q0");


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

//    System.out.println(Arrays.deepToString(verifier.termsMap.get(2)));
//    System.out.println(Arrays.deepToString(verifier.termsMap.get(3)));

     HashMap<String, Double> symbolMap = new HashMap<>();
     symbolMap.put(Symbolic.S_PHI, rand.nextDouble());
     symbolMap.put("theta1", rand.nextDouble());
     symbolMap.put("theta2", rand.nextDouble());
     System.out.println(verifier.hashCode(c1, symbolMap));
     System.out.println(verifier.hashCode(c2, symbolMap));
  }
}
