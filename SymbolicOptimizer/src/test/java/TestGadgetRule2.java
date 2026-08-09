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

public class TestGadgetRule2 {

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
        int trials = 5;
        int passes = 0;
        for (int t = 0; t < trials; t++) {
            Map<String, Double> symbolMap = new HashMap<>();
            symbolMap.put(Symbolic.S_PHI, rand.nextDouble());
            for (String a : new String[]{"theta1", "theta2", "theta3"}) {
                symbolMap.put(a, rand.nextDouble() * 2 * Math.PI);
            }
            if (verifier.verifyv2(lhs, rhs, symbolMap)) passes++;
        }
        String tag = (passes == trials) ? "✅ PASS" : ("❌ fail (" + passes + "/" + trials + ")");
        System.out.println(tag + "  " + label);
        return passes == trials;
    }

    public static void main(String[] args) {
        Random rand = new Random(42);
        Verifier verifier = new Verifier(rand, 2);
        Expr g = new Symbol("theta1");

        System.out.println("=== A: RXX(π/2);RZ(γ);RXX(π/2) variants (no middle RX) ===");

        {
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH()); Symbolic.rz(l, "q0", g); Symbolic.rxx(l, "q0", "q1", piH());
            Symbolic.ry(r, "q0", neg(piH())); Symbolic.ry(r, "q1", neg(piH()));
            Symbolic.rxx(r, "q0", "q1", g);
            Symbolic.ry(r, "q0", piH()); Symbolic.ry(r, "q1", piH());
            check("LHS RXX(π/2);RZ(γ);RXX(π/2) vs RY-RXX(γ)-RY", l, r, verifier, rand);
        }

        {
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH()); Symbolic.rz(l, "q0", g); Symbolic.rxx(l, "q0", "q1", piH());
            Symbolic.rxx(r, "q0", "q1", piE());
            Symbolic.rz(r, "q0", g);
            check("LHS RXX(π/2);RZ(γ);RXX(π/2) vs RXX(π);RZ(γ)", l, r, verifier, rand);
        }

        System.out.println("\n=== B: full QAOA gadget variants (RXX;RZ;RX;RXX) ===");

        {
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH()); Symbolic.rz(l, "q0", g);
            Symbolic.rx(l, "q0", neg(piH())); Symbolic.rxx(l, "q0", "q1", piH());

            Symbolic.rxx(r, "q0", "q1", piE()); Symbolic.rz(r, "q0", neg(g));
            check("RXX;RZ(γ);RX(-π/2);RXX  vs  RXX(π);RZ(-γ)", l, r, verifier, rand);
        }

        {
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH()); Symbolic.rz(l, "q0", g);
            Symbolic.rx(l, "q0", neg(piH())); Symbolic.rxx(l, "q0", "q1", piH());
            Symbolic.ry(r, "q0", neg(piH())); Symbolic.rz(r, "q0", g); Symbolic.ry(r, "q0", piH());
            check("RXX;RZ(γ);RX(-π/2);RXX  vs  RY(-π/2);RZ(γ);RY(π/2)  on q0", l, r, verifier, rand);
        }

        System.out.println("\n=== C: candidate from Clifford-conjugation theory — surrounding pattern ===");

        {
            Circuit l = start(2), r = start(2);
            Symbolic.rxx(l, "q0", "q1", piH()); Symbolic.rz(l, "q0", g);
            Symbolic.rx(l, "q0", neg(piH())); Symbolic.rxx(l, "q0", "q1", piH());

            Symbolic.rx(r, "q0", neg(piH()));
            Symbolic.ry(r, "q1", piH());
            Symbolic.rxx(r, "q0", "q1", g);
            Symbolic.ry(r, "q1", neg(piH()));
            Symbolic.rx(r, "q0", piH());
            check("RXX;RZ(γ);RX(-π/2);RXX  vs  RX⊥;RY;RXX(γ);RY⁻¹;RX⊥⁻¹", l, r, verifier, rand);
        }

        System.out.println("\n=== D: brute-force scan — try many simple RHS templates ===");

        Circuit fixed_lhs = start(2);
        Symbolic.rxx(fixed_lhs, "q0", "q1", piH()); Symbolic.rz(fixed_lhs, "q0", g);
        Symbolic.rx(fixed_lhs, "q0", neg(piH())); Symbolic.rxx(fixed_lhs, "q0", "q1", piH());

        String[] g1axis = {"rx", "ry", "rz"};
        Expr[] g1angs = {piH(), neg(piH()), piE(), neg(piE())};
        String[] g2axis = {"rxx"};

        int tried = 0, hits = 0;
        for (String a1 : g1axis) {
            for (Expr a1ang : g1angs) {
                for (String a2 : g1axis) {
                    for (Expr a2ang : g1angs) {
                        Circuit r = start(2);
                        applyG(r, a1, "q0", a1ang);
                        Symbolic.rxx(r, "q0", "q1", g);
                        applyG(r, a2, "q0", a2ang);
                        tried++;
                        int passes = 0;
                        for (int t = 0; t < 3; t++) {
                            Map<String, Double> sm = new HashMap<>();
                            sm.put(Symbolic.S_PHI, rand.nextDouble());
                            sm.put("theta1", rand.nextDouble() * 2 * Math.PI);
                            sm.put("theta2", rand.nextDouble() * 2 * Math.PI);
                            if (verifier.verifyv2(fixed_lhs, r, sm)) passes++;
                        }
                        if (passes == 3) {
                            hits++;
                            System.out.println("  ✅ HIT: " + a1 + "(" + a1ang + ") q0; RXX(γ); " + a2 + "(" + a2ang + ") q0");
                        }
                    }
                }
            }
        }
        System.out.println("D summary: " + hits + " hits / " + tried + " RHS templates tried");

        System.out.println("\nDone.");
    }

    static void applyG(Circuit c, String axis, String q, Expr ang) {
        switch (axis) {
            case "rx": Symbolic.rx(c, q, ang); break;
            case "ry": Symbolic.ry(c, q, ang); break;
            case "rz": Symbolic.rz(c, q, ang); break;
        }
    }
}
