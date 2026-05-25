


import org.jgrapht.graph.DirectedMultigraph;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import org.jgrapht.Graphs;

import java.util.Map;
import java.util.HashMap;

import org.jgrapht.traverse.TopologicalOrderIterator;

import ast.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;

import org.checkerframework.checker.units.qual.s;
public class CircuitDAG {
    private DirectedMultigraph<Node, Edge> dag;
    private List<String> rulesApplied;

    @Getter
    private Set<String> qubits;

    public CircuitDAG() {
        this.dag = new DirectedMultigraph<>(Edge.class);
        this.qubits = new HashSet<>();
        this.rulesApplied = new ArrayList<>();
    }


    public CircuitDAG(CircuitDAG other) {
        this.dag = (DirectedMultigraph<Node, Edge>) other.getDag().clone();
        this.qubits = new HashSet<>(other.getQubits());
        this.rulesApplied = new ArrayList<>(other.getRulesApplied());
    }

    public List<String> getRulesApplied() {
        return rulesApplied;
    }

    public void addRuleApplied(String rule) {
        rulesApplied.add(rule);
    }

    public void setRulesApplied(List<String> rulesApplied) {
        this.rulesApplied = rulesApplied;
    }

    public int countRulesApplied(String rule) {
        return (int) rulesApplied.stream().filter(r -> r.equals(rule)).count();
    }

    public DirectedMultigraph<Node, Edge> getDAG() {
        return dag;
    }

    public void addVertex(Node node) {
        if(node.isGate()) {
            for(int i = 0; i < node.getQubits().size(); i++) {
                String qubit = node.getQubits().get(i);
                qubits.add(qubit);
            }
        }
        dag.addVertex(node);
    }

    public void addEdge(Node source, Node target, Edge edge) {
        dag.addEdge(source, target, edge);
    }


    public Edge getEdge(Node source, Node target, String qubit) {
        Edge.Label sourceLabel = Edge.Label.NONE;
        Edge.Label targetLabel = Edge.Label.NONE;
        if (source.isCX()) {
            if (qubit.equals(source.getQubits().get(0))) {
                sourceLabel = Edge.Label.CONTROL;
            } else {
                sourceLabel = Edge.Label.TARGET;
            }
        } else if (source.isCCZ()) {
            if (qubit.equals(source.getQubits().get(0))) {
                sourceLabel = Edge.Label.CONTROL;
            } else if (qubit.equals(source.getQubits().get(1))) {
                sourceLabel = Edge.Label.CONTROL2;
            } else {
                sourceLabel = Edge.Label.TARGET;
            }
        }
        if (target.isCX()) {
            if (qubit.equals(target.getQubits().get(0))) {
                targetLabel = Edge.Label.CONTROL;
            } else {
                targetLabel = Edge.Label.TARGET;
            }
        } else if (target.isCCZ()) {
            if (qubit.equals(target.getQubits().get(0))) {
                targetLabel = Edge.Label.CONTROL;
            } else if (qubit.equals(target.getQubits().get(1))) {
                targetLabel = Edge.Label.CONTROL2;
            } else {
                targetLabel = Edge.Label.TARGET;
            }
        }

        return new Edge(sourceLabel, targetLabel, qubit);
    }

    public List<Node> getCircuitRoots() {
        List<Node> roots = new ArrayList<>();
        for (Node n : dag.vertexSet()) {
            if (n.isSourceQubit()) {
               roots.addAll(Graphs.successorListOf(dag, n));
            }
        }
        return roots;
    }

    public Set<Node> nodes() {
        return dag.vertexSet();
    }


    public DirectedMultigraph<Node, Edge> getDag() {
        return dag;
    }


    public List<List<Node>> topoSort() {
        //        dagToQasm(circuit); // useful for sanity check when debugging
        List<List<Node>> layers = new ArrayList<>();
        Set<Node> added = new HashSet<>();
        Set<Node> vertices = new HashSet<>(dag.vertexSet());

        while (added.size() != dag.vertexSet().size()) {
            List<Node> verticesInLayer = new ArrayList<>();
            for (Node n : vertices) {
                List<Node> preds = Graphs.predecessorListOf(dag, n);
                if (added.containsAll(preds)) {
                    verticesInLayer.add(n);
                }
            }
            vertices.removeAll(verticesInLayer);
            layers.add(verticesInLayer);
            added.addAll(verticesInLayer);
        }

        return layers;
    }





    public Map<String, Node> rootsMap() {
        Map<String, Node> roots = new HashMap<>();
        for (Node n : dag.vertexSet()) {
            if (n.isSourceQubit()) {
                var succs = Graphs.successorListOf(dag, n);
                if (succs.size() > 1) {
                    throw new RuntimeException("source node has more than one successor");
                }
                roots.put(n.getId(), succs.get(0));
            }
        }
        return roots;
    }

    public Map<String, Node> leavesMap() {
        Map<String, Node> leaves = new HashMap<>();
        for (Node n : dag.vertexSet()) {
            if (n.isSinkQubit()) {
                var preds = Graphs.predecessorListOf(dag, n);
                if (preds.size() > 1) {
                    throw new RuntimeException("sink node has more than one predecessor");
                }
                leaves.put(n.getId(), preds.get(0));
            }
        }
        return leaves;
    }

    public String toQASM() {
        TopologicalOrderIterator<Node, Edge> dagIter = new TopologicalOrderIterator<>(dag);

        StringBuilder qasm = new StringBuilder();
        dagIter.forEachRemaining((x) -> {
            if (x.isGate() && (x.getAngles().isEmpty() || !allAnglesZero(x.getAngles()))) {
                qasm.append(x.toString().concat(";\n"));
            }
        });

        return qasm.toString();
    }

