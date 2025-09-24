import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.PrintWriter;
import java.util.Random;
import java.util.AbstractMap.SimpleEntry;

import ast.BinOp;
import ast.Expr;
import ast.Expr.Op;
import ast.Real;
import ast.Symbol;
import ast.UnOp;
import ast.Var;
import org.apache.commons.cli.*;

public class EnumeratorPrune {
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
  private static final int MAX_QUBITS_SYMB = 2;

  private Verifier verifier;
  private String[] gates;
  
  private Expr[] symbAngles;
  private int maxQubits;
  private Random rand;

  private Map<Integer, List<ConstrainedCircuit>> map;
  private List<EquivalenceClass> ecs;
  private EggGen egraph;
  private List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> learned_rules;
  private List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> learned_symbolic_rules;
  public String filename;
  public String fileSymname;
  public String gatesetName;
  public EnumeratorPrune(String[] gates, int maxQubits, Random rand, Expr[] symbAngles) {
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
  }

  public EnumeratorPrune(String[] gates, int maxQubits, Random rand, Expr[] symbAngles, String gatesName) {
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
  }

  public void enumerateEqsat(int numQubits, int size) {
    this.filename = String.format("rules_%s_q%s_%s.txt", gatesetName, maxQubits, size);
    this.fileSymname = String.format("rules_%s_q%s_%s_symb.txt", gatesetName, maxQubits, size);
    Map<String, Double> symbolMap = getSymbolMap();
    // initialize map and reps with empty circuit
    Circuit emptyCircuit = getStart();
    List<SimpleEntry<Integer, List<Integer>>> emptyCircuitHash = verifier.hashCode(emptyCircuit, symbolMap);
    assert emptyCircuitHash.size() == 1;

    SimpleEntry<Integer, List<Integer>> emptyCircuitHashEntry = emptyCircuitHash.get(0);
    ArrayList<ConstrainedCircuit> equiv = new ArrayList<>();
    Map<String, ConstrainedCircuit> previousReps = new HashMap<>();
    ConstrainedCircuit emptyCCircuit = new ConstrainedCircuit(emptyCircuit, emptyCircuitHashEntry.getValue());
    egraph.addConstrainedCircuit(CircuitTranslator.translate(emptyCCircuit));
    previousReps.put(emptyCircuit.getQasmString(), new ConstrainedCircuit(emptyCircuit, new ArrayList<>()));

    for (int i = 1; i <= size; i++) {
      if (i == 1) {
        Circuit start = getStart();
        for (String gate : this.gates) {
          for (int q = 0; q < numQubits; q++) {
            List<Circuit> circuitsAfterApply = applyGate(start, gate, q, 2);
            for (Circuit c : circuitsAfterApply) {
              updateMapEqsat(c, symbolMap);
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
                updateMapEqsat(cSymb, symbolMap);
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
                  updateMapEqsat(caa, symbolMap);
                }
              }
            }
          }
        }
      }

      previousReps.clear();
      ecs.clear();

