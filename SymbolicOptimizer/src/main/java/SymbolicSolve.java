
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ast.BinOp;
import ast.Expr;
import ast.Expr.Op;
import ast.Real;
import ast.Symbol;
import ast.UnOp;

public class SymbolicSolve {

    /**
     * Represents a complex number with real and imaginary parts.
     */
    public static class Complex {
        
        public final String symbolicValue;

        // Constructor for numeric values
       
        // Constructor for symbolic values
        public Complex(String symbolic) {
            this.symbolicValue = symbolic;
        }

        @Override
        public String toString() {
            return symbolicValue;
        }
    }

    /**
     * Represents a sparse matrix using a list of non-zero entries (COO format).
     */
    public static class SparseMatrix {
        public final int rows;
        public final int cols;
        public final List<MatrixEntry> entries;

        public static class MatrixEntry {
            public final int row;
            public final int col;
            public final Complex value;

            public MatrixEntry(int row, int col, Complex value) {
                this.row = row;
                this.col = col;
                this.value = value;
            }

            @Override
            public String toString() {
                return String.format("(%d, %d, %s)", row, col, value);
            }

            public String toJson() {
                return String.format("{\"row\": %d, \"col\": %d, \"value\": \"%s\"}", row, col, value.toString());
            }
        }

        public SparseMatrix(int rows, int cols, List<MatrixEntry> entries) {
            this.rows = rows;
            this.cols = cols;
            this.entries = entries;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Matrix(");
            sb.append(rows + ";");
            sb.append(cols + ";");
            String entriesString = entries.stream().map(MatrixEntry::toString).collect(Collectors.joining(";"));
            sb.append(entriesString);
            sb.append(")");
            return sb.toString();
        }

        public String to_json_string() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append(String.format("\"rows\": %d, \"cols\": %d, ", rows, cols));
            sb.append("\"entries\": [");
            String entriesJson = entries.stream()
                                        .map(MatrixEntry::toJson)
                                        .collect(Collectors.joining(","));
            sb.append(entriesJson);
            sb.append("]");
            sb.append("}");
            return sb.toString();
        }
    }
    private static final String[] ANGLES = {"theta1", "theta2", "theta3"};
    private Map<String, Double> symbolMap;
    private Random rand;
    public SymbolicSolve (Random rand) {
        this.rand = rand;
        this.symbolMap = getSymbolMap();
    }

    public SymbolicSolve (Map<String, Double> symbolMap) {
        this.symbolMap = symbolMap;
    }

    public Map<String, Double> getSymbolMap() {
        HashMap<String, Double> symbolMap = new HashMap<>();
        symbolMap.put(Symbolic.S_PHI, rand.nextDouble());
        for (String angle : ANGLES) {
        symbolMap.put(angle, rand.nextDouble());
        }

        return symbolMap;
    }


