import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import ast.BinOp;
import ast.Expr;
import ast.Real;
import ast.Symbol;
import ast.UnOp;




public class Anchor {
    public static List<MatrixConstrainedRule> anchor(List<Rule> rules, List<MatrixConstrainedRule> symbRules) {
        List<MatrixConstrainedRule> anchored_rules = new ArrayList<>();
        
        Queue<MatrixConstrainedRule> queue = new LinkedList<>(symbRules);

        while(!queue.isEmpty()) {
            MatrixConstrainedRule r = queue.poll();
            String symblhs = r.getLHS();
            String symbrhs = r.getRHS();
            System.out.println("Trying to anchor rule0: " + symblhs + " -> " + symbrhs);
            symblhs = symblhs.replaceAll("\\bc\\b", "(Nil)");
            //symblhs = symblhs.replaceAll("q\\d+", "(Q \"$0\")");
            symblhs = symblhs.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
            symbrhs = symbrhs.replaceAll("\\bc\\b", "(Nil)");
            //symbrhs = symbrhs.replaceAll("q\\d+", "(Q \"$0\")");
            symbrhs = symbrhs.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");

            System.out.println("Trying to anchor rule: " + symblhs + " -> " + symbrhs);

            //four rules to anchor the symb rules
            EggGen.Circuit lhsCircuit = EggAstBuilder.parseCircuit(symblhs);
            EggGen.Circuit rhsCircuit = EggAstBuilder.parseCircuit(symbrhs);

            int lsymbindex = 0;
            int rsymbindex = 0;
            int maxQubit = 0;
            int i = 0;
            for(EggGen.Gate gate : lhsCircuit.gates) {
                if(gate instanceof EggGen.SYMB) {
                    lsymbindex = i;
                    maxQubit = ((EggGen.SYMB) gate).maxQubits;
                }
                i++;
            }

            i = 0;
            for(EggGen.Gate gate : rhsCircuit.gates) {
                if(gate instanceof EggGen.SYMB) {
                    rsymbindex = i;
                    maxQubit = ((EggGen.SYMB) gate).maxQubits;
                }
                i++;
            }
            List<EggGen.Gate> lhsBeforeGates = new ArrayList<>(lhsCircuit.gates.subList(0, lsymbindex));
            List<EggGen.Gate> lhsAfterGates = new ArrayList<>(lhsCircuit.gates.subList(lsymbindex + 1, lhsCircuit.gates.size()));
            List<EggGen.Gate> rhsBeforeGates = new ArrayList<>(rhsCircuit.gates.subList(0, rsymbindex));
            List<EggGen.Gate> rhsAfterGates = new ArrayList<>(rhsCircuit.gates.subList(rsymbindex + 1, rhsCircuit.gates.size()));

            for(Rule rule: rules) {
                EggGen.Circuit ruleLhs = rule.getLHS();
                EggGen.Circuit ruleRhs = rule.getRHS();
                System.out.println("Concrete rule: " + ruleLhs.toQASM() + " -> " + ruleRhs.toQASM());
                if(ruleLhs.gates.size() > ruleRhs.gates.size()) {
                    Map<String, Expr> symbolmap = new HashMap<>();
                    Map<String, String> qubitmap = new HashMap<>();
                    if(matchPrefix(rhsAfterGates, ruleLhs.gates, symbolmap, qubitmap)) {
                        List<EggGen.Gate> newGates = dropfront(ruleLhs.gates, rhsAfterGates.size());
                        EggGen.Circuit newGatesCircuit = new EggGen.Circuit(newGates);
                        EggGen.Circuit canonewGates = EggGen.canonicalizeCircuit(newGatesCircuit, qubitmap, true);
                        canonewGates.instantiate(symbolmap);
                        List<EggGen.Gate> newlhs = new ArrayList<>(lhsCircuit.gates);
                        newlhs.addAll(canonewGates.gates);
                        EggGen.Circuit newLhs = new EggGen.Circuit(newlhs);
                        String newlhsStr = EggGen.replaceNilWithVar(newLhs, "c");


                        List<EggGen.Gate> ruleLhsGates = new ArrayList<>(ruleLhs.gates);
                        EggGen.Circuit ruleLhsCircuit = new EggGen.Circuit(ruleLhsGates);
                        EggGen.Circuit canoruleLhs = EggGen.canonicalizeCircuit(ruleLhsCircuit, qubitmap, true);
                        canoruleLhs.instantiate(symbolmap);
                        List<EggGen.Gate> newrhs = new ArrayList<>(rhsBeforeGates);
                        newrhs.add(new EggGen.SYMB(maxQubit));
                        newrhs.addAll(canoruleLhs.gates);
                        EggGen.Circuit newRhs = new EggGen.Circuit(newrhs);
                        String newrhsStr = EggGen.replaceNilWithVar(newRhs, "c");
                        MatrixConstrainedRule newRule = new MatrixConstrainedRule(newlhsStr, newrhsStr, r.getConstraint(), r.getType());
                        System.out.println("Anchored rule: " + newlhsStr + " -> " + newrhsStr);
                        anchored_rules.add(newRule);
                        queue.add(newRule);
                    } else if(matchSuffix(rhsBeforeGates, ruleLhs.gates, symbolmap, qubitmap)) {
                        List<EggGen.Gate> newGates = dropback(ruleLhs.gates, rhsBeforeGates.size());
                        EggGen.Circuit newGatesCircuit = new EggGen.Circuit(newGates);
                        EggGen.Circuit canonewGates = EggGen.canonicalizeCircuit(newGatesCircuit, qubitmap, true);
                        canonewGates.instantiate(symbolmap);

                        List<EggGen.Gate> newlhs = new ArrayList<>(canonewGates.gates);
                        newlhs.addAll(lhsCircuit.gates);
                        EggGen.Circuit newLhs = new EggGen.Circuit(newlhs);
                        String newlhsStr = EggGen.replaceNilWithVar(newLhs, "c");


                        List<EggGen.Gate> newrhs = new ArrayList<>(ruleLhs.gates);
                        EggGen.Circuit ruleLhsCircuit = new EggGen.Circuit(newrhs);
                        EggGen.Circuit canoruleLhs = EggGen.canonicalizeCircuit(ruleLhsCircuit, qubitmap, true);
                        canoruleLhs.instantiate(symbolmap);
                        newrhs.add(new EggGen.SYMB(maxQubit));
                        newrhs.addAll(rhsAfterGates);
                        EggGen.Circuit newRhs = new EggGen.Circuit(newrhs);
                        String newrhsStr = EggGen.replaceNilWithVar(newRhs, "c");
                        MatrixConstrainedRule newRule = new MatrixConstrainedRule(newlhsStr, newrhsStr, r.getConstraint(), r.getType());
                        System.out.println("Anchored rule: " + newlhsStr + " -> " + newrhsStr);
                        anchored_rules.add(newRule);
                        queue.add(newRule);
                    }
                }
            }
        }

        return anchored_rules;
    }


