import java.util.*;

/**
 * Decides which rules in rule_copy.txt should be added in their forward
 * direction, their reverse direction, both, or neither.
 *
 * Replaces the old unconditional auto-reverse logic in Optimizer.optimize_SA
 * that auto-generated noisy or unfireable rules (e.g. `x -> sx;sx`,
 * `rz(t1+t2) -> rz(t1);rz(t2)` with unbound t2).
 *
 * Per-gateset strategy:
 *   - ibmnew / nam (CX-based): keep all forward-decreasing as-is, never
 *     auto-reverse them; keep forward-increasing ONLY if it matches a
 *     Pauli-pushthrough or CNOT-braid pattern; the reverse of those is
 *     auto-added (it's the size-decreasing direction, always useful).
 *   - other gatesets: temporary fallback to the OLD behaviour
 *     (unconditional auto-reverse). To be replaced when we work out the
 *     per-gateset patterns (Pauli-pushthrough on CZ has X->Z propagation,
 *     braids don't apply to CZ/RXX, etc).
 *
 * Definitions of the two patterns (CX gateset):
 *
 *  Pauli pushthrough:
 *    Exactly one CX gate on each side, on the same {control, target} pair.
 *    RHS has strictly more single-qubit Pauli gates than LHS. The 2q gate
 *    sits on either side of those Paulis. Captures
 *      X q0; cx q0 q1   ->   cx q0 q1; X q0; X q1
 *      Z q1; cx q0 q1   ->   cx q0 q1; Z q0; Z q1
 *
 *  CNOT braid (THE Toffoli-enabling rule):
 *    Exactly two CX gates on LHS, exactly three CX gates on RHS, no
 *    single-qubit gates on either side. The union of qubits touched has
 *    <= 3 distinct values. The two LHS CXs share at least one qubit
 *    (otherwise no rearrangement is possible). Captures the canonical
 *      cx a,b; cx b,c   ->   cx b,c; cx a,c; cx a,b
 *    and its 6 orientation variants.
 */
public class RuleReverser {

    public enum Direction { FORWARD_ONLY, REVERSE_ONLY, BOTH, DROP }

    /**
     * Decide which directions of `rule` to add to the rewrite set.
     * Used per-rule by Optimizer.optimize_SA after parsing rule_copy.txt.
     */
    public static Direction decide(Rule rule, String gateset) {
        if (!isCxGateset(gateset)) {
            // Fallback: keep the old behavior. Until we ship the per-gateset
            // patterns, don't change behavior for ion/rigetti/etc.
            return Direction.BOTH;
        }
        int lhsSize = rule.lhs.gates.size();
        int rhsSize = rule.rhs.gates.size();

        if (lhsSize == rhsSize) {
            // Symmetric — handled as birewrite by caller.
            return Direction.BOTH;
        }
        if (lhsSize > rhsSize) {
            // Forward direction is size-decreasing → always useful, ALWAYS keep.
            // Special case: a DECREASING CNOT braid (3 CX → 2 CX) should also
            // expose its increasing reverse (2 CX → 3 CX), because that
            // rearrangement direction is what lets later rules cancel CXs
            // (the Toffoli-enabling braid). isBraidCx recognizes the 2→3
            // shape, so we test it on the swapped rule.
            Rule swapped = new Rule(rule.rhs, rule.lhs, rule.conditions);
            if (isBraidCx(swapped) && reverseIsFireable(rule)) {
                return Direction.BOTH;
            }
            // Otherwise never auto-reverse: the reverse would be either
            // unfireable (empty LHS, e.g. ε -> cx;cx) or noise (e.g. x -> sx;sx,
            // rz(t1+t2) -> rz(t1);rz(t2) with vars that don't bind on
            // concrete circuits).
            return Direction.FORWARD_ONLY;
        }
        // lhsSize < rhsSize: increasing direction.
        if (isPauliPushthroughCx(rule) || isBraidCx(rule)) {
            // Forward (increasing) exposes the rearrangement opportunity.
            // Reverse (decreasing) is the contraction direction — always useful.
            return reverseIsFireable(rule) ? Direction.BOTH : Direction.FORWARD_ONLY;
        }
        // Increasing but not a target pattern. Forward direction would
        // inflate the e-graph for little gain, so drop it; BUT the reverse
        // (LHS↔RHS swap) is a real size-reducing rewrite — keep that.
        // Example: rz(pi/2);x;rz(pi) → x;rz(pi/2) is a useful 3→2 reduction
        // even though x;rz(pi/2) → rz(pi/2);x;rz(pi) (forward) is just
        // X-through-RZ noise.
        return reverseIsFireable(rule) ? Direction.REVERSE_ONLY : Direction.DROP;
    }

