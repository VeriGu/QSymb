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

        {
            Map<String, Expr> angleMap = new HashMap<>();
            Expr pattern = new Real(-1.5707963267948966);
            Expr circ = new UnOp(Expr.Op.MINUS, new Real(1.5707963267948966));
            boolean ok = (boolean) m.invoke(opt, pattern, circ, angleMap);
            System.out.println("Test 2 [Real(-π/2) vs (UnOp MINUS Real(π/2))]: " + ok);
            assert ok : "expected symmetric semantic match";
        }

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
