import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

public class TestNewSymbRules2 {
    static List<MatrixConstrainedRule> loadRules(String path, int skipFirst) throws Exception {
        List<MatrixConstrainedRule> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null) {
                if (idx++ < skipFirst) continue;
                String[] p = line.split("\\|");
                if (p.length < 4) continue;
                Pattern mp = Pattern.compile("\\[(.*)\\]");
                Matcher m = mp.matcher(p[3]);
                if (!m.matches()) continue;
                String[] matrices = m.group(1).split("::");
                List<SymbolicSolve.SparseMatrix> mlist = new ArrayList<>();
                for (String mm : matrices) {
                    if (!mm.startsWith("Matrix(")) continue;
                    String c = mm.substring("Matrix(".length(), mm.length() - 1);
                    String[] e = c.split(";");
                    int rows = Integer.valueOf(e[0]);
                    int cols = Integer.valueOf(e[1]);
                    List<SymbolicSolve.SparseMatrix.MatrixEntry> ents = new ArrayList<>();
                    for (int i = 2; i < e.length; i++) {
                        String entry = e[i].substring(1, e[i].length() - 1);
                        String[] el = entry.split(",");
                        ents.add(new SymbolicSolve.SparseMatrix.MatrixEntry(
                                Integer.valueOf(el[0].trim()),
                                Integer.valueOf(el[1].trim()),
                                SymbolicSolve.parseComplex(el[2])));
                    }
                    mlist.add(new SymbolicSolve.SparseMatrix(rows, cols, ents));
                }
                out.add(new MatrixConstrainedRule(p[0], p[1], mlist, p[2]));
            }
        }
        return out;
    }

    static void testRule(Optimizer opt, MatrixConstrainedRule r, int ruleIdx,
            String circuitPath, String label, int minSymb, int maxSymb) {
        try {
            String qasm = new String(Files.readAllBytes(Paths.get(circuitPath)));
            CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
            long t0 = System.currentTimeMillis();
            CircuitDAG result = opt.symbolicMatchBeforeAfter(
                    circuit, r.getLHS(), r.getRHS(), minSymb, maxSymb, r.getConstraint(), null);
            long dt = System.currentTimeMillis() - t0;
            String tag = (result != null) ? "✅ MATCH " : "❌ no-match";
            System.out.println("  rule#" + ruleIdx + " on " + label + ": " + tag + " (" + dt + " ms)");
        } catch (Throwable e) {
            System.out.println("  rule#" + ruleIdx + " on " + label + ": EXC " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        List<MatrixConstrainedRule> rules = loadRules("/root/anchored_ion_q3.txt", 279);
        System.out.println("Loaded " + rules.size() + " new anchored rules");
        for (int i = 0; i < rules.size(); i++) {
            String lhs = rules.get(i).getLHS();
            System.out.println("  rule#" + i + " LHS: " + (lhs.length() > 140 ? lhs.substring(0, 140) + "..." : lhs));
        }

        Optimizer opt = new Optimizer();
        String[][] circuits = {
                {"/tmp/qaoa_gadget.qasm", "qaoa_gadget (1 instance)"},
                {"/tmp/pattern_A.qasm", "Pattern A"},
                {"/tmp/pattern_B.qasm", "Pattern B"},
                {"/tmp/test_conjugation.qasm", "RXX(-π/2);[12 RX];RXX(π/2)"},
        };

        for (String[] c : circuits) {
            System.out.println("\n=== " + c[1] + " ===");
            for (int i = 0; i < rules.size(); i++) {
                testRule(opt, rules.get(i), i, c[0], c[1], 0, 25);
            }
        }
        System.out.println("\nDone.");
    }
}
