import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IlpCompactor {

    private static final Pattern QUBIT_INDEX = Pattern.compile("q\\[(\\d+)\\]");

    private IlpCompactor() {
    }

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
            if (lines.size() < 2) {
                System.out.println("[ILP] skipped: " + lines.size()
                        + " gate line(s), body length " + body.length());
                return circuit;
            }
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
            pb.redirectOutput(procLog.toFile());
            Process p = pb.start();

            boolean finished = p.waitFor(
                    Params.ILP_TIME_LIMIT_SEC + 60L, TimeUnit.SECONDS);
            if (!finished) {
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
            }
        }
    }
}
