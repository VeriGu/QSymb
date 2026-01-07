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
import org.jgrapht.alg.lca.NaiveLCAFinder;
import java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import java.util.stream.Collectors;

public class Optimizer {
    private Verifier verifier;
    private SymbolicSolve solver;
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
                //System.out.println("Out Degree Mismatch");
                return false;
            }

            for (Edge circE : circuit.outgoingEdgesOf(circuitNode)) {
                //System.out.println("Circ Edge:" + circuitNode.toString() + "->" + circuit.getEdgeTarget(circE).toString() + "qubit" + circE.getQubit());
                if (patternToCirc.containsKey(pattern.getEdgeTarget(pattE))) {
                    
                    if (pattE.sameSourceTargetLabels(circE) && circuit.getEdgeTarget(circE) == patternToCirc.get(pattern.getEdgeTarget(pattE))) {
                        //System.out.println("pattern matched");
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
                            //System.out.println("Matched Target: " + pattern.getEdgeTarget(pattE).getId());
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
        //System.out.println("Trying to match pattern starting with: " + start.getId());
        //System.out.println("Pattern: " + pattern.toQASM());
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
                }
                List<Node> succsToVisit = new ArrayList<>();
                List<Node> ancsToVisit = new ArrayList<>();
                Set<Node> seen = new HashSet<>();
                //System.out.println("matched:" + patternToCirc.toString());
                if (!matchOutgoing(circuit.getDag(), pattern.getDag(), circN, start, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                    continue;
                }
                //System.out.println("Matched Outgoing");
                if (!matchIncoming(circuit.getDag(), pattern.getDag(), circN, start, patternToCirc, patternToCircEdges, angleMap, succsToVisit)) {
                    continue;
                }
                //System.out.println("Matched Incoming");
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
                        // System.out.println("Visiting anc" + anc.getId());
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
                    //System.out.println("Matched All: " + patternToCirc.toString());
                    matched.addAll(patternToCirc.values());
                    matches.add(new HashMap<>(patternToCirc));

                    Map<String, String> patternToCircuitQubit = patternToCircuitQubit(patternToCirc);
                    if (new HashSet<>(patternToCircuitQubit.values()).size() != patternToCircuitQubit.values().size()) {
                        continue;
                    }

                    if (copy == null) {
                        copy = new CircuitDAG(circuit);
                    }

                    //System.out.println("replace: " + replace);

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
                    //System.out.println("angleMap: " + angleMap.toString());
                    replaceAfterSubst = replaceAngles(replaceAfterSubst, angleMap);

                    //System.out.println("Replace After Subst: " + replaceAfterSubst);
                    CircuitDAG replaceDag = QASMToDAGVisitor.parse(replaceAfterSubst);
                    //System.out.println("Replace DAG: " + replaceDag.toQASM());
                    replaced.addAll(replaceDag.nodes());

                    replace(copy.getDag(), pattern, replaceDag, patternToCirc, patternToCircuitQubit);
                    //System.out.println("After replace: " + copy.toQASM());
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
       // System.out.println("With: " + replace.toQASM());
       // System.out.println("patternToCirc map: " + patternToCirc.toString());
        //System.out.println("patternToCircuitQubit map: " + patternToCircuitQubit.toString());

        
        Map<String, Node> patternRoots = pattern.rootsMap();
        Map<String, Node> patternLeaves = pattern.leavesMap();

        //System.out.println("Pattern leaves:" + patternLeaves.toString());
        //System.out.println("Pattern roots:" + patternRoots.toString());

        Map<String, Node> replaceRoots = replace.rootsMap();
        Map<String, Node> replaceLeaves = replace.leavesMap();

        //System.out.println("Replace leaves:" + replaceLeaves.toString());
        //System.out.println("Replace roots:" + replaceRoots.toString());

        Map<String, Node> ancPatternRoots = new HashMap<>();
        for (String qubit : patternRoots.keySet()) {
            String circQubit = patternToCircuitQubit.getOrDefault(qubit, qubit);
            Node match = patternToCirc.getOrDefault(patternRoots.get(qubit), patternRoots.get(qubit));
            for (Edge e : circuit.incomingEdgesOf(match)) {
                // System.out.println("Incoming Edge of " + match.toString() + ": " + e.getQubit());
                if (e.getQubit().equals(circQubit)) {
                    ancPatternRoots.put(circQubit, circuit.getEdgeSource(e));
                }

            }
        }
        // System.out.println("Incoming Ancestors: " + ancPatternRoots.toString());

        Map<String, Node> decPatternLeaves = new HashMap<>();
        for (String qubit : patternLeaves.keySet()) {
            String circQubit = patternToCircuitQubit.getOrDefault(qubit, qubit);
            Node match = patternToCirc.getOrDefault(patternLeaves.get(qubit), patternLeaves.get(qubit));
            //System.out.println("Pattern leave qubit: " + qubit + ": " + circQubit);
            for (Edge e : circuit.outgoingEdgesOf(match)) {
                //System.out.println("Outgoing Edge of " + match.toString() + ": " + e.getQubit());
                if (e.getQubit().equals(circQubit)) {
                    decPatternLeaves.put(circQubit, circuit.getEdgeTarget(e));
                }
            }
        }
        // System.out.println("Outgoing Descendants: " + decPatternLeaves.toString());
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
        // for (String qubit : replaceLeaves.keySet()) {
        //     // qubits not in replaceLeaves should not have been in replaceRoots and therefore were connected already to decPatternLeave
        //     if (decPatternLeaves.containsKey(qubit)) {
        //         circuit.addEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), pattern.getEdge(replaceLeaves.get(qubit), decPatternLeaves.get(qubit), qubit));
        //     } else {
        //         String circQubit = patternToCircuitQubit.getOrDefault(qubit, qubit);
        //         Node match = patternToCirc.getOrDefault(replaceLeaves.get(qubit), replaceLeaves.get(qubit));
        //         Edge newedge = null;
        //         Node outNode = null;
        //         for (Edge e : circuit.outgoingEdgesOf(match)) {
        //             if (e.getQubit().equals(circQubit)) {
        //                 newedge = pattern.getEdge(replaceLeaves.get(qubit), circuit.getEdgeTarget(e), qubit);
        //                 outNode = circuit.getEdgeTarget(e);
        //             }
        //         }
        //         if(newedge != null) {
        //             circuit.addEdge(match, outNode, newedge);
        //         }
        //     }
        // }
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
            // System.out.println("applyRule: No Match Found");
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
        //CircuitDAG dag = QASMToDAGVisitor.parse(circuit.toQASM());
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
        // System.out.println("LHS Before Circuit: " + lhsBeforeCircuit.toQASM());
        EggGen.Circuit lhsAfterCircuit = new EggGen.Circuit(lhsAfterGates);
        // System.out.println("LHS After Circuit: " + lhsAfterCircuit.toQASM());
        CircuitDAG lhsBeforeDag = QASMToDAGVisitor.parse(lhsBeforeCircuit.toQASM());
        CircuitDAG lhsAfterDag = QASMToDAGVisitor.parse(lhsAfterCircuit.toQASM());
        Map<String, String> qubitMap = new HashMap<>();
        Map<String, String> reverseMap = new HashMap<>();
        List<Node> matchedNodes = null;
        if (!GraphTests.isConnected(lhsBeforeDag.getDAG()) && lhsBeforeCircuit.gates.size() > 0) {
            System.out.println("Symbolic LHS Before is not connected");
            return null;
        }
        if (!GraphTests.isConnected(lhsAfterDag.getDAG()) && lhsAfterCircuit.gates.size() > 0) {
            System.out.println("Symbolic LHS After is not connected");
            return null;
        }
        if(lhsBeforeCircuit.gates.size() > 0 && lhsAfterCircuit.gates.size() > 0) {
            matchedNodes = matchBeforeAfter(circuit, lhsBeforeDag, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, constraints, null, qubitMap, reverseMap, true);
        } else if(lhsBeforeCircuit.gates.size() == 0 && lhsAfterCircuit.gates.size() > 0) {
            matchedNodes = matchAfter(circuit, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, constraints, null, qubitMap, reverseMap, true);
        } else if(lhsBeforeCircuit.gates.size() > 0 && lhsAfterCircuit.gates.size() == 0) {
            matchedNodes = matchBefore(circuit, lhsBeforeDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, constraints, null, qubitMap, reverseMap, true);
        }
        if(matchedNodes == null) {
            //System.out.println("No Match Before After found");
            return null;
        }
        // System.out.println("Reverse Map: " + reverseMap.toString());
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
        //.println("RHS Combined: " + rhsCircuit.toQASM());
        //System.out.println("LHS Combined: " + lhsCircuit.toQASM());
        // try {
        //     if(checkEquivalenceWithQiskit(lhsCircuit.toQASM(), rhsCircuit.toQASM(), lhsCircuit.getMaxQubits()+1)){
        //         System.out.println("Equivalent to Qiskit");
        //     }
        // } catch (Exception e) {
        //     System.out.println("Error checking equivalence with Qiskit");
        //     e.printStackTrace();
        // }
        if(egraph != null)
            egraph.sendCommand(String.format("(union %s %s)", lhsCircuit.toEggString(), rhsCircuit.toEggString()));
        
        EggGen.ConstrainedCircuit constrainedRhsCircuit = new EggGen.ConstrainedCircuit(rhsCircuit, new EggGen.Permutation(new ArrayList<>()));
        Random rand = new Random();
        CircuitDAG result = applyRule(circuit, lhsDag, rhsCircuit.toQASM(), true, rand);
        String qasm = result.toQASM();
        //System.out.println("Before: " + circuit.toQASM());
        //System.out.println("Result: " + qasm);
        return result;
    }

    public CircuitDAG symbolicMatchBeforeAfter(CircuitDAG circuit, String rule, String rhs, int minSymbSize, int maxSymbSize, List<SymbolicSolve.SparseMatrix> basis, EggGen egraph) {
        //CircuitDAG dag = QASMToDAGVisitor.parse(circuit.toQASM());
        Map<String, Expr> angleMap = new HashMap<>();
        rule = rule.replaceAll("\\bc\\b", "(Nil)");
        rule = rule.replaceAll("q\\d+", "(Q \"$0\")");
        rule = rule.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
        // System.out.println("Rule: " + rule);
        EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(rule);
        // System.out.println("Symbolic LHS: " + symblhs.toQASM());
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
        // System.out.println("LHS Before Circuit: " + lhsBeforeCircuit.toQASM());
        EggGen.Circuit lhsAfterCircuit = new EggGen.Circuit(lhsAfterGates);
        // System.out.println("LHS After Circuit: " + lhsAfterCircuit.toQASM());
        CircuitDAG lhsBeforeDag = QASMToDAGVisitor.parse(lhsBeforeCircuit.toQASM());
        CircuitDAG lhsAfterDag = QASMToDAGVisitor.parse(lhsAfterCircuit.toQASM());
        Map<String, String> qubitMap = new HashMap<>();
        Map<String, String> reverseMap = new HashMap<>();
        System.out.println("LHS before:" + lhsBeforeDag.toQASM());
        System.out.println("LHS after:" + lhsAfterDag.toQASM());
        if (lhsBeforeCircuit.gates.size() > 0 && !GraphTests.isConnected(lhsBeforeDag.getDAG())) {
            System.out.println("Symbolic LHS is not connected");
            return null;    
        }
        if (lhsAfterCircuit.gates.size() > 0 && !GraphTests.isConnected(lhsAfterDag.getDAG())) {
            System.out.println("Symbolic LHS After is not connected");
            return null;
        }
        List<Node> matchedNodes = null;
        if(lhsBeforeCircuit.gates.size() > 0 && lhsAfterCircuit.gates.size() > 0) {
            matchedNodes = matchBeforeAfter(circuit, lhsBeforeDag, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false);
        } else if(lhsBeforeCircuit.gates.size() == 0 && lhsAfterCircuit.gates.size() > 0) {
            matchedNodes = matchAfter(circuit, lhsAfterDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false);
        } else if(lhsBeforeCircuit.gates.size() > 0 && lhsAfterCircuit.gates.size() == 0) {
            matchedNodes = matchBefore(circuit, lhsBeforeDag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false);
        }
        if(matchedNodes == null) {
            //System.out.println("No Match Before After found");
            return null;
        }
        // System.out.println("Reverse Map: " + reverseMap.toString());
        rhs = rhs.replaceAll("\\bc\\b", "(Nil)");
        rhs = rhs.replaceAll("q\\d+", "(Q \"$0\")");
        rhs = rhs.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
        EggGen.Circuit symbrhs = EggAstBuilder.parseCircuit(rhs);
        EggGen.Circuit symbrhsCan = EggGen.canonicalizeCircuit(symbrhs, reverseMap);
        EggGen.Circuit symbrhsCanInstantiated = symbrhsCan.instantiate(angleMap);
        System.out.println("Symbolic RHS: " + symbrhsCanInstantiated.toQASM());
        List<EggGen.Gate> rhsCan = symbrhsCanInstantiated.gates;
        i = 0;
        for(EggGen.Gate gate : rhsCan) {
            if(gate instanceof EggGen.SYMB) {
                symbindex = i;
            }
            i++;
        }
        List<EggGen.Gate> rhsBeforeGates = new ArrayList<>(rhsCan.subList(0, symbindex));
        List<EggGen.Gate> rhsAfterGates = new ArrayList<>(rhsCan.subList(symbindex + 1, rhsCan.size()));
        List<Node> matchedsymb = matchedNodes.subList(lhsBeforeGates.size(), matchedNodes.size() - lhsAfterGates.size());
        List<EggGen.Gate> symbGates = nodesToGates(matchedsymb);
        

        EggGen.Circuit lhsCircuit = new EggGen.Circuit(nodesToGates(matchedNodes));
        CircuitDAG lhsDag = QASMToDAGVisitor.parse(lhsCircuit.toQASM());
        List<EggGen.Gate> rhsCombined = new ArrayList<>(rhsBeforeGates);
        rhsCombined.addAll(symbGates);
        rhsCombined.addAll(new ArrayList<>(rhsAfterGates));
        EggGen.Circuit rhsCircuit = new EggGen.Circuit(rhsCombined);
        //System.out.println("RHS Combined: " + rhsCircuit.toQASM());
        //System.out.println("LHS Combined: " + lhsCircuit.toQASM());
       
        if(egraph != null)
            egraph.sendCommand(String.format("(union %s %s)", lhsCircuit.toEggString(), rhsCircuit.toEggString()));
        
        EggGen.ConstrainedCircuit constrainedRhsCircuit = new EggGen.ConstrainedCircuit(rhsCircuit, new EggGen.Permutation(new ArrayList<>()));
        Random rand = new Random();
        CircuitDAG result = applyRule(circuit, lhsDag, rhsCircuit.toQASM(), true, rand);
        String qasm = result.toQASM();
        //e: " + circuit.toQASM());
        //System.out.println("Result: " + qasm);
        return result;
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
            List<Node> matchedNodes = matchBefore(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false);
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
                // System.out.println("Angle: " + angle + " -> " + angleMap.get(angle).toEggString());
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
            //System.out.println("Before: " + dag.toQASM());
            //System.out.println("Result: " + qasm);
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
            List<Node> matchedNodes = matchAfter(dag, symbdag, angleMap, Params.MAX_QUBITS_SYMB, minSymbSize, maxSymbSize, null, basis, qubitMap, reverseMap, false);
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


    private List<Node> matchBeforeAfter(CircuitDAG dag, CircuitDAG symbbefore, CircuitDAG symbafter, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<Map<boolean[], boolean[]>> constraints, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap, boolean isMono) {
        List<Node> beforeRoots = symbbefore.getCircuitRoots();
        List<Node> afterRoots = symbafter.getCircuitRoots();
        Node patternBeforeStart = beforeRoots.get(0);
        Node patternAfterStart = afterRoots.get(0);
        
        List<List<Node>> layers = dag.topoSort();
        for(int i = 0; i < layers.size(); i++) {
            List<Node> layer = layers.get(i);
            for(Node node : layer) {
                qubitMap.clear();
                reverseMap.clear();
                angleMap.clear();
                if(node.isGate() && node.getId().equals(patternBeforeStart.getId())) {
                    if(patternBeforeStart.getAngles() != null) {
                        if(!matchAngles(node, patternBeforeStart, angleMap)) {
                            continue;
                        }
                    }
                    //System.out.println("Before Pattern" + symbbefore.toQASM());
                    //System.out.println("angleMap" + angleMap);
                    //System.out.println("matched node: " + node.getId());
                    for(int j = 0; j < node.getQubits().size(); j++) {
                        qubitMap.put(node.getQubits().get(j), patternBeforeStart.getQubits().get(j));
                        reverseMap.put(patternBeforeStart.getQubits().get(j), node.getQubits().get(j));
                    }
                    // System.out.println("Qubit Map: " + qubitMap);
                    // System.out.println("Reverse Map: " + reverseMap);
                    Node next = Graphs.successorListOf(symbbefore.getDAG(), patternBeforeStart).get(0);
                    int s = 1;
                    boolean foundOutter = true;
                    while(!next.isSinkQubit()) {
                        boolean found = false;
                        for(Node circN3: layers.get(i + s)) {
                            // System.out.println("Next Pattern: " + next.getId());
                            // System.out.println("Next Concrete: " + circN3.getId());
                            // System.out.println("Pattern Qubits: " + next.getQubits());
                            // System.out.println("Concrete Qubits: " + circN3.getQubits());
                            if(circN3.isGate() && circN3.getId().equals(next.getId())) {
                                // System.out.println("Intermediate Matched Node: " + circN3.getId());
                                if(!next.getAngles().isEmpty()) {
                                    if(matchAngles(circN3, next, angleMap)) {
                                        boolean qubitMatch = true;
                                        for(int j = 0; j < circN3.getQubits().size(); j++) {
                                            if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                                if(!qubitMap.get(circN3.getQubits().get(j)).equals(next.getQubits().get(j))) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                }
                                            } else {
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                }
                                                
                                            }
                                            qubitMap.put(circN3.getQubits().get(j), next.getQubits().get(j));
                                            reverseMap.put(next.getQubits().get(j), circN3.getQubits().get(j));
                                        }
                                        if(qubitMatch) {
                                            found = true;
                                            break;
                                        }
                                    }
                                } else {
                                    boolean qubitMatch = true;
                                    for(int j = 0; j < circN3.getQubits().size(); j++) {
                                        if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                            if(!qubitMap.get(circN3.getQubits().get(j)).equals(next.getQubits().get(j))) {
                                                qubitMatch = false;
                                                break;
                                            }
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                            }
                                        } else {
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                            }
                                           
                                        }
                                        qubitMap.put(circN3.getQubits().get(j), next.getQubits().get(j));
                                        reverseMap.put(next.getQubits().get(j), circN3.getQubits().get(j));
                                    }
                                    if(qubitMatch) {
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if(!found) {
                            // System.out.println("Did not find the next node");
                            foundOutter = false;
                            break;
                        }
                        next = Graphs.successorListOf(symbbefore.getDAG(), next).get(0);
                        s++;
                    }
                    if(!foundOutter) {
                        continue;
                    }
                    // System.out.println("After match before Qubit Map: " + qubitMap);
                    // System.out.println("After match before Reverse Map: " + reverseMap);
                    //start to grow symbolic circuit
                    Set<String> blockedQubits = new HashSet<>();
                    Set<String> trackedQubits = new HashSet<>();
                    List<Node> symb = new ArrayList<>();
                    List<Node> symbToReplace = new ArrayList<>();
                    symbToReplace.add(node);
                    next = Graphs.successorListOf(symbbefore.getDAG(), patternBeforeStart).get(0);
                    Node circNext = Graphs.successorListOf(dag.getDAG(), node).get(0);
                    while (!next.isSinkQubit()) {
                        symbToReplace.add(next);
                        circNext = Graphs.successorListOf(dag.getDAG(), circNext).get(0);
                        next = Graphs.successorListOf(symbbefore.getDAG(), next).get(0);
                    }

                    trackedQubits.addAll(node.getQubits());
                    for(int j = i + s; j < layers.size(); j++) {
                        if(trackedQubits.size() > maxSymbQubits || symb.size() > maxSymbSize) {
                            break;
                        }
                        List<Node> layerJ = layers.get(j);
                        
                        for(Node circN: layerJ) {
                            Map<String, String> tempQubitMap = new HashMap<>(qubitMap);
                            Map<String, String> tempReverseMap = new HashMap<>(reverseMap);
                            Map<String, Expr> tempAngleMap = new HashMap<>(angleMap);
                            // System.out.println("TempQubit" + tempQubitMap);
                            // System.out.println("TempReverse" + tempReverseMap);
                            // System.out.println("Checking node: " + circN.getId());
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

                                //match after here
                                boolean match = false;
                                // System.out.println("AngleMap:" + angleMap);
                                if(!blockedQubits.contains(circN.getQubits().get(0)) && circN.getId().equals(patternAfterStart.getId())) {
                                    // System.out.println("Matched Root: " + circN.getId());
                                    if(!patternAfterStart.getAngles().isEmpty()) {
                                        if(matchAngles(circN, patternAfterStart, tempAngleMap)){
                                            //System.out.println("Matched angle");
                                            boolean roottQubitMatch = true;
                                            for(int k = 0; k < circN.getQubits().size(); k++) {
                                                // System.out.println("CircN:" + circN.getQubits().get(k));
                                                // System.out.println("PatternAfterStart:" + patternAfterStart.getQubits().get(k));
                                                if(tempQubitMap.containsKey(circN.getQubits().get(k))) {
                                                    if(!tempQubitMap.get(circN.getQubits().get(k)).equals(patternAfterStart.getQubits().get(k))) {;
                                                        roottQubitMatch = false;
                                                        break;
                                                    }
                                                    if(tempReverseMap.containsKey(patternAfterStart.getQubits().get(k))) {
                                                        if(!tempReverseMap.get(patternAfterStart.getQubits().get(k)).equals(circN.getQubits().get(k))) {
                                                            roottQubitMatch = false;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    if(tempReverseMap.containsKey(patternAfterStart.getQubits().get(k))) {
                                                        if(!tempReverseMap.get(patternAfterStart.getQubits().get(k)).equals(circN.getQubits().get(k))) {
                                                            roottQubitMatch = false;
                                                            break;
                                                        }
                                                    }
                                                    
                                                }
                                                tempQubitMap.put(circN.getQubits().get(k), patternAfterStart.getQubits().get(k));
                                                tempReverseMap.put(patternAfterStart.getQubits().get(k), circN.getQubits().get(k));
                                            }
                                            if(roottQubitMatch) {
                                                Node nextA = Graphs.successorListOf(symbafter.getDAG(), patternAfterStart).get(0);
                                                int t = 1;
                                                boolean found = true;
                                                    
                                                while(!nextA.isSinkQubit()) {
                                                    boolean foundInner = false;
                                                    for(Node node2: layers.get(i + t)) {
                                                        // System.out.println("Trying to match Node2: " + node2.getId());
                                                        if(node2.isGate() && node2.getId().equals(nextA.getId())) {
                                                            if(!nextA.getAngles().isEmpty()) {
                                                                if(matchAngles(node2, nextA, tempAngleMap)) {
                                                                    boolean qubitMatch = true;
                                                                    for(int k = 0; k < node2.getQubits().size(); k++) {
                                                                        if(tempQubitMap.containsKey(node2.getQubits().get(k))) {
                                                                            if(!tempQubitMap.get(node2.getQubits().get(k)).equals(nextA.getQubits().get(k))) {
                                                                                qubitMatch = false;
                                                                                break;
                                                                            }
                                                                            if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                                if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                                    qubitMatch = false;
                                                                                    break;
                                                                                }
                                                                            }
                                                                        } else {
                                                                            if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                                if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                                    qubitMatch = false;
                                                                                    break;
                                                                                }
                                                                            }
                                                                            
                                                                        }
                                                                        tempQubitMap.put(node2.getQubits().get(k), nextA.getQubits().get(k));
                                                                        tempReverseMap.put(nextA.getQubits().get(k), node2.getQubits().get(k));
                                                                    }
                                                                    // System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                                    if(!qubitMatch) {
                                                                        foundInner = false;
                                                                        break;
                                                                    }
                                                                }
                                                                
                                                            }
                                                            else {
                                                                boolean qubitMatch = true;
                                                                for(int k = 0; k < node2.getQubits().size(); k++) {
                                                                    if(tempQubitMap.containsKey(node2.getQubits().get(k))) {
                                                                        if(!tempQubitMap.get(node2.getQubits().get(k)).equals(nextA.getQubits().get(k))) {
                                                                            qubitMatch = false;
                                                                            break;
                                                                        }
                                                                        if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                            if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                                qubitMatch = false;
                                                                                break;
                                                                            }
                                                                        }
                                                                    } else {
                                                                        if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                            if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                                qubitMatch = false;
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    tempQubitMap.put(node2.getQubits().get(k), nextA.getQubits().get(k));
                                                                    tempReverseMap.put(nextA.getQubits().get(k), node2.getQubits().get(k));
                                                                }
                                                                // System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                                if(qubitMatch) {
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
                                                    nextA = Graphs.successorListOf(symbafter.getDAG(), nextA).get(0);
                                                    t++;
                                                }
                                                if(found) {
                                                    System.out.println("Matched the entire pattern");
                                                    match = true;
                                                }
                                            }
                                        }
                                    } else {
                                        //System.out.println("Matched Angles");
                                        boolean roottQubitMatch = true;
                                        for(int k = 0; k < circN.getQubits().size(); k++) {
                                            // System.out.println("CircN:" + circN.getQubits().get(k));
                                            // System.out.println("PatternAfterStart:" + patternAfterStart.getQubits().get(k));
                                            if(tempQubitMap.containsKey(circN.getQubits().get(k))) {
                                                if(!tempQubitMap.get(circN.getQubits().get(k)).equals(patternAfterStart.getQubits().get(k))) {;
                                                    roottQubitMatch = false;
                                                    break;
                                                }
                                                if(tempReverseMap.containsKey(patternAfterStart.getQubits().get(k))) {
                                                    if(!tempReverseMap.get(patternAfterStart.getQubits().get(k)).equals(circN.getQubits().get(k))) {
                                                        roottQubitMatch = false;
                                                        break;
                                                    }
                                                }
                                            } else {
                                                if(tempReverseMap.containsKey(patternAfterStart.getQubits().get(k))) {
                                                    if(!tempReverseMap.get(patternAfterStart.getQubits().get(k)).equals(circN.getQubits().get(k))) {
                                                        roottQubitMatch = false;
                                                        break;
                                                    }
                                                }
                                                
                                            }
                                            tempQubitMap.put(circN.getQubits().get(k), patternAfterStart.getQubits().get(k));
                                            tempReverseMap.put(patternAfterStart.getQubits().get(k), circN.getQubits().get(k));
                                        }
                                        if(roottQubitMatch) {
                                            Node nextA = Graphs.successorListOf(symbafter.getDAG(), patternAfterStart).get(0);
                                            int t = 1;
                                            boolean found = true;
                                            while(!nextA.isSinkQubit()) {
                                                boolean foundInner = false;
                                                for(Node node2: layers.get(i + t)) {
                                                    //System.out.println("Trying to match Node2: " + node2.getId());
                                                    if(node2.isGate() && node2.getId().equals(nextA.getId())) {
                                                        if(!nextA.getAngles().isEmpty()) {
                                                            if(matchAngles(node2, nextA, tempAngleMap)) {
                                                                boolean qubitMatch = true;
                                                                for(int k = 0; k < node2.getQubits().size(); k++) {
                                                                    if(tempQubitMap.containsKey(node2.getQubits().get(k))) {
                                                                        if(!tempQubitMap.get(node2.getQubits().get(k)).equals(nextA.getQubits().get(k))) {
                                                                            qubitMatch = false;
                                                                            break;
                                                                        }
                                                                        if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                            if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                                qubitMatch = false;
                                                                                break;
                                                                            }
                                                                        }
                                                                    } else {
                                                                        if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                            if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                                qubitMatch = false;
                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                    tempQubitMap.put(node2.getQubits().get(k), nextA.getQubits().get(k));
                                                                    tempReverseMap.put(nextA.getQubits().get(k), node2.getQubits().get(k));
                                                                
                                                                }
                                                                System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                                if(qubitMatch) {
                                                                    foundInner = true;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        else {
                                                            boolean qubitMatch = true;
                                                            for(int k = 0; k < node2.getQubits().size(); k++) {
                                                                if(tempQubitMap.containsKey(node2.getQubits().get(k))) {
                                                                    if(!tempQubitMap.get(node2.getQubits().get(k)).equals(nextA.getQubits().get(k))) {
                                                                        qubitMatch = false;
                                                                        break;
                                                                    }
                                                                    if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                        if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                            qubitMatch = false;
                                                                            break;
                                                                        }
                                                                    }
                                                                } else {
                                                                    if(tempReverseMap.containsKey(nextA.getQubits().get(k))) {
                                                                        if(!tempReverseMap.get(nextA.getQubits().get(k)).equals(node2.getQubits().get(k))) {
                                                                            qubitMatch = false;
                                                                            break;
                                                                        }
                                                                    }
                                                                    
                                                                }
                                                                tempQubitMap.put(node2.getQubits().get(k), nextA.getQubits().get(k));
                                                                tempReverseMap.put(nextA.getQubits().get(k), node2.getQubits().get(k));
                                                            }
                                                            System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                            if(qubitMatch) {
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
                                                nextA = Graphs.successorListOf(symbafter.getDAG(), nextA).get(0);
                                                t++;
                                            }
                                            if(found) {
                                                System.out.println("Matched the entire pattern");
                                                match = true;
                                            }
                                        }
                                    }
                                }
                                //System.out.println("After match anglemap" + angleMap);
                                //System.out.println("After match after Qubit Map: " + tempQubitMap);
                                //System.out.println("After match after Reverse Map: " + tempReverseMap);
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
                                                    System.out.println("S Satisfy the monomial constraints");
                                                    qubitMap.clear();
                                                    qubitMap.putAll(tempQubitMap);
                                                    reverseMap.clear();
                                                    reverseMap.putAll(tempReverseMap);
                                                    angleMap.clear();
                                                    angleMap.putAll(tempAngleMap);
                                                    symbToReplace.add(circN);
                                                    Node nextA = Graphs.successorListOf(symbafter.getDAG(), patternAfterStart).get(0);
                                                    Node circNextA = Graphs.successorListOf(dag.getDAG(), circN).get(0);
                                                    while (!nextA.isSinkQubit()) {
                                                        symbToReplace.add(circNextA);
                                                        circNextA = Graphs.successorListOf(dag.getDAG(), circNextA).get(0);
                                                        nextA = Graphs.successorListOf(symbafter.getDAG(), nextA).get(0);
                                                    }
                                                    return symbToReplace;
                                                } 
                                                //System.out.println("S did not satisfy the monomial constraints");
                                            }
                                        } else {
                                            //check for constraints
                                            //System.out.println("checking constraints:");
                                            
                                            //canonicalize the circuit based on qubit map
                                            EggGen.Circuit canonicalizedCirc = EggGen.canonicalizeCircuit(symbCircConst.circuit, new HashMap<>(tempQubitMap));
        
                                            //System.out.println("Canonicalized Circuit: " + canonicalizedCirc.toQASM());
                                            try {
                                                List<Integer> subspace = new ArrayList<>();
                                                subspace.add(0);
                                                subspace.add(1);
                                                //System.out.println("Subspace: " + subspace);
                                                if(checkLinearCombination(canonicalizedCirc, basis, subspace, tempAngleMap)) {
                                                    //satisfy the constraints
                                                    System.out.println("S Satisfy the constraints");
                                                    symbToReplace.add(circN);
                                                    qubitMap.clear();
                                                    qubitMap.putAll(tempQubitMap);
                                                    reverseMap.clear();
                                                    reverseMap.putAll(tempReverseMap);
                                                    angleMap.clear();
                                                    angleMap.putAll(tempAngleMap);
                                                    Node nextA = Graphs.successorListOf(symbafter.getDAG(), patternAfterStart).get(0);
                                                    Node circNextA = Graphs.successorListOf(dag.getDAG(), circN).get(0);
                                                    while (!nextA.isSinkQubit()) {
                                                        symbToReplace.add(circNextA);
                                                        circNextA = Graphs.successorListOf(dag.getDAG(), circNextA).get(0);
                                                        nextA = Graphs.successorListOf(symbafter.getDAG(), nextA).get(0);
                                                    }
                                                    return symbToReplace;
                                                } else {
                                                    System.out.println("S did not satisfy the constraints");
                                                }
                                            } catch (IOException | InterruptedException e) {
                                                System.err.println("Error checking linear combination: " + e.getMessage());
                                            }
                                        }
                                    }
                                }

                                Set<String> blockedIntersection = new HashSet<>(blockedQubits);
                                blockedIntersection.retainAll(circN.getQubits());
                                if(!symbToReplace.contains(circN) && blockedIntersection.isEmpty()) {
                                    symb.add(circN);
                                    symbToReplace.add(circN);
                                    //System.out.println("Added node: " + symbToReplace.toString());
                                    //System.out.println("Symb: " + symb.toString());
                                } else {
                                    if(node.isCCZ()) {
                                        if(!blockedQubits.contains(circN.getQubits().get(2))) { 
                                            //block target qubit
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

    private List<Node> matchAfter(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<Map<boolean[], boolean[]>> constraints, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap, boolean isMono) {
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
            //System.out.println("layer:" + (i+1) + "/" + layers.size());
            for(Node node : layers.get(i)) {
                qubitMap.clear();
                reverseMap.clear();
                angleMap.clear();
                if(!node.isGate()) {
                    continue;
                }

                //System.out.println("Trying to match Node: " + node.toString());
                Set<String> trackedIntersection = new HashSet<>(trackedQubits);
                //System.out.println("Tracked Qubits" + trackedQubits);
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
                        //System.out.println("Matched Root: " + node.getId());
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
                                        //System.out.println("Trying to match Node2: " + node2.getId());
                                        if(node2.isGate() && node2.getId().equals(nextA.getId())) {
                                            if(!nextA.getAngles().isEmpty()) {
                                                if(matchAngles(node2, nextA, angleMap)) {
                                                    
                                                    boolean qubitMatch = true;
                                                    for(int j = 0; j < node2.getQubits().size(); j++) {
                                                        if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                            if(!qubitMap.get(node2.getQubits().get(j)).equals(nextA.getQubits().get(j))) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                            if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                                if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                                    qubitMatch = false;
                                                                    break;
                                                                }
                                                            }
                                                        } else {
                                                            if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                                if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                                    qubitMatch = false;
                                                                    break;
                                                                }
                                                            }
                                                            
                                                        }
                                                        qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                        reverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                                    }
                                                    System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                    if(qubitMatch) {
                                                        foundInner = true;
                                                        break;
                                                    }
                                                }
                                                
                                            }
                                            else {
                                                boolean qubitMatch = true;
                                                for(int j = 0; j < node2.getQubits().size(); j++) {
                                                    if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                        if(!qubitMap.get(node2.getQubits().get(j)).equals(nextA.getQubits().get(j))) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    } else {
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                    reverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                                }
                                                //System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                if(qubitMatch) {
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
                                    //System.out.println("Trying to match Node2: " + node2.getId());
                                    if(node2.isGate() && node2.getId().equals(nextA.getId())) {
                                        if(nextA.getAngles() != null) {
                                            if(matchAngles(node2, nextA, angleMap)) {
                                            
                                                boolean qubitMatch = true;
                                                for(int j = 0; j < node2.getQubits().size(); j++) {
                                                    if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                        if(!qubitMap.get(node2.getQubits().get(j)).equals(nextA.getQubits().get(j))) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    } else {
                                                        if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                            if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                                qubitMatch = false;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                    reverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                                   
                                                }
                                                //System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                                if(qubitMatch) {
                                                    foundInner = true;
                                                    break;
                                                }
                                            }
                                        }
                                        else {
                                            boolean qubitMatch = true;
                                            for(int j = 0; j < node2.getQubits().size(); j++) {
                                                if(qubitMap.containsKey(node2.getQubits().get(j))) {
                                                    if(qubitMap.get(node2.getQubits().get(j)) != nextA.getQubits().get(j)) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                    if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                        if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                    }
                                                } else {
                                                    if(reverseMap.containsKey(nextA.getQubits().get(j))) {
                                                        if(!reverseMap.get(nextA.getQubits().get(j)).equals(node2.getQubits().get(j))) {
                                                            qubitMatch = false;
                                                            break;
                                                        }
                                                    }
                                                    
                                                }
                                                qubitMap.put(node2.getQubits().get(j), nextA.getQubits().get(j));
                                                reverseMap.put(nextA.getQubits().get(j), node2.getQubits().get(j));
                                            }
                                            //System.out.println("Match node: " + node2.getId() + " nextA: " + nextA.getId());
                                            if(qubitMatch) {
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
                        System.out.println("Matched Nodes: " + symbToReplace.toString());
                        System.out.println("Symb: " + symb.toString());
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
                                        System.out.println("S satisfy the monomial constraints");
                                        symbToReplace.add(node);
                                        Node nextA = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                                        Node circNextA = Graphs.successorListOf(dag.getDAG(), node).get(0);
                                        while (!nextA.isSinkQubit()) {
                                            symbToReplace.add(circNextA);
                                            circNextA = Graphs.successorListOf(dag.getDAG(), circNextA).get(0);
                                            nextA = Graphs.successorListOf(symbdag.getDAG(), nextA).get(0);
                                        }
                                        return symbToReplace;
                                    } else {
                                        System.out.println("S does not satisfy the monomial constraints");
                                    }
                                }
                            } else {
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
                    }
                        
                    Set<String> blockedIntersection = new HashSet<>(blockedQubits);
                    blockedIntersection.retainAll(node.getQubits());
                    if(!symbToReplace.contains(node) && blockedIntersection.isEmpty()) {
                        symb.add(node);
                        symbToReplace.add(node);
                        //System.out.println("Added node: " + symbToReplace.toString());
                        //System.out.println("Symb: " + symb.toString());
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
        // System.out.println("Pattern:" + pattern);
        // System.out.println("Circ:" + circ);
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
        } else if (pattern instanceof Real){
            if (circ instanceof Real) {
                return sameAngle(pattern, circ);
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

    public List<Node> matchBefore(CircuitDAG dag, CircuitDAG symbdag, Map<String, Expr> angleMap, int maxSymbQubits, int minSymbSize, int maxSymbSize, List<Map<boolean[], boolean[]>> constraints, List<SymbolicSolve.SparseMatrix> basis, Map<String, String> qubitMap, Map<String, String> reverseMap, boolean isMono) {
        List<Node> roots = symbdag.getCircuitRoots();
        Node patternRoot = roots.get(0);
        List<List<Node>> layers = dag.topoSort();

        for(int i = 0; i < layers.size(); i++) {
            List<Node> layer = layers.get(i);
            for(Node node : layer) {
                qubitMap.clear();
                reverseMap.clear();
                angleMap.clear();
                // System.out.println("Pattern: " + symbdag.toQASM());
                if(node.isGate() && node.getId().equals(patternRoot.getId())) {
                    if(patternRoot.getAngles() != null) {
                        if(!matchAngles(node, patternRoot, angleMap)) {
                            continue;
                        }
                    }
                    // System.out.println("Angle Map:" + angleMap);
                    // System.out.println("matched node: " + node.getId());
                    for(int j = 0; j < node.getQubits().size(); j++) {
                        qubitMap.put(node.getQubits().get(j), patternRoot.getQubits().get(j));
                        reverseMap.put(patternRoot.getQubits().get(j), node.getQubits().get(j));
                    }
                    //System.out.println("Qubit Map: " + qubitMap);
                    Node next = Graphs.successorListOf(symbdag.getDAG(), patternRoot).get(0);
                    //System.out.println("Next:" + next.toString());
                    int s = 1;
                    boolean foundOutter = true;
                    while(!next.isSinkQubit()) {
                        boolean found = false;
                        for(Node circN3: layers.get(i + s)) {
                            // System.out.println("Next Pattern: " + next.getId());
                            // System.out.println("Next Concrete: " + circN3.getId());
                            // System.out.println("Pattern Qubits: " + next.getQubits());
                            // System.out.println("Concrete Qubits: " + circN3.getQubits());
                            if(circN3.isGate() && circN3.getId().equals(next.getId())) {
                                //System.out.println("Intermediate Matched Node: " + circN3.getId());
                                if(!next.getAngles().isEmpty()) {
                                    if(matchAngles(circN3, next, angleMap)) {
                                        System.out.println("Angle Map:" + angleMap);
                                        boolean qubitMatch = true;
                                        for(int j = 0; j < circN3.getQubits().size(); j++) {
                                            if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                                if(!qubitMap.get(circN3.getQubits().get(j)).equals(next.getQubits().get(j))) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                }
                                            } else {
                                                if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                    if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                        qubitMatch = false;
                                                        break;
                                                    }
                                                }
                                                
                                            }
                                            qubitMap.put(circN3.getQubits().get(j), next.getQubits().get(j));
                                            reverseMap.put(next.getQubits().get(j), circN3.getQubits().get(j));
                                        }
                                        if(qubitMatch) {
                                            found = true;
                                            break;
                                        }
                                    }
                                } else {
                                    boolean qubitMatch = true;
                                    for(int j = 0; j < circN3.getQubits().size(); j++) {
                                        if(qubitMap.containsKey(circN3.getQubits().get(j))) {
                                            if(!qubitMap.get(circN3.getQubits().get(j)).equals(node.getQubits().get(j))) {
                                                qubitMatch = false;
                                                break;
                                            }
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                            }
                                        } else {
                                            if(reverseMap.containsKey(next.getQubits().get(j))) {
                                                if(!reverseMap.get(next.getQubits().get(j)).equals(circN3.getQubits().get(j))) {
                                                    qubitMatch = false;
                                                    break;
                                                }
                                            }
                                           
                                        }
                                        qubitMap.put(circN3.getQubits().get(j), next.getQubits().get(j));
                                        reverseMap.put(next.getQubits().get(j), circN3.getQubits().get(j));
                                    }
                                    if(qubitMatch) {
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if(!found) {
                            //System.out.println("Did not find the next node");
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
                            //System.out.println("Checking node: " + circN.getId());
                            if(!circN.isGate()) {
                                Circuit symbCirc = opsToCircuit(symb);
                                EggGen.ConstrainedCircuit symbCircConst = CircuitTranslator.translate(symbCirc);
                                if(symbCirc.getUsedQubits().size() <= maxSymbQubits && symb.size() >= minSymbSize) {
                                    //check for constraints
                                    //canonicalize the circuit based on qubit map
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
                                                System.out.println("S satisfy Monomial");
                                                return symbToReplace;
                                            } else {
                                                System.out.println("S does not satisfy Monomial");
                                            }
                                        }
                                    } else {
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
                                    if(isMono) {
                                        System.out.println("Checking monomial constraints:");
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
                                                return symbToReplace;
                                            } else {
                                                System.out.println("S did not satisfy the constraints");
                                            }
                                        } catch (IOException | InterruptedException e) {
                                            System.err.println("Error checking linear combination: " + e.getMessage());
                                        }
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
        //System.out.print("Output:" + output);

        return output.toString().trim().equals("true");
    }

    
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


    public List<String> filterValidRules(List<String> rules) {
        List<String> validRules = new ArrayList<>();
        for(String rule: rules) {
            String[] splitRule = rule.split(" \\| ");
            String pattern = splitRule[1];
            String replace = splitRule[0];
            CircuitDAG patternDag = QASMToDAGVisitor.parse(pattern);
            CircuitDAG replaceDag = QASMToDAGVisitor.parse(replace);
            if (pattern.contains("+") || pattern.contains("-")) {
                continue;
            }
            
            if(patternDag.getDagHash() == replaceDag.getDagHash()) {
                continue;
            }

            if(!GraphTests.isConnected(patternDag.getDAG())) {
                continue;
            }

            if(replace.contains("theta1") && !pattern.contains("theta1")) {
                continue;
            }
            if(replace.contains("theta2") && !pattern.contains("theta2")) {
                continue;
            }
            if(replace.contains("theta3") && !pattern.contains("theta3")) {
                continue;
            }
            if(replace.contains("theta4") && !pattern.contains("theta4")) {
                continue;
            }

            Set<String> replaceQubits = replaceDag.getQubits();
            Set<String> patternQubits = patternDag.getQubits();
            if(!patternQubits.containsAll(replaceQubits)) {
                continue;
            }

            validRules.add(rule);

            // if (StringUtils.countMatches(splitRule[0], ";") == StringUtils.countMatches(splitRule[1], ";")) {
            //     if (GraphTests.isConnected(lhs.getDAG())) {
            //         validRules.add(splitRule[1] + " | " + splitRule[0]);
            //     }
            // }
        }

        return validRules;
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

            if(!GraphTests.isConnected(lhsDagBeforeSymb.getDAG())) {
                continue;
            }

            if(!GraphTests.isConnected(lhsDagAfterSymb.getDAG())) {
                continue;
            }
            
            validRules.add(rule);
        }
        return validRules;
    }


    public int scoreRule(String rule, int numAppliedBest, int numApplied) {
        return numApplied + (2 * numAppliedBest);
    }



    public void optimize_BEAM_normal(CircuitDAG circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, List<MononialRule> symbMonomialRules, int beam_width, int rule_limit, int symb_rule_limit, int min_symb_size, int max_symb_size, int timeout, boolean useSymb) {
        CircuitDAG bestCircuit = circuit;
        Set<Integer> seen = new HashSet<>();
        List<String> validRules = filterValidRules(rules);
        List<MononialRule> validMonomialRules = filterValidMonomialRules(symbMonomialRules);
        List<MatrixConstrainedRule> validMatrixRules = filterValidMatrixRules(symbRules);
        seen.add(circuit.hashCode());
        
        System.out.println("Original Size: " + circuit.totalGateCount());
        System.out.println("Original 2q: " + circuit.twoQGateCount());
        System.out.println("Rule size: " + validRules.size());
        System.out.println("Symb rule limit: " + symb_rule_limit);
        System.out.println("Min symb size: " + min_symb_size);
        System.out.println("Max symb size: " + max_symb_size);
        System.out.println("Timeout: " + timeout);
        System.out.println("Use symb: " + useSymb);
        //System.out.println("Candidate:" + circuit.toQASM());
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
                System.out.println("Current Best: " + c.toQASM());
                System.out.println("Current Best Cost: " + c.cost(Params.OPTIMIZATION_OBJECTIVE));
                System.out.println("Current Best Size: " + c.totalGateCount());
                //System.out.println("Current Best Rules Applied: " + c.getRulesApplied());
                //System.out.println("Current Rules Used: " + rulesUsed);
                //System.out.println("Current Rules Applied: " + rulesUsed.size());
            }

            c = dequeue(q, Params.TEMPERATURE, Params.OPTIMIZATION_OBJECTIVE, rand);
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
            
            //System.out.println("Rules TO USE: " + rulesToUse.size());
            boolean concreteruleApplied = false;
            for(String rule: rulesToUse) {
                String[] splitRule = rule.split(" \\| ");
                //System.out.println("Rule: " + rule);
                CircuitDAG candidate = applyRule(c, QASMToDAGVisitor.parse(splitRule[1]), splitRule[0], false, rand);
                if(candidate != c) {
                    //System.out.println("Apply Rule: " + rule);
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
                // List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);
                // List<MononialRule> copysymbMonomial = new ArrayList<>(validMonomialRules);
                // List<MatrixConstrainedRule> symbRulesToUse = new ArrayList<>();
                // List<MononialRule> symbMonomialRulesToUse = new ArrayList<>();
                // for(int i = 0; i < symb_rule_limit; i++) {
                //     CircuitDAG bestCircuitCopy = new CircuitDAG(bestCircuit);
                //     Map<String, Integer> symbRulesUsedCopy = new HashMap<>(symbRulesUsed);
                //     Map<String, Integer> symbMonomialRulesUsedCopy = new HashMap<>(symbMonomialRulesUsed);
                //     List<Integer> weights = copysymb.stream().map(r -> scoreRule(r.getLHS() + "|" + r.getRHS(), bestCircuitCopy.countRulesApplied(r.getLHS() + "|" + r.getRHS()), symbRulesUsedCopy.getOrDefault(r.getLHS() + "|" + r.getRHS(), 0))).collect(Collectors.toList());
                //     List<Integer> weightsMonomial = copysymbMonomial.stream().map(r -> scoreRule(r.getRhs() + "|" + r.getLhs(), bestCircuitCopy.countRulesApplied(r.getRhs() + "|" + r.getLhs()), symbMonomialRulesUsedCopy.getOrDefault(r.getRhs() + "|" + r.getLhs(), 0))).collect(Collectors.toList());
                //     if(Params.PRUNE_TEMPERATURE == 0) {
                //         // int maxWeight = weights.stream().max(Integer::compare).get();
                //         // int maxWeightMonomial = weightsMonomial.stream().max(Integer::compare).get();
                //         // symbRulesToUse.add(copysymb.get(weights.indexOf(maxWeight)));
                //         // symbMonomialRulesToUse.add(copysymbMonomial.get(weightsMonomial.indexOf(maxWeightMonomial)));
                //         // copysymb.remove(weights.indexOf(maxWeight));
                //         // copysymbMonomial.remove(weightsMonomial.indexOf(maxWeightMonomial));
                //         int index = rand.nextInt(copysymb.size());
                //         int indexMonomial = rand.nextInt(copysymbMonomial.size());
                //         symbRulesToUse.add(copysymb.get(index));
                //         symbMonomialRulesToUse.add(copysymbMonomial.get(indexMonomial));
                //         copysymb.remove(index);
                //         copysymbMonomial.remove(indexMonomial);
                //     } else {
                //         int index = sampleSoftMax(weights, Params.PRUNE_TEMPERATURE, rand);
                //         int indexMonomial = sampleSoftMax(weightsMonomial, Params.PRUNE_TEMPERATURE, rand);
                //         symbRulesToUse.add(copysymb.get(index));
                //         symbMonomialRulesToUse.add(copysymbMonomial.get(indexMonomial));
                //         copysymb.remove(index);
                //         copysymbMonomial.remove(indexMonomial);
                //     }
                // }

                // for (int i = 0; i < symbMonomialRulesToUse.size(); i++){
                //     //System.out.println("Current Monomial RULE: " + i + "/" + Integer.min(symb_rule_limit, symbMonomialRules.size()));
                //     int index = i;
                //     //System.out.println("Current SYMB MONOMIAL RULE: " + symbMonomialRulesToUse.get(index).getRhs() + " -> " + symbMonomialRulesToUse.get(index).getLhs());
                //     CircuitDAG optimizedDAG = symbolicMatchBeforeAfterMono(c, symbMonomialRulesToUse.get(index).getRhs(), symbMonomialRulesToUse.get(index).getLhs(), min_symb_size, max_symb_size, symbMonomialRulesToUse.get(index).getConstraints(), null);
                    
                //     if(optimizedDAG != null) {
                //         //System.out.println("Applyed Monomial Rule: " + symbMonomialRulesToUse.get(index).getRhs() + " -> " + symbMonomialRulesToUse.get(index).getLhs());
                //         List<String> rulesApplied = new ArrayList<>(c.getRulesApplied());
                //         rulesApplied.add(symbMonomialRulesToUse.get(index).getRhs() + "|" + symbMonomialRulesToUse.get(index).getLhs());
                //         optimizedDAG.setRulesApplied(rulesApplied);
                //         symbMonomialRulesUsed.put(symbMonomialRulesToUse.get(index).getRhs() + "|" + symbMonomialRulesToUse.get(index).getLhs(), symbMonomialRulesUsed.getOrDefault(symbMonomialRulesToUse.get(index), 0) + 1);
                //         //System.out.println("Optimized Using Monomial Rule: " + optimizedDAG.toQASM());
                //         if(optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) <= c.cost(Params.OPTIMIZATION_OBJECTIVE)) {
                //             symbRuleReductionsTotal += c.cost(Params.OPTIMIZATION_OBJECTIVE) - optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE);
                //             q.add(optimizedDAG);
                //         } else {
                        
                //             double acceptP = Math.min(1, Math.exp(-Params.TEMPERATURE * ((double) optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) / c.cost(Params.OPTIMIZATION_OBJECTIVE))));
                //             if (rand.nextDouble() <= acceptP) {
                //                 q.add(optimizedDAG);
                //             } else {
                //                 q.add(c);
                //             }
                
                //         }
                //     }
                // }

                
                // for (int i = 0; i < symbRulesToUse.size(); i++){
                //     //System.out.println("Current RULE: " + i + "/" + Integer.min(symb_rule_limit, symbRules.size()));
                //     int index = i;
                //     //System.out.println("Current SYMB RULE: " + symbRulesToUse.get(index).getLHS() + " -> " + symbRulesToUse.get(index).getRHS());
                   
                //     CircuitDAG optimizedDAG = symbolicMatchBeforeAfter(c, symbRulesToUse.get(index).getLHS(), symbRulesToUse.get(index).getRHS(), min_symb_size, max_symb_size, symbRulesToUse.get(index).getConstraint(), null);
                //     if(optimizedDAG != null) {
                //         List<String> rulesApplied = new ArrayList<>(c.getRulesApplied());
                //         rulesApplied.add(symbRulesToUse.get(index).getLHS() + "|" + symbRulesToUse.get(index).getRHS());
                //         optimizedDAG.setRulesApplied(rulesApplied);
                //         symbRulesUsed.put(symbRulesToUse.get(index).getLHS() + "|" + symbRulesToUse.get(index).getRHS(), symbRulesUsed.getOrDefault(symbRulesToUse.get(index), 0) + 1);
                //         //System.out.println("Optimized DAG: " + optimizedDAG.toQASM());
                       
                //         if(optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) <= c.cost(Params.OPTIMIZATION_OBJECTIVE)) {
                //             //System.out.println("Symb Rule Reduced: " + (c.cost(Params.OPTIMIZATION_OBJECTIVE) - optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE)));
                //             //System.out.println("From " + c.cost(Params.OPTIMIZATION_OBJECTIVE) + " to " + optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE));
                //             symbRuleReductionsTotal += c.cost(Params.OPTIMIZATION_OBJECTIVE) - optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE);
                //             q.add(optimizedDAG);
                //         } else {
                //             double acceptP = Math.min(1, Math.exp(-Params.TEMPERATURE * ((double) optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) / c.cost(Params.OPTIMIZATION_OBJECTIVE))));
                //             if (rand.nextDouble() <= acceptP) {
                //                 q.add(optimizedDAG);
                //             } else {
                //                 q.add(c);
                //             }
                //         }
                //     }
                // } 
                if(symbolicThread == null) {
                    symbolicThread = new SymbolicThread(c, validMatrixRules, validMonomialRules, min_symb_size, max_symb_size, rand, this);
                    symbolicThread.start();
                } else {
                    if(!symbolicThread.isAlive()) {
                        CircuitDAG result = symbolicThread.getResult();
                        if(result != null) {
                            q.add(result);
                        }
                        symbolicThread = new SymbolicThread(c, validMatrixRules, validMonomialRules, min_symb_size, max_symb_size, rand, this);
                        symbolicThread.start();
                    }
                }
            }
        }

        System.out.println("Final Gate Size: " + bestCircuit.totalGateCount());
        System.out.println("Final 2q: " + bestCircuit.twoQGateCount());
        System.out.println("Final Cost: " + bestCircuit.cost(Params.OPTIMIZATION_OBJECTIVE));
    }

    public void optimize_SA(EggGen.ConstrainedCircuit circuit, List<String> rules, List<String> longrules, List<MatrixConstrainedRule> symbRules, List<MononialRule> symbMonomialRules, int rule_limit, int symb_rule_limit, int min_symb_size, int max_symb_size, int timeout, boolean useSymb, List<String> commutative) {
        EggGen egraph = new EggGen();
        List<Rule> parsedRules = new ArrayList<>();
        for(String rule: rules) {
            System.err.println("Parsing rule: " + rule);
            Rule parsedRule = QASMAstBuilder.parseRule(rule);
            System.err.println("Parsed rule: " + parsedRule.toString());
            parsedRules.add(parsedRule);
        }
        for(String rule: commutative) {
            egraph.addRewrite(rule);
        }
        List<Rule> sizeDecreasingRules = new ArrayList<>();
        List<Rule> sizePreservingRules = new ArrayList<>();
        List<Rule> sizeIncreasingRules = new ArrayList<>();
        for(Rule rule: parsedRules) {
            int lhssize = rule.lhs.gates.size();
            int rhssize = rule.rhs.gates.size();
            if(lhssize > rhssize) {
                sizeDecreasingRules.add(rule);

                EggGen.Circuit lhs = rule.lhs;
                Set<String> lhsqubitsVars = new HashSet<>();
                lhs.getQubitVars(lhsqubitsVars);
                Set<String> rhsqubitsVars = new HashSet<>();
                EggGen.Circuit rhs = rule.rhs;
                rhs.getQubitVars(rhsqubitsVars);
                if(rhsqubitsVars.containsAll(lhsqubitsVars)) {
                    sizeIncreasingRules.add(new Rule(rule.rhs, rule.lhs, rule.conditions));
                }
            } else if(lhssize == rhssize) {
                sizePreservingRules.add(rule);
            } else if(lhssize < rhssize) {
                EggGen.Circuit lhs = rule.lhs;
                Set<String> lhsqubitsVars = new HashSet<>();
                lhs.getQubitVars(lhsqubitsVars);
                Set<String> rhsqubitsVars = new HashSet<>();
                EggGen.Circuit rhs = rule.rhs;
                rhs.getQubitVars(rhsqubitsVars);
                if(rhsqubitsVars.containsAll(lhsqubitsVars)) {
                    sizeDecreasingRules.add(new Rule(rule.rhs, rule.lhs, rule.conditions));
                }
                sizeIncreasingRules.add(rule);
            }
        }
        System.out.println("Size increasing rules: " + sizeIncreasingRules.size());
        System.out.println("Size preserving rules: " + sizePreservingRules.size());
        System.out.println("Size decreasing rules: " + sizeDecreasingRules.size());
        List<String> validLongRules = filterValidRules(longrules);
        try {
        FileWriter fileWriter = new FileWriter("validLongRules.txt");
        for(String rule: validLongRules) {
                fileWriter.write(rule + "\n");
        }
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<MononialRule> validMonomialRules = filterValidMonomialRules(symbMonomialRules);
        List<MatrixConstrainedRule> validMatrixRules = filterValidMatrixRules(symbRules);
        //System.out.println("circuit: " + circuit.circuit.toQASM());
        int sizeIncreasingRuleslimit = 17;
        int sizePreservingRuleslimit = 26;
        int sizeDecreasingRuleslimit = 21;
        int egg_rule_limit = Integer.min(rules.size(), 25);
        
       //EggGen.Circuit instantiatedCircuit = circuit.circuit.instantiate(new HashMap<>());
       //EggGen.ConstrainedCircuit instantiatedCircuitC = new EggGen.ConstrainedCircuit(instantiatedCircuit, new EggGen.Permutation(new ArrayList<>()));
        System.out.println("Starting SA optimization..., timeout: " + timeout);
        System.out.println("Original Size:" + circuit.circuit.gates.size());
        System.out.println("Original 2q:" + circuit.circuit.getTwoQubitsCount());
        System.out.println("Egraph rule size limit: " + rule_limit);
        System.out.println("Symb rule size limit: " + symb_rule_limit);
        System.out.println("Min symb size: " + min_symb_size);
        System.out.println("Max symb size: " + max_symb_size);
        System.out.println("candidate circuit: " + circuit.circuit.toQASM());
        EggGen.Circuit instantiatedCircuit = circuit.circuit.instantiate(new HashMap<>());
        CircuitDAG bestOptimized = QASMToDAGVisitor.parse(instantiatedCircuit.toQASM());

        Random random = new Random(30);
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
        q.add(optimized);
        SymbolicThread symbolicThread = null;
        while(!q.isEmpty()) {
            egraph.push();
            egraph.clearRules();
            if(circuitComparator.compare(q.peek(), bestOptimized) <= 0) {
                bestOptimized = q.peek();
                System.out.println("Best Optimized Size:" + bestOptimized.totalGateCount());
                System.out.println("Best Optimized 2q:" + bestOptimized.twoQGateCount());
            }

            CircuitDAG c = dequeue(q, Params.TEMPERATURE, Params.OPTIMIZATION_OBJECTIVE, random);
            //System.out.println("Current Circuit: " + c.toQASM());
            if(q.size() > Params.QUEUE_SIZE+2) {
                PriorityQueue<CircuitDAG> newQ = new PriorityQueue<>(new CircuitComparator(Params.OPTIMIZATION_OBJECTIVE));
                while(newQ.size() != Params.QUEUE_SIZE) {
                    newQ.add(q.poll());
                }
                q = newQ;
            }
            // choose egraph_rule_limit different rules from rules
            List<String> rulesToUse = new ArrayList<>();
            
            if(Params.PRUNE_TEMPERATURE == 0) {
                while(rulesToUse.size() < Integer.min(rule_limit, validLongRules.size())) {
                    int index = random.nextInt(validLongRules.size());
                    String rule = validLongRules.get(index);
                    if(!rulesToUse.contains(rule)) {
                        rulesToUse.add(rule);
                    }
                }
            } else {
                CircuitDAG bestOptimizedCopy = new CircuitDAG(bestOptimized);
                Map<String, Integer> rewriteRulesApplied = new HashMap<>(rewriteRulesUsed);
                List<Integer> weights = validLongRules.stream().map(r -> scoreRule(r, bestOptimizedCopy.countRulesApplied(r), rewriteRulesApplied.getOrDefault(r, 0))).collect(Collectors.toList());
                while(rulesToUse.size() < Integer.min(rule_limit, validLongRules.size())) {
                    int index = sampleSoftMax(weights, Params.TEMPERATURE, random);
                    String rule = validLongRules.get(index);
                    if(!rulesToUse.contains(rule)) {
                        rulesToUse.add(rule);
                    }
                }
            }
            
            
            String name = egraph.addCircuit(QASMAstBuilder.parse(c.toQASM()));
            for(String rule: rulesToUse) {
                String[] splitRule = rule.split(" \\| ");
                EggGen.Circuit lhs = QASMAstBuilder.parse(splitRule[0]);
                EggGen.Circuit rhs = QASMAstBuilder.parse(splitRule[1]);
                Map<String, String> qubitMap = new HashMap<>();
                String eggrule2 = String.format("%s|%s|rewrite", EggGen.circuitToGeneralizedOnlyRemoveQ(rhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(lhs,"c"));
                //first derive cross wire commutations
                //List<String> copySizePreservingRules = new ArrayList<>(sizePreservingRules);
                //System.out.println("Adding size preserving rule: " + copyRules.size());
                List<Integer> addedRules = new ArrayList<>();
                while(addedRules.size() < sizePreservingRuleslimit) {
                    int index = random.nextInt(sizePreservingRules.size());
                    Rule r = sizePreservingRules.get(index);
                    if(!addedRules.contains(index)) {
                        addedRules.add(index);
                        List<Rule.Equality> equalities = r.getEqualities();
                        String egg_rule = String.format("(%s %s %s %s :ruleset %s)", "birewrite", EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt1");
                        egraph.addRewrite(egg_rule);
                    }
                }
                

                addedRules = new ArrayList<>();
                while(addedRules.size() < sizeIncreasingRuleslimit) {
                    //System.out.println("Adding size increasing rule: " + copyRules.size());
                    int index = random.nextInt(sizeIncreasingRules.size());
                    Rule r = sizeIncreasingRules.get(index);
                    if(!addedRules.contains(index)) {
                        addedRules.add(index);
                        List<Rule.Equality> equalities = r.getEqualities();
                        String egg_rule = String.format("(%s %s %s %s :ruleset %s)", "rewrite", EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt1");
                        egraph.addRewrite(egg_rule);
                    }
                }

                addedRules = new ArrayList<>();
                while(addedRules.size() < sizeDecreasingRuleslimit) {
                    //System.out.println("Adding size increasing rule: " + copyRules.size());
                    int index = random.nextInt(sizeDecreasingRules.size());
                    Rule r = sizeDecreasingRules.get(index);
                    if(!addedRules.contains(index)) {
                        addedRules.add(index);
                        List<Rule.Equality> equalities = r.getEqualities();
                        String egg_rule = String.format("(%s %s %s %s :ruleset %s)", "rewrite", EggGen.circuitToGeneralizedOnlyRemoveQ(r.lhs, "c"), EggGen.circuitToGeneralizedOnlyRemoveQ(r.rhs, "c"), ":when (" + equalities.stream().map(e -> String.format("(%s %s %s)",  e.isEqual ? "=" : "!=", e.qubit1, e.qubit2)).collect(Collectors.joining(" ")) + ")", "opt1");
                        egraph.addRewrite(egg_rule);
                    }
                }

                //egraph.addRewritev2(eggrule2, "wire");
                // egraph.runN("wire", 10);
                // egraph.runN("opt2", 15);
                //egraph.runBackoff("wire", 15);
                EggGen.Circuit candidate = null;
                int i = 0;
                //egraph.runN("wire", 1);
                while(i < 9) {
                    i++;
                    egraph.runSaturation("const");
                
                    egraph.runN("opt1", 1);
                    //graph.runN("wire", 1);
                    //egraph.runN("opt2", 1);
                    candidate = egraph.extractCircuit(name);
                    CircuitDAG candidateDAG = QASMToDAGVisitor.parse(candidate.toQASM());
                    //System.out.println("Candidate: " + candidateDAG.toQASM());
                    //System.out.println("Candidate EGG String: " + candidate.toEggString());
                    if(!candidateDAG.toQASM().equals(c.toQASM())) {
                        q.add(candidateDAG);
                        break;
                    }
                }
               
                CircuitDAG candidateDAG = QASMToDAGVisitor.parse(candidate.toQASM());
                System.out.println("Candidate Total Gates: " + candidate.gates.size());
                System.out.println("Candidate 2q Gates: " + candidate.getTwoQubitsCount());
                if(circuitComparator.compare(candidateDAG, c) <= 0) {
                    q.add(candidateDAG);
                } else {
                    double acceptP = Math.min(1, Math.exp(-Params.TEMPERATURE * ((double) candidateDAG.cost(Params.OPTIMIZATION_OBJECTIVE) / optimized.cost(Params.OPTIMIZATION_OBJECTIVE))));
                    if(random.nextDouble() <= acceptP) {
                        System.out.println("Accept");
                        System.out.println("From " + optimized.cost(Params.OPTIMIZATION_OBJECTIVE) + " to " + candidateDAG.cost(Params.OPTIMIZATION_OBJECTIVE));
                        q.add(candidateDAG);
                        rewriteRulesUsed.put(rule, rewriteRulesUsed.getOrDefault(rule, 0) + 1);
                    } else {
                        q.add(c);
                    }
                }
                
            }
            if(useSymb) {
                c = dequeue(q, Params.TEMPERATURE, Params.OPTIMIZATION_OBJECTIVE, random);
                System.out.println("Using symbolic rules");
                if(symbolicThread == null) {
                    symbolicThread = new SymbolicThread(c, validMatrixRules, validMonomialRules, min_symb_size, max_symb_size, random, this);
                    symbolicThread.start();
                } else {
                    if(!symbolicThread.isAlive()) {
                        CircuitDAG result = symbolicThread.getResult();
                        if(result != null) {
                            q.add(result);
                        }
                        symbolicThread = new SymbolicThread(c, validMatrixRules, validMonomialRules, min_symb_size, max_symb_size, random, this);
                        symbolicThread.start();
                    }
                }
            }

            egraph.pop();

            // if(circuitComparator.compare(optimized, bestOptimized) <= 0) {
            //     bestOptimized = optimized;
            //     System.out.println("Best Optimized Size:" + bestOptimized.totalGateCount());
            //     System.out.println("Best Optimized 2q:" + bestOptimized.twoQGateCount());
            // }
            
          
            //System.out.println("Egraph Rule Reduction 2q:" + egraphRuleReduction2q);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            if(duration / 1000000000 > timeout) {
                break;
            }
        }
        System.out.println("Final Gate Size:" + bestOptimized.cost(CircuitDAG.OptObj.TOTAL));
        System.out.println("Final 2q:" + bestOptimized.cost(CircuitDAG.OptObj.TWO_Q));
        System.out.println("Symb Rule Obj Reductions Total:" + symbRuleReductionsTotal);
        System.out.println("Egraph Rule Obj Reductions Total:" + egraphRuleReductionsTotal);
    }
    
    // public void optimize(EggGen.ConstrainedCircuit circuit, List<String> rules, List<MatrixConstrainedRule> symbRules, int egraph_rule_limit, int symb_rule_limit, int timeout) {
    //     EggGen egraph = new EggGen();
    //     System.out.println("Original Size:" + circuit.circuit.gates.size());
    //     System.out.println("Original 2q:" + circuit.circuit.getTwoQubitsCount());
    //     EggGen.ConstrainedCircuit optimized = circuit;
    //     Random random = new Random();
    //     //We need to preprocess the symb rules to (rule .....).
    //     int j = 0;
    //     long startTime = System.nanoTime();
    //     while(true) {
    //         System.out.println("CURRENT iteration:" + j);
    //         egraph.push();
    //         String name = egraph.addConstrainedCircuit(optimized);
    //         // choose egraph_rule_limit different rules from rules
    //         List<String> copy = new ArrayList<>(rules);
    //         for(int i = 0; i < Integer.min(copy.size(), egraph_rule_limit); i++) {
    //             int index = random.nextInt(copy.size());
    //             egraph.addRewritev2(copy.get(index));
    //             copy.remove(index);
    //         }

    //         egraph.runN("opt", 15);
            
    //         // do ematching for symbolic rules
    //         List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);
    //         for (int i = 0; i < Integer.min(copysymb.size(), symb_rule_limit); i++){
    //             System.out.println("Current RULE: " + i + "/" + Integer.min(symb_rule_limit, symbRules.size()));
    //             int index = random.nextInt(copysymb.size());
    //             // int index = i;
    //             List<Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit>> matches = egraph.ematching(copysymb.get(index).getLHS(), copysymb.get(index).getRHS(), 500);
    //             System.out.println("Match Sizes: " + matches.size());
    //             int k = 0;
    //             for(Triple<EggGen.Circuit, EggGen.Circuit, EggGen.Circuit> match : matches) {
    //                 //now, we need to check that s satisfy the constraints
    //                 System.out.println("Current Match: " + k + "/" + matches.size());
    //                 k++;
    //                 EggGen.Circuit matchedLhs = match.getMiddle();
    //                 EggGen.Circuit matchedRhs = match.getRight();
    //                 EggGen.Circuit s = match.getLeft();

    //                 System.out.println("Match: s: " + match.getLeft().toEggString() +  "\nlhs:" + match.getMiddle().toEggString() + "\nrhs:" + match.getRight().toEggString());
    //                 if(matchedLhs.toEggString().equals(matchedRhs.toEggString()) && s.gates.isEmpty()) {
    //                     continue;
    //                 }
                    
    //                 String lhs = copysymb.get(index).getLHS();
    //                 Pattern pattern = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+c\\)");
    //                 Matcher matcher = pattern.matcher(lhs);
    //                 String removedSymb = null;
    //                 if(matcher.find()) {
    //                     String matched = matcher.group();
    //                     removedSymb = lhs.replace(matched, "(Nil)");
    //                 }

    //                 // Map<String, String> qubitMap = null;
    //                 // if(removedSymb != null) {
    //                 //     removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
    //                 //     removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
    //                 //     System.out.println("replaced symb rule:" + removedSymb);
    //                 //     EggGen.Circuit symblhs = EggAstBuilder.parseCircuit(removedSymb);
    //                 //     qubitMap = buildQubitMap(matchedLhs, symblhs);
    //                 // } else {
    //                 //     Pattern pattern2 = Pattern.compile("\\(Cons \\(SYMB \\d+\\)\\s+(.*)\\)");
    //                 //     Matcher matcher2 = pattern2.matcher(lhs);
    //                 //     if (matcher2.find()) {
    //                 //         removedSymb = matcher2.group(1).trim();
    //                 //     }
    //                 //     removedSymb = removedSymb.replaceAll("\\bc\\b", "(Nil)");
    //                 //     removedSymb = removedSymb.replaceAll("q\\d+", "(Q \"$0\")");
    //                 //     removedSymb = removedSymb.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
    //                 //     System.out.println("replaced symb rule:" + removedSymb);
    //                 //     List<EggGen.Gate> concretelhsgates = new ArrayList<>(matchedLhs.gates.subList(match.getLeft().gates.size(), matchedLhs.gates.size()));
    //                 //     EggGen.Circuit concretecircuit = new EggGen.Circuit(concretelhsgates);
    //                 //     System.out.println("replaced concrete lhs:" + concretecircuit.toEggString());
    //                 //     EggGen.Circuit symbpattern = EggAstBuilder.parseCircuit(removedSymb);
    //                 //     qubitMap = buildQubitMap(concretecircuit, symbpattern);
    //                 // }
                    
    //                 //EggGen.Circuit canonicalized = EggGen.canonicalizeCircuit(s, qubitMap);
    //                 //System.out.println("Maxqubits:" + (canonicalized.getMaxQubits() + 1));
                    
    //                 //System.out.println("Canonicaled:" + canonicalized.toEggString());
                    
    //                     ConstrainedCircuit cc1 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedLhs, new EggGen.Permutation(new ArrayList<>())), matchedLhs.getMaxQubits()+1);
    //                     ConstrainedCircuit cc2 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedRhs, new EggGen.Permutation(new ArrayList<>())), matchedRhs.getMaxQubits()+1);
    //                     try {
    //                         boolean equivalent = checkEquivalenceWithQiskit(cc1.getCircuit().getQasmString(), cc2.getCircuit().getQasmString(), matchedLhs.getMaxQubits()+1);
    //                         if(equivalent) {
    //                             egraph.sendCommand(String.format("(union %s %s)", matchedLhs.toEggString(), matchedRhs.toEggString()));
    //                         }
    //                         System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
    //                     } catch (IOException | InterruptedException e) {
    //                         System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
    //                     }
    //                     // if(checkLinearCombination(canonicalized, copysymb.get(index).getConstraint(), EnumeratorPrune.MAX_QUBITS_SYMB))  {
    //                     //     System.out.println("S satisfy the constraints!");
    //                     //     //substitube symb with matched s
    //                     //     System.out.println("Union:\nLHS:" + matchedLhs.toEggString() + "'\nRHS:" + matchedRhs.toEggString());
    //                     //     ConstrainedCircuit cc1 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedLhs, new EggGen.Permutation(new ArrayList<>())), matchedLhs.getMaxQubits()+1);
    //                     //     ConstrainedCircuit cc2 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedRhs, new EggGen.Permutation(new ArrayList<>())), matchedRhs.getMaxQubits()+1);
    //                     //     try {
    //                     //         boolean equivalent = checkEquivalenceWithQiskit(cc1.getCircuit().getQasmString(), cc2.getCircuit().getQasmString(), matchedLhs.getMaxQubits()+1);
    //                     //         System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
    //                     //     } catch (IOException | InterruptedException e) {
    //                     //         System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
    //                     //     }
    //                     //     egraph.sendCommand(String.format("(union %s %s)", matchedLhs.toEggString(), matchedRhs.toEggString()));
    //                     // } else {
    //                     //     // they are not equal, comfirme it with check equal
    //                     //     ConstrainedCircuit cc1 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedLhs, new EggGen.Permutation(new ArrayList<>())), matchedLhs.getMaxQubits()+1);
    //                     //     ConstrainedCircuit cc2 = CircuitTranslator.translateBack(new EggGen.ConstrainedCircuit(matchedRhs, new EggGen.Permutation(new ArrayList<>())), matchedRhs.getMaxQubits()+1);
    //                     //     try {
    //                     //         boolean equivalent = checkEquivalenceWithQiskit(cc1.getCircuit().getQasmString(), cc2.getCircuit().getQasmString(), matchedLhs.getMaxQubits()+1);
    //                     //         System.out.println("Circuits are equivalent (Qiskit): " + equivalent);
    //                     //     } catch (IOException | InterruptedException e) {
    //                     //         System.err.println("Error during Qiskit equivalence check: " + e.getMessage());
    //                     //     }
    //                     // }
                   
                    
    //             }
    //             copysymb.remove(index);
    //         }


    //         egraph.runN("opt", 5);

    //         optimized = egraph.extract(name);
    //         System.out.println("Current Gate Size:" + optimized.circuit.gates.size());
    //         System.out.println("Current 2q:" + optimized.circuit.getTwoQubitsCount());
    //         egraph.rules.clear();
    //         egraph.optrules.clear();
    //         egraph.pop();
    //         j++;

    //         Map<String,Long> data = egraph.getProfilingData();
    //         System.out.print("--------------------------Iteration Egraph Break Down-----------------\n");
    //         System.out.println("ematchingSaturationTime" + data.get("ematchingSaturationTime") / 1000000);
    //         System.out.println("ematchingPrefixTime" + data.get("ematchingPrefixTime") / 1000000);
    //         System.out.println("ematchingSuffixTime:" + data.get("ematchingSuffixTime") / 1000000);
    //         long endTime = System.nanoTime();
    //         long duration = endTime - startTime;
    //         if(duration / 1000000000 > timeout) {
    //             break;   
    //         }
    //     }
    //     System.out.println("Final Gate Size:" + optimized.circuit.gates.size());
    //     System.out.println("Final 2q:" + optimized.circuit.getTwoQubitsCount());
    //     Map<String,Long> data = egraph.getProfilingData();
    //     System.out.print("--------------------------Egraph Break Down-----------------\n");
    //     System.out.println("ematchingSaturationTime" + data.get("ematchingSaturationTime") / 1000000);
    //     System.out.println("ematchingPrefixTime" + data.get("ematchingPrefixTime") / 1000000);
    //     System.out.println("ematchingSuffixTime:" + data.get("ematchingSuffixTime") / 1000000);
    // }

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
        
        //System.out.println("Json Matrix" + jsonM);

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
        System.out.println(benchmarkFile);
        String rulesFile = cmd.getOptionValue("rule");
        String longrulesFile = cmd.getOptionValue("longrule");
        String symrulesFile = cmd.getOptionValue("symbrule");
        String symrulesMonomialFile = cmd.getOptionValue("monomial");

        String modeStr = cmd.getOptionValue("mode");
        int timeoutint = Integer.valueOf(cmd.getOptionValue("timeout"));
        boolean useSymb = Boolean.valueOf(cmd.getOptionValue("usesymb"));
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
                            //System.out.println("content: " + content);
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
        //System.out.println(circuitString);
        

        EggGen.Circuit circuit = QASMAstBuilder.parse(circuitString);
        //System.out.println(circuit.toEggString());

        // Use timeoutint to limit the time to run optimize; when time is up, terminate the program

        // Assume timeoutint is defined somewhere above as the time limit in seconds
       
        Optimizer optimizer = new Optimizer();
        new Thread(() -> {
            try {
                Thread.sleep(timeoutint * 1000);
                System.exit(0);
            } catch (InterruptedException ignored) {}
        }).start();
        //optimizer.optimize(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), egraph);
        if(modeStr.equals("SA")) {
            int minSymb = cmd.getOptionValue("minSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("minSymbSize")) : 10;
            int maxSymb = cmd.getOptionValue("maxSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("maxSymbSize")) : 30;
            optimizer.optimize_SA(new EggGen.ConstrainedCircuit(circuit, new EggGen.Permutation(new ArrayList<>())), rules, longrules, symbRules, symbRulesMonomials, 1, 1, minSymb, maxSymb, timeoutint, useSymb, commutative);
        } else if(modeStr.equals("BEAMN")) {
            int minSymb = cmd.getOptionValue("minSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("minSymbSize")) : 10;
            int maxSymb = cmd.getOptionValue("maxSymbSize") != null ? Integer.valueOf(cmd.getOptionValue("maxSymbSize")) : 20;
            Comparator<EggGen.ConstrainedCircuit> comparator = new Comparator<EggGen.ConstrainedCircuit>() {
                public int compare(EggGen.ConstrainedCircuit a, EggGen.ConstrainedCircuit b) {
                    return a.circuit.getTwoQubitsCount() - b.circuit.getTwoQubitsCount();
                }
            };
            EggGen.Circuit instantiatedCircuit = circuit.instantiate(new HashMap<>());
            optimizer.optimize_BEAM_normal(QASMToDAGVisitor.parse(instantiatedCircuit.toQASM()), rules, symbRules, symbRulesMonomials, 10, 1, 1, minSymb, maxSymb, timeoutint, useSymb);
        }
    }
}

}