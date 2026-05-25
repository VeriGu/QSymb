import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ast.Expr;
import ast.Symbol;

/**
 * End-to-end test of the priority pass via infer_symb directly.
 *
 * Bypasses the completeness pass (which requires a fully-initialised egglog
 * + enumeration pipeline) and just runs the orbit candidates through the
 * solver-driven rule emission path that the full main() also uses.
 *
 * Verifies:
 *   1. infer_symb accepts each orbit candidate without throwing.
 *   2. The resulting learned_matrix_constrained set is non-empty.
 *   3. At least one learned rule's LHS contains "RXX" and RHS contains
 *      either "RZZ" or "RYY" wrapper gates -- the hallmark of an
 *      orbit-derived rule.
 *
 * This is the canonical smoke test that the priority-pass integration
 * survives the full pair → solveSymb → MatrixConstrainedRule pipeline.
 */
public class CliffordOrbitEndToEndTest {

    @Test
    public void priorityPassEmitsOrbitRulesForIon() {
        // Minimal EnumeratorPrune construction. Concrete-rule generation isn't
        // exercised; we only need the infer_symb infrastructure.
        String[] gates = {"rx", "ry", "rz", "rxx"};
        Expr[] symbAngles = { new Symbol("theta1") };
        EnumeratorPrune enumerator = new EnumeratorPrune(
                gates, 3, new Random(0), symbAngles, "ion", true);

        // Generate orbit candidates and feed them through infer_symb.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> orbit =
                CliffordOrbitCandidates.generateForGateset("ion");
        assertEquals(4, orbit.size(), "ion orbit should have 4 candidates");

        enumerator.infer_symb(orbit, 2);

        Set<MatrixConstrainedRule> learned = enumerator.getLearnedMatrixConstrained();
        assertFalse(learned.isEmpty(),
                "expected at least one rule from orbit pass, got 0");

        // Verify at least one learned rule has the orbit-rule shape: LHS has
        // an RXX, RHS has either RZZ-like decomp (RY wrappers) or RYY-like
        // decomp (RZ wrappers). The exact strings come from infer_symb's
        // canonical form, but they must include those gate names.
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