    public static List<EggGen.Gate> dropfront(List<EggGen.Gate> c, int n) {
        return new ArrayList<>(c.subList(n, c.size()));
    }


    public static List<EggGen.Gate> dropback(List<EggGen.Gate> c, int n) {
        return new ArrayList<>(c.subList(0, c.size() - n));
    }

    // qubitMap will be modified no matter if the match is successful or not, so make sure to pass in a copy if you don't want it to be modified
    public static boolean matchPrefix(List<EggGen.Gate> prefix, List<EggGen.Gate> circuit, Map<String, Expr> angleMap, Map<String, String> qubitMap) {
            if(prefix.isEmpty()) {
                return false;
            }

            if(prefix.size() >= circuit.size()) {
                return false;
            }
            for(int i = 0; i < prefix.size(); i++) {
                EggGen.Gate prefixgate = prefix.get(i);
                EggGen.Gate circuitgate = circuit.get(i);
                if(!prefixgate.gateName().equals(circuitgate.gateName())) {
                    return false;
                }

                List<Expr> prefixpara = prefixgate.getParameters();
                List<Expr> circuitpara = circuitgate.getParameters();
                if(prefixpara.size() != circuitpara.size()) {
                    return false;
                }

                for(int j = 0; j < prefixpara.size(); j++) {
                    if(!matchAngle(circuitpara.get(j), prefixpara.get(j), angleMap)) {
                        return false;
                    }
                }

                List<String> prefixqubits = prefixgate.getQubits();
                List<String> circuitqubits = circuitgate.getQubits();
                for(int j = 0; j < prefixqubits.size(); j++) {
                    String prefixqubit = prefixqubits.get(j);
                    String circuitqubit = circuitqubits.get(j);
                    if(qubitMap.containsKey(prefixqubit)) {
                        if(!qubitMap.get(prefixqubit).equals(circuitqubit)) {
                            return false;
                        }
                    } else {
                        qubitMap.put(prefixqubit, circuitqubit);
                    }
                }
            }
            return true;
    }


    public static boolean matchSuffix(List<EggGen.Gate> suffix, List<EggGen.Gate> circuit, Map<String, Expr> angleMap, Map<String, String> qubitMap) {
        if(suffix.isEmpty()) {
            return false;
        }
        if(suffix.size() >= circuit.size()) {
            return false;
        }
        for(int i = 0; i < suffix.size(); i++) {
            EggGen.Gate suffixgate = suffix.get(suffix.size() - 1 - i);
            EggGen.Gate circuitgate = circuit.get(circuit.size() - 1 - i);
            if(!suffixgate.gateName().equals(circuitgate.gateName())) {
                return false;
            }

            List<Expr> suffixpara = suffixgate.getParameters();
            List<Expr> circuitpara = circuitgate.getParameters();
            if(suffixpara.size() != circuitpara.size()) {
                return false;
            }

            for(int j = 0; j < suffixpara.size(); j++) {
                if(!matchAngle(circuitpara.get(j), suffixpara.get(j), angleMap)) {
                    return false;
                }
            }

            List<String> suffixqubits = suffixgate.getQubits();
            List<String> circuitqubits = circuitgate.getQubits();
            for(int j = 0; j < suffixqubits.size(); j++) {
                String suffixqubit = suffixqubits.get(j);
                String circuitqubit = circuitqubits.get(j);
                if(qubitMap.containsKey(suffixqubit)) {
                    if(!qubitMap.get(suffixqubit).equals(circuitqubit)) {
                        return false;
                    }
                } else {
                    qubitMap.put(suffixqubit, circuitqubit);
                }
            }
        }
        return true;
    }


    private static boolean sameAngle(Expr angle1, Expr angle2) {
        return (Optimizer.eval(angle1) % (4 * Math.PI)) == (Optimizer.eval(angle2) % (4 * Math.PI));
    }


    private static boolean matchAngle(Expr pattern, Expr circ, Map<String, Expr> angleMap) {
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
}
