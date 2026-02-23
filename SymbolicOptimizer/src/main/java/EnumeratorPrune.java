import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.FileReader;
import java.io.BufferedReader;

import org.antlr.v4.runtime.tree.Tree;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.math3.analysis.function.Add;
import org.apache.commons.math3.ml.distance.EarthMoversDistance;
import org.checkerframework.checker.units.qual.s;
import org.jgrapht.GraphTests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ast.BinOp;
import ast.Expr;
import ast.Expr.Op;
import ast.Real;
import ast.Symbol;
import ast.UnOp;
import ast.Var;


public class EnumeratorPrune {
  public long egraphTime = 0;
  public long translateTime = 0;
  public long chooseTime = 0;
  public long enumerationTime = 0;
  public long filtertime = 0;
  private static final Logger logger = LoggerFactory.getLogger(EnumeratorPrune.class);
  private static final String[] ANGLES = {"theta1", "theta2", "theta3"};
//  private static final Expr[] symbAngles = {
//    new Symbol("theta1"), // nam, rigetti
//    new Symbol("theta2"), // nam, rigetti
////          new BinOp(Op.SUBTRACT, new BinOp(Op.MULT, new Real(4), new Symbol("pi")), new Symbol("theta1")),
////          new BinOp(Op.SUBTRACT, new BinOp(Op.MULT, new Real(4), new Symbol("pi")), new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2"))),
////    new Symbol("theta3"), // ibm,
//    new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2")), // nam, rigetti, ibm
////          new Symbol("pi"),
////          new BinOp(Op.DIV, new Symbol("pi"), new Real(2)),
////          new BinOp(Op.MULT, new Real(7), new BinOp(Op.DIV, new Symbol("pi"), new Real(2))),
////          new BinOp(Op.MULT, new Real(3), new Symbol("pi")),
////          new BinOp(Op.PLUS, new Symbol("theta1"), new BinOp(Op.PLUS, new Symbol("theta2"), new Symbol("theta3"))), // ibm
//  };
  public static final int MAX_QUBITS_SYMB = 2;

  private Verifier verifier;
  private String[] gates;
  
  private Expr[] symbAngles;
  private int maxQubits;
  private Random rand;

  private Map<Integer, List<ConstrainedCircuit>> map;
  private Map<Integer, List<ConstrainedCircuit>> symbmap;
  private List<EquivalenceClass> ecs;
  private List<EquivalenceClass> symbecs;
  private EggGen egraph;
  private EggGen egraphSymb;
  private List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> learned_rules;
  private List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> learned_symbolic_rules;
  private HashSet<MatrixConstrainedRule> learned_matrix_constrained;
  public String filename;
  public String fileSymname;
  public String gatesetName;
  private Set<String> canonicalCircuits = new HashSet<>();
  private SymbolicSolve solver;
  private Map<String, List<Circuit>> eigenmap;
  private List<Circuit> symbccs;
  private Set<String> badrules;
  private Set<String> goodRules;
  

  private class RuleWithPriority implements Comparable<RuleWithPriority> {
    SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule;
    int sizeDiff;
    int twoqubitDiff;
    int totalSize;
    boolean isSymbolic;
    int numberSymbs;
    public RuleWithPriority(SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule) {
        this.rule = rule;
        Circuit lhs = rule.getKey().getCircuit();
        Circuit rhs = rule.getValue().getCircuit();
        this.sizeDiff = Math.abs(lhs.getSize() - rhs.getSize());
        //this.twoqubitDiff = Math.abs(lhs.getTwoQubitsCount() - rhs.getTwoQubitGates().size());
        this.totalSize = lhs.getSize() + rhs.getSize();
        this.isSymbolic = lhs.hasSymb() || rhs.hasSymb();
        Set<String> symbols = new HashSet<>();
        CircuitTranslator.translate(lhs).circuit.getAllSymbols(symbols);
        CircuitTranslator.translate(rhs).circuit.getAllSymbols(symbols);
        symbols.remove("pi");
        this.numberSymbs = symbols.size();
    }

    @Override
    public int compareTo(RuleWithPriority other) {
        // 1. total size
        if(this.totalSize != other.totalSize) {
          return this.totalSize - other.totalSize;
        }
        // if(other.twoqubitDiff != this.twoqubitDiff) {
        //   return other.twoqubitDiff - this.twoqubitDiff;
        // }

        // 2. smaller size difference first
        if(this.sizeDiff != other.sizeDiff) {
          return this.sizeDiff - other.sizeDiff;
        }

        return other.numberSymbs - this.numberSymbs;
    }
  }


  private FileWriter fw;
  private FileWriter fw_symb;
  private FileWriter fw_symb_nm;
  private PrintWriter pw;
  private PrintWriter pw_symb;
  private PrintWriter pw_symb_nm;
  private boolean genSymb;

  public EnumeratorPrune(String[] gates, int maxQubits, Random rand, Expr[] symbAngles, boolean genSymb) {
    this.verifier = new Verifier(rand, maxQubits);
    this.gates = gates;
    this.symbAngles = symbAngles;
    this.maxQubits = maxQubits;
    this.rand = rand;
    this.map = new HashMap<>();
    this.ecs = new ArrayList<>();
    this.egraph = new EggGen();
    this.learned_rules = new ArrayList<>();
    this.learned_symbolic_rules = new ArrayList<>();
    this.solver = new SymbolicSolve(rand);
    this.eigenmap = new HashedMap<>();
    this.egraphSymb = new EggGen();
    this.learned_matrix_constrained = new HashSet<MatrixConstrainedRule>();
    this.symbmap = new HashMap<>();
    this.symbecs = new ArrayList<>();
    this.symbccs = new ArrayList<>();
    this.badrules = new TreeSet<>();
    this.genSymb = genSymb;
    this.goodRules = new HashSet<>();
  }

  public EnumeratorPrune(String[] gates, int maxQubits, Random rand, Expr[] symbAngles, String gatesName, boolean genSymb) {
    this.verifier = new Verifier(rand, maxQubits);
    this.gates = gates;
    this.symbAngles = symbAngles;
    this.maxQubits = maxQubits;
    this.rand = rand;
    this.map = new HashMap<>();
    this.ecs = new ArrayList<>();
    this.egraph = new EggGen();
    this.learned_rules = new ArrayList<>();
    this.learned_symbolic_rules = new ArrayList<>();
    this.gatesetName = gatesName;
    this.solver = new SymbolicSolve(rand);
    this.eigenmap = new HashedMap<>();
    this.egraphSymb = new EggGen();
    this.learned_matrix_constrained = new HashSet<MatrixConstrainedRule>();
    this.symbmap = new HashMap<>();
    this.symbecs = new ArrayList<>();
    this.symbccs = new ArrayList<>();
    this.badrules = new TreeSet<>();
    this.genSymb = genSymb;
    this.goodRules = new HashSet<>();
  }


