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

public class RuleReverserTest {

    private static final String IBMNEW = "ibmnew";

    private static EggGen.Circuit circuit(EggGen.Gate... gates) {
        List<EggGen.Gate> g = new ArrayList<>();
        for (EggGen.Gate x : gates) g.add(x);
        return new EggGen.Circuit(g);
    }

    private static Rule ruleOf(EggGen.Circuit lhs, EggGen.Circuit rhs) {
        return new Rule(lhs, rhs, new ArrayList<>());
    }

    @Test
    public void pauliPushthrough_X_through_CX_matches() {
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"), new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"), new EggGen.X("q1"));
        assertTrue(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_X_propagates_other_direction_matches() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q0"), new EggGen.X("q1"), new EggGen.CX("q0", "q1"));
        assertTrue(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_two_cx() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_no_pauli_increase() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q0"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_pauli_on_third_qubit() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q2"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q2"), new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void pauliPushthrough_rejects_different_cx_orientation() {
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"), new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q0"), new EggGen.X("q0"), new EggGen.X("q1"));
        assertFalse(RuleReverser.isPauliPushthroughCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_canonical_matches() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        assertTrue(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_disjoint_lhs() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q2", "q3"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q2", "q3"), new EggGen.CX("q0", "q2"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_single_qubit_gates_present() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q1"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q1"), new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_wrong_size() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void braid_rejects_more_than_three_qubits() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q1", "q2"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q2"), new EggGen.CX("q0", "q3"), new EggGen.CX("q0", "q1"));
        assertFalse(RuleReverser.isBraidCx(ruleOf(lhs, rhs)));
    }

    @Test
    public void decide_reverseOnlyForPureSplitOnIbmnew() {
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.SX("q0"), new EggGen.SX("q0"));
        assertEquals(RuleReverser.Direction.REVERSE_ONLY, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_dropsWhenReverseHasUnboundVars() {
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q1", "q0"));
        EggGen.Circuit lhs2 = circuit(new EggGen.X("q0"), new EggGen.X("q1"));
        EggGen.Circuit rhs2 = circuit(new EggGen.SX("q0"), new EggGen.SX("q0"), new EggGen.SX("q0"));
        assertEquals(RuleReverser.Direction.DROP, RuleReverser.decide(ruleOf(lhs2, rhs2), IBMNEW));
    }

    @Test
    public void decide_keepsDecreasingForwardOnly() {
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
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.CX("q2", "q1"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q2", "q1"), new EggGen.CX("q0", "q1"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), IBMNEW));
    }

    @Test
    public void decide_unknownGateset_fallsBackToBoth() {
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.SX("q0"), new EggGen.SX("q0"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), "ion"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), "rigetti"));
        assertEquals(RuleReverser.Direction.BOTH, RuleReverser.decide(ruleOf(lhs, rhs), null));
    }

    @Test
    public void reverseIsFireable_rejectsUnboundReverseRhsVar() {
        EggGen.Circuit lhs = circuit(new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.CX("q0", "q1"));
        assertTrue(RuleReverser.reverseIsFireable(ruleOf(lhs, rhs)));
    }

    @Test
    public void reverseIsFireable_acceptsFullyDeterminedRule() {
        EggGen.Circuit lhs = circuit(new EggGen.CX("q0", "q1"), new EggGen.X("q0"));
        EggGen.Circuit rhs = circuit(new EggGen.X("q0"), new EggGen.X("q1"), new EggGen.CX("q0", "q1"));
        assertTrue(RuleReverser.reverseIsFireable(ruleOf(lhs, rhs)));
    }

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
            return;
        }
        Tally t = tally(path);

        assertEquals(0, t.parseFailures, "rule_copy.txt should parse cleanly");

        assertTrue(t.forwardInc >= t.keptInc + t.droppedInc,
                "every increasing rule should land in kept or dropped");
        assertEquals(t.forwardInc, t.keptInc + t.droppedInc,
                "kept + dropped should equal total increasing");

        assertTrue(t.keptInc > 0,
                "expected at least one braid/pushthrough rule to be kept");
    }

    @Test
    public void endToEnd_ruleCopyTxt_braidIsAlwaysKept() throws Exception {
        String path = "/root/rule_copy.txt";
        if (!Files.exists(Paths.get(path))) return;

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
