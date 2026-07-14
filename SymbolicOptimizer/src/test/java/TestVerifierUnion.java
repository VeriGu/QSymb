import java.util.*;
import ast.*;

/**
 * Regression tests for Verifier.verifyv2.
 *
 * Pre-fix bug: qubitMaps were built from c1's qubits only, so when the
 * enumerator's empty circuit (declared [q0,q1,q2], used {}) was compared
 * against a single-gate circuit (declared [q0,q1,q2], used {q0}), gates
 * on q0 were still evaluated -- BUT the verifier's phase loop accepted
 * any per-qubitMap phase, which let bogus single-gate-vs-empty pairs slip.
 *
 * After the fix, qubitMaps build from the UNION of c1+c2 declared qubits.
 * These tests mirror the enumerator setup (both sides share the same
 * declared qubit list) so they reproduce the real failure mode.
 */
public class TestVerifierUnion {

    static Circuit start(int n) {
        ArrayList<String> qubits = new ArrayList<>();
        TreeMap<String, Expr> f = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            String name = "q" + i;
            qubits.add(name);
            f.put(name, new Var(name));
        }
        Symbolic s = new Symbolic(new Real(1), f);
        ArrayList<Symbolic> pathSum = new ArrayList<>(Arrays.asList(s));
        return new Circuit(qubits, pathSum, new ArrayList<>(), new ArrayList<>());
    }

    static Map<String, Double> symbolMap(Random r) {
        Map<String, Double> m = new HashMap<>();
        m.put(Symbolic.S_PHI, r.nextDouble());
        for (String a : new String[]{"theta1", "theta2", "theta3"}) {
            m.put(a, r.nextDouble() * 2 * Math.PI);
        }
        return m;
    }

    static int passed = 0;
    static int failed = 0;

    static void expect(String label, boolean expected, boolean actual) {
        boolean ok = expected == actual;
        System.out.println((ok ? "PASS" : "FAIL") + "  " + label
                + "  (expected=" + expected + ", got=" + actual + ")");
        if (ok) passed++; else failed++;
    }

    public static void main(String[] args) {
        Random rand = new Random(42);
        int N = 3;  // matches enumerator's maxQubits=3
        Verifier verifier = new Verifier(rand, N);
        Map<String, Double> sm = symbolMap(rand);

        Expr theta1 = new Symbol("theta1");
        Expr neg_theta1 = new UnOp(Expr.Op.MINUS, theta1);

        // --- Bug regression: empty vs single rotation, same declared qubits ---
        {
            Circuit lhs = start(N);                       // declared [q0..q2], used {}
            Circuit rhs = start(N);
            Symbolic.rx(rhs, "q0", theta1);               // declared [q0..q2], used {q0}
            expect("empty(N=3) vs rx(theta1) q0", false, verifier.verifyv2(lhs, rhs, sm));
        }
        {
            Circuit lhs = start(N);
            Symbolic.rx(lhs, "q0", theta1);
            Circuit rhs = start(N);
            expect("rx(theta1) q0 vs empty(N=3)", false, verifier.verifyv2(lhs, rhs, sm));
        }
        {
            Circuit lhs = start(N);
            Symbolic.rz(lhs, "q0", theta1);
            Circuit rhs = start(N);
            expect("empty(N=3) vs rz(theta1) q0", false, verifier.verifyv2(lhs, rhs, sm));
        }
        {
            Circuit lhs = start(N);
            Symbolic.rxx(lhs, "q0", "q1", theta1);
            Circuit rhs = start(N);
            expect("empty(N=3) vs rxx(theta1) q0,q1", false, verifier.verifyv2(lhs, rhs, sm));
        }

        // --- Valid inverse identities should still accept ---
        {
            Circuit lhs = start(N);
            Symbolic.rx(lhs, "q0", theta1);
            Symbolic.rx(lhs, "q0", neg_theta1);
            Circuit rhs = start(N);
            expect("rx(theta1) q0; rx(-theta1) q0 vs empty(N=3)", true, verifier.verifyv2(lhs, rhs, sm));
        }
        {
            Circuit lhs = start(N);
            Symbolic.cx(lhs, "q0", "q1");
            Symbolic.cx(lhs, "q0", "q1");
            Circuit rhs = start(N);
            expect("cx q0,q1; cx q0,q1 vs empty(N=3)", true, verifier.verifyv2(lhs, rhs, sm));
        }

        // --- Self-equivalence ---
        {
            Circuit lhs = start(N);
            Symbolic.rxx(lhs, "q0", "q1", theta1);
            Circuit rhs = start(N);
            Symbolic.rxx(rhs, "q0", "q1", theta1);
            expect("rxx(theta1) q0,q1 vs itself", true, verifier.verifyv2(lhs, rhs, sm));
        }

        // --- Different qubit acted upon ---
        {
            Circuit lhs = start(N);
            Symbolic.rx(lhs, "q0", theta1);
            Circuit rhs = start(N);
            Symbolic.rx(rhs, "q1", theta1);
            expect("rx(theta1) q0 vs rx(theta1) q1", false, verifier.verifyv2(lhs, rhs, sm));
        }

        // --- Global-phase fix: rx(π) and ry(π) are NOT equal up to a single
        //     global phase (rx(π) = -iX, ry(π) = -iY differ by X vs Y).
        //     Previously the verifier picked a separate phase per qubitMap
        //     and falsely accepted this. ---
        {
            Expr pi = new Symbol("pi");
            Circuit lhs = start(N);
            Symbolic.rx(lhs, "q0", pi);
            Circuit rhs = start(N);
            Symbolic.ry(rhs, "q0", pi);
            expect("rx(π) q0 vs ry(π) q0", false, verifier.verifyv2(lhs, rhs, sm));
        }

        // --- Genuine global-phase equivalence: rz(2π) ≡ identity up to -1 ---
        {
            Expr twopi = new BinOp(Expr.Op.MULT, new Real(2.0), new Symbol("pi"));
            Circuit lhs = start(N);
            Symbolic.rz(lhs, "q0", twopi);
            Circuit rhs = start(N);  // identity
            expect("rz(2π) q0 vs empty (global phase -1)", true, verifier.verifyv2(lhs, rhs, sm));
        }

        System.out.println("\n=== " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }
}
