import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

/**
 * Test the newly-constructed canonical rule
 *     LHS: RXX(pi/2); SYMB
 *     RHS: SYMB; RZZ(-pi/2)
 *  against the 4gt11_83 lines 14-19 fragment. Confirms that
 *  symbolicMatchBeforeAfter finds the match (which requires the matcher to
 *  pass its basis check on the 4-gate middle).
 */
public class TestNewRule4gt11 {

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
                        int firstComma = s.indexOf(',');
                        int secondComma = s.indexOf(',', firstComma + 1);
                        int r = Integer.parseInt(s.substring(0, firstComma).trim());
                        int col = Integer.parseInt(s.substring(firstComma + 1, secondComma).trim());
                        String val = s.substring(secondComma + 1).trim();
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
        String qasm = "OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[3];\ncreg c[3];\n"
                + "rxx(pi/2) q[2],q[1];\n"
                + "rx(-pi/2) q[1];\n"
                + "ry(pi/2) q[1];\n"
                + "rx(-pi/2) q[2];\n"
                + "ry(-pi/2) q[2];\n"
                + "rxx(pi/2) q[1],q[2];\n";

        System.out.println("Fragment (4gt11_83 lines 14-19):");
        System.out.println(qasm.replaceFirst("(?s).*creg c\\[3\\];\n", ""));

        List<MatrixConstrainedRule> rules = loadRules("/tmp/rule_4gt11.txt");
        System.out.println("Loaded " + rules.size() + " rule(s) from /tmp/rule_4gt11.txt");
        if (rules.isEmpty()) { System.err.println("FAIL: no rules"); System.exit(1); }
        MatrixConstrainedRule r = rules.get(0);
        System.out.println("LHS: " + r.getLHS());
        System.out.println("RHS: " + r.getRHS());
        System.out.println("basis matrices: " + r.getConstraint().size());

        Optimizer opt = new Optimizer();
        CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
        System.out.println("Circuit parsed");

        long t0 = System.currentTimeMillis();
        CircuitDAG result = opt.symbolicMatchBeforeAfter(
                circuit, r.getLHS(), r.getRHS(), 0, 25, r.getConstraint(), null);
        long dt = System.currentTimeMillis() - t0;

        System.out.println();
        if (result != null) {
            System.out.println("MATCH after " + dt + " ms");
            System.out.println("Result circuit:");
            System.out.println(result.toQASM());
        } else {
            System.out.println("NO MATCH after " + dt + " ms");
        }
    }
}
