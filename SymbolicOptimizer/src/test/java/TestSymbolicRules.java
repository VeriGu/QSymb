import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestSymbolicRules {

    static List<MatrixConstrainedRule> loadSymbRules(String path) throws Exception {
        List<MatrixConstrainedRule> rules = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] comp = line.split("\\|");
                if (comp.length < 4) continue;
                String lhs = comp[0];
                String rhs = comp[1];
                String type = comp[2];
                String matrix = comp[3];
                Pattern mp = Pattern.compile("\\[(.*)\\]");
                Matcher m = mp.matcher(matrix);
                if (!m.matches()) continue;
                String[] matrices = m.group(1).split("::");
                List<SymbolicSolve.SparseMatrix> matrixList = new ArrayList<>();
                for (String mm : matrices) {
                    if (!mm.startsWith("Matrix(")) continue;
                    String content = mm.substring("Matrix(".length(), mm.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                    for (int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(", ");
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(
                                Integer.valueOf(elems[0].trim()),
                                Integer.valueOf(elems[1].trim()),
                                value));
                    }
                    matrixList.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
                rules.add(new MatrixConstrainedRule(lhs, rhs, matrixList, type));
            }
        }
        return rules;
    }

    static int testCircuit(Optimizer opt, String qasmPath, List<MatrixConstrainedRule> rules,
            int minSymb, int maxSymb, String label) throws Exception {
        String qasm = new String(Files.readAllBytes(Paths.get(qasmPath)));
        CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
        int hits = 0;
        int total = rules.size();
        System.out.println("\n=== " + label + " (" + qasmPath + ") ===");
        System.out.println("Testing " + total + " rules. minSymb=" + minSymb + ", maxSymb=" + maxSymb);
        for (int i = 0; i < rules.size(); i++) {
            MatrixConstrainedRule r = rules.get(i);
            try {
                CircuitDAG result = opt.symbolicMatchBeforeAfter(
                        circuit, r.getLHS(), r.getRHS(), minSymb, maxSymb, r.getConstraint(), null);
                String shortLHS = r.getLHS().length() > 80 ? r.getLHS().substring(0, 80) + "..." : r.getLHS();
                if (result != null) {
                    hits++;
                    System.out.println("  ✓ MATCH rule#" + i + ": " + shortLHS);
                }
            } catch (Throwable e) {
                System.out.println("  ✗ EXC rule#" + i + ": " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        }
        System.out.println(label + " summary: " + hits + " / " + total + " rules matched");
        return hits;
    }

    public static void main(String[] args) throws Exception {
        List<MatrixConstrainedRule> rules = loadSymbRules("/tmp/anchored_rxx_conj.txt");
        System.out.println("Loaded " + rules.size() + " RXX-conjugation rules");

        Optimizer opt = new Optimizer();

        // Pattern A: RXX wraps gates on OTHER qubits
        testCircuit(opt, "/tmp/pattern_A.qasm", rules, 2, 25, "Pattern A (other-qubit middle, minSymb=2)");
        testCircuit(opt, "/tmp/pattern_A.qasm", rules, 0, 25, "Pattern A (other-qubit middle, minSymb=0)");

        // Pattern B: RXX wraps RX-only on wrapper qubits
        testCircuit(opt, "/tmp/pattern_B.qasm", rules, 2, 25, "Pattern B (RX-only middle)");

        // QAOA gadget
        testCircuit(opt, "/tmp/qaoa_gadget.qasm", rules, 2, 25, "QAOA gadget (RZ-heavy middle)");

        // Conjugation success case from earlier
        testCircuit(opt, "/tmp/test_conjugation.qasm", rules, 2, 25, "RXX(-π/2);[12 RX];RXX(π/2)");

        // ORIGINAL qaoa_5 circuit, no egglog preprocessing
        testCircuit(opt, "/root/guoq_benchmarks/ion/qaoa_5.qasm", rules, 0, 30, "qaoa_5 original (no egglog)");

        System.out.println("\nDone.");
    }
}
