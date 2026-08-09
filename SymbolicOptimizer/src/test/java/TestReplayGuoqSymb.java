import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestReplayGuoqSymb {
    public static void main(String[] args) throws Exception {
        String benchmark = "qsymb_benchmarks/nam_rz/adr4_197.qasm";
        String seqFile = "guoq_adr4_rule_seq.txt";

        String circuitString = new String(Files.readAllBytes(Paths.get(benchmark)));
        CircuitDAG circuit = QASMToDAGVisitor.parse(circuitString);

        Optimizer optimizer = new Optimizer();
        Applier applier = new Applier(new Random(30), Params.MAX_QUBITS_SYMB);
        Random rand = new Random(30);

        List<String> lines = Files.readAllLines(Paths.get(seqFile));

        int total = 0, concrete = 0, symbolic = 0;
        int cFired = 0, cNoop = 0;
        int sFired = 0, sNoop = 0, sParseErr = 0;
        int crossTotal = 0, crossFired = 0;
        int start2q = circuit.twoQGateCount();
        int best2q = start2q;

        System.out.println("START 2q=" + start2q + " total=" + circuit.totalGateCount()
                + "  steps=" + lines.size());

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            total++;

            if (line.contains("symb ")) {
                symbolic++;
                String[] f = line.split("\\s\\|\\s");
                if (f.length < 3) { sParseErr++; continue; }
                String result = f[0].trim();
                String pattern = f[1].trim();
                String basisStr = f[2].trim();
                if (basisStr.startsWith("[")) basisStr = basisStr.substring(1);
                if (basisStr.endsWith("]")) basisStr = basisStr.substring(0, basisStr.length() - 1);

                boolean cross = isCrossQubit(pattern);
                if (cross) crossTotal++;

                List<Map<boolean[], boolean[]>> constraints;
                try {
                    constraints = applier.parseConstraints(basisStr);
                } catch (Exception e) {
                    sParseErr++;
                    continue;
                }

                CircuitDAG res;
                try {
                    res = optimizer.symbolicMatchBeforeAfterMono(
                            circuit, pattern, result, 1, 40, constraints, null);
                } catch (Exception e) {
                    sParseErr++;
                    continue;
                }

                if (res != null && res != circuit) {
                    sFired++;
                    if (cross) crossFired++;
                    circuit = res;
                    int now = circuit.twoQGateCount();
                    if (now < best2q) best2q = now;
                } else {
                    sNoop++;
                }
                continue;
            }

            concrete++;
            int sep = line.indexOf(" | ");
            if (sep < 0) { continue; }
            String left = line.substring(0, sep).trim();
            String right = line.substring(sep + 3).trim();
            CircuitDAG candidate;
            try {
                CircuitDAG pat = QASMToDAGVisitor.parse(right);
                candidate = optimizer.applyRule(circuit, pat, left, true, rand);
            } catch (Exception e) {
                continue;
            }
            if (candidate != circuit) {
                cFired++;
                circuit = candidate;
                int now = circuit.twoQGateCount();
                if (now < best2q) best2q = now;
            } else {
                cNoop++;
            }
        }

        int end2q = circuit.twoQGateCount();
        System.out.println("=== REPLAY (concrete + symbolic) DONE ===");
        System.out.println("total lines        : " + total);
        System.out.println("concrete rules     : " + concrete + "  (fired " + cFired + ", no-op " + cNoop + ")");
        System.out.println("symbolic rules     : " + symbolic);
        System.out.println("  symb fired       : " + sFired);
        System.out.println("  symb no-op       : " + sNoop);
        System.out.println("  symb parse err   : " + sParseErr);
        if (symbolic - sParseErr > 0)
            System.out.printf("  symb apply rate  : %.1f%%%n",
                    100.0 * sFired / (symbolic - sParseErr));
        System.out.println("cross-qubit merges : " + crossTotal + "  (fired " + crossFired + ")");
        System.out.println("start 2q           : " + start2q);
        System.out.println("end   2q           : " + end2q);
        System.out.println("best  2q (replay)  : " + best2q);
        System.out.println("guoq best 2q       : 1013");
    }

    static boolean isCrossQubit(String pattern) {
        java.util.Set<String> rzQubits = new java.util.HashSet<>();
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("rz\\([^)]*\\)\\s+(q\\d+)").matcher(pattern);
        while (m.find()) rzQubits.add(m.group(1));
        return rzQubits.size() >= 2;
    }
}
