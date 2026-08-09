import java.io.*;
import java.util.*;
import java.util.regex.*;
import ast.*;

public class TestNamSymbApply {

    static List<MatrixConstrainedRule> loadRules(String path) throws Exception {
        List<MatrixConstrainedRule> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
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

    static MatrixConstrainedRule findByLhs(List<MatrixConstrainedRule> rules, String lhsPrefix) {
        for (MatrixConstrainedRule r : rules)
            if (r.getLHS().startsWith(lhsPrefix)) return r;
        return null;
    }

    static void runCase(Optimizer opt, MatrixConstrainedRule r, String qasm, String name) {
        System.out.println("\n=== CASE: " + name + " ===");
        System.out.println("rule LHS: " + r.getLHS());
        System.out.println("rule RHS: " + r.getRHS());
        System.out.println("circuit:\n" + qasm);
        try {
            CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
            CircuitDAG result = opt.symbolicMatchBeforeAfter(
                    circuit, r.getLHS(), r.getRHS(), 1, 5, r.getConstraint(), null);
            if (result != null) {
                System.out.println("MATCHED. result:\n" + result.toQASM());
            } else {
                System.out.println("NO MATCH (bug suspected)");
            }
            if (r.getType().contains("birewrite")) {
                CircuitDAG rev = opt.symbolicMatchBeforeAfter(
                        circuit, r.getRHS(), r.getLHS(), 1, 5, r.getConstraint(), null);
                System.out.println("reverse direction: " + (rev != null ? "MATCHED:\n" + rev.toQASM() : "no match"));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        List<MatrixConstrainedRule> rules = loadRules("/root/anchored_nam_q3.txt");
        System.out.println("Loaded " + rules.size() + " rules");
        Optimizer opt = new Optimizer();

        String hdr = "OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[2];\ncreg c[2];\n";

        MatrixConstrainedRule r1 = findByLhs(rules,
                "(Cons (SYMB 2) (Cons (RZ q0 (Symbol \"pi\")) (Cons (RZ q1 (Symbol \"pi\")) c)))");
        if (r1 == null) { System.out.println("case1 rule not found"); }
        else runCase(opt, r1, hdr
                + "cx q[0],q[1];\ncx q[0],q[1];\nrz(pi) q[0];\nrz(pi) q[1];\n",
                "disconnected after-fragment, 2-wire window");

        if (r1 != null) runCase(opt, r1, hdr
                + "rz(0.5) q[0];\nrz(pi) q[0];\nrz(pi) q[1];\n",
                "1-wire window, secondary wire uncovered (expect no match)");

        MatrixConstrainedRule r2 = findByLhs(rules,
                "(Cons (CX q1 q0) (Cons (SYMB 2) (Cons (CX q0 q1) c)))");
        if (r2 == null) { System.out.println("case2 rule not found"); }
        else runCase(opt, r2, hdr
                + "cx q[1],q[0];\ncx q[0],q[1];\ncx q[1],q[0];\ncx q[0],q[1];\n",
                "SWAP-fragment S inside cx sandwich");

        System.exit(0);
    }
}
