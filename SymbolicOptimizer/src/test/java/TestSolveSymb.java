import java.util.*;
import ast.*;

public class TestSolveSymb {
    public static void main(String[] args) throws Exception {
        SymbolicSolve solver = new SymbolicSolve(new Random(42));

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
