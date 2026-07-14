import java.util.*;
import ast.*;

/**
 * Reproduce the stuck case: solveSymb on
 *   c1 = SYMB(2); RXX(q0,q1,theta1); RXX(q0,q1,theta2)
 *   c2 = RXX(q0,q1,theta1); RXX(q0,q1,theta2); SYMB(2)
 *
 * Run with a 60s timeout to see whether it completes; if not, which Python
 * stage it's in (semantics.py prints [INT HH:MM:SS] markers to stderr).
 */
public class TestSolveSymb {
    public static void main(String[] args) throws Exception {
        SymbolicSolve solver = new SymbolicSolve(new Random(42));

        // The new stuck pair from the user (entry 0/8110 at 04:22):
        // c1 = SYMB(2); RX(q0, pi); RX(q0, theta2)
        // c2 = RX(q0, pi); RX(q0, theta2); SYMB(2)
        List<EggGen.Gate> g1 = new ArrayList<>();
        g1.add(new EggGen.SYMB(2));
        g1.add(new EggGen.RX("q0", new Symbol("pi")));
        g1.add(new EggGen.RX("q0", new Symbol("theta2")));
        EggGen.Circuit c1 = new EggGen.Circuit(g1);

        List<EggGen.Gate> g2 = new ArrayList<>();
        g2.add(new EggGen.RX("q0", new Symbol("pi")));
        g2.add(new EggGen.RX("q0", new Symbol("theta2")));
        g2.add(new EggGen.SYMB(2));
        EggGen.Circuit c2 = new EggGen.Circuit(g2);

        // Also dump the JSON Java sends to Python so we can compare it to the
        // EGG-string form in the log -- in case parameter passing is wrong.
        System.out.println("c1 JSON: " + solver.circuitToJson(c1.gates, 3));
        System.out.println("c2 JSON: " + solver.circuitToJson(c2.gates, 3));

        System.out.println("c1: " + c1.toEggString());
        System.out.println("c2: " + c2.toEggString());
        System.out.println("Calling solveSymb (n=3)...");

        long t0 = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            try {
                String basis = solver.solveSymb(c1, c2, 3);
                long dt = System.currentTimeMillis() - t0;
                System.out.println("DONE in " + dt + " ms");
                System.out.println("basis: " + (basis == null ? "null"
                        : (basis.length() > 200 ? basis.substring(0, 200) + "..." : basis)));
            } catch (Throwable e) {
                System.out.println("FAILED: " + e);
            }
        });
        t.setDaemon(true);
        t.start();
        t.join(120_000);
        if (t.isAlive()) {
            long dt = System.currentTimeMillis() - t0;
            System.out.println("TIMEOUT after " + dt + " ms — solveSymb is hung");
            System.exit(2);
        }
    }
}
