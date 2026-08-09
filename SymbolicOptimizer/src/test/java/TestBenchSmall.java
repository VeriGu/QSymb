import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import java.util.concurrent.*;
import ast.*;

public class TestBenchSmall {
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
        List<MatrixConstrainedRule> all = loadRules("/tmp/anchored_full.txt");
        Pattern qaoaShape = Pattern.compile("RXX q[01] q[01] [^|]*SYMB[^|]*RXX q[01] q[01]");
        List<MatrixConstrainedRule> gadget = new ArrayList<>();
        for (MatrixConstrainedRule r : all) {
            if (qaoaShape.matcher(r.getLHS()).find() || qaoaShape.matcher(r.getRHS()).find())
                gadget.add(r);
        }
        System.out.println("Total anchored: " + all.size() + ", gadget-shape: " + gadget.size());

        String[] benchmarks = {
            "/root/qsymb_benchmarks/ion/4gt11_83.qasm",
            "/root/qsymb_benchmarks/ion/mod5d1_63.qasm",
            "/root/qsymb_benchmarks/ion/4mod5-v1_22.qasm",
            "/root/qsymb_benchmarks/ion/ham3_102.qasm",
            "/root/qsymb_benchmarks/ion/ex1_226.qasm",
            "/root/qsymb_benchmarks/ion/qaoa_5.qasm",
        };

        Optimizer opt = new Optimizer();
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r); t.setDaemon(true); return t;
        });

        for (String b : benchmarks) {
            String qasm = new String(Files.readAllBytes(Paths.get(b)));
            CircuitDAG circuit;
            try { circuit = QASMToDAGVisitor.parse(qasm); }
            catch (Throwable t) { System.out.println(b + ": parse FAIL " + t); continue; }
            int hits = 0;
            List<Integer> matchedIdx = new ArrayList<>();
            for (int idx = 0; idx < gadget.size(); idx++) {
                final MatrixConstrainedRule r = gadget.get(idx);
                final CircuitDAG c = circuit;
                Future<CircuitDAG> f = exec.submit(() -> {
                    try { return opt.symbolicMatchBeforeAfter(c, r.getLHS(), r.getRHS(), 0, 25, r.getConstraint(), null); }
                    catch (Throwable t) { return null; }
                });
                try {
                    CircuitDAG result = f.get(10, TimeUnit.SECONDS);
                    if (result != null) { hits++; matchedIdx.add(idx); }
                } catch (TimeoutException te) {
                    f.cancel(true);
                }
            }
            String name = new File(b).getName();
            System.out.println(name + ": " + hits + "/" + gadget.size()
                    + (matchedIdx.isEmpty() ? "" : " — matched rules: " + matchedIdx));
        }
        System.exit(0);
    }
}
