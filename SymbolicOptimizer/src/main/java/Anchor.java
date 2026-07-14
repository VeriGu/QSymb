import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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
        // Always keep every original canonical rule. Anchoring must AUGMENT, not
        // replace: the bare canonical (e.g. RZ commuting across SYMB) is the most
        // broadly-applicable primitive and was previously dropped whenever it
        // anchored successfully, leaving only context-specific composites.
        anchored_rules.addAll(symbRules);

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
                    // Bindings of the SYMBOLIC rule's thetas to the concrete
                    // rule's angles (matchAngle's symmetric case). These must
                    // be substituted into the canonical's own gates AND its
                    // basis matrices, otherwise the anchored rule keeps theta
                    // free while only being valid at the bound angle.
                    Map<String, Expr> symbBindings = new HashMap<>();
                    if(matchPrefix(rhsAfterGates, ruleLhs.gates, symbolmap, qubitmap, symbBindings)) {
                        matched = true;
                        System.out.println("Matched prefix, qubitmap: " + qubitmap + ", symbBindings: " + symbBindings);
                        List<EggGen.Gate> newGates = dropfront(ruleLhs.gates, rhsAfterGates.size());
                        EggGen.Circuit newGatesCircuit = new EggGen.Circuit(newGates);
                        EggGen.Circuit canonewGates = EggGen.canonicalizeCircuit(newGatesCircuit, qubitmap, true);
                        canonewGates = canonewGates.substitute(symbolmap).substitute(symbBindings);
                        List<EggGen.Gate> newlhs = new ArrayList<>(new EggGen.Circuit(new ArrayList<>(lhsCircuit.gates)).substitute(symbBindings).gates);
                        newlhs.addAll(canonewGates.gates);
                        EggGen.Circuit newLhs = new EggGen.Circuit(newlhs);
                        String newlhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newLhs, "c");


                        List<EggGen.Gate> ruleLhsGates = new ArrayList<>(ruleLhs.gates);
                        EggGen.Circuit ruleLhsCircuit = new EggGen.Circuit(ruleLhsGates);
                        EggGen.Circuit canoruleLhs = EggGen.canonicalizeCircuit(ruleLhsCircuit, qubitmap, true);
                        canoruleLhs = canoruleLhs.substitute(symbolmap).substitute(symbBindings);
                        List<EggGen.Gate> newrhs = new ArrayList<>(new EggGen.Circuit(new ArrayList<>(rhsBeforeGates)).substitute(symbBindings).gates);
                        newrhs.add(new EggGen.SYMB(maxQubit));
                        newrhs.addAll(canoruleLhs.gates);
                        EggGen.Circuit newRhs = new EggGen.Circuit(newrhs);
                        // Foreign free angles introduced by the anchored concrete
                        // rule (e.g. merge's e2, which stays free) are not among
                        // the theta1/2/3 names recognized downstream. Renumber
                        // them — consistently across LHS and RHS — into unused
                        // theta slots. Canonical symbolic thetas are preserved
                        // (they are tied to the basis matrices).
                        Map<String, Expr> renumber = foreignRenumberMap(newLhs, newRhs);
                        newlhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newLhs.substitute(renumber), "c");
                        String newrhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newRhs.substitute(renumber), "c");
                        MatrixConstrainedRule newRule = new MatrixConstrainedRule(newlhsStr, newrhsStr, substituteBasis(r.getConstraint(), symbBindings), r.getType());
                        System.out.println("Anchored rule: " + newlhsStr + " -> " + newrhsStr);
                        anchored_rules.add(newRule);
                        queue.add(newRule);
                    } else if(matchSuffix(rhsBeforeGates, ruleLhs.gates, symbolmap, qubitmap, symbBindings)) {
                        matched = true;
                        System.out.println("Matched suffix, qubitmap: " + qubitmap + ", symbBindings: " + symbBindings);
                        List<EggGen.Gate> newGates = dropback(ruleLhs.gates, rhsBeforeGates.size());
                        EggGen.Circuit newGatesCircuit = new EggGen.Circuit(newGates);
                        EggGen.Circuit canonewGates = EggGen.canonicalizeCircuit(newGatesCircuit, qubitmap, true);
                        System.out.println("Canonicalized new gates: " + canonewGates.toQASM());
                        System.out.println(qubitmap);
                        System.out.println(symbolmap);
                        canonewGates = canonewGates.substitute(symbolmap).substitute(symbBindings);
                        System.out.println("New gates after canonicalization and substitution: " + canonewGates.toQASM());
                        List<EggGen.Gate> newlhs = new ArrayList<>(canonewGates.gates);
                        newlhs.addAll(new EggGen.Circuit(new ArrayList<>(lhsCircuit.gates)).substitute(symbBindings).gates);
                        EggGen.Circuit newLhs = new EggGen.Circuit(newlhs);
                        String newlhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newLhs, "c");


                        List<EggGen.Gate> ruleLhsGates = new ArrayList<>(ruleLhs.gates);
                        EggGen.Circuit ruleLhsCircuit = new EggGen.Circuit(ruleLhsGates);
                        EggGen.Circuit canoruleLhs = EggGen.canonicalizeCircuit(ruleLhsCircuit, qubitmap, true);
                        canoruleLhs = canoruleLhs.substitute(symbolmap).substitute(symbBindings);
                        List<EggGen.Gate> newrhs = new ArrayList<>(canoruleLhs.gates);
                        newrhs.add(new EggGen.SYMB(maxQubit));
                        newrhs.addAll(new EggGen.Circuit(new ArrayList<>(rhsAfterGates)).substitute(symbBindings).gates);
                        EggGen.Circuit newRhs = new EggGen.Circuit(newrhs);
                        // Foreign free angles introduced by the anchored concrete
                        // rule (e.g. merge's e2, which stays free) are not among
                        // the theta1/2/3 names recognized downstream. Renumber
                        // them — consistently across LHS and RHS — into unused
                        // theta slots. Canonical symbolic thetas are preserved
                        // (they are tied to the basis matrices).
                        Map<String, Expr> renumber = foreignRenumberMap(newLhs, newRhs);
                        newlhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newLhs.substitute(renumber), "c");
                        String newrhsStr = EggGen.circuitToGeneralizedOnlyRemoveQ(newRhs.substitute(renumber), "c");
                        MatrixConstrainedRule newRule = new MatrixConstrainedRule(newlhsStr, newrhsStr, substituteBasis(r.getConstraint(), symbBindings), r.getType());
                        System.out.println("Anchored rule: " + newlhsStr + " -> " + newrhsStr);
                        anchored_rules.add(newRule);
                        queue.add(newRule);
                    }
                }
            }

            if(!matched) {
                System.out.println("No anchor form: " + symblhs + " -> " + symbrhs);
            }
            // NOTE: original canonical rules are seeded into anchored_rules up
            // front; anchored composites are added at their creation site above.
            // Do not re-add here (that dropped bare canonicals and duplicated
            // composites).
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
        return matchPrefix(prefix, circuit, angleMap, qubitMap, new HashMap<>());
    }

    public static boolean matchPrefix(List<EggGen.Gate> prefix, List<EggGen.Gate> circuit, Map<String, Expr> angleMap, Map<String, String> qubitMap, Map<String, Expr> symbBindings) {
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
                    if(!matchAngle(circuitpara.get(j), prefixpara.get(j), angleMap, symbBindings)) {
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
        return matchSuffix(suffix, circuit, angleMap, qubitMap, new HashMap<>());
    }

    public static boolean matchSuffix(List<EggGen.Gate> suffix, List<EggGen.Gate> circuit, Map<String, Expr> angleMap, Map<String, String> qubitMap, Map<String, Expr> symbBindings) {
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
                if(!matchAngle(circuitpara.get(j), suffixpara.get(j), angleMap, symbBindings)) {
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


    private static boolean matchAngle(Expr pattern, Expr circ, Map<String, Expr> angleMap, Map<String, Expr> symbBindings) {
        // System.out.println("Pattern:" + pattern);
        // System.out.println("Circ:" + circ);
        if(pattern instanceof Symbol) {
            String key = pattern.toString();
            // A bindable free pattern symbol: either a canonical theta* angle,
            // or a hand-written merge-rule free angle (e1, e2, ...). Without the
            // e\d+ case the RZ-merge rule's e1/e2 require exact equality and can
            // never bind against the symbolic rule's concrete RZ angle, so the
            // RZ;SYMB;RZ anchored rule is never emitted.
            if(key.contains("theta") || key.matches("e\\d+")) {
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
        // The binding is recorded in symbBindings because it specializes the
        // SYMBOLIC rule: the caller must substitute it into the canonical's
        // gates and basis matrices, or the anchored rule is unsound (valid
        // only at the bound angle but matched with theta free).
        if(circ instanceof Symbol) {
            String key = circ.toString();
            if(key.contains("theta")) {
                if(angleMap.containsKey(key)) {
                    return sameAngle(angleMap.get(key), pattern);
                } else {
                    angleMap.put(key, pattern);
                    symbBindings.put(key, pattern);
                    return true;
                }
            }
        }
        if(pattern instanceof BinOp) {
            if(circ instanceof BinOp) {
                if(((BinOp) pattern).getOp().equals(((BinOp) circ).getOp())) {
                    return matchAngle(((BinOp) pattern).getE1(), ((BinOp) circ).getE1(), angleMap, symbBindings) && matchAngle(((BinOp) pattern).getE2(), ((BinOp) circ).getE2(), angleMap, symbBindings);
                } else {
                    return false;
                }
            }
        } else if(pattern instanceof UnOp) {
            if(circ instanceof UnOp) {
                if(((UnOp) pattern).getOp().equals(((UnOp) circ).getOp())) {
                    return matchAngle(((UnOp) pattern).getE(), ((UnOp) circ).getE(), angleMap, symbBindings);
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
     * Render an angle Expr as an exact sympy-parsable string for basis-matrix
     * substitution. Multiples of pi/4 are emitted symbolically ("3*pi/4") so
     * semantics.py keeps its exact algebraic-field path; anything else falls
     * back to the decimal value. Non-numeric (theta-bearing) exprs are
     * rendered as-is (theta renames).
     */
    static String angleToSympy(Expr e) {
        if (!isNumericAngle(e)) {
            return e.toString();
        }
        double v = Optimizer.eval(e);
        double k = v / (Math.PI / 4);
        long kk = Math.round(k);
        if (Math.abs(k - kk) < 1e-9) {
            if (kk == 0) return "0";
            return kk + "*pi/4";
        }
        return Double.toString(v);
    }

    /**
     * Substitute bound thetas into the basis matrices. The basis was solved
     * with the canonical rule's thetas free; once anchoring pins a theta to a
     * concrete angle, the stored basis must be specialized at that angle.
     * (Each basis element satisfies the intertwiner equation identically in
     * theta, so evaluating it at the bound angle stays a valid solution.)
     */
    static List<SymbolicSolve.SparseMatrix> substituteBasis(List<SymbolicSolve.SparseMatrix> basis, Map<String, Expr> symbBindings) {
        if (symbBindings.isEmpty() || basis == null) {
            return basis;
        }
        Map<String, String> repl = new HashMap<>();
        for (Map.Entry<String, Expr> en : symbBindings.entrySet()) {
            repl.put(en.getKey(), angleToSympy(en.getValue()));
        }
        List<SymbolicSolve.SparseMatrix> out = new ArrayList<>();
        for (SymbolicSolve.SparseMatrix m : basis) {
            List<SymbolicSolve.SparseMatrix.MatrixEntry> ents = new ArrayList<>();
            for (SymbolicSolve.SparseMatrix.MatrixEntry en : m.entries) {
                String v = en.value.toString();
                for (Map.Entry<String, String> r : repl.entrySet()) {
                    v = v.replaceAll("\\b" + r.getKey() + "\\b",
                            Matcher.quoteReplacement("(" + r.getValue() + ")"));
                }
                ents.add(new SymbolicSolve.SparseMatrix.MatrixEntry(en.row, en.col, new SymbolicSolve.Complex(v)));
            }
            out.add(new SymbolicSolve.SparseMatrix(m.rows, m.cols, ents));
        }
        return out;
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

    /**
     * Build a substitution that renumbers "foreign" free angles — those an
     * anchored concrete rule injects that are NOT among the theta1/2/3 names
     * recognized downstream (EggGen.replaceSymbolWithVar, the SymbolicThread
     * regex). The merge rule's second RZ angle (e2) is the motivating case: it
     * stays free after anchoring and must become a real theta slot or it is
     * silently dropped / unrecognized. Canonical symbolic thetas already
     * present are KEPT untouched — they are bound to the rule's basis matrices,
     * so renaming them would desync the constraint. Foreign symbols are mapped,
     * in first-occurrence order (LHS then RHS), into the theta slots not already
     * taken by a kept theta. Throws if there are more foreign angles than free
     * slots (max 3 distinct thetas are recognized downstream).
     */
    private static Map<String, Expr> foreignRenumberMap(EggGen.Circuit lhs, EggGen.Circuit rhs) {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        collectFreeSymbols(lhs, all);
        collectFreeSymbols(rhs, all);
        Set<String> kept = new HashSet<>();
        List<String> foreign = new ArrayList<>();
        for (String s : all) {
            if (s.matches("theta[123]?")) {
                kept.add(s);
            } else {
                foreign.add(s);
            }
        }
        if (foreign.isEmpty()) {
            return new HashMap<>();
        }
        List<String> slots = new ArrayList<>();
        for (String t : new String[]{"theta1", "theta2", "theta3"}) {
            if (!kept.contains(t)) {
                slots.add(t);
            }
        }
        if (foreign.size() > slots.size()) {
            throw new IllegalStateException("Anchored rule has " + foreign.size()
                    + " foreign free angles " + foreign + " but only " + slots.size()
                    + " theta slot(s) free (kept: " + kept + ")");
        }
        Map<String, Expr> map = new HashMap<>();
        for (int i = 0; i < foreign.size(); i++) {
            map.put(foreign.get(i), new Symbol(slots.get(i)));
        }
        return map;
    }

    private static void collectFreeSymbols(EggGen.Circuit c, LinkedHashSet<String> out) {
        for (EggGen.Gate g : c.gates) {
            List<Expr> params = g.getParameters();
            if (params == null) {
                continue;
            }
            for (Expr e : params) {
                collectSyms(e, out);
            }
        }
    }

    private static void collectSyms(Expr e, LinkedHashSet<String> out) {
        if (e == null) {
            return;
        }
        if (e instanceof Symbol) {
            String n = ((Symbol) e).getSymbol();
            if (!n.equals("pi")) {
                out.add(n);
            }
        } else if (e instanceof BinOp) {
            collectSyms(((BinOp) e).getE1(), out);
            collectSyms(((BinOp) e).getE2(), out);
        } else if (e instanceof UnOp) {
            collectSyms(((UnOp) e).getE(), out);
        }
    }

    /**
     * Load size-reducing ":ruleset merge" rewrites from a hand-written egglog
     * gateset file (e.g. rules_nam.txt) so they can serve as anchor targets.
     * These merge identities (RZ q a; RZ q b -> RZ q (a+b)) live ONLY in the
     * gateset file, never in the Clifford-only enumerated concrete rules, so
     * without this the RZ;SYMB;RZ anchored rule is never produced. The egglog
     * S-expr uses bare q0 / e1 / c forms; we rewrite them into the (Q "..")
     * (Symbol "..") (Nil) forms the Egg grammar expects before parsing.
     */
    static List<Rule> loadMergeRules(String gatesetFile) {
        List<Rule> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(gatesetFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.contains(":ruleset merge")) {
                    continue;
                }
                int rw = line.indexOf("rewrite");
                if (rw < 0) {
                    continue;
                }
                List<String> groups = topLevelGroups(line.substring(rw + "rewrite".length()));
                if (groups.size() < 2) {
                    continue;
                }
                EggGen.Circuit lhs = parseEggCircuit(groups.get(0));
                EggGen.Circuit rhs = parseEggCircuit(groups.get(1));
                // Only size-reducing merges are useful anchor targets (the
                // anchor loop's own gate at ruleLhs > ruleRhs also enforces it).
                if (lhs.gates.size() > rhs.gates.size()) {
                    out.add(new Rule(lhs, rhs, new ArrayList<>()));
                    System.out.println("Loaded merge rule for anchoring: "
                            + lhs.toQASM() + " -> " + rhs.toQASM());
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading merge rules from " + gatesetFile + ": " + e.getMessage());
        }
        return out;
    }

    // Extract the top-level balanced-paren groups from an egglog rewrite body
    // (the text after the "rewrite" keyword). The first two are LHS and RHS;
    // bare tokens like ":ruleset" / "merge" are naturally skipped.
    private static List<String> topLevelGroups(String s) {
        List<String> groups = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == ')') {
                depth--;
                if (depth == 0 && start >= 0) {
                    groups.add(s.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return groups;
    }

    private static EggGen.Circuit parseEggCircuit(String sexpr) {
        String s = sexpr;
        s = s.replaceAll("\\bc\\b", "(Nil)");
        s = s.replaceAll("q\\d+", "(Q \"$0\")");
        s = s.replaceAll("\\be\\d+\\b", "(Symbol \"$0\")");
        return EggAstBuilder.parseCircuit(s);
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

        Option gatesetO = new Option("g", "gateset", true,
                "gateset name; loads size-reducing :ruleset merge rules from rules_<gateset>.txt as anchor targets");
        gatesetO.setRequired(false);
        options.addOption(gatesetO);
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

        String gateset = cmd.getOptionValue("gateset");
        if (gateset != null) {
            rules.addAll(loadMergeRules("/root/rules_" + gateset + ".txt"));
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
