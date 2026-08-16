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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
  private LinkedHashSet<MatrixConstrainedRule> learned_matrix_constrained;

  public java.util.Set<MatrixConstrainedRule> getLearnedMatrixConstrained() {
    return java.util.Collections.unmodifiableSet(learned_matrix_constrained);
  }
  public String filename;
  public String fileSymname;
  public String gatesetName;
  private Set<String> canonicalCircuits = new HashSet<>();
  private SymbolicSolve solver;
  private Map<String, List<Circuit>> eigenmap;
  private List<Circuit> symbccs;
  private Set<String> badrules;
  private Set<String> goodRules;

  private static final boolean SMT_CHECK = !"false".equals(System.getProperty("smt.check"));
  public long smtChecked = 0, smtRejected = 0, smtUnknown = 0, smtTimeMs = 0;
  private Process smtProc;
  private java.io.BufferedWriter smtIn;
  private BufferedReader smtOut;

  private void startSmtServer() throws IOException {
    ProcessBuilder pb = new ProcessBuilder("python3", "smt_check.py", "--server");
    pb.redirectError(ProcessBuilder.Redirect.INHERIT);
    smtProc = pb.start();
    smtIn = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
        smtProc.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
    smtOut = new BufferedReader(new InputStreamReader(
        smtProc.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
  }

  private String smtCheckEquivalent(String lhsQasm, String rhsQasm) {
    long t0 = System.currentTimeMillis();
    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        if (smtProc == null || !smtProc.isAlive()) {
          startSmtServer();
        }
        java.util.Base64.Encoder enc = java.util.Base64.getEncoder();
        String req = enc.encodeToString("CHECK".getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + "\t" + enc.encodeToString(lhsQasm.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            + "\t" + enc.encodeToString(rhsQasm.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        smtIn.write(req);
        smtIn.write('\n');
        smtIn.flush();
        String line = smtOut.readLine();
        if (line == null) {
          throw new IOException("smt_check.py closed unexpectedly");
        }
        smtTimeMs += System.currentTimeMillis() - t0;
        return new String(java.util.Base64.getDecoder().decode(line.trim()),
            java.nio.charset.StandardCharsets.UTF_8);
      } catch (IOException e) {
        logger.warn("smt_check server error (attempt {}): {}", attempt + 1, e.getMessage());
        if (smtProc != null) smtProc.destroyForcibly();
        smtProc = null;
      }
    }
    smtTimeMs += System.currentTimeMillis() - t0;
    return "UNKNOWN server unavailable";
  }

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
        if(this.totalSize != other.totalSize) {
          return this.totalSize - other.totalSize;
        }

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
  public static boolean skipDistinctEigen = false;
  public static boolean disableSymbFilters = false;
  public static final boolean GROUP_SYMB_EIGEN = Boolean.getBoolean("group.symbeigen");

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
    this.learned_matrix_constrained = new LinkedHashSet<MatrixConstrainedRule>();
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
    this.learned_matrix_constrained = new LinkedHashSet<MatrixConstrainedRule>();
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
    long concretePassStart = System.currentTimeMillis();
    this.filename = String.format("rules_%s_q%s_%s.txt", gatesetName, maxQubits, size);
    this.fileSymname = String.format("rules_%s_q%s_%s_symb.txt", gatesetName, maxQubits, size);
    String filesymbnm = String.format("rules_%s_q%s_%s_symb_nm.txt", gatesetName, maxQubits, size);
    for(String rule: commutative) {
        egraph.addRewrite(rule);
    }
    egraph.push();
    egraph.markSetupEnd();
    fw = new FileWriter(filename, StandardCharsets.UTF_8, false);
    pw = new PrintWriter(fw);
    if (genSymb) {
      fw_symb = new FileWriter(fileSymname, StandardCharsets.UTF_8, false);
      pw_symb = new PrintWriter(fw_symb);
      fw_symb_nm = new FileWriter(filesymbnm, StandardCharsets.UTF_8, false);
      pw_symb_nm = new PrintWriter(fw_symb_nm);
    }
    Map<String, Double> symbolMap = getSymbolMap();
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

    previousReps.put(emptyCircuit.getQasmString(), new ConstrainedCircuit(emptyCircuit, new ArrayList<>()));
    for (int i = 1; i <= Symbsize; i++) {
      logger.info("Enumerating size: " + i);
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

          for (String gate : this.gates) {
            for (int q = 0; q <= Math.min(c.getCircuit().getQubits().size(), numQubits - 1); q++) {
              if (c.getCircuit().hasSymb() && q >= MAX_QUBITS_SYMB) { continue; }
              List<Circuit> circuitsAfterApply = applyGate(c.getCircuit(), gate, q, Math.min(c.getCircuit().getQubits().size() + 1, numQubits));
              for (Circuit caa : circuitsAfterApply) {
                if (previousReps.containsKey(caa.getQasmStringDropFirst())) {
                  long tmap = System.currentTimeMillis();
                  if(caa.hasSymb()) {
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
        ConstrainedCircuit smallest = pickSmallest(map.get(hashcode));
        ecs.add(new EquivalenceClass(map.get(hashcode), smallest));
        if (smallest.getCircuit().getSize() == i) {
          previousReps.putIfAbsent(smallest.getCircuit().getQasmString(), smallest);
        }
      }

        List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> filtered2 = retriveEqfingers(egraph, i);
        List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> centries = new ArrayList<>();
        for(SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit> entry : filtered2) {
          centries.add(new SimpleEntry(CircuitTranslator.translateBack(entry.getKey(), maxQubits), CircuitTranslator.translateBack(entry.getValue(), maxQubits)));
        }
        int learnedBefore = learned_rules.size();
        try {
          choose_eqs_n(centries, 2, false, commutative);
        } catch (IOException e) {
          e.printStackTrace();
        }

        List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> newThisSize = learned_rules.subList(learnedBefore, learned_rules.size());
        logger.info("[size " + i + "] learned " + newThisSize.size() + " new rules (cumulative " + learned_rules.size() + ")");
        for (SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> rule : newThisSize) {
          EggGen.ConstrainedCircuit c1 = CircuitTranslator.translate(rule.getKey());
          EggGen.ConstrainedCircuit c2 = CircuitTranslator.translate(rule.getValue());
          logger.info("[size " + i + "] " + rule.getKey().getCircuit().getQasmString()
              + " | " + rule.getValue().getCircuit().getQasmString());
          egraph.addRewriteRule(new SimpleEntry<>(c1, c2), true);
        }

      if (i == 1) {
        Circuit start = getStart();
        List<Circuit> circuitsAfterApplySymb = applyGate(start, "symb", 0, MAX_QUBITS_SYMB);
        previousReps.put(circuitsAfterApplySymb.get(0).getQasmString(), new ConstrainedCircuit(circuitsAfterApplySymb.get(0), new ArrayList<>()));
        symbccs.add(circuitsAfterApplySymb.get(0));
      }
    }

    HashMap<String, List<List<Integer>>> constraintMap = new HashMap<>();
    for(EquivalenceClass ec : symbecs) {
      for (ConstrainedCircuit cc : ec.getCircuits()) {
        Circuit r = ec.getRepresentative().getCircuit();
        Circuit other = cc.getCircuit();
        String rule = ec.getRepresentative().getCircuit().getQasmString() + " | " + cc.getCircuit().getQasmString();

        if (!r.getQasmString().equals(other.getQasmString())) {
          if (!hasCommonSubcircuit(r, other)) {
            if (r.hasSymb() && other.hasSymb()) {
              if (cc.getConstraint().equals(ec.getRepresentative().getConstraint())) {
                if (verifier.verify(r, other, cc.getConstraint())) {
                  logger.debug("rule accepted:" + rule);
                  logger.debug("constraint:" + Arrays.toString(cc.getConstraint().toArray()));
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

    if (pw_symb != null) {
      for (String rule : constraintMap.keySet()) {
        pw_symb.println(rule + " | " + constraintStrings(constraintMap.get(rule)));
      }
      pw_symb.close();
    }

    List<String> rules = egraph.getAllRewriteRulesOpt();
    for(String rule : rules) {
      pw.println(rule);
    }
    pw.close();

    long symbtime = 0;
    long symbtimeMs = 0;
    long eligibleSymbCircuits = 0;
    long withinBucketPairs = 0;
    long totalSymbPairs = 0;
    long rejectedByTrace = 0;
    long rejectedByQubitCount = 0;
    long rejectedByDistinctSymbols = 0;
    long rejectedByEigen = 0;
    long rejectedBySymbolicEigen = 0;
    int finalSymbCandidates = 0;
    long concreteWallSec = (System.currentTimeMillis() - concretePassStart) / 1000;
    logger.info("Concrete Rule generation time (s): " + concreteWallSec);
    if(genSymb) {
      long traceSeed = rand.nextLong() & Long.MAX_VALUE;
      int traceCount = 10;
      logger.info("Symbolic-trace seed: " + traceSeed + ", ntraces: " + traceCount);
      Map<String, List<ConstrainedCircuit>> traceMap = new java.util.concurrent.ConcurrentHashMap<>();
      long time = System.nanoTime();
      java.util.concurrent.atomic.AtomicLong eligibleSymbCirc = new java.util.concurrent.atomic.AtomicLong();
      List<ConstrainedCircuit> traceCandidates = new ArrayList<>();
      for (EquivalenceClass ec : ecs) {
        ConstrainedCircuit repre = ec.getRepresentative();
        if (repre.getCircuit().getGates().size() < size && repre.getCircuit().getGates().size() > 0
                && !repre.getCircuit().hasQubitGreaterThan(MAX_QUBITS_SYMB)) {
          traceCandidates.add(repre);
        }
      }
      traceCandidates.sort(java.util.Comparator.comparing(cc -> cc.getCircuit().getQasmString()));
      eligibleSymbCirc.set(traceCandidates.size());
      if (disableSymbFilters) {
        traceMap.put("all", java.util.Collections.synchronizedList(new ArrayList<>(traceCandidates)));
      } else {
        List<List<EggGen.Gate>> gateLists = new ArrayList<>(traceCandidates.size());
        for (ConstrainedCircuit repre : traceCandidates) {
          gateLists.add(repre.getCircuit().getGates());
        }
        List<String> fps = solver.batchTraceFingerprints(gateLists, MAX_QUBITS_SYMB, traceSeed, traceCount);
        if (fps.size() != traceCandidates.size()) {
          logger.warn("batch fingerprint returned {} lines for {} circuits; falling back to per-circuit calls",
              fps.size(), traceCandidates.size());
          fps = new ArrayList<>(traceCandidates.size());
          for (ConstrainedCircuit repre : traceCandidates) {
            fps.add(solver.getCircuitEigenFingerprint(repre.getCircuit().getGates(), MAX_QUBITS_SYMB, traceSeed));
          }
        }
        for (int ci = 0; ci < traceCandidates.size(); ci++) {
          ConstrainedCircuit repre = traceCandidates.get(ci);
          String fp = fps.get(ci);
          logger.debug("[TRACEBUCKET] key={} circ={}", fp,
              repre.getCircuit().getQasmString().replace("\n", " "));
          traceMap.computeIfAbsent(fp, k -> java.util.Collections.synchronizedList(new ArrayList<>())).add(repre);
        }
      }
      eligibleSymbCircuits += eligibleSymbCirc.get();

      logger.debug("Filtering Symbolic Candidates:" );
      List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> symbenties =
          java.util.Collections.synchronizedList(new ArrayList<>());
      java.util.concurrent.atomic.AtomicInteger rejQubit = new java.util.concurrent.atomic.AtomicInteger();
      java.util.concurrent.atomic.AtomicInteger rejEigen = new java.util.concurrent.atomic.AtomicInteger();
      java.util.concurrent.atomic.AtomicInteger rejSymbols = new java.util.concurrent.atomic.AtomicInteger();
      java.util.concurrent.atomic.AtomicInteger pairCount = new java.util.concurrent.atomic.AtomicInteger();
      int threads = SymbolicSolve.getPoolSize();
      java.util.concurrent.ExecutorService exec =
          java.util.concurrent.Executors.newFixedThreadPool(threads);
      int traceMapindex = 0;
      for (Map.Entry<String, List<ConstrainedCircuit>> entry : traceMap.entrySet()) {
        logger.debug("Processing Trace Group:" + traceMapindex + "/" + traceMap.size() + " Size:" + entry.getValue().size());
        traceMapindex++;
        List<ConstrainedCircuit> bucket = entry.getValue();

        java.util.Map<String, List<ConstrainedCircuit>> eigenGroups =
            new java.util.concurrent.ConcurrentHashMap<>();
        if (GROUP_SYMB_EIGEN || disableSymbFilters || bucket.size() <= 1) {
          eigenGroups.put("all", java.util.Collections.synchronizedList(new ArrayList<>(bucket)));
        } else {
          List<List<EggGen.Gate>> bgl = new ArrayList<>(bucket.size());
          for (ConstrainedCircuit cc : bucket) bgl.add(cc.getCircuit().getGates());
          List<String> efps = solver.batchEigenFingerprints(bgl, MAX_QUBITS_SYMB, traceSeed, traceCount);
          if (efps.size() != bucket.size()) {
            eigenGroups.put("all", java.util.Collections.synchronizedList(new ArrayList<>(bucket)));
          } else {
            for (int bi = 0; bi < bucket.size(); bi++) {
              eigenGroups.computeIfAbsent(efps.get(bi),
                  k -> java.util.Collections.synchronizedList(new ArrayList<>())).add(bucket.get(bi));
            }
          }
        }

        java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();
        for (List<ConstrainedCircuit> eigenBucket : eigenGroups.values()) {
          for (int i = 0; i < eigenBucket.size(); i++) {
            for (int j = i; j < eigenBucket.size(); j++) {
              final ConstrainedCircuit ccA = eigenBucket.get(i);
              final ConstrainedCircuit ccB = eigenBucket.get(j);
              futures.add(exec.submit(() -> {
                pairCount.incrementAndGet();
                Circuit cc1 = ccA.getCircuit();
                Circuit cc2 = ccB.getCircuit();
                Set<String> allQubits = new HashSet<>();
                allQubits.addAll(cc1.getUsedQubits());
                allQubits.addAll(cc2.getUsedQubits());
                if (allQubits.size() > MAX_QUBITS_SYMB) {
                  rejQubit.incrementAndGet();
                  return;
                }
                List<EggGen.Gate> gatesLeft = cc1.getGates();
                gatesLeft.add(0, new EggGen.SYMB(MAX_QUBITS_SYMB));
                List<EggGen.Gate> gatesRight = cc2.getGates();
                gatesRight.add(new EggGen.SYMB(MAX_QUBITS_SYMB));
                EggGen.Circuit cce1 = new EggGen.Circuit(gatesLeft);
                EggGen.Circuit cce2 = new EggGen.Circuit(gatesRight);
                if (cce1.getTwoQubitsCount() < cce2.getTwoQubitsCount()) {
                  EggGen.Circuit temp = cce1; cce1 = cce2; cce2 = temp;
                } else if (cce1.getTwoQubitsCount() == cce2.getTwoQubitsCount() && cce1.gates.size() < cce2.gates.size()) {
                  EggGen.Circuit temp = cce1; cce1 = cce2; cce2 = temp;
                }
                Set<String> symbols1 = new HashSet<>();
                Set<String> symbols2 = new HashSet<>();
                cce1.getAllSymbols(symbols1);
                cce2.getAllSymbols(symbols2);
                if (!disableSymbFilters
                    && !(symbols1.containsAll(symbols2) && symbols2.containsAll(symbols1))) {
                  rejSymbols.incrementAndGet();
                  return;
                }
                if (skipDistinctEigen && solver.hasAllDistinctEigen(cce1, cce2, MAX_QUBITS_SYMB)) {
                  return;
                }
                symbenties.add(new SimpleEntry<>(cce1, cce2));
              }));
            }
          }
        }
        for (java.util.concurrent.Future<?> f : futures) {
          try { f.get(); } catch (Exception e) { logger.warn("filter task failed: " + e); }
        }
      }
      exec.shutdown();
      withinBucketPairs += pairCount.get();
      rejectedByQubitCount += rejQubit.get();
      rejectedByEigen += rejEigen.get();
      rejectedByDistinctSymbols += rejSymbols.get();
      logger.debug("Symb Candidates Size (completeness pass):" + symbenties.size());
      synchronized (symbenties) {
        symbenties.sort(java.util.Comparator.comparing(
            e -> e.getKey().toEggString() + "|" + e.getValue().toEggString()));
      }
      finalSymbCandidates = symbenties.size();
      totalSymbPairs = eligibleSymbCircuits * (eligibleSymbCircuits + 1) / 2;
      rejectedByTrace = totalSymbPairs - withinBucketPairs;

      List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> orbitCandidates =
          CliffordOrbitCandidates.generateForGateset(gatesetName);
      if (!orbitCandidates.isEmpty()) {
        logger.info("Priority pass: adding " + orbitCandidates.size()
            + " Clifford-orbit candidates for gateset " + gatesetName);
        symbenties.addAll(orbitCandidates);
      }

      List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> canonicalSymbEntries = canonicalizeSymbEntries(symbenties);
      infer_symb(canonicalSymbEntries, MAX_QUBITS_SYMB);
      rejectedBySymbolicEigen += symbolicEigenRejects;
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
      symbtimeMs = (time2 - time) / 1000000;
    }

    if (pw_symb_nm != null) pw_symb_nm.close();
    egraph.pop();
    Map<String, Long> data = egraph.getProfilingData();
    egraph.stopEgglogREPL();
    logger.info("Concrete Rule sizes: " + rules.size());
    logger.info("Symbolic Rule sizes: " + learned_matrix_constrained.size());
    logger.info("Symbolic Rule generation time (s): " + symbtime);
    logger.info("Symbolic Rule generation time (ms): " + symbtimeMs);
    logger.info("Symbolic candidate totals: eligible circuits = " + eligibleSymbCircuits
        + ", total pairs considered = " + totalSymbPairs
        + ", rejected by trace bucketing = " + rejectedByTrace
        + ", rejected by qubit count = " + rejectedByQubitCount
        + ", rejected by distinct symbols = " + rejectedByDistinctSymbols
        + ", rejected by concrete eigen test = " + rejectedByEigen
        + ", rejected by symbolic eigen test = " + rejectedBySymbolicEigen
        + ", final accepted candidates = " + finalSymbCandidates);
    logger.info("E-graph time (ms): " + this.egraphTime);
    logger.info("Translation time (ms): " + this.translateTime);
    logger.info("Choose rules time (ms): " + this.chooseTime);
    if (SMT_CHECK) {
      logger.info("SMT rule validation: checked={} refuted={} inconclusive={} time (ms): {}",
          smtChecked, smtRejected, smtUnknown, smtTimeMs);
    }
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
          logger.debug("Add learned:" + CircuitTranslator.translate(rule.getKey()).toEggString() + " | " + CircuitTranslator.translate(rule.getValue()).toEggString());
          EggGen.ConstrainedCircuit c1 = CircuitTranslator.translate(rule.getKey());
          EggGen.ConstrainedCircuit c2 = CircuitTranslator.translate(rule.getValue());
          egg.addRewriteRule(new SimpleEntry<>(c1, c2), true);
        }
        List<Rule> rules = egg.processRules(egg.optrules);
        for(Rule r : rules) {
          egg.addOptRule(r, "opt", "birewrite");
        }
        List<SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit>> entries = new ArrayList<>();

        for (EquivalenceClass ec : ecs) {
          ConstrainedCircuit repre = ec.getRepresentative();
          EggGen.ConstrainedCircuit repreegg = CircuitTranslator.translate(repre);
          String repreStr = repreegg.toEggString();
          String repreCircuitStr = repreegg.circuit.toEggString();

          List<EggGen.ConstrainedCircuit> pairRhs = new ArrayList<>();
          List<String> pairRhsCircuitStrs = new ArrayList<>();
          List<String> pairKeys = new ArrayList<>();
          List<String> pairKeysRev = new ArrayList<>();
          HashMap<String, EggGen.ConstrainedCircuit> uniqueCircuits = new HashMap<>();
          List<String> uniqueOrder = new ArrayList<>();

          for (ConstrainedCircuit candidate : ec.getCircuits()) {
            EggGen.ConstrainedCircuit candidateEgg = CircuitTranslator.translate(candidate);
            String candStr = candidateEgg.toEggString();
            if (repreStr.equals(candStr)) continue;
            String key = repreStr + "|" + candStr;
            String keyRev = candStr + "|" + repreStr;
            if (goodRules.contains(key) || goodRules.contains(keyRev)) continue;
            if (badrules.contains(key) || badrules.contains(keyRev)) continue;
            if (uniqueCircuits.putIfAbsent(repreStr, repreegg) == null) uniqueOrder.add(repreStr);
            if (uniqueCircuits.putIfAbsent(candStr, candidateEgg) == null) uniqueOrder.add(candStr);
            pairRhs.add(candidateEgg);
            pairRhsCircuitStrs.add(candidateEgg.circuit.toEggString());
            pairKeys.add(key);
            pairKeysRev.add(keyRev);
          }

          if (pairKeys.isEmpty()) continue;

          egg.push();
          for (String s : uniqueOrder) {
            egg.addConstrainedCircuit(uniqueCircuits.get(s));
          }
          egg.runN("wire", 5);
          egg.runN("merge", 5);
          egg.runN("wire", 5);
          egg.runN("opt", 5);
          for (int k = 0; k < pairKeys.size(); k++) {
            if (!egg.check(String.format("(= %s %s)", repreCircuitStr, pairRhsCircuitStrs.get(k)))) {
              entries.add(new SimpleEntry<>(repreegg, pairRhs.get(k)));
            } else {
              goodRules.add(pairKeys.get(k));
              goodRules.add(pairKeysRev.get(k));
            }
          }
          egg.pop();
        }
        egg.pop();

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
        logger.debug("Current Entry:");
        for(SimpleEntry<EggGen.ConstrainedCircuit, EggGen.ConstrainedCircuit> entry : filtered2) {
          logger.debug(entry.getKey().toEggString() + ", " + entry.getValue().toEggString());
        }
        return filtered2;
  }

  public void infer_symb (List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> entries, int maxQubits) {
    final int N = entries.size();

    List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> unique = new ArrayList<>();
    List<MatrixConstrainedRule> uniqueRules = new ArrayList<>();
    Set<String> seenKeys = new HashSet<>();
    for (int idx = 0; idx < N; idx++) {
      SimpleEntry<EggGen.Circuit, EggGen.Circuit> entry = entries.get(idx);
      logger.debug("Enumerating Symb Candidats:" + idx + "/" + N);
      logger.debug(entry.getKey().toEggString() + "<->" + entry.getValue().toEggString());
      Map<String, String> qubitToVar = new HashMap<>();
      EggGen.Circuit canonical1 = EggGen.canonicalizeCircuit(entry.getKey(), qubitToVar, true);
      EggGen.Circuit canonical2 = EggGen.canonicalizeCircuit(entry.getValue(), qubitToVar, true);
      String lhsRule = EggGen.circuitToGeneralizedOnlyRemoveQ(canonical1, "c");
      String rhsRule = EggGen.circuitToGeneralizedOnlyRemoveQ(canonical2, "c");
      MatrixConstrainedRule rule = new MatrixConstrainedRule(lhsRule, rhsRule, "birewrite");
      if (!seenKeys.add(lhsRule + "|" + rhsRule)) {
        logger.info("[SYMB " + idx + "/" + N + "] DEDUP (duplicate candidate)");
        continue;
      }
      if (learned_matrix_constrained.contains(rule)) {
        logger.info("[SYMB " + idx + "/" + N + "] DEDUP (already learned)");
        continue;
      }
      unique.add(entry);
      uniqueRules.add(rule);
    }
    final int M = unique.size();
    logger.info("infer_symb: " + M + " unique candidates from " + N + " entries");

    final int threads = SymbolicSolve.getPoolSize();
    java.util.concurrent.ExecutorService exec =
        java.util.concurrent.Executors.newFixedThreadPool(threads);
    List<java.util.concurrent.Future<List<SymbolicSolve.SparseMatrix>>> futures = new ArrayList<>(M);
    for (int idx = 0; idx < M; idx++) {
      final int ii = idx;
      final SimpleEntry<EggGen.Circuit, EggGen.Circuit> entry = unique.get(idx);
      futures.add(exec.submit(() -> {
        EggGen.Circuit c1 = entry.getKey();
        EggGen.Circuit c2 = entry.getValue();
        logger.info("[SYMB " + ii + "/" + M + "] solveSymb start"
            + " | c1=" + c1.toEggString()
            + " | c2=" + c2.toEggString());
        long t0 = System.currentTimeMillis();
        String basis = solver.solveSymb(c1, c2, maxQubits);
        long dt = System.currentTimeMillis() - t0;
        logger.info("[SYMB " + ii + "/" + M + "] solveSymb DONE in " + dt
            + " ms, basis.length=" + (basis == null ? -1 : basis.length()));
        if (dt > 5000) {
          String pref = basis == null ? "null" : basis.substring(0, Math.min(800, basis.length()));
          logger.info("[SYMB " + ii + "/" + M + "] SLOW basis prefix: " + pref);
        }
        logger.debug(basis);
        return solver.parseBasis(basis);
      }));
    }

    symbolicEigenRejects = 0;
    for (int idx = 0; idx < M; idx++) {
      List<SymbolicSolve.SparseMatrix> ms;
      try {
        ms = futures.get(idx).get(Params.SYMB_SOLVE_TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS);
      } catch (java.util.concurrent.TimeoutException te) {
        futures.get(idx).cancel(true);
        logger.warn("[SYMB " + idx + "/" + M + "] solver timeout (>"
            + Params.SYMB_SOLVE_TIMEOUT_SEC + "s) -> skip");
        continue;
      } catch (Exception e) {
        logger.warn("infer_symb task failed: " + e);
        continue;
      }
      if (ms == null || ms.isEmpty()) {
        logger.info("[SYMB " + idx + "/" + M + "] empty basis -> skip");
        continue;
      }
      if (disableSymbFilters || GROUP_SYMB_EIGEN) {
        SimpleEntry<EggGen.Circuit, EggGen.Circuit> entry = unique.get(idx);
        if (!solver.checkEigenSymbolic(entry.getKey(), entry.getValue(), maxQubits)) {
          symbolicEigenRejects++;
          logger.info("[SYMB " + idx + "/" + M + "] symbolic eigen check FAILED -> skip");
          continue;
        }
      }
      MatrixConstrainedRule rule = uniqueRules.get(idx);
      rule.setConstraint(ms);
      learned_matrix_constrained.add(rule);
      logger.info("[SYMB " + idx + "/" + M + "] ACCEPTED (basis size=" + ms.size() + ")");
    }
    exec.shutdown();
  }

  private int symbolicEigenRejects = 0;

  public List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> choose_eqs_n (List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> entries, int n, boolean symb, List<String> commutative) throws FileNotFoundException, IOException {
    PriorityQueue<RuleWithPriority> pq = new PriorityQueue<>();
    HashMap<String, List<List<Integer>>> constraintMap = new HashMap<>();
    Map<String, Double> symbolMap = getSymbolMap();

    int step_size = 10;
    List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> entries_copy = new ArrayList<>(entries);
    List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> learned = new ArrayList<>();

    while(!entries_copy.isEmpty()) {
      logger.debug("Entry Size:" + entries_copy.size());
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
        logger.debug("PQ size: " + pq.size());
        SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> entry = pq.poll().rule;
        entries_copy.remove(entry);
        Circuit r = entry.getKey().getCircuit();
        Circuit other = entry.getValue().getCircuit();
        String rule = r.getQasmString() + " | " + other.getQasmString();
        if (!r.getQasmString().equals(other.getQasmString())) {
          if (!hasCommonSubcircuit(r, other)) {
            if (!r.hasSymb() && !other.hasSymb()) {
              if (verifier.verifyv2(r, other, symbolMap)) {
                String smtVerdict = SMT_CHECK
                    ? smtCheckEquivalent(r.getQasmString(), other.getQasmString())
                    : "VALID";
                if (SMT_CHECK) smtChecked++;
                if (!"VALID".equals(smtVerdict)) {
                  if (smtVerdict.startsWith("UNKNOWN")) {
                    smtUnknown++;
                    logger.warn("rule rejected, SMT inconclusive ({}): {}", smtVerdict, rule);
                  } else {
                    smtRejected++;
                    logger.warn("rule rejected, SMT refuted (passed sampling!): {}", rule);
                  }
                  badrules.add(CircuitTranslator.translate(r).toEggString() + "|" + CircuitTranslator.translate(other).toEggString());
                  badrules.add(CircuitTranslator.translate(other).toEggString() + "|" + CircuitTranslator.translate(r).toEggString());
                  continue;
                }
                learned_rules.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
                if(!symb) {
                  learned.add(new SimpleEntry<>(entry.getKey(), entry.getValue()));
                }
                logger.debug("rule accepted:" + rule);
              } else {
                logger.debug("rule rejected, verified failed:" + rule);
                badrules.add(CircuitTranslator.translate(r).toEggString() + "|" + CircuitTranslator.translate(other).toEggString());
                badrules.add(CircuitTranslator.translate(other).toEggString() + "|" + CircuitTranslator.translate(r).toEggString());
              }
            }
          } else {
            logger.debug("rule rejected, has common subcircuit:" + rule);
            badrules.add(CircuitTranslator.translate(r).toEggString() + "|" + CircuitTranslator.translate(other).toEggString());
            badrules.add(CircuitTranslator.translate(other).toEggString() + "|" + CircuitTranslator.translate(r).toEggString());
          }
        } else {
          logger.debug("rule rejected, lhs = rhs" + rule);
        }
      }

      EggGen egg = new EggGen();
      for(String rule : commutative) {
        egg.addRewrite(rule);
      }

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

      List<SimpleEntry<ConstrainedCircuit, ConstrainedCircuit>> pairEntries = new ArrayList<>();
      List<EggGen.Circuit> pairLhsCircuits = new ArrayList<>();
      List<EggGen.Circuit> pairRhsCircuits = new ArrayList<>();
      List<String> pairLhsStrings = new ArrayList<>();
      List<String> pairRhsStrings = new ArrayList<>();
      List<String> pairKeys = new ArrayList<>();
      List<String> pairKeysRev = new ArrayList<>();
      for (SimpleEntry<ConstrainedCircuit, ConstrainedCircuit> entry : entries_copy) {
        EggGen.Circuit lhs = CircuitTranslator.translate(entry.getKey()).circuit;
        EggGen.Circuit rhs = CircuitTranslator.translate(entry.getValue()).circuit;
        String lhsString = lhs.toEggString();
        String rhsString = rhs.toEggString();
        String key = lhsString + "|" + rhsString;
        String keyRev = rhsString + "|" + lhsString;
        if (goodRules.contains(key) || goodRules.contains(keyRev)) continue;
        pairEntries.add(entry);
        pairLhsCircuits.add(lhs);
        pairRhsCircuits.add(rhs);
        pairLhsStrings.add(lhsString);
        pairRhsStrings.add(rhsString);
        pairKeys.add(key);
        pairKeysRev.add(keyRev);
      }
      final int CHUNK_SIZE = 128;
      logger.debug("Verifying remaining entries (chunked): " + pairEntries.size() + "/" + entries_copy.size()
          + " in chunks of " + CHUNK_SIZE);
      for (int start = 0; start < pairEntries.size(); start += CHUNK_SIZE) {
        int end = Math.min(start + CHUNK_SIZE, pairEntries.size());
        HashMap<String, EggGen.Circuit> uniqueCircuits = new HashMap<>();
        List<String> uniqueOrder = new ArrayList<>();
        for (int k = start; k < end; k++) {
          if (uniqueCircuits.putIfAbsent(pairLhsStrings.get(k), pairLhsCircuits.get(k)) == null) {
            uniqueOrder.add(pairLhsStrings.get(k));
          }
          if (uniqueCircuits.putIfAbsent(pairRhsStrings.get(k), pairRhsCircuits.get(k)) == null) {
            uniqueOrder.add(pairRhsStrings.get(k));
          }
        }
        egg.push();
        for (String s : uniqueOrder) {
          egg.addCircuit(uniqueCircuits.get(s));
        }
        egg.runN("wire", 5);
        egg.runN("merge", 5);
        egg.runBackoff("opt", 5);
        for (int k = start; k < end; k++) {
          if (!egg.check(String.format("(= %s %s)", pairLhsStrings.get(k), pairRhsStrings.get(k)))) {
            new_entries_copy.add(pairEntries.get(k));
          } else {
            goodRules.add(pairKeys.get(k));
            goodRules.add(pairKeysRev.get(k));
          }
        }
        egg.pop();
      }

      logger.debug("new entries computed, size: " + new_entries_copy.size());
      egg.stopEgglogREPL();
      entries_copy = new_entries_copy;
    }

    return learned;
  }

  public void enumerate(int numQubits, int size) {
    Map<String, Double> symbolMap = getSymbolMap();

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
          if (!c.getCircuit().hasQubitGreaterThan(MAX_QUBITS_SYMB) && !c.getCircuit().hasCXH()) {
            List<Circuit> circuitsAfterApplySymb = applyGate(c.getCircuit(), "symb", 0, MAX_QUBITS_SYMB);
            for (Circuit cSymb : circuitsAfterApplySymb) {
              if (previousReps.containsKey(cSymb.getQasmStringDropFirst())) {
                updateMap(cSymb, symbolMap);
              }
            }
          }
          for (String gate : this.gates) {
            if ((gate.equals("cx") || gate.equals("cz")) && c.getCircuit().hasSymb()) { continue; }
            for (int q = 0; q <= Math.min(c.getCircuit().getQubits().size(), numQubits - 1); q++) {
              if (c.getCircuit().hasSymb() && q >= MAX_QUBITS_SYMB) { continue; }
              List<Circuit> circuitsAfterApply = applyGate(c.getCircuit(), gate, q, Math.min(c.getCircuit().getQubits().size() + 1, numQubits));
              for (Circuit caa : circuitsAfterApply) {
                if (previousReps.containsKey(caa.getQasmStringDropFirst())) {
                  updateMap(caa, symbolMap);
                }
              }
            }
          }
        }
      }

      ecs.clear();
      previousReps.clear();
      for (List<ConstrainedCircuit> possibleEC : map.values()) {
        ConstrainedCircuit smallest = pickSmallest(possibleEC);
        ecs.add(new EquivalenceClass(possibleEC, smallest));

        if (smallest.getCircuit().getSize() == i) {
          previousReps.putIfAbsent(smallest.getCircuit().getQasmString(), smallest);
        }
      }

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
              if (cc.getConstraint().equals(ec.getRepresentative().getConstraint())) {
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

  }

  private void updateMapEqsat(Circuit c, Map<String, Double> symbolMap, boolean addEgraph) {
    long t1 = System.currentTimeMillis();
    List<SimpleEntry<Integer, List<Integer>>> hash = verifier.hashCode(c, symbolMap);
    long t2 = System.currentTimeMillis();
    this.enumerationTime += (t2 - t1);
    for (SimpleEntry<Integer, List<Integer>> entry : hash) {
      ConstrainedCircuit cc = new ConstrainedCircuit(c, entry.getValue());
      EggGen.ConstrainedCircuit eggcc = CircuitTranslator.translate(cc);
      if(addEgraph) {
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
      if (map.containsKey(entry.getKey())) {
        symbmap.get(entry.getKey()).add(cc);
      } else {
        symbmap.put(entry.getKey(), new ArrayList<>(Arrays.asList(cc)));
      }
    }
    symbccs.add(c);
    logger.debug("Added Symb Circuit: " + c.getQasmString());

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

  static final class Grammar {
    final String[] gates;
    final Expr[] symbAngles;
    Grammar(String[] gates, Expr[] symbAngles) {
      this.gates = gates;
      this.symbAngles = symbAngles;
    }
  }

  static Grammar loadGrammar(String path) throws IOException {
    List<String> gateLines = new ArrayList<>();
    List<String> angleLines = new ArrayList<>();
    String section = null;
    int lineNo = 0;
    try (BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
      String raw;
      while ((raw = br.readLine()) != null) {
        lineNo++;
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        if (line.startsWith("[") && line.endsWith("]")) {
          section = line.substring(1, line.length() - 1).trim();
          continue;
        }
        if (section == null) {
          throw new IOException(path + ":" + lineNo + ": content outside any [section]");
        }
        switch (section) {
          case "gates":       gateLines.add(line); break;
          case "symbAngles":  angleLines.add(line); break;
          default:
            throw new IOException(path + ":" + lineNo + ": unknown section [" + section + "]"
                    + " (expected [gates] or [symbAngles])");
        }
      }
    }
    if (gateLines.isEmpty())  throw new IOException(path + ": [gates] section is empty or missing");
    if (angleLines.isEmpty()) throw new IOException(path + ": [symbAngles] section is empty or missing");

    LinkedHashSet<String> gateSet = new LinkedHashSet<>();
    for (String g : gateLines) {
      if (!g.matches("[A-Za-z_][A-Za-z0-9_]*"))
        throw new IOException(path + ": invalid gate name '" + g + "'");
      gateSet.add(g);
    }

    LinkedHashMap<String, Expr> angleMap = new LinkedHashMap<>();
    for (String s : angleLines) {
      Expr e = parseAngleExpr(s, path);
      angleMap.putIfAbsent(canonicalKey(e), e);
    }

    return new Grammar(gateSet.toArray(new String[0]),
                       angleMap.values().toArray(new Expr[0]));
  }

  private static String canonicalKey(Expr e) {
    if (e instanceof Symbol) return "S:" + ((Symbol) e).getSymbol();
    if (e instanceof Real)   return "R:" + ((Real) e).getNumber();
    if (e instanceof UnOp)   return "U(" + ((UnOp) e).getOp() + "," + canonicalKey(((UnOp) e).getE()) + ")";
    if (e instanceof BinOp)  return "B(" + ((BinOp) e).getOp() + "," + canonicalKey(((BinOp) e).getE1())
                                       + "," + canonicalKey(((BinOp) e).getE2()) + ")";
    return e.toString();
  }

  static Expr parseAngleExpr(String src, String path) {
    AngleParser p = new AngleParser(src, path);
    Expr e = p.parseExpr();
    p.expectEnd();
    return e;
  }

  private static final class AngleParser {
    private final String src, path;
    private int pos = 0;
    AngleParser(String src, String path) { this.src = src; this.path = path; }

    Expr parseExpr() {
      Expr left = parseTerm();
      while (true) {
        skipWs();
        if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
          char op = src.charAt(pos++);
          Expr right = parseTerm();
          left = new BinOp(op == '+' ? Op.PLUS : Op.SUBTRACT, left, right);
        } else break;
      }
      return left;
    }
    Expr parseTerm() {
      Expr left = parseFactor();
      while (true) {
        skipWs();
        if (pos < src.length() && (src.charAt(pos) == '*' || src.charAt(pos) == '/')) {
          char op = src.charAt(pos++);
          Expr right = parseFactor();
          left = new BinOp(op == '*' ? Op.MULT : Op.DIV, left, right);
        } else break;
      }
      return left;
    }
    Expr parseFactor() {
      skipWs();
      if (pos < src.length() && src.charAt(pos) == '-') {
        pos++;
        return new UnOp(Op.MINUS, parseFactor());
      }
      return parseAtom();
    }
    Expr parseAtom() {
      skipWs();
      if (pos >= src.length()) throw err("unexpected end of expression");
      char c = src.charAt(pos);
      if (c == '(') {
        pos++;
        Expr e = parseExpr();
        skipWs();
        if (pos >= src.length() || src.charAt(pos) != ')') throw err("expected ')'");
        pos++;
        return e;
      }
      if (Character.isDigit(c) || c == '.') {
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
        return new Real(Double.parseDouble(src.substring(start, pos)));
      }
      if (Character.isLetter(c) || c == '_') {
        int start = pos;
        while (pos < src.length()
                && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
        return new Symbol(src.substring(start, pos));
      }
      throw err("unexpected character '" + c + "'");
    }
    void expectEnd() {
      skipWs();
      if (pos < src.length()) throw err("trailing input: '" + src.substring(pos) + "'");
    }
    void skipWs() { while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++; }
    RuntimeException err(String msg) {
      return new RuntimeException(path + ": angle expression '" + src + "' at column " + pos + ": " + msg);
    }
  }

  public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException, IOException {
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

    Option verboseO = new Option("v", "verbose", false, "Enable DEBUG logging (default: INFO only)");
    verboseO.setRequired(false);
    options.addOption(verboseO);

    Option skipDistinctEigenO = new Option("skipDistinct", "skipDistinctEigen", false,
        "Skip symbolic-rule candidates whose L has all-distinct eigenvalues (small/diagonal intertwiner -- fast but structurally simple rules)");
    skipDistinctEigenO.setRequired(false);
    options.addOption(skipDistinctEigenO);

    Option grammarO = new Option("gr", "grammar", true,
        "path to a .grammar file defining [gates] and [symbAngles]; overrides the built-in defaults");
    grammarO.setRequired(false);
    options.addOption(grammarO);

    Option noFilterO = new Option("nofilter", "noSymbFilter", false,
        "disable trace/eigen pre-grouping; run the pairwise eigenvalue check + solve on all candidate pairs");
    noFilterO.setRequired(false);
    options.addOption(noFilterO);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    try {
      CommandLine cmd = parser.parse(options, args);
      boolean verbose = cmd.hasOption("verbose");
      ch.qos.logback.classic.Logger root =
          (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
      root.setLevel(verbose ? ch.qos.logback.classic.Level.DEBUG : ch.qos.logback.classic.Level.INFO);
      String gateset = cmd.getOptionValue("gateSet");
      Integer maxQubits = Integer.parseInt(cmd.getOptionValue("maxQubits"));
      Integer maxSize = Integer.parseInt(cmd.getOptionValue("maxSize"));
      boolean genSymb = Boolean.parseBoolean(cmd.getOptionValue("gensymb"));
      skipDistinctEigen = cmd.hasOption("skipDistinctEigen");
      if (skipDistinctEigen) logger.info("Flag: skipDistinctEigen ON -- filtering out all-distinct-eigenvalue intertwiner candidates");
      disableSymbFilters = cmd.hasOption("noSymbFilter");
      if (disableSymbFilters) logger.info("Flag: nofilter ON -- trace + eigenvalue filters DISABLED (direct solve)");
      Random rand = new Random(Params.ENUMERATOR_SEED);
      logger.info("Enumerator seed: {}", Params.ENUMERATOR_SEED);
      EnumeratorPrune enumerator = null;
      String grammarFile = cmd.getOptionValue("grammar");
      if (grammarFile != null) {
        Grammar gr = loadGrammar(grammarFile);
        logger.info("Loaded grammar from {}: {} gates, {} symbAngles",
                grammarFile, gr.gates.length, gr.symbAngles.length);
        enumerator = new EnumeratorPrune(gr.gates, maxQubits, rand, gr.symbAngles, gateset, genSymb);
      } else switch (gateset) {
        case "nam": {
          String[] gates = {"x", "h", "rz", "cx"};
          Expr[] symbAngles = {
                  new Symbol("theta1"),
                  new Symbol("theta2"),
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
          };
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset, genSymb);
          break;
        }
        case "ion": {
          String[] gates = {"rx", "ry", "rz", "rxx"};
          Expr[] symbAngles = {
                  new Symbol("theta1"),
                  new Symbol("pi"),
                  new BinOp(Op.DIV, new Symbol("pi"), new Real(2)),
                  new UnOp(Op.MINUS, new BinOp(Op.DIV, new Symbol("pi"), new Real(2)))
          };
          enumerator = new EnumeratorPrune(gates, maxQubits, rand, symbAngles, gateset, genSymb);
          break;
        }
        case "ibmnew": {
                    String[] gates = {"cx", "rz", "x", "sx"};
                    Expr[] symbAngles = {
                            new Symbol("theta1"),
                            new BinOp(Op.DIV, new Symbol("pi"), new Real(2)),
                            new UnOp(Op.MINUS, new BinOp(Op.DIV, new Symbol("pi"), new Real(2)))
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
      try{
        enumerator.enumerateEqsat(maxQubits, maxSize, maxSize, commutative);
      } catch (IOException e) {
        e.printStackTrace();
      }
      long time2 = System.currentTimeMillis();
      long time3 = System.currentTimeMillis();
      logger.info(String.format("%s q%s s%s total time (s): %s", gateset, maxQubits, maxSize, ((time3-time1)/1000)));
    } catch (ParseException e) {
      System.err.println(e.getMessage());
      formatter.printHelp("QUESO Rule Synthesizer", options);

      System.exit(1);
    }
  }
}
