
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


    public String solveSymb(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        String circuitj1 = circuitToJson(c1.gates, nqubits);
        String circuitj2 = circuitToJson(c2.gates, nqubits);
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-solve", circuitj1, circuitj2);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            BufferedReader ereader = new BufferedReader(new InputStreamReader(process.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {                                                  
                content.append(line);
                content.append("\n");
            }
            while ((line = ereader.readLine()) != null) {                                                  
                System.err.println(line);
            }
            process.waitFor();
            return content.toString();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
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
        String circuitj1 = circuitToJson(c1.gates, nqubits);
        String circuitj2 = circuitToJson(c2.gates, nqubits);
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-tracecheck", circuitj1, circuitj2);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            BufferedReader ereader = new BufferedReader(new InputStreamReader(process.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {                                                  
                content.append(line);
                content.append("\n");
            }
            while ((line = ereader.readLine()) != null) {                                                  
                System.err.println(line);
            }
            process.waitFor();
            return content.toString().contains("True");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean checkEigen(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        String circuitj1 = circuitToJson(c1.gates, nqubits);
        String circuitj2 = circuitToJson(c2.gates, nqubits);
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-eigencheck", circuitj1, circuitj2);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            BufferedReader ereader = new BufferedReader(new InputStreamReader(process.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {                                                  
                content.append(line);
                content.append("\n");
            }
            while ((line = ereader.readLine()) != null) {                                                  
                System.err.println(line);
            }
            process.waitFor();
            return content.toString().contains("True");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkBig(EggGen.Circuit c1, EggGen.Circuit c2, int nqubits) {
        String circuitj1 = circuitToJson(c1.gates, nqubits);
        String circuitj2 = circuitToJson(c2.gates, nqubits);
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-bigcheck", circuitj1, circuitj2);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            BufferedReader ereader = new BufferedReader(new InputStreamReader(process.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {                                                  
                content.append(line);
                content.append("\n");
            }
            while ((line = ereader.readLine()) != null) {                                                  
                System.err.println(line);
            }
            process.waitFor();

            //System.out.println(content.toString());
            return content.toString().contains("True");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }


    public String getTrace(List<EggGen.Gate> gates, int nqubits) {
        String circuitj1 = circuitToJson(gates, nqubits);
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-trace", circuitj1);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            BufferedReader ereader = new BufferedReader(new InputStreamReader(process.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {                                                  
                content.append(line);
            }
            while ((line = ereader.readLine()) != null) {                                                  
                System.err.println(line);
            }

            System.out.println(content.toString());
            process.waitFor();
            return content.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getEigenvalues(List<EggGen.Gate> gates, int nqubits) {
        String circuitj1 = circuitToJson(gates, nqubits);
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", "-eigenvals", circuitj1);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder content = new StringBuilder();
            BufferedReader ereader = new BufferedReader(new InputStreamReader(process.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {                                                  
                content.append(line);
            }
            while ((line = ereader.readLine()) != null) {                                                  
                System.err.println(line);
            }

            System.out.println(content.toString());
            process.waitFor();
            return content.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void computeAndPrintMatrix(String jsonCircuit) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "semantics.py", jsonCircuit);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),java.nio.charset.StandardCharsets.UTF_8));
            BufferedReader ereader = new BufferedReader(new InputStreamReader(process.getErrorStream(),java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("Matrix representation:");
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {                                                  
                System.out.println(line);
            }
            while ((line = ereader.readLine()) != null) {                                                  
                System.err.println(line);
            }
            //System.out.println(content.toString());
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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
