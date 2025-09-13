import org.jgrapht.graph.DirectedMultigraph;

import java.util.Comparator;

public class GraphComparator implements Comparator<OptCircuit> {

    public int sizeNoRz(DirectedMultigraph<Node, Edge> circuit) {
        int size = 0;
        for (Node n : circuit.vertexSet()) {
            if (n.isGate()) {
                if (!n.getId().equals("rz") && !n.getId().equals("u1")) {
                    size++;
                }
            }
        }
        return size;
    }

    @Override
    public int compare(OptCircuit o1, OptCircuit o2) {
//        if (sizeNoRz(o1.getCircuit()) < sizeNoRz(o2.getCircuit())) {
//            return -1;
//        } else if (sizeNoRz(o1.getCircuit()) > sizeNoRz(o2.getCircuit())) {
//            return 1;
//        } else {
            if (o1.getCircuit().vertexSet().size() < o2.getCircuit().vertexSet().size()) {
                return -1;
            } else if (o1.getCircuit().vertexSet().size() > o2.getCircuit().vertexSet().size()) {
                return 1;
            } else {
                if (o1.getCreateTime() < o2.getCreateTime()) {
                    return -1;
                } else if (o1.getCreateTime() > o2.getCreateTime()) {
                    return 1;
                } else {
                    return 0;
                }
            }
//        }
    }
}
