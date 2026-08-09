import java.util.List;

import java.util.ArrayList;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymbolicThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(SymbolicThread.class);
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

    private static final double SYMB_COVERAGE = Double.parseDouble(System.getProperty("symb.coverage", "0.20"));
    private static final int SYMB_TRIES_OVERRIDE = Integer.getInteger("symb.tries", 0);

    @Override
    public void run() {
        int total = symbRules.size() + symbRulesMonomials.size();
        if (total == 0) return;
        Set<String> circuitGates = circuitGateNames(circuit);

        int budget = (SYMB_TRIES_OVERRIDE > 0)
                ? Math.min(SYMB_TRIES_OVERRIDE, total)
                : Math.max(1, Math.min(total, (int) Math.round(SYMB_COVERAGE * total)));
        java.util.HashSet<Integer> tried = new java.util.HashSet<>();

        while (tried.size() < budget) {
            int idx = rand.nextInt(total);
            if (!tried.add(idx)) continue;

            Optimizer.SYMB_ATTEMPTS.incrementAndGet();
            CircuitDAG optimizedDAG;
            String appliedRuleDesc;
            if (idx < symbRulesMonomials.size()) {
                MononialRule rule = symbRulesMonomials.get(idx);
                appliedRuleDesc = "mono idx=" + idx + " " + rule.getLhs() + " -> " + rule.getRhs();
                optimizedDAG = optimizer.symbolicMatchBeforeAfterMono(
                        circuit, rule.getRhs(), rule.getLhs(), minSymb, maxSymb, rule.getConstraints(), null);
            } else {
                MatrixConstrainedRule rule = symbRules.get(idx - symbRulesMonomials.size());
                if (!circuitGates.containsAll(ruleLhsGateNames(rule.getLHS()))) {
                    Optimizer.SYMB_SKIP_GATES.incrementAndGet();
                    continue;
                }
                appliedRuleDesc = "matrix idx=" + (idx - symbRulesMonomials.size())
                        + " " + rule.getLHS() + " -> " + rule.getRHS();
                optimizedDAG = optimizer.symbolicMatchBeforeAfter(
                        circuit, rule.getLHS(), rule.getRHS(), minSymb, maxSymb, rule.getConstraint(), null);
            }

            if (optimizedDAG != null) {
                String d = appliedRuleDesc.length() > 160 ? appliedRuleDesc.substring(0, 160) + "..." : appliedRuleDesc;
                logger.info("[SYMBRULE applied] {}", d);
                System.out.println("[SYMBRULE applied] " + d);
                Optimizer.SYMB_APPLIED.incrementAndGet();
                applySAaccept(optimizedDAG);
                return;
            } else {
                Optimizer.SYMB_NO_MATCH.incrementAndGet();
            }
        }
    }

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
