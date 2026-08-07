import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

public class Test4gt11Full {
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
        String qasm = new String(Files.readAllBytes(Paths.get("/root/qsymb_benchmarks/ion/4gt11_83.qasm")));
        CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
        List<MatrixConstrainedRule> rules = loadRules("/tmp/rule_4gt11.txt");
        MatrixConstrainedRule r = rules.get(0);
        Optimizer opt = new Optimizer();
        long t0 = System.currentTimeMillis();
        CircuitDAG result = opt.symbolicMatchBeforeAfter(
                circuit, r.getLHS(), r.getRHS(), 0, 25, r.getConstraint(), null);
        long dt = System.currentTimeMillis() - t0;
        System.out.println("\n=== " + (result != null ? "MATCH" : "NO MATCH") + " after " + dt + " ms ===");
        System.exit(0);
    }
}
