import java.util.*;
import ast.*;

/**
 * Search for a 1-RXX equivalent of the QAOA gadget using the Verifier.
 * Tries both possible gate orders (rxx;rx;rz;rxx and rxx;rz;rx;rxx) and
 * a broad space of single-qubit Clifford pre/post corrections.
 */
public class TestQAOAIdentity {

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

    static boolean check(Circuit lhs, Circuit rhs, Verifier verifier, Random rand, int trials) {
        int passes = 0;
        for (int t = 0; t < trials; t++) {
            Map<String, Double> sm = new HashMap<>();
            sm.put(Symbolic.S_PHI, rand.nextDouble());
            for (String a : new String[]{"theta1", "theta2", "theta3"}) {
                sm.put(a, rand.nextDouble() * 2 * Math.PI);
            }
            if (verifier.verifyv2(lhs, rhs, sm)) passes++;
        }
        return passes == trials;
    }

    static void applyG(Circuit c, String axis, String q, Expr ang) {
        switch (axis) {
            case "rx": Symbolic.rx(c, q, ang); break;
            case "ry": Symbolic.ry(c, q, ang); break;
            case "rz": Symbolic.rz(c, q, ang); break;
            case "id": break;  // identity, no gate
        }
    }

    public static void main(String[] args) {
        Random rand = new Random(42);
        Verifier verifier = new Verifier(rand, 2);
        Expr g = new Symbol("theta1");

        // Try BOTH possible LHS orderings (which one matches qaoa_5 depends on transpilation)
        for (int order = 0; order < 2; order++) {
            String orderLabel = (order == 0) ? "rxx;rz;rx;rxx" : "rxx;rx;rz;rxx";
            System.out.println("\n========= LHS order: " + orderLabel + " =========");

            Circuit lhs = start(2);
            Symbolic.rxx(lhs, "q0", "q1", piH());
            if (order == 0) {
                Symbolic.rz(lhs, "q0", g);
                Symbolic.rx(lhs, "q0", neg(piH()));
            } else {
                Symbolic.rx(lhs, "q0", neg(piH()));
                Symbolic.rz(lhs, "q0", g);
            }
            Symbolic.rxx(lhs, "q0", "q1", piH());

            // Brute-force scan: pre and post single-qubit corrections on BOTH q0 and q1
            String[] axes = {"id", "rx", "ry", "rz"};
            Expr[] angs = {piH(), neg(piH()), piE(), neg(piE())};
            Expr[] all_angs_with_id = {new Real(0.0), piH(), neg(piH()), piE(), neg(piE())};

            int tried = 0, hits = 0;
            // pre0 on q0, pre1 on q1, RXX(g), post0 on q0, post1 on q1
            for (String pre0_ax : axes) {
                for (Expr pre0_ang : angs) {
                    if (pre0_ax.equals("id") && !pre0_ang.equals(angs[0])) continue;
                    for (String pre1_ax : axes) {
                        for (Expr pre1_ang : angs) {
                            if (pre1_ax.equals("id") && !pre1_ang.equals(angs[0])) continue;
                            for (String post0_ax : axes) {
                                for (Expr post0_ang : angs) {
                                    if (post0_ax.equals("id") && !post0_ang.equals(angs[0])) continue;
                                    for (String post1_ax : axes) {
                                        for (Expr post1_ang : angs) {
                                            if (post1_ax.equals("id") && !post1_ang.equals(angs[0])) continue;
                                            Circuit r = start(2);
                                            applyG(r, pre0_ax, "q0", pre0_ang);
                                            applyG(r, pre1_ax, "q1", pre1_ang);
                                            Symbolic.rxx(r, "q0", "q1", g);
                                            applyG(r, post0_ax, "q0", post0_ang);
                                            applyG(r, post1_ax, "q1", post1_ang);
                                            tried++;
                                            if (check(lhs, r, verifier, rand, 3)) {
                                                hits++;
                                                System.out.printf("  ✅ HIT #%d: %s(%s) q0; %s(%s) q1; RXX(γ); %s(%s) q0; %s(%s) q1%n",
                                                        hits, pre0_ax, pre0_ang, pre1_ax, pre1_ang,
                                                        post0_ax, post0_ang, post1_ax, post1_ang);
                                                if (hits >= 3) break;
                                            }
                                        }
                                        if (hits >= 3) break;
                                    }
                                    if (hits >= 3) break;
                                }
                                if (hits >= 3) break;
                            }
                            if (hits >= 3) break;
                        }
                        if (hits >= 3) break;
                    }
                    if (hits >= 3) break;
                }
                if (hits >= 3) break;
            }
            System.out.println("  Tried: " + tried + ", Hits: " + hits);
        }

        System.out.println("\nDone.");
    }
}
