import java.util.*;
import ast.*;

public class TestQAOAFinal {
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

    static boolean check(String label, Circuit lhs, Circuit rhs, Verifier verifier, Random rand, int trials) {
        int passes = 0;
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
        Expr g = new Symbol("theta1");

        {
            Circuit lhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piH());
            Symbolic.rz(lhs, "q0", g);
            Symbolic.rx(lhs, "q0", neg(piH()));
            Symbolic.rxx(lhs, "q0", "q1", piH());

            Circuit rhs = start(2);
            Symbolic.rxx(rhs, "q0", "q1", g);
            Symbolic.rx(rhs, "q0", piH());
            Symbolic.rx(rhs, "q1", piE());
            check("Identity A: rxx(π/2);rz(γ)q0;rx(-π/2)q0;rxx(π/2) == rxx(γ);rx(π/2)q0;rx(π)q1",
                    lhs, rhs, verifier, rand, 20);
        }

        {
            Circuit lhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piH());
            Symbolic.rx(lhs, "q0", neg(piH()));
            Symbolic.rz(lhs, "q0", g);
            Symbolic.rxx(lhs, "q0", "q1", piH());

            Circuit rhs = start(2);
            Symbolic.rx(rhs, "q0", piH());
            Symbolic.rz(rhs, "q1", piH());
            Symbolic.rxx(rhs, "q0", "q1", g);
            Symbolic.ry(rhs, "q0", piH());
            Symbolic.rx(rhs, "q1", piE());
            check("Identity B: rxx(π/2);rx(-π/2)q0;rz(γ)q0;rxx(π/2) == rx(π/2)q0;rz(π/2)q1;rxx(γ);ry(π/2)q0;rx(π)q1",
                    lhs, rhs, verifier, rand, 20);
        }

        {
            Circuit lhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piH());
            Symbolic.rz(lhs, "q0", g);
            Symbolic.rx(lhs, "q0", neg(piH()));
            Symbolic.rxx(lhs, "q0", "q1", piH());
            Circuit rhs = start(2);
            Symbolic.rxx(rhs, "q0", "q1", g);
            Symbolic.rx(rhs, "q0", piH());
            Symbolic.rx(rhs, "q1", neg(piE()));
            check("Identity A': same with rx(-π) on q1", lhs, rhs, verifier, rand, 20);
        }

        System.out.println("\nDone.");
    }
}
