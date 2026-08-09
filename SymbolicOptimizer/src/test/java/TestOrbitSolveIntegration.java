import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Random;

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
