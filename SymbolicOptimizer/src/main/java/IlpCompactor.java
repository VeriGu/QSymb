import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ILP-based circuit compaction.
 *
 * <p>Shells out to {@code scripts/ilp_compact.py}, which reuses Quasar's
 * {@code dag.linearized_circuit_from_dag} (the MinLA ILP at the heart of
 * Quasar's {@code _run_ilp}). The ILP re-linearizes the circuit DAG into a
 * topological order that minimizes dependency stretch, which tends to bring
 * dependent gates closer together so downstream rewriting/chunking can match
 * larger patterns. It is gate-set agnostic and never changes gate count.
 *
 * <p>The wrapper returns a permutation of the input gates (not QASM), so the
 * caller's exact angle representation is preserved across the round trip.
 * Any failure (missing interpreter, parse error, timeout, ...) is swallowed
 * and the original circuit is returned unchanged.
 */
public final class IlpCompactor {

    private static final Pattern QUBIT_INDEX = Pattern.compile("q\\[(\\d+)\\]");

    private IlpCompactor() {
    }

    /**
     * Returns an ILP-compacted copy of {@code circuit}, or {@code circuit}
     * itself if compaction is not possible.
     */
    public static CircuitDAG compact(CircuitDAG circuit) {
        if (circuit == null) {
            System.out.println("[ILP] skipped: null circuit");
            return circuit;
        }
        Path inFile = null;
        Path outFile = null;
        Path procLog = null;
        try {
            String body = circuit.toQASM();

            List<String> lines = new ArrayList<>();
            for (String raw : body.split("\\R")) {
                String line = raw.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            // Nothing to reorder for trivial circuits.
            if (lines.size() < 2) {
                System.out.println("[ILP] skipped: " + lines.size()
                        + " gate line(s), body length " + body.length());
                return circuit;
            }
            // The MinLA ILP is O(n^2) in gate count; skip circuits too large
            // for it to ever finish (see Params.ILP_MAX_GATES).
            if (lines.size() > Params.ILP_MAX_GATES) {
                System.out.println("[ILP] skipped: " + lines.size()
                        + " gates exceeds ILP_MAX_GATES=" + Params.ILP_MAX_GATES);
                return circuit;
            }

            int maxQubit = -1;
            Matcher m = QUBIT_INDEX.matcher(body);
            while (m.find()) {
                maxQubit = Math.max(maxQubit, Integer.parseInt(m.group(1)));
            }
            if (maxQubit < 0) {
                System.out.println("[ILP] skipped: no q[i] qubit token in body; "
                        + "first line: " + lines.get(0));
                return circuit;
            }

            StringBuilder qasm = new StringBuilder();
            qasm.append("OPENQASM 2.0;\n");
            qasm.append("include \"qelib1.inc\";\n");
            qasm.append("qreg q[").append(maxQubit + 1).append("];\n");
            for (String line : lines) {
                qasm.append(line).append('\n');
            }

            inFile = Files.createTempFile("ilp_in", ".qasm");
            outFile = Files.createTempFile("ilp_out", ".perm");
            procLog = Files.createTempFile("ilp_log", ".txt");
            Files.writeString(inFile, qasm.toString());

            ProcessBuilder pb = new ProcessBuilder(
                    Params.ILP_PYTHON,
                    Params.ILP_SCRIPT,
                    inFile.toString(),
                    outFile.toString(),
                    String.valueOf(Params.ILP_TIME_LIMIT_SEC));
            pb.redirectErrorStream(true);
            // Redirect output to a file rather than draining it in-process.
            // A blocking stdout drain would sit ahead of waitFor() and make
            // the timeout below unreachable when the subprocess hangs.
            pb.redirectOutput(procLog.toFile());
            Process p = pb.start();

            // Hard wall-clock bound on the whole subprocess: the solver budget
            // plus slack for process startup / qiskit import.
            boolean finished = p.waitFor(
                    Params.ILP_TIME_LIMIT_SEC + 60L, TimeUnit.SECONDS);
            if (!finished) {
                // Kill the python process AND its solver children (CBC),
                // otherwise they orphan and keep burning CPU/RAM.
                p.descendants().forEach(ProcessHandle::destroyForcibly);
                p.destroyForcibly();
                p.waitFor(10, TimeUnit.SECONDS);
                System.err.println("ILP compaction timed out after "
                        + (Params.ILP_TIME_LIMIT_SEC + 60L)
                        + "s; keeping circuit.");
                return circuit;
            }
            if (p.exitValue() != 0) {
                String procOut = Files.exists(procLog)
                        ? Files.readString(procLog).trim() : "";
                System.err.println("ILP compaction failed (exit "
                        + p.exitValue() + "): " + procOut);
                return circuit;
            }

            List<Integer> perm = new ArrayList<>();
            for (String l : Files.readAllLines(outFile)) {
                String t = l.trim();
                if (!t.isEmpty()) {
                    perm.add(Integer.parseInt(t));
                }
            }

            // The permutation must cover exactly the input gates.
            if (perm.size() != lines.size()) {
                System.err.println("ILP compaction size mismatch ("
                        + perm.size() + " vs " + lines.size()
                        + "); keeping circuit.");
                return circuit;
            }
            boolean[] seen = new boolean[lines.size()];
            for (int idx : perm) {
                if (idx < 0 || idx >= lines.size() || seen[idx]) {
                    System.err.println("ILP compaction returned an invalid "
                            + "permutation; keeping circuit.");
                    return circuit;
                }
                seen[idx] = true;
            }

            StringBuilder reordered = new StringBuilder();
            int moved = 0;
            for (int k = 0; k < perm.size(); k++) {
                int idx = perm.get(k);
                if (idx != k) {
                    moved++;
                }
                reordered.append(lines.get(idx)).append('\n');
            }
            System.out.println("ILP compaction: " + lines.size()
                    + " gates, " + moved + " moved");
            return QASMToDAGVisitor.parse(reordered.toString());
        } catch (Exception e) {
            System.err.println("ILP compaction error: " + e);
            return circuit;
        } finally {
            try {
                if (inFile != null) {
                    Files.deleteIfExists(inFile);
                }
                if (outFile != null) {
                    Files.deleteIfExists(outFile);
                }
                if (procLog != null) {
                    Files.deleteIfExists(procLog);
                }
            } catch (Exception ignored) {
                // best-effort temp cleanup
            }
        }
    }
}
