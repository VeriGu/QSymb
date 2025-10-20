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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Triple;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedMultigraph;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.HashSet;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.lca.NaiveLCAFinder;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import java.util.stream.Collectors;

public class Optimizer {
    private SymbolicSolve solver;
    public Optimizer() {
        solver = new SymbolicSolve(new Random());
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
                return new EggGen.X(node.getQubits().get(0));
            case "sx":
                return new EggGen.SX(node.getQubits().get(0));
            case "cx":
                return new EggGen.CX(node.getQubits().get(0), node.getQubits().get(1));
            case "cz":
                return new EggGen.CZ(node.getQubits().get(0), node.getQubits().get(1));
            //check in egggen for other gates
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
                        if (pattern.getEdgeTarget(pattE).getAngles() != null) {
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
                            if (pattern.getEdgeSource(pattE).getAngles() != null) {
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
                if (start.getAngles() != null) {
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
            // qubits not in replaceLeaves should not have been in replaceRoots and therefore were connected already to decPatternLeaves
            circuit.addEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), pattern.getEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), qubit));
        }
    }

    private String replaceAngles(String replace, Map<String, Expr> angleMap) {
        for (String angle : angleMap.keySet()) {
            replace = replace.replace(angle, eval(angleMap.get(angle)).toString());
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
                // TODO improve
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


    public CircuitDAG symbolicMatch(Circuit circuit, String rule, String rhs, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis) {
        CircuitDAG dag = QASMToDAGVisitor.parse(circuit.getQasmString());
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
            CircuitDAG symbdag = QASMToDAGVisitor.parse(CircuitTranslator.translateBack(constrainedSymblhs, symblhs.getMaxQubits()+1).getCircuit().getQasmString());
            if (!GraphTests.isConnected(symbdag.getDAG())) {
                System.out.println("Symbolic LHS is not connected");
                return null;
            }
            Map<String, String> qubitMap = new HashMap<>();
            Map<String, String> reverseMap = new HashMap<>();
            List<Node> matchedNodes = matchBefore(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, maxSymbSize, basis, qubitMap, reverseMap);
            if(matchedNodes == null) {
                System.out.println("No Match Before found");
                return null;
            }
            for(Node node : matchedNodes) {
                System.out.println("Matched Successfully: " + node.getId());
            }

            Pattern pattern2 = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+(.*)\\)");
            Matcher matcher2 = pattern2.matcher(rhs);
            String removedRhs = null;
            if (matcher2.find()) {
                removedRhs = matcher2.group(1).trim();
            }
            removedRhs = removedRhs.replaceAll("\\bc\\b", "(Nil)");
            removedRhs = removedRhs.replaceAll("q\\d+", "(Q \"$0\")"); 
            for(String angle: angleMap.keySet()) {
                removedRhs = removedRhs.replace(angle, angleMap.get(angle).toEggString());
            }
            System.out.println("Removed RHS: " + removedRhs);
           
            EggGen.Circuit symbrhs = EggAstBuilder.parseCircuit(removedRhs);
            EggGen.Circuit symbrhsCan = EggGen.canonicalizeCircuit(symbrhs, reverseMap);
            List<EggGen.Gate> gates = new ArrayList<>(symbrhsCan.gates);
            List<Node> matchedsymb = matchedNodes.subList(symblhs.gates.size(), matchedNodes.size());
            List<EggGen.Gate> lhsgates = nodesToGates(matchedNodes);
            EggGen.Circuit lhsCircuit = new EggGen.Circuit(lhsgates);
            EggGen.Circuit lhsCircuitCan = EggGen.canonicalizeCircuit(lhsCircuit, reverseMap);
            EggGen.ConstrainedCircuit lhsConst = new EggGen.ConstrainedCircuit(lhsCircuitCan, new EggGen.Permutation(new ArrayList<>()));
            Circuit lhsCircuitTrans = CircuitTranslator.translateBack(lhsConst, lhsCircuitCan.getMaxQubits()+1).getCircuit();
            CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuitTrans.getQasmString());
            
            List<EggGen.Gate> matchedgates = nodesToGates(matchedsymb);
            gates.addAll(0, matchedgates);
            EggGen.Circuit combinedCircuit = new EggGen.Circuit(gates);
            EggGen.Circuit canonized = EggGen.canonicalizeCircuit(combinedCircuit, reverseMap);
            EggGen.ConstrainedCircuit combinedConst = new EggGen.ConstrainedCircuit(canonized, new EggGen.Permutation(new ArrayList<>()));
            ConstrainedCircuit combinedCircuitConst = CircuitTranslator.translateBack(combinedConst, combinedCircuit.getMaxQubits()+1);
            
            System.out.println("Combined LHS Circuit: " + lhsCircuitTrans.getQasmString());
            System.out.println("Combined RHS Circuit: " + combinedCircuitConst.getCircuit().getQasmString());
            Random rand = new Random();
            CircuitDAG result = applyRule(dag, lhsDag, combinedCircuitConst.getCircuit().getQasmString(), true, rand);
            for(Node node : result.nodes()) {
                System.out.println("Result Node: " + node.getId());
            }
            String qasm = result.toQASM();
            System.out.println("Result: " + qasm);
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
            CircuitDAG symbdag = QASMToDAGVisitor.parse(CircuitTranslator.translateBack(constrainedSymblhs, symblhs.getMaxQubits()+1).getCircuit().getQasmString());
            if (!GraphTests.isConnected(symbdag.getDAG())) {
                System.out.println("Symbolic RHS is not connected");
                return null;
            }
            Map<String, String> qubitMap = new HashMap<>();
            Map<String, String> reverseMap = new HashMap<>();
            List<Node> matchedNodes = matchAfter(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, maxSymbSize, basis, qubitMap, reverseMap);
            for(Node node : matchedNodes) {
                System.out.println("Matched node: " + node.getId());
            }

            matcher = pattern.matcher(rhs);
            String removedRhs = null;
            if (matcher.find()) {
                String matched = matcher.group();
                removedRhs = rhs.replace(matched, "(Nil)");
                removedRhs = removedRhs.replaceAll("q\\d+", "(Q \"$0\")"); 
                //removedRhs = removedRhs.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
                for(String angle: angleMap.keySet()) {
                    removedRhs = removedRhs.replace(angle, angleMap.get(angle).toEggString());
                }
            }

            EggGen.Circuit symbrhs = EggAstBuilder.parseCircuit(removedRhs);
            EggGen.Circuit symbrhsCan = EggGen.canonicalizeCircuit(symbrhs, reverseMap);
            List<EggGen.Gate> gates = new ArrayList<>(symbrhsCan.gates);
            List<Node> matchedsymb = matchedNodes.subList(0, matchedNodes.size() - symblhs.gates.size());
            List<EggGen.Gate> matchedgates = nodesToGates(matchedsymb);
            
            List<EggGen.Gate> lhsgates = nodesToGates(matchedNodes);
            EggGen.Circuit lhsCircuit = new EggGen.Circuit(lhsgates);
            EggGen.Circuit lhsCircuitCan = EggGen.canonicalizeCircuit(lhsCircuit, reverseMap);
            EggGen.ConstrainedCircuit lhsConst = new EggGen.ConstrainedCircuit(lhsCircuitCan, new EggGen.Permutation(new ArrayList<>()));
            Circuit lhsCircuitTrans = CircuitTranslator.translateBack(lhsConst, lhsCircuitCan.getMaxQubits()+1).getCircuit();
            CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuitTrans.getQasmString());
            gates.addAll(matchedgates);
            EggGen.Circuit combinedCircuit = new EggGen.Circuit(gates);
            EggGen.Circuit canonized = EggGen.canonicalizeCircuit(combinedCircuit, reverseMap);
            EggGen.ConstrainedCircuit combinedConst = new EggGen.ConstrainedCircuit(canonized, new EggGen.Permutation(new ArrayList<>()));
            ConstrainedCircuit combinedCircuitConst = CircuitTranslator.translateBack(combinedConst, combinedCircuit.getMaxQubits()+1);

            System.out.println("Combined LHS Circuit: " + lhsCircuitTrans.getQasmString());
            System.out.println("Combined RHS Circuit: " + combinedCircuitConst.getCircuit().getQasmString());
            Random rand = new Random();
            CircuitDAG result = applyRule(dag, lhsDag, combinedCircuitConst.getCircuit().getQasmString(), true, rand);
            for(Node node : result.nodes()) {
                System.out.println("Result Node: " + node.getId());
            }
            String qasm = result.toQASM();
            System.out.println("Result: " + qasm);
            return result;
        }
    }

    private List<Node> matchAfter(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap) {
        List<Node> roots = symbdag.getCircuitRoots();
        Node patternRoot = roots.get(0);
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
                if(!node.isGate()) {
                    continue;
                }
                System.out.println("Node: " + node.getId());

                Set<String> trackedIntersection = new HashSet<>(trackedQubits);
                if(isFirst) {
                    trackedIntersection.addAll(node.getQubits());
                    isFirst = false;
                } else {
                    trackedIntersection.retainAll(node.getQubits());
                }
                if(!trackedIntersection.isEmpty()) {
                    trackedQubits.addAll(trackedIntersection);
                    if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                        break;
                    }

                    boolean match = false;
                    if(!blockedQubits.contains(node.getQubits().get(0)) && node.getId().equals(patternRoot.getId())) {
                        System.out.println("Matched Root: " + node.getId());
                        if(!patternRoot.getAngles().isEmpty()) {
                            if(matchAngles(node, patternRoot, angleMap)) {
                                System.out.println("Matched Angles");
                                Node nextA = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                                int t = 1;
                                boolean found = true;
                                    
                                while(!nextA.isSinkQubit()) {
                                    boolean foundInner = false;
                                    for(Node node2: layers.get(i + t)) {
                                        System.out.println("Concrete Node: " + node2.getId());
                                        System.out.println("Next A: " + nextA.getId());
                                        if(node2.isGate() && node2.getId().equals(nextA.getId()) && node2.getQubits().equals(node.getQubits())) {
                                            if(!nextA.getAngles().isEmpty()) {
                                                if(matchAngles(node2, nextA, angleMap)) {
                                                    for(int j = 0; j < node2.getQubits().size(); j++) {
                                                        if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                            if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                                found = false;
                                                                continue;
                                                            }
                                                            if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                                if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                    found = false;
                                                                    continue;
                                                                }
                                                            }
                                                        } else {
                                                            if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                                if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                    found = false;
                                                                    continue;
                                                                }
                                                            }
                                                            qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                        }
                                                    }
                                                    foundInner = true;
                                                    System.out.println("Found Inner: " + node2.getId());
                                                    break;
                                                }
                                                
                                            }
                                            else {
                                                for(int j = 0; j < node2.getQubits().size(); j++) {
                                                    if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                        if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                            found = false;
                                                            continue;
                                                        }
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                found = false;
                                                                continue;
                                                            }
                                                        }
                                                    } else {
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                found = false;
                                                                continue;
                                                            }
                                                        }
                                                        qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                    }
                                                }
                                                foundInner = true;
                                                System.out.println("Found Inner: " + node2.getId());
                                                break;
                                            }
                                        }
                                    }
                                    if(!foundInner) {
                                        found = false;
                                        break;
                                    }
                                    nextA = Graphs.successorListOf(symbdag.getDAG(), nextA).get(0);
                                    t++;
                                }
                                if(found) {
                                    match = true;
                                }
                            }
                        } else {
                            System.out.println("Matched Angles");
                            Node nextA = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                            int t = 1;
                            boolean found = true;
                            while(!nextA.isSinkQubit()) {
                                boolean foundInner = false;
                                for(Node node2: layers.get(i + t)) {
                                    if(node2.isGate() && node2.getId().equals(nextA.getId()) && node2.getQubits().equals(node.getQubits())) {
                                        if(nextA.getAngles() != null) {
                                            if(matchAngles(node2, nextA, angleMap)) {
                                                for(int j = 0; j < node2.getQubits().size(); j++) {
                                                    if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                        if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                            found = false;
                                                            continue;
                                                        }
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                found = false;
                                                                continue;
                                                            }
                                                        }
                                                    } else {
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                found = false;
                                                                continue;
                                                            }
                                                        }
                                                        qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                    }
                                                }
                                                foundInner = true;
                                                System.out.println("Found Inner: " + node2.getId());
                                                break;
                                            }
                                        }
                                        else {
                                            for(int j = 0; j < node2.getQubits().size(); j++) {
                                                if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                    if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                        found = false;
                                                        continue;
                                                    }
                                                    if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                        if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                            found = false;
                                                            continue;
                                                        }
                                                    }
                                                } else {
                                                    if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                        if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                            found = false;
                                                            continue;
                                                        }
                                                    }
                                                    qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                }
                                            }
                                            foundInner = true;
                                            System.out.println("Found Inner: " + node2.getId());
                                            break;
                                        }
                                    }
                                }
                                if(!foundInner) {
                                    found = false;
                                    break;
                                }
                                nextA = Graphs.successorListOf(symbdag.getDAG(), nextA).get(0);
                                t++;
                            }
                            if(found) {
                                System.out.println("Matched the entire pattern");
                                match = true;
                            }
                        }
                    }

                    if(match) {
                        Circuit symbCirc = opsToCircuit(symb);
                        EggGen.ConstrainedCircuit symbCircConst = CircuitTranslator.translate(symbCirc);
                        if(symbCirc.getUsedQubits().size() <= maxSymbQubits) {
                            EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, qubitMap);
                            System.out.println("Canonicalized Circuit: " + canonicalizedCirc.toEggString());
                            try {
                                List<Integer> subspace = new ArrayList<>();
                                subspace.add(0);
                                subspace.add(1);
                                System.out.println("Subspace: " + subspace);
                                if(checkLinearCombination(canonicalizedCirc, basis, subspace, angleMap)) {
                                    symbToReplace.add(node);
                                    Node nextA = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                                    Node circNextA = Graphs.successorListOf(dag.getDAG(), node).get(0);
                                    while(!nextA.isSinkQubit()) {
                                        symbToReplace.add(circNextA);
                                        circNextA = Graphs.successorListOf(dag.getDAG(), circNextA).get(0);
                                        nextA = Graphs.successorListOf(symbdag.getDAG(), nextA).get(0);
                                    }
                                    System.out.println("S matches the basis");
                                    return symbToReplace;
                                } else {
                                    System.out.println("S does not match the basis");
                                }
                            }
                            catch (IOException | InterruptedException e) {
                                e.printStackTrace();
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
                                //block target qubit
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
        return (eval(angle1) % (4 * Math.PI)) == (eval(angle2) % (4 * Math.PI));
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
                return null; // stupid hack to make the compiler happy ugh
        }
    }

    private boolean matchAngles(Node circN, Node patternN, Map<String, Expr> angleMap) {
        Map<String, Expr> tempAngleMap = new HashMap<>();
        tempAngleMap.putAll(angleMap);
        boolean matchAngles = true;
        int i = 0;
        for (Expr angle : patternN.getAngles()) {
            String key = angle.toString();
            if (key.contains("theta")) {
                if (tempAngleMap.containsKey(key)) {
                    if (!sameAngle(tempAngleMap.get(key), circN.getAngles().get(i))) {
                        matchAngles = false;
                        break;
                    }
                } else {
                    tempAngleMap.put(key, circN.getAngles().get(i));
                }
            } else {
                if (!sameAngle(angle, circN.getAngles().get(i))) {
                    matchAngles = false;
                    break;
                }
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

    public List<Node> matchBefore(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap) {
        List<Node> roots = symbdag.getCircuitRoots();
        Node patternRoot = roots.get(0);
        List<List<Node>> layers = dag.topoSort();

        for(int i = 0; i < layers.size(); i++) {
            List<Node> layer = layers.get(i);
            for(Node node : layer) {
                qubitMap.clear();
                reverseMap.clear();
                if(node.isGate() && node.getId().equals(patternRoot.getId())) {
                    if(patternRoot.getAngles() != null) {
                        if(!matchAngles(node, patternRoot, angleMap)) {
                            continue;
                        }
                    }
                    System.out.println("matched node: " + node.getId());
                    for(int j = 0; j < node.getQubits().size(); j++) {
                        qubitMap.put(node.getQubits().get(j), patternRoot.getQubits().get(j));
                        reverseMap.put(patternRoot.getQubits().get(j), node.getQubits().get(j));
                    }
                    //System.out.println("Qubit Map: " + qubitMap);
                    Node next = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                    int s = 1;
                    boolean foundOutter = true;
                    while(!next.isSinkQubit()) {
                        boolean found = false;
                        for(Node circN3: layers.get(i + s)) {
                            System.out.println("Next Pattern: " + next.getId());
                            System.out.println("Next Concrete: " + circN3.getId());
                            System.out.println("Pattern Qubits: " + next.getQubits());
                            System.out.println("Concrete Qubits: " + circN3.getQubits());
                            if(circN3.isGate() && circN3.getId().equals(next.getId())) {
                                System.out.println("Intermediate Matched Node: " + circN3.getId());
                                if(patternRoot.getAngles() != null) {
                                    if(matchAngles(circN3, next, angleMap)) {
                                        for(int j = 0; j < circN3.getQubits().size(); j++) {
                                            if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                                if(qubitMap.get(circN3.getQubits().get(j)) != next.getQubits().get(j)) {
                                                    found = false;
                                                    continue;
                                                }
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                        found = false;
                                                        continue;
                                                    }
                                                }
                                            } else {
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                        found = false;
                                                        continue;
                                                    }
                                                }
                                                qubitMap.put(circN3.getQubits().get(j), next.getQubits().get(j));
                                            }
                                        }
                                        found = true;
                                        System.out.println("Matched Node: " + circN3.getId());
                                        break;
                                    }
                                } else {
                                    for(int j = 0; j < circN3.getQubits().size(); j++) {
                                        if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                            if(qubitMap.get(circN3.getQubits().get(j)) != node.getQubits().get(j)) {
                                                found = false;
                                                continue;
                                            }
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                    found = false;
                                                    continue;
                                                }
                                            }
                                        } else {
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                    found = false;
                                                    continue;
                                                }
                                            }
                                            qubitMap.put(circN3.getQubits().get(j), node.getQubits().get(j));
                                        }
                                    }
                                    found = true;
                                    System.out.println("Matched Node: " + circN3.getId());
                                    break;
                                }
                            }
                        }
                        if(!found) {
                            System.out.println("Did not find the next node");
                            foundOutter = false;
                            break;
                        }
                        next = Graphs.successorListOf(symbdag.getDAG(), next).get(0);
                        s++;
                    }
                    if(!foundOutter) {

                        continue;
                    }
                    //start to grow symbolic circuit
                    Set<String> blockQubits = new HashSet<>();
                    Set<String> trackedQubits = new HashSet<>();
                    List<Node> symb = new ArrayList<>();
                    List<Node> symbToReplace = new ArrayList<>();
                    symbToReplace.add(node);
                    next = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                    Node circNext = Graphs.successorListOf(dag.getDAG(), node).get(0);
                    while (!next.isSinkQubit()) {
                        symbToReplace.add(next);
                        circNext = Graphs.successorListOf(dag.getDAG(), circNext).get(0);
                        next = Graphs.successorListOf(symbdag.getDAG(), next).get(0);
                    }
                    trackedQubits.add(node.getQubits().get(0));
                    for(int j = i + s; j < layers.size(); j++) {
                        if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                            break;
                        }
                        List<Node> layerJ = layers.get(j);
                        for(Node circN: layerJ) {
                            System.out.println("Checking node: " + circN.getId());
                            if(!circN.isGate()) {
                                Circuit symbCirc = opsToCircuit(symb);
                                EggGen.ConstrainedCircuit symbCircConst = CircuitTranslator.translate(symbCirc);
                                if(symbCirc.getUsedQubits().size() <= maxSymbQubits) {
                                    //check for constraints
                                    //canonicalize the circuit based on qubit map
                                    
                                    EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, qubitMap);
                                    System.out.println("checking constraints:");
                                    System.out.println(canonicalizedCirc.toEggString());
                                    try {
                                        List<Integer> subspace = new ArrayList<>();
                                        subspace.add(0);
                                        subspace.add(1);
                                        System.out.println("Subspace: " + subspace);
                                        if(checkLinearCombination(canonicalizedCirc, basis, subspace, angleMap)) {
                                            //satisfy the constraints
                                            System.out.println("Satisfy the constraints");
                                            //symbToReplace.add(circN);
                                            return symbToReplace;
                                        } else {
                                            System.out.println("did not satisfy the constraints");
                                        }
                                    } catch (IOException | InterruptedException e) {
                                        System.err.println("Error checking linear combination: " + e.getMessage());
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
                                if(symbCirc.getUsedQubits().size() <= maxSymbQubits) {
                                    //check for constraints
                                    System.out.println("checking constraints:");
                                    System.out.println(symbCircConst.toEggString());
                                    //canonicalize the circuit based on qubit map
                                    EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, qubitMap);
    
                                    try {
                                        List<Integer> subspace = new ArrayList<>();
                                        subspace.add(0);
                                        subspace.add(1);
                                        System.out.println("Subspace: " + subspace);
                                        if(checkLinearCombination(canonicalizedCirc, basis, subspace, angleMap)) {
                                            //satisfy the constraints
                                            System.out.println("Satisfy the constraints");
                                            symbToReplace.add(circN);
                                            return symbToReplace;
                                        } else {
                                            System.out.println("did not satisfy the constraints");
                                        }
                                    } catch (IOException | InterruptedException e) {
                                        System.err.println("Error checking linear combination: " + e.getMessage());
                                    }
                                }

                                if(!symbToReplace.contains(circN)) {
                                    System.out.println("Adding to symbolic circuit: " + circN.getId());
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
        // Create temporary files for the QASM strings
        String header = String.format("OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[%s];\n", maxQubits);
        qasm1 = header + qasm1;
        qasm2 = header + qasm2;
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

        // Clean up temporary files
        Files.delete(tempFile1);
        Files.delete(tempFile2);

        if (exitCode != 0) {
            System.err.println("Qiskit equivalence checker script exited with error code: " + exitCode);
            System.err.println("Error output: " + errorOutput.toString());
            return false;
        }
        System.out.print("Output:" + output);

        return output.toString().trim().equals("true");
    }

    public void optimize(EggGen.ConstrainedCircuit circuit, EggGen egraph) {
         System.out.println("Original Gate Size: " + circuit.toEggString());
        System.out.println("Original Gate Size: " + circuit.circuit.gates.size());
        String name = egraph.addConstrainedCircuit(circuit);
        egraph.runSaturation("opt");
        EggGen.ConstrainedCircuit extracted = egraph.extract(name);
        System.out.println("Optimized Circuit: " + extracted.toEggString());
        System.out.println("Optimized gate size:" + extracted.circuit.gates.size());
        
        String originalQasm = CircuitTranslator.translateBack(circuit, circuit.circuit.getMaxQubits()+1).getCircuit().getQasmString();
        
        String optimizedQasm = CircuitTranslator.translateBack(extracted, extracted.circuit.getMaxQubits()+1).getCircuit().getQasmString();
        System.out.println(originalQasm);
        System.out.println(optimizedQasm);
        try {
            boolean equivalent = checkEquivalenceWithQiskit(originalQasm, optimizedQasm, circuit.circuit.getMaxQubits()+1);
            System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
        }
    }


    // private String substitute(Circuit s, String rhs) {

    // }


    // Helper method to build qubit mapping from concrete circuit to pattern circuit
    Map<String, String> buildQubitMap(EggGen.Circuit concrete, EggGen.Circuit pattern) {
        Map<String, String> qubitMap = new HashMap<>();
        int n = Math.min(concrete.gates.size(), pattern.gates.size());
        for (int k = 0; k < n; k++) {
            EggGen.Gate cg = concrete.gates.get(k);
            EggGen.Gate pg = pattern.gates.get(k);
            // For 1-qubit gates
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
            // For 2-qubit gates
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
            // Could add more gate types here as needed
        }
        return qubitMap;
    }


    double saProbability(double currentCost, double newCost, double temperature) {
        if(newCost < currentCost) {
            return 1.0;
        }

        return Math.exp((currentCost - newCost) / temperature);
    }

    public void optimize_SA(EggGen.ConstrainedCircuit circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, int egraph_rule_limit, int symb_rule_limit, int iterations) {
        EggGen egraph = new EggGen();
        System.out.println("Original Size:" + circuit.circuit.gates.size());
        System.out.println("Original 2q:" + circuit.circuit.getTwoQubitsCount());
        EggGen.ConstrainedCircuit optimized = circuit;
        Random random = new Random();

        EggGen.ConstrainedCircuit bestOptimized = optimized;
        int j = 0;
        while(j < iterations) {
            System.out.println("CURRENT iteration:" + j);
            egraph.push();
            String name = egraph.addConstrainedCircuit(optimized);
            // choose egraph_rule_limit different rules from rules
            List<String> copy = new ArrayList<>(rules);
            for(int i = 0; i < Integer.min(copy.size(), egraph_rule_limit); i++) {
                int index = random.nextInt(copy.size());
                egraph.addRewritev2(copy.get(index));
                copy.remove(index);
            }

            egraph.runN("opt", 15);
            EggGen.ConstrainedCircuit candidate = egraph.extract(name);
            double acceptP = saProbability(optimized.circuit.gates.size(), candidate.circuit.gates.size(), Params.TEMPERATURE);
            if(random.nextDouble() < acceptP) {
                optimized = candidate;
            }
            
            List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);
            for (int i = 0; i < Integer.min(copysymb.size(), symb_rule_limit); i++){
                System.out.println("Current RULE: " + i + "/" + Integer.min(symb_rule_limit, symbRules.size()));
                int index = random.nextInt(copysymb.size());
                Circuit c = CircuitTranslator.translateBack(optimized, optimized.circuit.getMaxQubits()+1).getCircuit();
                CircuitDAG optimizedDAG = symbolicMatch(c, copysymb.get(index).getLHS(), copysymb.get(index).getRHS(), EnumeratorPrune.MAX_QUBITS_SYMB, copysymb.get(index).getConstraint());
                if(optimizedDAG != null) {
                    acceptP = saProbability(optimized.circuit.gates.size(), optimizedDAG.cost(CircuitDAG.OptObj.TOTAL), Params.TEMPERATURE);
                    if(random.nextDouble() < acceptP) {
                        String qasm = optimizedDAG.toQASM();
                        EggGen.Circuit circuitnew = EggAstBuilder.parseCircuit(qasm);
                        EggGen.ConstrainedCircuit cc = new EggGen.ConstrainedCircuit(circuitnew, new EggGen.Permutation(new ArrayList<>()));
                        optimized = cc;
                    }
                }
            }

            if(optimized.circuit.gates.size() < bestOptimized.circuit.gates.size()) {
                bestOptimized = optimized;
            }
        }

    }
    
    public void optimize(EggGen.ConstrainedCircuit circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, int egraph_rule_limit, int symb_rule_limit, int iterations) {
        EggGen egraph = new EggGen();
        System.out.println("Original Size:" + circuit.circuit.gates.size());
        System.out.println("Original 2q:" + circuit.circuit.getTwoQubitsCount());
        EggGen.ConstrainedCircuit optimized = circuit;
        Random random = new Random();
        //We need to preprocess the symb rules to (rule .....).
        int j = 0;
        while(j < iterations) {
            System.out.println("CURRENT iteration:" + j);
            egraph.push();
            String name = egraph.addConstrainedCircuit(optimized);
            // choose egraph_rule_limit different rules from rules
            List<String> copy = new ArrayList<>(rules);
            for(int i = 0; i < Integer.min(copy.size(), egraph_rule_limit); i++) {
                int index = random.nextInt(copy.size());
                egraph.addRewritev2(copy.get(index));
                copy.remove(index);
            }

            egraph.runN("opt", 15);
            
            // do ematching for symbolic rules
            List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);
            for (int i = 0; i < Integer.min(copysymb.size(), symb_rule_limit); i++){
                System.out.println("Current RULE: " + i + "/" + Integer.min(symb_rule_limit, symbRules.size()));
                int index = random.nextInt(copysymb.size());
                // int index = i;
                List<Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit>> matches = egraph.ematching(copysymb.get(index).getLHS(), copysymb.get(index).getRHS(), 500);
                System.out.println("Match Sizes: " + matches.size());
                int k = 0;
                for(Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit> match : matches) {
                    //now, we need to check that s satisfy the constraints
                    System.out.println("Current Match: " + k + "/" + matches.size());
                    k++;
                    EggGen.Circuit matchedLhs = match.getMiddle();
                    EggGen.Circuit matchedRhs = match.getRight();
                    EggGen.Circuit s = match.getLeft();

                    System.out.println("Match: s: " + match.getLeft().toEggString() +  "\nlhs:" + match.getMiddle().toEggString() + "\nrhs:" + match.getRight().toEggString());
                    if(matchedLhs.toEggString().equals(matchedRhs.toEggString()) && s.gates.isEmpty()) {
                        continue;
                    }
                    
                    String lhs = copysymb.get(index).getLHS();
                    Pattern pattern = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+c\\)");
                    Matcher matcher = pattern.matcher(lhs);
                    String removedSymb = null;
                    if(matcher.find()) {
                        String matched = matcher.group();
                        removedSymb = lhs.replace(matched, "(Nil)");
                    }

                    // Map<String, String> qubitMap = null;
                    // if(removedSymb != null) {
                    //     removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
                    //     removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
                    //     System.out.println("replaced symb rule:" + removedSymb);
                    //     EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(removedSymb);
                    //     qubitMap = buildQubitMap(matchedLhs, symblhs);
                    // } else {
                    //     Pattern pattern2 = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+(.*)\\)");
                    //     Matcher matcher2 = pattern2.matcher(lhs);
                    //     if (matcher2.find()) {
                    //         removedSymb = matcher2.group(1).trim();
                    //     }
                    //     removedSymb = removedSymb.replaceAll("\\bc\\b", "(Nil)");
                    //     removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
                    //     removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
                    //     System.out.println("replaced symb rule:" + removedSymb);
                    //     List<EggGen.Gate> concretelhsgates = new ArrayList<>(matchedLhs.gates.subList(match.getLeft().gates.size(), matchedLhs.gates.size()));
                    //     EggGen.Circuit concretecircuit = new EggGen.Circuit(concretelhsgates);
                    //     System.out.println("replaced concrete lhs:" + concretecircuit.toEggString());
                    //     EggGen.Circuit symbpattern = EggAstBuilder.parseCircuit(removedSymb);
                    //     qubitMap = buildQubitMap(concretecircuit, symbpattern);
                    // }
                    
                    //EggGen.Circuit canonicalized = EggGen.canonicalizeCircuit(s, qubitMap);
                    //System.out.println("Maxqubits:" + (canonicalized.getMaxQubits() + 1));
                    
                    //System.out.println("Canonicaled:" + canonicalized.toEggString());
                    
                        ConstrainedCircuit cc1 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedLhs, new EggGen.Permutation(new ArrayList<>())), matchedLhs.getMaxQubits()+1);
                        ConstrainedCircuit cc2 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedRhs, new EggGen.Permutation(new ArrayList<>())), matchedRhs.getMaxQubits()+1);
                        try {
                            boolean equivalent = checkEquivalenceWithQiskit(cc1.getCircuit().getQasmString(), cc2.getCircuit().getQasmString(), matchedLhs.getMaxQubits()+1);
                            if(equivalent) {
                                egraph.sendCommand(String.format("(union %s %s)", matchedLhs.toEggString(), matchedRhs.toEggString()));
                            }
                            System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
                        } catch (IOException | InterruptedException e) {
                            System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
                        }
                        // if(checkLinearCombination(canonicalized, copysymb.get(index).getConstraint(), EnumeratorPrune.MAX_QUBITS_SYMB))  {
                        //     System.out.println("S satisfy the constraints!");
                        //     //substitube symb with matched s
                        //     System.out.println("Union:\nLHS:" + matchedLhs.toEggString() + "'\nRHS:" + matchedRhs.toEggString());
                        //     ConstrainedCircuit cc1 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedLhs, new EggGen.Permutation(new ArrayList<>())), matchedLhs.getMaxQubits()+1);
                        //     ConstrainedCircuit cc2 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedRhs, new EggGen.Permutation(new ArrayList<>())), matchedRhs.getMaxQubits()+1);
                        //     try {
                        //         boolean equivalent = checkEquivalenceWithQiskit(cc1.getCircuit().getQasmString(), cc2.getCircuit().getQasmString(), matchedLhs.getMaxQubits()+1);
                        //         System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
                        //     } catch (IOException | InterruptedException e) {
                        //         System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
                        //     }
                        //     egraph.sendCommand(String.format("(union %s %s)", matchedLhs.toEggString(), matchedRhs.toEggString()));
                        // } else {
                        //     // they are not equal, comfirme it with check equal
                        //     ConstrainedCircuit cc1 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedLhs, new EggGen.Permutation(new ArrayList<>())), matchedLhs.getMaxQubits()+1);
                        //     ConstrainedCircuit cc2 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedRhs, new EggGen.Permutation(new ArrayList<>())), matchedRhs.getMaxQubits()+1);
                        //     try {
                        //         boolean equivalent = checkEquivalenceWithQiskit(cc1.getCircuit().getQasmString(), cc2.getCircuit().getQasmString(), matchedLhs.getMaxQubits()+1);
                        //         System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
                        //     } catch (IOException | InterruptedException e) {
                        //         System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
                        //     }
                        // }
                   
                    
                }
                copysymb.remove(index);
            }


            egraph.runN("opt", 5);

            optimized = egraph.extract(name);
            System.out.println("Current Gate Size:" + optimized.circuit.gates.size());
            System.out.println("Current 2q:" + optimized.circuit.getTwoQubitsCount());
            egraph.rules.clear();
            egraph.optrules.clear();
            egraph.pop();
            j++;

            Map<String,Long> data = egraph.getProfilingData();
            System.out.print("--------------------------Iteration Egraph Break Down-----------------\n");
            System.out.println("ematchingSaturationTime" + data.get("ematchingSaturationTime") / 1000000);
            System.out.println("ematchingPrefixTime" + data.get("ematchingPrefixTime") / 1000000);
            System.out.println("ematchingSuffixTime:" + data.get("ematchingSuffixTime") / 1000000);
        }

        System.out.println("Final Gate Size:" + optimized.circuit.gates.size());
        System.out.println("Final 2q:" + optimized.circuit.getTwoQubitsCount());
        Map<String,Long> data = egraph.getProfilingData();
        System.out.print("--------------------------Egraph Break Down-----------------\n");
        System.out.println("ematchingSaturationTime" + data.get("ematchingSaturationTime") / 1000000);
        System.out.println("ematchingPrefixTime" + data.get("ematchingPrefixTime") / 1000000);
        System.out.println("ematchingSuffixTime:" + data.get("ematchingSuffixTime") / 1000000);
    }



    private boolean checkLinearCombination(EggGen.Circuit circuit, List<SymbolicSolve.SparseMatrix> basis, List<Integer> subspace, Map<String, Expr> symbolMap) throws IOException, InterruptedException {
        String jsonString = solver.circuitToJson(circuit.gates, circuit.getMaxQubits()+1);
        String jsonM = "[";
        // Create a temporary file for the basis matrices
        List<String> basisStrList = new ArrayList<>();
        for(SymbolicSolve.SparseMatrix m : basis) {
            String basis_str = m.to_json_string();
            basisStrList.add(basis_str);
        }
        jsonM = jsonM + String.join(",", basisStrList) + "]";
        
        System.out.println("Json Matrix" + jsonM);

        String subspaceStr = subspace.stream().map(String::valueOf).collect(Collectors.joining(","));
        subspaceStr = "[" + subspaceStr + "]";

        String symbolMapStr = symbolMap.entrySet().stream()
            .map(entry -> "\"" + entry.getKey() + "\": " + CircuitDAG.eval(entry.getValue()))
            .collect(Collectors.joining(","));
        symbolMapStr = "{" + symbolMapStr + "}";

        ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-is_subspace_linear", jsonString, jsonM, subspaceStr, symbolMapStr);
        Process p = pb.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = in.readLine()) != null) {
            output.append(line);
        }

        BufferedReader ereader = new BufferedReader(new InputStreamReader(p.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
        while ((line = ereader.readLine()) != null) {                                                  
            System.err.println(line);
        }

        int exitCode = p.waitFor();

        if (exitCode != 0) {
            System.err.println("Semantic check script exited with error code: " + exitCode);
            return false;
        }
        //System.out.println("Output: " + output.toString().trim());
        return output.toString().trim().contains("True");
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

        timeout.setRequired(false);
        options.addOption(timeout);
        

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Optimizer", options);
            System.exit(1);
            return;
        }

        String benchmarkFile = cmd.getOptionValue("benchmark");
        System.out.println(benchmarkFile);
        String rulesFile = cmd.getOptionValue("rule");
        String symrulesFile = cmd.getOptionValue("symbrule");
        int timeoutint = Integer.valueOf(cmd.getOptionValue("timeout"));
        List<String> rules = new ArrayList<>();
        EggGen egraph = new EggGen();

        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                egraph.addRewritev2(line);
                rules.add(line);
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
                System.out.println("LHS" + lhs);
                System.out.println("RHS" + rhs);
                System.out.println("matrix:" + matrix);
                Pattern matrixP = Pattern.compile("\\[(.*)\\]");
                Matcher matcher = matrixP.matcher(matrix);
                matcher.matches();
                String matricesStr = matcher.group(1);
                String[] matrices = matricesStr.split("::");
                List<SymbolicSolve.SparseMatrix> matrixList = new ArrayList<>();
                //TODO: Construct Matrices
                for(String m: matrices) {
                        List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                        if(m.startsWith("Matrix(")) {
                            String content = m.substring("Matrix(".length(), m.length() - 1);
                            System.out.println("content: " + content);
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
        System.out.println(circuitString);
        

        EggGen.Circuit circuit = QASMAstBuilder.parse(circuitString);
        System.out.println(circuit.toEggString());

        // Use timeoutint to limit the time to run optimize; when time is up, terminate the program

        // Assume timeoutint is defined somewhere above as the time limit in seconds
        Thread optThread = new Thread(() -> {
            Optimizer optimizer = new Optimizer();
            //optimizer.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), egraph);
            optimizer.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), rules, symbRules, 20, 5, 50);
        });

        optThread.start();

        try {
            optThread.join(timeoutint * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (optThread.isAlive()) {
            egraph.stopEgglogREPL();
            System.out.println("Timeout reached (" + timeoutint + "s). Terminating optimization.");
            optThread.stop(); // Hard stop; not recommended but used here since optimize is potentially long/uninterruptible
            System.exit(1);
        }
    }
}
