import java.util.*;
import ast.*;

/**
 * Verify the simpler reduction path: zero-angle RY removal + RX commutation
 * lets two RXX(π/2) merge.
 *
 * Identity 1: RY(α);RY(-α) = I  (trivial, but tests merge+zero-removal chain)
 * Identity 2: RXX(π/2);RY(α)q1;RY(-α)q1;RXX(π/2) = RXX(π) (after RY cancels, RXX merge fires)
 * Identity 3: Full simplified qaoa-style gadget on q1 side:
 *             RXX(π/2);RX(-π/2)q1;RY(-π/2)q1;RY(π/2)q1;RXX(π/2)
 *             vs RX(-π/2)q1;RXX(π)  (saves 1 RXX)
 */
public class TestZeroAngleRule {

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

    static Expr piH() { return new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(2)); }
    static Expr piE() { return new Symbol("pi"); }
    static Expr neg(Expr e) { return new UnOp(Expr.Op.MINUS, e); }

    static boolean check(String label, Circuit lhs, Circuit rhs, Verifier verifier, Random rand) {
        int passes = 0, trials = 5;
        for (int t = 0; t < trials; t++) {
            Map<String, Double> sm = new HashMap<>();
            sm.put(Symbolic.S_PHI, rand.nextDouble());
            for (String a : new String[]{"theta1", "theta2", "theta3"}) {
                sm.put(a, rand.nextDouble() * 2 * Math.PI);
            }
            if (verifier.verifyv2(lhs, rhs, sm)) passes++;
        }
        boolean ok = passes == trials;
        System.out.println((ok ? "✅ PASS" : ("❌ FAIL (" + passes + "/" + trials + ")")) + "  " + label);
        return ok;
    }

    public static void main(String[] args) {
        Random rand = new Random(42);
        Verifier verifier = new Verifier(rand, 2);
        Expr theta1 = new Symbol("theta1");

        // Sanity: RY(α);RY(-α) on q0 == identity
        {
            Circuit l = start(2), r = start(2);
            Symbolic.ry(l, "q0", theta1);
            Symbolic.ry(l, "q0", neg(theta1));
            // RHS: empty circuit
            check("[Test 1] RY(α);RY(-α) q0 == I", l, r, verifier, rand);
        }

        // RY(α);RY(-α) sandwich preserves the RXX
        {
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH());
            Symbolic.ry(l, "q1", neg(piH()));
            Symbolic.ry(l, "q1", piH());
            Symbolic.rxx(l, "q0", "q1", piH());

            Symbolic.rxx(r, "q0", "q1", piE());  // merged RXX(π)
            check("[Test 2] RXX;RY(-π/2);RY(π/2);RXX == RXX(π)", l, r, verifier, rand);
        }

        // Add an RX(-π/2) on q1 before the cancelling RY pair (as in qaoa_5)
        {
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH());
            Symbolic.rx(l, "q1", neg(piH()));
            Symbolic.ry(l, "q1", neg(piH()));
            Symbolic.ry(l, "q1", piH());
            Symbolic.rxx(l, "q0", "q1", piH());

            // RX(q1) commutes with RXX → RX commutes out, RY pair cancels, two RXXs merge to RXX(π)
            Symbolic.rx(r, "q1", neg(piH()));
            Symbolic.rxx(r, "q0", "q1", piE());
            check("[Test 3] RXX;RX;RY(-π/2);RY(π/2);RXX == RX;RXX(π)", l, r, verifier, rand);
        }

        // Full qaoa-style q1 portion (3 single-qubit gates on q1, q0 portion ignored for now)
        {
            // q0 has a chain U_0; q1 has rx(-π/2); ry(-π/2); ry(π/2)
            // Approximate by setting q0 to a 1-q symbol via theta1 (RZ on q0).
            // For this test, set q0 to just identity (only test the q1 reduction).
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH());
            // q0 middle: just RZ(γ) as a stand-in for the long q0 chain
            Symbolic.rz(l, "q0", theta1);
            // q1 middle
            Symbolic.rx(l, "q1", neg(piH()));
            Symbolic.ry(l, "q1", neg(piH()));
            Symbolic.ry(l, "q1", piH());
            Symbolic.rxx(l, "q0", "q1", piH());

            // Equivalent: RZ(γ) on q0 stays; RX(-π/2) on q1 commutes out; two RXX merge to RXX(π).
            // But RZ on q0 also has to commute past RXX --- and RZ on q0 does NOT commute with RXX.
            // So we'd need RZ between the two RXXs OR after the RXXs collapse.
            // Try: RXX(π);RZ(γ) q0;RX(-π/2) q1  (collapse the gadget, RZ left in original spot)
            Symbolic.rxx(r, "q0", "q1", piE());
            Symbolic.rz(r, "q0", theta1);
            Symbolic.rx(r, "q1", neg(piH()));
            check("[Test 4] full RXX(π/2);RZ(γ)q0;[RY cancel]q1;RXX(π/2) ≟ RXX(π);RZ(γ)q0;RX(-π/2)q1", l, r, verifier, rand);
        }

        System.out.println("\nDone.");
    }
}
