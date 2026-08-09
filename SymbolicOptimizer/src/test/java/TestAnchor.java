import java.util.*;
import java.util.regex.*;
import ast.*;

public class TestAnchor {

    static MatrixConstrainedRule mkSymbRule(String lhs, String rhs) {
        return new MatrixConstrainedRule(lhs, rhs, new ArrayList<>(), "birewrite");
    }

    static Rule mkConcreteRule(String lhsQasm, String rhsQasm) {
        EggGen.Circuit l = QASMAstBuilder.parse(lhsQasm);
        EggGen.Circuit r = QASMAstBuilder.parse(rhsQasm);
        return new Rule(l, r, new ArrayList<>());
    }

    public static void main(String[] args) throws Exception {
        List<Rule> concreteRules = new ArrayList<>();
        concreteRules.add(mkConcreteRule(
                "rxx(theta2) q[0],q[1]; rxx(theta1) q[0],q[1];",
                "rxx((theta1+theta2)) q[0],q[1];"
        ));
        concreteRules.add(mkConcreteRule(
                "rxx(pi/2) q[0],q[1]; rz(theta1) q[0]; rx(-pi/2) q[0]; rxx(pi/2) q[0],q[1];",
                "rxx(theta1) q[0],q[1]; rx(pi/2) q[0]; rx(pi) q[1];"
        ));
        System.out.println("Loaded " + concreteRules.size() + " concrete rules");

        List<MatrixConstrainedRule> symbRules = new ArrayList<>();
        symbRules.add(mkSymbRule(
                "(Cons (RXX q0 q1 theta1) (Cons (SYMB 2) (Cons (RXX q0 q1 theta1) c)))",
                "(Cons (RXX q0 q1 theta1) (Cons (RXX q0 q1 theta1) (Cons (SYMB 2) c)))"
        ));
        symbRules.add(mkSymbRule(
                "(Cons (RXX q0 q1 (UnOp (MINUS) theta1)) (Cons (SYMB 2) (Cons (RXX q0 q1 theta1) c)))",
                "(Cons (RXX q0 q1 (UnOp (MINUS) theta1)) (Cons (RXX q0 q1 theta1) (Cons (SYMB 2) c)))"
        ));
        System.out.println("Loaded " + symbRules.size() + " base symbolic rules");
        System.out.println();

        List<MatrixConstrainedRule> anchoredRules = Anchor.anchor(concreteRules, symbRules);

        System.out.println("\n========== ANCHOR RESULTS ==========");
        System.out.println("Total anchored rules: " + anchoredRules.size());
        System.out.println();
        for (int i = 0; i < anchoredRules.size(); i++) {
            MatrixConstrainedRule r = anchoredRules.get(i);
            System.out.println("Rule #" + i + ":");
            System.out.println("  LHS: " + r.getLHS());
            System.out.println("  RHS: " + r.getRHS());
            System.out.println();
        }
        System.out.println("Done.");
    }
}