    private  String gateToJson(EggGen.Gate gate) {
        String gateName;
        List<Integer> targets = new ArrayList<>();
        String paramsJson = null;

        if (gate instanceof EggGen.X) {
            gateName = "x";
            targets.add(parseQubit(((EggGen.X) gate).qubit));
        } else if (gate instanceof EggGen.H) {
            gateName = "h";
            targets.add(parseQubit(((EggGen.H) gate).qubit));
        } else if (gate instanceof EggGen.SX) {
            gateName = "sx";
            targets.add(parseQubit(((EggGen.SX) gate).qubit));
        } else if (gate instanceof EggGen.CX) {
            gateName = "cx";
            targets.add(parseQubit(((EggGen.CX) gate).control));
            targets.add(parseQubit(((EggGen.CX) gate).target));
        } else if (gate instanceof EggGen.CZ) {
            gateName = "cz";
            targets.add(parseQubit(((EggGen.CZ) gate).control));
            targets.add(parseQubit(((EggGen.CZ) gate).target));
        } else if (gate instanceof EggGen.RZ) {
            gateName = "rz";
            targets.add(parseQubit(((EggGen.RZ) gate).qubit));
            paramsJson = paramToJson("gamma", ((EggGen.RZ) gate).angle);
        } else if (gate instanceof EggGen.RX) {
            gateName = "rx";
            targets.add(parseQubit(((EggGen.RX) gate).qubit));
            paramsJson = paramToJson("theta1", ((EggGen.RX) gate).angle);
        } else if (gate instanceof EggGen.RY) {
            gateName = "ry";
            targets.add(parseQubit(((EggGen.RY) gate).qubit));
            paramsJson = paramToJson("theta1", ((EggGen.RY) gate).angle);
        } else if (gate instanceof EggGen.U1) {
            gateName = "u1";
            targets.add(parseQubit(((EggGen.U1) gate).qubit));
            paramsJson = paramToJson("lam", ((EggGen.U1) gate).lambda);
        } else if (gate instanceof EggGen.U2) {
            gateName = "u2";
            targets.add(parseQubit(((EggGen.U2) gate).qubit));
            paramsJson = paramsToJson(
                new String[]{"phi", "lam"},
                new Expr[]{((EggGen.U2) gate).phi, ((EggGen.U2) gate).lambda}
            );
        } else if (gate instanceof EggGen.U3) {
            gateName = "u3";
            targets.add(parseQubit(((EggGen.U3) gate).qubit));
            paramsJson = paramsToJson(
                new String[]{"theta1", "phi", "lam"},
                new Expr[]{((EggGen.U3) gate).theta, ((EggGen.U3) gate).phi, ((EggGen.U3) gate).lambda}
            );
        } else if (gate instanceof EggGen.RXX) {
            gateName = "rxx";
            targets.add(parseQubit(((EggGen.RXX) gate).qubit1));
            targets.add(parseQubit(((EggGen.RXX) gate).qubit2));
            paramsJson = paramToJson("theta1", ((EggGen.RXX) gate).angle);
        } else if (gate instanceof EggGen.SYMB){
            gateName = "symb";
            targets.add(0);
        } else {
          return null;
        }

        if (paramsJson == null && (gate instanceof EggGen.RZ || gate instanceof EggGen.RX || gate instanceof EggGen.RY || gate instanceof EggGen.U1 || gate instanceof EggGen.U2 || gate instanceof EggGen.U3 || gate instanceof EggGen.RXX)) {
            return null;
        }

        String targetsJson = "[" + targets.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
        
        if (paramsJson != null) {
            return String.format("{\"gate\":\"%s\",\"targets\":%s,\"params\":%s}", gateName, targetsJson, paramsJson);
        } else {
            return String.format("{\"gate\":\"%s\",\"targets\":%s}", gateName, targetsJson);
        }
    }

