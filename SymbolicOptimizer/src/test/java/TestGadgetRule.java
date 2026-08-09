import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import ast.Expr;
import ast.Real;
import ast.Symbol;
import ast.UnOp;
import ast.Var;
import ast.BinOp;

public class TestGadgetRule {

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

    static Expr piHalf() {
        return new BinOp(Expr.Op.DIV, new Symbol("pi"), new Real(2));
    }

    static Expr neg(Expr e) {
        return new UnOp(Expr.Op.MINUS, e);
    }

    static boolean tryRule(String label, Runnable buildLhs, Runnable buildRhs,
            Circuit lhs, Circuit rhs, Verifier verifier, Random rand, int trials) {
        buildLhs.run();
        buildRhs.run();
        int passes = 0;
        for (int t = 0; t < trials; t++) {
            Map<String, Double> symbolMap = new HashMap<>();
            symbolMap.put(Symbolic.S_PHI, rand.nextDouble());
            for (String a : new String[]{"theta1", "theta2", "theta3"}) {
                symbolMap.put(a, rand.nextDouble() * 2 * Math.PI);
            }
            if (verifier.verifyv2(lhs, rhs, symbolMap)) passes++;
        }
        System.out.println(label + ": passed " + passes + "/" + trials + " random samples");
        return passes == trials;
    }

    public static void main(String[] args) throws Exception {
        Random rand = new Random(42);
        Verifier verifier = new Verifier(rand, 2);
        Expr theta1 = new Symbol("theta1");

        {
            Circuit lhs = start(2);
            Circuit rhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piHalf());
            Symbolic.rxx(rhs, "q0", "q1", piHalf());
            boolean ok = tryRule("[Sanity] RXX(π/2) == RXX(π/2)",
                    () -> {}, () -> {}, lhs, rhs, verifier, rand, 3);
            System.out.println("  result: " + ok);
        }

        {
            Circuit lhs = start(2);
            Circuit rhs = start(2);
            Symbolic.ry(lhs, "q0", piHalf());
            Symbolic.ry(lhs, "q1", piHalf());
            Symbolic.rxx(lhs, "q0", "q1", theta1);
            Symbolic.ry(lhs, "q0", neg(piHalf()));
            Symbolic.ry(lhs, "q1", neg(piHalf()));
            Symbolic.ry(rhs, "q0", piHalf());
            Symbolic.ry(rhs, "q1", piHalf());
            Symbolic.rxx(rhs, "q0", "q1", theta1);
            Symbolic.ry(rhs, "q0", neg(piHalf()));
            Symbolic.ry(rhs, "q1", neg(piHalf()));
            boolean ok = tryRule("[Sanity] identical RY-RXX-RY block",
                    () -> {}, () -> {}, lhs, rhs, verifier, rand, 3);
            System.out.println("  result: " + ok);
        }

        {
            Circuit lhs = start(2);
            Circuit rhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piHalf());
            Symbolic.rz(lhs, "q0", theta1);
            Symbolic.rx(lhs, "q0", neg(piHalf()));
            Symbolic.rxx(lhs, "q0", "q1", piHalf());

            Symbolic.ry(rhs, "q0", neg(piHalf()));
            Symbolic.ry(rhs, "q1", neg(piHalf()));
            Symbolic.rxx(rhs, "q0", "q1", theta1);
            Symbolic.ry(rhs, "q0", piHalf());
            Symbolic.ry(rhs, "q1", piHalf());
            boolean ok = tryRule("[Test 3] LHS_gadget vs RY-conjugated RXX(γ)",
                    () -> {}, () -> {}, lhs, rhs, verifier, rand, 3);
            System.out.println("  result: " + ok);
        }

        {
            Circuit lhs = start(2);
            Circuit rhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piHalf());
            Symbolic.rz(lhs, "q0", theta1);
            Symbolic.rx(lhs, "q0", neg(piHalf()));
            Symbolic.rxx(lhs, "q0", "q1", piHalf());

            Symbolic.rx(rhs, "q0", piHalf());
            Symbolic.rxx(rhs, "q0", "q1", theta1);
            Symbolic.rx(rhs, "q0", neg(piHalf()));
            boolean ok = tryRule("[Test 4] LHS_gadget vs RX(π/2);RXX(γ);RX(-π/2)",
                    () -> {}, () -> {}, lhs, rhs, verifier, rand, 3);
            System.out.println("  result: " + ok);
        }

        {
            Circuit lhs = start(2);
            Circuit rhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piHalf());
            Symbolic.rz(lhs, "q0", theta1);
            Symbolic.rx(lhs, "q0", neg(piHalf()));
            Symbolic.rxx(lhs, "q0", "q1", piHalf());

            Symbolic.rxx(rhs, "q0", "q1", new Symbol("pi"));
            Symbolic.rxx(rhs, "q0", "q1", theta1);
            boolean ok = tryRule("[Test 5] LHS vs RXX(π);RXX(γ)",
                    () -> {}, () -> {}, lhs, rhs, verifier, rand, 3);
            System.out.println("  result: " + ok);
        }

        System.out.println("\nDone.");
    }
}
