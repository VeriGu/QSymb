import java.util.List;

import java.util.ArrayList;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymbolicThread extends Thread {
    private final CircuitDAG circuit;
    private final List<MatrixConstrainedRule> symbRules;
    private final List<MononialRule> symbRulesMonomials;
    private final int minSymb;
    private final int maxSymb;


    private final Random rand;
    private final Optimizer optimizer;
    private CircuitDAG result;
    public SymbolicThread(CircuitDAG circuit, List<MatrixConstrainedRule> symbRules, List<MononialRule> symbRulesMonomials, int minSymb, int maxSymb, Random rand, Optimizer optimizer) {
        this.circuit = circuit;
        this.symbRules = symbRules;
        this.symbRulesMonomials = symbRulesMonomials;
        this.minSymb = minSymb;
        this.maxSymb = maxSymb;
        this.rand = rand;
        this.optimizer = optimizer;
        this.result = null;
    }

    public CircuitDAG getResult() {
        return result;
    }

    @Override
    public void run() {
        // Draw ONE random rule from the combined pool per spawn -- the SA loop
        // exploration relies on rule-level diversity across many spawns, not
        // on each spawn exhausting the rule list. A cheap LHS-gate-set
        // prefilter still applies: if the picked rule wants a gate that isn't
        // even in the circuit, we skip it (no work) instead of paying for the
        // matcher + checkLinearCombination on a guaranteed miss.
        int total = symbRules.size() + symbRulesMonomials.size();
        if (total == 0) return;
        int idx = rand.nextInt(total);

        Set<String> circuitGates = circuitGateNames(circuit);
        CircuitDAG optimizedDAG;
        if (idx < symbRulesMonomials.size()) {
            MononialRule rule = symbRulesMonomials.get(idx);
            optimizedDAG = optimizer.symbolicMatchBeforeAfterMono(
                    circuit, rule.getRhs(), rule.getLhs(), minSymb, maxSymb, rule.getConstraints(), null);
        } else {
            MatrixConstrainedRule rule = symbRules.get(idx - symbRulesMonomials.size());
            if (!circuitGates.containsAll(ruleLhsGateNames(rule.getLHS()))) {
                return;   // picked rule's LHS gates aren't in this circuit -- skip
            }
            // [SYMB_PICK idx=...] line removed -- enable Optimizer.logger.debug if needed.
            optimizedDAG = optimizer.symbolicMatchBeforeAfter(
                    circuit, rule.getLHS(), rule.getRHS(), minSymb, maxSymb, rule.getConstraint(), null);
        }

        if (optimizedDAG != null) {
            applySAaccept(optimizedDAG);
        }
        // else: result stays null; main loop treats this as a symbolic-skip.
    }

    /**
     * Pattern that finds gate-like tokens in a rule LHS: an upper-case head
     * right after an opening paren. Skips structural / non-gate tokens
     * (Cons, Nil, SYMB, BinOp, UnOp, Real, Symbol, Q, ...).
     */
    private static final Pattern RULE_GATE = Pattern.compile("\\(([A-Z][A-Za-z0-9]*)\\b");
    private static final Set<String> NOT_GATES = new HashSet<>(java.util.Arrays.asList(
            "Cons", "Nil", "SYMB", "BinOp", "UnOp", "Real", "Symbol", "Q",
            "PLUS", "MINUS", "MULT", "DIV", "SUBTRACT"));

    private static Set<String> ruleLhsGateNames(String lhs) {
        Set<String> names = new HashSet<>();
        Matcher m = RULE_GATE.matcher(lhs);
        while (m.find()) {
            String tok = m.group(1);
            if (!NOT_GATES.contains(tok)) names.add(tok.toLowerCase());
        }
        return names;
    }

    private static Set<String> circuitGateNames(CircuitDAG c) {
        Set<String> names = new HashSet<>();
        for (Node n : c.nodes()) {
            if (n.isGate()) names.add(n.getId().toLowerCase());
        }
        return names;
    }

    /** Apply the SA accept/reject decision and store the resulting DAG. */
    private void applySAaccept(CircuitDAG optimizedDAG) {
        List<String> rulesApplied = new ArrayList<>(circuit.getRulesApplied());
        optimizedDAG.setRulesApplied(rulesApplied);
        if (optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) <= circuit.cost(Params.OPTIMIZATION_OBJECTIVE)) {
            result = optimizedDAG;
        } else {
            double acceptP = Math.min(1, Math.exp(-Params.TEMPERATURE
                    * ((double) optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE)
                       / circuit.cost(Params.OPTIMIZATION_OBJECTIVE))));
            result = (rand.nextDouble() <= acceptP) ? optimizedDAG : circuit;
        }
    }
}
