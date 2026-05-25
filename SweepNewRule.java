import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import java.util.concurrent.*;
import ast.*;

/** Sweep: for 10 small ion benchmarks, count how many distinct sites the
 *  RXX(pi/2); SYMB -> SYMB; RZZ(-pi/2)  rule matches. For each benchmark we
 *  rebuild the circuit at the start, then repeatedly try the rule; each
 *  successful application replaces a site and reduces the circuit. We
 *  count the number of successful applications. */
public class SweepNewRule {

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
        // 10 small ion benchmarks (sorted by size, descending list of smallest first).
        String[] benchmarks = {
            "tof_3.qasm",
            "rd32-v0_66.qasm",
            "4gt11_83.qasm",
            "mod5d1_63.qasm",
            "4mod5-v1_22.qasm",
            "4mod5-v0_20.qasm",
            "ham3_102.qasm",
            "ex-1_166.qasm",
            "4gt11_84.qasm",
            "ex1_226.qasm",
        };

        List<MatrixConstrainedRule> rules = loadRules("/tmp/rule_4gt11.txt");
        MatrixConstrainedRule r = rules.get(0);
        System.out.println("Rule LHS: " + r.getLHS());
        System.out.println("Rule RHS: " + r.getRHS());
        System.out.println();
        System.out.printf("%-22s %8s %8s %8s%n", "benchmark", "matches", "RXX(pi/2)", "total");

        Optimizer opt = new Optimizer();

        for (String bn : benchmarks) {
            String path = "/root/guoq_benchmarks/ion/" + bn;
            String qasm;
            try { qasm = new String(Files.readAllBytes(Paths.get(path))); }
            catch (Exception ex) { System.out.println(bn + ": missing"); continue; }

            CircuitDAG circuit;
            try { circuit = QASMToDAGVisitor.parse(qasm); }
            catch (Throwable t) { System.out.println(bn + ": parse fail"); continue; }

            // Count RXX(pi/2) sites for context (sites where this rule could even syntactically anchor).
            int rxxPiHalfCount = 0;
            for (String line : qasm.split("\n")) {
                String s = line.trim().toLowerCase();
                if (s.startsWith("rxx(pi/2)") || s.startsWith("rxx(1.5707") || s.startsWith("rxx(pi /2)")) rxxPiHalfCount++;
            }
            int totalGates = 0;
            for (String line : qasm.split("\n")) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("//") || s.startsWith("OPENQASM") ||
                    s.startsWith("include") || s.startsWith("qreg") || s.startsWith("creg")) continue;
                totalGates++;
            }

            // Apply the rule repeatedly. Each application returns a NEW DAG; loop until
            // the rule no longer matches. Cap iterations to avoid pathological loops.
            int matches = 0;
            CircuitDAG current = circuit;
            long benchStart = System.currentTimeMillis();
            for (int iter = 0; iter < 30; iter++) {
                if (System.currentTimeMillis() - benchStart > 60_000) break;
                CircuitDAG result;
                try {
                    // maxSymb=6 (our rule's natural middle is 4 gates; 6 gives buffer).
                    result = opt.symbolicMatchBeforeAfter(
                            current, r.getLHS(), r.getRHS(), 0, 6, r.getConstraint(), null);
                } catch (Throwable t) { result = null; }
                if (result == null) break;
                matches++;
                current = result;
            }
            long dt = System.currentTimeMillis() - benchStart;
            System.out.printf("%-22s %8d %8d %8d  (%.1fs)%n", bn, matches, rxxPiHalfCount, totalGates, dt/1000.0);
        }
        System.exit(0);
    }
}
