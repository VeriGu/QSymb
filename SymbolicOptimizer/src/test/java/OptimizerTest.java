import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

public class OptimizerTest {
    @Test
    public void testSymbolicMatch() throws IOException {
        System.out.println("Test 1 Symbolic Match");
        Optimizer optimizer = new Optimizer();
        String qasm = new String(Files.readAllBytes(Paths.get("/root/simple.qasm")));
        EggGen.Circuit eggCircuit = QASMAstBuilder.parse(qasm);
        EggGen.ConstrainedCircuit eggConstrainedCircuit = new EggGen.ConstrainedCircuit(eggCircuit, new EggGen.Permutation(new ArrayList<>()));
        System.out.println(eggCircuit.toQASM());
        String rule = "(Cons (H (Q \"q1\")) (Cons (CX (Q \"q1\") (Q \"q0\")) (Cons (H (Q \"q1\")) (Cons (SYMB 2) c))))";
        String rhs = "(Cons (SYMB 2) (Cons (H (Q \"q1\")) (Cons (CX (Q \"q1\") (Q \"q0\")) (Cons (H (Q \"q1\")) c))))";
        int maxSymbSize = 1;
        String basisString = "[Matrix(4;4;(0,0,1);(1,1,1);(2,2,1);(3,3,1))]";
        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }
        try {
            optimizer.symbolicMatch(eggCircuit, rule, rhs, 0, maxSymbSize, basis, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSymbolicMatchWithBasis() throws IOException {
        System.out.println("Test 2 Symbolic Match With Basis");
        Optimizer optimizer = new Optimizer();
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "creg c[2];\n" +
                      "x q[0];\n" +
                      "cx q[1],q[0];\n";
        EggGen.Circuit eggCircuit = QASMAstBuilder.parse(qasm);
        EggGen.ConstrainedCircuit eggConstrainedCircuit = new EggGen.ConstrainedCircuit(eggCircuit, new EggGen.Permutation(new ArrayList<>()));

        String rule = "(Cons (X (Q \"q0\")) (Cons (SYMB 2) c))";
        String rhs = "(Cons (SYMB 2) (Cons (X (Q \"q0\")) c)))";
        String basisString = "[Matrix(4;4;(0,0,1);(1,3,1);(2,2,1);(3,1,1))]";

        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }

        int maxSymbSize = 1;
        try {
            optimizer.symbolicMatch(eggConstrainedCircuit.circuit, rule, rhs, 0, maxSymbSize, basis, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSymbolicMatchWithSymbolicHead() throws IOException {
        System.out.println("Test 3 Symbolic Match With Symbolic Head");
        Optimizer optimizer = new Optimizer();
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "creg c[2];\n" +
                      "x q[0];\n" +
                      "cx q[1],q[0];\n";
        EggGen.Circuit eggCircuit = QASMAstBuilder.parse(qasm);
        EggGen.ConstrainedCircuit eggConstrainedCircuit = new EggGen.ConstrainedCircuit(eggCircuit, new EggGen.Permutation(new ArrayList<>()));
        ConstrainedCircuit constrainedCircuit = CircuitTranslator.translateBack(eggConstrainedCircuit, eggCircuit.getMaxQubits() + 1);
        Circuit c = constrainedCircuit.getCircuit();

        String rule = "(Cons (SYMB 2) (Cons (CX (Q \"q1\") (Q \"q0\")) c))";
        String rhs = "(Cons (CX (Q \"q1\") (Q \"q0\")) (Cons (SYMB 2) c))";
        String basisString = "[Matrix(4;4;(0,2,1);(1,3,1);(2,0,1);(3,1,1))]";

        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }

        int maxSymbSize = 1;
        try {
            optimizer.symbolicMatch(eggCircuit, rule, rhs, 0, maxSymbSize, basis, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testSymbolicRzMatch() throws IOException {
        System.out.println("Test 4 Symbolic Rz Match");
        Optimizer optimizer = new Optimizer();
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "rz(0.5) q[0];\n";
        EggGen.Circuit eggCircuit = QASMAstBuilder.parse(qasm);
        EggGen.ConstrainedCircuit eggConstrainedCircuit = new EggGen.ConstrainedCircuit(eggCircuit, new EggGen.Permutation(new ArrayList<>()));
        ConstrainedCircuit constrainedCircuit = CircuitTranslator.translateBack(eggConstrainedCircuit, eggCircuit.getMaxQubits() + 1);
        Circuit c = constrainedCircuit.getCircuit();

        String rule = "(Cons (RZ (Q \"q0\") theta1) (Cons (SYMB 2) c))";
        String rhs = "(Cons (SYMB 2) (Cons (RZ (Q \"q0\") theta1) c))";
        int maxSymbSize = 1;
        String basisString = "[Matrix(4;4;(0,0,1);(1,1,1);(2,2,1);(3,3,1))]";
        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }
        try {
            CircuitDAG result = optimizer.symbolicMatch(eggCircuit, rule, rhs, 0, maxSymbSize, basis, null);
            assertTrue(result != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSymbolicCXGateCancellation() throws IOException {
        System.out.println("Test 5 Symbolic CX Gate Cancellation");
        Optimizer optimizer = new Optimizer();
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "cx q[0],q[1];\n" +
                      "rz(0.1) q[0];\n" +
                      "x q[1];\n" +
                      "rz(0.2) q[0];\n" +
                      "x q[1];\n" +
                      "rz(0.3) q[0];\n" +
                      "x q[1];\n" +
                      "rz(0.4) q[0];\n" +
                      "x q[1];\n" +
                      "x q[1];\n" +
                      "rz(0) q[0];\n" +
                      "cx q[0],q[1];\n";
        EggGen.Circuit eggCircuit = QASMAstBuilder.parse(qasm);
        String rule = "(Cons (SYMB 2) (Cons (CX (Q \"q0\") (Q \"q1\")) c))";
        String rhs = "(Cons (CX (Q \"q0\") (Q \"q1\")) (Cons (SYMB 2) c))";
        int maxSymbSize = 11;
        String basisString = "[Matrix(4;4;(0, 0, 1);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 1);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 1);(0, 3, 1);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 1);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 1);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 1);(1, 3, 1);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 1);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 1);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 1);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 1);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 1);(3, 0, 0);(3, 1, 0);(3, 2, 1);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 1);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 1))]";
        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }

        try {
            CircuitDAG result = optimizer.symbolicMatch(eggCircuit, rule, rhs, 1, maxSymbSize, basis, null);
            System.out.println("----------------------Test 5 Ended-------------------------------");
            assertTrue(result != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSymbolicCXGateCancellation2() throws IOException {
        System.out.println("Test 6 Symbolic CX Gate Cancellation2");
        Optimizer optimizer = new Optimizer();
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "cx q[0],q[1];\n" +
                      "rz(0.1) q[0];\n" +
                      "rz(0) q[0];\n" +
                      "x q[1];\n" +
                      "cx q[0],q[1];\n";
        EggGen.Circuit eggCircuit = QASMAstBuilder.parse(qasm);
        String rule = "(Cons (CX (Q \"q0\") (Q \"q1\")) (Cons (SYMB 2) c))";
        String rhs = "(Cons (SYMB 2) (Cons (CX (Q \"q0\") (Q \"q1\")) c))";
        int maxSymbSize = 11;
        String basisString = "[Matrix(4;4;(0, 0, 1);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 1);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 1);(0, 3, 1);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 1);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 1);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 1);(1, 3, 1);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 1);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 1);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 1);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 1);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 1);(3, 0, 0);(3, 1, 0);(3, 2, 1);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 1);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 1))]";
        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }

        try {
            CircuitDAG result = optimizer.symbolicMatch(eggCircuit, rule, rhs, 2, maxSymbSize, basis, null);
            System.out.println("----------------------Test 6 Ended-------------------------------");
            assertTrue(result != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSymbolicCXGateCancellation7() throws IOException {
        System.out.println("Test 7 Symbolic CX Gate Cancellation");
        Optimizer optimizer = new Optimizer();
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[2];\n" +
                      "cx q[2],q[4];\n" +
                      "rz(pi) q[2];\n" +
                      "x q[4];\n" +
                      "rz(pi/2) q[2];\n" +
                      "x q[4];\n" +
                      "x q[5];\n" +
                      "rz(pi/4) q[2];\n" +
                      "x q[4];\n" +
                      "rz(pi/8) q[2];\n" +
                      "x q[4];\n" +
                      "rz(0) q[6];\n" +
                      "x q[4];\n" +
                      "rz(0) q[2];\n" +
                      "cx q[2],q[4];\n";
        CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
        String rule = "(Cons (CX (Q \"q2\") (Q \"q4\")) (Cons (SYMB 2) (Cons (CX (Q \"q2\") (Q \"q4\")) c)))";
        String rhs = "(Cons (CX (Q \"q2\") (Q \"q4\")) (Cons (CX (Q \"q2\") (Q \"q4\")) (Cons (SYMB 2) c)))";
        int maxSymbSize = 11;
        String basisString = "[Matrix(4;4;(0, 0, 1);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 1);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 1);(0, 3, 1);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 1);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 1);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 1);(1, 3, 1);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 1);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 1);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 1);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 1);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 1);(3, 0, 0);(3, 1, 0);(3, 2, 1);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 1);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 1))]";
        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }

        try {
            CircuitDAG result = optimizer.symbolicMatchBeforeAfter(circuit, rule, rhs, 1, maxSymbSize, basis, null);
            System.out.println(result.toQASM());
            System.out.println("----------------------Test 7 Ended-------------------------------");
            assertTrue(result != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSymbolicRZCommute() throws IOException {
        System.out.println("Test 8 Symbolic RZ commute");
        Optimizer optimizer = new Optimizer();
        String qasm = "OPENQASM 2.0;\n" +
                      "include \"qelib1.inc\";\n" +
                      "qreg q[4];\n" +
                        "rz(-0.7853981633974483) q[0];\n"+
                        "cx q[0],q[3];\n" +
                        "rz(1.5707963267948966) q[3];\n" +
                        "rz(0.7853981633974483) q[1];\n" +
                        "x q[0];\n" +
                        "rz(1.5707963267948966) q[3];\n" +
                        "rz(1.5707963267948966) q[3];\n" +
                        "x q[0];\n" +
                        "rz(1.5707963267948966) q[0];\n";

        CircuitDAG circuit = QASMToDAGVisitor.parse(qasm);
        String rule = "(Cons (RZ q0 theta1) (Cons (SYMB 2) (Cons (RZ q0 theta2) c)))";
        String rhs = "(Cons (RZ q0 theta1) (Cons (RZ q0 theta2) (Cons (SYMB 2) c)))";
        int maxSymbSize = 11;
        String basisString = "[Matrix(4;4;(0, 0, 1);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 1);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 1);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 1);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 1);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 1);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 1);(3, 3, 0))::Matrix(4;4;(0, 0, 0);(0, 1, 0);(0, 2, 0);(0, 3, 0);(1, 0, 0);(1, 1, 0);(1, 2, 0);(1, 3, 0);(2, 0, 0);(2, 1, 0);(2, 2, 0);(2, 3, 0);(3, 0, 0);(3, 1, 0);(3, 2, 0);(3, 3, 1))]";

        List<SymbolicSolve.SparseMatrix> basis = new ArrayList<>();
        Pattern matrixP = Pattern.compile("\\[(.*)\\]");
        Matcher matcher = matrixP.matcher(basisString);
        if (matcher.matches()) {
            String matricesStr = matcher.group(1);
            String[] matrices = matricesStr.split("::");
            for(String m: matrices) {
                List<SymbolicSolve.SparseMatrix.MatrixEntry> sentries = new ArrayList<>();
                if(m.startsWith("Matrix(")) {
                    String content = m.substring("Matrix(".length(), m.length() - 1);
                    String[] entries = content.split(";");
                    int rows = Integer.valueOf(entries[0]);
                    int cols = Integer.valueOf(entries[1]);
                    for(int i = 2; i < entries.length; i++) {
                        String entry = entries[i].substring(1, entries[i].length() - 1);
                        String[] elems = entry.split(",");
                        int row = Integer.valueOf(elems[0].trim());
                        int col = Integer.valueOf(elems[1].trim());
                        SymbolicSolve.Complex value = SymbolicSolve.parseComplex(elems[2]);
                        sentries.add(new SymbolicSolve.SparseMatrix.MatrixEntry(row, col, value));
                    }
                    basis.add(new SymbolicSolve.SparseMatrix(rows, cols, sentries));
                }
            }
        }

        try {
            CircuitDAG result = optimizer.symbolicMatchBeforeAfter(circuit, rule, rhs, 1, maxSymbSize, basis, null);
            System.out.println("----------------------Test 8 Ended-------------------------------");
            assertTrue(result != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
