import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ast.Expr;
import ast.Symbol;

public class CliffordOrbitEndToEndTest {

    @Test
    public void priorityPassEmitsOrbitRulesForIon() {
        String[] gates = {"rx", "ry", "rz", "rxx"};
        Expr[] symbAngles = { new Symbol("theta1") };
        EnumeratorPrune enumerator = new EnumeratorPrune(
                gates, 3, new Random(0), symbAngles, "ion", true);

        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> orbit =
                CliffordOrbitCandidates.generateForGateset("ion");
        assertEquals(4, orbit.size(), "ion orbit should have 4 candidates");

        enumerator.infer_symb(orbit, 2);

        Set<MatrixConstrainedRule> learned = enumerator.getLearnedMatrixConstrained();
        assertFalse(learned.isEmpty(),
                "expected at least one rule from orbit pass, got 0");

        boolean foundOrbitShape = false;
        for (MatrixConstrainedRule r : learned) {
            String lhs = r.getLHS();
            String rhs = r.getRHS();
            boolean lhsHasRxx = lhs.contains("RXX");
            boolean rhsHasRyWrap = rhs.contains("RY ") || rhs.contains("RY\n");
            boolean rhsHasRzWrap = rhs.contains("RZ ") || rhs.contains("RZ\n");
            if (lhsHasRxx && (rhsHasRyWrap || rhsHasRzWrap)) {
                foundOrbitShape = true;
                break;
            }
        }
        assertTrue(foundOrbitShape,
                "no learned rule had the expected orbit shape (RXX on LHS, RY/RZ wrappers on RHS)");
    }
}