      while (true) {
        for (SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule : learned_rules) {
          egraph.addRewriteRule(new SimpleEntry<>(CircuitTranslator.translate(rule.getKey()), CircuitTranslator.translate(rule.getValue())));
        }
        for (SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule : learned_symbolic_rules) {
          egraph.addRewriteRule(new SimpleEntry<>(CircuitTranslator.translate(rule.getKey()), CircuitTranslator.translate(rule.getValue())));
        }
        egraph.runSaturation();
        egraph.runSaturation("sizeanalysis");
        //egraph.mergeFingerPrintsEQ();
        // get the set of terms X terms that are not devived by R.
        egraph.runSaturation("noteqfinger");
        String rel = egraph.printFunctionCSV("notSameButEqfinger");

        List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> entries = egraph.parseRelation(rel);
        if(entries.size() == 0) {
          break;
        }
        List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> centries = new ArrayList<>();
        for(SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit> entry : entries) {
          centries.add(new SimpleEntry(CircuitTranslator.translateBack(entry.getKey(), maxQubits), CircuitTranslator.translateBack(entry.getValue(), maxQubits)));
        }
        try {
        choose_eqs_n(centries, 2);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      String nodes = egraph.printFunctionCSV("CCircuit");
      Map<String, List<EggGen.ConstrainedCircuit>> eccs = egraph.parseEnodes(nodes);
      for(String rep : eccs.keySet()) {
        List<EggGen.ConstrainedCircuit> ecircuits = eccs.get(rep);
        //   List<ConstrainedCircuit> enodes = new ArrayList<>();
        //   //note that it is expected that only have one element because it only return a representative for each eclass
        //   ConstrainedCircuit repre = CircuitTranslator.translateBack(egraph.parseConstrainedCircuit(rep));
        //   for(EggGen.ConstrainedCircuit ecircuit : ecircuits) {
        //     ConstrainedCircuit eqc = CircuitTranslator.translateBack(ecircuit);
        //     enodes.add(eqc);
        //   }
        //   EquivalenceClass eclass = new EquivalenceClass(enodes, repre);
        //   ecs.add(eclass);
        ConstrainedCircuit repre = CircuitTranslator.translateBack(EggAstBuilder.parse(rep), maxQubits);
        if(repre.getCircuit().getSize() == i) {
          previousReps.putIfAbsent(repre.getCircuit().getQasmString(), repre);
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


  public void choose_eqs_n (List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> entries, int n) throws FileNotFoundException, IOException {
    HashMap<String, List<List<Integer>>> constraintMap = new HashMap<>();
    FileWriter fw = new FileWriter(filename, true);
    FileWriter fw_symb = new FileWriter(fileSymname, true);
    PrintWriter pw = new PrintWriter(fw);
    PrintWriter pw_symb = new PrintWriter(fw_symb);
    for(SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> entry: entries) {
      Circuit r = entry.getKey().getCircuit();
      Circuit other = entry.getValue().getCircuit();
      String rule = r.getQasmString() + " | " + other.getQasmString();
      System.out.println("rule:" + rule);
      if (!r.getQasmString().equals(other.getQasmString())) {
        if (!hasCommonSubcircuit(r, other)) {
          if (r.hasSymb() && other.hasSymb()) {
            if (entry.getKey().getConstraint().equals(entry.getValue().getConstraint())) { // same constraint
              if (verifier.verify(r, other, entry.getKey().getConstraint())) {
                learned_symbolic_rules.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
                if (constraintMap.containsKey(rule)) {
                  constraintMap.get(rule).add(entry.getKey().getConstraint());
                } else {
                  constraintMap.put(rule, new ArrayList<>(Arrays.asList(entry.getKey().getConstraint())));
                }
              }
            }
          } else if (!r.hasSymb() && !other.hasSymb()) {
            if (verifier.verify(r, other, new ArrayList<>())) {
              learned_rules.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
              pw.println(rule);
            }
          }
        }
      }
    }

    for (String rule : constraintMap.keySet()) {
      pw_symb.println(rule + " | " + constraintStrings(constraintMap.get(rule)));
    }

    pw.close();
    pw_symb.close();
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

  private void updateMapEqsat(Circuit c, Map<String, Double> symbolMap) {
    List<SimpleEntry<Integer, List<Integer>>> hash = verifier.hashCode(c, symbolMap);
    for (SimpleEntry<Integer, List<Integer>> entry : hash) {
      ConstrainedCircuit cc = new ConstrainedCircuit(c, entry.getValue());
      EggGen.ConstrainedCircuit eggcc = CircuitTranslator.translate(cc);
      egraph.addConstrainedCircuit(eggcc);
      egraph.setFingerprint(eggcc, entry.getKey());
    }
  }

  private void updateMap(Circuit c, Map<String, Double> symbolMap) {
    List<SimpleEntry<Integer, List<Integer>>> hash = verifier.hashCode(c, symbolMap);
    for (SimpleEntry<Integer, List<Integer>> entry : hash) {
      ConstrainedCircuit cc = new ConstrainedCircuit(c, entry.getValue());
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

  private Map<String, Double> getSymbolMap() {
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
    copied.setUsedQubits(new ArrayList<>(c.getUsedQubits()));
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

  public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
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

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    try {
      CommandLine cmd = parser.parse(options, args);
      String gateset = cmd.getOptionValue("gateSet");
      Integer maxQubits = Integer.parseInt(cmd.getOptionValue("maxQubits"));
      Integer maxSize = Integer.parseInt(cmd.getOptionValue("maxSize"));

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
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset);
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
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset);
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
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset);
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
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset);
          break;
        }
        default: throw new RuntimeException("unreachable");
      }

      long time1 = System.currentTimeMillis();
      //enumerator.enumerate(maxQubits, maxSize);
      enumerator.enumerateEqsat(maxQubits, maxSize);
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

