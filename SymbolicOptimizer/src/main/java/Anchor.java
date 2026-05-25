import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
            symblhs = symblhs.replaceAll("q\\d+", "(Q \"$0\")");
            symblhs = symblhs.replaceAll("theta1|theta2|theta3", "(Symbol \"$0\")");
            symbrhs = symbrhs.replaceAll("\\bc\\b", "(Nil)");
            symbrhs = symbrhs.replaceAll("q\\d+", "(Q \"$0\")");
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
            boolean matched = false;
            for(Rule rule: rules) {
                EggGen.Circuit ruleLhs = rule.getLHS();
                EggGen.Circuit ruleRhs = rule.getRHS();
                System.out.println("Concrete rule: " + ruleLhs.toQASM() + " -> " + ruleRhs.toQASM());
                if(ruleLhs.gates.size() > ruleRhs.gates.size()) {
                    Map<String, Expr> symbolmap = new HashMap<>();
                    Map<String, String> qubitmap = new HashMap<>();
                    if(matchPrefix(rhsAfterGates, ruleLhs.gates, symbolmap, qubitmap)) {
                        matched = true;
                        System.out.println("Matched prefix, qubitmap: " + qubitmap);
                        List<EggGen.Gate> newGates = dropfront(ruleLhs.gates, rhsAfterGates.size());
                        EggGen.Circuit newGatesCircuit = new EggGen.Circuit(newGates);
                        EggGen.Circuit canonewGates = EggGen.canonicalizeCircuit(newGatesCircuit, qubitmap, true);
                        canonewGates = canonewGates.substitute(symbolmap);
                        List<EggGen.Gate> newlhs = new ArrayList<>(lhsCircuit.gates);
                        newlhs.addAll(canonewGates.gates);
                        EggGen.Circuit newLhs = new EggGen.Circuit(newlhs);
                        String newlhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newLhs, "c");


                        List<EggGen.Gate> ruleLhsGates = new ArrayList<>(ruleLhs.gates);
                        EggGen.Circuit ruleLhsCircuit = new EggGen.Circuit(ruleLhsGates);
                        EggGen.Circuit canoruleLhs = EggGen.canonicalizeCircuit(ruleLhsCircuit, qubitmap, true);
                        canoruleLhs = canoruleLhs.substitute(symbolmap);
                        List<EggGen.Gate> newrhs = new ArrayList<>(rhsBeforeGates);
                        newrhs.add(new EggGen.SYMB(maxQubit));
                        newrhs.addAll(canoruleLhs.gates);
                        EggGen.Circuit newRhs = new EggGen.Circuit(newrhs);
                        String newrhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newRhs, "c");
                        MatrixConstrainedRule newRule = new MatrixConstrainedRule(newlhsStr, newrhsStr, r.getConstraint(), r.getType());
                        System.out.println("Anchored rule: " + newlhsStr + " -> " + newrhsStr);
                        anchored_rules.add(newRule);
                        queue.add(newRule);
                    } else if(matchSuffix(rhsBeforeGates, ruleLhs.gates, symbolmap, qubitmap)) {
                        matched = true;
                        System.out.println("Matched suffix, qubitmap: " + qubitmap);
                        List<EggGen.Gate> newGates = dropback(ruleLhs.gates, rhsBeforeGates.size());
                        EggGen.Circuit newGatesCircuit = new EggGen.Circuit(newGates);
                        EggGen.Circuit canonewGates = EggGen.canonicalizeCircuit(newGatesCircuit, qubitmap, true);
                        System.out.println("Canonicalized new gates: " + canonewGates.toQASM());
                        System.out.println(qubitmap);
                        System.out.println(symbolmap);
                        canonewGates = canonewGates.substitute(symbolmap);
                        System.out.println("New gates after canonicalization and substitution: " + canonewGates.toQASM());
                        List<EggGen.Gate> newlhs = new ArrayList<>(canonewGates.gates);
                        newlhs.addAll(lhsCircuit.gates);
                        EggGen.Circuit newLhs = new EggGen.Circuit(newlhs);
                        String newlhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newLhs, "c");


                        List<EggGen.Gate> ruleLhsGates = new ArrayList<>(ruleLhs.gates);
                        EggGen.Circuit ruleLhsCircuit = new EggGen.Circuit(ruleLhsGates);
                        EggGen.Circuit canoruleLhs = EggGen.canonicalizeCircuit(ruleLhsCircuit, qubitmap, true);
                        canoruleLhs = canoruleLhs.substitute(symbolmap);
                        List<EggGen.Gate> newrhs = new ArrayList<>(canoruleLhs.gates);
                        newrhs.add(new EggGen.SYMB(maxQubit));
                        newrhs.addAll(rhsAfterGates);
                        EggGen.Circuit newRhs = new EggGen.Circuit(newrhs);
                        String newrhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newRhs, "c");
                        MatrixConstrainedRule newRule = new MatrixConstrainedRule(newlhsStr, newrhsStr, r.getConstraint(), r.getType());
                        System.out.println("Anchored rule: " + newlhsStr + " -> " + newrhsStr);
                        anchored_rules.add(newRule);
                        queue.add(newRule);
                    }
                }
            }

            if(!matched) {
                System.out.println("No anchor form: " + symblhs + " -> " + symbrhs);
                anchored_rules.add(r);
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
                    if(qubitMap.containsKey(circuitqubit)) {
                        if(!qubitMap.get(circuitqubit).equals(prefixqubit)) {
                            return false;
                        }
                    } else {
                        // The qubit map must be injective: two distinct circuit
                        // qubits must not collapse onto the same pattern qubit,
                        // which would turn e.g. cx q0,q1 into a degenerate
                        // cx q1,q1 after canonicalization.
                        if(qubitMap.containsValue(prefixqubit)) {
                            return false;
                        }
                        qubitMap.put(circuitqubit, prefixqubit);
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
                if(qubitMap.containsKey(circuitqubit)) {
                    if(!qubitMap.get(circuitqubit).equals(suffixqubit)) {
                        return false;
                    }
                } else {
                    // The qubit map must be injective: two distinct circuit
                    // qubits must not collapse onto the same pattern qubit,
                    // which would turn e.g. cx q0,q1 into a degenerate
                    // cx q1,q1 after canonicalization.
                    if(qubitMap.containsValue(suffixqubit)) {
                        return false;
                    }
                    qubitMap.put(circuitqubit, suffixqubit);
                }
            }
        }
        return true;
    }


    // True when an angle expression is free of symbolic parameters (theta*),
    // i.e. it only involves Real literals, pi, and arithmetic on them, so it
    // can be evaluated to a concrete number.
    private static boolean isNumericAngle(Expr e) {
        if (e instanceof Real) {
            return true;
        }
        if (e instanceof Symbol) {
            return ((Symbol) e).getSymbol().equals("pi");
        }
        if (e instanceof BinOp) {
            return isNumericAngle(((BinOp) e).getE1()) && isNumericAngle(((BinOp) e).getE2());
        }
        if (e instanceof UnOp) {
            return isNumericAngle(((UnOp) e).getE());
        }
        return false;
    }

    private static boolean sameAngle(Expr angle1, Expr angle2) {
        // Structural equality first: handles symbolic angles (theta1,
        // theta1+theta2, ...) which Optimizer.eval cannot evaluate.
        if (angle1.toString().equals(angle2.toString())) {
            return true;
        }
        // Numeric comparison (mod 4*pi) only when both sides are
        // parameter-free; otherwise we cannot prove equality, so be
        // conservative and report not-equal (a missed anchor, not an
        // unsound one).
        if (isNumericAngle(angle1) && isNumericAngle(angle2)) {
            return (Optimizer.eval(angle1) % (4 * Math.PI)) == (Optimizer.eval(angle2) % (4 * Math.PI));
        }
        return false;
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
        }
        // Symmetric case: pattern is concrete but circ is a "theta*" symbol.
        // Bind the symbolic rule's theta to whatever concrete expression the
        // anchor-input concrete rule has at this position. Without this, e.g.
        // matching circuit RXX(pi/2) against symbolic RXX(theta1) fails.
        if(circ instanceof Symbol) {
            String key = circ.toString();
            if(key.contains("theta")) {
                if(angleMap.containsKey(key)) {
                    return sameAngle(angleMap.get(key), pattern);
                } else {
                    angleMap.put(key, pattern);
                    return true;
                }
            }
        }
        if(pattern instanceof BinOp) {
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


    /**
     * A two-qubit CX is degenerate when its control and target are the same
     * qubit (e.g. "cx q1, q1"). Anchoring can produce such malformed rules;
     * they are not valid gates and must be dropped from the output. Matches
     * both "(CX q0 q1)" and "(CX (Q \"q0\") (Q \"q1\"))" serializations.
     */
    private static final Pattern DEGENERATE_CX = Pattern.compile(
            "\\(CX\\s+(?:\\(Q\\s+\"([^\"]+)\"\\)|(q\\d+))\\s+(?:\\(Q\\s+\"([^\"]+)\"\\)|(q\\d+))\\)");

    static boolean hasDegenerateCx(String rule) {
        Matcher m = DEGENERATE_CX.matcher(rule);
        while (m.find()) {
            String control = m.group(1) != null ? m.group(1) : m.group(2);
            String target = m.group(3) != null ? m.group(3) : m.group(4);
            if (control != null && control.equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option rulesO = new Option("r", "rule", true, "ruleset file path");
        rulesO.setRequired(true);
        options.addOption(rulesO);



        Option symbRulesO = new Option("sr", "symbrule", true, "symb ruleset file path");
        symbRulesO.setRequired(false);
        options.addOption(symbRulesO);

        Option outputO = new Option("o", "output", true, "output file path");
        outputO.setRequired(false);
        options.addOption(outputO);
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Optimizer", options);
            System.exit(1);
            return;
        }
        List<MatrixConstrainedRule> symbRules = new ArrayList<>();
        String symrulesFile = cmd.getOptionValue("symbrule");
        try (BufferedReader br = new BufferedReader(new FileReader(symrulesFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
            String[] comp = line.split("\\|");
            String lhs = comp[0];
            String rhs = comp[1];
            String type = comp[2];
            String matrix = comp[3];
            
            Pattern matrixP = Pattern.compile("\\[(.*)\\]");
            Matcher matcher = matrixP.matcher(matrix);
            matcher.matches();
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            List<SymbolicSolve.SparseMatrix> matrixList = new ArrayList<>();
            //TODO: Construct Matrices
            for(String m: matrices) {
                    List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                    if(m.startsWith("Matrix(")) {
                        String content = m.substring("Matrix(".length(), m.length() - 1);
                        //System.out.println("content: " + content);
                        String[] entries = content.split(";");
                        int rows = Integer.valueOf(entries[0]);
                        int cols = Integer.valueOf(entries[1]);
                        for(int i = 2; i < entries.length; i++) {
                            String entry = entries[i].substring(1, entries[i].length() - 1);
                            String[] elems = entry.split(", ");
                            int row = Integer.valueOf(elems[0].trim());
                            int col = Integer.valueOf(elems[1].trim());
                            SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                            sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                        }
                        matrixList.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                    }
            }
            symbRules.add(new MatrixConstrainedRule(lhs, rhs, matrixList, type));
            }
        } catch (Exception e) {
            System.out.println("Error reading symb ruleset file: " + e.getMessage());
            System.exit(1);
        }


        List<Rule> rules = new ArrayList<>();
        String rulesFile = cmd.getOptionValue("rule");
        try (BufferedReader br = new BufferedReader(new FileReader(rulesFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                rules.add(QASMAstBuilder.parseRule(line));
            }
        } catch (Exception e) {
            System.out.println("Error reading ruleset file: " + e.getMessage());
            System.exit(1);
        }

        List<MatrixConstrainedRule> anchoredRules = anchor(rules, symbRules);

        String outputfile = cmd.getOptionValue("output");
        try {
            FileWriter fw = new FileWriter(outputfile, StandardCharsets.UTF_8, false);
            PrintWriter pw = new PrintWriter(fw);
            int written = 0;
            int skippedDegenerate = 0;
            for(MatrixConstrainedRule rule: anchoredRules) {
                String ruleString = rule.toString();
                if(hasDegenerateCx(ruleString)) {
                    skippedDegenerate++;
                    continue;
                }
                pw.println(ruleString);
                written++;
            }
            pw.close();
            System.out.println("Anchored rules written: " + written
                    + " (dropped " + skippedDegenerate + " degenerate cx rules)");
        } catch (Exception e) {
            System.out.println("Error writing output file: " + e.getMessage());
            System.exit(1);
        }
    }
}
