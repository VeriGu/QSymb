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
                return new EggGen.H(node.getQubits().get(0));
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
                            if (pattern.getEdgeSource(pattE).getAngles().isEmpty()) {
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
        //System.out.println("Trying to match pattern starting with: " + start.getId());

        for (Node circN : nodes) {
            //System.out.println("Circuit node: " + circN.getId());
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
                    //System.out.println("Matched Angles"); 
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
                        //System.out.println("Succ: " + succ.getId() + " patternToCirc.get(succ): " + patternToCirc.get(succ));
                        if (!matchOutgoing(circuit.getDag(), pattern.getDag(), patternToCirc.get(succ), succ, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                            match = false;
                            //System.out.println("No Match Found Outgoing " + succ.getId());
                            break;
                        }
                        
                        if (!matchIncoming(circuit.getDag(), pattern.getDag(), patternToCirc.get(succ), succ, patternToCirc, patternToCircEdges, angleMap, ancsToVisit)) {
                            match = false;
                            //System.out.println("No Match Found Incoming " + succ.getId());
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
                        System.out.println("Visiting anc" + anc.getId());
                        if (seen.contains(anc)) {
                            continue;
                        }

                        if (matched.contains(patternToCirc.get(anc)) || replaced.contains(patternToCirc.get(anc))) {
                            match = false;
                            break;
                        }
                        //System.out.println("Anc: " + anc.getId() + " patternToCirc.get(anc): " + patternToCirc.get(anc));
                        if (!matchOutgoing(circuit.getDag(), pattern.getDag(), patternToCirc.get(anc), anc, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                            match = false;
                            break;
                        }
                        //System.out.println("Matched Outgoing anc" + anc.getId());
                        if (!matchIncoming(circuit.getDag(), pattern.getDag(), patternToCirc.get(anc), anc, patternToCirc, patternToCircEdges, angleMap, ancsToVisit)) {
                            match = false;
                            break;
                        }
                        //System.out.println("Matched Incoming anc" + anc.getId());
                        seen.add(anc);
                    }
                    if (!match) {
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                //System.out.println("patternToCirc map: " + patternToCirc.toString());
                if (patternToCirc.size() == pattern.totalGateCount()) {
                    //System.out.println("Matched All");
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
        //System.out.println("Replacing pattern: " + pattern.toQASM());
        //System.out.println("With: " + replace.toQASM());
        //System.out.println("patternToCirc map: " + patternToCirc.toString());
        //System.out.println("patternToCircuitQubit map: " + patternToCircuitQubit.toString());
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

        //the replace leaves might contain new qubits that need to be connected to the decPatternLeaves
        for (String qubit : replaceLeaves.keySet()) {
            // qubits not in replaceLeaves should not have been in replaceRoots and therefore were connected already to decPatternLeave
            if (decPatternLeaves.containsKey(qubit)) {
                circuit.addEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), pattern.getEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), qubit));
            } else {
                String circQubit = patternToCircuitQubit.getOrDefault(qubit, qubit);
                Node match = patternToCirc.getOrDefault(replaceLeaves.get(qubit), replaceLeaves.get(qubit));
                Edge newedge = null;
                Node outNode = null;
                for (Edge e : circuit.outgoingEdgesOf(match)) {
                    if (e.getQubit().equals(circQubit)) {
                        newedge = pattern.getEdge(replaceLeaves.get(qubit), circuit.getEdgeTarget(e), qubit);
                        outNode = circuit.getEdgeTarget(e);
                    }
                }
                if(newedge != null) {
                    circuit.addEdge(match, outNode, newedge);
                }
            }
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
            System.out.println("applyRule: No Match Found");
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


    public CircuitDAG symbolicMatch(EggGen.Circuit circuit, String rule, String rhs, int minSymbSize, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, EggGen egraph) {
        System.out.println(circuit.toQASM());
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
                //System.out.println("Symbolic LHS is not connected");
                return null;
            }
            Map<String, String> qubitMap = new HashMap<>();
            Map<String, String> reverseMap = new HashMap<>();
            //System.out.println("LHS DAG: " + symbdag.toQASM());
            List<Node> matchedNodes = matchBefore(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, basis, qubitMap, reverseMap);
            if(matchedNodes == null) {
                //System.out.println("No Match Before found");
                return null;
            }
            //System.out.println("Reverse Map: " + reverseMap.toString());
            //System.out.println("Qubit Map: " + qubitMap.toString());

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
                System.out.println("Angle: " + angle + " -> " + angleMap.get(angle).toEggString());
                if(premap.containsKey(angle)) {
                    removedRhs = removedRhs.replace(premap.get(angle), angleMap.get(angle).toEggString());
                } else {
                    removedRhs = removedRhs.replace(angle, angleMap.get(angle).toEggString());
                }
            }
            //System.out.println("Removed RHS: " + removedRhs);
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
            System.out.println("Combined LHS Circuit: " + lhsCircuit.toQASM());
            System.out.println("Combined RHS Circuit: " + combinedCircuit.toQASM());
            Random rand = new Random();
            CircuitDAG result = applyRule(dag, lhsDag, combinedCircuit.toQASM(), true, rand);
            String qasm = result.toQASM();
            System.out.println("Before: " + dag.toQASM());
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
            //System.out.println("Removed LHS: " + removedSymb);
            EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(removedSymb);
            EggGen.ConstrainedCircuit constrainedSymblhs = new EggGen.ConstrainedCircuit(symblhs, new EggGen.Permutation(new ArrayList<>()));
            String symbqasm = constrainedSymblhs.circuit.toQASM();
            System.out.println("Symbolic LHS QASM: " + symbqasm);
            CircuitDAG symbdag = QASMToDAGVisitor.parse(symbqasm);
            if (!GraphTests.isConnected(symbdag.getDAG())) {
                //System.out.println("Symbolic LHS is not connected");
                return null;
            }
            Map<String, String> qubitMap = new HashMap<>();
            Map<String, String> reverseMap = new HashMap<>();
            //System.out.println("LHS DAG: " + symbdag.toQASM());
            List<Node> matchedNodes = matchAfter(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, basis, qubitMap, reverseMap);
            if(matchedNodes == null) {
                //System.out.println("No Match After found");
                return null;
            }

            //System.out.println("Reverse Map: " + reverseMap.toString());
            //System.out.println("Qubit Map: " + qubitMap.toString());
            matcher = pattern.matcher(rhs);
            String removedRhs = null;
            if (matcher.find()) {
                String matched = matcher.group();
                removedRhs = rhs.replace(matched, "(Nil)");
                removedRhs = removedRhs.replaceAll("q\\d+", "(Q \"$0\")"); 
                //removedRhs = removedRhs.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
            }

            System.out.println("Angle Map: " + angleMap.toString());
            for(String angle: angleMap.keySet()) {
                //System.out.println("Angle: " + angle + " -> " + angleMap.get(angle).toEggString());
                removedRhs = removedRhs.replace(angle, angleMap.get(angle).toEggString());
            }
            System.out.println("Removed RHS: " + removedRhs);

            EggGen.Circuit symbrhs = EggAstBuilder.parseCircuit(removedRhs);
            EggGen.Circuit symbrhsCan = EggGen.canonicalizeCircuit(symbrhs, reverseMap);
            //System.out.println("Canonicalized RHS: " + symbrhsCan.toEggString());
            List<EggGen.Gate> gates = new ArrayList<>(symbrhsCan.gates);
            System.out.print("MatchedNode size:"+ matchedNodes.size());
            List<Node> matchedsymb = matchedNodes.subList(0, matchedNodes.size() - symblhs.gates.size());
            List<EggGen.Gate> matchedgates = nodesToGates(matchedsymb);
            
            List<EggGen.Gate> lhsgates = nodesToGates(matchedNodes);
            EggGen.Circuit lhsCircuit = new EggGen.Circuit(lhsgates);
            //System.out.println("LHS Circuit: " + lhsCircuit.toQASM());
            CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuit.toQASM());

            gates.addAll(matchedgates);
            EggGen.Circuit combinedCircuit = new EggGen.Circuit(gates);
            
            EggGen.ConstrainedCircuit combinedConst = new EggGen.ConstrainedCircuit(combinedCircuit, new EggGen.Permutation(new ArrayList<>()));

            System.out.println("Combined LHS Circuit: " + lhsCircuit.toQASM());
            System.out.println("Combined RHS Circuit: " + combinedCircuit.toQASM());
            if(egraph != null) {
                egraph.sendCommand(String.format("(union %s %s)", lhsCircuit.toEggString(), combinedCircuit.toEggString()));
            }
            Random rand = new Random();
            CircuitDAG result = applyRule(dag, lhsDag, combinedCircuit.toQASM(), true, rand);
            String qasm = result.toQASM();
            System.out.println("Before: " + dag.toQASM());
            System.out.println("Result: " + qasm);
            return result;
        }
    }

    private List<Node> matchAfter(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap) {
        List<Node> roots = symbdag.getCircuitRoots();
        Node patternRoot = roots.get(0);
        List<List<Node>> layers = dag.topoSort();
        Set<String> blockedQubits = new HashSet<>();
        Set<String> trackedQubits = new HashSet<>();
        List<Node> symb = new ArrayList<>();
        List<Node> symbToReplace = new ArrayList<>();
        System.out.println("Matching after");
        
        boolean isFirst = true;
        for(int i = 0; i < layers.size(); i++) {
            if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                break;
            }
            System.out.println("layer:" + (i+1) + "/" + layers.size());
            for(Node node : layers.get(i)) {
                qubitMap.clear();
                reverseMap.clear();
                if(!node.isGate()) {
                    continue;
                }

                System.out.println("Trying to match Node: " + node.toString());
                Set<String> trackedIntersection = new HashSet<>(trackedQubits);
                System.out.println("Tracked Qubits" + trackedQubits);
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
                    if(!blockedQubits.contains(node.getQubits().get(0)) && node.getId().equals(patternRoot.getId())) {
                        System.out.println("Matched Root: " + node.getId());
                        if(!patternRoot.getAngles().isEmpty()) {
                            if(matchAngles(node, patternRoot, angleMap)) {
                                for(int j = 0; j < node.getQubits().size(); j++) {
                                    qubitMap.put(node.getQubits().get(j), patternRoot.getQubits().get(j));
                                    reverseMap.put(patternRoot.getQubits().get(j), node.getQubits().get(j));
                                }
                                Node nextA = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                                int t = 1;
                                boolean found = true;
                                    
                                while(!nextA.isSinkQubit()) {
                                    boolean foundInner = false;
                                    for(Node node2: layers.get(i + t)) {
                                        System.out.println("Trying to match Node2: " + node2.getId());
                                        if(node2.isGate() && node2.getId().equals(nextA.getId())) {
                                            if(!nextA.getAngles().isEmpty()) {
                                                if(matchAngles(node2, nextA, angleMap)) {
                                                    Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                                    Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                                    boolean qubitMatch = true;
                                                    for(int j = 0; j < node2.getQubits().size(); j++) {
                                                        if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                            if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                            if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                                if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                    qubitMatch = false;
                                                                    break;
                                                                }
                                                            }
                                                        } else {
                                                            if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                                if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                    qubitMatch = false;
                                                                    break;
                                                                }
                                                            }
                                                            
                                                        }
                                                        tempQubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                        tempReverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                                    }
                                                    System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                    if(qubitMatch) {
                                                        qubitMap.clear();
                                                        qubitMap.putAll(tempQubitMap);
                                                        reverseMap.clear();
                                                        reverseMap.putAll(tempReverseMap);
                                                        foundInner = true;
                                                        break;
                                                    }
                                                }
                                                
                                            }
                                            else {
                                                Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                                Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                                boolean qubitMatch = true;
                                                for(int j = 0; j < node2.getQubits().size(); j++) {
                                                    if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                        if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    } else {
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    tempQubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                    tempReverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                                }
                                                System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                if(qubitMatch) {
                                                    foundInner = true;
                                                    qubitMap.clear();
                                                    qubitMap.putAll(tempQubitMap);
                                                    reverseMap.clear();
                                                    reverseMap.putAll(tempReverseMap);
                                                    break;
                                                }
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
                        } else {
                            //System.out.println("Matched Angles");
                            for(int j = 0; j < node.getQubits().size(); j++) {
                                qubitMap.put(node.getQubits().get(j), patternRoot.getQubits().get(j));
                                reverseMap.put(patternRoot.getQubits().get(j), node.getQubits().get(j));
                            }
                            Node nextA = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                            int t = 1;
                            boolean found = true;
                            while(!nextA.isSinkQubit()) {
                                boolean foundInner = false;
                                for(Node node2: layers.get(i + t)) {
                                    System.out.println("Trying to match Node2: " + node2.getId());
                                    if(node2.isGate() && node2.getId().equals(nextA.getId())) {
                                        if(nextA.getAngles() != null) {
                                            if(matchAngles(node2, nextA, angleMap)) {
                                                Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                                Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                                boolean qubitMatch = true;
                                                for(int j = 0; j < node2.getQubits().size(); j++) {
                                                    if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                        if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    } else {
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    tempQubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                    tempReverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                                   
                                                }
                                                System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                if(qubitMatch) {
                                                    qubitMap.clear();
                                                    qubitMap.putAll(tempQubitMap);
                                                    reverseMap.clear();
                                                    reverseMap.putAll(tempReverseMap);
                                                    foundInner = true;
                                                    break;
                                                }
                                            }
                                        }
                                        else {
                                            Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                            Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                            boolean qubitMatch = true;
                                            for(int j = 0; j < node2.getQubits().size(); j++) {
                                                if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                    if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                    if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                        if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                        if(reverseMap.get(nextA.getQubits().get(j)) != node2.getQubits().get(j)) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                    }
                                                    
                                                }
                                                tempQubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                tempReverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                            }
                                            System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                            if(qubitMatch) {
                                                qubitMap.clear();
                                                qubitMap.putAll(tempQubitMap);
                                                reverseMap.clear();
                                                reverseMap.putAll(tempReverseMap);
                                                foundInner = true;
                                                break;
                                            }
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
                        if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                            EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(qubitMap));
                            System.out.println("Canonicalized Circuit: " + canonicalizedCirc.toEggString());
                            try {
                                List<Integer> subspace = new ArrayList<>();
                                subspace.add(0);
                                subspace.add(1);
                                //System.out.println("Subspace: " + subspace);
                                if(checkLinearCombination(canonicalizedCirc, basis, subspace, angleMap)) {
                                    symbToReplace.add(node);
                                    Node nextA = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                                    Node circNextA = Graphs.successorListOf(dag.getDAG(), node).get(0);
                                    while(!nextA.isSinkQubit()) {
                                        symbToReplace.add(circNextA);
                                        circNextA = Graphs.successorListOf(dag.getDAG(), circNextA).get(0);
                                        nextA = Graphs.successorListOf(symbdag.getDAG(), nextA).get(0);
                                    }
                                    System.out.println("S satisfy the constraints");
                                    return symbToReplace;
                                } else {
                                    System.out.println("S does not satisfy the constraints");
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
                        System.out.println("Added node: " + symbToReplace.toString());
                        System.out.println("Symb: " + symb.toString());
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
            if(circ instanceof UnOp) {
                if(((UnOp) pattern).getOp().equals(((UnOp) circ).getOp())) {
                    return matchAngle(((UnOp) pattern).getE(), ((UnOp) circ).getE(), angleMap);
                } else {
                    return false;
                }
            }
        } else {
            return sameAngle(pattern, circ);
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

    public List<Node> matchBefore(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap) {
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
                                        Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                        Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                        boolean qubitMatch = true;
                                        for(int j = 0; j < circN3.getQubits().size(); j++) {
                                            if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                                if(qubitMap.get(circN3.getQubits().get(j)) != next.getQubits().get(j)) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                }
                                            } else {
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                }
                                                
                                            }
                                            tempQubitMap.put(circN3.getQubits().get(j), next.getQubits().get(j));
                                            tempReverseMap.put(next.getQubits().get(j), circN3.getQubits().get(j));
                                        }
                                        if(qubitMatch) {
                                            qubitMap.clear();
                                            qubitMap.putAll(tempQubitMap);
                                            reverseMap.clear();
                                            reverseMap.putAll(tempReverseMap);
                                            found = true;
                                            break;
                                        }
                                    }
                                } else {
                                    Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                                    Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                                    boolean qubitMatch = true;
                                    for(int j = 0; j < circN3.getQubits().size(); j++) {
                                        if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                            if(qubitMap.get(circN3.getQubits().get(j)) != node.getQubits().get(j)) {
                                                qubitMatch = false;
                                                break;
                                            }
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                            }
                                        } else {
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(reverseMap.get(next.getQubits().get(j)) != circN3.getQubits().get(j)) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                            }
                                           
                                        }
                                        tempQubitMap.put(circN3.getQubits().get(j), next.getQubits().get(j));
                                        tempReverseMap.put(next.getQubits().get(j), circN3.getQubits().get(j));
                                    }
                                    if(qubitMatch) {
                                        qubitMap.clear();
                                        qubitMap.putAll(tempQubitMap);
                                        reverseMap.clear();
                                        reverseMap.putAll(tempReverseMap);
                                        found = true;
                                        break;
                                    }
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
                                if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                                    //check for constraints
                                    //canonicalize the circuit based on qubit map
                                    
                                    EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(qubitMap));
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
                                if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                                    //check for constraints
                                    System.out.println("checking constraints:");
                                    System.out.println(symbCircConst.toEggString());
                                    //canonicalize the circuit based on qubit map
                                    EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(qubitMap));
    
                                    try {
                                        List<Integer> subspace = new ArrayList<>();
                                        subspace.add(0);
                                        subspace.add(1);
                                        System.out.println("Subspace: " + subspace);
                                        if(checkLinearCombination(canonicalizedCirc, basis, subspace, angleMap)) {
                                            //satisfy the constraints
                                            System.out.println("S Satisfy the constraints");
                                            symbToReplace.add(circN);
                                            return symbToReplace;
                                        } else {
                                            System.out.println("S did not satisfy the constraints");
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

    public void optimize(EggGen.ConstrainedCircuit circuit, EggGen egraph, int timeout) {
        System.out.println("Original Gate Size: " + circuit.toEggString());
        System.out.println("Original Gate Size: " + circuit.circuit.gates.size());
        String name = egraph.addConstrainedCircuit(circuit);
        EggGen.ConstrainedCircuit bestOptimized = circuit;
        long startTime = System.nanoTime();
        while(true) {
            egraph.push();
            egraph.runN("opt", 20);
            EggGen.ConstrainedCircuit extracted = egraph.extract(name);
            if(extracted.circuit.gates.size() < bestOptimized.circuit.gates.size()) {
                bestOptimized = extracted;
            }
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
            egraph.pop();
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            if(duration / 1000000000 > timeout) {
                break;
            }
        }
        System.out.println("Final Gate Size:" + bestOptimized.circuit.gates.size());
        System.out.println("Final 2q:" + bestOptimized.circuit.getTwoQubitsCount());
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


    public void optimize_BEAM(EggGen.ConstrainedCircuit circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, int beam_width, int egraph_rule_limit, int symb_rule_limit, int min_symb_size, int max_symb_size, int timeout, boolean useSymb, Comparator<EggGen.ConstrainedCircuit> comparator, List<String> commutative) {
        EggGen egraph = new EggGen();
        for(String rule: commutative) {
            egraph.addRewrite(rule);
        }
        System.out.println("Starting BEAM optimization..., timeout: " + timeout);
        System.out.println("Original Size:" + circuit.circuit.gates.size());
        System.out.println("Original 2q:" + circuit.circuit.getTwoQubitsCount());
        EggGen.ConstrainedCircuit bestOptimized = circuit;
        Random random = new Random(Params.SEED);
        long startTime = System.nanoTime();
        EggGen.ConstrainedCircuit optimized = bestOptimized;

        PriorityQueue<EggGen.ConstrainedCircuit> q = new PriorityQueue<>(comparator);
        Set<Integer> visited = new HashSet<>();
        visited.add(circuit.circuit.toQASM().hashCode());
        q.add(optimized);

        long timesStart = System.nanoTime();
        int iters = 0;
        while(!q.isEmpty()) {
            if((System.nanoTime() - timesStart) / 1000000000 > timeout) {
                break;
            }

            iters++;

            EggGen.ConstrainedCircuit current = q.peek();
            if(comparator.compare(current, bestOptimized) < 0) {
                System.out.println("New best optimized: " + current.circuit.toQASM());
                System.out.println("New best optimized 2q:" + current.circuit.getTwoQubitsCount());
                System.out.println("New best optimized gate size:" + current.circuit.gates.size());
                bestOptimized = current;
            }

            EggGen.ConstrainedCircuit candidate = dequeueCircuit(q, Params.TEMPERATURE, CircuitDAG.OptObj.TWO_Q, random);
            
            //sample rules
            List<List<String>> rulesToUse = new ArrayList<>();
            for(int j = 0; j < beam_width; j++) {
                rulesToUse.add(new ArrayList<>());
                List<String> copy = new ArrayList<>(rules);
                for(int i = 0; i < Integer.min(copy.size(), egraph_rule_limit); i++) {
                    int index = random.nextInt(copy.size());
                    rulesToUse.get(j).add(copy.get(index));
                    copy.remove(index);
                }
            }
            //sample symbolic Rules
            List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);
            List<MatrixConstrainedRule> symbRulesToUse = new ArrayList<>();
            for(int i = 0; i < Integer.min(copysymb.size(), symb_rule_limit); i++) {
                int index = random.nextInt(copysymb.size());
                symbRulesToUse.add(copysymb.get(index));
                copysymb.remove(index);
            }

            //beam search
            if(q.size() > Params.QUEUE_SIZE + 1000) {
                System.out.println("Queue size: " + q.size());
                System.out.println("Prune queue");
                PriorityQueue<EggGen.ConstrainedCircuit> newQ = new PriorityQueue<>(comparator);
                while(newQ.size() != 1000) {
                    EggGen.ConstrainedCircuit newCandidate = dequeueCircuit(q, Params.TEMPERATURE, CircuitDAG.OptObj.TWO_Q, random);
                    newQ.add(q.poll());
                }
                q = newQ;
            }

            for(List<String> rs: rulesToUse) {
                egraph.push();
                String name = egraph.addConstrainedCircuit(current);
                for(String rule: rs) {
                    egraph.addRewritev2(rule);
                }
                egraph.runBackoff("opt", 25);
                List<EggGen.ConstrainedCircuit> newcandidates = egraph.extract(name, beam_width);
                for(EggGen.ConstrainedCircuit newcandidate: newcandidates) {
                    int hashcode = newcandidate.circuit.toQASM().hashCode();
                    if(newcandidate.circuit.getTwoQubitsCount() <= bestOptimized.circuit.getTwoQubitsCount() && !visited.contains(hashcode)) {
                        q.add(newcandidate);
                    }
                    visited.add(hashcode);
                }
                egraph.pop();
            }

            if(useSymb) {
                for(MatrixConstrainedRule r: symbRulesToUse) {
                    int reverse = random.nextInt(2);
                    CircuitDAG optimizedDAG = null;
                    if(reverse == 0) {
                        optimizedDAG = symbolicMatch(candidate.circuit, r.getLHS(), r.getRHS(), min_symb_size, max_symb_size, r.getConstraint(), null);
                    } else {
                        optimizedDAG = symbolicMatch(candidate.circuit, r.getRHS(), r.getLHS(), min_symb_size, max_symb_size, r.getConstraint(), null);
                    }
                    if(optimizedDAG != null) {
                        System.out.println("Optimized DAG: " + optimizedDAG.toQASM());
                        String qasm = optimizedDAG.toQASM();
                        EggGen.Circuit circuitnew = QASMAstBuilder.parse(qasm);
                        int hashcode = circuitnew.toQASM().hashCode();
                        double acceptP = saProbability(candidate.circuit.getTwoQubitsCount(), circuitnew.getTwoQubitsCount(), Params.TEMPERATURE);
                        if(acceptP >= random.nextDouble() && !visited.contains(hashcode)) {
                            q.add(new EggGen.ConstrainedCircuit(circuitnew, new EggGen.Permutation(new ArrayList<>())));
                            visited.add(hashcode);
                        }
                    }
                }
            }
        }

        System.out.println("Final Gate Size:" + bestOptimized.circuit.gates.size());
        System.out.println("Final 2q:" + bestOptimized.circuit.getTwoQubitsCount());
        System.out.println("BEAM iterations:" + iters);
        System.out.println("BEAM time:" + (System.nanoTime() - timesStart) / 1000000000.0 + " seconds");
    }


    private EggGen.ConstrainedCircuit dequeueCircuit(PriorityQueue<EggGen.ConstrainedCircuit> q, double temperature, CircuitDAG.OptObj optobj, Random random) {
        if(temperature == 0)
            return q.poll();
        else {
            List<EggGen.ConstrainedCircuit> qList = new ArrayList<>(q);
            List<Integer> weights = qList.stream().map(c -> c.circuit.getTwoQubitsCount()).collect(Collectors.toList());
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

    public void optimize_SA(EggGen.ConstrainedCircuit circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, int egraph_rule_limit, int symb_rule_limit, int min_symb_size, int max_symb_size, int timeout, boolean useSymb, List<String> commutative) {
        EggGen egraph = new EggGen();
        for(String rule: commutative) {
            egraph.addRewrite(rule);
        }
        System.out.println("Starting SA optimization..., timeout: " + timeout);
        System.out.println("Original Size:" + circuit.circuit.gates.size());
        System.out.println("Original 2q:" + circuit.circuit.getTwoQubitsCount());
        EggGen.ConstrainedCircuit bestOptimized = circuit;
        Random random = new Random(Params.SEED);
        long startTime = System.nanoTime();
        EggGen.ConstrainedCircuit optimized = bestOptimized;
        Map<MatrixConstrainedRule, Integer> symbRulesUsed = new HashMap();
        
        int symbRuleReductionsTotal = 0;
        int symbRuleReduction2q = 0;
        int egraphRuleReductionsTotal = 0;
        int egraphRuleReduction2q = 0;

        while(true) {
           
            egraph.push();
            egraph.clearRules();
            String name = egraph.addConstrainedCircuit(optimized);
            // choose egraph_rule_limit different rules from rules
            List<String> copy = new ArrayList<>(rules);
            for(int i = 0; i < Integer.min(copy.size(), egraph_rule_limit); i++) {
                int index = random.nextInt(copy.size());
                egraph.addRewritev2(copy.get(index));
                copy.remove(index);
            }
            int lastSize = -1;
            int size = 1;
            int delta = -10;
            int initialn = 20;
            int n = initialn;
            while(size < 3500 && size != lastSize && n > 0) {
                lastSize = size;
                egraph.runBackoff("opt", n);
                size = Integer.parseInt(egraph.printSize("Cons"));
                n += delta;
            }

            EggGen.ConstrainedCircuit candidate = egraph.extract(name);
            System.out.println("Candidate: " + candidate.toEggString());
            double acceptP = saProbability(optimized.circuit.getTwoQubitsCount(), candidate.circuit.getTwoQubitsCount(), Params.TEMPERATURE);
            if(random.nextDouble() <= acceptP) {
                if(candidate.circuit.gates.size() < optimized.circuit.gates.size()) {
                    egraphRuleReductionsTotal += optimized.circuit.gates.size() - candidate.circuit.gates.size();
                }
                if(candidate.circuit.getTwoQubitsCount() < optimized.circuit.getTwoQubitsCount()) {
                    egraphRuleReduction2q += optimized.circuit.getTwoQubitsCount() - candidate.circuit.getTwoQubitsCount();
                }
                optimized = candidate;
            }
            
            if(useSymb) {
                List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);
                for (int i = 0; i < Integer.min(copysymb.size(), symb_rule_limit); i++){
                    System.out.println("Current RULE: " + i + "/" + Integer.min(symb_rule_limit, symbRules.size()));
                    int index = random.nextInt(copysymb.size());
                    System.out.println("Current SYMB RULE: " + copysymb.get(index).getLHS() + " -> " + copysymb.get(index).getRHS());
                    int reverse = random.nextInt(2);
                    CircuitDAG optimizedDAG = null;
                    if(reverse == 0) {
                        optimizedDAG = symbolicMatch(optimized.circuit, copysymb.get(index).getLHS(), copysymb.get(index).getRHS(), min_symb_size, max_symb_size, copysymb.get(index).getConstraint(), egraph);
                    } else {
                        optimizedDAG = symbolicMatch(optimized.circuit, copysymb.get(index).getRHS(), copysymb.get(index).getLHS(), min_symb_size, max_symb_size, copysymb.get(index).getConstraint(), egraph);
                    }
                    if(optimizedDAG != null) {
                        symbRulesUsed.add(copysymb.get(index));
                        System.out.println("Optimized DAG: " + optimizedDAG.toQASM());
                        String qasm = optimizedDAG.toQASM();
                        EggGen.Circuit circuitnew = QASMAstBuilder.parse(qasm);
                        
                        System.out.println("Union: " + optimized.circuit.toEggString() + " " + circuitnew.toEggString());
                        egraph.runBackoff("opt", 5);
                        candidate = egraph.extract(name);
                        
                        acceptP = saProbability(optimized.circuit.getTwoQubitsCount(), candidate.circuit.getTwoQubitsCount(), Params.TEMPERATURE);
                        if(random.nextDouble() < acceptP) {
                            System.out.println("Accept ");
                            optimized = candidate;
                            // String qasm = optimizedDAG.toQASM();
                            // EggGen.Circuit circuitnew = QASMAstBuilder.parse(qasm);
                            
                            if(circuitnew.gates.size() < optimized.circuit.gates.size()) {
                                System.out.println("Symb Rule Reduced: " + (optimized.circuit.gates.size() - circuitnew.gates.size()));
                                symbRuleReductionsTotal += optimized.circuit.gates.size() - circuitnew.gates.size();
                            }
                            if(circuitnew.getTwoQubitsCount() < optimized.circuit.getTwoQubitsCount()) {
                                System.out.println("Symb Rule Reduced 2q: " + (optimized.circuit.getTwoQubitsCount() - circuitnew.getTwoQubitsCount()));
                                symbRuleReduction2q += optimized.circuit.getTwoQubitsCount() - circuitnew.getTwoQubitsCount();
                            }
                            // EggGen.ConstrainedCircuit cc = new EggGen.ConstrainedCircuit(circuitnew, new EggGen.Permutation(new ArrayList<>()));
                            // egraph.sendCommand(String.format("(union %s %s)", optimized.circuit.toEggString(), cc.circuit.toEggString()));
                            // optimized = cc;

                            // egraph.runBackoff("opt", 5);
                            // optimized = egraph.extract(name);
                        }
                    }
                } 
            } 

            egraph.pop();

            if(optimized.circuit.getTwoQubitsCount() <= bestOptimized.circuit.getTwoQubitsCount()) {
                bestOptimized = optimized;
            }
            
            System.out.println("Best Optimized Size:" + bestOptimized.circuit.gates.size());
            System.out.println("Best Optimized 2q:" + bestOptimized.circuit.getTwoQubitsCount());
            System.out.println("Symb Rule Reductions Total:" + symbRuleReductionsTotal);
            System.out.println("Symb Rule Reduction 2q:" + symbRuleReduction2q);
            System.out.println("Egraph Rule Reductions Total:" + egraphRuleReductionsTotal);
            System.out.println("Egraph Rule Reduction 2q:" + egraphRuleReduction2q);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            if(duration / 1000000000 > timeout) {
                break;
            }
        }
        System.out.println("Final Gate Size:" + bestOptimized.circuit.gates.size());
        System.out.println("Final 2q:" + bestOptimized.circuit.getTwoQubitsCount());
        System.out.println("Symb Rule Reductions Total:" + symbRuleReductionsTotal);
        System.out.println("Symb Rule Reduction 2q:" + symbRuleReduction2q);
        System.out.println("Egraph Rule Reductions Total:" + egraphRuleReductionsTotal);
        System.out.println("Egraph Rule Reduction 2q:" + egraphRuleReduction2q);
    }
    
    public void optimize(EggGen.ConstrainedCircuit circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, int egraph_rule_limit, int symb_rule_limit, int timeout) {
        EggGen egraph = new EggGen();
        System.out.println("Original Size:" + circuit.circuit.gates.size());
        System.out.println("Original 2q:" + circuit.circuit.getTwoQubitsCount());
        EggGen.ConstrainedCircuit optimized = circuit;
        Random random = new Random();
        //We need to preprocess the symb rules to (rule .....).
        int j = 0;
        long startTime = System.nanoTime();
        while(true) {
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
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            if(duration / 1000000000 > timeout) {
                break;   
            }
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
            output.append(line + "\n");
        }

        BufferedReader ereader = new BufferedReader(new InputStreamReader(p.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
        while ((line = ereader.readLine()) != null) {                                                  
            System.err.println(line);
        }

        System.out.println("Output: " + output.toString().trim());

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

        Option mode = new Option("m", "mode", true, "mode");
        mode.setRequired(false);
        options.addOption(mode);

        Option usesymb = new Option("symb", "usesymb", true, "usesymb");
        usesymb.setRequired(false);
        options.addOption(usesymb);

        Option minSymbSize = new Option("minsymb", "minSymbSize", true, "minSymbSize");
        minSymbSize.setRequired(false);
        options.addOption(minSymbSize);

        Option maxSymbSize = new Option("maxsymb", "maxSymbSize", true, "maxSymbSize");
        maxSymbSize.setRequired(false);
        options.addOption(maxSymbSize);

        Option gateset = new Option("g", "gateset", true, "gateset");
        gateset.setRequired(true);
        options.addOption(gateset);


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

        EggGen egraph = new EggGen();
        List<String> commutative = new ArrayList<>();
        String g = cmd.getOptionValue("gateset");
        FileReader fr = new FileReader("rules_" + g + ".txt", StandardCharsets.UTF_8);
        try (BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                commutative.add(line);
                egraph.addRewrite(line);
            }
        }

        String benchmarkFile = cmd.getOptionValue("benchmark");
        System.out.println(benchmarkFile);
        String rulesFile = cmd.getOptionValue("rule");
        String symrulesFile = cmd.getOptionValue("symbrule");
        String modeStr = cmd.getOptionValue("mode");
        int timeoutint = Integer.valueOf(cmd.getOptionValue("timeout"));
        boolean useSymb = Boolean.valueOf(cmd.getOptionValue("usesymb"));
        List<String> rules = new ArrayList<>();

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
       
        Optimizer optimizer = new Optimizer();
        //optimizer.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), egraph);
        if(modeStr.equals("egraph")) {
            optimizer.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), egraph, timeoutint);
            egraph.stopEgglogREPL();
        } else if(modeStr.equals("egraphsym")) {
            optimizer.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), rules, symbRules, 20, 5, timeoutint);
        } else if(modeStr.equals("SA")) {
            int minSymb = cmd.getOptionValue("minSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("minSymbSize")) : 10;
            int maxSymb = cmd.getOptionValue("maxSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("maxSymbSize")) : 30;
            optimizer.optimize_SA(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), rules, symbRules, rules.size()/3*2, 2, minSymb, maxSymb, timeoutint, useSymb, commutative);
        } else if(modeStr.equals("BEAM")) {
            int minSymb = cmd.getOptionValue("minSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("minSymbSize")) : 10;
            int maxSymb = cmd.getOptionValue("maxSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("maxSymbSize")) : 30;
            Comparator<EggGen.ConstrainedCircuit> comparator = new Comparator<EggGen.ConstrainedCircuit>() {
                public int compare(EggGen.ConstrainedCircuit a, EggGen.ConstrainedCircuit b) {
                    return a.circuit.getTwoQubitsCount() - b.circuit.getTwoQubitsCount();
                }
            };
            optimizer.optimize_BEAM(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), rules, symbRules, 10, rules.size()/3*2, 2, minSymb, maxSymb, timeoutint, useSymb, comparator, commutative);
        }
    }
}
