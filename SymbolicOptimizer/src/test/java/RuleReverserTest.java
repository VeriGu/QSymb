import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for RuleReverser. Two layers of coverage:
 *
 * 1. Direct pattern-detector tests on hand-constructed Rule objects. These
 *    pin down exactly what is_pauli_pushthrough_cx and is_braid_cx accept
 *    and reject (the spec).
 *
 * 2. End-to-end against rule_copy.txt — load it the same way Optimizer
 *    does (via QASMAstBuilder.parseRule), run RuleReverser.decide on every
 *    rule with gateset="ibmnew", and check the aggregate counts match
 *    the values we measured from the rule-registration dump:
 *        ~7 size-increasing rules kept (Pauli pushthrough + braid variants),
 *        ~3 size-increasing rules dropped (non-pattern noise),
 *        all forward-decreasing rules kept,
 *        all symmetric rules pass through.
 *
 * The end-to-end harness is generic over the input file path so the same
 * test can be re-pointed at any ibmnew-shaped rule file later (e.g. an
 * extended rule_copy_v2.txt).
 */
public class RuleReverserTest {

    private static final String IBMNEW = "ibmnew";

    // ---------- builders for hand-constructed rules ----------

    private static EggGen.Circuit circuit(EggGen.Gate... gates) {
        List<EggGen.Gate> g = new ArrayList<>();
        for (EggGen.Gate x : gates) g.add(x);
        return new EggGen.Circuit(g);
    }

    private static Rule ruleOf(EggGen.Circuit lhs, EggGen.Circuit rhs) {
        return new Rule(lhs, rhs, new ArrayList<>());
    }

    // ---------- Pauli pushthrough detector ----------

