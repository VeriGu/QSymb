import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ast.Expr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class OptimizerHelperTest {

    private static final String HDR =
            "OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[5];\ncreg c[5];\n";

    private final Optimizer opt = new Optimizer();

    private CircuitDAG parse(String body) {
        return QASMToDAGVisitor.parse(HDR + body);
    }

    private List<Node> candidateStarts(CircuitDAG circuit, Node patternStart) {
        List<Node> out = new ArrayList<>();
        for (Node n : circuit.nodes()) {
            if (n.isGate() && n.getId().equals(patternStart.getId())) out.add(n);
        }
        return out;
    }

    private List<Node> firstFullMatch(CircuitDAG circuit, CircuitDAG pattern) {
        Node patternStart = opt.primaryGateRoot(pattern);
        int primarySize = opt.primaryComponentSize(pattern, opt.secondaryComponents(pattern));
        for (Node start : candidateStarts(circuit, patternStart)) {
            Map<String, String> qm = new HashMap<>();
            Map<String, String> rm = new HashMap<>();
            Map<String, Expr> am = new HashMap<>();
            List<Node> matched = opt.matchPrimaryComponent(
                    circuit, pattern, start, patternStart, qm, rm, am, new HashSet<>());
            if (matched != null && matched.size() == primarySize) return matched;
        }
        return null;
    }

    @Test
    public void matchesLinearChainFully() {
        CircuitDAG circuit = parse("cx q[0],q[1];\nh q[0];\nx q[1];\n");
        CircuitDAG pattern = parse("cx q[0],q[1];\nh q[0];\n");
        int primarySize = opt.primaryComponentSize(pattern, opt.secondaryComponents(pattern));
        assertEquals(2, primarySize);

        List<Node> matched = firstFullMatch(circuit, pattern);
        assertNotNull(matched, "linear chain fragment should match");
        assertEquals(2, matched.size());
        assertEquals(2, new HashSet<>(matched).size());
        for (Node n : matched) assertTrue(n.isGate());
    }

    @Test
    public void matchesBranchingFragmentFully() {
        CircuitDAG circuit = parse(
                "cx q[0],q[1];\nh q[0];\nrz(pi) q[1];\ncx q[2],q[1];\n"
                + "cx q[0],q[1];\ncx q[0],q[1];\nx q[0];\n");
        CircuitDAG pattern = parse(
                "cx q[0],q[1];\nh q[0];\nrz(pi) q[1];\ncx q[2],q[1];\n");
        int primarySize = opt.primaryComponentSize(pattern, opt.secondaryComponents(pattern));
        assertEquals(4, primarySize);
        assertTrue(opt.secondaryComponents(pattern).isEmpty(), "fragment is one component");

        List<Node> matched = firstFullMatch(circuit, pattern);
        assertNotNull(matched, "branching fragment should match in full (regression)");
        assertEquals(4, matched.size());
        assertEquals(4, new HashSet<>(matched).size());
    }

    @Test
    public void returnsNullWhenFragmentAbsent() {
        CircuitDAG circuit = parse("cx q[0],q[1];\nh q[0];\nx q[1];\n");
        CircuitDAG pattern = parse("cx q[0],q[1];\nt q[0];\n");
        assertNull(firstFullMatch(circuit, pattern), "absent fragment must not match");
    }

    @Test
    public void respectsTakenNodes() {
        CircuitDAG circuit = parse("cx q[0],q[1];\nh q[0];\nx q[1];\n");
        CircuitDAG pattern = parse("cx q[0],q[1];\nh q[0];\n");
        Node patternStart = opt.primaryGateRoot(pattern);
        List<Node> starts = candidateStarts(circuit, patternStart);
        assertTrue(!starts.isEmpty());

        Set<Node> taken = new HashSet<>(starts);
        for (Node start : starts) {
            List<Node> matched = opt.matchPrimaryComponent(circuit, pattern, start, patternStart,
                    new HashMap<>(), new HashMap<>(), new HashMap<>(), taken);
            assertNull(matched, "a taken start node must not match");
        }
    }

    @Test
    public void rejectsConflictingQubitBinding() {
        CircuitDAG circuit = parse("cx q[0],q[1];\nh q[0];\nx q[1];\n");
        CircuitDAG pattern = parse("cx q[0],q[1];\nh q[0];\n");
        Node patternStart = opt.primaryGateRoot(pattern);

        boolean anyMatched = false;
        for (Node start : candidateStarts(circuit, patternStart)) {
            Map<String, String> qm = new HashMap<>();
            qm.put("q0", "qZ");
            List<Node> matched = opt.matchPrimaryComponent(circuit, pattern, start, patternStart,
                    qm, new HashMap<>(), new HashMap<>(), new HashSet<>());
            anyMatched |= (matched != null);
        }
        assertTrue(!anyMatched, "a conflicting incoming qubit binding must be rejected");
    }

    @Test
    public void componentDecompositionCountsAllGates() {
        CircuitDAG pattern = parse("cx q[0],q[1];\nh q[0];\nx q[2];\n");
        List<List<Node>> secs = opt.secondaryComponents(pattern);
        int primarySize = opt.primaryComponentSize(pattern, secs);

        int secTotal = 0;
        for (List<Node> c : secs) secTotal += c.size();
        assertEquals(3, primarySize + secTotal, "primary + secondary gates == total gates");
        assertEquals(1, secs.size(), "exactly one disconnected secondary component");
        assertEquals(1, secs.get(0).size(), "secondary component is the single x gate");
    }

    @Test
    public void adjacentGateOnWireFollowsWires() {
        CircuitDAG circuit = parse("cx q[0],q[1];\nh q[0];\nx q[1];\n");
        Node cx = null, h = null, x = null;
        for (Node n : circuit.nodes()) {
            if (!n.isGate()) continue;
            if (n.isCX()) cx = n;
            else if (n.getQubits().contains("q0")) h = n;
            else if (n.getQubits().contains("q1")) x = n;
        }
        assertNotNull(cx); assertNotNull(h); assertNotNull(x);

        assertEquals(h, opt.adjacentGateOnWire(circuit, cx, "q0", false));
        assertEquals(x, opt.adjacentGateOnWire(circuit, cx, "q1", false));
        assertEquals(cx, opt.adjacentGateOnWire(circuit, h, "q0", true));
        assertNull(opt.adjacentGateOnWire(circuit, cx, "q0", true));
        assertNull(opt.adjacentGateOnWire(circuit, cx, "q1", true));
    }
}
