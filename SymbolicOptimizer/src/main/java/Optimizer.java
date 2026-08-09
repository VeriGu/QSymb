import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ast.*;

import java.util.concurrent.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.analysis.function.Max;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedMultigraph;

import java.io.FileWriter;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.HashSet;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import java.util.LinkedHashSet;
import org.jgrapht.alg.lca.NaiveLCAFinder;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Optimizer {
    private static final Logger logger = LoggerFactory.getLogger(Optimizer.class);
    public static final boolean LONGRULE_REVERSE = Boolean.getBoolean("longrule.reverse");
    private Verifier verifier;
    private SymbolicSolve solver;
    public volatile CircuitDAG bestCircuitOverall = null;
    public static final java.util.concurrent.atomic.AtomicInteger SYMB_APPLIED =
            new java.util.concurrent.atomic.AtomicInteger(0);
    public static final java.util.concurrent.atomic.AtomicLong SYMB_ATTEMPTS = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_SKIP_GATES = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_NO_MATCH = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_BASIS_CALLS = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_BASIS_PASS = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_BASIS_MS = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_QISKIT_CALLS = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_QISKIT_PASS = new java.util.concurrent.atomic.AtomicLong();
    public static final java.util.concurrent.atomic.AtomicLong SYMB_QISKIT_MS = new java.util.concurrent.atomic.AtomicLong();

    static String symbStatsLine() {
        return "Symb stats: attempts=" + SYMB_ATTEMPTS.get()
                + " skipGates=" + SYMB_SKIP_GATES.get()
                + " noMatch=" + SYMB_NO_MATCH.get()
                + " basisCalls=" + SYMB_BASIS_CALLS.get()
                + " basisPass=" + SYMB_BASIS_PASS.get()
                + " basisMs=" + SYMB_BASIS_MS.get()
                + " qiskitCalls=" + SYMB_QISKIT_CALLS.get()
                + " qiskitPass=" + SYMB_QISKIT_PASS.get()
                + " qiskitMs=" + SYMB_QISKIT_MS.get()
                + " applied=" + SYMB_APPLIED.get();
    }
    public Optimizer() {
        Random rand = new Random();
        solver = new SymbolicSolve(new Random());
        verifier = new Verifier(rand, 7);
    }

    public CircuitDAG qasmToDAG(String qasm) {
        QASMLexer lexer = new QASMLexer(CharStreams.fromString(qasm));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        QASMParser parser = new QASMParser(tokens);
        QASMToDAGVisitor visitor = new QASMToDAGVisitor();
        visitor.visit(parser.program());
        return visitor.getDAG();
    }

    private EggGen.Gate nodeToGate(Node node) {
        switch(node.getId()) {
            case "h":
                return new EggGen.H(node.getQubits().get(0));
            case "sx":
                return new EggGen.SX(node.getQubits().get(0));
            case "cx":
                return new EggGen.CX(node.getQubits().get(0), node.getQubits().get(1));
            case "cz":
                return new EggGen.CZ(node.getQubits().get(0), node.getQubits().get(1));
            case "rz":
                return new EggGen.RZ(node.getQubits().get(0), node.getAngles().get(0));
            case "rx":
                return new EggGen.RX(node.getQubits().get(0), node.getAngles().get(0));
            case "rxx":
                return new EggGen.RXX(node.getQubits().get(0), node.getQubits().get(1), node.getAngles().get(0));
            case "ry":
                return new EggGen.RY(node.getQubits().get(0), node.getAngles().get(0));
            case "u1":
                return new EggGen.U1(node.getQubits().get(0), node.getAngles().get(0));
            case "u2":
                return new EggGen.U2(node.getQubits().get(0), node.getAngles().get(0), node.getAngles().get(1));
            case "u3":
                return new EggGen.U3(node.getQubits().get(0), node.getAngles().get(0), node.getAngles().get(1), node.getAngles().get(2));
            case "x":
                return new EggGen.X(node.getQubits().get(0));

            default:
                throw new RuntimeException(String.format("unimplemented gate: %s", node.getId()));
        }
    }

    private boolean matchOutgoing(DirectedMultigraph<Node, Edge> circuit,
                                  DirectedMultigraph<Node, Edge> pattern,
                                  Node circuitNode,
                                  Node patternNode,
                                  Map<Node, Node> patternToCirc,
                                  Map<Edge, Edge> patternToCircEdges,
                                  Map<String, Expr> angleMap,
                                  List<Node> succsToVisit) {
        for (Edge pattE : pattern.outgoingEdgesOf(patternNode)) {
            if (pattern.getEdgeTarget(pattE).isSinkQubit()) {
                continue;
            }
            boolean foundMatch = false;
            if (circuit.outDegreeOf(circuitNode) != pattern.outDegreeOf(patternNode)) {
                return false;
            }

            for (Edge circE : circuit.outgoingEdgesOf(circuitNode)) {
                if (patternToCirc.containsKey(pattern.getEdgeTarget(pattE))) {

                    if (pattE.sameSourceTargetLabels(circE) && circuit.getEdgeTarget(circE) == patternToCirc.get(pattern.getEdgeTarget(pattE))) {
                        foundMatch = true;
                    }
                } else {
                    if (pattE.sameSourceTargetLabels(circE) && circuit.getEdgeTarget(circE).getId().equals(pattern.getEdgeTarget(pattE).getId())) {
                        if (!pattern.getEdgeTarget(pattE).getAngles().isEmpty()) {
                            if (matchAngles(circuit.getEdgeTarget(circE), pattern.getEdgeTarget(pattE), angleMap)) {
                                patternToCirc.put(pattern.getEdgeTarget(pattE), circuit.getEdgeTarget(circE));
                                foundMatch = true;
                            }
                        } else {
                            patternToCirc.put(pattern.getEdgeTarget(pattE), circuit.getEdgeTarget(circE));
                            foundMatch = true;
                        }
                    }
                }
                if (foundMatch) {
                    patternToCircEdges.put(pattE, circE);
                    succsToVisit.add(pattern.getEdgeTarget(pattE));
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }

        return true;
    }

    private boolean sameQubits(Node n1, Node n2) {
        return n1.getQubits().equals(n2.getQubits()) || (n1.getQubits().get(0).equals(n2.getQubits().get(1)) && n1.getQubits().get(1).equals(n2.getQubits().get(0)));
    }

    private String getCommonQubit(Node n1, Node n2) {
        if (n1.getQubits().get(0).equals(n2.getQubits().get(0)) || n1.getQubits().get(0).equals(n2.getQubits().get(1))) {
            return n1.getQubits().get(0);
        } else if (n1.getQubits().get(1).equals(n2.getQubits().get(1)) || n1.getQubits().get(1).equals(n2.getQubits().get(0))) {
            return n1.getQubits().get(1);
        }
        return null;
    }

    private boolean sameLCA(DirectedMultigraph<Node, Edge> circuit,
                            DirectedMultigraph<Node, Edge> pattern,
                            Node circuitNode,
                            Node patternNode,
                            Node circuitCXAnc,
                            Node pattCXAnc) {
        NaiveLCAFinder<Node, Edge> lcaP = new NaiveLCAFinder<>(pattern);
        String commonPattQubit = getCommonQubit(pattCXAnc, patternNode);
        String commonCircQubit = getCommonQubit(circuitCXAnc, circuitNode);

        for (Edge e : pattern.incomingEdgesOf(patternNode)) {
            if (!e.getQubit().equals(commonPattQubit)) {
                if (pattCXAnc != lcaP.getLCA(pattCXAnc, pattern.getEdgeSource(e))) {
                    for (Edge e2 : circuit.incomingEdgesOf(circuitNode)) {
                        if (!e2.getQubit().equals(commonCircQubit)) {
                            NaiveLCAFinder<Node, Edge> lcaC = new NaiveLCAFinder<>(circuit);
                            if (circuitCXAnc != lcaC.getLCA(circuitCXAnc, circuit.getEdgeSource(e2))) {
                                return true;
                            }
                        }
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkLCA(DirectedMultigraph<Node, Edge> circuit,
                             DirectedMultigraph<Node, Edge> pattern,
                             Node circuitNode,
                             Node patternNode,
                             Map<Node, Node> patternToCirc) {
        if (patternNode.isCX()) {
            for (Node cxAnc : allCXAncs(pattern, patternNode)) {
                if (!sameQubits(cxAnc, patternNode)) {
                    if (patternToCirc.containsKey(cxAnc)) {
                        if (!sameLCA(circuit, pattern, circuitNode, patternNode, patternToCirc.get(cxAnc), cxAnc)) {
                            return false;
                        }
                    }
                }
            }
            for (Node cxDec : allCXDecs(pattern, patternNode)) {
                if (!sameQubits(cxDec, patternNode)) {
                    if (patternToCirc.containsKey(cxDec)) {
                        if (!sameLCA(circuit, pattern, patternToCirc.get(cxDec), cxDec, circuitNode, patternNode)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean matchIncoming(DirectedMultigraph<Node, Edge> circuit,
                                  DirectedMultigraph<Node, Edge> pattern,
                                  Node circuitNode,
                                  Node patternNode,
                                  Map<Node, Node> patternToCirc,
                                  Map<Edge, Edge> patternToCircEdges,
                                  Map<String, Expr> angleMap,
                                  List<Node> ancsToVisit) {
        for (Edge pattE : pattern.incomingEdgesOf(patternNode)) {
            if (pattern.getEdgeSource(pattE).isSourceQubit()) {
                continue;
            }
            boolean foundMatch = false;
            if (circuit.inDegreeOf(circuitNode) != pattern.inDegreeOf(patternNode)) {
                return false;
            }

            for (Edge circE : circuit.incomingEdgesOf(circuitNode)) {
                if (patternToCirc.containsKey(pattern.getEdgeSource(pattE))) {
                    if (pattE.sameSourceTargetLabels(circE) && circuit.getEdgeSource(circE) == patternToCirc.get(pattern.getEdgeSource(pattE))) {
                        if (checkLCA(circuit, pattern, circuitNode, patternNode, patternToCirc)) {
                            foundMatch = true;
                        }
                    }
                } else {
                    if (pattE.sameSourceTargetLabels(circE) && circuit.getEdgeSource(circE).getId().equals(pattern.getEdgeSource(pattE).getId())) {
                        if (checkLCA(circuit, pattern, circuitNode, patternNode, patternToCirc)) {
                            if (!pattern.getEdgeSource(pattE).getAngles().isEmpty()) {
                                if (matchAngles(circuit.getEdgeSource(circE), pattern.getEdgeSource(pattE), angleMap)) {
                                    patternToCirc.put(pattern.getEdgeSource(pattE), circuit.getEdgeSource(circE));
                                    foundMatch = true;
                                }
                            } else {
                                patternToCirc.put(pattern.getEdgeSource(pattE), circuit.getEdgeSource(circE));
                                foundMatch = true;
                            }
                        }
                    }
                }
                if (foundMatch) {
                    patternToCircEdges.put(pattE, circE);
                    ancsToVisit.add(pattern.getEdgeSource(pattE));
                    break;
                }
            }

            if (!foundMatch) {
                return false;
            }
        }
        return true;
    }

    public CircuitDAG find(CircuitDAG circuit, CircuitDAG pattern, String replace, boolean applyOnce, Random rand) {
        List<Node> roots = pattern.getCircuitRoots();
        Node start = roots.get(0);
        Map<Node, Node> patternToCirc = new HashMap<>();
        Map<Edge, Edge> patternToCircEdges = new HashMap<>();
        Map<String, Expr> angleMap = new HashMap<>();
        Set<Node> matched = new HashSet<>();
        Set<Node> replaced = new HashSet<>();
        List<Map<Node, Node>> matches = new ArrayList<>();

        CircuitDAG copy = null;
        List<Node> nodes = new ArrayList<>(circuit.nodes());
        Collections.shuffle(nodes, rand);
        for (Node circN : nodes) {
            patternToCirc.clear();
            patternToCircEdges.clear();
            angleMap.clear();
            if (matched.contains(circN) || replaced.contains(circN)) {
                continue;
            }
            if (circN.isGate() && circN.getId().equals(start.getId())) {
                patternToCirc.put(start, circN);
                if (!start.getAngles().isEmpty()) {
                    if (!matchAngles(circN, start, angleMap)) {
                        continue;
                    }
                }
                List<Node> succsToVisit = new ArrayList<>();
                List<Node> ancsToVisit = new ArrayList<>();
                Set<Node> seen = new HashSet<>();
                if (!matchOutgoing(circuit.getDag(), pattern.getDag(), circN, start, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                    continue;
                }
                if (!matchIncoming(circuit.getDag(), pattern.getDag(), circN, start, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                    continue;
                }
                seen.add(start);

                boolean match = true;
                while (!succsToVisit.isEmpty() || !ancsToVisit.isEmpty()) {
                    while (!succsToVisit.isEmpty()) {
                        Node succ = succsToVisit.get(0);
                        succsToVisit.remove(0);

                        if (seen.contains(succ)) {
                            continue;
                        }

                        if (matched.contains(patternToCirc.get(succ)) || replaced.contains(patternToCirc.get(succ))) {
                            match = false;
                            break;
                        }

                        if (!matchOutgoing(circuit.getDag(), pattern.getDag(), patternToCirc.get(succ), succ, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                            match = false;
                            break;
                        }

                        if (!matchIncoming(circuit.getDag(), pattern.getDag(), patternToCirc.get(succ), succ, patternToCirc, patternToCircEdges, angleMap, ancsToVisit)) {
                            match = false;
                            break;
                        }

                        seen.add(succ);
                    }
                    if (!match) {
                        break;
                    }

                    while (!ancsToVisit.isEmpty()) {
                        Node anc = ancsToVisit.get(0);
                        ancsToVisit.remove(0);
                        if (seen.contains(anc)) {
                            continue;
                        }

                        if (matched.contains(patternToCirc.get(anc)) || replaced.contains(patternToCirc.get(anc))) {
                            match = false;
                            break;
                        }
                        if (!matchOutgoing(circuit.getDag(), pattern.getDag(), patternToCirc.get(anc), anc, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                            match = false;
                            break;
                        }
                        if (!matchIncoming(circuit.getDag(), pattern.getDag(), patternToCirc.get(anc), anc, patternToCirc, patternToCircEdges, angleMap, ancsToVisit)) {
                            match = false;
                            break;
                        }
                        seen.add(anc);
                    }
                    if (!match) {
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                if (patternToCirc.size() == pattern.totalGateCount()) {
                    matched.addAll(patternToCirc.values());
                    matches.add(new HashMap<>(patternToCirc));

                    Map<String, String> patternToCircuitQubit = patternToCircuitQubit(patternToCirc);
                    if (new HashSet<>(patternToCircuitQubit.values()).size() != patternToCircuitQubit.values().size()) {
                        continue;
                    }

                    if (copy == null) {
                        copy = new CircuitDAG(circuit);
                    }

                    String[] searchList = new String[patternToCircuitQubit.size() * 2];
                    String[] replaceList = new String[patternToCircuitQubit.size() * 2];
                    int i = 0;
                    for (String key : patternToCircuitQubit.keySet()) {
                        searchList[i] = key + ",";
                        replaceList[i] = patternToCircuitQubit.get(key) + ",";
                        i++;
                        searchList[i] = key + ";";
                        replaceList[i] = patternToCircuitQubit.get(key) + ";";
                        i++;
                    }
                    String replaceAfterSubst = StringUtils.replaceEach(replace, searchList, replaceList);
                    replaceAfterSubst = replaceAngles(replaceAfterSubst, angleMap);

                    CircuitDAG replaceDag = QASMToDAGVisitor.parse(replaceAfterSubst);
                    replaced.addAll(replaceDag.nodes());

                    replace(copy.getDag(), pattern, replaceDag, patternToCirc, patternToCircuitQubit);
                    if (applyOnce) {
                        return copy;
                    }
                    circuit = copy;
                }
            }
        }
        return copy;
    }

    public void replace(DirectedMultigraph<Node, Edge> circuit,
                        CircuitDAG pattern,
                        CircuitDAG replace,
                        Map<Node, Node> patternToCirc,
                        Map<String, String> patternToCircuitQubit) {

        Map<String, Node> patternRoots = pattern.rootsMap();
        Map<String, Node> patternLeaves = pattern.leavesMap();

        Map<String, Node> replaceRoots = replace.rootsMap();
        Map<String, Node> replaceLeaves = replace.leavesMap();

        Map<String, Node> ancPatternRoots = new HashMap<>();
        for (String qubit : patternRoots.keySet()) {
            String circQubit = patternToCircuitQubit.getOrDefault(qubit, qubit);
            Node match = patternToCirc.getOrDefault(patternRoots.get(qubit), patternRoots.get(qubit));
            for (Edge e : circuit.incomingEdgesOf(match)) {
                if (e.getQubit().equals(circQubit)) {
                    ancPatternRoots.put(circQubit, circuit.getEdgeSource(e));
                }

            }
        }

        Map<String, Node> decPatternLeaves = new HashMap<>();
        for (String qubit : patternLeaves.keySet()) {
            String circQubit = patternToCircuitQubit.getOrDefault(qubit, qubit);
            Node match = patternToCirc.getOrDefault(patternLeaves.get(qubit), patternLeaves.get(qubit));
            for (Edge e : circuit.outgoingEdgesOf(match)) {
                if (e.getQubit().equals(circQubit)) {
                    decPatternLeaves.put(circQubit, circuit.getEdgeTarget(e));
                }
            }
        }
        Set<Node> toRemove = new HashSet<>();
        for (Node n : replace.nodes()) {
            if (n.isQubit()) {
                toRemove.add(n);
            }
        }
        replace.getDag().removeAllVertices(toRemove);
        Graphs.addGraph(circuit, replace.getDag());

        for (Node n : pattern.nodes()) {
            circuit.removeVertex(patternToCirc.getOrDefault(n, n));
        }

        for (String qubit : ancPatternRoots.keySet()) {
            if (replaceRoots.containsKey(qubit)) {
                circuit.addEdge(ancPatternRoots.get(qubit), replaceRoots.get(qubit), pattern.getEdge(ancPatternRoots.get(qubit), replaceRoots.get(qubit), qubit));
            } else {
                circuit.addEdge(ancPatternRoots.get(qubit), decPatternLeaves.get(qubit), pattern.getEdge(ancPatternRoots.get(qubit), decPatternLeaves.get(qubit), qubit));
            }
        }

        for (String qubit : replaceLeaves.keySet()) {
            circuit.addEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), pattern.getEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), qubit));
        }
    }

    private String replaceAngles(String replace, Map<String, Expr> angleMap) {
        for (String angle : angleMap.keySet()) {
            replace = replace.replace(angle, "(" + eval(angleMap.get(angle)).toString() + ")");
        }

        return replace;
    }

    public Map<String, String> patternToCircuitQubit(Map<Node, Node> patternToCirc) {
        Map<String, String> result = new HashMap<>();
        for (Node patternNode : patternToCirc.keySet()) {
            result.put(patternNode.getQubits().get(0), patternToCirc.get(patternNode).getQubits().get(0));
            if (patternNode.isCX()) {
                result.put(patternNode.getQubits().get(1), patternToCirc.get(patternNode).getQubits().get(1));
            } else if (patternNode.isCCZ()) {
                result.put(patternNode.getQubits().get(1), patternToCirc.get(patternNode).getQubits().get(1));
                result.put(patternNode.getQubits().get(2), patternToCirc.get(patternNode).getQubits().get(2));
            }
        }
        return result;
    }

    public CircuitDAG applyRule(CircuitDAG circuit, CircuitDAG lhs, String rhs, boolean applyOnce, Random rand) {
        CircuitDAG pattern = lhs;
        var result = find(circuit, pattern, rhs, applyOnce, rand);
        if (result == null) {
            return circuit;
        }
        return result;
    }

    private List<EggGen.Gate> nodesToGates(List<Node> nodes) {
        List<EggGen.Gate> gates = new ArrayList<>();
        for(Node node : nodes) {
            gates.add(nodeToGate(node));
        }
        return gates;
    }

    private List<Node> allCXAncs(DirectedMultigraph<Node, Edge> circuit, Node node) {
        List<Node> ancs = new ArrayList<>();

        List<Node> ancsToVisit = new ArrayList<>();
        ancsToVisit.addAll(Graphs.predecessorListOf(circuit, node));
        while (!ancsToVisit.isEmpty()) {
            Node anc = ancsToVisit.get(ancsToVisit.size() - 1);
            ancsToVisit.remove(ancsToVisit.size() - 1);
            if (!anc.isGate()) {
                continue;
            }

            if (anc.isCX()) {
                ancs.add(anc);
            }
            ancsToVisit.addAll(Graphs.predecessorListOf(circuit, anc));
        }

        return ancs;
    }

    private List<Node> allCXDecs(DirectedMultigraph<Node, Edge> circuit, Node node) {
        List<Node> decs = new ArrayList<>();

        List<Node> decsToVisit = new ArrayList<>();
        decsToVisit.addAll(Graphs.successorListOf(circuit, node));
        while (!decsToVisit.isEmpty()) {
            Node dec = decsToVisit.get(decsToVisit.size() - 1);
            decsToVisit.remove(decsToVisit.size() - 1);
            if (!dec.isGate()) {
                continue;
            }

            if (dec.isCX()) {
                decs.add(dec);
            }
            decsToVisit.addAll(Graphs.successorListOf(circuit, dec));
        }

        return decs;
    }

    public CircuitDAG symbolicMatchBeforeAfterMono(CircuitDAG circuit, String rule, String rhs, int minSymbSize, int maxSymbSize, List<Map<boolean[], boolean[]>> constraints, EggGen egraph) {
        Map<String, Expr> angleMap = new HashMap<>();
        String lhs = rule;
        int findSymbIndex = lhs.indexOf("symb");
        String lhsbefore = StringUtils.stripStart(lhs.substring(0, findSymbIndex).trim(), ";");
        String lhsafter = StringUtils.stripStart(lhs.substring(lhs.indexOf(";", findSymbIndex)).trim(), ";").trim();
        EggGen.Circuit symblhsbefore = QASMAstBuilder.parse(lhsbefore);
        EggGen.Circuit symblhsafter = QASMAstBuilder.parse(lhsafter);
        List<EggGen.Gate> lhsBeforeGates = symblhsbefore.gates;

        List<EggGen.Gate> lhsAfterGates = symblhsafter.gates;

        EggGen.Circuit lhsBeforeCircuit = new EggGen.Circuit(lhsBeforeGates);
        EggGen.Circuit lhsAfterCircuit = new EggGen.Circuit(lhsAfterGates);
        CircuitDAG lhsBeforeDag = QASMToDAGVisitor.parse(lhsBeforeCircuit.toQASM());
        CircuitDAG lhsAfterDag = QASMToDAGVisitor.parse(lhsAfterCircuit.toQASM());
        Map<String, String> qubitMap = new HashMap<>();
        Map<String, String> reverseMap = new HashMap<>();
        List<Node> matchedNodes = null;
        if (!GraphTests.isConnected(lhsBeforeDag.getDAG()) && lhsBeforeCircuit.gates.size() > 0) {
            logger.debug("Symbolic LHS Before is not connected");
            return null;
        }
        if (!GraphTests.isConnected(lhsAfterDag.getDAG()) && lhsAfterCircuit.gates.size() > 0) {
            logger.debug("Symbolic LHS After is not connected");
            return null;
        }
        if(lhsBeforeCircuit.gates.size() > 0 && lhsAfterCircuit.gates.size() > 0) {
            matchedNodes = matchBeforeAfter(circuit, lhsBeforeDag, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, constraints, null, qubitMap, reverseMap, true, null, new ArrayList<>(), new ArrayList<>());
        } else if(lhsBeforeCircuit.gates.size() == 0 && lhsAfterCircuit.gates.size() > 0) {
            matchedNodes = matchAfter(circuit, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, constraints, null, qubitMap, reverseMap, true, null, new ArrayList<>());
        } else if(lhsBeforeCircuit.gates.size() > 0 && lhsAfterCircuit.gates.size() == 0) {
            matchedNodes = matchBefore(circuit, lhsBeforeDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, constraints, null, qubitMap, reverseMap, true, null, new ArrayList<>());
        }
        if(matchedNodes == null) {
            return null;
        }
        findSymbIndex = rhs.indexOf("symb");
        String rhsbefore = StringUtils.stripStart(rhs.substring(0, findSymbIndex).trim(), ";");
        String rhsafter = StringUtils.stripStart(rhs.substring(rhs.indexOf(";", findSymbIndex)).trim(), ";").trim();
        EggGen.Circuit symbrhsBefore = QASMAstBuilder.parse(rhsbefore);
        EggGen.Circuit symbrhsAfter = QASMAstBuilder.parse(rhsafter);

        EggGen.Circuit symbrhsCanBefore = EggGen.canonicalizeCircuit(symbrhsBefore, reverseMap);
        EggGen.Circuit symbrhsCanAfter = EggGen.canonicalizeCircuit(symbrhsAfter, reverseMap);
        EggGen.Circuit symbrhsCanInstantiatedB = symbrhsCanBefore.instantiate(angleMap);
        EggGen.Circuit symbrhsCanInstantiatedA = symbrhsCanAfter.instantiate(angleMap);

        List<EggGen.Gate> rhsBeforeGates = symbrhsCanInstantiatedB.gates;
        List<EggGen.Gate> rhsAfterGates = symbrhsCanInstantiatedA.gates;
        List<Node> matchedsymb = matchedNodes.subList(lhsBeforeGates.size(), matchedNodes.size() - lhsAfterGates.size());
        List<EggGen.Gate> symbGates = nodesToGates(matchedsymb);

        EggGen.Circuit lhsCircuit = new EggGen.Circuit(nodesToGates(matchedNodes));
        CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuit.toQASM());
        List<EggGen.Gate> rhsCombined = new ArrayList<>(rhsBeforeGates);
        rhsCombined.addAll(symbGates);
        rhsCombined.addAll(new ArrayList<>(rhsAfterGates));
        EggGen.Circuit rhsCircuit = new EggGen.Circuit(rhsCombined);
        if(egraph != null)
            egraph.sendCommand(String.format("(union %s %s)", lhsCircuit.toEggString(), rhsCircuit.toEggString()));

        EggGen.ConstrainedCircuit constrainedRhsCircuit = new EggGen.ConstrainedCircuit(rhsCircuit, new EggGen.Permutation(new ArrayList<>()));
        Random rand = new Random();
        CircuitDAG result = applyRule(circuit, lhsDag, rhsCircuit.toQASM(), true, rand);
        String qasm = result.toQASM();
        return result;
    }

    public static String toBracketForm(String s) {
        return s.replaceAll("q(\\d+)", "q[$1]");
    }

    public CircuitDAG symbolicMatchBeforeAfter(CircuitDAG circuit, String rule, String rhs, int minSymbSize, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, EggGen egraph) {
        Map<String, Expr> angleMap = new HashMap<>();
        rule = rule.replaceAll("\\bc\\b", "(Nil)");
        rule = rule.replaceAll("q\\d+", "(Q \"$0\")");
        rule = rule.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
        EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(rule);
        List<EggGen.Gate> lhsGates = symblhs.gates;
        int symbindex = 0;
        int i = 0;
        for(EggGen.Gate gate : lhsGates) {
            if(gate instanceof EggGen.SYMB) {
                symbindex = i;
            }
            i++;
        }
        List<EggGen.Gate> lhsBeforeGates = new ArrayList<>(lhsGates.subList(0, symbindex));
        List<EggGen.Gate> lhsAfterGates = new ArrayList<>(lhsGates.subList(symbindex + 1, lhsGates.size()));

        EggGen.Circuit lhsBeforeCircuit = new EggGen.Circuit(lhsBeforeGates);
        EggGen.Circuit lhsAfterCircuit = new EggGen.Circuit(lhsAfterGates);
        CircuitDAG lhsBeforeDag = QASMToDAGVisitor.parse(lhsBeforeCircuit.toQASM());
        CircuitDAG lhsAfterDag = QASMToDAGVisitor.parse(lhsAfterCircuit.toQASM());
        Map<String, String> qubitMap = new HashMap<>();
        Map<String, String> reverseMap = new HashMap<>();
        logger.debug("LHS before:" + lhsBeforeDag.toQASM());
        logger.debug("LHS after:" + lhsAfterDag.toQASM());
        List<List<Node>> secBefore = secondaryComponents(lhsBeforeDag);
        List<List<Node>> secAfter = secondaryComponents(lhsAfterDag);
        rhs = rhs.replaceAll("\\bc\\b", "(Nil)");
        rhs = rhs.replaceAll("q\\d+", "(Q \"$0\")");
        rhs = rhs.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
        EggGen.Circuit symbrhs = EggAstBuilder.parseCircuit(rhs);
        int lhsBeforeSize = lhsBeforeGates.size();
        int lhsAfterSize = lhsAfterGates.size();

        MatchAcceptor acceptor = (matched, qm, rm, am) -> {
            try {
                EggGen.Circuit lhsC = new EggGen.Circuit(nodesToGates(matched));
                EggGen.Circuit rhsC = buildSymbolicRhs(matched, lhsBeforeSize, lhsAfterSize, symbrhs, rm, am);
                Map<String, String> renameMap = new HashMap<>();
                EggGen.Circuit lhsCanon = EggGen.canonicalizeCircuit(lhsC, renameMap);
                EggGen.Circuit rhsCanon = EggGen.canonicalizeCircuit(rhsC, renameMap);
                boolean eq = checkEquivalenceWithQiskit(toBracketForm(lhsCanon.toQASM()),
                        toBracketForm(rhsCanon.toQASM()), lhsCanon.getMaxQubits() + 1);
                logger.debug(eq ? "Circuits are equivalent"
                        : "Circuits are not equivalent; growing symbolic region");
                return eq;
            } catch (Throwable e) {
                logger.warn("Symbolic candidate rejected (RHS build/equivalence failed): {}", e.toString());
                return false;
            }
        };

        List<Node> matchedNodes = null;
        if(lhsBeforeSize > 0 && lhsAfterSize > 0) {
            matchedNodes = matchBeforeAfter(circuit, lhsBeforeDag, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false, acceptor, secBefore, secAfter);
        } else if(lhsBeforeSize == 0 && lhsAfterSize > 0) {
            matchedNodes = matchAfter(circuit, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false, acceptor, secAfter);
        } else if(lhsBeforeSize > 0 && lhsAfterSize == 0) {
            matchedNodes = matchBefore(circuit, lhsBeforeDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false, acceptor, secBefore);
        }
        if(matchedNodes == null) {
            return null;
        }

        EggGen.Circuit lhsCircuit = new EggGen.Circuit(nodesToGates(matchedNodes));
        CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuit.toQASM());
        EggGen.Circuit rhsCircuit = buildSymbolicRhs(matchedNodes, lhsBeforeSize, lhsAfterSize, symbrhs, reverseMap, angleMap);
        logger.debug("Symbolic RHS: {}", rhsCircuit.toQASM());

        if(egraph != null)
            egraph.sendCommand(String.format("(union %s %s)", lhsCircuit.toEggString(), rhsCircuit.toEggString()));

        return applyRule(circuit, lhsDag, rhsCircuit.toQASM(), true, new Random());
    }

    private EggGen.Circuit buildSymbolicRhs(List<Node> matched, int lhsBeforeSize, int lhsAfterSize,
                                            EggGen.Circuit symbrhs, Map<String, String> reverseMap,
                                            Map<String, Expr> angleMap) {
        List<EggGen.Gate> rhsGates = EggGen.canonicalizeCircuit(symbrhs, reverseMap).instantiate(angleMap).gates;
        int symbIdx = 0;
        for(int k = 0; k < rhsGates.size(); k++) {
            if(rhsGates.get(k) instanceof EggGen.SYMB) symbIdx = k;
        }
        List<EggGen.Gate> combined = new ArrayList<>(rhsGates.subList(0, symbIdx));
        combined.addAll(nodesToGates(matched.subList(lhsBeforeSize, matched.size() - lhsAfterSize)));
        combined.addAll(rhsGates.subList(symbIdx + 1, rhsGates.size()));
        return new EggGen.Circuit(combined);
    }

    public CircuitDAG symbolicMatch(EggGen.Circuit circuit, String rule, String rhs, int minSymbSize, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, EggGen egraph) {
        logger.debug("circuit: {}", circuit.toQASM());
        CircuitDAG dag = QASMToDAGVisitor.parse(circuit.toQASM());
        Pattern pattern = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+c\\)");
        Matcher matcher = pattern.matcher(rule);
        String removedSymb = null;
        Map<String, Expr> angleMap = new HashMap<>();
        if(matcher.find()) {
            String matched = matcher.group();
            removedSymb = rule.replace(matched, "(Nil)");
            removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
            removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
            EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(removedSymb);
            EggGen.ConstrainedCircuit constrainedSymblhs = new EggGen.ConstrainedCircuit(symblhs, new EggGen.Permutation(new ArrayList<>()));
            CircuitDAG symbdag = QASMToDAGVisitor.parse(constrainedSymblhs.circuit.toQASM());
            if (!GraphTests.isConnected(symbdag.getDAG())) {
                logger.debug("Symbolic LHS is not connected");
                return null;
            }
            Map<String, String> qubitMap = new HashMap<>();
            Map<String, String> reverseMap = new HashMap<>();
            List<Node> matchedNodes = matchBefore(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false, null, new ArrayList<>());
            if(matchedNodes == null) {
                return null;
            }

            Pattern pattern2 = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+(.*)\\)");
            Matcher matcher2 = pattern2.matcher(rhs);
            String removedRhs = null;
            if (matcher2.find()) {
                removedRhs = matcher2.group(1).trim();
            }
            removedRhs = removedRhs.replaceAll("\\bc\\b", "(Nil)");
            removedRhs = removedRhs.replaceAll("q\\d+", "(Q \"$0\")");
            Map<String, String> premap = new HashMap<>();
            premap.put("theta1+theta2", "(BinOp (PLUS) theta1 theta2)");
            for(String angle: angleMap.keySet()) {
                if(premap.containsKey(angle)) {
                    removedRhs = removedRhs.replace(premap.get(angle), angleMap.get(angle).toEggString());
                } else {
                    removedRhs = removedRhs.replace(angle, angleMap.get(angle).toEggString());
                }
            }
            EggGen.Circuit symbrhs = EggAstBuilder.parseCircuit(removedRhs);
            EggGen.Circuit symbrhsCan = EggGen.canonicalizeCircuit(symbrhs, reverseMap);
            List<EggGen.Gate> gates = new ArrayList<>(symbrhsCan.gates);
            List<Node> matchedsymb = matchedNodes.subList(symblhs.gates.size(), matchedNodes.size());
            List<EggGen.Gate> lhsgates = nodesToGates(matchedNodes);
            EggGen.Circuit lhsCircuit = new EggGen.Circuit(lhsgates);

            CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuit.toQASM());

            List<EggGen.Gate> matchedgates = nodesToGates(matchedsymb);
            gates.addAll(0, matchedgates);
            EggGen.Circuit combinedCircuit = new EggGen.Circuit(gates);

            EggGen.ConstrainedCircuit combinedConst = new EggGen.ConstrainedCircuit(combinedCircuit, new EggGen.Permutation(new ArrayList<>()));

            if(egraph != null) {
                egraph.sendCommand(String.format("(union %s %s)", lhsCircuit.toEggString(), combinedCircuit.toEggString()));
            }
            logger.debug("Combined LHS Circuit: {}", lhsCircuit.toQASM());
            logger.debug("Combined RHS Circuit: {}", combinedCircuit.toQASM());
            Random rand = new Random();
            CircuitDAG result = applyRule(dag, lhsDag, combinedCircuit.toQASM(), true, rand);
            String qasm = result.toQASM();
            return result;
        } else {
            Pattern pattern2 = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+(.*)\\)");
            Matcher matcher2 = pattern2.matcher(rule);
            if (matcher2.find()) {
                removedSymb = matcher2.group(1).trim();
            }
            removedSymb = removedSymb.replaceAll("\\bc\\b", "(Nil)");
            removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
            removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
            EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(removedSymb);
            EggGen.ConstrainedCircuit constrainedSymblhs = new EggGen.ConstrainedCircuit(symblhs, new EggGen.Permutation(new ArrayList<>()));
            String symbqasm = constrainedSymblhs.circuit.toQASM();
            CircuitDAG symbdag = QASMToDAGVisitor.parse(symbqasm);
            if (!GraphTests.isConnected(symbdag.getDAG())) {
                return null;
            }
            Map<String, String> qubitMap = new HashMap<>();
            Map<String, String> reverseMap = new HashMap<>();
            List<Node> matchedNodes = matchAfter(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false, null, new ArrayList<>());
            if(matchedNodes == null) {
                return null;
            }

            matcher = pattern.matcher(rhs);
            String removedRhs = null;
            if (matcher.find()) {
                String matched = matcher.group();
                removedRhs = rhs.replace(matched, "(Nil)");
                removedRhs = removedRhs.replaceAll("q\\d+", "(Q \"$0\")");
            }

            for(String angle: angleMap.keySet()) {
                removedRhs = removedRhs.replace(angle, angleMap.get(angle).toEggString());
            }

            EggGen.Circuit symbrhs = EggAstBuilder.parseCircuit(removedRhs);
            EggGen.Circuit symbrhsCan = EggGen.canonicalizeCircuit(symbrhs, reverseMap);
            List<EggGen.Gate> gates = new ArrayList<>(symbrhsCan.gates);
            List<Node> matchedsymb = matchedNodes.subList(0, matchedNodes.size() - symblhs.gates.size());
            List<EggGen.Gate> matchedgates = nodesToGates(matchedsymb);

            List<EggGen.Gate> lhsgates = nodesToGates(matchedNodes);
            EggGen.Circuit lhsCircuit = new EggGen.Circuit(lhsgates);
            CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuit.toQASM());

            gates.addAll(matchedgates);
            EggGen.Circuit combinedCircuit = new EggGen.Circuit(gates);

            EggGen.ConstrainedCircuit combinedConst = new EggGen.ConstrainedCircuit(combinedCircuit, new EggGen.Permutation(new ArrayList<>()));

            if(egraph != null) {
                egraph.sendCommand(String.format("(union %s %s)", lhsCircuit.toEggString(), combinedCircuit.toEggString()));
            }
            Random rand = new Random();
            CircuitDAG result = applyRule(dag, lhsDag, combinedCircuit.toQASM(), true, rand);
            String qasm = result.toQASM();
            return result;
        }
    }

    Node primaryGateRoot(CircuitDAG fragDag) {
        for (Node r : fragDag.getCircuitRoots()) {
            if (r.isGate()) return r;
        }
        return null;
    }

    List<List<Node>> secondaryComponents(CircuitDAG fragDag) {
        List<List<Node>> result = new ArrayList<>();
        Node primary = primaryGateRoot(fragDag);
        if (primary == null) return result;
        ConnectivityInspector<Node, Edge> insp = new ConnectivityInspector<>(fragDag.getDAG());
        Set<Node> seen = new HashSet<>(insp.connectedSetOf(primary));
        List<List<Node>> layers = fragDag.topoSort();
        for (Node r : fragDag.getCircuitRoots()) {
            if (!r.isGate() || seen.contains(r)) continue;
            Set<Node> comp = insp.connectedSetOf(r);
            seen.addAll(comp);
            List<Node> gates = new ArrayList<>();
            for (List<Node> layer : layers) {
                for (Node n : layer) {
                    if (n.isGate() && comp.contains(n)) gates.add(n);
                }
            }
            result.add(gates);
        }
        return result;
    }

    int primaryComponentSize(CircuitDAG fragDag, List<List<Node>> secComps) {
        int total = 0;
        for (Node n : fragDag.getDAG().vertexSet()) {
            if (n.isGate()) total++;
        }
        for (List<Node> comp : secComps) total -= comp.size();
        return total;
    }

    Node adjacentGateOnWire(CircuitDAG dag, Node gate, String wire, boolean predecessor) {
        Set<Edge> edges = predecessor ? dag.getDAG().incomingEdgesOf(gate)
                                      : dag.getDAG().outgoingEdgesOf(gate);
        for (Edge e : edges) {
            if (!wire.equals(e.getQubit())) continue;
            Node n = predecessor ? dag.getDAG().getEdgeSource(e) : dag.getDAG().getEdgeTarget(e);
            return n.isGate() ? n : null;
        }
        return null;
    }

    private Node boundaryWindowGateOn(List<Node> window, String wire, boolean first) {
        if (first) {
            for (Node n : window) {
                if (n.getQubits().contains(wire)) return n;
            }
        } else {
            for (int k = window.size() - 1; k >= 0; k--) {
                if (window.get(k).getQubits().contains(wire)) return window.get(k);
            }
        }
        return null;
    }

    private String sharedPatternWire(Node a, Node b) {
        for (String q : a.getQubits()) {
            if (b.getQubits().contains(q)) return q;
        }
        return null;
    }

    private boolean matchGateAgainstPattern(Node circN, Node patN, Map<String, String> qubitMap,
                                            Map<String, String> reverseMap, Map<String, Expr> angleMap) {
        if (circN == null || !circN.isGate() || !circN.getId().equals(patN.getId())) return false;
        if (circN.getQubits().size() != patN.getQubits().size()) return false;
        if (!patN.getAngles().isEmpty() && !matchAngles(circN, patN, angleMap)) return false;
        for (int k = 0; k < circN.getQubits().size(); k++) {
            String cw = circN.getQubits().get(k);
            String pw = patN.getQubits().get(k);
            if (qubitMap.containsKey(cw) && !qubitMap.get(cw).equals(pw)) return false;
            if (reverseMap.containsKey(pw) && !reverseMap.get(pw).equals(cw)) return false;
        }
        for (int k = 0; k < circN.getQubits().size(); k++) {
            qubitMap.put(circN.getQubits().get(k), patN.getQubits().get(k));
            reverseMap.put(patN.getQubits().get(k), circN.getQubits().get(k));
        }
        return true;
    }

    private List<Node> matchComponentFromAnchor(CircuitDAG dag, List<Node> compGates, boolean before,
                                                Node anchor, Map<String, String> qubitMap,
                                                Map<String, String> reverseMap, Map<String, Expr> angleMap,
                                                Set<Node> taken) {
        Node[] bound = new Node[compGates.size()];
        int anchorIdx = before ? compGates.size() - 1 : 0;
        if (taken.contains(anchor)
                || !matchGateAgainstPattern(anchor, compGates.get(anchorIdx), qubitMap, reverseMap, angleMap)) {
            return null;
        }
        bound[anchorIdx] = anchor;
        int step = before ? -1 : 1;
        for (int idx = anchorIdx + step; idx >= 0 && idx < compGates.size(); idx += step) {
            Node pat = compGates.get(idx);
            Node prevPat = compGates.get(idx - step);
            String sharedPq = sharedPatternWire(pat, prevPat);
            if (sharedPq == null) return null;
            String cw = reverseMap.get(sharedPq);
            if (cw == null) return null;
            Node cand = adjacentGateOnWire(dag, bound[idx - step], cw, before);
            if (cand == null || taken.contains(cand) || Arrays.asList(bound).contains(cand)) return null;
            if (!matchGateAgainstPattern(cand, pat, qubitMap, reverseMap, angleMap)) return null;
            bound[idx] = cand;
        }
        return new ArrayList<>(Arrays.asList(bound));
    }

    private List<Node> matchSecondaryComponent(CircuitDAG dag, List<Node> compGates, boolean before,
                                               List<Node> windowNodes, Map<String, String> qubitMap,
                                               Map<String, String> reverseMap, Map<String, Expr> angleMap,
                                               Set<Node> taken) {
        Node anchorPat = before ? compGates.get(compGates.size() - 1) : compGates.get(0);
        LinkedHashSet<String> wireCands = new LinkedHashSet<>();
        boolean pinned = false;
        for (String pq : anchorPat.getQubits()) {
            if (reverseMap.containsKey(pq)) {
                wireCands.add(reverseMap.get(pq));
                pinned = true;
            }
        }
        if (!pinned) {
            for (Node w : windowNodes) {
                for (String q : w.getQubits()) {
                    if (!qubitMap.containsKey(q)) wireCands.add(q);
                }
            }
        }
        for (String w : wireCands) {
            Node boundary = boundaryWindowGateOn(windowNodes, w, before);
            if (boundary == null) continue;
            Node anchor = adjacentGateOnWire(dag, boundary, w, before);
            if (anchor == null) continue;
            Map<String, String> qm = new HashMap<>(qubitMap);
            Map<String, String> rm = new HashMap<>(reverseMap);
            Map<String, Expr> am = new HashMap<>(angleMap);
            List<Node> boundGates = matchComponentFromAnchor(dag, compGates, before, anchor, qm, rm, am, taken);
            if (boundGates != null) {
                qubitMap.clear(); qubitMap.putAll(qm);
                reverseMap.clear(); reverseMap.putAll(rm);
                angleMap.clear(); angleMap.putAll(am);
                return boundGates;
            }
        }
        return null;
    }

    private boolean matchSecondaryComponents(CircuitDAG dag, List<List<Node>> comps, boolean before,
                                             List<Node> windowNodes, Map<String, String> qubitMap,
                                             Map<String, String> reverseMap, Map<String, Expr> angleMap,
                                             Set<Node> taken, List<Node> out) {
        if (comps.isEmpty()) return true;
        if (windowNodes.isEmpty()) return false;
        Map<String, String> qm = new HashMap<>(qubitMap);
        Map<String, String> rm = new HashMap<>(reverseMap);
        Map<String, Expr> am = new HashMap<>(angleMap);
        Set<Node> used = new HashSet<>(taken);
        List<Node> matched = new ArrayList<>();
        for (List<Node> comp : comps) {
            List<Node> b = matchSecondaryComponent(dag, comp, before, windowNodes, qm, rm, am, used);
            if (b == null) return false;
            used.addAll(b);
            matched.addAll(b);
        }
        qubitMap.clear(); qubitMap.putAll(qm);
        reverseMap.clear(); reverseMap.putAll(rm);
        angleMap.clear(); angleMap.putAll(am);
        out.addAll(matched);
        return true;
    }

    List<Node> matchPrimaryComponent(CircuitDAG circuit, CircuitDAG patternDag,
                                             Node startCirc, Node patternStart,
                                             Map<String, String> qubitMap, Map<String, String> reverseMap,
                                             Map<String, Expr> angleMap, Set<Node> taken) {
        if (startCirc == null || !startCirc.isGate() || taken.contains(startCirc)) return null;
        if (!startCirc.getId().equals(patternStart.getId())) return null;

        DirectedMultigraph<Node, Edge> circ = circuit.getDag();
        DirectedMultigraph<Node, Edge> patt = patternDag.getDag();

        Map<String, Expr> am = new HashMap<>(angleMap);
        if (!patternStart.getAngles().isEmpty() && !matchAngles(startCirc, patternStart, am)) return null;

        Map<Node, Node> patternToCirc = new HashMap<>();
        Map<Edge, Edge> patternToCircEdges = new HashMap<>();
        patternToCirc.put(patternStart, startCirc);

        List<Node> succsToVisit = new ArrayList<>();
        List<Node> ancsToVisit = new ArrayList<>();
        Set<Node> seen = new HashSet<>();
        if (!matchOutgoing(circ, patt, startCirc, patternStart, patternToCirc, patternToCircEdges, am, succsToVisit)) return null;
        if (!matchIncoming(circ, patt, startCirc, patternStart, patternToCirc, patternToCircEdges, am, ancsToVisit)) return null;
        seen.add(patternStart);

        boolean match = true;
        while (!succsToVisit.isEmpty() || !ancsToVisit.isEmpty()) {
            while (!succsToVisit.isEmpty()) {
                Node succ = succsToVisit.remove(0);
                if (seen.contains(succ)) continue;
                Node circSucc = patternToCirc.get(succ);
                if (taken.contains(circSucc)) { match = false; break; }
                if (!matchOutgoing(circ, patt, circSucc, succ, patternToCirc, patternToCircEdges, am, succsToVisit)) { match = false; break; }
                if (!matchIncoming(circ, patt, circSucc, succ, patternToCirc, patternToCircEdges, am, ancsToVisit)) { match = false; break; }
                seen.add(succ);
            }
            if (!match) break;
            while (!ancsToVisit.isEmpty()) {
                Node anc = ancsToVisit.remove(0);
                if (seen.contains(anc)) continue;
                Node circAnc = patternToCirc.get(anc);
                if (taken.contains(circAnc)) { match = false; break; }
                if (!matchOutgoing(circ, patt, circAnc, anc, patternToCirc, patternToCircEdges, am, succsToVisit)) { match = false; break; }
                if (!matchIncoming(circ, patt, circAnc, anc, patternToCirc, patternToCircEdges, am, ancsToVisit)) { match = false; break; }
                seen.add(anc);
            }
            if (!match) break;
        }
        if (!match) return null;

        Map<String, String> qm = new HashMap<>(qubitMap);
        Map<String, String> rm = new HashMap<>(reverseMap);
        for (Map.Entry<Node, Node> e : patternToCirc.entrySet()) {
            Node pat = e.getKey();
            Node cir = e.getValue();
            if (!pat.isGate()) continue;
            List<String> pq = pat.getQubits();
            List<String> cq = cir.getQubits();
            if (pq.size() != cq.size()) return null;
            for (int k = 0; k < pq.size(); k++) {
                String c = cq.get(k), p = pq.get(k);
                if (qm.containsKey(c) && !qm.get(c).equals(p)) return null;
                if (rm.containsKey(p) && !rm.get(p).equals(c)) return null;
                qm.put(c, p);
                rm.put(p, c);
            }
        }

        List<Node> matchedGates = new ArrayList<>();
        for (List<Node> layer : patternDag.topoSort()) {
            for (Node n : layer) {
                if (n.isGate() && patternToCirc.containsKey(n)) matchedGates.add(patternToCirc.get(n));
            }
        }

        qubitMap.clear(); qubitMap.putAll(qm);
        reverseMap.clear(); reverseMap.putAll(rm);
        angleMap.clear(); angleMap.putAll(am);
        return matchedGates;
    }

    private List<Node> matchBeforeAfter(CircuitDAG dag, CircuitDAG symbbefore, CircuitDAG symbafter, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<Map<boolean[], boolean[]>> constraints, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap, boolean isMono, MatchAcceptor acceptor, List<List<Node>> secBefore, List<List<Node>> secAfter) {
        Node patternBeforeStart = primaryGateRoot(symbbefore);
        Node patternAfterStart = primaryGateRoot(symbafter);
        if (patternBeforeStart == null || patternAfterStart == null) {
            return null;
        }
        int primaryBeforeSize = primaryComponentSize(symbbefore, secBefore);
        int primaryAfterSize = primaryComponentSize(symbafter, secAfter);

        List<List<Node>> layers = dag.topoSort();
        Map<Node, Integer> layerOf = new HashMap<>();
        for(int li = 0; li < layers.size(); li++) {
            for(Node n : layers.get(li)) layerOf.put(n, li);
        }
        for(int i = 0; i < layers.size(); i++) {
            List<Node> layer = layers.get(i);
            for(Node node : layer) {
                qubitMap.clear();
                reverseMap.clear();
                angleMap.clear();
                if(node.isGate() && node.getId().equals(patternBeforeStart.getId())) {
                    List<Node> beforeMatched = matchPrimaryComponent(
                            dag, symbbefore, node, patternBeforeStart,
                            qubitMap, reverseMap, angleMap, new HashSet<>());
                    if(beforeMatched == null || beforeMatched.size() != primaryBeforeSize) {
                        continue;
                    }
                    int growthStart = 0;
                    Set<String> blockedQubits = new HashSet<>();
                    Set<String> trackedQubits = new HashSet<>();
                    List<Node> symb = new ArrayList<>();
                    List<Node> symbToReplace = new ArrayList<>();
                    symbToReplace.addAll(beforeMatched);
                    int primaryBeforeCount = symbToReplace.size();
                    for(Node bn : beforeMatched) {
                        trackedQubits.addAll(bn.getQubits());
                        Integer ly = layerOf.get(bn);
                        if(ly != null && ly + 1 > growthStart) growthStart = ly + 1;
                    }
                    for(int j = growthStart; j < layers.size(); j++) {
                        if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                            break;
                        }
                        List<Node> layerJ = layers.get(j);

                        for(Node circN: layerJ) {
                            Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                            Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                            Map<String, Expr> tempAngleMap = new HashMap<>(angleMap);
                            if(!circN.isGate()) {
                                continue;
                            }
                            Set<String> trackedIntersection = new HashSet<>(trackedQubits);
                            trackedIntersection.retainAll(circN.getQubits());
                            if(!trackedIntersection.isEmpty()) {
                                trackedQubits.addAll(circN.getQubits());
                                if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                                    break;
                                }

                                boolean match = false;
                                List<Node> afterChainNodes = new ArrayList<>();
                                if(!blockedQubits.contains(circN.getQubits().get(0)) && circN.getId().equals(patternAfterStart.getId())) {
                                    Set<Node> takenAfter = new HashSet<>(symbToReplace);
                                    List<Node> afterMatched = matchPrimaryComponent(
                                            dag, symbafter, circN, patternAfterStart,
                                            tempQubitMap, tempReverseMap, tempAngleMap, takenAfter);
                                    if(afterMatched != null && afterMatched.size() == primaryAfterSize) {
                                        afterChainNodes = new ArrayList<>(afterMatched);
                                        afterChainNodes.remove(circN);
                                        match = true;
                                    }
                                }
                                if(match) {
                                    Circuit symbCirc = opsToCircuit(symb);
                                    EggGen.ConstrainedCircuit symbCircConst = CircuitTranslator.translate(symbCirc);
                                    if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                                        if(isMono) {
                                            for (Map<boolean[], boolean[]> constraint : constraints) {
                                                boolean satisfiesConstraint = true;
                                                for (Map.Entry<boolean[], boolean[]> e : constraint.entrySet()) {
                                                    Map<String, Integer> qubitMapMono = new HashMap<>();
                                                    Map<String, Boolean> expectedMap = new HashMap<>();
                                                    if (patternBeforeStart.getQubits().get(0).equals("q0")) {
                                                        qubitMapMono.put(node.getQubits().get(0), e.getKey()[0] ? 1 : 0);
                                                        expectedMap.put(node.getQubits().get(0), e.getValue()[0]);
                                                    }
                                                    if (patternAfterStart.getQubits().get(0).equals("q0")) {
                                                        qubitMapMono.put(circN.getQubits().get(0), e.getKey()[0] ? 1 : 0);
                                                        expectedMap.put(circN.getQubits().get(0), e.getValue()[0]);
                                                    }
                                                    if (patternBeforeStart.getQubits().get(0).equals("q1")) {
                                                        qubitMapMono.put(node.getQubits().get(0), e.getKey()[1] ? 1 : 0);
                                                        expectedMap.put(node.getQubits().get(0), e.getValue()[1]);
                                                    }
                                                    if (patternAfterStart.getQubits().get(0).equals("q1")) {
                                                        qubitMapMono.put(circN.getQubits().get(0), e.getKey()[1] ? 1 : 0);
                                                        expectedMap.put(circN.getQubits().get(0), e.getValue()[1]);
                                                    }
                                                    if (!verifier.verify(symbCirc, qubitMapMono, expectedMap)) {
                                                        satisfiesConstraint = false;
                                                        break;
                                                    }
                                                }

                                                if (satisfiesConstraint) {
                                                    qubitMap.clear();
                                                    qubitMap.putAll(tempQubitMap);
                                                    reverseMap.clear();
                                                    reverseMap.putAll(tempReverseMap);
                                                    angleMap.clear();
                                                    angleMap.putAll(tempAngleMap);
                                                    symbToReplace.add(circN);
                                                    symbToReplace.addAll(afterChainNodes);
                                                    return symbToReplace;
                                                }
                                            }
                                        } else {
                                            List<Node> candidate = new ArrayList<>(symbToReplace);
                                            candidate.add(circN);
                                            candidate.addAll(afterChainNodes);
                                            List<Node> secBeforeNodes = new ArrayList<>();
                                            List<Node> secAfterNodes = new ArrayList<>();
                                            Set<Node> taken = new HashSet<>(candidate);
                                            boolean secOk = matchSecondaryComponents(dag, secBefore, true, symb, tempQubitMap, tempReverseMap, tempAngleMap, taken, secBeforeNodes);
                                            if(secOk) {
                                                taken.addAll(secBeforeNodes);
                                                secOk = matchSecondaryComponents(dag, secAfter, false, symb, tempQubitMap, tempReverseMap, tempAngleMap, taken, secAfterNodes);
                                            }
                                            if(secOk) {
                                                candidate.addAll(primaryBeforeCount, secBeforeNodes);
                                                candidate.addAll(secAfterNodes);
                                                EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(tempQubitMap));

                                                logger.debug("Canonicalized Circuit: " + canonicalizedCirc.toQASM());
                                                try {
                                                    List<Integer> subspace = new ArrayList<>();
                                                    subspace.add(0);
                                                    subspace.add(1);
                                                    if(checkLinearCombination(canonicalizedCirc, basis, subspace, tempAngleMap)) {
                                                        logger.info("S Satisfy the constraints");
                                                        if(acceptor == null || acceptor.accept(candidate, tempQubitMap, tempReverseMap, tempAngleMap)) {
                                                            qubitMap.clear();
                                                            qubitMap.putAll(tempQubitMap);
                                                            reverseMap.clear();
                                                            reverseMap.putAll(tempReverseMap);
                                                            angleMap.clear();
                                                            angleMap.putAll(tempAngleMap);
                                                            return candidate;
                                                        }
                                                    } else {
                                                        logger.info("S did not satisfy the constraints");
                                                    }
                                                } catch (IOException | InterruptedException e) {
                                                    logger.warn("Error checking linear combination: {}", e.getMessage());
                                                }
                                            }
                                        }
                                    }
                                }

                                Set<String> blockedIntersection = new HashSet<>(blockedQubits);
                                blockedIntersection.retainAll(circN.getQubits());
                                if(!symbToReplace.contains(circN) && blockedIntersection.isEmpty()) {
                                    symb.add(circN);
                                    symbToReplace.add(circN);
                                } else {
                                    if(node.isCCZ()) {
                                        if(!blockedQubits.contains(circN.getQubits().get(2))) {
                                            blockedQubits.add(circN.getQubits().get(2));
                                            symbToReplace.add(circN);
                                        } else {
                                            symbToReplace.add(circN);
                                        }
                                    } else if(node.isCX()) {
                                        if(blockedQubits.contains(circN.getQubits().get(0))) {
                                            symbToReplace.add(circN);
                                            blockedQubits.add(circN.getQubits().get(1));
                                        } else if(blockedQubits.contains(circN.getQubits().get(1))) {
                                            symbToReplace.add(circN);
                                        }
                                    } else {
                                        symbToReplace.add(circN);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<Node> matchAfter(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<Map<boolean[], boolean[]>> constraints, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap, boolean isMono, MatchAcceptor acceptor, List<List<Node>> secComps) {
        Node patternRoot = primaryGateRoot(symbdag);
        if (patternRoot == null) {
            return null;
        }
        int primarySize = primaryComponentSize(symbdag, secComps);
        List<List<Node>> layers = dag.topoSort();
        Set<String> blockedQubits = new HashSet<>();
        Set<String> trackedQubits = new HashSet<>();
        List<Node> symb = new ArrayList<>();
        List<Node> symbToReplace = new ArrayList<>();

        boolean isFirst = true;
        for(int i = 0; i < layers.size(); i++) {
            if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                break;
            }
            for(Node node : layers.get(i)) {
                qubitMap.clear();
                reverseMap.clear();
                angleMap.clear();
                if(!node.isGate()) {
                    continue;
                }

                Set<String> trackedIntersection = new HashSet<>(trackedQubits);
                if(isFirst) {
                    trackedIntersection.addAll(node.getQubits());
                    isFirst = false;
                } else {
                    trackedIntersection.retainAll(node.getQubits());
                }
                if(!trackedIntersection.isEmpty()) {
                    trackedQubits.addAll(node.getQubits());
                    if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                        break;
                    }

                    boolean match = false;
                    List<Node> afterChainNodes = new ArrayList<>();
                    if(!blockedQubits.contains(node.getQubits().get(0)) && node.getId().equals(patternRoot.getId())) {
                        Set<Node> takenAfter = new HashSet<>(symbToReplace);
                        List<Node> afterMatched = matchPrimaryComponent(
                                dag, symbdag, node, patternRoot,
                                qubitMap, reverseMap, angleMap, takenAfter);
                        if(afterMatched != null && afterMatched.size() == primarySize) {
                            afterChainNodes = new ArrayList<>(afterMatched);
                            afterChainNodes.remove(node);
                            match = true;
                        }
                    }

                    if(match) {
                        Circuit symbCirc = opsToCircuit(symb);
                        EggGen.ConstrainedCircuit symbCircConst = CircuitTranslator.translate(symbCirc);
                        if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                            if(isMono) {
                                for (Map<boolean[], boolean[]> constraint : constraints) {
                                    boolean satisfiesConstraint = true;
                                    for (Map.Entry<boolean[], boolean[]> e : constraint.entrySet()) {
                                        Map<String, Integer> qubitMapMono = new HashMap<>();
                                        Map<String, Boolean> expectedMap = new HashMap<>();

                                        if (patternRoot.getQubits().get(0).equals("q0")) {
                                            qubitMapMono.put(node.getQubits().get(0), e.getKey()[0] ? 1 : 0);
                                            expectedMap.put(node.getQubits().get(0), e.getValue()[0]);
                                        }

                                        if (patternRoot.getQubits().get(0).equals("q1")) {
                                            qubitMapMono.put(node.getQubits().get(0), e.getKey()[1] ? 1 : 0);
                                            expectedMap.put(node.getQubits().get(0), e.getValue()[1]);
                                        }

                                        if (!verifier.verify(symbCirc, qubitMapMono, expectedMap)) {
                                            satisfiesConstraint = false;
                                            break;
                                        }
                                    }

                                    if (satisfiesConstraint) {
                                        logger.info("S satisfy the monomial constraints");
                                        symbToReplace.add(node);
                                        symbToReplace.addAll(afterChainNodes);
                                        return symbToReplace;
                                    } else {
                                        logger.info("S does not satisfy the monomial constraints");
                                    }
                                }
                            } else {
                                Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                Map<String, Expr> tempAngleMap = new HashMap<>(angleMap);
                                List<Node> candidate = new ArrayList<>(symbToReplace);
                                candidate.add(node);
                                candidate.addAll(afterChainNodes);
                                List<Node> secNodes = new ArrayList<>();
                                if(matchSecondaryComponents(dag, secComps, false, symb, tempQubitMap, tempReverseMap, tempAngleMap, new HashSet<>(candidate), secNodes)) {
                                    candidate.addAll(secNodes);
                                    EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(tempQubitMap));
                                    try {
                                        List<Integer> subspace = new ArrayList<>();
                                        subspace.add(0);
                                        subspace.add(1);
                                        if(checkLinearCombination(canonicalizedCirc, basis, subspace, tempAngleMap)) {
                                            logger.info("S satisfy the constraints");
                                            if(acceptor == null || acceptor.accept(candidate, tempQubitMap, tempReverseMap, tempAngleMap)) {
                                                qubitMap.clear();
                                                qubitMap.putAll(tempQubitMap);
                                                reverseMap.clear();
                                                reverseMap.putAll(tempReverseMap);
                                                angleMap.clear();
                                                angleMap.putAll(tempAngleMap);
                                                return candidate;
                                            }
                                        } else {
                                            logger.info("S does not satisfy the constraints");
                                        }
                                    }
                                    catch (IOException | InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    Set<String> blockedIntersection = new HashSet<>(blockedQubits);
                    blockedIntersection.retainAll(node.getQubits());
                    if(!symbToReplace.contains(node) && blockedIntersection.isEmpty()) {
                        symb.add(node);
                        symbToReplace.add(node);
                    } else {
                        if(node.isCCZ()) {
                            if(!blockedQubits.contains(node.getQubits().get(2))) {
                                blockedQubits.add(node.getQubits().get(2));
                                symbToReplace.add(node);
                            } else {
                                symbToReplace.add(node);
                            }
                        } else if(node.isCX()) {
                            if(blockedQubits.contains(node.getQubits().get(0))) {
                                symbToReplace.add(node);
                                blockedQubits.add(node.getQubits().get(1));
                            } else if(blockedQubits.contains(node.getQubits().get(1))) {
                                symbToReplace.add(node);
                            }
                        } else {
                            symbToReplace.add(node);
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean sameAngle(Expr angle1, Expr angle2) {
        double period = 4 * Math.PI;
        double a = eval(angle1) % period;
        double b = eval(angle2) % period;
        if (a < 0) a += period;
        if (b < 0) b += period;
        double d = Math.abs(a - b);
        return d < 1e-9 || Math.abs(d - period) < 1e-9;
    }

    private static double evalBinOp(BinOp bo) {
        double v1 = eval(bo.getE1());
        double v2 = eval(bo.getE2());
        switch (bo.getOp()) {
            case PLUS:
                return v1 + (v2);
            case SUBTRACT:
                return v1 - (v2);
            case MULT:
                return v1 * (v2);
            case DIV:
                return v1 / (v2);
            default:
                throw new RuntimeException(String.format("unimplemented BinOp: %s", bo.getOp()));
        }
    }

    private static double evalUnOp(UnOp uo) {
        double v = eval(uo.getE());
        switch (uo.getOp()) {
            case MINUS:
                return -v;
            default:
                throw new RuntimeException(String.format("unimplemented UnOp: %s", uo.getOp()));
        }
    }

    public static Double eval(Expr e) {
        switch (e) {
            case Real r:
                return r.getNumber();
            case BinOp bo:
                return evalBinOp(bo);
            case Symbol s: {
                if (s.getSymbol().equals("pi")) {
                    return Math.PI;
                } else {
                    throw new RuntimeException(String.format("unimplemented symbol: %s", s));
                }
            }
            case UnOp uo:
                return evalUnOp(uo);
            default:
                assert false;
                return null;
        }
    }

    private boolean matchAngle(Expr pattern, Expr circ, Map<String, Expr> angleMap) {
        if(pattern instanceof Symbol) {
            String key = pattern.toString();
            if(key.contains("theta")) {
                if(angleMap.containsKey(key)) {

                    return sameAngle(angleMap.get(key), circ);
                } else {
                    angleMap.put(key, circ);
                    return true;
                }
            } else {
                return sameAngle(pattern, circ);
            }
        } else if(pattern instanceof BinOp) {
            if(circ instanceof BinOp) {
                if(((BinOp) pattern).getOp().equals(((BinOp) circ).getOp())) {
                    return matchAngle(((BinOp) pattern).getE1(), ((BinOp) circ).getE1(), angleMap) && matchAngle(((BinOp) pattern).getE2(), ((BinOp) circ).getE2(), angleMap);
                } else {
                    return false;
                }
            }
        } else if(pattern instanceof UnOp) {
            UnOp upat = (UnOp) pattern;
            if(circ instanceof UnOp) {
                if(upat.getOp().equals(((UnOp) circ).getOp())) {
                    return matchAngle(upat.getE(), ((UnOp) circ).getE(), angleMap);
                } else {
                    return false;
                }
            }
            if (upat.getOp() == Expr.Op.MINUS && circ instanceof Real) {
                return matchAngle(upat.getE(),
                        new Real(-((Real) circ).getNumber()), angleMap);
            }
        } else if (pattern instanceof Real){
            if (circ instanceof Real) {
                return sameAngle(pattern, circ);
            }
            if (((Real) pattern).getNumber() < 0 && circ instanceof UnOp
                    && ((UnOp) circ).getOp() == Expr.Op.MINUS) {
                return matchAngle(new Real(-((Real) pattern).getNumber()),
                        ((UnOp) circ).getE(), angleMap);
            }
        }

        return false;
    }

    private boolean matchAngles(Node circN, Node patternN, Map<String, Expr> angleMap) {
        Map<String, Expr> tempAngleMap = new HashMap<>();
        tempAngleMap.putAll(angleMap);
        boolean matchAngles = true;
        int i = 0;
        for (Expr angle : patternN.getAngles()) {
            if(!matchAngle(angle, circN.getAngles().get(i), tempAngleMap)) {
                matchAngles = false;
                break;
            }
            i++;
        }
        if (matchAngles) {
            angleMap.clear();
            angleMap.putAll(tempAngleMap);
            return true;
        } else {
            return false;
        }
    }

    public Circuit opsToCircuit(List<Node> ops) {
        Expr phi = new Real(1);
        TreeMap<String, Expr> f = new TreeMap<>();
        Symbolic s = new Symbolic(phi, f);
        ArrayList<Symbolic> pathSum = new ArrayList<>(Arrays.asList(s));
        Circuit c = new Circuit(new ArrayList<>(), pathSum, new ArrayList<>(), new ArrayList<>());

        for (Node op : ops) {
            switch (op.getId()) {
                case "h": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.h(c, op.getQubits().get(0));
                    break;
                }
                case "sx": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.sx(c, op.getQubits().get(0));
                    break;
                }
                case "t": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.rz(c, op.getQubits().get(0), new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(4)));
                    break;
                }
                case "tdg": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.rz(c, op.getQubits().get(0), new BinOp(Expr.Op.DIV, new BinOp(Expr.Op.MULT, new Real(7), new Symbol("pi")), new Real(4)));
                    break;
                }
                case "s": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.rz(c, op.getQubits().get(0), new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(2)));
                    break;
                }
                case "sdg": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.rz(c, op.getQubits().get(0), new BinOp(Expr.Op.DIV, new BinOp(Expr.Op.MULT, new Real(3), new Symbol("pi")), new Real(2)));
                    break;
                }
                case "rz": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.rz(c, op.getQubits().get(0), op.getAngles().get(0));
                    break;
                }
                case "rx": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.rx(c, op.getQubits().get(0), op.getAngles().get(0));
                    break;
                }
                case "ry": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.ry(c, op.getQubits().get(0), op.getAngles().get(0));
                    break;
                }
                case "rxx": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    if (!c.hasQubit(op.getQubits().get(1))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(1), new Var(op.getQubits().get(1)));
                        }
                    }
                    Symbolic.rxx(c, op.getQubits().get(0), op.getQubits().get(1), op.getAngles().get(0));
                    break;
                }
                case "u1": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.u1(c, op.getQubits().get(0), op.getAngles().get(0));
                    break;
                }
                case "u2": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.u2(c, op.getQubits().get(0), op.getAngles().get(0), op.getAngles().get(1));
                    break;
                }
                case "u3": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.u3(c, op.getQubits().get(0), op.getAngles().get(0), op.getAngles().get(1), op.getAngles().get(2));
                    break;
                }
                case "x": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.x(c, op.getQubits().get(0));
                    break;
                }
                case "z": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    Symbolic.rz(c, op.getQubits().get(0), new Symbol("pi"));
                    break;
                }
                case "cx": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    if (!c.hasQubit(op.getQubits().get(1))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(1), new Var(op.getQubits().get(1)));
                        }
                    }
                    Symbolic.cx(c, op.getQubits().get(0), op.getQubits().get(1));
                    break;
                }
                case "cz": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    if (!c.hasQubit(op.getQubits().get(1))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(1), new Var(op.getQubits().get(1)));
                        }
                    }
                    Symbolic.cz(c, op.getQubits().get(0), op.getQubits().get(1));
                    break;
                }
                case "ccz": {
                    if (!c.hasQubit(op.getQubits().get(0))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(0), new Var(op.getQubits().get(0)));
                        }
                    }
                    if (!c.hasQubit(op.getQubits().get(1))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(1), new Var(op.getQubits().get(1)));
                        }
                    }
                    if (!c.hasQubit(op.getQubits().get(2))) {
                        for (Symbolic symb : c.getPathSum()) {
                            symb.getF().put(op.getQubits().get(2), new Var(op.getQubits().get(2)));
                        }
                    }
                    Symbolic.cx(c, op.getQubits().get(1), op.getQubits().get(2));
                    Symbolic.rz(c, op.getQubits().get(2), new BinOp(Expr.Op.DIV, new BinOp(Expr.Op.MULT, new Real(7), new Symbol("pi")), new Real(4)));
                    Symbolic.cx(c, op.getQubits().get(0), op.getQubits().get(2));
                    Symbolic.rz(c, op.getQubits().get(2), new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(4)));
                    Symbolic.cx(c, op.getQubits().get(1), op.getQubits().get(2));
                    Symbolic.rz(c, op.getQubits().get(2), new BinOp(Expr.Op.DIV, new BinOp(Expr.Op.MULT, new Real(7), new Symbol("pi")), new Real(4)));
                    Symbolic.cx(c, op.getQubits().get(0), op.getQubits().get(2));
                    Symbolic.cx(c, op.getQubits().get(0), op.getQubits().get(1));
                    Symbolic.rz(c, op.getQubits().get(1), new BinOp(Expr.Op.DIV, new BinOp(Expr.Op.MULT, new Real(7), new Symbol("pi")), new Real(4)));
                    Symbolic.cx(c, op.getQubits().get(0), op.getQubits().get(1));
                    Symbolic.rz(c, op.getQubits().get(0), new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(4)));
                    Symbolic.rz(c, op.getQubits().get(1), new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(4)));
                    Symbolic.rz(c, op.getQubits().get(2), new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(4)));
                    break;
                }
                default: throw new RuntimeException(String.format("unimplemented gate: %s", op.getId()));
            }
        }

        return c;
    }

    public List<Node> matchBefore(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<Map<boolean[], boolean[]>> constraints, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap, boolean isMono, MatchAcceptor acceptor, List<List<Node>> secComps) {
        Node patternRoot = primaryGateRoot(symbdag);
        if (patternRoot == null) {
            return null;
        }
        int primarySize = primaryComponentSize(symbdag, secComps);
        List<List<Node>> layers = dag.topoSort();
        Map<Node, Integer> layerOf = new HashMap<>();
        for(int li = 0; li < layers.size(); li++) {
            for(Node n : layers.get(li)) layerOf.put(n, li);
        }

        for(int i = 0; i < layers.size(); i++) {
            List<Node> layer = layers.get(i);
            for(Node node : layer) {
                qubitMap.clear();
                reverseMap.clear();
                angleMap.clear();
                if(node.isGate() && node.getId().equals(patternRoot.getId())) {
                    List<Node> beforeMatched = matchPrimaryComponent(
                            dag, symbdag, node, patternRoot,
                            qubitMap, reverseMap, angleMap, new HashSet<>());
                    if(beforeMatched == null || beforeMatched.size() != primarySize) {
                        continue;
                    }
                    int growthStart = 0;
                    Set<String> blockQubits = new HashSet<>();
                    Set<String> trackedQubits = new HashSet<>();
                    List<Node> symb = new ArrayList<>();
                    List<Node> symbToReplace = new ArrayList<>();
                    symbToReplace.addAll(beforeMatched);
                    int primaryBeforeCount = symbToReplace.size();
                    for(Node bn : beforeMatched) {
                        trackedQubits.addAll(bn.getQubits());
                        Integer ly = layerOf.get(bn);
                        if(ly != null && ly + 1 > growthStart) growthStart = ly + 1;
                    }
                    for(int j = growthStart; j < layers.size(); j++) {
                        if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                            break;
                        }
                        List<Node> layerJ = layers.get(j);
                        for(Node circN: layerJ) {
                            if(!circN.isGate()) {
                                Circuit symbCirc = opsToCircuit(symb);
                                EggGen.ConstrainedCircuit symbCircConst = CircuitTranslator.translate(symbCirc);
                                if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                                    logger.debug("Symb: {}", symb.toString());
                                    if(isMono) {
                                        for (Map<boolean[], boolean[]> constraint : constraints) {
                                            boolean satisfiesConstraint = true;
                                            for (Map.Entry<boolean[], boolean[]> e : constraint.entrySet()) {
                                                Map<String, Integer> qubitMapMono = new HashMap<>();
                                                Map<String, Boolean> expectedMap = new HashMap<>();
                                                if (patternRoot.getQubits().get(0).equals("q0")) {
                                                    qubitMapMono.put(node.getQubits().get(0), e.getKey()[0] ? 1 : 0);
                                                    expectedMap.put(node.getQubits().get(0), e.getValue()[0]);
                                                }

                                                if (patternRoot.getQubits().get(0).equals("q1")) {
                                                    qubitMapMono.put(node.getQubits().get(0), e.getKey()[1] ? 1 : 0);
                                                    expectedMap.put(node.getQubits().get(0), e.getValue()[1]);
                                                }

                                                if (!verifier.verify(symbCirc, qubitMapMono, expectedMap)) {
                                                    satisfiesConstraint = false;
                                                    break;
                                                }
                                            }

                                            if (satisfiesConstraint) {
                                                logger.info("S satisfy Monomial");
                                                return symbToReplace;
                                            } else {
                                                logger.info("S does not satisfy Monomial");
                                            }
                                        }
                                    } else {
                                        Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                        Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                        Map<String, Expr> tempAngleMap = new HashMap<>(angleMap);
                                        List<Node> candidate = new ArrayList<>(symbToReplace);
                                        List<Node> secNodes = new ArrayList<>();
                                        if(matchSecondaryComponents(dag, secComps, true, symb, tempQubitMap, tempReverseMap, tempAngleMap, new HashSet<>(candidate), secNodes)) {
                                            candidate.addAll(primaryBeforeCount, secNodes);
                                            EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(tempQubitMap));
                                            try {
                                                List<Integer> subspace = new ArrayList<>();
                                                subspace.add(0);
                                                subspace.add(1);
                                                if(checkLinearCombination(canonicalizedCirc, basis, subspace, tempAngleMap)) {
                                                    logger.info("Satisfy the constraints");
                                                    if(acceptor == null || acceptor.accept(candidate, tempQubitMap, tempReverseMap, tempAngleMap)) {
                                                        qubitMap.clear();
                                                        qubitMap.putAll(tempQubitMap);
                                                        reverseMap.clear();
                                                        reverseMap.putAll(tempReverseMap);
                                                        angleMap.clear();
                                                        angleMap.putAll(tempAngleMap);
                                                        return candidate;
                                                    }
                                                } else {
                                                    logger.info("did not satisfy the constraints");
                                                }
                                            } catch (IOException | InterruptedException e) {
                                                logger.warn("Error checking linear combination: {}", e.getMessage());
                                            }
                                        }
                                    }
                                }
                                continue;
                            }
                            Set<String> trackedIntersection = new HashSet<>(trackedQubits);
                            trackedIntersection.retainAll(circN.getQubits());
                            if(!trackedIntersection.isEmpty()) {
                                trackedQubits.addAll(circN.getQubits());
                                if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                                    break;
                                }

                                Circuit symbCirc = opsToCircuit(symb);
                                EggGen.ConstrainedCircuit symbCircConst = CircuitTranslator.translate(symbCirc);
                                if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                                    if(isMono) {
                                        for (Map<boolean[], boolean[]> constraint : constraints) {
                                            boolean satisfiesConstraint = true;
                                            for (Map.Entry<boolean[], boolean[]> e : constraint.entrySet()) {
                                                Map<String, Integer> qubitMapMono = new HashMap<>();
                                                Map<String, Boolean> expectedMap = new HashMap<>();
                                                if (patternRoot.getQubits().get(0).equals("q0")) {
                                                    qubitMapMono.put(node.getQubits().get(0), e.getKey()[0] ? 1 : 0);
                                                    expectedMap.put(node.getQubits().get(0), e.getValue()[0]);
                                                }

                                                if (patternRoot.getQubits().get(0).equals("q1")) {
                                                    qubitMapMono.put(node.getQubits().get(0), e.getKey()[1] ? 1 : 0);
                                                    expectedMap.put(node.getQubits().get(0), e.getValue()[1]);
                                                }

                                                if (!verifier.verify(symbCirc, qubitMapMono, expectedMap)) {
                                                    satisfiesConstraint = false;
                                                    break;
                                                }
                                            }

                                            if (satisfiesConstraint) {
                                                return symbToReplace;
                                            }
                                        }
                                    } else {
                                        Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                        Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                        Map<String, Expr> tempAngleMap = new HashMap<>(angleMap);
                                        List<Node> candidate = new ArrayList<>(symbToReplace);
                                        List<Node> secNodes = new ArrayList<>();
                                        if(matchSecondaryComponents(dag, secComps, true, symb, tempQubitMap, tempReverseMap, tempAngleMap, new HashSet<>(candidate), secNodes)) {
                                            candidate.addAll(primaryBeforeCount, secNodes);
                                            EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(tempQubitMap));

                                            try {
                                                List<Integer> subspace = new ArrayList<>();
                                                subspace.add(0);
                                                subspace.add(1);
                                                if(checkLinearCombination(canonicalizedCirc, basis, subspace, tempAngleMap)) {
                                                    logger.info("S Satisfy the constraints");
                                                    if(acceptor == null || acceptor.accept(candidate, tempQubitMap, tempReverseMap, tempAngleMap)) {
                                                        qubitMap.clear();
                                                        qubitMap.putAll(tempQubitMap);
                                                        reverseMap.clear();
                                                        reverseMap.putAll(tempReverseMap);
                                                        angleMap.clear();
                                                        angleMap.putAll(tempAngleMap);
                                                        return candidate;
                                                    }
                                                } else {
                                                    logger.info("S did not satisfy the constraints");
                                                }
                                            } catch (IOException | InterruptedException e) {
                                                logger.warn("Error checking linear combination: {}", e.getMessage());
                                            }
                                        }
                                    }
                                }

                                if(!symbToReplace.contains(circN)) {
                                    logger.info("Adding to symbolic circuit: {}", circN.getId());
                                    symb.add(circN);
                                    symbToReplace.add(circN);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean checkEquivalenceWithQiskit(String qasm1, String qasm2, int maxQubits) throws IOException, InterruptedException {
        long qiskitT0 = System.currentTimeMillis();
        SYMB_QISKIT_CALLS.incrementAndGet();
        try {
            boolean eq = checkEquivalenceWithQiskitInner(qasm1, qasm2, maxQubits);
            if (eq) SYMB_QISKIT_PASS.incrementAndGet();
            return eq;
        } finally {
            SYMB_QISKIT_MS.addAndGet(System.currentTimeMillis() - qiskitT0);
        }
    }

    private static boolean checkEquivalenceWithQiskitInner(String qasm1, String qasm2, int maxQubits) throws IOException, InterruptedException {
        String header = String.format("OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[%s];\n", maxQubits);
        qasm1 = header + qasm1;
        qasm2 = header + qasm2;

        logger.debug("qasm1: {}", qasm1);
        logger.debug("qasm2: {}", qasm2);
        Path tempFile1 = Files.createTempFile("qasm1", ".qasm");
        Path tempFile2 = Files.createTempFile("qasm2", ".qasm");

        Files.writeString(tempFile1, qasm1);
        Files.writeString(tempFile2, qasm2);

        ProcessBuilder pb = new ProcessBuilder("python3", "/root/qiskit_equivalence_checker.py", tempFile1.toString(), tempFile2.toString());
        Process p = pb.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        String line;
        StringBuilder output = new StringBuilder();
        while ((line = in.readLine()) != null) {
            output.append(line);
        }

        StringBuilder errorOutput = new StringBuilder();
        while ((line = err.readLine()) != null) {
            errorOutput.append(line);
        }

        int exitCode = p.waitFor();

        Files.delete(tempFile1);
        Files.delete(tempFile2);

        if (exitCode != 0) {
            logger.warn("Qiskit equivalence checker script exited with error code: {}", exitCode);
            logger.warn("Error output: {}", errorOutput.toString());
            return false;
        }
        logger.debug("Qiskit checker output: {}", output);

        return output.toString().trim().equals("true");
    }

    Map<String, String> buildQubitMap(EggGen.Circuit concrete, EggGen.Circuit pattern) {
        Map<String, String> qubitMap = new HashMap<>();
        int n = Math.min(concrete.gates.size(), pattern.gates.size());
        for (int k = 0; k < n; k++) {
            EggGen.Gate cg = concrete.gates.get(k);
            EggGen.Gate pg = pattern.gates.get(k);
            if (cg instanceof EggGen.X && pg instanceof EggGen.X) {
                qubitMap.put(((EggGen.X) cg).qubit, ((EggGen.X) pg).qubit);
            } else if (cg instanceof EggGen.H && pg instanceof EggGen.H) {
                qubitMap.put(((EggGen.H) cg).qubit, ((EggGen.H) pg).qubit);
            } else if (cg instanceof EggGen.SX && pg instanceof EggGen.SX) {
                qubitMap.put(((EggGen.SX) cg).qubit, ((EggGen.SX) pg).qubit);
            } else if (cg instanceof EggGen.RZ && pg instanceof EggGen.RZ) {
                qubitMap.put(((EggGen.RZ) cg).qubit, ((EggGen.RZ) pg).qubit);
            } else if (cg instanceof EggGen.RX && pg instanceof EggGen.RX) {
                qubitMap.put(((EggGen.RX) cg).qubit, ((EggGen.RX) pg).qubit);
            } else if (cg instanceof EggGen.RY && pg instanceof EggGen.RY) {
                qubitMap.put(((EggGen.RY) cg).qubit, ((EggGen.RY) pg).qubit);
            } else if (cg instanceof EggGen.U1 && pg instanceof EggGen.U1) {
                qubitMap.put(((EggGen.U1) cg).qubit, ((EggGen.U1) pg).qubit);
            } else if (cg instanceof EggGen.U2 && pg instanceof EggGen.U2) {
                qubitMap.put(((EggGen.U2) cg).qubit, ((EggGen.U2) pg).qubit);
            } else if (cg instanceof EggGen.U3 && pg instanceof EggGen.U3) {
                qubitMap.put(((EggGen.U3) cg).qubit, ((EggGen.U3) pg).qubit);
            } else if (cg instanceof EggGen.GPI && pg instanceof EggGen.GPI) {
                qubitMap.put(((EggGen.GPI) cg).qubit, ((EggGen.GPI) pg).qubit);
            } else if (cg instanceof EggGen.GPI2 && pg instanceof EggGen.GPI2) {
                qubitMap.put(((EggGen.GPI2) cg).qubit, ((EggGen.GPI2) pg).qubit);
            } else if (cg instanceof EggGen.VZ && pg instanceof EggGen.VZ) {
                qubitMap.put(((EggGen.VZ) cg).qubit, ((EggGen.VZ) pg).qubit);
            }
            else if (cg instanceof EggGen.CX && pg instanceof EggGen.CX) {
                qubitMap.put(((EggGen.CX) cg).control, ((EggGen.CX) pg).control);
                qubitMap.put(((EggGen.CX) cg).target, ((EggGen.CX) pg).target);
            } else if (cg instanceof EggGen.CZ && pg instanceof EggGen.CZ) {
                qubitMap.put(((EggGen.CZ) cg).control, ((EggGen.CZ) pg).control);
                qubitMap.put(((EggGen.CZ) cg).target, ((EggGen.CZ) pg).target);
            } else if (cg instanceof EggGen.RXX && pg instanceof EggGen.RXX) {
                qubitMap.put(((EggGen.RXX) cg).qubit1, ((EggGen.RXX) pg).qubit1);
                qubitMap.put(((EggGen.RXX) cg).qubit2, ((EggGen.RXX) pg).qubit2);
            } else if (cg instanceof EggGen.MS && pg instanceof EggGen.MS) {
                qubitMap.put(((EggGen.MS) cg).qubit1, ((EggGen.MS) pg).qubit1);
                qubitMap.put(((EggGen.MS) cg).qubit2, ((EggGen.MS) pg).qubit2);
            }
        }
        return qubitMap;
    }

    double saProbability(double currentCost, double newCost, double temperature) {
        if(newCost < currentCost) {
            return 1.0;
        }

        return Math.exp((currentCost - newCost) / temperature);
    }

    private EggGen.ConstrainedCircuit dequeueCircuit(PriorityQueue<EggGen.ConstrainedCircuit> q, double temperature, CircuitDAG.OptObj optobj, Random random) {
        if(temperature == 0)
            return q.poll();
        else {
            List<EggGen.ConstrainedCircuit> qList = new ArrayList<>(q);
            List<Integer> weights = qList.stream().map(c -> -c.circuit.getTwoQubitsCount()).collect(Collectors.toList());
            int index = sampleSoftMax(weights, temperature, random);
            q.remove(qList.get(index));
            return qList.get(index);
        }
    }

    private CircuitDAG dequeue(PriorityQueue<CircuitDAG> q, double temperature, CircuitDAG.OptObj optobj, Random random) {
        if(temperature == 0)
            return q.poll();
        else {
            List<CircuitDAG> qList = new ArrayList<>(q);
            List<Integer> weights = qList.stream().map(c -> -c.cost(optobj)).collect(Collectors.toList());
            int index = sampleSoftMax(weights, temperature, random);
            q.remove(qList.get(index));
            return qList.get(index);
        }
    }

    private int sampleSoftMax(List<Integer> weights, double temperature, Random random) {
        double[] probs = softmax(weights, temperature);
        return sampleIndex(probs, random);
    }

    private double[] softmax(List<Integer> weights, double temperature) {
        double[] probs = new double[weights.size()];
        double sum = 0;
        int max = weights.stream().max(Integer::compare).get();
        for (int i = 0; i < weights.size(); i++) {
            probs[i] = Math.exp((weights.get(i) - max) / temperature);
            sum += probs[i];
        }

        for (int i = 0; i < probs.length; i++) {
            probs[i] /= sum;
        }

        return probs;
    }

    private int sampleIndex(double[] distribution, Random random) {
        double rand = random.nextDouble();
        double cumulativeProb = 0;
        for (int i = 0; i < distribution.length; i++) {
            cumulativeProb += distribution[i];
            if (rand <= cumulativeProb) {
                return i;
            }
        }

        return distribution.length - 1;
    }

     private boolean validRule(String pattern,
                              String replace,
                              CircuitDAG patternDag,
                              CircuitDAG replaceDag,
                              boolean removeSizePreservingRules,
                              int maxRuleQubits) {
        if (removeSizePreservingRules) {
            if (StringUtils.countMatches(pattern, ";") == StringUtils.countMatches(replace, ";")) {
                return false;
            }
        }

        if (pattern.contains("+")) {
            return false;
        }

        Set<String> patternQubits = patternDag.getQubits();

        if (maxRuleQubits != -1 && patternQubits.size() > maxRuleQubits) {
            return false;
        }

        if (replace.contains("theta1") && !pattern.contains("theta1")) {
            return false;
        }
        if (replace.contains("theta2") && !pattern.contains("theta2")) {
            return false;
        }
        if (replace.contains("theta3") && !pattern.contains("theta3")) {
            return false;
        }
        if (replace.contains("theta4") && !pattern.contains("theta4")) {
            return false;
        }

        if (!GraphTests.isConnected(patternDag.getDag())) {
            return false;
        }

        Set<String> replaceQubits = replaceDag.getQubits();
        if (!patternQubits.containsAll(replaceQubits)) {
            return false;
        }

        if (patternDag.getDagHash() == replaceDag.getDagHash()) {
            return false;
        }

        return true;
    }

    public List<String> filterValidRules(List<String> rules) {
        List<String> validRules = new ArrayList<>();
        for(String rule: rules) {
            String[] splitRule = rule.split(" \\| ");
            String pattern = splitRule[1];
            String replace = splitRule[0];
            CircuitDAG patternDag = QASMToDAGVisitor.parse(pattern);
            CircuitDAG replaceDag = QASMToDAGVisitor.parse(replace);
            if (validRule(splitRule[1], splitRule[0], patternDag, replaceDag, Params.REMOVE_SIZE_PRESERVING_RULES, Params.MAX_RULE_QUBITS)) {
                validRules.add(rule);
            }

            if (Params.USE_SIZE_INCREASING_RULES) {
                if (StringUtils.countMatches(splitRule[0], ";") < StringUtils.countMatches(splitRule[1], ";")) {
                    if (validRule(splitRule[0], splitRule[1], replaceDag, patternDag, !Params.USE_SIZE_PRESERVE_RULE_REFLECTION, Params.MAX_RULE_QUBITS)) {
                        validRules.add(splitRule[1] + " | " + splitRule[0]);
                    }
                }
            }
        }

        return validRules;
    }

    private String reverseRuleString(String rule) {
        String when = "";
        String body = rule;
        int wi = rule.toLowerCase().indexOf(" when ");
        if (wi >= 0) { body = rule.substring(0, wi); when = rule.substring(wi); }
        String[] s = body.split(" \\| ", 2);
        if (s.length < 2) return rule;
        return s[1].trim() + " | " + s[0].trim() + when;
    }

    public List<String> filterValidLongRules(List<String> rules, String gateset) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        for (String rule : rules) {
            String[] parts = rule.split(" \\| ", 2);
            if (parts.length < 2) continue;
            String a = parts[0].trim();
            String brest = parts[1];
            String b = brest, when = "";
            int wi = brest.toLowerCase().indexOf(" when ");
            if (wi >= 0) { b = brest.substring(0, wi); when = brest.substring(wi); }
            b = b.trim();
            String forward = a + " | " + b + when;
            String reverse = b + " | " + a + when;

            Rule parsed;
            try { parsed = QASMAstBuilder.parseRule(rule); }
            catch (Throwable t) { continue; }
            Rule lrForward = new Rule(parsed.rhs, parsed.lhs, parsed.conditions);
            int lsz = lrForward.lhs.gates.size();
            int rsz = lrForward.rhs.gates.size();

            boolean addFwd = false, addRev = false;
            if (lsz == rsz) {
                addFwd = addRev = true;
            } else {
                RuleReverser.Direction d = RuleReverser.decide(lrForward, gateset);
                if (d == RuleReverser.Direction.FORWARD_ONLY || d == RuleReverser.Direction.BOTH) addFwd = true;
                if ((d == RuleReverser.Direction.REVERSE_ONLY || d == RuleReverser.Direction.BOTH)
                        && RuleReverser.reverseIsFireable(lrForward)) addRev = true;
            }

            if (addFwd) {
                CircuitDAG pat = QASMToDAGVisitor.parse(b), rep = QASMToDAGVisitor.parse(a);
                if (validRule(b, a, pat, rep, Params.REMOVE_SIZE_PRESERVING_RULES, Params.MAX_RULE_QUBITS))
                    out.add(forward);
            }
            if (addRev) {
                CircuitDAG pat = QASMToDAGVisitor.parse(a), rep = QASMToDAGVisitor.parse(b);
                if (validRule(a, b, pat, rep, Params.REMOVE_SIZE_PRESERVING_RULES, Params.MAX_RULE_QUBITS))
                    out.add(reverse);
            }
        }
        return new ArrayList<>(out);
    }

    public List<MononialRule> filterValidMonomialRules(List<MononialRule> rules) {
        List<MononialRule> validRules = new ArrayList<>();
        for(MononialRule rule: rules) {

            String find = rule.getRhs();
            String replace = rule.getLhs();
            int findSymbIndex = find.indexOf("symb");
            String findBeforeSymb = StringUtils.stripStart(find.substring(0, findSymbIndex).trim(), ";");
            String findAfterSymb = StringUtils.stripStart(find.substring(find.indexOf(";", findSymbIndex)).trim(), ";").trim();
            int replaceSymbIndex = replace.indexOf("symb");
            String replaceBeforeSymb = StringUtils.stripStart(replace.substring(0, replaceSymbIndex).trim(), ";");
            String replaceAfterSymb = StringUtils.stripStart(replace.substring(replace.indexOf(";", replaceSymbIndex)).trim(), ";").trim();

            CircuitDAG patternBefore = QASMToDAGVisitor.parse(findBeforeSymb);
            CircuitDAG patternAfter = QASMToDAGVisitor.parse(findAfterSymb);
            CircuitDAG targetBefore = QASMToDAGVisitor.parse(replaceBeforeSymb);
            CircuitDAG targetAfter = QASMToDAGVisitor.parse(replaceAfterSymb);

            if(!GraphTests.isConnected(patternBefore.getDAG())) {
                continue;
            }

            if(!GraphTests.isConnected(patternAfter.getDAG())) {
                continue;
            }

            Set<String> replaceQubits = targetBefore.getQubits();
            replaceQubits.addAll(targetAfter.getQubits());
            Set<String> patternQubits = patternBefore.getQubits();
            patternQubits.addAll(patternAfter.getQubits());
            if (!patternQubits.containsAll(replaceQubits)) {
                continue;
            }

            if (replace.contains("theta1") && !find.contains("theta1")) {
                continue;
            }
            if (replace.contains("theta2") && !find.contains("theta2")) {
                continue;
            }
            if (replace.contains("theta3") && !find.contains("theta3")) {
                continue;
            }
            if (replace.contains("theta4") && !find.contains("theta4")) {
                continue;
            }

            validRules.add(new MononialRule(replace, find, rule.getConstraints()));

            if(Params.USE_SIZE_PRESERVING_SYMB_RULES) {
                if(StringUtils.countMatches(find, ";") == StringUtils.countMatches(replace, ";")) {
                    if(replaceQubits.containsAll(patternQubits)) {
                        validRules.add(new MononialRule(find, replace, rule.getConstraints()));
                    }
                }
            }
        }
        return validRules;
    }

    public List<MatrixConstrainedRule> filterValidMatrixRules(List<MatrixConstrainedRule> rules) {
        List<MatrixConstrainedRule> validRules = new ArrayList<>();
        for(MatrixConstrainedRule rule: rules) {
            String lhs = rule.getLHS();
            String rhs = rule.getRHS();
            lhs = lhs.replaceAll("\\bc\\b", "(Nil)");
            lhs = lhs.replaceAll("q\\d+", "(Q \"$0\")");
            lhs = lhs.replaceAll("theta1|theta2|theta3|theta4", "(Symbol \"$0\")");
            rhs = rhs.replaceAll("\\bc\\b", "(Nil)");
            rhs = rhs.replaceAll("q\\d+", "(Q \"$0\")");
            rhs = rhs.replaceAll("theta1|theta2|theta3|theta4", "(Symbol \"$0\")");
            EggGen.Circuit lhsEgg = EggAstBuilder.parseCircuit(lhs);
            EggGen.Circuit rhsEgg = EggAstBuilder.parseCircuit(rhs);

            int symbindex = 0;
            int i = 0;
            for(EggGen.Gate gate : lhsEgg.gates) {
                if(gate instanceof EggGen.SYMB) {
                    symbindex = i;
                    break;
                }
                i++;
            }
            EggGen.Circuit lhsEggBeforeSymb = new EggGen.Circuit(lhsEgg.gates.subList(0, symbindex));
            EggGen.Circuit lhsEggAfterSymb = new EggGen.Circuit(lhsEgg.gates.subList(symbindex + 1, lhsEgg.gates.size()));

            symbindex = 0;
            i = 0;
            for(EggGen.Gate gate : rhsEgg.gates) {
                if(gate instanceof EggGen.SYMB) {
                    symbindex = i;
                    break;
                }
                i++;
            }
            EggGen.Circuit rhsEggBeforeSymb = new EggGen.Circuit(rhsEgg.gates.subList(0, symbindex));
            EggGen.Circuit rhsEggAfterSymb = new EggGen.Circuit(rhsEgg.gates.subList(symbindex + 1, rhsEgg.gates.size()));

            CircuitDAG lhsDagBeforeSymb = QASMToDAGVisitor.parse(lhsEggBeforeSymb.toQASM());
            CircuitDAG lhsDagAfterSymb = QASMToDAGVisitor.parse(lhsEggAfterSymb.toQASM());
            CircuitDAG rhsDagBeforeSymb = QASMToDAGVisitor.parse(rhsEggBeforeSymb.toQASM());
            CircuitDAG rhsDagAfterSymb = QASMToDAGVisitor.parse(rhsEggAfterSymb.toQASM());

            Set<String> replaceQubits = rhsDagBeforeSymb.getQubits();
            replaceQubits.addAll(rhsDagAfterSymb.getQubits());
            Set<String> patternQubits = lhsDagBeforeSymb.getQubits();
            patternQubits.addAll(lhsDagAfterSymb.getQubits());
            if (!patternQubits.containsAll(replaceQubits)) {
                continue;
            }

            if(!GraphTests.isConnected(lhsDagBeforeSymb.getDAG()) && !lhsDagBeforeSymb.getDAG().vertexSet().isEmpty()) {
                continue;
            }

            if(!GraphTests.isConnected(lhsDagAfterSymb.getDAG()) && !lhsDagAfterSymb.getDAG().vertexSet().isEmpty()) {
                continue;
            }

            validRules.add(rule);
        }
        return validRules;
    }

    public int scoreRule(String rule, int numAppliedBest, int numApplied) {
        return numApplied + (2 * numAppliedBest);
    }

    public void optimize_BEAM_normal(CircuitDAG circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, List<MononialRule> symbMonomialRules, int beam_width, int rule_limit, int symb_rule_limit, int min_symb_size, int max_symb_size, int timeout, boolean useSymb, boolean ilp) {
        if(ilp) {
            circuit = IlpCompactor.compact(circuit);
        }
        CircuitDAG bestCircuit = circuit;
        bestCircuitOverall = bestCircuit;
        Set<Integer> seen = new HashSet<>();
        List<String> validRules = filterValidRules(rules);
        List<MononialRule> validMonomialRules = filterValidMonomialRules(symbMonomialRules);
        List<MatrixConstrainedRule> validMatrixRules = filterValidMatrixRules(symbRules);
        seen.add(circuit.hashCode());

        logger.debug("Original Size: {}", circuit.totalGateCount());
        logger.debug("Original 2q: {}", circuit.twoQGateCount());
        logger.debug("Original Circuit: {}", circuit.toQASM());
        logger.debug("Rule size: {}", validRules.size());
        logger.debug("Symb rule limit: {}", symb_rule_limit);
        logger.debug("Min symb size: {}", min_symb_size);
        logger.debug("Max symb size: {}", max_symb_size);
        logger.debug("Timeout: {}", timeout);
        logger.debug("Use symb: {}", useSymb);
        logger.debug("Use ILP compaction: {}{}", ilp, (ilp ? " (init + every " + Params.ILP_PERIOD + " iters)" : ""));
        CircuitComparator comparator = new CircuitComparator(Params.OPTIMIZATION_OBJECTIVE);
        PriorityQueue<CircuitDAG> q = new PriorityQueue<>(comparator);
        q.add(circuit);

        int iters = 0;
        long timesStart = System.nanoTime();
        int rulesize = rules.size();
        int symbRulesSize = symbRules.size();
        int symbMonomialRulesSize = symbMonomialRules.size();
        Random rand = new Random(new Random(1697753314).nextInt());
        Map<String, Integer> rulesUsed = new HashMap<>();
        Map<String, Integer> symbRulesUsed = new HashMap<>();
        Map<String, Integer> symbMonomialRulesUsed = new HashMap<>();
        int symbRuleReductionsTotal = 0;
        int symbRuleReduction2q = 0;
        int egraphRuleReductionsTotal = 0;
        int egraphRuleReduction2q = 0;
        SymbolicThread symbolicThread = null;
        while(!q.isEmpty()) {
            iters++;

            if((System.nanoTime() - timesStart) / 1000000000.0 > timeout) {
                break;
            }

            CircuitDAG c = q.peek();
            if(comparator.compare(c, bestCircuit) < 0) {
                bestCircuit = c;
                bestCircuitOverall = bestCircuit;
                logger.debug("Current Best: {}", c.toQASM());
                logger.debug("Current Best Cost: {}", c.cost(Params.OPTIMIZATION_OBJECTIVE));
                logger.debug("Current Best Size: {}", c.totalGateCount());
            }

            c = dequeue(q, Params.TEMPERATURE, Params.OPTIMIZATION_OBJECTIVE, rand);
            if(ilp && iters % Params.ILP_PERIOD == 0) {
                c = IlpCompactor.compact(c);
            }
            List<String> rulesToUse = new ArrayList<>();

            if(q.size() > Params.QUEUE_SIZE) {
                PriorityQueue<CircuitDAG> newQ = new PriorityQueue<>(new CircuitComparator(Params.OPTIMIZATION_OBJECTIVE));
                while(newQ.size() != Params.QUEUE_SIZE) {
                    newQ.add(q.poll());
                }
                q = newQ;
            }

            if(Params.PRUNE_TEMPERATURE == 0) {
                while(rulesToUse.size() < Integer.min(rule_limit, validRules.size())) {
                    int index = rand.nextInt(validRules.size());
                    String r = validRules.get(index);
                    if(!rulesToUse.contains(r)) {
                        rulesToUse.add(r);
                    }
                }
            } else {
                CircuitDAG bestCircuitCopy = new CircuitDAG(bestCircuit);
                Map<String, Integer> rulesUsedCopy = new HashMap<>(rulesUsed);
                List<Integer> weights = validRules.stream().map(r -> scoreRule(r, bestCircuitCopy.countRulesApplied(r), rulesUsedCopy.getOrDefault(r, 0))).collect(Collectors.toList());
                int index = sampleSoftMax(weights, Params.TEMPERATURE, rand);
                if(!rulesToUse.contains(validRules.get(index))) {
                    rulesToUse.add(validRules.get(index));
                }
            }

            boolean concreteruleApplied = false;
            for(String rule: rulesToUse) {
                String[] splitRule = rule.split(" \\| ");
                CircuitDAG candidate = applyRule(c, QASMToDAGVisitor.parse(splitRule[1]), splitRule[0], false, rand);
                if(candidate != c) {
                    List<String> rulesApplied = new ArrayList<>(c.getRulesApplied());
                    rulesApplied.add(rule);
                    candidate.setRulesApplied(rulesApplied);
                    rulesUsed.put(rule, rulesUsed.getOrDefault(rule, 0) + 1);
                    concreteruleApplied = true;
                }

                if(candidate.cost(Params.OPTIMIZATION_OBJECTIVE) <= c.cost(Params.OPTIMIZATION_OBJECTIVE)) {
                    q.add(candidate);
                } else {
                    double acceptP = Math.min(1, Math.exp(-Params.TEMPERATURE * ((double) candidate.cost(Params.OPTIMIZATION_OBJECTIVE) / c.cost(Params.OPTIMIZATION_OBJECTIVE))));
                    if (rand.nextDouble() <= acceptP) {
                        q.add(candidate);
                    } else {
                        q.add(c);
                    }
                }
            }

            if(useSymb && concreteruleApplied) {

                if(symbolicThread == null) {
                    symbolicThread = new SymbolicThread(c, validMatrixRules, validMonomialRules, min_symb_size, max_symb_size, rand, this);
                    symbolicThread.setDaemon(true);
                    symbolicThread.start();
                } else {
                    if(!symbolicThread.isAlive()) {
                        CircuitDAG result = symbolicThread.getResult();
                        if(result != null) {
                            q.add(result);
                        }
                        symbolicThread = new SymbolicThread(c, validMatrixRules, validMonomialRules, min_symb_size, max_symb_size, rand, this);
                        symbolicThread.setDaemon(true);
                        symbolicThread.start();
                    }
                }
            }
        }

        logger.info("Final Gate Size: {}", bestCircuit.totalGateCount());
        logger.info("Final 2q: {}", bestCircuit.twoQGateCount());
        logger.debug("Final Cost: {}", bestCircuit.cost(Params.OPTIMIZATION_OBJECTIVE));
        logger.debug("Final Circuit: {}", bestCircuit.toQASM());
    }

    public void optimize_SA(EggGen.ConstrainedCircuit circuit, List<String> rules, List<String> longrules, List<MatrixConstrainedRule> symbRules, List<MononialRule> symbMonomialRules, int rule_limit, int symb_rule_limit, int min_symb_size, int max_symb_size, int timeout, boolean useSymb, List<String> commutative, boolean ilp, String gateset) {
        EggGen egraph = new EggGen();
        List<Rule> parsedRules = new ArrayList<>();
        for(String rule: rules) {
            logger.debug("Parsing rule: {}", rule);
            Rule parsedRule = QASMAstBuilder.parseRule(rule);
            logger.debug("Parsed rule: {}", parsedRule.toString());
            parsedRules.add(parsedRule);
        }
        for(String rule: commutative) {
            String merged = rule.replace(":ruleset wire", ":ruleset opt1");
            egraph.addRewrite(merged);
        }
        List<Rule> sizeDecreasingRules = new ArrayList<>();
        List<Rule> sizePreservingRules = new ArrayList<>();
        List<Rule> sizeIncreasingRules = new ArrayList<>();
        int k = Integer.getInteger("longrule.k", 1);
        int droppedIncreasing = 0;
        int keptIncreasingPattern = 0;
        for(Rule rule: parsedRules) {
            int lhssize = rule.lhs.gates.size();
            int rhssize = rule.rhs.gates.size();

            RuleReverser.Direction decision = RuleReverser.decide(rule, gateset);

            if (lhssize == rhssize) {
                sizePreservingRules.add(rule);
                continue;
            }
            if (decision == RuleReverser.Direction.DROP) {
                droppedIncreasing++;
                continue;
            }

            boolean forwardIsDec = lhssize > rhssize;
            if (decision == RuleReverser.Direction.FORWARD_ONLY
                    || decision == RuleReverser.Direction.BOTH) {
                if (forwardIsDec) sizeDecreasingRules.add(rule);
                else { sizeIncreasingRules.add(rule); keptIncreasingPattern++; }
            }

            if (decision == RuleReverser.Direction.REVERSE_ONLY
                    || decision == RuleReverser.Direction.BOTH) {
                if (RuleReverser.reverseIsFireable(rule)) {
                    Rule reversed = new Rule(rule.rhs, rule.lhs, rule.conditions);
                    if (forwardIsDec) sizeIncreasingRules.add(reversed);
                    else sizeDecreasingRules.add(reversed);
                }
            }
        }
        logger.debug("Rule selection (gateset={}): kept_increasing_pattern={} dropped_non_pattern_increasing={}",
                gateset, keptIncreasingPattern, droppedIncreasing);
        logger.debug("Size increasing rules: {}", sizeIncreasingRules.size());
        logger.debug("Size preserving rules: {}", sizePreservingRules.size());
        logger.debug("Size decreasing rules: {}", sizeDecreasingRules.size());
        logger.debug("Filtering Valid Long Rules.txt (reverse={})", LONGRULE_REVERSE);
        List<String> validLongRules = LONGRULE_REVERSE
                ? filterValidLongRules(longrules, gateset)
                : filterValidRules(longrules);
        logger.debug("Filtering Valid Symbolic Rules");
        List<MononialRule> validMonomialRules = filterValidMonomialRules(symbMonomialRules);
        validMonomialRules.clear();
        List<MatrixConstrainedRule> validMatrixRules = filterValidMatrixRules(symbRules);
        logger.debug("Valid Symb rules: {}", validMatrixRules.size());
        int sizeIncreasingRuleslimit = 700;
        int sizePreservingRuleslimit = 700;
        int sizeDecreasingRuleslimit = 700;

        logger.info("Starting SA optimization, timeout: {}s", timeout);
        logger.debug("Use ILP compaction: {}", ilp + (ilp ? " (init + every " + Params.ILP_PERIOD + " iters)" : ""));
        logger.info("Original Size: {}", circuit.circuit.gates.size());
        logger.info("Original 2q: {}", circuit.circuit.getTwoQubitsCount());
        logger.debug("Egraph rule size limit: {}", rule_limit);
        logger.debug("Symb rule size limit: {}", symb_rule_limit);
        logger.debug("Min symb size: {}", min_symb_size);
        logger.debug("Max symb size: {}", max_symb_size);
        EggGen.Circuit instantiatedCircuit = circuit.circuit.instantiate(new HashMap<>());
        CircuitDAG bestOptimized = QASMToDAGVisitor.parse(instantiatedCircuit.toQASM());
        bestCircuitOverall = bestOptimized;

        Random random = new Random(30);
        Random symbRandom = new Random(31);
        Random chunkRandom = new Random(32);
        long startTime = System.nanoTime();
        CircuitDAG optimized = bestOptimized;
        Map<MatrixConstrainedRule, Integer> symbRulesUsed = new HashMap<>();
        Map<MononialRule, Integer> symbMonomialRulesUsed = new HashMap<>();
        Map<String, Integer> rewriteRulesUsed = new HashMap<>();
        int symbRuleReductionsTotal = 0;
        int symbRuleReduction2q = 0;
        int egraphRuleReductionsTotal = 0;
        int egraphRuleReduction2q = 0;
        CircuitComparator circuitComparator = new CircuitComparator(Params.OPTIMIZATION_OBJECTIVE);
        PriorityQueue<CircuitDAG> q = new PriorityQueue<>(circuitComparator);
        if(ilp) {
            logger.debug("Performing initial ILP compaction...");
            optimized = IlpCompactor.compact(optimized);
            logger.debug("Initial ILP compaction done.");
        }
        q.add(optimized);
        SymbolicThread symbolicThread = null;
        final int EGRAPH_DEPTH_CEILING = Integer.getInteger("egraph.depth.ceiling", 13);
        int egraphDepth = 1;
        boolean egraphDepthFrozen = false;
        egraph.markSetupEnd();
        while(!q.isEmpty()) {
            logger.debug("Current Depth: {}", k);
            egraph.push();
            egraph.clearRules();
            if(circuitComparator.compare(q.peek(), bestOptimized) <= 0) {
                int prev2q = bestOptimized.twoQGateCount();
                bestOptimized = q.peek();
                bestCircuitOverall = bestOptimized;
                logger.debug("Q size: {}", q.size());
                logger.info("Time: {} minutes", String.format("%.3f", (System.nanoTime() - startTime) / 1000000000.0 / 60));
                logger.info("Best Optimized Size: {}", bestOptimized.totalGateCount());
                logger.info("Best Optimized 2q: {}", bestOptimized.twoQGateCount());
                logger.debug("Fidelity: {}", bestOptimized.fidelity());
                int cur2q = bestOptimized.twoQGateCount();
                if (cur2q < prev2q) {
                    double elapsedSec = (System.nanoTime() - startTime) / 1e9;
                    System.out.println(String.format(
                            "Progress 2q: %d (total %d) at %.1fs",
                            cur2q, bestOptimized.totalGateCount(), elapsedSec));
                }
            }

            CircuitDAG c = dequeue(q, Params.TEMPERATURE, Params.OPTIMIZATION_OBJECTIVE, random);
            if(ilp && k % Params.ILP_PERIOD == 0) {
                c = IlpCompactor.compact(c);
            }
            List<String> rulesToUse = new ArrayList<>();

            boolean randomRuleApplied = false;
            CircuitDAG glob_candidate = c;
            if (!validLongRules.isEmpty()) {
                java.util.Set<String> bannedReverses = new java.util.HashSet<>();
                for(int i = 0;i < k; i++) {
                    while(true) {
                        int index = random.nextInt(validLongRules.size());
                        String rule = validLongRules.get(index);
                        if (LONGRULE_REVERSE && bannedReverses.contains(rule)) {
                            continue;
                        }
                        String[] splitRule = rule.split(" \\| ");
                        CircuitDAG candidate = applyRule(glob_candidate, QASMToDAGVisitor.parse(splitRule[1]), splitRule[0], LONGRULE_REVERSE, random);
                        if(candidate != glob_candidate) {
                            if (LONGRULE_REVERSE && StringUtils.countMatches(splitRule[0], ";")
                                    == StringUtils.countMatches(splitRule[1], ";")) {
                                bannedReverses.add(reverseRuleString(rule));
                            }
                            String shortRule = rule.length() > 100 ? rule.substring(0, 100) + "..." : rule;
                            logger.info("[LONGRULE idx={}] {}", index, shortRule);
                            List<String> rulesApplied = new ArrayList<>(c.getRulesApplied());
                            rulesApplied.add(rule);
                            candidate.setRulesApplied(rulesApplied);
                            rewriteRulesUsed.put(rule, rewriteRulesUsed.getOrDefault(rule, 0) + 1);
                            randomRuleApplied = true;
                            glob_candidate = candidate;
                            q.add(candidate);
                            break;
                        }
                    }
                }
            }
            if(q.size() > Params.QUEUE_SIZE+1) {
                PriorityQueue<CircuitDAG> newQ = new PriorityQueue<>(new CircuitComparator(Params.OPTIMIZATION_OBJECTIVE));
                while(newQ.size() != Params.QUEUE_SIZE+1) {
                    newQ.add(q.poll());
                }
                q = newQ;
            }

            List<Integer> addedRules = new ArrayList<>();
            for(int i = 0; i < Math.min(sizePreservingRules.size(), sizePreservingRuleslimit); i++) {
                Rule r = sizePreservingRules.get(i);
                if(!addedRules.contains(i)) {
                    addedRules.add(i);
                    List<Rule.Equality> equalities = r.getEqualities();
                    String egg_rule = String.format("(%s %s %s %s :ruleset %s)", "birewrite", EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt1");
                    egraph.addRewrite(egg_rule);
                }
            }

            addedRules = new ArrayList<>();
            for(int i = 0; i < Math.min(sizeIncreasingRules.size(), sizeIncreasingRuleslimit); i++) {
                Rule r = sizeIncreasingRules.get(i);
                if(!addedRules.contains(i)) {
                    addedRules.add(i);
                    List<Rule.Equality> equalities = r.getEqualities();
                    String egg_rule = String.format("(%s %s %s %s :ruleset %s)", "rewrite", EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt1");
                    egraph.addRewrite(egg_rule);
                }
            }

            addedRules = new ArrayList<>();
            for(int i = 0; i < Math.min(sizeDecreasingRules.size(), sizeDecreasingRuleslimit); i++) {
                Rule r = sizeDecreasingRules.get(i);
                if(!addedRules.contains(i)) {
                    addedRules.add(i);
                    List<Rule.Equality> equalities = r.getEqualities();
                    String egg_rule = String.format("(%s %s %s %s :ruleset %s)", "rewrite", EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt1");
                    egraph.addRewrite(egg_rule);
                }
            }

            logger.info("[STAGE longrule k={}] 2q {} -> {}  total {} -> {}", k,
                    c.cost(CircuitDAG.OptObj.TWO_Q), glob_candidate.cost(CircuitDAG.OptObj.TWO_Q),
                    c.cost(CircuitDAG.OptObj.TOTAL), glob_candidate.cost(CircuitDAG.OptObj.TOTAL));
            CircuitDAG egraphInput = glob_candidate;
            if (useSymb && symbolicThread != null && !symbolicThread.isAlive()) {
                CircuitDAG symbResult = symbolicThread.getResult();
                symbolicThread = null;
                if (symbResult != null) {
                    logger.info("SYMB applied (pre-eqsat): 2q {} total {}",
                            symbResult.cost(CircuitDAG.OptObj.TWO_Q),
                            symbResult.cost(CircuitDAG.OptObj.TOTAL));
                    egraphInput = symbResult;
                }
            } else if (useSymb && symbolicThread != null) {
                logger.debug("Symbolic thread still running; e-saturating popped circuit");
            }
            EggGen.Circuit parsedFull = QASMAstBuilder.parse(egraphInput.toQASM());
            int totalGatesForEgraph = parsedFull.gates.size();
            CircuitDAG candidateDAG;
            boolean stageEgraphOk = true;
            egraph.resetTimeoutFlag();
            if (totalGatesForEgraph > Params.EGRAPH_CHUNK_THRESHOLD) {
                int window = Math.min(Params.EGRAPH_CHUNK_SIZE, totalGatesForEgraph);
                int maxStart = totalGatesForEgraph - window;
                int start = (maxStart > 0) ? chunkRandom.nextInt(maxStart + 1) : 0;
                int end = start + window;
                logger.debug("Chunked egraph: {} gates -> window [{}, {}) of size {}",
                        totalGatesForEgraph, start, end, window);
                EggGen.Circuit chunk = new EggGen.Circuit(new ArrayList<>(parsedFull.gates.subList(start, end)));
                List<EggGen.Gate> optimizedGates = new ArrayList<>(parsedFull.gates.subList(0, start));
                egraph.push();
                try {
                    egraph.push();
                    String chunkName = egraph.addCircuit(chunk);
                    egraph.runN("const", 1);
                    egraph.runN("merge", 1);
                    EggGen.Circuit temp = egraph.extractCircuit(chunkName);
                    egraph.pop();
                    logger.debug("Rotation Merged");
                    chunkName = egraph.addCircuit(temp);
                    egraph.runN("opt1", egraphDepth);
                    EggGen.Circuit optChunk = egraph.extractCircuit(chunkName);
                    if (optChunk != null && optChunk.gates != null) {
                        optimizedGates.addAll(optChunk.gates);
                    } else {
                        optimizedGates.addAll(chunk.gates);
                    }
                } catch (Exception ex) {
                    logger.warn("Chunk window [{}, {}) egraph failed: {} - keeping original window", start, end, ex.getClass().getSimpleName());
                    optimizedGates.addAll(chunk.gates);
                } finally {
                    egraph.pop();
                }
                optimizedGates.addAll(parsedFull.gates.subList(end, totalGatesForEgraph));
                EggGen.Circuit combined = new EggGen.Circuit(optimizedGates);
                candidateDAG = QASMToDAGVisitor.parse(combined.toQASM());
                logger.debug("Chunked ESAT Candidate Cost: " + candidateDAG.cost(Params.OPTIMIZATION_OBJECTIVE));
            } else {
                candidateDAG = egraphInput;
                try {
                    egraph.push();
                    String name = egraph.addCircuit(parsedFull);
                    egraph.runN("const", 1);
                    egraph.runN("merge", 1);
                    EggGen.Circuit temp = egraph.extractCircuit(name);
                    egraph.pop();

                    name = egraph.addCircuit(temp);
                    logger.debug("Rotation Merged");
                    egraph.runN("opt1", egraphDepth);
                    EggGen.Circuit candidate = egraph.extractCircuit(name);
                    candidateDAG = QASMToDAGVisitor.parse(candidate.toQASM());
                    logger.debug("ESAT Candidate Cost: " + candidateDAG.cost(Params.OPTIMIZATION_OBJECTIVE));
                } catch (Exception ex) {
                    logger.warn("Non-chunked egraph failed: {} - keeping input circuit",
                            ex.getClass().getSimpleName());
                    candidateDAG = egraphInput;
                }
            }
            if (egraph.timedOut()) {
                stageEgraphOk = false;
                candidateDAG = egraphInput;
            }
            if (!stageEgraphOk) {
                if (egraphDepth > 1) {
                    int prev = egraphDepth;
                    egraphDepth = egraphDepth - 1;
                    logger.info("[EGRAPH-DEPTH] timeout at depth {}, decreasing to {}", prev, egraphDepth);
                } else {
                    logger.info("[EGRAPH-DEPTH] timeout at depth 1 (floor), holding");
                }
                egraphDepthFrozen = true;
            } else if (!egraphDepthFrozen) {
                if (egraphDepth >= EGRAPH_DEPTH_CEILING) {
                    egraphDepthFrozen = true;
                    logger.info("[EGRAPH-DEPTH] hit ceiling, freezing dynamic max at {}", egraphDepth);
                } else {
                    egraphDepth++;
                    logger.debug("[EGRAPH-DEPTH] stage ok, growing dynamic max to {}", egraphDepth);
                }
            }
            logger.info("[STAGE egglog k={}] 2q {} -> {}  total {} -> {}", k,
                    egraphInput.cost(CircuitDAG.OptObj.TWO_Q), candidateDAG.cost(CircuitDAG.OptObj.TWO_Q),
                    egraphInput.cost(CircuitDAG.OptObj.TOTAL), candidateDAG.cost(CircuitDAG.OptObj.TOTAL));
            q.add(candidateDAG);
            glob_candidate = candidateDAG;

            if(useSymb && symbolicThread == null) {
                logger.debug("Spawning symbolic thread on eqsat result");
                symbolicThread = new SymbolicThread(glob_candidate, validMatrixRules, validMonomialRules, min_symb_size, max_symb_size, symbRandom, this);
                symbolicThread.setDaemon(true);
                symbolicThread.start();
            }

            if (!egraph.timedOut()) {
                egraph.pop();
            }
            k++;

            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            if(duration / 1000000000 > timeout) {
                logger.info("Timeout reached, stopping optimization");
                break;
            }
        }
        logger.info("Final Gate Size: {}", bestOptimized.cost(CircuitDAG.OptObj.TOTAL));
        logger.info("Final 2q: {}", bestOptimized.cost(CircuitDAG.OptObj.TWO_Q));
        logger.debug("Final Circuit: {}", bestOptimized.toQASM());
        logger.debug("Symb Rule Obj Reductions Total: {}", symbRuleReductionsTotal);
        logger.debug("Egraph Rule Obj Reductions Total: {}", egraphRuleReductionsTotal);
    }

    private boolean checkLinearCombination(EggGen.Circuit circuit, List<SymbolicSolve.SparseMatrix> basis, List<Integer> subspace, Map<String, Expr> symbolMap) throws IOException, InterruptedException {
        String jsonString = solver.circuitToJson(circuit.gates, circuit.getMaxQubits()+1);
        String jsonM = "[";
        List<String> basisStrList = new ArrayList<>();
        for(SymbolicSolve.SparseMatrix m : basis) {
            String basis_str = m.to_json_string();
            basisStrList.add(basis_str);
        }
        jsonM = jsonM + String.join(",", basisStrList) + "]";

        String subspaceStr = subspace.stream().map(String::valueOf).collect(Collectors.joining(","));
        subspaceStr = "[" + subspaceStr + "]";

        String symbolMapStr = symbolMap.entrySet().stream()
            .map(entry -> "\"" + entry.getKey() + "\": " + CircuitDAG.eval(entry.getValue()))
            .collect(Collectors.joining(","));
        symbolMapStr = "{" + symbolMapStr + "}";

        long basisT0 = System.currentTimeMillis();
        String output = (Params.SYMB_APPROX_EPS != null)
                ? solver.isSubspaceLinear(jsonString, jsonM, subspaceStr, symbolMapStr, Params.SYMB_APPROX_EPS)
                : solver.isSubspaceLinear(jsonString, jsonM, subspaceStr, symbolMapStr);
        SYMB_BASIS_CALLS.incrementAndGet();
        SYMB_BASIS_MS.addAndGet(System.currentTimeMillis() - basisT0);

        logger.debug("Output: {}", output.trim());

        boolean pass = output.trim().contains("True");
        if (pass) SYMB_BASIS_PASS.incrementAndGet();
        return pass;
    }

    public static List<Map<boolean[], boolean[]>> parseConstraints(String stringConstraints) {
        List<Map<boolean[], boolean[]>> constraints = new ArrayList<>();
        String[] splitOuter = stringConstraints.split("},");
        for (String out : splitOuter) {
            String[] splitInner = out.split("],");
            Map<boolean[], boolean[]> constraint = new HashMap<>();
            for (String in : splitInner) {
                in = in.replace("]=[", ", ");
                in = in.replace("[", "");
                in = in.replace("]", "");
                in = in.replace("{", "");
                in = in.replace("}", "");
                in = in.trim();

                boolean[] key = new boolean[2];
                boolean[] val = new boolean[2];
                String[] bools = in.split(", ");

                key[0] = Boolean.parseBoolean(bools[0]);
                key[1] = Boolean.parseBoolean(bools[1]);
                val[0] = Boolean.parseBoolean(bools[2]);
                val[1] = Boolean.parseBoolean(bools[3]);

                constraint.put(key, val);
            }
            constraints.add(constraint);
        }

        return constraints;
    }

    static void writeOptimizedQasm(CircuitDAG best, String benchmarkFile, String outputDir) {
        if (outputDir == null || outputDir.isEmpty() || best == null) return;
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(outputDir);
            java.nio.file.Files.createDirectories(dir);
            String stem = new java.io.File(benchmarkFile).getName();
            int dot = stem.lastIndexOf('.');
            if (dot > 0) stem = stem.substring(0, dot);
            java.nio.file.Path out = dir.resolve(stem + "_optimized.qasm");
            java.nio.file.Files.writeString(out, best.toQASM());
        } catch (Exception e) {
            System.err.println("writeOptimizedQasm failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        List<MatrixConstrainedRule> symbRules = new ArrayList<>();
        Options options = new Options();

        Option benchmarkO = new Option("b", "benchmark", true, "benchmark file path");
        benchmarkO.setRequired(true);
        options.addOption(benchmarkO);

        Option rulesO = new Option("r", "rule", true, "ruleset file path");
        rulesO.setRequired(true);
        options.addOption(rulesO);

        Option symbRulesO = new Option("sr", "symbrule", true, "symb ruleset file path");
        Option timeout = new Option("t", "timeout", true, "timeout");
        symbRulesO.setRequired(false);
        options.addOption(symbRulesO);

        Option symbRulesMonomial = new Option("sm", "monomial", true, "monomial");
        symbRulesMonomial.setRequired(false);
        options.addOption(symbRulesMonomial);

        timeout.setRequired(false);
        options.addOption(timeout);

        Option mode = new Option("m", "mode", true, "mode");
        mode.setRequired(false);
        options.addOption(mode);

        Option usesymb = new Option("symb", "usesymb", true, "usesymb");
        usesymb.setRequired(false);
        options.addOption(usesymb);

        Option useilp = new Option("ilp", "useilp", true, "enable ILP compaction each iteration (true/false)");
        useilp.setRequired(false);
        options.addOption(useilp);

        Option minSymbSize = new Option("minsymb", "minSymbSize", true, "minSymbSize");
        minSymbSize.setRequired(false);
        options.addOption(minSymbSize);

        Option maxSymbSize = new Option("maxsymb", "maxSymbSize", true, "maxSymbSize");
        maxSymbSize.setRequired(false);
        options.addOption(maxSymbSize);

        Option gateset = new Option("g", "gateset", true, "gateset");
        gateset.setRequired(true);
        options.addOption(gateset);

        Option longrulesO = new Option("lr", "longrule", true, "longruleset file path");
        longrulesO.setRequired(false);
        options.addOption(longrulesO);

        Option quietO = new Option("q", "quiet", false, "quiet: only final 2q + total gates + qasm save");
        quietO.setRequired(false);
        options.addOption(quietO);

        Option outdirO = new Option("o", "out", true, "output directory for the optimized qasm");
        outdirO.setRequired(false);
        options.addOption(outdirO);

        Option approxO = new Option("approx", "approxEps", true,
                "approximate symbolic-match tolerance (e.g. 1e-3); omit for exact matching");
        approxO.setRequired(false);
        options.addOption(approxO);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error("{}", e.getMessage());
            formatter.printHelp("Optimizer", options);
            System.exit(1);
            return;
        }

        boolean quiet = cmd.hasOption("quiet");
        if (quiet) {
            ch.qos.logback.classic.Logger root =
                    (ch.qos.logback.classic.Logger)
                            org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(ch.qos.logback.classic.Level.OFF);
        }
        String outputDir = cmd.getOptionValue("out");
        if (cmd.getOptionValue("approxEps") != null) {
            Params.SYMB_APPROX_EPS = Double.valueOf(cmd.getOptionValue("approxEps"));
            logger.info("Approximate symbolic matching ON, eps = {}", Params.SYMB_APPROX_EPS);
        }

        List<String> commutative = new ArrayList<>();
        String g = cmd.getOptionValue("gateset");
        FileReader fr = new FileReader("rules_" + g + ".txt", StandardCharsets.UTF_8);
        try (BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                commutative.add(line);
            }
        }

        String benchmarkFile = cmd.getOptionValue("benchmark");
        logger.info("Benchmark: {}", benchmarkFile);
        String rulesFile = cmd.getOptionValue("rule");
        String longrulesFile = cmd.getOptionValue("longrule");
        String symrulesFile = cmd.getOptionValue("symbrule");
        String symrulesMonomialFile = cmd.getOptionValue("monomial");

        String modeStr = cmd.getOptionValue("mode");
        int timeoutint = Integer.valueOf(cmd.getOptionValue("timeout"));
        boolean useSymb = Boolean.valueOf(cmd.getOptionValue("usesymb"));
        boolean useIlp = Boolean.valueOf(cmd.getOptionValue("useilp"));
        List<String> rules = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                rules.add(line);
            }
        }

        List<String> longrules = new ArrayList<>();
        if(longrulesFile != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(longrulesFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    longrules.add(line);
                }
            }
        }
        List<MononialRule> symbRulesMonomials = new ArrayList<>();

        if(symrulesMonomialFile != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(symrulesMonomialFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] comp = line.split("\\|");
                    String lhs = comp[0].trim();
                    String rhs = comp[1].trim();
                    String permutation = comp[2].trim();
                    List<Map<boolean[], boolean[]>> constraints = parseConstraints(permutation);
                    symbRulesMonomials.add(new MononialRule(lhs, rhs, constraints));
                }
            }
        }

        if(symrulesFile != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(symrulesFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                String[] comp = line.split("\\|");
                String lhs = comp[0];
                String rhs = comp[1];
                String type = comp[2];
                String matrix = comp[3];

                Pattern matrixP = Pattern.compile("\\[(.*)\\]");
                Matcher matcher = matrixP.matcher(matrix);
                matcher.matches();
                String matricesStr = matcher.group(1);
                String[] matrices = matricesStr.split("::");
                List<SymbolicSolve.SparseMatrix> matrixList = new ArrayList<>();
                for(String m: matrices) {
                        List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                        if(m.startsWith("Matrix(")) {
                            String content = m.substring("Matrix(".length(), m.length() - 1);
                            String[] entries = content.split(";");
                            int rows = Integer.valueOf(entries[0]);
                            int cols = Integer.valueOf(entries[1]);
                            for(int i = 2; i < entries.length; i++) {
                                String entry = entries[i].substring(1, entries[i].length() - 1);
                                String[] elems = entry.split(", ");
                                int row = Integer.valueOf(elems[0].trim());
                                int col = Integer.valueOf(elems[1].trim());
                                SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                                sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                            }
                            matrixList.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                        }
                }
                symbRules.add(new MatrixConstrainedRule(lhs, rhs, matrixList, type));
                }
            }
        }

        String circuitString = new String(Files.readAllBytes(Paths.get(benchmarkFile)));

        EggGen.Circuit circuit = QASMAstBuilder.parse(circuitString);

        final int originalTotal = circuit.gates.size();
        final int originalTwoQ  = circuit.getTwoQubitsCount();

        Optimizer optimizer = new Optimizer();
        new Thread(() -> {
            try {
                Thread.sleep(timeoutint * 1000);
                if (optimizer.bestCircuitOverall != null) {
                    if (!quiet) {
                        logger.info("Timeout reached, printing best circuit so far:");
                        logger.info("Original Gate Size: {}", originalTotal);
                        logger.info("Original 2q: {}", originalTwoQ);
                        logger.info("Final Gate Size: {}", optimizer.bestCircuitOverall.totalGateCount());
                        logger.info("Final 2q: {}", optimizer.bestCircuitOverall.twoQGateCount());
                        logger.info("Symbolic rules applied: {}", SYMB_APPLIED.get());
                        logger.info(symbStatsLine());
                        logger.debug("Final Cost: {}", optimizer.bestCircuitOverall.cost(Params.OPTIMIZATION_OBJECTIVE));
                        logger.debug("Final Fidelity: {}", optimizer.bestCircuitOverall.fidelity());
                        logger.debug("Final Circuit: {}", optimizer.bestCircuitOverall.toQASM());
                    } else {
                        System.out.println("Original Gate Size: " + originalTotal);
                        System.out.println("Original 2q: " + originalTwoQ);
                        System.out.println("Final Gate Size: " + optimizer.bestCircuitOverall.totalGateCount());
                        System.out.println("Final 2q: " + optimizer.bestCircuitOverall.twoQGateCount());
                        System.out.println("Symbolic rules applied: " + SYMB_APPLIED.get());
                        System.out.println(symbStatsLine());
                    }
                    writeOptimizedQasm(optimizer.bestCircuitOverall, benchmarkFile, outputDir);
                }
                System.exit(0);
            } catch (InterruptedException ignored) {}
        }).start();

        if(modeStr.equals("SA")) {
            int minSymb = cmd.getOptionValue("minSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("minSymbSize")) : 10;
            int maxSymb = cmd.getOptionValue("maxSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("maxSymbSize")) : 30;
            optimizer.optimize_SA(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), rules, longrules, symbRules, symbRulesMonomials, 1, 1, minSymb, maxSymb, timeoutint, useSymb, commutative, useIlp, g);
        } else if(modeStr.equals("BEAMN")) {
            int minSymb = cmd.getOptionValue("minSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("minSymbSize")) : 10;
            int maxSymb = cmd.getOptionValue("maxSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("maxSymbSize")) : 20;
            Comparator<EggGen.ConstrainedCircuit> comparator = new Comparator<EggGen.ConstrainedCircuit>() {
                public int compare(EggGen.ConstrainedCircuit a, EggGen.ConstrainedCircuit b) {
                    return a.circuit.getTwoQubitsCount() - b.circuit.getTwoQubitsCount();
                }
            };
            EggGen.Circuit instantiatedCircuit = circuit.instantiate(new HashMap<>());
            optimizer.optimize_BEAM_normal(QASMToDAGVisitor.parse(instantiatedCircuit.toQASM()), rules, symbRules, symbRulesMonomials, 10, 1, 1, minSymb, maxSymb, timeoutint, useSymb, useIlp);
        }
    }
}