    // ---- Pattern detectors ----

    static boolean isPauliPushthroughCx(Rule rule) {
        int lhsCx = countCx(rule.lhs);
        int rhsCx = countCx(rule.rhs);
        if (lhsCx != 1 || rhsCx != 1) return false;

        EggGen.CX lhsCxGate = firstCx(rule.lhs);
        EggGen.CX rhsCxGate = firstCx(rule.rhs);
        if (lhsCxGate == null || rhsCxGate == null) return false;

        // Same {control, target} pair (order can differ — CX is asymmetric
        // but the rewrites only swap roles when control/target are
        // explicitly different gates; here we want literal same orientation).
        if (!lhsCxGate.control.equals(rhsCxGate.control)
                || !lhsCxGate.target.equals(rhsCxGate.target)) {
            // Allow the symmetric-pair case too: same qubits, swapped
            // control/target — that's a "pivot" rule, not a pushthrough.
            return false;
        }

        int lhsPaulis = countSingleQubitPaulis(rule.lhs);
        int rhsPaulis = countSingleQubitPaulis(rule.rhs);

        // RHS must have strictly more Paulis than LHS.
        if (rhsPaulis <= lhsPaulis) return false;

        // All single-qubit gates on either side must be on {control, target}.
        // (Pushthrough doesn't introduce Paulis on a third qubit.)
        Set<String> allowed = new HashSet<>();
        allowed.add(lhsCxGate.control);
        allowed.add(lhsCxGate.target);
        if (!singleQubitGatesOnlyOn(rule.lhs, allowed)) return false;
        if (!singleQubitGatesOnlyOn(rule.rhs, allowed)) return false;

        return true;
    }

    static boolean isBraidCx(Rule rule) {
        int lhsCx = countCx(rule.lhs);
        int rhsCx = countCx(rule.rhs);
        if (lhsCx != 2 || rhsCx != 3) return false;

        // No single-qubit gates on either side.
        if (rule.lhs.gates.size() != 2 || rule.rhs.gates.size() != 3) return false;
        for (EggGen.Gate g : rule.lhs.gates) if (!(g instanceof EggGen.CX)) return false;
        for (EggGen.Gate g : rule.rhs.gates) if (!(g instanceof EggGen.CX)) return false;

        // <= 3 distinct qubits total.
        Set<String> qubits = new HashSet<>();
        for (EggGen.Gate g : rule.lhs.gates) addCxQubits(qubits, (EggGen.CX) g);
        for (EggGen.Gate g : rule.rhs.gates) addCxQubits(qubits, (EggGen.CX) g);
        if (qubits.size() > 3) return false;

        // The two LHS CXs must share at least one qubit (otherwise they're
        // already disjoint and there's nothing to braid).
        EggGen.CX a = (EggGen.CX) rule.lhs.gates.get(0);
        EggGen.CX b = (EggGen.CX) rule.lhs.gates.get(1);
        Set<String> aq = new HashSet<>(); addCxQubits(aq, a);
        Set<String> bq = new HashSet<>(); addCxQubits(bq, b);
        aq.retainAll(bq);
        return !aq.isEmpty();
    }

