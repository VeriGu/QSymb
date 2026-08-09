import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

public class TestHamSnippet {

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
                        String[] el = s.split(",");
                        ents.add(new SymbolicSolve.SparseMatrix.MatrixEntry(
                                Integer.valueOf(el[0].trim()), Integer.valueOf(el[1].trim()),
                                SymbolicSolve.parseComplex(el[2])));
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
                + "rx(-pi/2) q[2];\n"
                + "ry(-pi/2) q[2];\n"
                + "ry(pi/2) q[2];\n"
                + "rxx(pi/2) q[2],q[1];\n";
        System.out.println("Snippet: ham3_102 lines 56-61");
        System.out.println(qasm.replace("OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[3];\ncreg c[3];\n", ""));

        List<MatrixConstrainedRule> all = loadRules("/root/anchored_ion_q3_only.txt");
        Pattern qaoaShape = Pattern.compile("RXX q[01] q[01] [^|]*SYMB[^|]*RXX q[01] q[01]");
        List<MatrixConstrainedRule> gadget = new ArrayList<>();
        for (MatrixConstrainedRule r : all) {
            if (qaoaShape.matcher(r.getLHS()).find()) gadget.add(r);
        }
        System.out.println("\nTrying " + gadget.size() + " same-pair gadget rules:\n");

        Optimizer opt = new Optimizer();
        CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);

        int matches = 0;
        for (int i = 0; i < gadget.size(); i++) {
            MatrixConstrainedRule r = gadget.get(i);
            String lhs = r.getLHS();
            String dispL = lhs.length() > 110 ? lhs.substring(0, 110) + "..." : lhs;
            try {
                CircuitDAG result = opt.symbolicMatchBeforeAfter(
                        circuit, r.getLHS(), r.getRHS(), 0, 25, r.getConstraint(), null);
                String tag = (result != null) ? "✅ MATCH " : "❌ no-match";
                System.out.println(String.format("rule#%-3d %s | %s", i, tag, dispL));
                if (result != null) matches++;
            } catch (Throwable t) {
                System.out.println(String.format("rule#%-3d EXC %s | %s",
                        i, t.getClass().getSimpleName(), dispL));
            }
        }
        System.out.println("\n=== " + matches + "/" + gadget.size() + " rules matched ham3_102 snippet ===");
    }
}
