import java.io.*;
import java.util.*;
import java.util.regex.*;
import ast.*;

/**
 * Regression test for the branching primary-fragment bug: the chain walk in
 * matchBeforeAfter follows a single successor path, so a connected-but-
 * branching before-fragment (cx q0,q1; h q0; rz(pi) q1; cx q2,q1) was
 * silently partially matched, causing either a subList crash in
 * buildSymbolicRhs or a malformed RHS with an out-of-range qubit.
 * Expected with the fix: no exception and no match (conservative reject).
 */
public class TestBranchingFragment {

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
                    String c = mm.substring("Matrix(".length(), mm.length() - 1);
                    String[] e = c.split(";");
                    List<SymbolicSolve.SparseMatrix.MatrixEntry> ents = new ArrayList<>();
                    for (int i = 2; i < e.length; i++) {
                        String entry = e[i].substring(1, e[i].length() - 1);
                        String[] el = entry.split(",");
                        ents.add(new SymbolicSolve.SparseMatrix.MatrixEntry(
                                Integer.valueOf(el[0].trim()),
                                Integer.valueOf(el[1].trim()),
                                SymbolicSolve.parseComplex(el[2])));
                    }
                    mlist.add(new SymbolicSolve.SparseMatrix(
                            Integer.valueOf(e[0]), Integer.valueOf(e[1]), ents));
                }
                out.add(new MatrixConstrainedRule(p[0], p[1], mlist, p[2]));
            }
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        List<MatrixConstrainedRule> rules = loadRules("/root/anchored_nam_q3.txt");
        Optimizer opt = new Optimizer();
        String prefix = "(Cons (CX q0 q1) (Cons (H q0) (Cons (RZ q1 (Real 3.1415926535897930)) (Cons (CX q2 q1) (Cons (SYMB 2)";
        int failures = 0;
        int tested = 0;
        String hdr = "OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[3];\ncreg c[3];\n";
        // Circuit embeds the branching before-fragment, an identity window
        // (cx;cx), then x q0 — the shape that used to partially match.
        String qasm = hdr
                + "cx q[0],q[1];\nh q[0];\nrz(pi) q[1];\ncx q[2],q[1];\n"
                + "cx q[0],q[1];\ncx q[0],q[1];\nx q[0];\n";
        for (MatrixConstrainedRule r : rules) {
            if (!r.getLHS().startsWith(prefix)) continue;
            tested++;
            System.out.println("--- rule LHS: " + r.getLHS());
            try {
                CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
                CircuitDAG res = opt.symbolicMatchBeforeAfter(
                        circuit, r.getLHS(), r.getRHS(), 1, 10, r.getConstraint(), null);
                if (res == null) {
                    System.out.println("    no match (OK: conservative reject, no crash)");
                } else {
                    String out = res.toQASM();
                    System.out.println("    MATCHED, result:\n" + out);
                    // a sound match must not reference qubits outside the register
                    if (out.matches("(?s).*q\\[[3-9]\\].*")) {
                        System.out.println("    FAIL: result references out-of-range qubit");
                        failures++;
                    }
                }
            } catch (Throwable e) {
                System.out.println("    FAIL: exception escaped matcher: " + e);
                e.printStackTrace();
                failures++;
            }
        }
        System.out.println("\nTested " + tested + " branching-fragment rules, failures=" + failures);
        System.exit(failures == 0 && tested > 0 ? 0 : 1);
    }
}
