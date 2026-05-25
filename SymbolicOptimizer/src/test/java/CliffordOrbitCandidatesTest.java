import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;

/**
 * Unit tests for the {@link CliffordOrbitCandidates} generator.
 *
 * Structural tests live here. The semantic correctness check (each (L, R)
 * pair has an 8-dim intertwiner basis) lives in the Python test suite
 * `test_clifford_orbit.py` -- those tests verify the math via direct matrix
 * evaluation. Here we verify only that the Java generator emits the
 * expected number of pairs with the expected gate shape, so the pair list
 * threads through to `infer_symb` cleanly.
 *
 * JUnit 5 assertion order: (condition, message). Distinct from JUnit 4.
 */
public class CliffordOrbitCandidatesTest {

    @Test
    public void emptyForUnknownGateset() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("unknown");
        assertNotNull(cands);
        assertTrue(cands.isEmpty(),
                "unknown gateset should yield empty list, got " + cands.size());
    }

    @Test
    public void emptyForNullGateset() {
        assertTrue(CliffordOrbitCandidates.generateForGateset(null).isEmpty());
    }

    @Test
    public void ionGeneratesFourCandidates() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        assertEquals(4, cands.size());
    }

    @Test
    public void ionEveryLHSIsRXXThenSYMB() {
        // LHS shape: [RXX(θ) on q0,q1, SYMB] -- this is the canonical
        // "A; SYMB" form QSymb's compute_L_R expects (A = RXX, B = ε).
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> p : cands) {
            EggGen.Circuit L = p.getKey();
            assertEquals(2, L.gates.size(), "LHS must be RXX + SYMB");
            assertTrue(L.gates.get(0) instanceof EggGen.RXX,
                    "LHS[0] must be RXX, got " + L.gates.get(0).getClass().getSimpleName());
            assertTrue(L.gates.get(1) instanceof EggGen.SYMB,
                    "LHS[1] must be SYMB, got " + L.gates.get(1).getClass().getSimpleName());
            EggGen.RXX rxx = (EggGen.RXX) L.gates.get(0);
            assertEquals("q0", rxx.qubit1);
            assertEquals("q1", rxx.qubit2);
        }
    }

    @Test
    public void ionEveryRHSIsSYMBThenFiveGates() {
        // RHS shape: [SYMB, ...5-gate-decomp...] -- canonical "SYMB; D" form
        // (C = ε, D = 5-gate decomposition).
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (SimpleEntry<EggGen.Circuit, EggGen.Circuit> p : cands) {
            EggGen.Circuit R = p.getValue();
            assertEquals(6, R.gates.size(), "RHS must be SYMB + 5-gate decomp = 6 gates");
            assertTrue(R.gates.get(0) instanceof EggGen.SYMB,
                    "RHS[0] must be SYMB");
            // Middle RXX is at index 3 (SYMB + 2 wrappers + RXX = position 3).
            assertTrue(R.gates.get(3) instanceof EggGen.RXX,
                    "RHS[3] must be RXX, got " + R.gates.get(3).getClass().getSimpleName());
            EggGen.RXX rxx = (EggGen.RXX) R.gates.get(3);
            assertEquals("q0", rxx.qubit1);
            assertEquals("q1", rxx.qubit2);
        }
    }

    @Test
    public void ionFirstTwoCandidatesAreRyDecomp() {
        // The first two pairs are the RZZ orbit (via Y-axis conjugation):
        // wrappers are RY. The other two are RZ-wrapped (RYY orbit).
        // After the SYMB at index 0, the decomp gates occupy indices 1..5.
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 0; i < 2; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            assertTrue(R.gates.get(1) instanceof EggGen.RY, "R[" + i + "].1 must be RY");
            assertTrue(R.gates.get(2) instanceof EggGen.RY, "R[" + i + "].2 must be RY");
            assertTrue(R.gates.get(4) instanceof EggGen.RY, "R[" + i + "].4 must be RY");
            assertTrue(R.gates.get(5) instanceof EggGen.RY, "R[" + i + "].5 must be RY");
        }
    }

    @Test
    public void ionLastTwoCandidatesAreRzDecomp() {
        List<SimpleEntry<EggGen.Circuit, EggGen.Circuit>> cands =
                CliffordOrbitCandidates.generateForGateset("ion");
        for (int i = 2; i < 4; i++) {
            EggGen.Circuit R = cands.get(i).getValue();
            assertTrue(R.gates.get(1) instanceof EggGen.RZ, "R[" + i + "].1 must be RZ");
            assertTrue(R.gates.get(2) instanceof EggGen.RZ, "R[" + i + "].2 must be RZ");
            assertTrue(R.gates.get(4) instanceof EggGen.RZ, "R[" + i + "].4 must be RZ");
            assertTrue(R.gates.get(5) instanceof EggGen.RZ, "R[" + i + "].5 must be RZ");
        }
    }
}
