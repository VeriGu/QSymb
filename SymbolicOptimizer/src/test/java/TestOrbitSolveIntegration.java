import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Random;

/**
 * Integration test: feed every CliffordOrbitCandidates pair through the real
 * Python intertwiner solver (SymbolicSolve.solveSymb) and verify each
 * produces a non-empty basis.
 *
 * Skipped by JUnit suite because it spawns the semantics.py process pool.
 * Run manually after building the jar:
 *   javac --enable-preview --release 17 -cp <jar> TestOrbitSolveIntegration.java
 *   java  --enable-preview -cp .:<jar> TestOrbitSolveIntegration
 */
public class TestOrbitSolveIntegration {
    public static void main(String[] args) throws Exception {
        SymbolicSolve solver = new SymbolicSolve(new Random(0));
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");

        System.out.println("Generated " + cands.size() + " orbit candidates for ion.");
        int passes = 0;
        for (int i = 0; i < cands.size(); i++) {
            SimpleEntry<EggGen.Circuit, EggGen.Circuit> p = cands.get(i);
            EggGen.Circuit L = p.getKey();
            EggGen.Circuit R = p.getValue();
            System.out.println();
            System.out.println("=== candidate " + i + " ===");
            System.out.println("L: " + L.toEggString());
            System.out.println("R: " + R.toEggString());
            long t0 = System.currentTimeMillis();
            String basis = solver.solveSymb(L, R, 2);
            long dt = System.currentTimeMillis() - t0;
            List<SymbolicSolve.SparseMatrix> parsed = SymbolicSolve.parseBasis(basis);
            System.out.println("Solved in " + dt + " ms; basis size = " + parsed.size());
            if (parsed.isEmpty()) {
                System.err.println("FAIL: candidate " + i + " produced empty basis");
                System.exit(1);
            }
            // Sylvester: for L, R sharing eigenvalue multiset {a^2, b^2} on a
            // 4-dim space, the intertwiner has dim 8.
            if (parsed.size() != 8) {
                System.out.println("NOTE: candidate " + i + " produced basis size "
                        + parsed.size() + " (expected 8); investigate.");
            }
            passes++;
        }
        System.out.println();
        System.out.println("=== " + passes + "/" + cands.size() + " candidates produced non-empty basis ===");
        System.exit(passes == cands.size() ? 0 : 1);
    }
}
