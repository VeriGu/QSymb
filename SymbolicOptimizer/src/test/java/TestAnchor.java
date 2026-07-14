import java.util.*;
import java.util.regex.*;
import ast.*;

/**
 * Test the Anchor algorithm with the new QAOA gadget concrete rule.
 * Goal: see what new anchored symbolic rules get generated when we compose
 * existing symbolic rules with the new gadget concrete rule.
 */
public class TestAnchor {

    static MatrixConstrainedRule mkSymbRule(String lhs, String rhs) {
        // Build an empty-basis MatrixConstrainedRule (we only care about LHS/RHS structure here)
        return new MatrixConstrainedRule(lhs, rhs, new ArrayList<>(), "birewrite");
    }

    static Rule mkConcreteRule(String lhsQasm, String rhsQasm) {
        EggGen.Circuit l = QASMAstBuilder.parse(lhsQasm);
        EggGen.Circuit r = QASMAstBuilder.parse(rhsQasm);
        return new Rule(l, r, new ArrayList<>());
    }

    public static void main(String[] args) throws Exception {
        // --- Concrete rules to anchor with ---
        List<Rule> concreteRules = new ArrayList<>();
        // Standard RXX merge (existing, line 167 of rules_ion_q3_3.txt)
        concreteRules.add(mkConcreteRule(
                "rxx(theta2) q[0],q[1]; rxx(theta1) q[0],q[1];",
                "rxx((theta1+theta2)) q[0],q[1];"
        ));
        // Our new QAOA gadget rule (just verified):
        //   rxx(π/2); rz(γ) q0; rx(-π/2) q0; rxx(π/2)  →  rxx(γ); rx(π/2) q0; rx(π) q1
        concreteRules.add(mkConcreteRule(
                "rxx(pi/2) q[0],q[1]; rz(theta1) q[0]; rx(-pi/2) q[0]; rxx(pi/2) q[0],q[1];",
                "rxx(theta1) q[0],q[1]; rx(pi/2) q[0]; rx(pi) q[1];"
        ));
        System.out.println("Loaded " + concreteRules.size() + " concrete rules");

        // --- Symbolic rules (from anchored_ion_q3.txt — these are the RXX-conjugation ones) ---
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

        // Run Anchor
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