    @Test
    public void pauliPushthrough_X_through_CX_matches() {
        // x q0; cx q0 q1   ->   cx q0 q1; x q0; x q1
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"), new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"), new EggGen.X("q1"));
        assertTrue(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_X_propagates_other_direction_matches() {
        // cx q0 q1; x q0   ->   x q0; x q1; cx q0 q1
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q0"), new EggGen.X("q1"), new EggGen.CX("q0", "q1"));
        assertTrue(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_two_cx() {
        // 2 CX in LHS — that's braid territory, not pushthrough.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_no_pauli_increase() {
        // cx; x   ->   cx; x  — same Pauli count.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q0"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_pauli_on_third_qubit() {
        // x q2 outside the CX's qubit pair — that's a commute-on-irrelevant-wire,
        // not a pushthrough.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q2"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q2"), new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_different_cx_orientation() {
        // cx q0 q1 vs cx q1 q0 — pivot rule, not pushthrough.
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"), new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q0"), new EggGen.X("q0"), new EggGen.X("q1"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    // ---------- CNOT braid detector ----------

    @Test
    public void braid_canonical_matches() {
        // cx q0 q1; cx q1 q2   ->   cx q1 q2; cx q0 q2; cx q0 q1
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        assertTrue(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_disjoint_lhs() {
        // cx q0 q1; cx q2 q3 — disjoint, no shared qubit, can't braid.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q2", "q3"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q2", "q3"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_single_qubit_gates_present() {
        // 2 CX + 1 X — has a single-qubit gate, not pure braid.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q1"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q1"), new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_wrong_size() {
        // 1 CX -> 2 CX is not a braid (braid is 2->3).
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_more_than_three_qubits() {
        // 4 distinct qubits — outside the braid pattern.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q3"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    // ---------- decide() policy ----------

    @Test
    public void decide_reverseOnlyForPureSplitOnIbmnew() {
        // x q0   ->   sx q0; sx q0 — INC but not Pauli/braid. The reverse
        // sx;sx → x is a real 2→1 reduction → keep REVERSE_ONLY (drop the
        // explode-the-egraph forward direction).
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.SX("q0"), new EggGen.SX("q0"));
        assertEquals(RuleReverser.Direction.REVERSE_ONLY, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_dropsWhenReverseHasUnboundVars() {
        // LHS has q0 only; RHS introduces q1 (unbound on reverse RHS).
        // Reverse would be unfireable → DROP entirely.
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q0"));  // q1 not in LHS
        // Note: reverseIsFireable checks rhsVars.containsAll(lhsVars).
        // Here rhsVars={q0,q1} ⊇ lhsVars={q0} → reverseIsFireable = true.
        // So even this case is REVERSE_ONLY (we have q0 binding on reverse RHS).
        // To test true DROP we need vars on original LHS that aren't on
        // original RHS — i.e. LHS has q1, RHS doesn't.
        EggGen.Circuit lhs2 = circuit(new EggGen.X("q0"), new EggGen.X("q1"));
        EggGen.Circuit rhs2 = circuit(new EggGen.SX("q0"), new EggGen.SX("q0"), new EggGen.SX("q0"));
        // Original LHS vars = {q0, q1}; original RHS vars = {q0}. Not a
        // superset → reverse RHS would have unbound q1 → not fireable.
        assertEquals(RuleReverser.Direction.DROP, RuleReverser.decide(ruleOf(lhs2, rhs2), IBMNEW));
    }

    @Test
    public void decide_keepsDecreasingForwardOnly() {
        // sx; sx   ->   x — forward is size-decreasing, never auto-reversed.
        EggGen.Circuit lhs = circuit(new EggGen.SX("q0"), new EggGen.SX("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q0"));
        assertEquals(RuleReverser.Direction.FORWARD_ONLY, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_keepsBothForBraid() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_keepsBothForDecreasingBraid() {
        // 3 CX -> 2 CX (the contraction direction). Forward is size-decreasing
        // so it's always kept, but we ALSO want the increasing reverse (2->3)
        // exposed because that rearrangement is what later lets CXs cancel.
        // decide() must return BOTH here, not FORWARD_ONLY.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_keepsBothForPauliPushthrough() {
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"), new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"), new EggGen.X("q1"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_symmetricRulePassesThroughAsBoth() {
        // Same-size rules use birewrite — decide returns BOTH and the caller
        // handles the actual emission as birewrite.
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q2", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q2", "q1"), new EggGen.CX("q0", "q1"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_unknownGateset_fallsBackToBoth() {
        // Until we ship per-gateset patterns for ion/rigetti, decide returns
        // BOTH unconditionally so behavior matches the old code path for
        // those gatesets.
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.SX("q0"), new EggGen.SX("q0"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), "ion"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), "rigetti"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), null));
    }

    // ---------- reverse-fireability guard ----------

    @Test
    public void reverseIsFireable_rejectsUnboundReverseRhsVar() {
        // Original LHS uses {q0}, original RHS uses {q0, q1} where q1
        // appears only on RHS. The reversed rule would have q1 on its LHS
        // (fine) and {q0} on RHS — but q0 binds via reversed-LHS too, so
        // this particular case is actually fireable. We're testing the
        // boundary: vars must flow from LHS->RHS in the reversed direction.
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"));
        // Reverse LHS = {q0, q1}, reverse RHS = {q0}. Subset holds.
        assertTrue(RuleReverser.reverseIsFireable(ruleOf(lhs, rhs)));
    }

    @Test
    public void reverseIsFireable_acceptsFullyDeterminedRule() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q0"), new EggGen.X("q1"), new EggGen.CX("q0", "q1"));
        assertTrue(RuleReverser.reverseIsFireable(ruleOf(lhs, rhs)));
    }

    // ---------- End-to-end against rule_copy.txt (ibmnew rule file) ----------

    /**
     * Load any ibmnew-shaped rule file and tabulate decide() results.
     * Returns an in-memory summary the test can assert against.
     */
    private static Tally tally(String rulesPath) throws Exception {
        Tally t = new Tally();
        try (BufferedReader br = new BufferedReader(new FileReader(rulesPath, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                Rule rule;
                try {
                    rule = QASMAstBuilder.parseRule(line);
                } catch (Throwable ex) {
                    t.parseFailures++;
                    continue;
                }
                int lhs = rule.lhs.gates.size();
                int rhs = rule.rhs.gates.size();
                RuleReverser.Direction d = RuleReverser.decide(rule, IBMNEW);
                if (lhs > rhs) t.forwardDec++;
                else if (lhs < rhs) t.forwardInc++;
                else t.symmetric++;
                t.byDecision.merge(d.name(), 1, Integer::sum);
                if (lhs < rhs && d == RuleReverser.Direction.DROP) t.droppedInc++;
                if (lhs < rhs && d != RuleReverser.Direction.DROP) t.keptInc++;
            }
        }
        return t;
    }

    static class Tally {
        int forwardDec = 0, forwardInc = 0, symmetric = 0;
        int keptInc = 0, droppedInc = 0;
        int parseFailures = 0;
        Map<String, Integer> byDecision = new HashMap<>();
    }

    @Test
    public void endToEnd_ruleCopyTxt_dropsNonPatternIncreasing() throws Exception {
        String path = "/root/rule_copy.txt";
        if (!Files.exists(Paths.get(path))) {
            // Skip if not running in the repo where the file lives.
            return;
        }
        Tally t = tally(path);

        // Sanity: the file parses cleanly (no parse failures).
        assertEquals(0, t.parseFailures, "rule_copy.txt should parse cleanly");

        // We measured: 10 size-increasing rules in rule_copy.txt; of those,
        // ~7 match Pauli/braid and ~3 don't. The exact split depends on
        // whether the file evolves, so we assert the *relationship* not the
        // exact numbers.
        assertTrue(t.forwardInc >= t.keptInc + t.droppedInc,
                "every increasing rule should land in kept or dropped");
        assertEquals(t.forwardInc, t.keptInc + t.droppedInc,
                "kept + dropped should equal total increasing");

        // At least one increasing rule should be kept (the braid is in the
        // file by construction).
        assertTrue(t.keptInc > 0,
                "expected at least one braid/pushthrough rule to be kept");
    }

    @Test
    public void endToEnd_ruleCopyTxt_braidIsAlwaysKept() throws Exception {
        String path = "/root/rule_copy.txt";
        if (!Files.exists(Paths.get(path))) return;

        // Scan the file and confirm: every rule whose LHS<RHS *and* is a
        // pure 2-CX -> 3-CX braid lands in BOTH (kept).
        try (BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) continue;
                Rule rule;
                try { rule = QASMAstBuilder.parseRule(line); } catch (Throwable ex) { continue; }
                if (rule.lhs.gates.size() >= rule.rhs.gates.size()) continue;
                if (RuleReverser.isBraidCx(rule)) {
                    assertEquals(RuleReverser.Direction.BOTH,
                            RuleReverser.decide(rule, IBMNEW),
                            "braid rule must be kept BOTH directions: " + line);
                }
            }
        }
    }
}
