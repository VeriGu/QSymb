import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

// Replay guoq's winning rule sequence through OUR applyRule, egglog OFF.
// Answers: do the rules guoq applied also fire in our matcher? Rules out a
// rule-applying / matcher problem vs a search-throughput problem.
public class TestReplayGuoq {
    public static void main(String[] args) throws Exception {
        String benchmark = "guoq_benchmarks/nam_rz/adr4_197.qasm";
        String seqFile = "guoq_adr4_rule_seq.txt";

        String circuitString = new String(Files.readAllBytes(Paths.get(benchmark)));
        CircuitDAG circuit = QASMToDAGVisitor.parse(circuitString);

        Optimizer optimizer = new Optimizer();
        Random rand = new Random(30);

        List<String> lines = Files.readAllLines(Paths.get(seqFile));

        int total = 0, concrete = 0, symbolic = 0, fired = 0, noop = 0, parseErr = 0;
        int start2q = circuit.twoQGateCount();
        int best2q = start2q;

        System.out.println("START 2q=" + start2q + " total=" + circuit.totalGateCount()
                + "  steps=" + lines.size());

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            total++;

            // Symbolic rules carry a basis matrix as a 3rd " | " field and contain "symb".
            if (line.contains("symb ")) {
                symbolic++;
                continue;
            }
            concrete++;

            int sep = line.indexOf(" | ");
            if (sep < 0) { parseErr++; continue; }
            String left = line.substring(0, sep).trim();   // RESULT / RHS
            String right = line.substring(sep + 3).trim();  // PATTERN / LHS

            CircuitDAG candidate;
            try {
                CircuitDAG pattern = QASMToDAGVisitor.parse(right);
                candidate = optimizer.applyRule(circuit, pattern, left, true, rand);
            } catch (Exception e) {
                parseErr++;
                continue;
            }

            if (candidate != circuit) {
                fired++;
                circuit = candidate;
                int now = circuit.twoQGateCount();
                if (now < best2q) best2q = now;
            } else {
                noop++;
            }
        }

        int end2q = circuit.twoQGateCount();
        System.out.println("=== REPLAY DONE ===");
        System.out.println("total lines      : " + total);
        System.out.println("concrete rules   : " + concrete);
        System.out.println("symbolic rules   : " + symbolic + " (skipped, different path)");
        System.out.println("parse errors     : " + parseErr);
        System.out.println("fired (changed)  : " + fired);
        System.out.println("no-op (no match) : " + noop);
        if (concrete - parseErr > 0)
            System.out.printf("apply rate       : %.1f%%%n",
                    100.0 * fired / (concrete - parseErr));
        System.out.println("start 2q         : " + start2q);
        System.out.println("end   2q         : " + end2q);
        System.out.println("best  2q (replay): " + best2q);
        System.out.println("guoq best 2q     : 1013");
    }
}
