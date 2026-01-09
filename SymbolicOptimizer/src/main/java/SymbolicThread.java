import java.util.List;

import java.util.ArrayList;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

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
        List<MatrixConstrainedRule> copysymb = new ArrayList<>(symbRules);
        List<MononialRule> copysymbMonomial = new ArrayList<>(symbRulesMonomials);
        List<MatrixConstrainedRule> symbRulesToUse = new ArrayList<>();
        List<MononialRule> symbMonomialRulesToUse = new ArrayList<>();
        
       
        int index1 = rand.nextInt(copysymb.size() + copysymbMonomial.size());
        if(index1 < copysymb.size()) symbRulesToUse.add(copysymb.get(index1));
        else symbMonomialRulesToUse.add(copysymbMonomial.get(index1 - copysymb.size()));
    

        for (int i = 0; i < symbMonomialRulesToUse.size(); i++){
            //System.out.println("Current Monomial RULE: " + i + "/" + Integer.min(symb_rule_limit, symbMonomialRules.size()));
            int index = i;
            //System.out.println("Current SYMB MONOMIAL RULE: " + symbMonomialRulesToUse.get(index).getRhs() + " -> " + symbMonomialRulesToUse.get(index).getLhs());
            CircuitDAG optimizedDAG = optimizer.symbolicMatchBeforeAfterMono(circuit, symbMonomialRulesToUse.get(index).getRhs(), symbMonomialRulesToUse.get(index).getLhs(), minSymb, maxSymb, symbMonomialRulesToUse.get(index).getConstraints(), null);
            
            if(optimizedDAG != null) {
                //System.out.println("Applyed Monomial Rule: " + symbMonomialRulesToUse.get(index).getRhs() + " -> " + symbMonomialRulesToUse.get(index).getLhs());
                List<String> rulesApplied = new ArrayList<>(circuit.getRulesApplied());
                //rulesApplied.add(symbMonomialRulesToUse.get(index).getRhs() + "|" + symbMonomialRulesToUse.get(index).getLhs());
                optimizedDAG.setRulesApplied(rulesApplied);
                //symbMonomialRulesUsed.put(symbMonomialRulesToUse.get(index).getRhs() + "|" + symbMonomialRulesToUse.get(index).getLhs(), symbMonomialRulesUsed.getOrDefault(symbMonomialRulesToUse.get(index), 0) + 1);
                //System.out.println("Optimized Using Monomial Rule: " + optimizedDAG.toQASM());
                if(optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) <= circuit.cost(Params.OPTIMIZATION_OBJECTIVE)) {
                    result = optimizedDAG;
                } else {
                
                    double acceptP = Math.min(1, Math.exp(-Params.TEMPERATURE * ((double) optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) / circuit.cost(Params.OPTIMIZATION_OBJECTIVE))));
                    if (rand.nextDouble() <= acceptP) {
                        result = optimizedDAG;
                    } else {
                        result = circuit;
                    }
        
                }
            }
        }

        
        for (int i = 0; i < symbRulesToUse.size(); i++){
            //System.out.println("Current RULE: " + i + "/" + Integer.min(symb_rule_limit, symbRules.size()));
            int index = i;
            System.out.println("Current SYMB RULE: " + symbRulesToUse.get(index).getLHS() + " -> " + symbRulesToUse.get(index).getRHS());
            
            CircuitDAG optimizedDAG = optimizer.symbolicMatchBeforeAfter(circuit, symbRulesToUse.get(index).getLHS(), symbRulesToUse.get(index).getRHS(), minSymb, maxSymb, symbRulesToUse.get(index).getConstraint(), null);
            if(optimizedDAG != null) {
                List<String> rulesApplied = new ArrayList<>(circuit.getRulesApplied());
                //rulesApplied.add(symbRulesToUse.get(index).getLHS() + "|" + symbRulesToUse.get(index).getRHS());
                optimizedDAG.setRulesApplied(rulesApplied);
                //symbRulesUsed.put(symbRulesToUse.get(index).getLHS() + "|" + symbRulesToUse.get(index).getRHS(), symbRulesUsed.getOrDefault(symbRulesToUse.get(index), 0) + 1);
                //System.out.println("Optimized DAG: " + optimizedDAG.toQASM());
                
                if(optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) <= circuit.cost(Params.OPTIMIZATION_OBJECTIVE)) {
                    //System.out.println("Symb Rule Reduced: " + (c.cost(Params.OPTIMIZATION_OBJECTIVE) - optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE)));
                    //System.out.println("From " + c.cost(Params.OPTIMIZATION_OBJECTIVE) + " to " + optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE));
                    result = optimizedDAG;
                } else {
                    double acceptP = Math.min(1, Math.exp(-Params.TEMPERATURE * ((double) optimizedDAG.cost(Params.OPTIMIZATION_OBJECTIVE) / circuit.cost(Params.OPTIMIZATION_OBJECTIVE))));
                    if (rand.nextDouble() <= acceptP) {
                        result = optimizedDAG;
                    } else {
                        result = circuit;
                    }
                }
            }
        } 
    }
}