    public enum OptObj {
        TOTAL,
        TWO_Q,
        T,
        FT,
        TOTAL_IGNORE_RZ,
        FIDELITY
    }


    public int cost(OptObj optObj) {
        switch (optObj) {
            case TOTAL: {
                return totalGateCount();
            }
            case T: {
                return tGateCount();
            }
            case TWO_Q: {
                return twoQGateCount();
            }
            case TOTAL_IGNORE_RZ: {
                return totalGateCountIgnoreRz();
            }
            case FIDELITY: {
                return fidelity();
            }
            case FT: {
                return Params.FIDELITY_BREAKEVEN * tGateCount() + twoQGateCount();
            }
            default:
                throw new RuntimeException("Unsupported optObj: " + optObj);
        }
    }


    private static boolean isZeroAngleGate(Node n) {
        List<Expr> angles = n.getAngles();
        return angles != null && !angles.isEmpty() && allAnglesZero(angles);
    }

    public int totalGateCount() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.isGate() && !isZeroAngleGate(n)) {
                size++;
            }
        }
        return size;
    }

    public int twoQGateCount() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.is2QGate() && !isZeroAngleGate(n)) {
                size++;
            }
        }
        return size;
    }

    public int tGateCount() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.isTGate() && !isZeroAngleGate(n)) {
                size++;
            }
        }
        return size;
    }

    public int totalGateCountIgnoreRz() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (n.isGate() && !isZeroAngleGate(n)) {
                if (!n.getId().equals("rz") && !n.getId().equals("u1")) {
                    size++;
                }
            }
        }
        return size;
    }

    public int fidelity() {
        int size = 0;
        for (Node n : dag.vertexSet()) {
            if (isZeroAngleGate(n)) {
                continue;
            }
            if (n.is2QGate()) {
                size += Params.FIDELITY_BREAKEVEN;
                continue;
            }
            if (n.isGate() && !n.getId().equals("rz") && !n.getId().equals("u1")) {
                size++;
            }
        }
        return size;
    }

    public static boolean allAnglesZero(List<Expr> angles) {
        for (Expr angle : angles) {
            if (angle.toString().contains("theta")) {
                return false;
            }
            if (!isMult4PI(eval(angle))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMult4PI(double angle) {
        return angle % (4 * Math.PI) == 0;
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


    public static Real eval(Expr e, Map<String, Expr> angleMap) {
        switch (e) {
            case Real r:
                return r;
            case BinOp bo:
                return evalBinOp(bo, angleMap);
            case Symbol s: {
                if (s.getSymbol().equals("pi")) {
                    return new Real(Math.PI);
                } else {
                    return eval(angleMap.get(s.getSymbol()), angleMap);
                }
            }
            case UnOp uo:
                return evalUnOp(uo, angleMap);
            default:
                assert false;
                return null; // stupid hack to make the compiler happy ugh
        }
    }


    public static Expr substitute(Expr e, Map<String, Expr> angleMap) {
        switch (e) {
            case Real r:
                return r;
            case BinOp bo:
                return new BinOp(bo.getOp(), substitute(bo.getE1(), angleMap), substitute(bo.getE2(), angleMap));
            case Symbol s: {
                if (s.getSymbol().equals("pi")) {
                    return new Real(Math.PI);
                } else {
                    if(!angleMap.containsKey(s.getSymbol())) {
                        return s;
                    }
                    return angleMap.get(s.getSymbol());
                }
            }
            case UnOp uo:
                return new UnOp(uo.getOp(), substitute(uo.getE(), angleMap));
            case Var v:
                if (!angleMap.containsKey(v.getId())) {
                    return v;
                }
                return angleMap.get(v.getId());
            default:
                assert false;
                return null; // stupid hack to make the compiler happy ugh
        }
    }

    private static Real evalBinOp(BinOp bo, Map<String, Expr> angleMap) {
        Real v1 = eval(bo.getE1(), angleMap);
        Real v2 = eval(bo.getE2(), angleMap);
        switch (bo.getOp()) {
            case PLUS:
                return new Real(v1.getNumber() + v2.getNumber());
            case SUBTRACT:
                return new Real(v1.getNumber() - v2.getNumber());
            case MULT:
                return new Real(v1.getNumber() * v2.getNumber());
            case DIV:
                return new Real(v1.getNumber() / v2.getNumber());
            default:
                throw new RuntimeException(String.format("unimplemented BinOp: %s", bo.getOp()));
        }
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

    private static Real evalUnOp(UnOp uo, Map<String, Expr> angleMap) {
        Real v = eval(uo.getE(), angleMap);
        switch (uo.getOp()) {
            case MINUS:
                return new Real(-v.getNumber());
            default:
                throw new RuntimeException(String.format("unimplemented UnOp: %s", uo.getOp()));
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


    public int getDagHash() {
        int hash = 31;
        boolean isDirected = dag.getType().isDirected();

        int part;
        for (Iterator<Edge> var3 = dag.edgeSet().iterator(); var3.hasNext(); hash += part) {
            Edge e = var3.next();
            part = e.hash();
            int source = dag.getEdgeSource(e).hash();
            int target = dag.getEdgeTarget(e).hash();
            int pairing = source + target;
            if (isDirected) {
                pairing = pairing * (pairing + 1) / 2 + target;
            }

            part = 31 * part + pairing;
            part = 31 * part + Double.hashCode(dag.getEdgeWeight(e));
        }

        return hash;
    }
}