    // ---- Helpers ----

    static int countCx(EggGen.Circuit c) {
        int n = 0;
        for (EggGen.Gate g : c.gates) if (g instanceof EggGen.CX) n++;
        return n;
    }

    static EggGen.CX firstCx(EggGen.Circuit c) {
        for (EggGen.Gate g : c.gates) if (g instanceof EggGen.CX) return (EggGen.CX) g;
        return null;
    }

    static int countSingleQubitPaulis(EggGen.Circuit c) {
        int n = 0;
        for (EggGen.Gate g : c.gates) {
            if (g instanceof EggGen.X || g instanceof EggGen.SX) n++;
            // Z is not a class here in this codebase; SZ/T/etc could be
            // added later. RZ counts as a Clifford-rotation, not a Pauli
            // pushthrough trigger.
        }
        return n;
    }

    static boolean singleQubitGatesOnlyOn(EggGen.Circuit c, Set<String> allowed) {
        for (EggGen.Gate g : c.gates) {
            String q = singleQubit(g);
            if (q == null) continue; // not a 1q gate
            if (!allowed.contains(q)) return false;
        }
        return true;
    }

    static String singleQubit(EggGen.Gate g) {
        if (g instanceof EggGen.X)   return ((EggGen.X) g).qubit;
        if (g instanceof EggGen.SX)  return ((EggGen.SX) g).qubit;
        if (g instanceof EggGen.H)   return ((EggGen.H) g).qubit;
        if (g instanceof EggGen.RX)  return ((EggGen.RX) g).qubit;
        if (g instanceof EggGen.RY)  return ((EggGen.RY) g).qubit;
        if (g instanceof EggGen.RZ)  return ((EggGen.RZ) g).qubit;
        return null;
    }

    static void addCxQubits(Set<String> qubits, EggGen.CX cx) {
        qubits.add(cx.control);
        qubits.add(cx.target);
    }

    static boolean isCxGateset(String g) {
        return "ibmnew".equals(g) || "ibm".equals(g) || "nam".equals(g)
                // Used by the differential test where wire rules are empty
                // (rules_ibmnewmin.txt) so we can compare against Quasar's
                // atomic-only ruleset on an equal footing.
                || "ibmnewmin".equals(g);
    }

    static boolean reverseIsFireable(Rule rule) {
        // Reverse swaps LHS and RHS. For the reversed rule to be fireable,
        // every variable used on its RHS (= the original LHS) must be
        // bound by its LHS (= the original RHS). i.e.
        //     original_RHS_vars ⊇ original_LHS_vars
        // Otherwise reversed-RHS introduces an unbound variable and egglog
        // rejects the rule at registration time. (Note: we check qubit
        // vars only here; angle-var binding is checked by egglog's parser
        // at addRewrite time and surfaces via [EGGLOG-ERR].)
        Set<String> lhsVars = collectQubitVars(rule.lhs);
        Set<String> rhsVars = collectQubitVars(rule.rhs);
        return rhsVars.containsAll(lhsVars);
    }

    static Set<String> collectQubitVars(EggGen.Circuit c) {
        Set<String> vars = new HashSet<>();
        for (EggGen.Gate g : c.gates) {
            String s = singleQubit(g);
            if (s != null) vars.add(s);
            else if (g instanceof EggGen.CX) {
                vars.add(((EggGen.CX) g).control);
                vars.add(((EggGen.CX) g).target);
            } else if (g instanceof EggGen.CZ) {
                vars.add(((EggGen.CZ) g).control);
                vars.add(((EggGen.CZ) g).target);
            } else if (g instanceof EggGen.RXX) {
                vars.add(((EggGen.RXX) g).qubit1);
                vars.add(((EggGen.RXX) g).qubit2);
            }
        }
        return vars;
    }
}
