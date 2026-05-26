import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

/** Sweep: for 10 ion benchmarks, count how many sites each of the 4 orbit
 *  canonical rules matches. Note: each application here INCREASES total
 *  gate count (LHS=2, RHS=6), so this measures only matcher coverage, not
 *  optimization quality. Real optimizer would use SA accept/reject. */
public class SweepOrbitRules {

    static List<MatrixConstrainedRule> loadRules(String path) throws Exception {
        List<MatrixConstrainedRule> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p.length < 4) continue;
                Matcher m = Pattern.compile("\\[(.*)\\]").matcher(p[3]);
                if (!m.matches()) continue;
                List<SymbolicSolve.SparseMatrix> mlist = new ArrayList<>();
                for (String mm : m.group(1).split("::")) {
                    if (!mm.startsWith("Matrix(")) continue;
                    String c = mm.substring(7, mm.length() - 1);
                    String[] e = c.split(";");
                    int rows = Integer.valueOf(e[0]), cols = Integer.valueOf(e[1]);
                    List<SymbolicSolve.SparseMatrix.MatrixEntry> ents = new ArrayList<>();
                    for (int i = 2; i < e.length; i++) {
                        String s = e[i].substring(1, e[i].length() - 1);
                        int c1 = s.indexOf(','), c2 = s.indexOf(',', c1 + 1);
                        int r = Integer.parseInt(s.substring(0, c1).trim());
                        int col = Integer.parseInt(s.substring(c1 + 1, c2).trim());
                        String val = s.substring(c2 + 1).trim();
                        ents.add(new SymbolicSolve.SparseMatrix.MatrixEntry(r, col,
                                SymbolicSolve.parseComplex(val)));
                    }
                    mlist.add(new SymbolicSolve.SparseMatrix(rows, cols, ents));
                }
                out.add(new MatrixConstrainedRule(p[0], p[1], mlist, p[2]));
            }
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        List<MatrixConstrainedRule> rules = loadRules("/tmp/orbit_rules_v5.txt");
        System.out.println("Loaded " + rules.size() + " orbit canonical rules.");
        for (int i = 0; i < rules.size(); i++) {
            System.out.println("  rule#" + i + " LHS: " + rules.get(i).getLHS().substring(0, Math.min(80, rules.get(i).getLHS().length())) + "...");
        }
        System.out.println();

        String[] benchmarks = {
            "tof_3.qasm", "rd32-v0_66.qasm", "4gt11_83.qasm", "mod5d1_63.qasm",
            "4mod5-v1_22.qasm", "4mod5-v0_20.qasm", "ham3_102.qasm",
            "ex-1_166.qasm", "4gt11_84.qasm", "ex1_226.qasm",
        };

        Optimizer opt = new Optimizer();

        System.out.printf("%-22s %s%n", "benchmark", "  total / out_of  matched_idx                                 wall");
        for (String bn : benchmarks) {
            String path = "/root/guoq_benchmarks/ion/" + bn;
            String qasm = new String(Files.readAllBytes(Paths.get(path)));
            CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);

            int[] perRule = new int[rules.size()];
            int total = 0;
            StringBuilder matched = new StringBuilder();
            long t0 = System.currentTimeMillis();
            for (int rIdx = 0; rIdx < rules.size(); rIdx++) {
                MatrixConstrainedRule r = rules.get(rIdx);
                try {
                    CircuitDAG result = opt.symbolicMatchBeforeAfter(
                            circuit, r.getLHS(), r.getRHS(), 0, 6, r.getConstraint(), null);
                    if (result != null) {
                        perRule[rIdx]++;
                        total++;
                        if (matched.length() > 0) matched.append(",");
                        matched.append(rIdx);
                    }
                } catch (Throwable t) { /* skip */ }
            }
            long dt = System.currentTimeMillis() - t0;
            System.out.printf("%-22s   %3d / %3d    [%s]   %5.1fs%n",
                    bn, total, rules.size(), matched.toString(), dt / 1000.0);
        }
        System.exit(0);
    }
}
