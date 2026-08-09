import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

public class TestGadgetMatch {

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

    public static void main(String[] args) throws Exception {
        String gadgetQasm = "OPENQASM 2.0;\ninclude \"qelib1.inc\";\nqreg q[2];\ncreg c[2];\n"
                + "rxx(pi/2) q[0],q[1];\n"
                + "rx(-pi/2) q[0];\n"
                + "rz(5.7817631) q[0];\n"
                + "rz(-pi/2) q[0];\n"
                + "ry(pi/2) q[0];\n"
                + "rx(pi) q[0];\n"
                + "rz(-pi/2) q[0];\n"
                + "rz(pi) q[0];\n"
                + "rz(-pi/2) q[0];\n"
                + "ry(pi/2) q[0];\n"
                + "rx(pi) q[0];\n"
                + "rz(-pi/2) q[0];\n"
                + "rz(3*pi) q[0];\n"
                + "rx(-pi/2) q[1];\n"
                + "ry(-pi/2) q[1];\n"
                + "ry(pi/2) q[1];\n"
                + "rxx(pi/2) q[0],q[1];\n";
        Files.write(Paths.get("/tmp/qaoa_gadget_inst.qasm"), gadgetQasm.getBytes());
        System.out.println("Gadget written to /tmp/qaoa_gadget_inst.qasm (17 gates, 2 RXX)");

        List<MatrixConstrainedRule> rules = loadRules("/root/anchored_ion_q3_only.txt");
        System.out.println("Loaded " + rules.size() + " anchored rules");

        Pattern qaoaShape = Pattern.compile(
                "RXX q[01] q[01] [^|]*SYMB[^|]*RXX q[01] q[01]");
        List<MatrixConstrainedRule> gadgetRules = new ArrayList<>();
        for (MatrixConstrainedRule r : rules) {
            if (qaoaShape.matcher(r.getLHS()).find()
                    || qaoaShape.matcher(r.getRHS()).find()) {
                gadgetRules.add(r);
            }
        }
        System.out.println("QAOA-shape gadget rules: " + gadgetRules.size());

        Optimizer opt = new Optimizer();
        CircuitDAG circuit = QASMToDAGVisitor.parse(gadgetQasm);

        int matches = 0;
        for (int i = 0; i < gadgetRules.size(); i++) {
            MatrixConstrainedRule r = gadgetRules.get(i);
            String lhs = r.getLHS();
            String shortLhs = lhs.length() > 100 ? lhs.substring(0, 100) + "..." : lhs;
            try {
                CircuitDAG result = opt.symbolicMatchBeforeAfter(
                        circuit, r.getLHS(), r.getRHS(), 0, 25, r.getConstraint(), null);
                String tag = (result != null) ? "✅ MATCH" : "❌ no-match";
                System.out.println(String.format("  rule#%-3d %s | LHS: %s", i, tag, shortLhs));
                if (result != null) matches++;
            } catch (Throwable e) {
                System.out.println(String.format("  rule#%-3d EXC %s | LHS: %s",
                        i, e.getClass().getSimpleName(), shortLhs));
            }
        }
        System.out.println("\n=== " + matches + "/" + gadgetRules.size() + " rules matched the QAOA-5 gadget ===");
    }
}
