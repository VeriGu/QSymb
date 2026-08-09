import java.util.*;

public class RuleReverser {

    public enum Direction { FORWARD_ONLY, REVERSE_ONLY, BOTH, DROP }

    public static Direction decide(Rule rule, String gateset) {
        if (!isCxGateset(gateset)) {
            return Direction.BOTH;
        }
        int lhsSize = rule.lhs.gates.size();
        int rhsSize = rule.rhs.gates.size();

        if (lhsSize == rhsSize) {
            return Direction.BOTH;
        }
        if (lhsSize > rhsSize) {
            Rule swapped = new Rule(rule.rhs, rule.lhs, rule.conditions);
            if (isBraidCx(swapped) && reverseIsFireable(rule)) {
                return Direction.BOTH;
            }
            return Direction.FORWARD_ONLY;
        }
        if (isPauliPushthroughCx(rule) || isBraidCx(rule)) {
            return reverseIsFireable(rule) ? Direction.BOTH : Direction.FORWARD_ONLY;
        }
        return reverseIsFireable(rule) ? Direction.REVERSE_ONLY : Direction.DROP;
    }

    static boolean isPauliPushthroughCx(Rule rule) {
        int lhsCx = countCx(rule.lhs);
        int rhsCx = countCx(rule.rhs);
        if (lhsCx != 1 || rhsCx != 1) return false;

        EggGen.CX lhsCxGate = firstCx(rule.lhs);
        EggGen.CX rhsCxGate = firstCx(rule.rhs);
        if (lhsCxGate == null || rhsCxGate == null) return false;

        if (!lhsCxGate.control.equals(rhsCxGate.control)
                || !lhsCxGate.target.equals(rhsCxGate.target)) {
            return false;
        }

        int lhsPaulis = countSingleQubitPaulis(rule.lhs);
        int rhsPaulis = countSingleQubitPaulis(rule.rhs);

        if (rhsPaulis <= lhsPaulis) return false;

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

        if (rule.lhs.gates.size() != 2 || rule.rhs.gates.size() != 3) return false;
        for (EggGen.Gate g : rule.lhs.gates) if (!(g instanceof EggGen.CX)) return false;
        for (EggGen.Gate g : rule.rhs.gates) if (!(g instanceof EggGen.CX)) return false;

        Set<String> qubits = new HashSet<>();
        for (EggGen.Gate g : rule.lhs.gates) addCxQubits(qubits, (EggGen.CX) g);
        for (EggGen.Gate g : rule.rhs.gates) addCxQubits(qubits, (EggGen.CX) g);
        if (qubits.size() > 3) return false;

        EggGen.CX a = (EggGen.CX) rule.lhs.gates.get(0);
        EggGen.CX b = (EggGen.CX) rule.lhs.gates.get(1);
        Set<String> aq = new HashSet<>(); addCxQubits(aq, a);
        Set<String> bq = new HashSet<>(); addCxQubits(bq, b);
        aq.retainAll(bq);
        return !aq.isEmpty();
    }

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
        }
        return n;
    }

    static boolean singleQubitGatesOnlyOn(EggGen.Circuit c, Set<String> allowed) {
        for (EggGen.Gate g : c.gates) {
            String q = singleQubit(g);
            if (q == null) continue;
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
                || "ibmnewmin".equals(g);
    }

    static boolean reverseIsFireable(Rule rule) {
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