    public String circuitToJson(List<EggGen.Gate> gates, int num_qubits) {
        return "{\"gates\":[" + gates.stream()
                .map(this::gateToJson)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.joining(",")) + "], \"n_qubits\":" + num_qubits + "}";
    }


    // ---- persistent semantics.py server pool ----
    // Pool of long-lived `python3 semantics.py --server` processes. Each slot
    // serializes its own request/response so multiple threads in the JVM can
    // make concurrent solveSymb / checkBig / etc. calls -- one per slot at a
    // time. Each slot independently restarts itself every SERVER_RESTART_INTERVAL
    // requests to bound sympy global-state accumulation.
    private static final int POOL_SIZE = Math.max(1,
            Integer.parseInt(System.getProperty("semantics.pool.size",
                    Integer.toString(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)))));
    private static final int SERVER_RESTART_INTERVAL = 500;
    private static final java.util.concurrent.BlockingQueue<ServerSlot> pool =
            new java.util.concurrent.LinkedBlockingQueue<>();
    private static volatile boolean poolInitialized = false;
    private static final Object poolInitLock = new Object();

    private static final class ServerSlot {
        final int id;
        Process proc;
        java.io.BufferedWriter in;
        BufferedReader out;
        int requestCount;

        ServerSlot(int id) throws IOException { this.id = id; start(); }

        void start() throws IOException {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "--server");
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            proc = pb.start();
            in = new java.io.BufferedWriter(new java.io.OutputStreamWriter(
                    proc.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
            out = new BufferedReader(new InputStreamReader(
                    proc.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            requestCount = 0;
        }

        void stop() {
            try { in.write("SHUTDOWN\n"); in.flush(); } catch (Exception ignored) {}
            try { proc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS); } catch (Exception ignored) {}
            if (proc.isAlive()) proc.destroyForcibly();
            proc = null;
            in = null;
            out = null;
        }
    }

    private static void initPool() throws IOException {
        synchronized (poolInitLock) {
            if (poolInitialized) return;
            System.err.println("[SEMSERVER] initializing pool of " + POOL_SIZE + " python servers");
            for (int i = 0; i < POOL_SIZE; i++) {
                pool.offer(new ServerSlot(i));
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (ServerSlot s : pool) {
                    try { s.stop(); } catch (Exception ignored) {}
                }
            }));
            poolInitialized = true;
        }
    }

    /** Sends one request to a slot from the pool and returns its output.
     *  Acquires a free slot, sends the request, releases the slot. */
    private static String runSemantics(String... argv) {
        ServerSlot slot = null;
        try {
            if (!poolInitialized) initPool();
            slot = pool.take();
            // Restart if dead or quota reached.
            if (slot.proc == null || !slot.proc.isAlive()) {
                System.err.println("[SEMSERVER #" + slot.id + "] starting fresh (was null/dead)");
                slot.start();
            } else if (slot.requestCount >= SERVER_RESTART_INTERVAL) {
                System.err.println("[SEMSERVER #" + slot.id + "] restarting after " + slot.requestCount + " requests");
                slot.stop();
                slot.start();
            }
            slot.requestCount++;
            StringBuilder req = new StringBuilder();
            for (int i = 0; i < argv.length; i++) {
                if (i > 0) req.append('\t');
                req.append(java.util.Base64.getEncoder().encodeToString(
                        argv[i].getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
            slot.in.write(req.toString());
            slot.in.write('\n');
            slot.in.flush();
            String line = slot.out.readLine();
            if (line == null) {
                throw new IOException("semantics.py server #" + slot.id + " closed unexpectedly");
            }
            return new String(java.util.Base64.getDecoder().decode(line),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            if (slot != null) {
                try { pool.put(slot); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public static int getPoolSize() { return POOL_SIZE; }

    public String solveSymb(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        return runSemantics("-solve", circuitToJson(c1.gates, nqubits),
                circuitToJson(c2.gates, nqubits));
    }


    private String evalExpr(Expr expr) {
         if (expr instanceof Symbol) {
            return ((Symbol) expr).getSymbol();
         } else if(expr instanceof Real){
            return String.format("%.17g", ((Real) expr).getNumber());
         } else if(expr instanceof BinOp) {
            return evalBinop((BinOp) expr);
         } else if(expr instanceof UnOp) {
            return evalUniOp((UnOp) expr);
         }
         return "";
    }

    private String evalBinop(BinOp bo) {
       String v1 = evalExpr(bo.getE1());
       String v2 = evalExpr(bo.getE2());
       switch (bo.getOp()) {
        case PLUS: return v1 + "+" + v2;
        case SUBTRACT: return v1 + "-" + v2;
        case MULT: return v1 + "*" + v2;
        case DIV: return v1 + "/" + v2;
        default: throw new RuntimeException(String.format("unimplemented BinOp: %s", bo.getOp()));
       }
    }

    private String evalUniOp(UnOp uniop) { 
        if(uniop.getOp() == Op.MINUS) {
            return "-" + evalExpr(uniop.getE());
        }
        return "";
    }
    
    
    private String paramToJson(String paramName, Expr expr) {
        return String.format("{\"" + paramName + "\":\"%s\"}", evalExpr(expr));
    }

    private String paramsToJson(String[] paramNames, Expr[] exprs) {
        List<String> paramEntries = new ArrayList<>();
        for (int i = 0; i < paramNames.length; i++) {
            paramEntries.add(String.format("\"%s\":\"%s\"", paramNames[i], evalExpr(exprs[i])));
        }
        return "{" + String.join(",", paramEntries) + "}";
    }

    private int parseQubit(String qubitStr) {
        if (qubitStr.startsWith("q")) {
            return Integer.parseInt(qubitStr.substring(1));
        }
        return Integer.parseInt(qubitStr);
    }


    public boolean checkTrace(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        return runSemantics("-tracecheck", circuitToJson(c1.gates, nqubits),
                circuitToJson(c2.gates, nqubits)).contains("True");
    }


    public boolean checkEigen(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        return runSemantics("-eigencheck", circuitToJson(c1.gates, nqubits),
                circuitToJson(c2.gates, nqubits)).contains("True");
    }

    public boolean checkBig(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        return checkBig(c1, c2, nqubits, null, 1);
    }

    /** True iff L (the intertwiner LHS) has all distinct eigenvalues at a random
     *  concrete sample. Such cases produce dim(intertwiner) = n -- the smallest
     *  non-trivial basis (3-torus of unitary middles mod global phase). When the
     *  caller's enumerator wants only "richer" rules (degenerate eigenvalue
     *  cases with off-diagonal freedom), it filters these out via this check. */
    public boolean hasAllDistinctEigen(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        return runSemantics("-distincteigen", circuitToJson(c1.gates, nqubits),
                circuitToJson(c2.gates, nqubits)).contains("True");
    }

    /** Eigenvalue fingerprint string of L (from compute_L_R(c1, c2)) at a random
     *  concrete sample. Two circuits with identical fingerprints share the same
     *  eigenvalue multiset at that sample -- so they're plausible intertwiner
     *  partners. Cheap bucket-key for the filter phase. Pass seed to reproduce. */
    public String getEigenFingerprint(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits, Long seed) {
        String[] argv = (seed != null)
                ? new String[]{"-eigenfp", circuitToJson(c1.gates, nqubits), circuitToJson(c2.gates, nqubits),
                               "-seed", Long.toString(seed)}
                : new String[]{"-eigenfp", circuitToJson(c1.gates, nqubits), circuitToJson(c2.gates, nqubits)};
        return runSemantics(argv).trim();
    }

    /** Per-circuit eigenvalue fingerprint at a random concrete sample (SYMB
     *  treated as identity). Same-fingerprint circuits inside a trace bucket
     *  are unitarily similar -- only those can form intertwiner pairs, so we
     *  use this for pre-bucketing before O(N^2) pair-wise checkBig. */
    public String getCircuitEigenFingerprint(List<EggGen.Gate> gates, int nqubits, Long seed) {
        String[] argv = (seed != null)
                ? new String[]{"-singleeigenfp", circuitToJson(gates, nqubits),
                               "-seed", Long.toString(seed)}
                : new String[]{"-singleeigenfp", circuitToJson(gates, nqubits)};
        return runSemantics(argv).trim();
    }

    public boolean checkSymbolicEigen(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        return runSemantics("-symbeigencheck", circuitToJson(c1.gates, nqubits),
                circuitToJson(c2.gates, nqubits)).contains("True");
    }

    public boolean checkBig(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits, Long seed, int ntraces) {
        String j1 = circuitToJson(c1.gates, nqubits);
        String j2 = circuitToJson(c2.gates, nqubits);
        String content = (seed != null)
                ? runSemantics("-bigcheck", j1, j2, "-seed", Long.toString(seed),
                        "-ntraces", Integer.toString(Math.max(1, ntraces)))
                : runSemantics("-bigcheck", j1, j2);
        return content.contains("True");
    }


    public String getTrace(List<EggGen.Gate> gates, int nqubits) {
        return getTrace(gates, nqubits, null, 1);
    }

    public String getTrace(List<EggGen.Gate> gates, int nqubits, Long seed) {
        return getTrace(gates, nqubits, seed, 1);
    }

    public String getTrace(List<EggGen.Gate> gates, int nqubits, Long seed, int ntraces) {
        String j1 = circuitToJson(gates, nqubits);
        String content = (seed != null)
                ? runSemantics("-trace", j1, "-seed", Long.toString(seed),
                        "-ntraces", Integer.toString(Math.max(1, ntraces)))
                : runSemantics("-trace", j1);
        return content.trim();
    }

    public String getEigenvalues(List<EggGen.Gate> gates, int nqubits) {
        return runSemantics("-eigenvals", circuitToJson(gates, nqubits)).trim();
    }

    public void computeAndPrintMatrix(String jsonCircuit) {
        System.out.println("Matrix representation:");
        System.out.println(runSemantics(jsonCircuit));
    }

    /**
     * Parses the string output from the python script's solve_intertwiner_equation
     * method and returns a list of basis matrices.
     *
     * @param pythonOutput The string output from the python script.
     * @return A list of Matrix objects representing the basis.
     */
    public static List<SparseMatrix> parseBasis(String pythonOutput) {
        List<SparseMatrix> basis = new ArrayList<>();
        String[] lines = pythonOutput.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Matrix(")) {
                String content = line.substring("Matrix(".length(), line.length() - 1);
                content = content.substring(1, content.length() - 1);
                
                List<String[]> rowElementsList = new ArrayList<>();
                Pattern rowPattern = Pattern.compile("\\[(.*?)\\]");
                Matcher rowMatcher = rowPattern.matcher(content);

                while(rowMatcher.find()) {
                    rowElementsList.add(rowMatcher.group(1).split(", "));
                }

                int numRows = rowElementsList.size();
                if (numRows == 0) continue;
                int numCols = rowElementsList.get(0).length;

                List<SparseMatrix.MatrixEntry> entries = new ArrayList<>();
                for (int i = 0; i < numRows; i++) {
                    for (int j = 0; j < numCols; j++) {
                        Complex value = parseComplex(rowElementsList.get(i)[j]);
                        entries.add(new SparseMatrix.MatrixEntry(i, j, value));
                    }
                }
                basis.add(new SparseMatrix(numRows, numCols, entries));
            }
        }
        return basis;
    }

    /**
     * Parses a string representation of a number from sympy.
     * NOTE: This parser is designed to handle integers and simple imaginary 
     * numbers like 'I', '-I', '2*I'. It does not handle combined 
     * expressions like '1 + 2*I'.
     * @param s The string to parse.
     * @return A Complex number.
     */
    private static double parseDoubleWithSqrt(String s) {
        s = s.trim();
        if (s.contains("sqrt")) {
            double coefficient = 1.0;
            String numberPart = s;

            if (s.startsWith("-")) {
                coefficient = -1.0;
                numberPart = s.substring(1).trim();
            } else if (s.startsWith("+")) {
                numberPart = s.substring(1).trim();
            }
            
            if (numberPart.startsWith("sqrt")) {
                // It's of the form sqrt(n)
            } else if (numberPart.contains("*sqrt")) {
                // It's of the form c*sqrt(n)
                String[] parts = numberPart.split("\\*sqrt");
                coefficient *= Double.parseDouble(parts[0]);
                numberPart = "sqrt" + parts[1];
            }

            Pattern p = Pattern.compile("sqrt\\((\\d+\\.?\\d*)\\)");
            Matcher m = p.matcher(numberPart);
            if (m.matches()) {
                double n = Double.parseDouble(m.group(1));
                return coefficient * Math.sqrt(n);
            }
        }
        
        return Double.parseDouble(s);
    }

    public static Complex parseComplex(String s) {
         return new Complex(s);
    }
    
    public static void main(String[] args) {
        Random rand = new Random();
        rand.setSeed(54);
        SymbolicSolve solver = new SymbolicSolve(rand);

        // Test case 1: Bell state (non-parameterized)
        List<EggGen.Gate> bellGates = new ArrayList<>();
        bellGates.add(new EggGen.CX("q0", "q1"));
        EggGen.Circuit bellStateCircuit = new EggGen.Circuit(bellGates);
        String bellJson = solver.circuitToJson(bellStateCircuit.gates, 2);
        System.out.println("Generated JSON for Bell state circuit:");
        System.out.println(bellJson);
        solver.computeAndPrintMatrix(bellJson);
        System.out.println("Eigenvalues for Bell state circuit:");
        //String eigenvalues = solver.getEigenvalues(bellJson);
        //System.out.println(eigenvalues);
        System.out.println();

        // Test case 2: Parameterized circuit from Test 12
        List<EggGen.Gate> paramGates = new ArrayList<>();
        paramGates.add(new EggGen.RZ("q0", new Symbol("theta1")));
        paramGates.add(new EggGen.SYMB(2));
        paramGates.add(new EggGen.RZ("q1", new Symbol("theta2")));
        EggGen.Circuit paramCircuit = new EggGen.Circuit(paramGates);
        
        List<EggGen.Gate> paramGatesR = new ArrayList<>();
        paramGatesR.add(new EggGen.SYMB(2));
        paramGatesR.add(new EggGen.RZ("q1", new BinOp(Op.PLUS, new Symbol("theta1"), new Symbol("theta2"))));
        EggGen.Circuit paramCircuitR = new EggGen.Circuit(paramGatesR);
        String basisString = solver.solveSymb(paramCircuit, paramCircuitR, 2);
        System.out.println("--- Raw output from semantics.py (Test 12) ---");
        System.out.println(basisString);

        System.out.println("\n--- Parsed Basis Matrices (Test 12) ---");
        List<SparseMatrix> parsedBasis = parseBasis(basisString);
        System.out.println("Parsed " + parsedBasis.size() + " basis matrices:\n");
        for (int i = 0; i < parsedBasis.size(); i++) {
            System.out.println("--- Basis Element " + (i + 1) + " ---");
            System.out.println(parsedBasis.get(i));
        }

        // --- Test for sqrt parsing from Test 19 ---
        System.out.println("\n--- Parsing irrational basis from Test 19 output ---");
        String irrationalOutput = "Matrix([[1, sqrt(2)], [1, 0]])\n" +
                                  "Matrix([[sqrt(2), 1], [0, 1]])";
        
        List<SparseMatrix> irrationalBasis = parseBasis(irrationalOutput);
        System.out.println("Parsed " + irrationalBasis.size() + " basis matrices:\n");
        for (int i = 0; i < irrationalBasis.size(); i++) {
            System.out.println("--- Basis Element " + (i + 1) + " ---");
            System.out.println(irrationalBasis.get(i));
        }

        // --- Test for symbolic exp parsing ---
        System.out.println("\n--- Parsing symbolic 'exp' basis ---");
        String symbolicOutput = "Matrix([[exp(-I*theta1), 0], [0, 1]])";
        
        List<SparseMatrix> symbolicBasis = parseBasis(symbolicOutput);
        System.out.println("Parsed " + symbolicBasis.size() + " basis matrices:\n");
        for (int i = 0; i < symbolicBasis.size(); i++) {
            System.out.println("--- Basis Element " + (i + 1) + " ---");
            System.out.println(symbolicBasis.get(i));
        }
    }
}
