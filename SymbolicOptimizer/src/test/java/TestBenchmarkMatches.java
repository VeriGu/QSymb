import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import ast.*;

/**
 * Sweep: for each ion benchmark, try every anchored symbolic rule and report
 * which rules match somewhere in the circuit. Outputs a per-benchmark + per-
 * rule match matrix.
 */
public class TestBenchmarkMatches {

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
        List<MatrixConstrainedRule> rules = loadRules("/root/anchored_ion_q3_only.txt");
        System.out.println("Loaded " + rules.size() + " anchored symbolic rules");

        File dir = new File("/root/guoq_benchmarks/ion");
        File[] files = dir.listFiles((f, n) -> n.endsWith(".qasm"));
        if (files == null) { System.err.println("no benchmarks"); return; }
        Arrays.sort(files, Comparator.comparing(File::getName));

        Optimizer opt = new Optimizer();
        Map<String, Integer> ruleMatchCount = new HashMap<>();
        Map<String, Set<String>> benchHits = new LinkedHashMap<>();

        // Per-benchmark/per-rule cap: 5s wall time max per match attempt
        int timeoutMs = 5000;

        // Subsample rules to keep total runtime bounded: take first 100
        int RULE_CAP = Math.min(rules.size(), 100);

        // Subsample benchmarks: every 4th
        List<File> picks = new ArrayList<>();
        for (int i = 0; i < files.length; i += 4) picks.add(files[i]);

        for (File f : picks) {
            String qasm;
            try { qasm = new String(Files.readAllBytes(f.toPath())); }
            catch (Exception e) { continue; }
            CircuitDAG circuit;
            try { circuit = QASMToDAGVisitor.parse(qasm); }
            catch (Throwable t) { continue; }
            int hits = 0;
            List<Integer> matchedIdx = new ArrayList<>();
            long bench_t0 = System.currentTimeMillis();
            for (int idx = 0; idx < RULE_CAP; idx++) {
                MatrixConstrainedRule r = rules.get(idx);
                long t0 = System.currentTimeMillis();
                Thread t = new Thread(() -> {});
                CircuitDAG result;
                try {
                    result = opt.symbolicMatchBeforeAfter(
                            circuit, r.getLHS(), r.getRHS(), 0, 25, r.getConstraint(), null);
                } catch (Throwable th) { result = null; }
                long dt = System.currentTimeMillis() - t0;
                if (result != null) {
                    hits++;
                    matchedIdx.add(idx);
                    ruleMatchCount.merge("rule#" + idx, 1, Integer::sum);
                }
                if (System.currentTimeMillis() - bench_t0 > 60_000) break; // per-bench cap
            }
            String name = f.getName();
            benchHits.put(name, new LinkedHashSet<>(matchedIdx.stream().map(i -> "rule#"+i).toList()));
            System.out.println(name + ": " + hits + " rule matches (of " + RULE_CAP + " tried)"
                    + (matchedIdx.isEmpty() ? "" : " — first: rule#" + matchedIdx.get(0)));
        }

        System.out.println("\n=== Most-matching rules (top 10) ===");
        ruleMatchCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue() + " benchmarks"));

        long benchesAnyMatch = benchHits.values().stream().filter(s -> !s.isEmpty()).count();
        System.out.println("\n=== Coverage ===");
        System.out.println("  benchmarks with at least 1 rule matching: " + benchesAnyMatch + "/" + benchHits.size());
    }
}
