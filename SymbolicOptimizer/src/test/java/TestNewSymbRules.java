import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

public class TestNewSymbRules {

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
            String shortLHS = r.getLHS().length() > 100 ? r.getLHS().substring(0, 100) + "..." : r.getLHS();
            try {
                CircuitDAG result = opt.symbolicMatchBeforeAfter(
                        circuit, r.getLHS(), r.getRHS(), minSymb, maxSymb, r.getConstraint(), null);
                String tag = (result != null) ? "✅ MATCH " : "❌ no-match";
                System.out.println("  rule#" + ruleIdx + " on " + label + ": " + tag);
            } catch (Throwable e) {
                System.out.println("  rule#" + ruleIdx + " on " + label + ": EXC " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("  rule#" + ruleIdx + " on " + label + ": setup-exc " + e);
        }
    }

    public static void main(String[] args) throws Exception {
        List<MatrixConstrainedRule> allRules = loadRules("/root/anchored_ion_q3.txt", 279);
        System.out.println("Loaded " + allRules.size() + " new anchored rules");
        for (int i = 0; i < allRules.size(); i++) {
            String lhs = allRules.get(i).getLHS();
            System.out.println("  rule#" + i + " LHS: " + (lhs.length() > 140 ? lhs.substring(0, 140) + "..." : lhs));
        }

        Optimizer opt = new Optimizer();

        String[][] circuits = {
                {"/root/qsymb_benchmarks/ion/qaoa_5.qasm", "qaoa_5"},
                {"/tmp/qaoa_gadget.qasm", "qaoa_gadget"},
                {"/tmp/pattern_A.qasm", "Pattern A"},
                {"/tmp/pattern_B.qasm", "Pattern B"},
                {"/tmp/test_conjugation.qasm", "conjugation test"},
        };

        for (String[] c : circuits) {
            String path = c[0], label = c[1];
            System.out.println("\n=== " + label + " (" + path + ") ===");
            for (int i = 0; i < allRules.size(); i++) {
                testRule(opt, allRules.get(i), i, path, label, 0, 30);
            }
        }

        System.out.println("\nDone.");
    }
}
