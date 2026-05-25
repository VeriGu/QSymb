import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnchorTest {

    @Test
    public void testAnchor() {
        // 1. Create symbolic rule
        List<MatrixConstrainedRule> symbRules = new ArrayList<>();
        String rule = "(Cons (SYMB 2) (Cons (CX (Q \"q1\") (Q \"q2\")) c))";
        String rhs = "(Cons (CX (Q \"q1\") (Q \"q2\")) (Cons (SYMB 2) c))";
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
        symbRules.add(new MatrixConstrainedRule(rule, rhs, basis, "birewrite"));

        // 2. Create concrete rule
        List<Rule> rules = new ArrayList<>();
        // The parser needs a valid QASM file, so we need to add headers.
        String concreteLHSStr = "cx q[0],q[1]; cx q[0],q[1];";
        String concreteRHSStr = ";";
        EggGen.Circuit concreteLHS = QASMAstBuilder.parse(concreteLHSStr);
        EggGen.Circuit concreteRHS = QASMAstBuilder.parse(concreteRHSStr);
        rules.add(new Rule(concreteLHS, concreteRHS, new ArrayList<>()));

        
        List<MatrixConstrainedRule> anchoredRules = Anchor.anchor(rules, symbRules);

        System.out.println("Number of anchored rules: " + anchoredRules.size());
        for (MatrixConstrainedRule r : anchoredRules) {
            System.out.println("Anchored rule: " + r.getLHS() + " -> " + r.getRHS());
        }

        
        MatrixConstrainedRule anchoredRule = anchoredRules.get(0);
        assertEquals(1, anchoredRules.size());
    }
}
