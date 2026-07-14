import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ast.Expr;
import ast.Real;
import ast.Symbol;
import ast.UnOp;

public class TestMatchAngle {
    public static void main(String[] args) throws Exception {
        Optimizer opt = new Optimizer();
        Method m = Optimizer.class.getDeclaredMethod("matchAngle", Expr.class, Expr.class, Map.class);
        m.setAccessible(true);

        // Test 1: pattern = (UnOp MINUS theta1), circ = Real(-1.5707963...)
        // Should bind theta1 -> Real(1.5707963...)
        {
            Map<String, Expr> angleMap = new HashMap<>();
            Expr pattern = new UnOp(Expr.Op.MINUS, new Symbol("theta1"));
            Expr circ = new Real(-1.5707963267948966);
            boolean ok = (boolean) m.invoke(opt, pattern, circ, angleMap);
            System.out.println("Test 1 [(UnOp MINUS theta1) vs Real(-π/2)]: " + ok
                    + ", angleMap=" + angleMap);
            assert ok : "expected match";
            assert angleMap.containsKey("theta1") : "expected theta1 binding";
            Expr bound = angleMap.get("theta1");
            assert bound instanceof Real : "expected Real binding";
            assert Math.abs(((Real) bound).getNumber() - 1.5707963267948966) < 1e-12
                    : "expected positive π/2 binding, got " + ((Real) bound).getNumber();
        }

        // Test 2: symmetric. pattern = Real(-1.57...), circ = (UnOp MINUS Real(1.57...))
        {
            Map<String, Expr> angleMap = new HashMap<>();
            Expr pattern = new Real(-1.5707963267948966);
            Expr circ = new UnOp(Expr.Op.MINUS, new Real(1.5707963267948966));
            boolean ok = (boolean) m.invoke(opt, pattern, circ, angleMap);
            System.out.println("Test 2 [Real(-π/2) vs (UnOp MINUS Real(π/2))]: " + ok);
            assert ok : "expected symmetric semantic match";
        }

        // Test 3: confirm SAME-sign still works (pattern theta1 vs Real(π/2) twice)
        {
            Map<String, Expr> angleMap = new HashMap<>();
            Expr pattern1 = new Symbol("theta1");
            Expr circ1 = new Real(1.5707963267948966);
            boolean ok1 = (boolean) m.invoke(opt, pattern1, circ1, angleMap);
            Expr pattern2 = new Symbol("theta1");
            Expr circ2 = new Real(1.5707963267948966);
            boolean ok2 = (boolean) m.invoke(opt, pattern2, circ2, angleMap);
            System.out.println("Test 3 [theta1=π/2 twice]: " + (ok1 && ok2)
                    + ", angleMap=" + angleMap);
            assert ok1 && ok2;
        }

        // Test 4: REGRESSION: opposite signs in same rule should still fail.
        //   pattern1 = theta1 binds to Real(π/2).
        //   pattern2 = (UnOp MINUS theta1) against Real(π/2)
        //     -> recurses: theta1 vs Real(-π/2). Already bound to π/2. sameAngle(π/2, -π/2)?
        //        sameAngle uses mod 4π, so π/2 mod 4π = π/2, -π/2 mod 4π = -π/2 (mod arithmetic).
        //        Java's % preserves sign: -π/2 % 4π = -π/2. So π/2 ≠ -π/2 → false. Good.
        {
            Map<String, Expr> angleMap = new HashMap<>();
            Expr pattern1 = new Symbol("theta1");
            boolean ok1 = (boolean) m.invoke(opt, pattern1, new Real(1.5707963267948966), angleMap);
            Expr pattern2 = new UnOp(Expr.Op.MINUS, new Symbol("theta1"));
            boolean ok2 = (boolean) m.invoke(opt, pattern2, new Real(1.5707963267948966), angleMap);
            System.out.println("Test 4 [bind theta1=π/2, then (-theta1) vs Real(π/2)]: ok1="
                    + ok1 + ", ok2_should_be_false=" + ok2);
            assert ok1 && !ok2 : "expected reject (π/2 ≠ -π/2)";
        }

        // Test 5: the key qaoa-style match — bind in the SAME rule
        //   Rule: RXX(theta1); SYMB; RXX(-theta1)
        //   Circuit: RXX(π/2); SYMB; RXX(-π/2)
        //   First match: theta1 vs Real(π/2) -> binds theta1=π/2.
        //   Second match: (UnOp MINUS theta1) vs Real(-π/2)
        //     -> recurse: theta1 vs Real(π/2) (after negation). theta1 bound, sameAngle(π/2, π/2)=true.
        {
            Map<String, Expr> angleMap = new HashMap<>();
            Expr p1 = new Symbol("theta1");
            boolean ok1 = (boolean) m.invoke(opt, p1, new Real(1.5707963267948966), angleMap);
            Expr p2 = new UnOp(Expr.Op.MINUS, new Symbol("theta1"));
            boolean ok2 = (boolean) m.invoke(opt, p2, new Real(-1.5707963267948966), angleMap);
            System.out.println("Test 5 [RXX(theta1); ...; RXX(-theta1) vs RXX(π/2); ...; RXX(-π/2)]: ok1="
                    + ok1 + " ok2=" + ok2 + ", angleMap=" + angleMap);
            assert ok1 && ok2 : "the actual case the user wants — should pass";
        }

        System.out.println("\nAll tests passed.");
    }
}