  private List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> canonicalizeSymbEntries(List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> entries) {
    List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> canonicalEntries = new ArrayList<>(entries.size());
    for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> entry : entries) {
      Map<String, String> qubitMap = new HashMap<>();
      EggGen.Circuit lhsCanonical = canonicalizeCircuit(entry.getKey(), qubitMap);
      EggGen.Circuit rhsCanonical = canonicalizeCircuit(entry.getValue(), qubitMap);
      canonicalEntries.add(new SimpleEntry<>(lhsCanonical, rhsCanonical));
    }
    return canonicalEntries;
  }

  private EggGen.Circuit canonicalizeCircuit(EggGen.Circuit circuit, Map<String, String> qubitMap) {
    List<EggGen.Gate> canonicalGates = new ArrayList<>(circuit.gates.size());
    for (EggGen.Gate gate : circuit.gates) {
      canonicalGates.add(canonicalizeGate(gate, qubitMap));
    }
    EggGen.Circuit canonicalEggCircuit = new EggGen.Circuit(canonicalGates);
    return canonicalEggCircuit;
  }

  private EggGen.Gate canonicalizeGate(EggGen.Gate gate, Map<String, String> qubitMap) {
    if (gate instanceof EggGen.X x) {
      return new EggGen.X(canonicalizeQubit(x.qubit, qubitMap));
    }
    if (gate instanceof EggGen.H h) {
      return new EggGen.H(canonicalizeQubit(h.qubit, qubitMap));
    }
    if (gate instanceof EggGen.SX sx) {
      return new EggGen.SX(canonicalizeQubit(sx.qubit, qubitMap));
    }
    if (gate instanceof EggGen.RZ rz) {
      return new EggGen.RZ(canonicalizeQubit(rz.qubit, qubitMap), rz.angle);
    }
    if (gate instanceof EggGen.RX rx) {
      return new EggGen.RX(canonicalizeQubit(rx.qubit, qubitMap), rx.angle);
    }
    if (gate instanceof EggGen.RY ry) {
      return new EggGen.RY(canonicalizeQubit(ry.qubit, qubitMap), ry.angle);
    }
    if (gate instanceof EggGen.U1 u1) {
      return new EggGen.U1(canonicalizeQubit(u1.qubit, qubitMap), u1.lambda);
    }
    if (gate instanceof EggGen.U2 u2) {
      return new EggGen.U2(canonicalizeQubit(u2.qubit, qubitMap), u2.phi, u2.lambda);
    }
    if (gate instanceof EggGen.U3 u3) {
      return new EggGen.U3(canonicalizeQubit(u3.qubit, qubitMap), u3.theta, u3.phi, u3.lambda);
    }
    if (gate instanceof EggGen.GPI gpi) {
      return new EggGen.GPI(canonicalizeQubit(gpi.qubit, qubitMap), gpi.phi);
    }
    if (gate instanceof EggGen.GPI2 gpi2) {
      return new EggGen.GPI2(canonicalizeQubit(gpi2.qubit, qubitMap), gpi2.phi);
    }
    if (gate instanceof EggGen.VZ vz) {
      return new EggGen.VZ(canonicalizeQubit(vz.qubit, qubitMap), vz.theta);
    }
    if (gate instanceof EggGen.CX cx) {
      return new EggGen.CX(canonicalizeQubit(cx.control, qubitMap), canonicalizeQubit(cx.target, qubitMap));
    }
    if (gate instanceof EggGen.CZ cz) {
      return new EggGen.CZ(canonicalizeQubit(cz.control, qubitMap), canonicalizeQubit(cz.target, qubitMap));
    }
    if (gate instanceof EggGen.RXX rxx) {
      return new EggGen.RXX(canonicalizeQubit(rxx.qubit1, qubitMap), canonicalizeQubit(rxx.qubit2, qubitMap), rxx.angle);
    }
    if (gate instanceof EggGen.MS ms) {
      return new EggGen.MS(canonicalizeQubit(ms.qubit1, qubitMap), canonicalizeQubit(ms.qubit2, qubitMap), ms.phi1, ms.phi2);
    }
    if (gate instanceof EggGen.SYMB symb) {
      return new EggGen.SYMB(symb.maxQubits);
    }
    throw new IllegalArgumentException("Unsupported gate type: " + gate.getClass());
  }
  private String canonicalizeQubit(String qubit, Map<String, String> qubitMap) {
    return qubitMap.computeIfAbsent(qubit, q -> "q" + qubitMap.size());
  }

  public void enumerateEqsat(int numQubits, int size, int Symbsize, List<String> commutative) throws IOException {
    this.filename = String.format("rules_%s_q%s_%s.txt", gatesetName, maxQubits, size);
    this.fileSymname = String.format("rules_%s_q%s_%s_symb.txt", gatesetName, maxQubits, size);
    String filesymbnm = String.format("rules_%s_q%s_%s_symb_nm.txt", gatesetName, maxQubits, size);
    for(String rule: commutative) {
        egraph.addRewrite(rule);
    }
    egraph.push();
    fw = new FileWriter(filename, StandardCharsets.UTF_8, false);
    fw_symb = new FileWriter(fileSymname, StandardCharsets.UTF_8, false);
    pw = new PrintWriter(fw);
    pw_symb = new PrintWriter(fw_symb);
    fw_symb_nm = new FileWriter(filesymbnm, StandardCharsets.UTF_8, false);
    pw_symb_nm = new PrintWriter(fw_symb_nm);
    Map<String, Double> symbolMap = getSymbolMap();
    // initialize map and reps with empty circuit
    Circuit emptyCircuit = getStart();
    List<SimpleEntry<Integer, List<Integer>>> emptyCircuitHash = verifier.hashCode(emptyCircuit, symbolMap);
    assert emptyCircuitHash.size() == 1;
    assert Symbsize >= size;

    SimpleEntry<Integer, List<Integer>> emptyCircuitHashEntry = emptyCircuitHash.get(0);
    ArrayList<ConstrainedCircuit> equiv = new ArrayList<>();
    Map<String, ConstrainedCircuit> previousReps = new HashMap<>();
    ConstrainedCircuit emptyCCircuit = new ConstrainedCircuit(emptyCircuit, emptyCircuitHashEntry.getValue());
    EggGen.ConstrainedCircuit eggcc = CircuitTranslator.translate(emptyCCircuit);
    equiv.add(emptyCCircuit);
    map.put(emptyCircuitHashEntry.getKey(), equiv);
    //egraph.setFingerprint(eggcc, emptyCircuitHashEntry.getKey());
    //egraph.addConstrainedCircuit(eggcc);

    previousReps.put(emptyCircuit.getQasmString(), new ConstrainedCircuit(emptyCircuit, new ArrayList<>()));
    for (int i = 1; i <= Symbsize; i++) {
      logger.debug("Enumerating size: " + i);
      long t1 = System.currentTimeMillis();
      long updateMapTime = 0;
      if (i == 1) {
        Circuit start = getStart();
        for (String gate : this.gates) {
          for (int q = 0; q < numQubits; q++) {
            List<Circuit> circuitsAfterApply = applyGate(start, gate, q, 2);
            for (Circuit c : circuitsAfterApply) {
              long tmap = System.currentTimeMillis();
              updateMapEqsat(c, symbolMap, true);
              long tmapafter = System.currentTimeMillis();
              updateMapTime += tmapafter - tmap;
            }
          }
        }
      } else {
        int j = 0;
        for (ConstrainedCircuit c : previousReps.values()) {
          logger.debug(String.format("Enumerating circuit: %d/%d", j, previousReps.size()));
          j++;
          // apply symb
          // if (!c.getCircuit().hasQubitGreaterThan(MAX_QUBITS_SYMB)) {
          //   List<Circuit> circuitsAfterApplySymb = applyGate(c.getCircuit(), "symb", 0, MAX_QUBITS_SYMB);
          //   for (Circuit cSymb : circuitsAfterApplySymb) {
          //     //if (previousReps.containsKey(cSymb.getQasmStringDropFirst())) { // TODO
          //       long tmap = System.currentTimeMillis();
          //       //updateMapEqsat(cSymb, symbolMap);
          //       updateMapSymbEqsat(cSymb, symbolMap);
          //       long tmapafter = System.currentTimeMillis();
          //       updateMapTime += tmapafter - tmap;
          //     //}
          //   }
          // }

          // apply other gates
          for (String gate : this.gates) {
            //if ((gate.equals("cx") || gate.equals("cz")) && c.getCircuit().hasSymb()) { continue; }
            for (int q = 0; q <= Math.min(c.getCircuit().getQubits().size(), numQubits - 1); q++) {
              if (c.getCircuit().hasSymb() && q >= MAX_QUBITS_SYMB) { continue; }
              List<Circuit> circuitsAfterApply = applyGate(c.getCircuit(), gate, q, Math.min(c.getCircuit().getQubits().size() + 1, numQubits));
              for (Circuit caa : circuitsAfterApply) {
                //System.out.println("Candidate:" + caa.getQasmString());
                if (previousReps.containsKey(caa.getQasmStringDropFirst())) { // TODO
                  long tmap = System.currentTimeMillis();
                  if(caa.hasSymb()) {
                    //updateMapSymbEqsat(caa, symbolMap);
                  } else {
                    if(i <= size) {
                      updateMapEqsat(caa, symbolMap, true);
                    } else {
                      updateMapEqsat(caa, symbolMap, false);
                    }
                  }
                  long tmapafter = System.currentTimeMillis();
                  updateMapTime += tmapafter - tmap;
                }
              }
            }
          }
        }
      }
      String sizes = egraph.printSize("CCircuit");
      logger.debug("Enode size:" + sizes);
      long t2 = System.currentTimeMillis();
      this.enumerationTime += (t2 - t1) - updateMapTime;

      previousReps.clear();
      ecs.clear();
      symbecs.clear();

    
      for (Integer hashcode : map.keySet()) {
        // pick smallest
        ConstrainedCircuit smallest = pickSmallest(map.get(hashcode));
        //System.out.println("Equivalence Class Representative of size " + i + ": " + smallest.getCircuit().getQasmString());
        //System.out.println("  Hashcode: " + hashcode);
        ecs.add(new EquivalenceClass(map.get(hashcode), smallest));
        if (smallest.getCircuit().getSize() == i) {
          previousReps.putIfAbsent(smallest.getCircuit().getQasmString(), smallest);
        }
      }

    
        List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> filtered2 = retriveEqfingers(egraph, i);
        // if(filtered2.isEmpty()) {
        //   break;
        // }
        List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> centries = new ArrayList<>();
        for(SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit> entry : filtered2) {
          centries.add(new SimpleEntry(CircuitTranslator.translateBack(entry.getKey(), maxQubits), CircuitTranslator.translateBack(entry.getValue(), maxQubits)));
        }
        try {
          choose_eqs_n(centries, 2, false, commutative);
        } catch (IOException e) {
          e.printStackTrace();
        }

        
        for (SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule : learned_rules) {
          //System.out.println("Add learned:" + CircuitTranslator.translate(rule.getKey()).toEggString() + " | " + CircuitTranslator.translate(rule.getValue()).toEggString());
          EggGen.ConstrainedCircuit c1 = CircuitTranslator.translate(rule.getKey());
          EggGen.ConstrainedCircuit c2 = CircuitTranslator.translate(rule.getValue());
          egraph.addRewriteRule(new SimpleEntry<>(c1, c2), true);
        }
      
    
      //add symb as first op so it can be added onto in future iteration but we don't want this in the hash table
      if (i == 1) {
        Circuit start = getStart();
        List<Circuit> circuitsAfterApplySymb = applyGate(start, "symb", 0, MAX_QUBITS_SYMB);
        previousReps.put(circuitsAfterApplySymb.get(0).getQasmString(), new ConstrainedCircuit(circuitsAfterApplySymb.get(0), new ArrayList<>()));
        symbccs.add(circuitsAfterApplySymb.get(0));
      }
    }

    //gathering rules for symbolic rules, each class are ccs that are same for some permutation matrix
    //should we also collect mononial rules, or we just collect it altogether
    HashMap<String, List<List<Integer>>> constraintMap = new HashMap<>();
    for(EquivalenceClass ec : symbecs) {
      for (ConstrainedCircuit cc : ec.getCircuits()) {
        Circuit r = ec.getRepresentative().getCircuit();
        Circuit other = cc.getCircuit();
        String rule = ec.getRepresentative().getCircuit().getQasmString() + " | " + cc.getCircuit().getQasmString();

        if (!r.getQasmString().equals(other.getQasmString())) {
          if (!hasCommonSubcircuit(r, other)) {
            if (r.hasSymb() && other.hasSymb()) {
              if (cc.getConstraint().equals(ec.getRepresentative().getConstraint())) { // same constraint
                if (verifier.verify(r, other, cc.getConstraint())) {
                  System.out.println("rule accepted:" + rule);
                  System.out.println("constraint:" + Arrays.toString(cc.getConstraint().toArray()));
                  if (constraintMap.containsKey(rule)) {
                    constraintMap.get(rule).add(cc.getConstraint());
                  } else {
                    constraintMap.put(rule, new ArrayList<>(Arrays.asList(cc.getConstraint())));
                  }
                }
              }
            }
          }
        }
      }
    }

    for (String rule : constraintMap.keySet()) {
      pw_symb.println(rule + " | " + constraintStrings(constraintMap.get(rule)));
    }
    pw_symb.close();

    List<String> rules = egraph.getAllRewriteRulesOpt();
    for(String rule : rules) {
      pw.println(rule);
    }
    pw.close();

    // gathering non-monomial rules.
    // first generate an C X C set
    // group by L;S and R;S
    long symbtime = 0;
    if(genSymb) {
      Map<String, List<ConstrainedCircuit>> traceMap = new HashMap<>();
      long time = System.nanoTime();
      // List<Circuit> lforms = new ArrayList<>();
      // List<Circuit> rforms = new ArrayList<>();
      for(EquivalenceClass ec : ecs) {
        ConstrainedCircuit repre = ec.getRepresentative();
        if(repre.getCircuit().getGates().size() < size) {
          if(repre.getCircuit().hasQubitGreaterThan(MAX_QUBITS_SYMB)) {
            continue;
          }
          System.out.println("Processing Circuit for symb candidates:" + repre.getCircuit().getQasmString());
          String trace = solver.getTrace(repre.getCircuit().getGates(), numQubits);
          System.out.println("Trace:" + trace);
          if(traceMap.containsKey(trace)) {
            traceMap.get(trace).add(repre);
          } else {
            List<ConstrainedCircuit> list = new ArrayList<>();
            list.add(repre);
            traceMap.put(trace, list);
          }
          // List<EggGen.Gate> gatesleft = repre.getCircuit().getGates();
          // gatesleft.add(0, new EggGen.SYMB(MAX_QUBITS_SYMB));
          // EggGen.Circuit eggCircuitleft = new EggGen.Circuit(gatesleft);
          // Circuit lcircuit = CircuitTranslator.translateCircuitBack(eggCircuitleft, numQubits);
          // lforms.add(lcircuit);
          // List<EggGen.Gate> gatesright = repre.getCircuit().getGates();
          // gatesright.add(new EggGen.SYMB(MAX_QUBITS_SYMB));
          // EggGen.Circuit eggCircuitright = new EggGen.Circuit(gatesright);
          // Circuit rcircuit = CircuitTranslator.translateCircuitBack(eggCircuitright, numQubits);
          // rforms.add(rcircuit);
        }
      }

      System.out.println("Filtering Symbolic Candidates:" );
      List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> symbenties = new ArrayList<>();
      int traceMapindex = 0;
      for (Map.Entry<String, List<ConstrainedCircuit>> entry : traceMap.entrySet()) {
        System.out.print("Processing Trace Group:" + traceMapindex + "/" + traceMap.size() + " Size:" + entry.getValue().size() + "\n");
        traceMapindex++;
        int z = 0;
        for(int i = 0; i < entry.getValue().size(); i++) {
          for(int j = i+1; j < entry.getValue().size();j++) {
            System.out.println("Filtering Symbolic Candidates:" + z + "/" + entry.getValue().size() * entry.getValue().size());
            z++;
            Circuit cc1 = entry.getValue().get(i).getCircuit();
            Circuit cc2 = entry.getValue().get(j).getCircuit();
            System.out.println(cc1.getQasmString() + "|" + cc2.getQasmString());
            if (!hasCommonSubcircuit(cc1, cc2)) {
              List<EggGen.Gate> gatesLeft = cc1.getGates();
              gatesLeft.add(0, new EggGen.SYMB(MAX_QUBITS_SYMB));
              List<EggGen.Gate> gatesRight = cc2.getGates();
              gatesRight.add(new EggGen.SYMB(MAX_QUBITS_SYMB));
              EggGen.Circuit cce1 = new EggGen.Circuit(gatesLeft);
              EggGen.Circuit cce2 = new EggGen.Circuit(gatesRight);
              if(cce1.getTwoQubitsCount() < cce2.getTwoQubitsCount()) {
                EggGen.Circuit temp = cce1;
                cce1 = cce2;
                cce2 = temp;
              } else if(cce1.getTwoQubitsCount() == cce2.getTwoQubitsCount() && cce1.gates.size() < cce2.gates.size()) {
                EggGen.Circuit temp = cce1;
                cce1 = cce2;
                cce2 = temp;
              }
              Set<String> symbols1 = new HashSet<>();
              Set<String> symbols2 = new HashSet<>();
              cce1.getAllSymbols(symbols1);
              cce2.getAllSymbols(symbols2);
              Set<String> qubitVars = new HashSet<>();
              Set<String> qubitVars2 = new HashSet<>();
              cce1.getQubitVars(qubitVars);
              cce2.getQubitVars(qubitVars2);
              if(symbols1.containsAll(symbols2) && symbols2.containsAll(symbols1) && qubitVars.containsAll(qubitVars2)) {
                if(solver.checkBig(cce1, cce2, maxQubits)) {
                  // if(solver.checkEigen(cce1, cce2, numQubits)) {
                    symbenties.add(new SimpleEntry<>(cce1, cce2));
                    System.out.println("Choosed!");
                  // } else {
                  //   System.out.println("Check failed, eigenvalues");
                  // }
                } else {
                  System.out.println("Check failed, diff traces");
                }
              } else {
                System.out.println("Distinguished Symbols!");
              }
            }
          }
        }
      }
      System.out.println("Symb Candidates Size:" + symbenties.size());
      List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> canonicalSymbEntries = canonicalizeSymbEntries(symbenties);
      infer_symb(canonicalSymbEntries, MAX_QUBITS_SYMB);
      Set<String> added = new HashSet<>();
      for(MatrixConstrainedRule rule: learned_matrix_constrained) {
        String ruleString = rule.toString();
        if(!added.contains(ruleString)) {
          pw_symb_nm.println(ruleString);
          added.add(ruleString);
        }
      }

      long time2 = System.nanoTime();
      symbtime = (time2 - time) / 1000000000;
    }
   
    pw_symb_nm.close();
    egraph.pop();
    Map<String, Long> data = egraph.getProfilingData();
    egraph.stopEgglogREPL();
    logger.info("Concrete Rule sizes: " + rules.size());
    logger.info("Symbolic Rule sizes: " + learned_matrix_constrained.size());
    logger.info("Symbolic Rule generation time (s): " + symbtime);
    //System.out.println("Symbolic Rule sizes: " + added.size());
    logger.info("E-graph time (ms): " + this.egraphTime);
    logger.info("Translation time (ms): " + this.translateTime);
    logger.info("Choose rules time (ms): " + this.chooseTime);
    logger.info("Enumeration time (ms): " + this.enumerationTime);
    logger.info("Filter time (ms): " + this.filtertime);

    logger.info("--------------------------Egraph Break Down-----------------");
    logger.info("Add Circuit Time (ms):" + data.get("addNewCircuitTime") / 1000000);
    logger.info("Esat Time (ms):" + data.get("equalitySaturationTime") / 1000000);
    logger.info("Print function Time (ms):" + data.get("printFunctionTime") / 1000000);
    logger.info("add Rewrit Rule Time (ms):" + data.get("addRewriteRuleTime") / 1000000);
    logger.info("Check equality time (ms): " + data.get("checkEqualityTime") / 1000000);
  }


  public List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> retriveEqfingers(EggGen egg, int size) {
        egg.push();
        egg.clearRules();
        for (SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule : learned_rules) {
          System.out.println("Add learned:" + CircuitTranslator.translate(rule.getKey()).toEggString() + " | " + CircuitTranslator.translate(rule.getValue()).toEggString());
          EggGen.ConstrainedCircuit c1 = CircuitTranslator.translate(rule.getKey());
          EggGen.ConstrainedCircuit c2 = CircuitTranslator.translate(rule.getValue());
          egg.addRewriteRule(new SimpleEntry<>(c1, c2), true);
        }
        List<Rule> rules = egg.processRules(egg.optrules);
        for(Rule r : rules) {
          egg.addOptRule(r, "opt", "birewrite");
        }
        List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> entries = new ArrayList<>();
        pruneECS();
        int i = 0;
        for(EquivalenceClass ec: ecs) {
          System.out.println("Checking eclass:" + i + "/" + ecs.size());
          i++;
          List<ConstrainedCircuit> circuits = ec.getCircuits();
          ConstrainedCircuit repre = ec.getRepresentative();
          // egg.push();
          // egg.addConstrainedCircuit(CircuitTranslator.translate(repre));
          // for(ConstrainedCircuit candidate : circuits) {
          //   if(candidate.getCircuit().getSize() == i) {
          //     egg.addConstrainedCircuit(CircuitTranslator.translate(candidate));
          //   }
          // }
          // //egg.rnunN("opt",20);
          // egg.runN("wire",6);
          // egg.runBackoff("opt", 7);
          // while(size < 5000 && size != lastSize) {
          //       lastSize = size;
          //       egraph.runBackoff("opt", 1);
          //       size = Integer.parseInt(egraph.printSize("Cons"));
          //       //System.out.println("Enode Size: " + size);
          // }
          EggGen.ConstrainedCircuit repreegg = CircuitTranslator.translate(repre);
          for(ConstrainedCircuit candidate : circuits) {
            EggGen.ConstrainedCircuit candidateEgg = CircuitTranslator.translate(candidate);
            String eggString = repreegg.toEggString() + "|" + candidateEgg.toEggString();
            String eggStringRev = candidateEgg.toEggString() + "|" + repreegg.toEggString();
            System.out.println("Checking:" + repre.getCircuit().getQasmString() + "<->" + candidate.getCircuit().getQasmString());
            if(!goodRules.contains(eggString) && !goodRules.contains(eggStringRev)) {
              if(!badrules.contains(eggString) && !badrules.contains(eggStringRev)) {
                if (!repreegg.toEggString().equals(candidateEgg.toEggString())) {
                  egg.push();
                  egg.addConstrainedCircuit(repreegg);
                  egg.addConstrainedCircuit(candidateEgg);
                  egg.runN("wire",5);
                  egg.runBackoff("opt", 5);
                  if(!egg.check(String.format("(= %s %s)", repreegg.circuit.toEggString(), candidateEgg.circuit.toEggString()))){
                    entries.add(new SimpleEntry<>(repreegg, candidateEgg));
                  } else {
                    goodRules.add(eggString);
                    goodRules.add(eggStringRev);
                  }
                  egg.pop();
                }
              }
            }
          }
        }
        egg.pop();

        //eliminate symmetric duplicates
        TreeSet<String> seen = new TreeSet<>();
        List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> filtered2 = new ArrayList<>();
        for(SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit> entry : entries) {
          String fwd = entry.getKey().toEggString() + "|" + entry.getValue().toEggString();
          String bwd = entry.getValue().toEggString() + "|" + entry.getKey().toEggString();
          if(!seen.contains(fwd) && !seen.contains(bwd)) {
            seen.add(fwd);
            seen.add(bwd);
            filtered2.add(entry);
          }
        }
        System.out.println("Current Entry:");
        for(SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit> entry : filtered2) {
          System.out.println(entry.getKey().toEggString() + ", " + entry.getValue().toEggString());
        }
        return filtered2;
  }




  public void infer_symb (List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> entries, int maxQubits) {
    //for each symb rule, compute its canonocal form L; S = S; R, then solve for S.
    int i = 0;
    for(SimpleEntry<EggGen.Circuit, EggGen.Circuit> entry: entries) { 
      System.out.println("Enumerating Symb Candidats:" + i + "/" + entries.size());
      System.out.println(entry.getKey().toEggString() + "<->" + entry.getValue().toEggString());

      i++;
      EggGen.Circuit c1 = entry.getKey();
      EggGen.Circuit c2 = entry.getValue();
      Map<String, String> qubitToVar = new HashMap<>();
      EggGen.Circuit canonical1 = EggGen.canonicalizeCircuit(c1, qubitToVar, true);
      EggGen.Circuit canonical2 = EggGen.canonicalizeCircuit(c2, qubitToVar, true);
      String lhsRule = EggGen.replaceNilWithVar(canonical1, "c");
      String rhsRule = EggGen.replaceNilWithVar(canonical2, "c");
      // String lhsRule = canonical1.toQASM(false);
      // String rhsRule = canonical2.toQASM(false);

      MatrixConstrainedRule rule = new MatrixConstrainedRule(lhsRule, rhsRule, "birewrite");
      if(learned_matrix_constrained.contains(rule)) {
        continue;
      }

      String basis = solver.solveSymb(c1, c2, maxQubits);
      System.out.println(basis);
      List<SymbolicSolve.SparseMatrix> ms = solver.parseBasis(basis);
      if(ms.isEmpty()){
        continue;
      }
      rule.setConstraint(ms);
      learned_matrix_constrained.add(rule);
    }
  }

  
  public List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> choose_eqs_n (List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> entries, int n, boolean symb, List<String> commutative) throws FileNotFoundException, IOException {
    PriorityQueue<RuleWithPriority> pq = new PriorityQueue<>();
    HashMap<String, List<List<Integer>>> constraintMap = new HashMap<>();
    Map<String, Double> symbolMap = getSymbolMap();
    
    int step_size = 10;
    List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> entries_copy = new ArrayList<>(entries);
    List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> learned = new ArrayList<>();
    
    while(!entries_copy.isEmpty()) {
      System.out.println("Entry Size:" + entries_copy.size());
      if(entries_copy.size() < step_size) {
        step_size = Integer.max(1, step_size / 10);
      }
      pq.clear();
      for (int i = 0; i < entries_copy.size(); i++) {
        pq.add(new RuleWithPriority(entries_copy.get(i)));
      }
      int iters = 0;
      while (!pq.isEmpty() && iters < step_size) {
        iters++;
        System.out.println("PQ size: " + pq.size());
        SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> entry = pq.poll().rule;
        entries_copy.remove(entry);
        Circuit r = entry.getKey().getCircuit();
        Circuit other = entry.getValue().getCircuit();
        String rule = r.getQasmString() + " | " + other.getQasmString();
        if (!r.getQasmString().equals(other.getQasmString())) {
          if (!hasCommonSubcircuit(r, other)) {
            if (!r.hasSymb() && !other.hasSymb()) {
              if (verifier.verifyv2(r, other, symbolMap)) {
                learned_rules.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
                if(!symb) {
                  learned.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
                }
                System.out.println("rule accepted:" + rule);
                //pw.println(rule);
              } else {
                System.out.println("rule rejected, verified failed:" + rule);
                badrules.add(CircuitTranslator.translate(r).toEggString() + "|" + CircuitTranslator.translate(other).toEggString());
                badrules.add(CircuitTranslator.translate(other).toEggString() + "|" + CircuitTranslator.translate(r).toEggString());
                // egraph.insertBad(CircuitTranslator.translate(entry.getKey()), CircuitTranslator.translate(entry.getValue()));
                // egraph.insertBad(CircuitTranslator.translate(entry.getValue()), CircuitTranslator.translate(entry.getKey()));
                // egraphSymb.insertBad(CircuitTranslator.translate(entry.getKey()), CircuitTranslator.translate(entry.getValue()));
                // egraphSymb.insertBad(CircuitTranslator.translate(entry.getValue()), CircuitTranslator.translate(entry.getKey()));
              }
            }
          } else {
            System.out.println("rule rejected, has common subcircuit:" + rule);
            badrules.add(CircuitTranslator.translate(r).toEggString() + "|" + CircuitTranslator.translate(other).toEggString());
            badrules.add(CircuitTranslator.translate(other).toEggString() + "|" + CircuitTranslator.translate(r).toEggString());
          }
        } else {
          System.out.println("rule rejected, lhs = rhs" + rule);
        }
      }

      // for (String rule : constraintMap.keySet()) {
      //   pw_symb.println(rule + " | " + constraintStrings(constraintMap.get(rule)));
      // }

      //shrink the entries.
      EggGen egg = new EggGen();
      for(String rule : commutative) {
        egg.addRewrite(rule);
      }
      // Set<String> canonicals = new HashSet<>();
      // for(SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> entry: entries_copy) {
      //   egg.addConstrainedCircuit(CircuitTranslator.translate(entry.getKey()));
      //   System.out.println("Add Term" + CircuitTranslator.translate(entry.getKey()).toEggString());
        
      //   egg.addConstrainedCircuit(CircuitTranslator.translate(entry.getValue()));
      //   System.out.println("Add Term" + CircuitTranslator.translate(entry.getValue()).toEggString());
      // }
      //System.out.println("Added Terms in C");
      if(!symb) {
        for (SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule : learned_rules) {
          egg.addRewriteRule(new SimpleEntry<>(CircuitTranslator.translate(rule.getKey()), CircuitTranslator.translate(rule.getValue())), true);
        }
        List<Rule> rules = egg.processRules(egg.optrules);
        for(Rule r : rules) {
          egg.addOptRule(r, "opt", "birewrite");
        }
      }
      
      List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> new_entries_copy = new ArrayList<>();
      int i = 0;
      int size = entries_copy.size();
      for(SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> entry: entries_copy) {
        System.out.println("Verifying remaining entries:" + i + "/" + size);
        i++;
        EggGen.Circuit lhs = CircuitTranslator.translate(entry.getKey()).circuit;
        EggGen.Circuit rhs = CircuitTranslator.translate(entry.getValue()).circuit;
        egg.push();
        String lhsString = lhs.toEggString();
        String rhsString = rhs.toEggString();
        
        String eggString = lhsString + "|" + rhsString;
        String eggStringRev = rhsString + "|" + lhsString;
        if(!goodRules.contains(eggString) && !goodRules.contains(eggStringRev)) {
          egg.addCircuit(lhs);
          egg.addCircuit(rhs);
          egg.runN("wire",5);
          egg.runBackoff("opt", 5);
          if(!egg.check(String.format("(= %s %s)", lhsString, rhsString))) {
            new_entries_copy.add(entry);
          } else {
            String goodrule1 = eggString;
            String goodrule2 = eggStringRev;
            goodRules.add(goodrule1);
            goodRules.add(goodrule2);
          }
        }
        egg.pop();
      }
      
      System.out.println("new entries computed, size: " + new_entries_copy.size());
      egg.stopEgglogREPL();
      entries_copy = new_entries_copy;
    }

    return learned;
  }

  public void enumerate(int numQubits, int size) {
    Map<String, Double> symbolMap = getSymbolMap();

    // initialize map and reps with empty circuit
    Circuit emptyCircuit = getStart();
    List<SimpleEntry<Integer, List<Integer>>> emptyCircuitHash = verifier.hashCode(emptyCircuit, symbolMap);
    assert emptyCircuitHash.size() == 1;

    SimpleEntry<Integer, List<Integer>> emptyCircuitHashEntry = emptyCircuitHash.get(0);
    ArrayList<ConstrainedCircuit> equiv = new ArrayList<>();
    ConstrainedCircuit emptyCCircuit = new ConstrainedCircuit(emptyCircuit, emptyCircuitHashEntry.getValue());
    equiv.add(emptyCCircuit);
    map.put(emptyCircuitHashEntry.getKey(), equiv);
    Map<String, ConstrainedCircuit> previousReps = new HashMap<>();
    previousReps.put(emptyCircuit.getQasmString(), new ConstrainedCircuit(emptyCircuit, new ArrayList<>()));

    for (int i = 1; i <= size; i++) {
      // enumerate circuit size i
      if (i == 1) {
        Circuit start = getStart();
        for (String gate : this.gates) {
          for (int q = 0; q < numQubits; q++) {
            List<Circuit> circuitsAfterApply = applyGate(start, gate, q, 2);
            for (Circuit c : circuitsAfterApply) {
              updateMap(c, symbolMap);
            }
          }
        }
      } else {
        for (ConstrainedCircuit c : previousReps.values()) {
          // apply symb
          if (!c.getCircuit().hasQubitGreaterThan(MAX_QUBITS_SYMB) && !c.getCircuit().hasCXH()) {
            List<Circuit> circuitsAfterApplySymb = applyGate(c.getCircuit(), "symb", 0, MAX_QUBITS_SYMB);
            for (Circuit cSymb : circuitsAfterApplySymb) {
              if (previousReps.containsKey(cSymb.getQasmStringDropFirst())) { // TODO
                updateMap(cSymb, symbolMap);
              }
            }
          }
          // apply other gates
          for (String gate : this.gates) {
            if ((gate.equals("cx") || gate.equals("cz")) && c.getCircuit().hasSymb()) { continue; }
            for (int q = 0; q <= Math.min(c.getCircuit().getQubits().size(), numQubits - 1); q++) {
              if (c.getCircuit().hasSymb() && q >= MAX_QUBITS_SYMB) { continue; }
              List<Circuit> circuitsAfterApply = applyGate(c.getCircuit(), gate, q, Math.min(c.getCircuit().getQubits().size() + 1, numQubits));
              for (Circuit caa : circuitsAfterApply) {
                if (previousReps.containsKey(caa.getQasmStringDropFirst())) { // TODO
                  updateMap(caa, symbolMap);
                }
              }
            }
          }
        }
      }

      // recompute ecs
      // simplified version compared to quartz
      ecs.clear();
      previousReps.clear();
      for (List<ConstrainedCircuit> possibleEC : map.values()) {
        // pick smallest
        ConstrainedCircuit smallest = pickSmallest(possibleEC);
        ecs.add(new EquivalenceClass(possibleEC, smallest));

        if (smallest.getCircuit().getSize() == i) {
          previousReps.putIfAbsent(smallest.getCircuit().getQasmString(), smallest);
        }
      }

      // add symb as first op so it can be added onto in future iteration but we don't want this in the hash table
      if (i == 1) {
        Circuit start = getStart();
        List<Circuit> circuitsAfterApplySymb = applyGate(start, "symb", 0, MAX_QUBITS_SYMB);
        previousReps.put(circuitsAfterApplySymb.get(0).getQasmString(), new ConstrainedCircuit(circuitsAfterApplySymb.get(0), new ArrayList<>()));
      }
    }
  }

  public void pruneECS() {
    List<EquivalenceClass> newECS = new ArrayList<>();
    for (EquivalenceClass ec : ecs) {
      if (ec.size() > 1) {
        newECS.add(ec);
      }
    }
    ecs = newECS;
  }

  private boolean hasCommonSubcircuit(Circuit c1, Circuit c2) {
    return c1.getSize() != 0 && c2.getSize() != 0 && (c1.getQasm().get(0).equals(c2.getQasm().get(0)) || c1.getQasm().get(c1.getSize() - 1).equals(c2.getQasm().get(c2.getSize() - 1)));
  }

  public void gatherRules(String filename) throws FileNotFoundException, UnsupportedEncodingException {
    PrintWriter writer = new PrintWriter(String.format("%s.txt", filename), "UTF-8");
    PrintWriter writerSymb = new PrintWriter(String.format("%s_symb.txt", filename), "UTF-8");
    
    int makeCircuitSmaller = 0;
    int haveSymb = 0;
    int total = 0;
    HashMap<String, List<List<Integer>>> constraintMap = new HashMap<>();
    for (EquivalenceClass ec : ecs) {
      for (ConstrainedCircuit cc : ec.getCircuits()) {
        Circuit r = ec.getRepresentative().getCircuit();
        Circuit other = cc.getCircuit();
        String rule = ec.getRepresentative().getCircuit().getQasmString() + " | " + cc.getCircuit().getQasmString();

        if (!r.getQasmString().equals(other.getQasmString())) {
          if (!hasCommonSubcircuit(r, other)) {
            if (r.hasSymb() && other.hasSymb()) {
              if (cc.getConstraint().equals(ec.getRepresentative().getConstraint())) { // same constraint
                if (verifier.verify(r, other, cc.getConstraint())) {
                  if (constraintMap.containsKey(rule)) {
                    constraintMap.get(rule).add(cc.getConstraint());
                  } else {
                    total++;
                    haveSymb++;
                    if (r.getSize() < other.getSize()) {
                      makeCircuitSmaller++;
                    }
                    constraintMap.put(rule, new ArrayList<>(Arrays.asList(cc.getConstraint())));
                  }
                }
              }
            } else if (!r.hasSymb() && !other.hasSymb()) {
              if (verifier.verify(r, other, new ArrayList<>())) {
                total++;
                if (r.getSize() < other.getSize()) {
                  makeCircuitSmaller++;
                }
                writer.println(rule);
              }
            }
          }
        }
      }
    }

    for (String rule : constraintMap.keySet()) {
      writerSymb.println(rule + " | " + constraintStrings(constraintMap.get(rule)));
    }

    writer.close();
    writerSymb.close();

//    System.out.println(String.format("total: %s, make circuit smaller: %s, have symb: %s", total, makeCircuitSmaller, haveSymb));
  }

  

  private void updateMapEqsat(Circuit c, Map<String, Double> symbolMap, boolean addEgraph) {
    // String canonicalString = circuitToAlphaEquivalentString(c);
    // System.out.println("Add Term:" + canonicalString);
    // if (canonicalCircuits.contains(canonicalString)) {
    //     return;
    // }
    // canonicalCircuits.add(canonicalString);
    //System.out.println("Processing Circuit for egraph:" + c.getQasmString());
    long t1 = System.currentTimeMillis();
    List<SimpleEntry<Integer, List<Integer>>> hash = verifier.hashCode(c, symbolMap);
    long t2 = System.currentTimeMillis();
    this.enumerationTime += (t2 - t1);
    for (SimpleEntry<Integer, List<Integer>> entry : hash) {
      ConstrainedCircuit cc = new ConstrainedCircuit(c, entry.getValue());
      EggGen.ConstrainedCircuit eggcc = CircuitTranslator.translate(cc);
      // System.out.println("Adding to egraph: " + c.getQasmString() + " with fingerprint " + entry.getKey());
      if(addEgraph) {
        //egraph.addConstrainedCircuit(eggcc);
        //egraph.setFingerprint(eggcc, entry.getKey());
      }
      if (c.getQasmString().equals("rz(pi/2.0) q[0]; sx q[0]; rz(pi/2.0) q[0];")) {
        System.out.println("Circuit added to egraph with fingerprint " + entry.getKey());
      }
      if (c.getQasmString().equals("sx q[0]; rz(pi/2.0) q[0]; sx q[0];")) {
        System.out.println("Circuit added to egraph with fingerprint " + entry.getKey());
      }
      if (map.containsKey(entry.getKey())) {
        map.get(entry.getKey()).add(cc);
      } else {
        map.put(entry.getKey(), new ArrayList<>(Arrays.asList(cc)));
      }
    }
    long t3 = System.currentTimeMillis();
    this.egraphTime += (t3 - t2);
  }


  private void updateMapSymbEqsat(Circuit c, Map<String, Double> symbolMap) {
    long t1 = System.currentTimeMillis();
    List<SimpleEntry<Integer, List<Integer>>> hash = verifier.hashCode(c, symbolMap);
    long t2 = System.currentTimeMillis();
    this.enumerationTime += (t2 - t1);
    for (SimpleEntry<Integer, List<Integer>> entry : hash) {
      ConstrainedCircuit cc = new ConstrainedCircuit(c, entry.getValue());
      // EggGen.ConstrainedCircuit eggcc = CircuitTranslator.translate(cc);
      // System.out.println("Adding to egraph: " + eggcc.toEggString() + " with fingerprint " + entry.getKey());
      // egraphSymb.addConstrainedCircuit(eggcc);
      // egraphSymb.setFingerprint(eggcc, entry.getKey());
      if (map.containsKey(entry.getKey())) {
        symbmap.get(entry.getKey()).add(cc);
      } else {
        symbmap.put(entry.getKey(), new ArrayList<>(Arrays.asList(cc)));
      }
    }
    symbccs.add(c);
    System.out.println("Added Symb Circuit: " + c.getQasmString());
    // long t3 = System.currentTimeMillis();
    // this.egraphTime += (t3 - t2);
    //calculate L or R.

    // String eigens = solver.getEigenvalues(solver.circuitToJson(c.getGates(), c.getQubits().size()));
    // if(eigenmap.containsKey(eigens)){
    //   eigenmap.get(eigens).add(c);
    // } else {
    //   eigenmap.put(eigens, new ArrayList<>(Arrays.asList(c)));
    // }
  }

  private void updateMap(Circuit c, Map<String, Double> symbolMap) {
    List<SimpleEntry<Integer, List<Integer>>> hash = verifier.hashCode(c, symbolMap);
    for (SimpleEntry<Integer, List<Integer>> entry : hash) {
      ConstrainedCircuit cc = new ConstrainedCircuit(c, entry.getValue());
      //System.out.println("Adding to eclass: " + cc.getCircuit().getQasmString() + " with fingerprint " + entry.getKey());
      if (map.containsKey(entry.getKey())) {
        map.get(entry.getKey()).add(cc);
      } else {
        map.put(entry.getKey(), new ArrayList<>(Arrays.asList(cc)));
      }
    }
  }

  private ConstrainedCircuit pickSmallest(List<ConstrainedCircuit> possibleEC) {
    ConstrainedCircuit smallest = null;
    int smallestSize = Integer.MAX_VALUE;
    for (ConstrainedCircuit cc : possibleEC) {
      if (cc.getCircuit().getSize() < smallestSize) {
        smallestSize = cc.getCircuit().getSize();
        smallest = cc;
      } else if (cc.getCircuit().getSize() == smallestSize) {
        if (smallest.getCircuit().getQasmString().compareTo(cc.getCircuit().getQasmString()) > 0) {
          smallest = cc;
        }
      }
    }
    return smallest;
  }

  public Map<String, Double> getSymbolMap() {
    HashMap<String, Double> symbolMap = new HashMap<>();
    symbolMap.put(Symbolic.S_PHI, rand.nextDouble());
    for (String angle : ANGLES) {
      symbolMap.put(angle, rand.nextDouble());
    }

    return symbolMap;
  }

  private Circuit getStart() {
    ArrayList<String> qubits = new ArrayList<>();
    TreeMap<String, Expr> f = new TreeMap<>();

    for (int i = 0; i < maxQubits; i++) {
      String name = getName(i);
      qubits.add(name);
      f.put(name, new Var(name));
    }

    Symbolic s = new Symbolic(new Real(1), f);
    ArrayList<Symbolic> pathSum = new ArrayList<>(Arrays.asList(s));

    return new Circuit(qubits, pathSum, new ArrayList<>(), new ArrayList<>());
  }

  private String getName(int qubit) {
    return String.format("q%s", qubit);
  }

  public void applyGate(Runnable r) {
    r.run();
  }
  interface Command {
    default void runOneQubit(Circuit c, String q){};
    default void runTwoQubit(Circuit c, String q1, String q2){};
    default void runOneQubitOneParam(Circuit c, String q, Expr angle){};
    default void runOneQubitTwoParam(Circuit c, String q, Expr angle1, Expr angle2){};
    default void runOneQubitThreeParam(Circuit c, String q, Expr angle1, Expr angle2, Expr angle3){};
    default void runSymb(Circuit circ, int numQubits){};
    default void runTwoQubitOneParam(Circuit c, String q1, String q2, Expr angle){};
    default void runTwoQubitTwoParam(Circuit c, String q1, String q2, Expr angle1, Expr angle2){};
  }

  private boolean containsAngle(Circuit c, Expr angle) {
    return c.getQasmString().contains("("+angle+")") ||
            c.getQasmString().contains("("+angle+",") ||
            c.getQasmString().contains(","+angle+",") ||
            c.getQasmString().contains(","+angle+")");
  }

  private List<Circuit> applyGate(Circuit c, String gate, int qubit, int numQubits) {
    String name = getName(qubit);
    ArrayList<Circuit> circuitsAfterApply = new ArrayList<>();

    Map<String, Command> map = new HashMap<>();
    map.put("x", new Command() {
      @Override
      public void runOneQubit(Circuit circ, String q) { Symbolic.x(circ, q); };
    });
    map.put("h", new Command() {
      @Override
      public void runOneQubit(Circuit circ, String q) { Symbolic.h(circ, q); };
    });
    map.put("rz", new Command() {
      @Override
      public void runOneQubitOneParam(Circuit circ, String q, Expr angle) { Symbolic.rz(circ, q, angle); };
    });
    map.put("cx", new Command() {
      @Override
      public void runTwoQubit(Circuit circ, String q1, String q2) { Symbolic.cx(circ, q1, q2); };
    });
    map.put("symb", new Command() {
      @Override
      public void runSymb(Circuit circ, int numQubits) { Symbolic.symb(circ, numQubits); };
    });
    map.put("u1", new Command() {
      @Override
      public void runOneQubitOneParam(Circuit circ, String q, Expr angle) { Symbolic.u1(circ, q, angle); };
    });
    map.put("u2", new Command() {
      @Override
      public void runOneQubitTwoParam(Circuit circ, String q, Expr angle1, Expr angle2) { Symbolic.u2(circ, q, angle1, angle2); };
    });
    map.put("u3", new Command() {
      @Override
      public void runOneQubitThreeParam(Circuit circ, String q, Expr angle1, Expr angle2, Expr angle3) { Symbolic.u3(circ, q, angle1, angle2, angle3); };
    });
    map.put("cz", new Command() {
      @Override
      public void runTwoQubit(Circuit circ, String q1, String q2) { Symbolic.cz(circ, q1, q2); };
    });
    map.put("rx", new Command() {
      @Override
      public void runOneQubitOneParam(Circuit circ, String q, Expr angle) { Symbolic.rx(circ, q, angle); };
    });
    map.put("ry", new Command() {
      @Override
      public void runOneQubitOneParam(Circuit circ, String q, Expr angle) { Symbolic.ry(circ, q, angle); };
    });
    map.put("rxx", new Command() {
      @Override
      public void runTwoQubitOneParam(Circuit circ, String q1, String q2, Expr angle) { Symbolic.rxx(circ, q1, q2, angle); };
    });
    map.put("gpi", new Command() {
      @Override
      public void runOneQubitOneParam(Circuit circ, String q, Expr angle) { Symbolic.gpi(circ, q, angle); };
    });
    map.put("gpi2", new Command() {
      @Override
      public void runOneQubitOneParam(Circuit circ, String q, Expr angle) { Symbolic.gpi2(circ, q, angle); };
    });
    map.put("vz", new Command() {
      @Override
      public void runOneQubitOneParam(Circuit circ, String q, Expr angle) { Symbolic.vz(circ, q, angle); };
    });
    map.put("ms", new Command() {
      @Override
      public void runTwoQubitTwoParam(Circuit circ, String q1, String q2, Expr angle1, Expr angle2) { Symbolic.ms(circ, q1, q2, angle1, angle2); };
    });
    map.put("sx", new Command() {
      @Override
      public void runOneQubit(Circuit circ, String q1) { Symbolic.sx(circ, q1); };
    });

    switch (gate) {
      case "x":
      case "sx":
      case "h": {
        Circuit deepCopy = copy(c);
        map.get(gate).runOneQubit(deepCopy, name);
        circuitsAfterApply.add(deepCopy);
        return circuitsAfterApply;
      }
      case "rz":
      case "rx":
      case "ry":
      case "u1":
      case "gpi":
      case "gpi2":
      case "vz": {
        for (Expr angle : symbAngles) {
          if (gate.equals("u1")) {
            if (containsAngle(c, angle)) {
              continue;
            }
          }
//          if (gate.equals("rx") || gate.equals("ry")) {
//            if (!angle.toString().contains("theta")) {
//              if (c.getUsedQubits().size() > 1) {
//                continue;
//              }
//            }
//          }
          Circuit deepCopy = copy(c);
          map.get(gate).runOneQubitOneParam(deepCopy, name, angle);
          circuitsAfterApply.add(deepCopy);
        }
        return circuitsAfterApply;
      }
      case "rx1": {
        Circuit deepCopy = copy(c);
        map.get("rx").runOneQubitOneParam(deepCopy, name, new BinOp(Op.DIV, new Symbol("pi"), new Real(2)));
        circuitsAfterApply.add(deepCopy);
        return circuitsAfterApply;
      }
      case "rx2": {
        Circuit deepCopy = copy(c);
//        map.get("rx").runOneQubitOneParam(deepCopy, name, new UnOp(Op.MINUS, new BinOp(Op.DIV, new Symbol("pi"), new Real(2))));
        map.get("rx").runOneQubitOneParam(deepCopy, name, new BinOp(Op.DIV, new BinOp(Op.MULT, new Real(3), new Symbol("pi")), new Real(2)));
        circuitsAfterApply.add(deepCopy);
        return circuitsAfterApply;
      }
      case "rx3": {
        Circuit deepCopy = copy(c);
        map.get("rx").runOneQubitOneParam(deepCopy, name, new Symbol("pi"));
        circuitsAfterApply.add(deepCopy);
        return circuitsAfterApply;
      }
      case "u2": {
        for (Expr angle : symbAngles) {
          if (containsAngle(c, angle)) {
            continue;
          }
          for (Expr angle2 : symbAngles) {
            if (containsAngle(c, angle2) || angle.toString().equals(angle2.toString())) {
              continue;
            }
            Circuit deepCopy = copy(c);
            map.get(gate).runOneQubitTwoParam(deepCopy, name, angle, angle2);
            circuitsAfterApply.add(deepCopy);
          }
        }
        return circuitsAfterApply;
      }
      case "u3": {
        for (Expr angle : symbAngles) {
          if (containsAngle(c, angle)) {
            continue;
          }
          for (Expr angle2 : symbAngles) {
            if (containsAngle(c, angle2) || angle.toString().equals(angle2.toString())) {
              continue;
            }
            for (Expr angle3 : symbAngles) {
              if (containsAngle(c, angle3) || angle.toString().equals(angle3.toString()) || angle3.toString().equals(angle2.toString())) {
                continue;
              }
              Circuit deepCopy = copy(c);
              map.get(gate).runOneQubitThreeParam(deepCopy, name, angle, angle2, angle3);
              circuitsAfterApply.add(deepCopy);
            }
          }
        }
        return circuitsAfterApply;
      }
      case "cx":
      case "cz": {
        for (int i = 0; i < numQubits; i++) {
          if (c.hasSymb()) {
            if (i >= MAX_QUBITS_SYMB) { continue; }
          }
          Circuit deepCopy = copy(c);
          String targetName = getName(i);

          if (qubit != i) {
            map.get(gate).runTwoQubit(deepCopy, name, targetName);
            circuitsAfterApply.add(deepCopy);
          }
        }
        return circuitsAfterApply;
      }
      case "rxx": {
        for (int i = 0; i < numQubits; i++) {
          if (c.hasSymb()) {
            if (i >= MAX_QUBITS_SYMB) { continue; }
          }
          String targetName = getName(i);

          if (qubit != i) {
            for (Expr angle : symbAngles) {
              if (containsAngle(c, angle)) {
                continue;
              }
//              if (!angle.toString().contains("theta")) {
//                if (c.getUsedQubits().size() > 1) {
//                  continue;
//                }
//              }
              Circuit deepCopy = copy(c);
              map.get(gate).runTwoQubitOneParam(deepCopy, name, targetName, angle);
              circuitsAfterApply.add(deepCopy);
            }
          }
        }
        return circuitsAfterApply;
      }
      case "ms": {
        for (int i = 0; i < numQubits; i++) {
          if (c.hasSymb()) {
            if (i >= MAX_QUBITS_SYMB) { continue; }
          }
          String targetName = getName(i);

          if (qubit != i) {
            for (Expr angle : symbAngles) {
              if (containsAngle(c, angle)) {
                continue;
              }
              for (Expr angle2 : symbAngles) {
                if (containsAngle(c, angle2) || angle.toString().equals(angle2.toString())) {
                  continue;
                }
                Circuit deepCopy = copy(c);
                map.get(gate).runTwoQubitTwoParam(deepCopy, name, targetName, angle, angle2);
                circuitsAfterApply.add(deepCopy);
              }
            }
          }
        }
        return circuitsAfterApply;
      }
      case "symb": {
        if (!c.hasSymb()) {
          Circuit deepCopy = copy(c);
          map.get(gate).runSymb(deepCopy, numQubits);
          circuitsAfterApply.add(deepCopy);
        }
        return circuitsAfterApply;
      }
      default: throw new RuntimeException("unimplemented gate");
    }
  }

  private Circuit copy(Circuit c) {
    ArrayList<Symbolic> copyPathSum = new ArrayList<>();
    for (Symbolic s : c.getPathSum()) {
      TreeMap<String,Expr> copyF = new TreeMap<>();
      copyF.putAll(s.getF());
      Symbolic copyS = new Symbolic(s.getPhi(), copyF);
      copyPathSum.add(copyS);
    }
    Circuit copied = new Circuit(new ArrayList<>(c.getQubits()), copyPathSum, new ArrayList<>(c.getQasm()), new ArrayList<>(c.getGates()));
    copied.setUsedQubits(new TreeSet<>(c.getUsedQubits()));
    return copied;
  }

  private List<String> constraintStrings(List<List<Integer>> constraints) {
    ArrayList<String> strings = new ArrayList<>();
    for (List<Integer> perm : constraints) {
      int numQubits = getExponent(perm.size());
      boolean[][] terms = verifier.getTermsMap().get(numQubits);
      HashMap<String, String> constraint = new HashMap<>();
      for (int i = 0; i < terms.length; i++) {
        constraint.put(Arrays.toString(terms[i]),  Arrays.toString(terms[perm.get(i)]));
      }
      strings.add(constraint.toString());
    }

    return strings;
  }

  private int getExponent(int powerOfTwo) {
    int count = 0;
    while (powerOfTwo != 1) {
      powerOfTwo = powerOfTwo / 2;
      count++;
    }
    return count;
  }

  public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, IOException {
//    String[] gates = {"x", "h", "rz", "cx"};
//    String[] gates = {"u1", "u2", "u3", "cx"};
//    String[] gates = {"rx1", "rx2", "rx3", "rz", "cz"};
//    String[] gates = {"gpi", "gpi2", "vz", "ms"};
//    String[] gates = {"rx", "ry", "rz", "rxx"};
//    String[] gates = {"cx", "rz", "x", "sx"};
    Options options = new Options();
    
    Option gatesetO = new Option("g", "gateSet", true, "gate set (options: nam, ibm, rigetti, ion)");
    gatesetO.setRequired(true);
    options.addOption(gatesetO);

    Option maxQubitsO = new Option("q", "maxQubits", true, "max qubits");
    maxQubitsO.setRequired(true);
    options.addOption(maxQubitsO);

    Option maxSizeO = new Option("s", "maxSize", true, "max size (number of gates)");
    maxSizeO.setRequired(true);
    options.addOption(maxSizeO);

    Option gensSymb = new Option("symb", "gensymb", true, "Generate Symb or not");
    gensSymb.setRequired(true);
    options.addOption(gensSymb);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    try {
      CommandLine cmd = parser.parse(options, args);
      String gateset = cmd.getOptionValue("gateSet");
      Integer maxQubits = Integer.parseInt(cmd.getOptionValue("maxQubits"));
      Integer maxSize = Integer.parseInt(cmd.getOptionValue("maxSize"));
      boolean genSymb = Boolean.parseBoolean(cmd.getOptionValue("gensymb"));
      Random rand = new Random();
      EnumeratorPrune enumerator = null;
      switch (gateset) {
        case "nam": {
          String[] gates = {"x", "h", "rz", "cx"};
          Expr[] symbAngles = {
                  new Symbol("theta1"),
                  new Symbol("theta2"),
                  new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2"))
          };
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset, genSymb);
          break;
        }
        case "ibm": {
          String[] gates = {"u1", "u2", "u3", "cx"};
          Expr[] symbAngles = {
                  new Symbol("theta1"),
                  new Symbol("theta2"),
                  new Symbol("theta3"),
                  new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2")),
                  new BinOp(Op.PLUS, new Symbol("theta1"), new BinOp(Op.PLUS, new Symbol("theta2"), new Symbol("theta3")))
          };
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset, genSymb);
          break;
        }
        case "rigetti": {
          String[] gates = {"rx1", "rx2", "rx3", "rz", "cz"};
          Expr[] symbAngles = {
                  new Symbol("theta1"),
                  new Symbol("theta2"),
                  new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2")),
                  new BinOp(Op.SUBTRACT, new BinOp(Op.MULT, new Real(4), new Symbol("pi")), new Symbol("theta1"))
          };
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset, genSymb);
          break;
        }
        case "ion": {
          String[] gates = {"rx", "ry", "rz", "rxx"};
          Expr[] symbAngles = {
                  new Symbol("theta1"),
                  new Symbol("theta2"),
                  new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2")),
                  new BinOp(Op.SUBTRACT, new BinOp(Op.MULT, new Real(4), new Symbol("pi")), new Symbol("theta1")),
                  new Symbol("pi"),
                  new BinOp(Op.DIV, new Symbol("pi"), new Real(2))
          };
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset, genSymb);
          break;
        }
        case "ibmnew": {
                    String[] gates = {"cx", "rz", "x", "sx"};
                    Expr[] symbAngles = {
                            new Symbol("theta1"),
                            new Symbol("theta2"),
                            new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2")),
                            //new BinOp(Op.DIV, new Symbol("pi"), new Real(2))
                    };
            enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset, genSymb);
            break;
        }
        default: throw new RuntimeException("unreachable");
      }

      long time1 = System.currentTimeMillis();
      List<String> commutative = new ArrayList<>();
     
      FileReader fr = new FileReader("rules_" + gateset + ".txt", StandardCharsets.UTF_8);
      try (BufferedReader br = new BufferedReader(fr)) {
          String line;
          while ((line = br.readLine()) != null) {
                commutative.add(line);
          }
      }
      //enumerator.enumerate(maxQubits, maxSize);
      try{
        enumerator.enumerateEqsat(maxQubits, maxSize, maxSize, commutative);
      } catch (IOException e) {
        e.printStackTrace();
      }
      //enumerator.pruneECS();
      long time2 = System.currentTimeMillis();
//      System.out.println("enumerate time (s): " + ((time2-time1)/1000));
      //enumerator.gatherRules(String.format("rules_q%s_s%s_%s", maxQubits, maxSize, gateset));
      long time3 = System.currentTimeMillis();
      System.out.println(String.format("%s q%s s%s total time (s): %s", gateset, maxQubits, maxSize, ((time3-time1)/1000)));
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("QUESO Rule Synthesizer", options);

      System.exit(1);
    }
  }
}
